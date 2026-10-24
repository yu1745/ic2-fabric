"""离线预览（无需游戏/Blender）：原版 2D 贴图放大 + 3D 模型两个视角，拼成一张对照图。

用法：python3 tools/model3d/item3d/preview.py <assets 目录> <输出.png> <物品名>...
渲染：深度缓冲逐三角形光栅化，面色 = 材质贴图在面 UV 中心的颜色 × MC 固定面亮度（上 1.0 / 南北 0.8 / 东西 0.6 / 下 0.5）。
"""
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import build  # noqa: E402,F401  注册全部物品
import core  # noqa: E402
from models import polys  # noqa: E402

S = 256


def original_texture(assets, name):
    # 物品模型改写前引用的 2D 贴图：优先读 git 中的旧模型 JSON，取 layer0 对应的 ic2 原图
    import subprocess
    rel = f'core/src/main/resources/assets/{core.NS}/models/item/{name}.json'
    root = Path(assets).resolve().parents[3]
    try:
        old = json.loads(subprocess.check_output(['git', '-C', str(root), 'show', f'HEAD:{rel}'], stderr=subprocess.DEVNULL))
        tex = old.get('textures', {}).get('layer0')
    except Exception:
        tex = None
    if not tex:
        return None
    path = tex.split(':', 1)[-1]
    for ns in ('ic2', core.NS):
        p = Path(assets) / ns / 'textures' / f'{path}.png'
        if p.exists():
            im = Image.open(p).convert('RGBA')
            return im.crop((0, 0, im.width, im.width))
    return None


def render(model, assets, yaw, pitch):
    tdir = Path(assets) / core.NS / 'textures' / core.TEX_DIR
    cache = {}

    def color(key, uv):
        if key not in cache:
            p = tdir / f'{key}.png'
            cache[key] = Image.open(p).convert('RGBA') if p.exists() else core.make_texture(key)
        im = cache[key]
        u = sum(a for a, _ in uv) / len(uv) / 16 % 1
        v = sum(b for _, b in uv) / len(uv) / 16 % 1
        return im.getpixel((min(im.width - 1, int(u * im.width)), min(im.height - 1, int(v * im.height))))

    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def proj(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return S / 2 + x * S / 18, S / 2 - y * S / 18, z

    # 深度缓冲光栅化（画家算法在凸起细节处排序错误）；SS 倍超采样后缩小抗锯齿
    ss = 2
    W = S * ss
    zbuf = np.full((W, W), -1e9)
    img = np.zeros((W, W, 3))
    img[:] = (70, 74, 84)
    ys, xs = np.mgrid[0:W, 0:W]
    for tex, pts, uvs, nrm in polys(model):
        q = [proj(p) for p in pts]
        n = [nrm[0] * cy - nrm[2] * sy, nrm[1], nrm[0] * sy + nrm[2] * cy]
        n = [n[0], n[1] * cp - n[2] * sp, n[1] * sp + n[2] * cp]
        if n[2] <= 1e-6:  # 背面剔除（视点在 +Z）
            continue
        ln = math.sqrt(sum(c * c for c in nrm)) or 1
        nx, ny, nz = (c / ln for c in nrm)
        shade = nx * nx * 0.6 + nz * nz * 0.8 + ny * ny * (1.0 if ny > 0 else 0.5)
        if tex.startswith('glow_'):
            shade = 1.0
        col = np.array(color(tex, uvs)[:3]) * shade
        P = [(x * ss, y * ss, z) for x, y, z in q]
        for i in range(1, len(P) - 1):
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = P[0], P[i], P[i + 1]
            d = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(d) < 1e-9:
                continue
            bx0, bx1 = max(0, int(min(x0, x1, x2))), min(W - 1, int(max(x0, x1, x2)) + 1)
            by0, by1 = max(0, int(min(y0, y1, y2))), min(W - 1, int(max(y0, y1, y2)) + 1)
            if bx0 > bx1 or by0 > by1:
                continue
            X = xs[by0:by1 + 1, bx0:bx1 + 1] + 0.5
            Y = ys[by0:by1 + 1, bx0:bx1 + 1] + 0.5
            a0 = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / d
            a1 = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / d
            a2 = 1 - a0 - a1
            inside = (a0 >= -1e-6) & (a1 >= -1e-6) & (a2 >= -1e-6)
            Z = a0 * z0 + a1 * z1 + a2 * z2
            zb = zbuf[by0:by1 + 1, bx0:bx1 + 1]
            hit = inside & (Z > zb)
            zb[hit] = Z[hit]
            img[by0:by1 + 1, bx0:bx1 + 1][hit] = col
    im = Image.fromarray(img.clip(0, 255).astype('uint8'), 'RGB').resize((S, S), Image.LANCZOS).convert('RGBA')
    return im


def main(assets, out, names):
    rows = []
    for name in names:
        model = core.ITEMS[name]()
        row = Image.new('RGBA', (S * 3 + 16, S), (50, 52, 60, 255))
        orig = original_texture(assets, name)
        if orig:
            row.paste(orig.resize((S, S), Image.NEAREST), (0, 0), orig.resize((S, S), Image.NEAREST))
        row.paste(render(model, assets, -35, 25), (S + 8, 0))   # 与 GUI 显示变换一致
        row.paste(render(model, assets, 0, 0), (2 * S + 16, 0))  # 正视
        rows.append(row)
    sheet = Image.new('RGBA', (S * 3 + 16, len(rows) * (S + 6)), (30, 30, 34, 255))
    for i, row in enumerate(rows):
        sheet.paste(row, (0, i * (S + 6)))
    sheet.save(out)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3:])
