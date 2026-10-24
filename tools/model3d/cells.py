"""流体单元 3D 化：物品模型为 builtin/entity，由客户端 FluidCellItemRenderer 绘制。

- 外壳（上下对称的端座、钢箍、护条）导出为 OBJ `item/cell3d/shell`；
- 液柱用流体真实贴图、液面随手部运动晃动，玻璃半透明，均在渲染器中动态绘制；
- 非流体内容物（air_cell、bio_cell）用原图取色生成的静态贴图。

用法：python3 cells.py <assets 目录（含 ic2 与 ic2_120）>
"""
import colorsys
import json
import math
import sys
from pathlib import Path

from PIL import Image

from models import Model, to_obj, write
import textures as T

NS = 'ic2_120'
DIR = 'item/cell3d'
N = 32


# 物品 → ic2 原图（item/cell/ 下）；None 表示通用/空单元
CELLS = {
    'air_cell': 'air_cell', 'bio_cell': 'bio_cell', 'biofuel_cell': 'ref_bio_cell', 'biomass_cell': 'biomass_cell',
    'construction_foam_cell': 'cf_cell', 'coolant_cell': 'coolant_cell', 'distilled_water_cell': 'distilled_water_cell',
    'hot_coolant_cell': 'hot_coolant_cell', 'lava_cell': 'lava_cell', 'pahoehoe_lava_cell': 'pahoehoelava_cell',
    'steam_cell': 'steam_cell', 'superheated_steam_cell': 'superheated_steam_cell', 'uu_matter_cell': 'uu_matter_cell',
    'water_cell': 'water_cell', 'weed_ex_cell': None, 'fluid_cell': None, 'empty_cell': None,
}
STATIC_FILL = ('air_cell', 'bio_cell')


def cell_model():
    m = Model()
    c, ph = (8, 8), math.pi / 8  # 八棱柱，平面朝向坐标轴
    m.prism('y', 1, 2.5, c, 3.9, 8, {'*': 'dark_steel', 'cap': 'dark_steel'}, ph)       # 底座
    m.prism('y', 2.5, 3.5, c, 3.6, 8, {'*': 'steel', 'cap': 'steel'}, ph)               # 下箍
    m.prism('y', 12.5, 13.5, c, 3.6, 8, {'*': 'steel', 'cap': 'steel'}, ph)             # 上箍
    m.prism('y', 13.5, 15, c, 3.9, 8, {'*': 'dark_steel', 'cap': 'dark_steel'}, ph)     # 顶座（与底座对称）
    # 四根护条：竖向贯穿玻璃段
    for x, z in ((4.4, 7.4), (10.4, 7.4), (7.4, 4.4), (7.4, 10.4)):
        m.box(x, 3.5, z, x + 1.2, 12.5, z + 1.2, 'steel')
    return m


def fluid_color(src_dir, name):
    ref = CELLS[name]
    if ref is None:
        return None
    im = Image.open(src_dir / f'{ref}.png').convert('RGBA')
    empty = Image.open(src_dir / 'empty_cell.png').convert('RGBA')
    px = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width)
          if im.getpixel((x, y))[3] > 128 and im.getpixel((x, y)) != empty.getpixel((x, y))]
    if not px:
        return (0x9a, 0xb4, 0xc4)
    # 用饱和度最高的一半，避免轮廓暗像素拉低
    px.sort(key=lambda c: colorsys.rgb_to_hsv(*(v / 255 for v in c[:3]))[1], reverse=True)
    px = px[:max(1, len(px) // 2)]
    return tuple(sum(c[i] for c in px) // len(px) for i in range(3))


def shade(rgb, k):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    r, g, b = colorsys.hsv_to_rgb(h, s, max(0.0, min(1.0, v * k)))
    return (int(r * 255), int(g * 255), int(b * 255), 255)


def fluid_tex(rgb):
    # 竖向渐变 + 液面亮线 + 两道竖向反光；明度先抬到 ≥0.55，避免暗沉
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    base = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(h, s, max(v, 0.62)))
    im = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            k = 0.82 + 0.25 * (y / N)
            if x % 16 in (3, 4):
                k += 0.22
            elif x % 16 == 11:
                k += 0.1
            im.putpixel((x, y), shade(base, k))
    return im


def glass_tex():
    im = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            c = T.hx('b9d6e0')
            if x % 16 in (3, 4):
                c = T.hx('eef8fb')
            elif x % 16 == 11:
                c = T.hx('d6e9ef')
            im.putpixel((x, y), c)
    return im


def main(assets):
    assets = Path(assets)
    dst = assets / NS
    src = assets / 'ic2' / 'textures' / 'item' / 'cell'
    tdir = dst / 'textures' / DIR
    tdir.mkdir(parents=True, exist_ok=True)
    glass_tex().save(tdir / 'glass.png')
    # 非流体内容物（压缩空气、生物质块）没有流体贴图，用原图取色生成
    for name in STATIC_FILL:
        fluid_tex(fluid_color(src, name)).save(tdir / f'{name}.png')

    # 外壳：只含金属件；液柱与玻璃由 FluidCellItemRenderer 动态绘制
    obj, mtl, _ = to_obj(cell_model(), 'shell')
    mdir = dst / 'models' / DIR
    write(mdir / 'shell.obj', obj)
    write(mdir / 'shell.mtl', mtl)
    write(mdir / 'shell.json', {
        'loader': 'ic2_120:obj',
        'model': f'{NS}:models/{DIR}/shell.obj',
        'textures': {
            'particle': f'{NS}:block/machine3d/steel',
            'dark_steel': f'{NS}:block/machine3d/dark_steel',
            'steel': f'{NS}:block/machine3d/steel',
        },
    })
    for name in CELLS:
        write(dst / 'models' / 'item' / f'{name}.json', {
            'parent': f'{NS}:{DIR}/display',
            'textures': {'particle': f'{NS}:block/machine3d/steel'},
        })
    # builtin/entity：交给 BuiltinItemRenderer；显示变换在此定义（GUI 放大，手持缩小）
    write(mdir / 'display.json', {
        'parent': 'minecraft:builtin/entity',
        'gui_light': 'side',
        'display': {
            'gui': {'rotation': [25, 225, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.8, 0.8, 0.8]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 3, 0], 'scale': [0.38, 0.38, 0.38]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 3, 0], 'scale': [0.38, 0.38, 0.38]},
        },
    })
    print(f'流体单元 {len(CELLS)} 个')


if __name__ == '__main__':
    main(sys.argv[1])
