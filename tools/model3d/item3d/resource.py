"""资源家族：参照 ic2 resource/{ingot,plate,casing,crushed,purified,raw,nuclear}/* 与 resource/*。

家族部件（见 b_parts）：
- 锭：三段收口梯形块，平放；
- 板：立放朝南的薄板（不规则外缘凸耳），正面凸起一层；致密板为三层叠压、更厚；
- 外壳：U 形冲压槽；
- 粉碎矿石：岩石碎块（灰）+ 矿物碎块混堆；纯净矿石：同堆形但全为矿物色、顶面高光；
- 粗矿：两三块交叠的大疙瘩；
- 核材料：球团（铀/钚/MOX）、小撮为小球、燃料芯块为横置八棱短圆柱。
"""
import math

from core import Model, item, mat
from core import sphere as lsphere
from b_parts import PH, chunk, sphere, ingot, plate, dense_plate, casing, mound


def M(c, s='flat'):
    return mat(c, s)


# ================================================================ 锭

def _ingot(edge, body, top, glint=None, style='metal'):
    m = Model()
    x0, y, z0, x1, z1 = ingot(m, M(body, style), M(top, style), M(edge))
    if glint:  # 顶面一道高光棱
        m.box(x0 + 0.5, y, z0 + 0.5, x0 + 4.5, y + 0.25, z0 + 1.25, M(glint))
    return m


@item('bronze_ingot')
def bronze_ingot():
    return _ingot('8a370c', 'c89042', 'd99c4c', 'ffa34e')


@item('lead_ingot')
def lead_ingot():
    return _ingot('535f51', '909981', 'd5d9b9', 'fafafa')


@item('refined_iron_ingot')
def refined_iron_ingot():
    return _ingot('595c68', '91a3aa', 'b9d6d9', 'fafafa')


@item('steel_ingot')
def steel_ingot():
    # 原图与精炼铁锭相同；钢锭主体压暗一档，顶面加冲压印记以区分
    m = _ingot('4a4d58', '6f7983', '91a3aa', 'b9d6d9')
    m.box(6.5, 11.0, 7.0, 9.5, 11.25, 9.0, M('595c68'))
    return m


@item('silver_ingot')
def silver_ingot():
    return _ingot('b8c0c0', 'dde3e3', 'f4f6f6', 'fafafa')


@item('tin_ingot')
def tin_ingot():
    return _ingot('717171', 'c8c8c8', 'e7e7e7', 'ffffff')


@item('uranium_ingot')
def uranium_ingot():
    return _ingot('1f5615', '62ac4d', '8bdb74', 'd5feb8')


@item('mixed_metal_ingot')
def mixed_metal_ingot():
    """合金锭：原图自上而下为钢（青白）/ 青铜 / 锡（灰）三层，做成三层叠压。"""
    m = Model()
    x0, x1, z0, z1 = 1.5, 14.5, 4.0, 12.0
    m.box(x0, 4.0, z0, x1, 4.5, z1, M('7b7b7b'))                                        # 底边
    m.box(x0 + 0.25, 4.5, z0 + 0.25, x1 - 0.25, 6.75, z1 - 0.25, M('9d9d9d', 'metal'))  # 锡层
    m.box(x0 + 0.75, 6.75, z0 + 0.75, x1 - 0.75, 9.0, z1 - 0.75, M('c89042', 'metal'))  # 青铜层
    m.box(x0 + 1.25, 9.0, z0 + 1.25, x1 - 1.25, 10.75, z1 - 1.25, M('91a3aa', 'metal'))  # 钢层
    m.box(x0 + 2.0, 10.75, z0 + 2.0, x1 - 2.0, 11.25, z1 - 2.0, M('b9d6d9', 'metal'))
    return m


# ================================================================ 板

LUGS = ((1.75, 6.0, 3.0, 10.0), (13.0, 4.0, 14.25, 8.0), (5.0, 13.0, 9.5, 14.25), (8.0, 1.75, 12.0, 3.0))


def _plate(face, rim, back, hi, lo=None, style='metal', pattern=None):
    m = Model()
    z = plate(m, M(face, style), M(rim), M(back))
    for (a, b, c, d) in LUGS:  # 外缘不规则凸耳（同厚度）
        m.box(a, b, 7.0, c, d, 8.5, {'*': M(rim), 'n': M(back), 's': M(face, style)})
    m.box(4.0, 11.0, z, 7.0, 12.0, z + 0.25, M(hi))         # 左上高光
    if lo:
        m.box(9.5, 4.0, z, 12.0, 5.0, z + 0.25, M(lo))      # 右下暗边
    if pattern:
        for (a, b, c) in pattern:
            m.box(a, b, z, a + 1.0, b + 1.0, z + 0.25, M(c))
    return m


@item('bronze_plate')
def bronze_plate():
    return _plate('ff9c3f', '9f3d00', '842600', 'ffcd98', 'de7b1e')


@item('copper_plate')
def copper_plate():
    return _plate('e77c56', '9c4529', '6d3421', 'fc9982', 'a75a40')


@item('gold_plate')
def gold_plate():
    return _plate('fad64a', 'b26411', '752802', 'fffde0', 'dc9613')


@item('iron_plate')
def iron_plate():
    return _plate('d8d8d8', '727272', '4a4a4a', 'ffffff', '9c9c9c')


@item('lead_plate')
def lead_plate():
    return _plate('c8d6ba', '6a7560', '47503f', 'f5ffec', 'a1ae93')


@item('steel_plate')
def steel_plate():
    return _plate('a2a2ac', '3a3a46', '2e2e38', 'e1e1e6', '70707a')


@item('tin_plate')
def tin_plate():
    return _plate('cccccc', '8e8e8e', '7c7c7c', 'ffffff', 'aaaaaa')


@item('lapis_plate')
def lapis_plate():
    # 青金石板：方格斑纹
    pat = [(4.5, 5.5, '7497ea'), (7.5, 8.5, '7497ea'), (10.0, 6.5, '5a82e2'), (6.0, 10.0, '345ec3'), (9.0, 11.0, '7497ea')]
    return _plate('1c53a8', '1a3d8f', '163070', '7497ea', '345ec3', style='flat', pattern=pat)


@item('obsidian_plate')
def obsidian_plate():
    pat = [(4.5, 5.0, '3b2754'), (8.0, 9.0, '3b2754'), (10.0, 5.5, '271e3d'), (5.5, 9.5, '271e3d')]
    return _plate('342648', '1e1730', '1a1428', '4a3568', '271e3d', style='flat', pattern=pat)


def _dense(face, mid, rim, hi, style='metal'):
    m = Model()
    z = dense_plate(m, M(face, style), M(mid, style), M(rim))
    m.box(4.5, 10.5, z, 8.0, 11.5, z + 0.25, M(hi))          # 高光
    m.box(4.5, 11.5, z, 5.5, 8.0 + 4.5, z + 0.25, M(hi))
    # 侧面压痕：叠层之间的分缝（右下角露出的台阶边已体现，这里加铆点）
    for (x, y) in ((4.0, 4.0), (11.0, 4.0), (11.0, 11.0)):
        m.box(x, y, z, x + 1.0, y + 1.0, z + 0.4, M(mid, style))
    return m


@item('dense_bronze_plate')
def dense_bronze_plate():
    return _dense('c89042', 'b35915', '772500', 'ffa34e')


@item('dense_copper_plate')
def dense_copper_plate():
    return _dense('b4713d', 'a66430', '894b1b', 'bc7945')


@item('dense_gold_plate')
def dense_gold_plate():
    return _dense('ffff8b', 'dede00', '868600', 'ffffff')


@item('dense_iron_plate')
def dense_iron_plate():
    return _dense('b8b8b8', 'a3a3a3', '878787', 'e0e0e0')


@item('dense_lapis_plate')
def dense_lapis_plate():
    return _dense('2a4071', '1d2e8e', '172461', '4a64a8', style='flat')


@item('dense_lead_plate')
def dense_lead_plate():
    return _dense('d5d9b9', '8f9781', '384238', 'fafafa')


@item('dense_obsidian_plate')
def dense_obsidian_plate():
    return _dense('362b4f', '201a2c', '18141f', '4a3c6a', style='flat')


@item('dense_steel_plate')
def dense_steel_plate():
    return _dense('b9d6d9', '808f97', '383842', 'fafafa')


@item('dense_tin_plate')
def dense_tin_plate():
    return _dense('e8e8e8', 'c1c1c1', '8e8e8e', 'ffffff')


# ================================================================ 外壳

def _casing(light, mid, dark):
    m = Model()
    casing(m, M(light, 'metal'), M(mid, 'metal'), M(dark))
    return m


@item('bronze_casing')
def bronze_casing():
    return _casing('ffcd98', 'de7b1e', '9f3d00')


@item('copper_casing')
def copper_casing():
    return _casing('ffb8a8', 'e77c56', '9c4529')


@item('gold_casing')
def gold_casing():
    return _casing('fffde0', 'e9b115', 'b26411')


@item('iron_casing')
def iron_casing():
    return _casing('ffffff', 'b5b5b5', '727272')


@item('lead_casing')
def lead_casing():
    return _casing('f5ffec', 'a1ae93', '6a7560')


@item('steel_casing')
def steel_casing():
    return _casing('e1e1e6', '70707a', '3a3a46')


@item('tin_casing')
def tin_casing():
    return _casing('ffffff', 'c1c1c1', '939393')


# ================================================================ 粉碎矿石 / 纯净矿石

# 碎块簇布局：(x, y, z, sx, sy, sz, 类别) 类别 r=岩石 o=矿物
CRUSHED = (
    # 底层（铺满）
    (2.0, 3.0, 4.0, 4.5, 4.0, 4.5, 'r'), (6.0, 3.0, 3.5, 4.5, 4.5, 5.0, 'o'), (10.0, 3.0, 4.5, 4.0, 3.5, 4.5, 'r'),
    (2.5, 3.0, 8.5, 4.0, 3.0, 4.0, 'o'), (6.5, 3.0, 8.5, 4.0, 3.5, 4.0, 'r'), (10.0, 3.0, 9.0, 4.0, 3.0, 3.5, 'o'),
    # 中层
    (3.0, 7.0, 5.0, 4.0, 3.5, 4.5, 'o'), (6.5, 7.5, 4.5, 4.0, 3.0, 4.5, 'r'), (10.0, 6.5, 5.5, 3.5, 3.5, 4.0, 'o'),
    (4.5, 6.0, 9.0, 3.5, 2.5, 3.5, 'r'), (8.0, 6.5, 9.0, 3.5, 2.5, 3.0, 'o'),
    # 顶部
    (5.0, 10.5, 5.5, 3.5, 2.5, 3.5, 'o'), (8.5, 10.0, 6.0, 3.0, 2.5, 3.0, 'r'),
)
PURIFIED = (
    (2.0, 3.0, 4.0, 4.5, 4.0, 5.0, 'o'), (6.0, 3.0, 3.5, 4.5, 4.5, 5.5, 'o'), (10.0, 3.0, 4.0, 4.0, 4.0, 5.0, 'o'),
    (3.0, 3.0, 9.0, 4.5, 3.0, 3.5, 'o'), (7.5, 3.0, 9.0, 4.5, 3.0, 3.5, 'r'),
    (3.0, 7.0, 5.0, 4.5, 3.5, 5.0, 'o'), (7.0, 7.5, 4.5, 4.5, 3.5, 5.0, 'o'), (11.0, 7.0, 5.5, 3.0, 3.0, 4.0, 'o'),
    (5.0, 11.0, 5.5, 5.0, 2.5, 4.0, 'o'),
)
ROCK = (M('616161'), M('464646'))


def _crushed(ores, tops, layout=CRUSHED, rock=ROCK):
    m = Model()
    i = j = 0
    for (x, y, z, sx, sy, sz, k) in layout:
        if k == 'r':
            chunk(m, x, y, z, sx, sy, sz, rock[i % len(rock)])
            i += 1
        else:
            chunk(m, x, y, z, sx, sy, sz, M(ores[j % len(ores)]), M(tops[j % len(tops)]))
            j += 1
    return m


@item('crushed_copper')
def crushed_copper():
    return _crushed(('c95e35', '2a5447', '883f26', '84e2c5'), ('e97a52', '84e2c5', 'c95e35', 'e9fff8'))


@item('crushed_gold')
def crushed_gold():
    return _crushed(('e29104', 'b26411', 'faea2e'), ('faea2e', 'fff79d', 'fffffd'))


@item('crushed_iron')
def crushed_iron():
    return _crushed(('af8e77', '887455', 'd8af93'), ('d8af93', 'e9c8b1', 'fef4ed'), rock=(M('574d39'), M('464646')))


@item('crushed_lead')
def crushed_lead():
    return _crushed(('9aaeaf', '6b757c', 'afcdce'), ('cae7e8', 'afcdce', 'e9feff'))


@item('crushed_silver')
def crushed_silver():
    return _crushed(('aab9d3', '8a9ac1', 'c3d3e8'), ('c3d3e8', 'd8e8f9', 'f6fbfb'))


@item('crushed_tin')
def crushed_tin():
    return _crushed(('9a9a9a', 'c3c3c3', '727272'), ('dadada', 'f0f0f0', 'c3c3c3'))


@item('crushed_uranium')
def crushed_uranium():
    return _crushed(('2a7a00', '5db123', '146800'), ('81c54f', 'acea7c', '5db123'))


@item('purified_copper')
def purified_copper():
    return _crushed(('c95e35', 'a54926', '4d8d79', 'e97a52'), ('e97a52', 'fc9982', '84e2c5', 'fc9982'), PURIFIED, (M('635b40'),))


@item('purified_gold')
def purified_gold():
    return _crushed(('e29104', 'b26411', 'e29104'), ('faea2e', 'fff79d', 'fffffd'), PURIFIED, (M('6d4718'),))


@item('purified_iron')
def purified_iron():
    return _crushed(('af8e77', 'd8af93', '887455'), ('e9c8b1', 'fedec8', 'fef4ed'), PURIFIED, (M('574d39'),))


@item('purified_lead')
def purified_lead():
    return _crushed(('849199', '9aaeaf', '6b757c'), ('afcdce', 'cae7e8', 'e9feff'), PURIFIED, (M('515c64'),))


@item('purified_silver')
def purified_silver():
    return _crushed(('aab9d3', '8a9ac1', 'c3d3e8'), ('d8e8f9', 'e3f1fb', 'f6fbfb'), PURIFIED, (M('8a9ac1'),))


@item('purified_tin')
def purified_tin():
    return _crushed(('c3c3c3', '9a9a9a', 'dadada'), ('f0f0f0', 'ffffff', 'dadada'), PURIFIED, (M('727272'),))


@item('purified_uranium')
def purified_uranium():
    return _crushed(('3b9500', '5db123', '2a7a00'), ('81c54f', 'caff96', 'acea7c'), PURIFIED, (M('146800'),))


# ================================================================ 粗矿

def _raw(dark, base, light, hi):
    """粗矿疙瘩：一大一中一小三块交叠，顶面亮色倒角。"""
    m = Model()
    chunk(m, 2.5, 4.0, 4.0, 7.0, 6.5, 7.5, M(base), M(light))
    chunk(m, 8.0, 3.0, 5.0, 5.5, 5.5, 6.5, M(dark), M(base))
    chunk(m, 5.0, 8.5, 5.5, 5.0, 4.0, 5.0, M(base), M(hi))
    chunk(m, 9.5, 3.0, 9.0, 3.5, 3.0, 3.0, M(light), M(hi))
    return m


@item('raw_lead')
def raw_lead():
    return _raw('5a5c73', '91a7b7', 'a7d6de', 'dfffff')


@item('raw_tin')
def raw_tin():
    return _raw('636c73', '8c949c', 'a4adb7', 'e0e4e7')


@item('raw_uranium')
def raw_uranium():
    m = _raw('2f6e40', '3c8811', '74c700', 'b4f900')
    m.box(3.0, 4.5, 11.5, 5.0, 6.5, 11.75, M('93f91f', 'glow'))  # 原图亮绿斑点
    return m


# ================================================================ 核材料

def _ball(cols, rad=5.6, cy=8.0, specks=()):
    """球团：低多边形球（cols：底/主体/顶 取色），表面嵌斑点小凸块。"""
    m = Model()
    lsphere(m, 8.0, cy, 8.0, rad, M(cols[2]), rings=5, segs=10, tex_top=M(cols[4]), tex_bottom=M(cols[0]))
    for (x, y, c) in specks:
        m.box(x, y, 8.0 + rad * 0.82, x + 1.0, y + 1.0, 8.0 + rad * 0.82 + 0.8, M(c))
    return m


@item('uranium')
def uranium():
    return _ball(('294c07', '437a0c', '52950f', '60ae11', '70cc13'),
                 specks=((5.5, 9.0, '70cc13'), (9.0, 7.0, '335e09'), (7.0, 5.5, '60ae11')))


@item('uranium_235')
def uranium_235():
    return _ball(('2b5008', '3b6d0b', '579e10', '62b311', '6cc611'),
                 specks=((5.5, 9.0, '75d810'), (9.0, 6.5, '2b5008')))


@item('uranium_238')
def uranium_238():
    return _ball(('214007', '315709', '3c6a0a', '477e0c', '4f8e0f'),
                 specks=((6.0, 9.0, '5ca00e'), (9.0, 6.5, '214007')))


@item('plutonium')
def plutonium():
    return _ball(('575757', '6d6d6d', '999999', 'acacac', 'bcbcbc'),
                 specks=((5.5, 9.0, 'c9c9c9'), (9.0, 6.5, '868686')))


@item('mox')
def mox():
    # MOX：铀绿球团混入钚灰颗粒
    return _ball(('395a1a', '487121', '68a72a', '7bb243', '7ca159'),
                 specks=((5.0, 8.5, '8fa678'), (8.5, 10.0, '96b07f'), (9.5, 6.0, '8fa678'), (6.5, 5.5, '96b07f')))


def _small(cols, hi):
    m = Model()
    lsphere(m, 8.0, 7.5, 8.0, 3.2, M(cols[2]), rings=4, segs=8, tex_top=M(cols[3]), tex_bottom=M(cols[0]))
    m.box(6.5, 8.0, 10.2, 7.5, 9.0, 11.0, M(hi))
    return m


@item('small_uranium_235')
def small_uranium_235():
    return _small(('2b5008', '3b6d0b', '62b311', '6cc611'), '75d810')


@item('small_uranium_238')
def small_uranium_238():
    return _small(('214007', '315709', '477e0c', '4f8e0f'), '5ca00e')


@item('small_plutonium')
def small_plutonium():
    return _small(('575757', '6d6d6d', 'acacac', 'bcbcbc'), 'c9c9c9')


def _pellet(body, band, cap, hi, rad=4.6):
    """燃料芯块：横置八棱短圆柱，中段一道箍槽，两端倒角端面。"""
    m = Model()
    m.prism('x', 3.0, 3.8, (8.0, 8.0), rad * 0.85, 8, M(cap), PH)
    m.prism('x', 3.8, 7.4, (8.0, 8.0), rad, 8, M(body), PH)
    m.prism('x', 7.4, 8.6, (8.0, 8.0), rad * 0.92, 8, M(band), PH)
    m.prism('x', 8.6, 12.2, (8.0, 8.0), rad, 8, M(body), PH)
    m.prism('x', 12.2, 13.0, (8.0, 8.0), rad * 0.85, 8, M(cap), PH)
    m.box(4.5, 8.0 + rad * 0.92 - 0.3, 6.8, 7.0, 8.0 + rad * 0.92 + 0.15, 9.2, M(hi))  # 顶面高光
    return m


@item('uranium_pellet')
def uranium_pellet():
    return _pellet('52950f', '60ae11', '437a0c', '70cc13')


@item('mox_pellet')
def mox_pellet():
    return _pellet('68a72a', '8aaa6c', '446520', 'a1bd88')


@item('rtg_pellet')
def rtg_pellet():
    return _pellet('c73d33', 'dc5144', '9a2721', 'e06a5c', rad=5.2)


# ================================================================ 其他资源

@item('coke')
def coke():
    """焦炭：多孔的灰黑疙瘩（两团交叠的低多边形球），正面嵌深色气孔。"""
    m = Model()
    lsphere(m, 7.0, 7.5, 8.0, 5.4, M('484751'), rings=4, segs=7, tex_top=M('5f5e69'), tex_bottom=M('373743'))
    lsphere(m, 10.5, 6.0, 8.5, 3.8, M('403f49'), rings=3, segs=6, tex_top=M('51505b'), tex_bottom=M('32323d'))
    for (x, y, z) in ((4.5, 6.5, 12.4), (7.5, 9.0, 12.4), (6.5, 4.5, 11.8), (11.0, 6.0, 11.8)):
        m.box(x, y, z, x + 1.0, y + 1.0, z + 0.6, M('23232e'))
    return m


@item('iodine')
def iodine():
    """碘：棕色玻璃皿（八棱扁盒）中盛紫黑色碘晶。"""
    m = Model()
    m.prism('y', 3.5, 4.5, (8.0, 8.0), 6.5, 8, M('6e3c27'), PH)          # 底
    m.prism('y', 4.5, 8.0, (8.0, 8.0), 6.5, 8, M('8c4e33', 'glass'), PH)  # 皿壁
    m.prism('y', 4.5, 7.6, (8.0, 8.0), 5.4, 8, M('453484'), PH)           # 碘晶层
    m.prism('y', 7.6, 9.5, (8.0, 8.0), 3.6, 8, M('3a2c74'), PH)           # 晶堆
    m.prism('y', 9.5, 10.5, (8.0, 8.0), 2.0, 8, M('32265e'), PH)
    for (x, y, z) in ((5.0, 7.6, 7.0), (9.5, 7.6, 9.0), (7.5, 9.5, 6.5)):  # 晶体棱
        m.box(x, y, z, x + 1.5, y + 1.5, z + 1.5, M('5a48a0', 'glass'))
    return m


@item('iridium_ore_item')
def iridium_ore_item():
    """铱矿石：两块亮银白疙瘩，表面带亮斑。"""
    m = Model()
    chunk(m, 2.0, 5.0, 4.0, 8.0, 7.5, 7.0, M('b4b4b4', 'metal'), M('dddddd'))
    chunk(m, 8.5, 3.0, 5.5, 5.5, 5.5, 6.0, M('9f9f9f', 'metal'), M('cecece'))
    chunk(m, 4.0, 3.5, 9.0, 4.0, 2.5, 3.0, M('808080'))
    m.box(3.5, 10.0, 11.0, 5.5, 11.5, 11.25, M('ffffff'))
    m.box(10.0, 6.5, 11.5, 11.5, 7.5, 11.75, M('ffffff'))
    return m


@item('iridium_shard')
def iridium_shard():
    """铱碎片：一枚斜向收尖的碎晶（逐级错位叠块）。"""
    m = Model()
    cols = ('808080', '9f9f9f', 'b4b4b4', 'cecece', 'dddddd', 'ffffff')
    for i, c in enumerate(cols):
        x = 4.0 + i * 1.0
        w = 5.5 - i * 0.7
        m.box(x, 3.0 + i * 1.6, 6.0, x + w, 4.6 + i * 1.6, 10.0 - i * 0.3, M(c, 'metal'))
    return m


@item('resin')
def resin():
    """粘性树脂：琥珀色水滴形胶团（下部球、上部收尖）。"""
    m = Model()
    sphere(m, 8.0, 6.5, 8.0, 4.8, [M(c, 'glass') for c in ('7b5828', 'a06a24', 'b57520', 'c48a3c', 'b57520')], layers=5)
    m.prism('y', 10.5, 12.0, (8.0, 8.0), 2.4, 8, M('c48a3c', 'glass'), PH)
    m.prism('y', 12.0, 13.5, (8.0, 8.0), 1.3, 8, M('d98b24', 'glass'), PH)
    m.box(5.5, 6.5, 12.5, 6.5, 8.5, 12.9, M('e19632', 'glow'))  # 高光点
    return m


@item('slag')
def slag():
    """炉渣：深褐色的尖刺状熔块（主块 + 斜向尖刺）。"""
    m = Model()
    D, B, L = M('301400'), M('3a1900'), M('512800')
    m.box(4.0, 3.0, 5.0, 12.0, 7.0, 11.0, D)
    m.box(5.0, 7.0, 6.0, 10.0, 9.0, 10.0, B)
    for (x, y0, h, w) in ((3.0, 6.0, 7.0, 1.5), (7.0, 9.0, 5.5, 1.5), (10.5, 7.0, 4.5, 1.5), (12.0, 4.0, 3.5, 1.5)):
        m.box(x, y0, 7.0, x + w, y0 + h, 8.5, B)          # 尖刺
        m.box(x + 0.25, y0 + h, 7.25, x + w - 0.25, y0 + h + 1.0, 8.25, L)
    m.box(4.5, 2.0, 6.0, 7.0, 3.0, 9.0, B)
    m.box(9.0, 2.0, 7.0, 11.5, 3.0, 10.0, B)
    return m
