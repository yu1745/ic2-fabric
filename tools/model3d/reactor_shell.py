"""反应堆透明外壳（压力容器 + 三种接口）的方块模型与贴图。

外壳统一造型：铅框（12 条棱）+ 强化玻璃面板（六面，带 cullface，相邻外壳方块之间的面由
ReactorShellBlock.isSideInvisible 隐藏，整圈外壳看起来是一体的透明壳）。接口在方块中心放置功能件，从任意面都能透过玻璃看到：
- 流体接口：黄铜三向管接头；
- 红石接口：红石芯块 + 四向导线；
- 访问接口：带把手的钢制舱门盒。

用法：python3 tools/model3d/reactor_shell.py <assets/ic2_120 目录>
"""
import json
import sys
from pathlib import Path

from PIL import Image

NS = 'ic2_120'
MDIR = 'block/generator/reactor'
T = 1.5  # 框宽

FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def uv(face, frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return {
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
        'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
        'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0],
    }[face]


def box(frm, to, tex, cull=False):
    faces = {}
    for f in FACES:
        d = {'uv': uv(f, frm, to), 'texture': '#' + tex}
        if cull:
            d['cullface'] = f
        faces[f] = d
    return {'from': list(frm), 'to': list(to), 'faces': faces}


def edges():
    """12 条棱：(名字, 元素, 该棱两侧的两个面)。棱只在两侧面都未与外壳/核心相连时绘制。"""
    out = []
    lo, hi = (0, T), (16 - T, 16)
    for an, a in (('lo', lo), ('hi', hi)):
        for bn, b in (('lo', lo), ('hi', hi)):
            yf = 'down' if an == 'lo' else 'up'
            out.append((f'x_{an}_{bn}', box((0, a[0], b[0]), (16, a[1], b[1]), 'frame'),
                        (yf, 'north' if bn == 'lo' else 'south')))                       # 沿 x：y、z
            out.append((f'y_{an}_{bn}', box((a[0], 0, b[0]), (a[1], 16, b[1]), 'frame'),
                        ('west' if an == 'lo' else 'east', 'north' if bn == 'lo' else 'south')))  # 沿 y：x、z
            out.append((f'z_{an}_{bn}', box((a[0], b[0], 0), (a[1], b[1], 16), 'frame'),
                        ('west' if an == 'lo' else 'east', 'down' if bn == 'lo' else 'up')))     # 沿 z：x、y
    return out


def frame():
    return [e for _, e, _ in edges()]


def glass():
    return [box((0, 0, 0), (16, 16, 16), 'glass', cull=True)]


def fluid_port():
    els = [box((6, 1.5, 6), (10, 14.5, 10), 'pipe'), box((1.5, 6, 6), (14.5, 10, 10), 'pipe'),
           box((6, 6, 1.5), (10, 10, 14.5), 'pipe')]
    for a, b in ((1.5, 3), (13, 14.5)):  # 管口法兰
        els += [box((5, a, 5), (11, b, 11), 'flange'), box((a, 5, 5), (b, 11, 11), 'flange'),
                box((5, 5, a), (11, 11, b), 'flange')]
    return els


def redstone_port():
    els = [box((5, 5, 5), (11, 11, 11), 'core')]
    for frm, to in (((7.25, 1.5, 7.25), (8.75, 5, 8.75)), ((7.25, 11, 7.25), (8.75, 14.5, 8.75)),
                    ((1.5, 7.25, 7.25), (5, 8.75, 8.75)), ((11, 7.25, 7.25), (14.5, 8.75, 8.75)),
                    ((7.25, 7.25, 1.5), (8.75, 8.75, 5)), ((7.25, 7.25, 11), (8.75, 8.75, 14.5))):
        els.append(box(frm, to, 'wire'))
    return els


def access_hatch():
    return [box((3.5, 3.5, 3.5), (12.5, 12.5, 12.5), 'hatch'),
            box((5, 7.25, 2.5), (11, 8.75, 3.5), 'handle'), box((5, 7.25, 12.5), (11, 8.75, 13.5), 'handle'),
            box((2.5, 7.25, 5), (3.5, 8.75, 11), 'handle'), box((12.5, 7.25, 5), (13.5, 8.75, 11), 'handle')]


BLOCKS = {
    'reactor_vessel': (lambda: [], {}),
    'reactor_fluid_port': (fluid_port, {'pipe': f'{NS}:block/machine3d/brass', 'flange': f'{NS}:block/machine3d/copper'}),
    'reactor_redstone_port': (redstone_port, {'core': f'{NS}:{MDIR}/shell_redstone_core', 'wire': f'{NS}:{MDIR}/shell_redstone_core'}),
    'reactor_access_hatch': (access_hatch, {'hatch': f'{NS}:block/machine3d/steel', 'handle': f'{NS}:block/machine3d/dark_steel'}),
}


def textures(tdir):
    tdir.mkdir(parents=True, exist_ok=True)
    n = 16
    # 铅框：蓝灰金属，外沿高光、内沿暗边，每 8px 一颗铆钉
    im = Image.new('RGBA', (n, n))
    for y in range(n):
        for x in range(n):
            c = (0x6a, 0x71, 0x82)
            if y == 0 or x == 0:
                c = (0x8c, 0x93, 0xa3)
            elif y == n - 1 or x == n - 1:
                c = (0x4c, 0x52, 0x60)
            if (x % 8, y % 8) in ((3, 3), (4, 4)):
                c = (0x9a, 0xa1, 0xb0) if x % 8 == 3 else (0x50, 0x56, 0x64)
            im.putpixel((x, y), c + (255,))
    im.save(tdir / 'shell_frame.png')
    # 强化玻璃：淡青、低不透明度，边沿一圈稍实，一道斜向反光
    im = Image.new('RGBA', (n, n))
    for y in range(n):
        for x in range(n):
            a, c = 46, (0xc4, 0xe4, 0xec)
            if x in (0, n - 1) or y in (0, n - 1):
                a = 90
            if (x + y) % 16 in (5, 6) and 3 <= x <= 12:
                a, c = 120, (0xf2, 0xfa, 0xfc)
            im.putpixel((x, y), c + (a,))
    im.save(tdir / 'shell_glass.png')
    # 红石芯：亮红块，中心更亮
    im = Image.new('RGBA', (n, n))
    for y in range(n):
        for x in range(n):
            d = max(abs(x - 7.5), abs(y - 7.5))
            v = 255 if d < 3 else 210 if d < 6 else 160
            im.putpixel((x, y), (v, int(v * 0.12), int(v * 0.08), 255))
    im.save(tdir / 'shell_redstone_core.png')


def write(p, data):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, indent=1, ensure_ascii=False) + '\n', encoding='utf-8')


def main(assets):
    a = Path(assets)
    textures(a / 'textures' / MDIR)
    base_tex = {'particle': f'{NS}:{MDIR}/shell_frame', 'frame': f'{NS}:{MDIR}/shell_frame',
                'glass': f'{NS}:{MDIR}/shell_glass'}
    # 12 条棱各自一个模型，所有外壳方块共用
    for en, el, _ in edges():
        write(a / 'models' / MDIR / f'shell_edge_{en}.json',
              {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': base_tex, 'elements': [el]})
    for name, (parts, extra) in BLOCKS.items():
        tex = {**base_tex, **extra}
        # 方块主体：玻璃 + 功能件（不含棱）；物品模型：完整外观（含 12 条棱）
        write(a / 'models' / MDIR / f'{name}.json',
              {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': tex,
               'elements': parts() + glass()})
        write(a / 'models' / MDIR / f'{name}_item.json',
              {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': tex,
               'elements': frame() + parts() + glass()})
        write(a / 'models' / 'item' / f'{name}.json', {'parent': f'{NS}:{MDIR}/{name}_item'})
        multipart = [{'apply': {'model': f'{NS}:{MDIR}/{name}'}}]
        for en, _, (f1, f2) in edges():
            multipart.append({'when': {f1: 'false', f2: 'false'}, 'apply': {'model': f'{NS}:{MDIR}/shell_edge_{en}'}})
        write(a / 'blockstates' / f'{name}.json', {'multipart': multipart})
    print(f'反应堆外壳模型 {len(BLOCKS)} 个')


if __name__ == '__main__':
    main(sys.argv[1])
