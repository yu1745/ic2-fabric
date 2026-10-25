"""物品 3D 建模公共部分：注册、材质、导出。

每个物品由一个手写函数建模（参照 ic2 原版 2D 贴图的结构与配色），返回 models.Model。
坐标沿用原版模型单位（1 格 = 16），物品竖直立于 x/y 平面中央，正面朝南（+Z，GUI 等轴视角可见）。

材质：mat('#5a9e1e') / mat('#5a9e1e', 'glass') / mat('#ffcc33', 'glow')，生成 16px 小贴图
（无磨损、轻微竖向渐变），文件名由样式与颜色决定，同色同样式的材质全局共享。
"""
import colorsys
import json
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from models import Model, polys, fix_winding, r, write  # noqa: E402

NS = 'ic2_120'
MODEL_DIR = 'item/item3d'
TEX_DIR = 'item/item3d'
STYLES = ('flat', 'glass', 'glow', 'metal')

ITEMS = {}  # 模型名（models/item/ 下的相对路径）→ 建模函数
HANDHELD = set()  # 使用手持工具显示变换的模型


def item(*names, handheld=False):
    """装饰器：注册建模函数。names 为 models/item/ 下的模型路径（含 overrides 子模型，如 battery/re_battery_1）。
    handheld=True：工具类，模型沿 +Y 竖直建模（工具头朝上），由 display_handheld 斜握。"""
    def deco(fn):
        for n in names:
            assert n not in ITEMS, f'重复注册 {n}'
            ITEMS[n] = fn
            if handheld:
                HANDHELD.add(n)
        return fn
    return deco


def obox(m, cx, cy, w, h, ang, z0, z1, front, side=None):
    """x/y 平面内旋转 ang（弧度）的长方体：w 沿旋转后的长轴，h 沿短轴，z0..z1 为厚度。
    用于斜放的物品（原版斜 45° 的叶片、轴、木棍、卡片等）。"""
    import math
    side = side or front
    ux, uy = math.cos(ang), math.sin(ang)
    vx, vy = -uy, ux
    corners = [(cx + sx * w / 2 * ux + sy * h / 2 * vx, cy + sx * w / 2 * uy + sy * h / 2 * vy)
               for sx, sy in ((-1, -1), (1, -1), (1, 1), (-1, 1))]  # 逆时针
    m.poly([(x, y, z1) for x, y in corners], front)
    m.poly([(x, y, z0) for x, y in corners[::-1]], side)
    for k in range(4):
        (ax, ay), (bx, by) = corners[k], corners[(k + 1) % 4]
        m.poly([(ax, ay, z0), (bx, by, z0), (bx, by, z1), (ax, ay, z1)], side)


def sphere(m, cx, cy, cz, r, tex, rings=4, segs=8, tex_top=None, tex_bottom=None, sy=1.0):
    """低多边形球（经纬划分，平面刻面，无层纹）；sy 为竖向缩放（<1 扁球）。"""
    import math
    def pt(i, j):
        th = math.pi * i / rings
        ph = 2 * math.pi * j / segs + math.pi / segs
        return (cx + r * math.sin(th) * math.cos(ph), cy + r * sy * math.cos(th), cz + r * math.sin(th) * math.sin(ph))
    for i in range(rings):
        t = tex_top if (i == 0 and tex_top) else tex_bottom if (i == rings - 1 and tex_bottom) else tex
        for j in range(segs):
            a, b, c, d = pt(i, j), pt(i, j + 1), pt(i + 1, j + 1), pt(i + 1, j)
            quad = [a, d, c, b]
            pts = [p for k, p in enumerate(quad) if p != quad[k - 1]]  # 极点处退化为三角形
            # 朝外：用面中心相对球心的方向纠正顶点顺序
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = pts[0], pts[1], pts[2]
            n = ((y1 - y0) * (z2 - z0) - (z1 - z0) * (y2 - y0), (z1 - z0) * (x2 - x0) - (x1 - x0) * (z2 - z0),
                 (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0))
            cc = [sum(p[k] for p in pts) / len(pts) - (cx, cy, cz)[k] for k in range(3)]
            if sum(n[k] * cc[k] for k in range(3)) < 0:
                pts = pts[::-1]
            m.poly(pts, t)


def mat(color, style='flat'):
    assert style in STYLES, style
    c = color.lstrip('#').lower()
    assert len(c) == 6, color
    return f'{style}_{c}'


def _rgb(hexs):
    return tuple(int(hexs[i:i + 2], 16) for i in (0, 2, 4))


def _shade(rgb, k):
    h, s, v = colorsys.rgb_to_hsv(*(x / 255 for x in rgb))
    rr, gg, bb = colorsys.hsv_to_rgb(h, s, max(0.0, min(1.0, v * k)))
    return int(rr * 255), int(gg * 255), int(bb * 255), 255


def make_texture(key):
    style, hexs = key.split('_', 1)
    base = _rgb(hexs)
    n = 16
    im = Image.new('RGBA', (n, n))
    for y in range(n):
        for x in range(n):
            k = 1.06 - 0.12 * (y / n)  # 上亮下暗的轻微渐变
            if style == 'glass' and x % 16 in (3, 4):
                k += 0.28
            elif style == 'metal' and x % 8 == 2:
                k += 0.12
            im.putpixel((x, y), _shade(base, k))
    return im


def to_obj_mtl(model, name):
    lines = ['# ic2_120 tools/model3d/item3d 生成，勿手改', f'mtllib {Path(name).name}.mtl']
    vi = 0
    used = []
    for tex, pts, uvs, nrm in polys(model):
        if tex not in used:
            used.append(tex)
        if not fix_winding(pts, nrm):
            pts, uvs = pts[::-1], uvs[::-1]
        lines.append(f'usemtl {tex}')
        for (x, y, z), (u, v) in zip(pts, uvs):
            lines.append(f'v {r(x / 16)} {r(y / 16)} {r(z / 16)}')
            lines.append(f'vt {r(u / 16)} {r(v / 16)}')
        ln = (sum(c * c for c in nrm)) ** 0.5
        lines.append('vn ' + ' '.join(str(r(c / ln)) for c in nrm))
        lines.append('f ' + ' '.join(f'{vi + k + 1}/{vi + k + 1}/-1' for k in range(len(pts))))
        vi += len(pts)
    mtl = []
    for t in used:
        mtl += [f'newmtl {t}', 'Kd 1 1 1', f'map_Kd #{t}'] + (['Ke 1 1 1'] if t.startswith('glow_') else []) + ['']
    return '\n'.join(lines) + '\n', '\n'.join(mtl), used


# 显示变换：手持/第三人称/掉落/展示框沿用原版 item/generated 的角度（原版物品是斜着拿的），
# 左手不写，由原版按右手镜像；GUI 略俯视以看出体积
DISPLAY = {
    'gui': {'rotation': [25, -35, 0], 'translation': [0, 0, 0], 'scale': [0.95, 0.95, 0.95]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
    'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 3, 1], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.4, 0.4, 0.4]},  # 原版手持锚点；3D 物品有厚度，缩小
}


# 工具：竖直建模，GUI 斜 45° 摆放（与原版手持物图标一致的朝向），手持时像原版工具一样斜握
DISPLAY_HANDHELD = {
    **DISPLAY,
    'gui': {'rotation': [20, -30, -45], 'translation': [0, 0, 0], 'scale': [0.95, 0.95, 0.95]},
    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
}


def export(assets, names=None):
    """导出 OBJ/MTL、物品模型 JSON（OBJ 声明）与材质贴图。names 为空则导出全部已注册物品。"""
    a = Path(assets) / NS
    mdir = a / 'models' / MODEL_DIR
    tdir = a / 'textures' / TEX_DIR
    tdir.mkdir(parents=True, exist_ok=True)
    write(mdir / 'display.json', {'parent': 'minecraft:block/block', 'display': DISPLAY})
    write(mdir / 'display_handheld.json', {'parent': 'minecraft:block/block', 'display': DISPLAY_HANDHELD})
    keys = set()
    for name in names or sorted(ITEMS):
        model = ITEMS[name]()
        obj, mtl, used = to_obj_mtl(model, name)
        write(mdir / f'{name}.obj', obj)
        write(mdir / f'{name}.mtl', mtl)
        textures = {'particle': f'{NS}:{TEX_DIR}/{used[0]}'}
        for t in used:
            textures[t] = f'{NS}:{TEX_DIR}/{t}'
            keys.add(t)
        item_json = a / 'models' / 'item' / f'{name}.json'
        old = json.loads(item_json.read_text()) if item_json.exists() else {}
        out = {
            'loader': 'ic2_120:obj',
            'model': f'{NS}:models/{MODEL_DIR}/{name}.obj',
            'parent': f'{NS}:{MODEL_DIR}/' + ('display_handheld' if name in HANDHELD else 'display'),
            'textures': textures,
        }
        if 'overrides' in old:
            out['overrides'] = old['overrides']  # 加载器负责烘焙 overrides（电量档位等）
        write(item_json, out)
    for k in keys:
        make_texture(k).save(tdir / f'{k}.png')
    return len(names or ITEMS)
