"""粉末家族：参照 ic2 resource/dust/*。

统一造型：圆顶粉堆（八棱柱逐层收口），底层暗色、中段主色、顶部高光，
正面/侧面嵌少量颗粒（取原图点缀色）。小撮粉（small_*）为更矮更小的同款堆。
"""
import math

from core import Model, item, mat
from b_parts import PH, mound

SPECK_DIRS = (0.0, 0.9, -0.9, 1.9, -1.7, 3.1)  # 颗粒方位角（弧度，0 = 正南）


def dust_pile(m, dark, base, light, specks=(), small=False, top=None, style='flat'):
    """粉堆部件。dark/base/light 为 hex 颜色；specks 为点缀色列表（逐个分布在堆表面）。"""
    if small:
        cx, cz, y0, h, rad, n = 8.0, 8.0, 5.0, 4.5, 4.4, 4
    else:
        cx, cz, y0, h, rad, n = 8.0, 8.0, 3.0, 8.5, 7.0, 6
    D, B, L = mat(dark), mat(base, style), mat(light, style)
    tex = [D] + [B] * (n - 2) + [L]
    layers = mound(m, cx, cz, y0, h, rad, tex, layers=n, power=2.4)
    # 侧面小鼓包（打破完全对称，像倒出来的一堆）
    k = 0.55
    mound(m, cx - rad * 0.45, cz + rad * 0.3, y0, h * 0.5, rad * k, [D] + [B] * 2, layers=3, power=2.4)
    mound(m, cx + rad * 0.5, cz - rad * 0.25, y0, h * 0.42, rad * k * 0.9, [D] + [B] * 2, layers=3, power=2.4)
    # 顶尖
    ly1, r1 = layers[-1][1], layers[-1][2]
    m.prism('y', ly1, ly1 + (0.8 if small else 1.2), (cx, cz), r1 * 0.55, 8, mat(top or light, style), PH)
    # 表面颗粒：在第 1..n-2 层的八棱面上外凸 0.4
    s = 0.7 if small else 1.0
    for k, c in enumerate(specks):
        li = 1 + k % max(1, n - 2)
        ya, yb, rr = layers[li]
        a = SPECK_DIRS[k % len(SPECK_DIRS)]
        ap = rr * math.cos(math.pi / 8) - 0.15
        px, pz = cx + ap * math.sin(a), cz + ap * math.cos(a)
        ym = (ya + yb) / 2
        m.box(px - s / 2, ym - s / 2, pz - s / 2, px + s / 2, ym + s / 2, pz + s / 2, mat(c, style if style != 'glass' else 'glow'))
    return m


def _dust(dark, base, light, specks=(), **kw):
    m = Model()
    dust_pile(m, dark, base, light, specks, **kw)
    return m


# ---------------------------------------------------------------- 普通粉

@item('bronze_dust')
def bronze_dust():
    return _dust('9c5417', 'ce762e', 'dc8a42', ('9f5c23', 'c4691b', 'e09450'))


@item('clay_dust')
def clay_dust():
    return _dust('4f5874', '757d90', 'acaebd', ('9499a4', '5e6c8d', 'afb9d6', '373944'))


@item('coal_dust')
def coal_dust():
    return _dust('303030', '3a3a3e', '525252', ('261e24', '1f1721', '4a4a4a'))


@item('coal_fuel_dust')
def coal_fuel_dust():
    # 燃料煤粉：煤粉底色偏藏青（含水分），点缀深蓝颗粒
    return _dust('283246', '343c52', '525a6a', ('0c1833', '1c2537', '4c5468'))


@item('copper_dust')
def copper_dust():
    return _dust('774115', 'a35d25', 'b8723a', ('9b561e', '7a4417', 'c07a40'))


@item('diamond_dust')
def diamond_dust():
    return _dust('11727a', '20c5b5', 'a1fbe8', ('d5fff6', 'ffffff', '4aedd9', 'd5fff6'), style='glass')


@item('ender_pearl_dust')
def ender_pearl_dust():
    # 原 JSON 误用青金石粉贴图；按末影珍珠的墨绿-青色系区分，结构同族
    return _dust('0f3d35', '1f6f5f', '4fb39a', ('2c8f7a', '7fd8c0', '135447'))


@item('energium_dust')
def energium_dust():
    return _dust('6c1717', 'c11f1f', 'e7aeae', ('ecd2d2', 'd46666', 'f7f0f0', 'db8888'), style='glass')


@item('gold_dust')
def gold_dust():
    return _dust('c8c720', 'fdff48', 'feff8c', ('e5e426', 'feff73', 'e5e42e'))


@item('hydrated_tin_dust')
def hydrated_tin_dust():
    return _dust('726ea9', 'a492a4', 'b3a1b2', ('7e82a9', '8b515a', '9d6968', '979ab2'))


@item('iron_dust')
def iron_dust():
    return _dust('878787', 'a4a4a4', 'c8c8c8', ('e3e3e3', 'b8b8b8', '878787'))


@item('lapis_dust')
def lapis_dust():
    return _dust('122d95', '1a3d8f', '5a82e2', ('345ec3', '7497ea', '052463', '344a97'))


@item('lead_dust')
def lead_dust():
    return _dust('2b332c', '76806e', 'd5d9b9', ('bcc2a5', 'fafafa', '535f51', '909981'))


@item('lithium_dust')
def lithium_dust():
    return _dust('616772', '819098', 'b9d6d9', ('2b2b32', 'b9d6d9', '2b2b32'))


@item('netherrack_dust')
def netherrack_dust():
    return _dust('411616', '572121', '934f4e', ('831919', '7d2a2a', '7c3a3a'))


@item('obsidian_dust')
def obsidian_dust():
    return _dust('201b2b', '342c48', '483d60', ('0e0f16', '392f4c', '171522'))


@item('silicon_dioxide_dust')
def silicon_dioxide_dust():
    return _dust('5c544b', 'b97346', 'b9a198', ('bd7e5c', 'bd93a5', '7b594d', 'bfa9a1'))


@item('silver_dust')
def silver_dust():
    return _dust('c8c8c8', 'e5e5e5', 'f2f2f2', ('d4d4d4', 'ffffff', 'dadada'))


@item('stone_dust')
def stone_dust():
    return _dust('4e4e4e', '8c8c8c', '9b9b9b', ('626262', '626262', '4e4e4e'))


@item('sulfur_dust')
def sulfur_dust():
    return _dust('676800', 'a6a61c', 'd3d92b', ('7f7f0a', 'bec21c', '919013', 'd3d92b'))


@item('tin_dust')
def tin_dust():
    return _dust('b0b0b0', 'd4d4d4', 'f2f2f2', ('c0c0c0', 'dedede', 'ffffff'))


# ---------------------------------------------------------------- 小撮粉（更小更矮的同款堆）

@item('small_bronze_dust')
def small_bronze_dust():
    return _dust('6e4f2c', 'cd752d', 'd87e34', ('b76825', '89592a'), small=True)


@item('small_copper_dust')
def small_copper_dust():
    return _dust('774115', '9b561d', 'a76229', ('a35d25', '784115'), small=True)


@item('small_gold_dust')
def small_gold_dust():
    return _dust('c8c720', 'e5e426', 'feff8c', ('fdff47', 'feff73'), small=True)


@item('small_iron_dust')
def small_iron_dust():
    return _dust('6c6c6c', 'a4a4a4', 'e3e3e3', ('b8b8b8', '878787'), small=True)


@item('small_lapis_dust')
def small_lapis_dust():
    return _dust('122d95', '1a3d8f', '5a82e2', ('345ec3', '052463'), small=True)


@item('small_lead_dust')
def small_lead_dust():
    return _dust('384238', '8f9781', 'd3d7b8', ('c0c4b8', 'f5f5f3'), small=True)


@item('small_lithium_dust')
def small_lithium_dust():
    return _dust('69767b', '91a3aa', 'bddbdd', ('222027', 'acc7ca'), small=True)


@item('small_obsidian_dust')
def small_obsidian_dust():
    return _dust('1b1827', '2c2440', '342a4b', ('101018', '322947'), small=True)


@item('small_silver_dust')
def small_silver_dust():
    return _dust('d4d4d4', 'e3e3e3', 'f1f1f1', ('dadada', 'eaeaea'), small=True)


@item('small_sulfur_dust')
def small_sulfur_dust():
    return _dust('676800', 'a6a61c', 'd3d92b', ('bec21c', '7f7f0a'), small=True)


@item('small_tin_dust')
def small_tin_dust():
    return _dust('b0b0b0', 'c0c0c0', 'f2f2f2', ('d4d4d4', 'dcdcdc'), small=True)
