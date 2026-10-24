"""物品贴图重制：以 ic2 原图为结构参考，按新风格（冷钢灰 + 划痕磨损，32px）重新上色。

一对一迁移：ic2:item/<路径> → ic2_120:item/<路径>，原来共享同一贴图的物品继续共享。

流程（每帧独立处理，竖条多帧贴图按 N×N 切帧）：
1. 16px 原图最近邻放大到 32px（已是 32px 的保持）；
2. 色彩重映射：低饱和像素映射到冷钢色阶；彩色像素保留色相，按亮度量化到
   "暗部偏冷、亮部偏暖"的色阶；
3. 形体：外缘 1px 暗轮廓，内侧受光倒角（左上亮、右下暗）；
4. 质感：金属区域按文件名种子加划痕与边缘磨损，2×2 放大块内做轻微抖动去块感。

用法：
  python3 items.py remaster <ic2 textures/item 目录> <输出目录> <相对路径>...
  python3 items.py sheet <输出.png> <原图目录> <新图目录> <相对路径>...
"""
import colorsys
import hashlib
import random
import sys
from pathlib import Path

from PIL import Image

N = 32

# 冷钢色阶（暗→亮），与 3D 机器材质同源
STEEL = [(0x1c, 0x1e, 0x23), (0x29, 0x2c, 0x33), (0x3a, 0x3f, 0x4a), (0x4f, 0x57, 0x67), (0x66, 0x6f, 0x80),
         (0x7f, 0x88, 0x99), (0x9a, 0xa3, 0xb2), (0xb8, 0xc0, 0xcc), (0xdd, 0xe2, 0xe8)]


LIFT = 0.75  # 整体提亮：伽马 <1 抬暗部，保留高光


def lift(v):
    return int(round(255 * (v / 255) ** LIFT))


def seed_of(name):
    return int(hashlib.md5(name.encode()).hexdigest()[:8], 16)


def lum(c):
    r, g, b = c[:3]
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255


def hue_ramp(h, s, levels=7):
    """色相 h 的色阶：暗部向蓝紫偏移并降饱和，亮部向黄偏移。"""
    out = []
    for i in range(levels):
        t = i / (levels - 1)
        # 暗部色相朝 0.66（蓝）拉，亮部朝 0.14（黄）拉，幅度 0.06
        target = 0.66 if t < 0.5 else 0.14
        k = abs(t - 0.5) * 2 * 0.06
        dh = ((target - h + 0.5) % 1.0) - 0.5
        hh = (h + dh * k / 0.5 * 0.5) % 1.0
        ss = max(0.0, min(1.0, s * (0.7 + 0.3 * (1 - abs(t - 0.55) * 1.6))))
        v = 0.14 + 0.84 * t
        r, g, b = colorsys.hsv_to_rgb(hh, ss, v)
        out.append((int(r * 255), int(g * 255), int(b * 255)))
    return out


class Frame:
    """一帧 32×32：每个像素记为 (色阶, 等级) 或 None（透明）。"""

    def __init__(self):
        self.px = [[None] * N for _ in range(N)]

    def get(self, x, y):
        if 0 <= x < N and 0 <= y < N:
            return self.px[y][x]
        return None

    def shift(self, x, y, d):
        p = self.get(x, y)
        if p:
            ramp, lv, metal = p
            self.px[y][x] = (ramp, max(0, min(len(ramp) - 1, lv + d)), metal)

    def image(self):
        im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        for y in range(N):
            for x in range(N):
                p = self.px[y][x]
                if p:
                    ramp, lv, _ = p
                    im.putpixel((x, y), tuple(lift(c) for c in ramp[lv]) + (255,))
        return im


NEUTRAL = [tuple(int(20 + i * 29) for _ in range(3)) for i in range(9)]


def set_size(n):
    global N
    N = n


def classify(src, neutral=False):
    """把源帧映射为 Frame：决定每个像素用哪条色阶、在第几级。"""
    src = src.convert('RGBA')
    scale = N // src.width
    ramps = {}
    f = Frame()
    for y in range(N):
        for x in range(N):
            c = src.getpixel((x // scale, y // scale)) if scale >= 1 else src.getpixel((x, y))
            if c[3] < 128:
                continue
            r, g, b = (v / 255 for v in c[:3])
            h, s, v = colorsys.rgb_to_hsv(r, g, b)
            L = lum(c)
            if s < 0.16 or v < 0.08:
                ramp = NEUTRAL if neutral else STEEL
                lv = min(len(ramp) - 1, int(round(L * (len(ramp) - 1) * 1.05)))
                f.px[y][x] = (ramp, lv, not neutral)
            else:
                key = round(h * 24) % 24, min(3, int(s * 4))
                if key not in ramps:
                    # 档位中值作为色阶饱和度，避免把低饱和的木色/棕色抬亮
                    ramps[key] = hue_ramp(key[0] / 24, (key[1] + 0.5) / 4)
                ramp = ramps[key]
                # 彩色按明度分级：纯红/纯蓝亮度低但明度高，按亮度会被压暗
                lv = min(len(ramp) - 1, int(round(v * (len(ramp) - 1))))
                f.px[y][x] = (ramp, lv, False)
    return f


def shape(f):
    """外缘暗轮廓 + 内侧倒角。整格不透明贴图（方块）按四方连续处理，不勾边。"""
    opaque = [[f.px[y][x] is not None for x in range(N)] for y in range(N)]
    tile = all(all(r) for r in opaque)

    def solid(x, y):
        if tile:
            return opaque[y % N][x % N]
        return 0 <= x < N and 0 <= y < N and opaque[y][x]
    edits = []
    for y in range(N):
        for x in range(N):
            if not opaque[y][x]:
                continue
            ramp, lv, metal = f.px[y][x]
            out_l, out_u = not solid(x - 1, y), not solid(x, y - 1)
            out_r, out_d = not solid(x + 1, y), not solid(x, y + 1)
            if out_l or out_u or out_r or out_d:
                # 细线条（两侧都透明，如作物支架、细杆）整根都是边缘，勾边会涂黑，只压暗一级
                thin = (out_l and not solid(x + 2, y)) or (out_r and not solid(x - 2, y)) or \
                       (out_u and not solid(x, y + 2)) or (out_d and not solid(x, y - 2))
                if thin:
                    edits.append((x, y, (ramp, max(0, lv - 1), metal)))
                else:
                    # 轮廓：取本色阶最暗两级之一
                    edits.append((x, y, (ramp, min(lv, 1 if metal else 0), metal)))
                continue
            # 轮廓内一圈：左上受光，右下背光
            near_lu = not solid(x - 2, y) or not solid(x, y - 2)
            near_rd = not solid(x + 2, y) or not solid(x, y + 2)
            if near_lu and not near_rd:
                edits.append((x, y, (ramp, min(len(ramp) - 1, lv + 1), metal)))
            elif near_rd and not near_lu:
                edits.append((x, y, (ramp, max(0, lv - 1), metal)))
    for x, y, p in edits:
        f.px[y][x] = p


def texture(f, rnd, wear=0):
    """2×2 块抖动 + 金属划痕/磨损。wear=0 时完全不加（当前默认）。"""
    if wear <= 0:
        return
    for y in range(0, N, 2):
        for x in range(0, N, 2):
            p = f.get(x + 1, y + 1)
            if p and p[1] >= 4 and rnd.random() < 0.04:
                f.shift(x + 1, y + 1, -1)
    metal = [(x, y) for y in range(2, N - 2) for x in range(2, N - 2)
             if f.get(x, y) and f.get(x, y)[2] and f.get(x, y)[1] >= 3]
    if not metal:
        return
    for _ in range(int(len(metal) / 90 * wear)):
        x, y = rnd.choice(metal)
        slope = rnd.choice((0.5, 1.0, -0.5))
        for i in range(rnd.randint(2, 5)):
            px, py = x + i, y + int(round(i * slope))
            p = f.get(px, py)
            if p and p[2] and p[1] >= 3:
                f.shift(px, py, 2 if i % 2 == 0 else 1)
                f.shift(px, py + 1, -1)
    for _ in range(int(len(metal) / 120 * wear)):
        x, y = rnd.choice(metal)
        f.shift(x, y, -2)
        f.shift(x + 1, y + 1, 1)


def remaster_image(src, name, neutral=False):
    w, h = src.size
    set_size(max(32, w))
    frames = h // w if h > w else 1
    out = Image.new('RGBA', (N, N * frames), (0, 0, 0, 0))
    for k in range(frames):
        fr = src.crop((0, k * w, w, (k + 1) * w))
        f = classify(fr, neutral)
        shape(f)
        texture(f, random.Random(seed_of(name) + k))
        out.paste(f.image(), (0, k * N))
    return out


def remaster(src_dir, out_dir, rels):
    src_dir, out_dir = Path(src_dir), Path(out_dir)
    done = 0
    for rel in rels:
        sp = src_dir / f'{rel}.png'
        if not sp.exists():
            print('缺原图', rel, file=sys.stderr)
            continue
        op = out_dir / f'{rel}.png'
        op.parent.mkdir(parents=True, exist_ok=True)
        remaster_image(Image.open(sp), rel).save(op)
        meta = sp.with_suffix('.png.mcmeta')
        if meta.exists():
            op.with_suffix('.png.mcmeta').write_text(meta.read_text())
        done += 1
    print(f'重制 {done} 张')


def sheet(out, src_dir, new_dir, rels, cols=6, S=128):
    rels = [r for r in rels if (Path(new_dir) / f'{r}.png').exists()]
    rows = (len(rels) + cols - 1) // cols
    im = Image.new('RGBA', (cols * (2 * S + 16), rows * (S + 8)), (58, 62, 70, 255))
    for i, r in enumerate(rels):
        x, y = (i % cols) * (2 * S + 16), (i // cols) * (S + 8)
        for j, d in enumerate((src_dir, new_dir)):
            try:  # 原图缺失或损坏（如 ic2 的 storage_box.png）时留空
                t = Image.open(Path(d) / f'{r}.png').convert('RGBA')
            except Exception:
                continue
            t = t.crop((0, 0, t.width, t.width)).resize((S, S), Image.NEAREST)
            im.paste(t, (x + j * (S + 4), y), t)
    im.save(out)


if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'remaster':
        remaster(sys.argv[2], sys.argv[3], sys.argv[4:])
    elif cmd == 'sheet':
        sheet(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5:])
