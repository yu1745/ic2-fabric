"""合成材料家族：参照 ic2 item/crafting/*。

家族部件：
- chip：电路板（基板 + 芯片 + 四周引脚，引脚末端红色焊点）；
- rotor_blade：竖直的转子叶片（八棱根部套筒 + 收窄叶身 + 前缘高光）；
- shaft：竖直八棱轴（两端轴环、中段高光棱）；
- saw_blade：朝南的圆锯片（八棱盘 + 齿 + 中心轴孔）。
"""
import math

from core import Model, item, mat, obox
from core import sphere as lsphere
from b_parts import PH, sphere, mound, chunk, disc_z, dense_plate
from dust import dust_pile


def M(c, s='flat'):
    return mat(c, s)


# ================================================================ 电路

def chip(m, board, body, core, hi, pin, tip, pins_x=(4.5, 7.5, 10.5), pins_y=(5.0, 8.0, 11.0)):
    """电路板：深色基板 + 凸起芯片（中心核心区）+ 四边引脚（银色，末端焊点）。"""
    m.box(3.0, 3.0, 6.5, 13.0, 13.0, 8.0, M(board))                       # 基板
    m.box(4.0, 4.0, 8.0, 12.0, 12.0, 9.5, M(body, 'metal'))              # 芯片封装
    m.box(6.0, 6.0, 9.5, 10.0, 10.0, 10.0, M(core))                       # 核心
    m.box(4.5, 10.5, 9.5, 7.0, 11.5, 9.75, M(hi))                         # 高光
    for x in pins_x:                                                      # 上下引脚
        m.box(x, 13.0, 7.0, x + 1.0, 14.5, 7.75, M(pin))
        m.box(x, 14.5, 7.0, x + 1.0, 15.5, 7.75, M(tip))
        m.box(x, 1.5, 7.0, x + 1.0, 3.0, 7.75, M(pin))
        m.box(x, 0.5, 7.0, x + 1.0, 1.5, 7.75, M(tip))
    for y in pins_y:                                                      # 左右引脚
        m.box(13.0, y, 7.0, 14.5, y + 1.0, 7.75, M(pin))
        m.box(14.5, y, 7.0, 15.5, y + 1.0, 7.75, M(tip))
        m.box(1.5, y, 7.0, 3.0, y + 1.0, 7.75, M(pin))
        m.box(0.5, y, 7.0, 1.5, y + 1.0, 7.75, M(tip))


@item('circuit')
def circuit():
    m = Model()
    chip(m, '636363', 'afafaf', '747474', 'd9d9d9', 'bababa', 'd92e2e')
    return m


@item('advanced_circuit')
def advanced_circuit():
    m = Model()
    chip(m, '597086', 'b2c9de', '7a91a6', 'd7ecff', 'cde4f9', 'ff2020')
    m.box(7.0, 7.0, 10.0, 9.0, 9.0, 10.25, M('96adc5', 'glow'))  # 高级电路核心发光点
    return m


# ================================================================ 转子叶片 / 轴

DIAG = math.pi / 4  # 原图斜 45°：左下 → 右上


def along(t, n=0.0):
    """沿斜轴（中心 8,8）距中心 t、法向偏移 n 的 x/y 坐标。"""
    c, s_ = math.cos(DIAG), math.sin(DIAG)
    return 8.0 + t * c - n * s_, 8.0 + t * s_ + n * c


def rotor_blade(m, root, body, edge, hi, tip=None):
    """斜放转子叶片：左下八棱感的根部套筒（两层方块），叶身向右上收窄（三段），
    上缘高光、下缘暗边、中筋压线。"""
    x, y = along(-7.0)
    obox(m, x, y, 3.0, 4.2, DIAG, 6.5, 9.5, M(root))                 # 根部套筒
    x, y = along(-5.2)
    obox(m, x, y, 1.0, 3.2, DIAG, 6.8, 9.2, M(edge))                 # 套筒颈
    for (t0, t1, w) in ((-4.7, -0.5, 5.2), (-0.5, 3.5, 4.4), (3.5, 7.2, 3.4)):
        tc, L = (t0 + t1) / 2, t1 - t0
        x, y = along(tc)
        obox(m, x, y, L, w, DIAG, 7.25, 8.75, M(body, 'metal'), M(edge))
        x, y = along(tc, w / 2 + 0.25)
        obox(m, x, y, L, 0.5, DIAG, 7.5, 8.5, M(hi))                 # 前缘
        x, y = along(tc, -w / 2 - 0.25)
        obox(m, x, y, L, 0.5, DIAG, 7.5, 8.5, M(edge))               # 后缘
    x, y = along(7.6)
    obox(m, x, y, 0.8, 2.6, DIAG, 7.5, 8.5, M(tip or hi))            # 叶尖
    x, y = along(1.0)
    obox(m, x, y, 9.0, 0.7, DIAG, 8.75, 9.0, M(edge))                # 中筋


@item('bronze_rotor_blade')
def bronze_rotor_blade():
    m = Model()
    rotor_blade(m, '772500', 'c89042', '8a370c', 'ffa34e')
    return m


@item('iron_rotor_blade')
def iron_rotor_blade():
    m = Model()
    rotor_blade(m, '5e5e5e', 'a8a8a8', '5e5e5e', 'ffffff', 'd8d8d8')
    return m


@item('steel_rotor_blade')
def steel_rotor_blade():
    m = Model()
    rotor_blade(m, '2b2b32', '91a3aa', '595c68', 'fafafa', 'b9d6d9')
    return m


@item('carbon_rotor_blade')
def carbon_rotor_blade():
    m = Model()
    rotor_blade(m, '262626', '363636', '262626', '5a5a5a')
    return m


@item('wooden_rotor_blade')
def wooden_rotor_blade():
    m = Model()
    rotor_blade(m, '59482d', '9f844d', '6f5a38', 'c5a678', 'bc9862')
    x, y = along(1.0, 1.2)
    obox(m, x, y, 7.0, 0.5, DIAG, 8.75, 9.0, M('836c3d'))  # 木纹
    return m


def shaft(m, dark, body, hi, collar):
    """斜放轴：方截面轴身（厚 4）+ 两端加粗轴环 + 端头 + 上表面键槽高光 + 中段刻线。"""
    obox(m, 8.0, 8.0, 13.0, 3.6, DIAG, 6.2, 9.8, M(body, 'metal'), M(dark))
    for t in (-5.4, 5.4):
        x, y = along(t)
        obox(m, x, y, 1.6, 4.8, DIAG, 5.6, 10.4, M(collar, 'metal'), M(dark))
    x, y = along(-7.2)
    obox(m, x, y, 1.2, 2.6, DIAG, 6.7, 9.3, M(dark))
    x, y = along(7.2)
    obox(m, x, y, 1.2, 2.6, DIAG, 6.7, 9.3, M(hi))
    x, y = along(0.0, 0.6)
    obox(m, x, y, 8.0, 0.9, DIAG, 9.8, 10.1, M(hi))                  # 键槽高光
    obox(m, 8.0, 8.0, 0.7, 3.9, DIAG, 5.9, 10.1, M(dark))            # 中段刻线


@item('bronze_shaft')
def bronze_shaft():
    m = Model()
    shaft(m, '5c1b00', 'b26a29', 'ffa34e', 'c89042')
    return m


@item('iron_shaft')
def iron_shaft():
    m = Model()
    shaft(m, '353535', '828282', 'ffffff', 'a8a8a8')
    return m


@item('steel_shaft')
def steel_shaft():
    m = Model()
    shaft(m, '2b2b32', '6f7983', 'fafafa', '91a3aa')
    return m


# ================================================================ 碳材料

CF_D, CF_M, CF_L = '303030', '3c3c3c', '4e4e4e'


@item('carbon_fibre')
def carbon_fibre():
    """碳纤维：一束蓬松的黑色纤维——主束沿左下→右上斜放，几缕反向纤维交叉缠绕，深浅相间。"""
    m = Model()
    for (n, L, z, c) in ((0.0, 13.0, 7.2, CF_M), (1.2, 11.0, 8.0, CF_L), (-1.3, 11.5, 7.8, CF_D),
                         (2.4, 8.0, 7.0, CF_M), (-2.5, 8.5, 7.0, CF_L)):
        x, y = along(n * 0.3, n)
        obox(m, x, y, L, 0.9, DIAG, z, z + 0.9, M(c))
    for (t, L, z, c) in ((-3.0, 6.0, 8.6, CF_L), (0.5, 7.5, 8.6, CF_M), (3.5, 5.5, 6.4, CF_L)):
        x, y = along(t)
        obox(m, x, y, L, 0.8, -DIAG, z, z + 0.8, M(c))
    return m


@item('carbon_mesh')
def carbon_mesh():
    """碳网：经纬交织的编织方片，经线与纬线交替上下（前后错开）。"""
    m = Model()
    m.box(2.5, 2.5, 7.4, 13.5, 13.5, 8.2, M('303030'))
    for i in range(5):
        a = 3.0 + i * 2.1
        for j in range(5):
            b = 3.0 + j * 2.1
            over = (i + j) % 2 == 0
            # 每个交点处：经线（竖）或纬线（横）在上
            if over:
                m.box(a, b - 0.3, 8.2, a + 1.4, b + 1.7, 8.9, M(CF_L))
            else:
                m.box(a - 0.3, b, 8.2, a + 1.7, b + 1.4, 8.9, M(CF_M))
    return m


@item('carbon_plate')
def carbon_plate():
    """碳板：黑色厚板，外框 + 竖向纤维压纹。"""
    m = Model()
    m.box(2.0, 3.5, 6.5, 14.0, 12.5, 8.5, M('303030'))
    m.box(2.5, 4.0, 8.5, 13.5, 12.0, 9.0, M('3a3a3a'))
    for i in range(6):
        x = 3.5 + i * 1.65
        m.box(x, 5.0, 9.0, x + 0.9, 11.0, 9.4, M('4a4a4a' if i % 2 else '424242'))
    return m


@item('cf_powder')
def cf_powder():
    """建筑泡沫粉：灰色粉堆（同粉末家族造型）。"""
    m = Model()
    dust_pile(m, '4e4e4e', '868686', '9b9b9b', ('686868', '4e4e4e', '9b9b9b'))
    return m


# ================================================================ 煤系

@item('coal_ball')
def coal_ball():
    """煤球：低多边形黑球，表面灰白碎屑，顶部略亮。"""
    m = Model()
    lsphere(m, 8.0, 8.0, 8.0, 6.0, M('343434'), rings=5, segs=10, tex_top=M('444444'), tex_bottom=M('303030'))
    for (x, y, c) in ((5.0, 9.5, 'd4d4d4'), (9.0, 6.0, '808080'), (6.5, 5.0, '808080'), (10.0, 10.0, 'd4d4d4')):
        m.box(x, y, 13.0, x + 1.0, y + 1.0, 13.8, M(c))
    return m


@item('coal_block')
def coal_block():
    """压缩煤球：倒角方块，顶/正面亮边。"""
    m = Model()
    m.box(3.0, 3.5, 3.0, 13.0, 12.5, 13.0, M('343434'))
    m.box(3.5, 12.5, 3.5, 12.5, 13.0, 12.5, M('434343'))
    m.box(3.0, 11.5, 13.0, 13.0, 12.5, 13.3, M('6b6b6b'))   # 正面上棱高光
    m.box(3.0, 4.0, 13.0, 4.0, 11.5, 13.3, M('4a4a4a'))
    for (x0, y0, x1, y1) in ((4.5, 5.0, 9.0, 5.8), (8.0, 8.0, 11.5, 8.8), (5.0, 9.5, 7.0, 10.3)):  # 压痕
        m.box(x0, y0, 13.0, x1, y1, 13.2, M('2a2a2a'))
    return m


@item('coal_chunk')
def coal_chunk():
    """煤块：大块深灰煤块，表面紫色矿脉纹（压缩成钻石前的原料）。"""
    m = Model()
    m.box(2.5, 2.5, 4.0, 13.5, 13.5, 12.0, M('343438'))
    m.box(3.0, 13.5, 4.5, 13.0, 14.0, 11.5, M('525257'))
    for (x0, y0, x1, y1, c) in ((3.5, 10.0, 8.0, 11.0, '4a3a5c'), (7.0, 6.5, 12.5, 7.5, '4a3a5c'),
                                (4.5, 4.0, 6.5, 6.0, '525257'), (9.5, 10.5, 12.5, 12.5, '525257')):
        m.box(x0, y0, 12.0, x1, y1, 12.3, M(c))
    return m


@item('compressed_hydrated_coal')
def compressed_hydrated_coal():
    """压缩水合煤：灰色压块，下方挂着深蓝水滴状尾部。"""
    m = Model()
    m.box(4.5, 9.0, 5.5, 11.5, 14.0, 10.5, M('595959'))
    m.box(4.5, 13.0, 10.5, 11.5, 14.0, 10.8, M('7d7d7d'))
    m.box(5.0, 7.5, 6.0, 11.0, 9.0, 10.0, M('2a3040'))
    m.prism('y', 4.5, 7.5, (8.0, 8.0), 1.4, 8, M('1f2848'), PH)
    m.prism('y', 2.0, 4.5, (8.0, 8.0), 2.2, 8, M('22305a', 'glass'), PH)
    return m


# ================================================================ 植物

@item('compressed_plants')
def compressed_plants():
    """压缩植物：一棵小树形的植物团——绿色叶团 + 短茎 + 根部土块。"""
    m = Model()
    lsphere(m, 8.0, 10.0, 8.0, 4.6, M('487717'), rings=4, segs=8, tex_top=M('5b8423'), tex_bottom=M('3b6c26'))
    m.box(6.0, 10.0, 12.0, 7.5, 11.5, 12.8, M('7bc22e'))
    m.box(9.0, 8.0, 11.8, 10.0, 9.0, 12.5, M('3da016'))
    m.prism('y', 3.0, 6.0, (8.0, 8.0), 0.9, 8, M('5a4a22'), PH)
    m.box(6.0, 1.5, 6.0, 10.0, 3.0, 10.0, M('4b4020'))
    return m


@item('plant_ball')
def plant_ball():
    """植物球：低多边形绿色叶团球，表面凸起的叶簇。"""
    m = Model()
    lsphere(m, 8.0, 8.0, 8.0, 6.0, M('4b9713'), rings=5, segs=10, tex_top=M('57af16'), tex_bottom=M('224e08'))
    for (x, y, z, c) in ((4.5, 9.0, 12.4, '57af16'), (9.0, 11.0, 11.8, '377612'), (8.0, 5.0, 12.6, '377612'),
                         (1.8, 6.0, 7.0, '57af16'), (12.8, 8.0, 7.5, '57af16'), (7.0, 13.6, 7.0, '57af16')):
        m.box(x, y, z, x + 1.5, y + 1.5, z + 1.2, M(c))
    m.box(6.0, 6.5, 13.6, 7.0, 7.5, 14.1, M('534210'))
    return m


@item('bio_chaff')
def bio_chaff():
    """生物糠：深绿色碎草堆，茎秆杂乱伸出。"""
    m = Model()
    dust_pile(m, '303012', '3a480e', '3d581d', ('51441e', '393b08', '51441e'))
    for (x, y0, y1, z, c) in ((4.0, 6.0, 12.0, 9.0, '3d581d'), (10.5, 5.0, 11.0, 7.5, '51441e'),
                              (7.5, 9.0, 14.0, 6.5, '3a480e'), (12.0, 3.0, 8.0, 10.0, '3d581d')):
        m.box(x, y0, z, x + 0.8, y1, z + 0.8, M(c))
    m.box(3.0, 10.0, 7.5, 9.0, 10.8, 8.3, M('3d581d'))
    return m


# ================================================================ 铜部件

COPPER, COPPER_D, COPPER_L = 'a66430', '9d5a26', 'bc7945'


@item('coil')
def coil():
    """线圈：深灰铁芯 + 铜线绕组（多圈环带，深浅交替）+ 两端法兰。"""
    m = Model()
    m.prism('y', 1.5, 14.5, (8.0, 8.0), 2.2, 8, M('40424c'), PH)
    m.prism('y', 1.5, 2.5, (8.0, 8.0), 5.6, 8, M('40424c'), PH)
    m.prism('y', 13.5, 14.5, (8.0, 8.0), 5.6, 8, M('40424c'), PH)
    for i in range(11):
        y = 2.5 + i * 1.0
        m.prism('y', y, y + 1.0, (8.0, 8.0), 4.8 if i % 2 == 0 else 4.6, 8, M(COPPER_L if i % 2 == 0 else COPPER, 'metal'), PH)
    return m


@item('copper_boiler')
def copper_boiler():
    """铜锅炉：立式铜罐，竖向铆接板 + 上下箍环 + 顶盖。"""
    m = Model()
    m.prism('y', 2.0, 14.0, (8.0, 8.0), 6.2, 8, M(COPPER, 'metal'), PH)
    for y in (2.0, 7.5, 13.0):
        m.prism('y', y, y + 1.0, (8.0, 8.0), 6.6, 8, M(COPPER_D), PH)
    m.prism('y', 14.0, 15.0, (8.0, 8.0), 4.5, 8, M(COPPER_L), PH)
    m.prism('y', 1.0, 2.0, (8.0, 8.0), 5.2, 8, M(COPPER_D), PH)
    for k in range(8):  # 竖向接缝
        a = PH * 2 * k + PH
        x, z = 8.0 + 6.0 * math.cos(a), 8.0 + 6.0 * math.sin(a)
        if z > 8.0:
            m.box(x - 0.3, 3.0, z - 0.3, x + 0.3, 13.0, z + 0.3, M(COPPER_L))
    return m


@item('heat_conductor')
def heat_conductor():
    """热传导器：铜制工字形导热件，上下黑色端板，中部两侧散热翅片。"""
    m = Model()
    m.box(3.0, 1.0, 5.5, 13.0, 2.5, 10.5, M('303030'))
    m.box(3.0, 13.5, 5.5, 13.0, 15.0, 10.5, M('303030'))
    m.box(6.5, 2.5, 6.5, 9.5, 13.5, 9.5, M(COPPER, 'metal'))
    m.box(4.5, 2.5, 7.0, 6.5, 4.0, 9.0, M('b4713d'))
    m.box(9.5, 2.5, 7.0, 11.5, 4.0, 9.0, M('b4713d'))
    m.box(4.5, 12.0, 7.0, 6.5, 13.5, 9.0, M('b4713d'))
    m.box(9.5, 12.0, 7.0, 11.5, 13.5, 9.0, M('b4713d'))
    m.box(1.5, 6.5, 7.0, 14.5, 9.5, 9.0, M('894b1b'))          # 横向翅片
    m.box(1.5, 6.5, 9.0, 3.0, 9.5, 9.25, M('b4713d'))
    m.box(13.0, 6.5, 9.0, 14.5, 9.5, 9.25, M('b4713d'))
    return m


# ================================================================ 晶体 / 钻石

@item('industrial_diamond')
def industrial_diamond():
    """工业钻石：多面体宝石——上部平台冠、腰部最宽、下部收尖的亭部。"""
    m = Model()
    layers = ((2.0, 3.5, 1.6, '142f2b'), (3.5, 5.0, 3.2, '2e685e'), (5.0, 6.5, 4.6, '377e72'),
              (6.5, 8.0, 5.8, '4cad9b'), (8.0, 9.5, 6.4, '68cab8'), (9.5, 11.0, 5.8, '97d6cc'),
              (11.0, 12.5, 4.6, 'a8dbd1'), (12.5, 13.5, 3.4, 'ebebeb'))
    for (y0, y1, rr, c) in layers:
        m.prism('y', y0, y1, (8.0, 8.0), rr, 8, M(c, 'glass'), PH)
    m.box(5.5, 10.0, 13.6, 7.0, 11.5, 14.0, M('ebebeb', 'glow'))
    return m


@item('iridium')
def iridium():
    """铱强化板：致密白色合金板，正面十字加强筋 + 四角铆钉。"""
    m = Model()
    z = dense_plate(m, M('f5f5f5', 'metal'), M('d8d8d8', 'metal'), M('9f9f9f'))
    m.box(7.0, 3.5, z, 9.0, 12.5, z + 0.75, M('ffffff'))
    m.box(3.5, 7.0, z, 12.5, 9.0, z + 0.75, M('eeeeee'))
    for (x, y) in ((4.0, 4.0), (11.0, 4.0), (4.0, 11.0), (11.0, 11.0)):
        m.box(x, y, z, x + 1.0, y + 1.0, z + 0.6, M('bbbbbb'))
    return m


@item('alloy')
def alloy():
    """高级合金：三层复合板（灰褐色），层层错位露出叠层边，正面铆点。"""
    m = Model()
    z = dense_plate(m, M('938d78', 'metal'), M('797460'), M('676455'))
    m.box(4.0, 10.5, z, 8.5, 11.5, z + 0.25, M('a8a391'))
    for (x, y) in ((4.0, 4.0), (11.0, 4.0), (11.0, 11.0)):
        m.box(x, y, z, x + 1.0, y + 1.0, z + 0.5, M('585446'))
    return m


def memory_disc(m, face, ring, rim, hub, hi):
    """晶体存储盘：朝南八棱圆盘，外缘暗圈 + 盘面 + 中心轴毂。"""
    disc_z(m, 8.0, 8.0, 6.5, 9.0, 6.6, M(rim))
    disc_z(m, 8.0, 8.0, 9.0, 9.5, 5.8, M(face, 'glass'))
    disc_z(m, 8.0, 8.0, 9.5, 9.8, 3.6, M(ring))
    disc_z(m, 8.0, 8.0, 9.5, 10.3, 1.6, M(hub))
    m.box(4.0, 10.0, 9.5, 6.5, 11.0, 9.75, M(hi))


@item('crystal_memory')
def crystal_memory():
    m = Model()
    memory_disc(m, 'f1e3d6', 'd4c4b7', '382c34', '857877', 'f4e6d9')
    return m


@item('raw_crystal_memory')
def raw_crystal_memory():
    m = Model()
    memory_disc(m, 'ad644d', 'a85941', '431c40', '3e1b3a', 'c88060')
    return m


# ================================================================ 锯片

def saw_blade(m, disc, tooth, hub, hi, glow=False):
    """方块切割刀片：朝南圆锯片——八棱盘 + 16 枚锯齿 + 中心轴孔（深色）+ 高光。"""
    s = 'glass' if glow else 'metal'
    disc_z(m, 8.0, 8.0, 7.25, 8.75, 5.4, M(disc, s))
    for k in range(16):
        a = 2 * math.pi * k / 16
        x, y = 8.0 + 6.0 * math.cos(a), 8.0 + 6.0 * math.sin(a)
        m.box(x - 0.7, y - 0.7, 7.5, x + 0.7, y + 0.7, 8.5, M(tooth))
    disc_z(m, 8.0, 8.0, 8.75, 9.0, 3.8, M(hi, s))
    disc_z(m, 8.0, 8.0, 7.0, 9.2, 2.0, M(hub))
    disc_z(m, 8.0, 8.0, 9.2, 9.4, 0.9, M('303030'))


@item('iron_block_cutting_blade')
def iron_block_cutting_blade():
    m = Model()
    saw_blade(m, 'a8a8a8', 'd8d8d8', '565656', 'ececec')
    return m


@item('steel_block_cutting_blade')
def steel_block_cutting_blade():
    m = Model()
    saw_blade(m, '7a7a85', 'b5b5be', '35353f', 'a8a8b2')
    return m


@item('diamond_block_cutting_blade')
def diamond_block_cutting_blade():
    m = Model()
    saw_blade(m, '11727a', 'a1fbe8', '494956', '4aedd9', glow=True)
    return m


# ================================================================ 电机 / 动力

@item('electric_motor')
def electric_motor():
    """电动马达：横置银色圆筒机身（散热环纹）+ 左侧深色端盖 + 右侧输出轴。"""
    m = Model()
    m.prism('x', 2.0, 3.0, (8.0, 8.0), 4.4, 8, M('424040'), PH)
    m.prism('x', 3.0, 12.0, (8.0, 8.0), 5.0, 8, M('bcbcbc', 'metal'), PH)
    for x in (4.5, 7.0, 9.5):
        m.prism('x', x, x + 0.8, (8.0, 8.0), 5.3, 8, M('838480'), PH)
    m.prism('x', 12.0, 13.0, (8.0, 8.0), 4.4, 8, M('6f7269'), PH)
    m.prism('x', 13.0, 15.0, (8.0, 8.0), 1.0, 8, M('797677'), PH)   # 输出轴
    m.box(5.0, 12.4, 6.5, 10.0, 13.6, 9.5, M('2b2b2b'))               # 接线盒
    return m


def ring_torus(m, cx, cy, z0, z1, rad, w, tex):
    """朝南的八段方环（近似圆环）。"""
    for k in range(8):
        a = 2 * math.pi * k / 8
        x, y = cx + rad * math.cos(a), cy + rad * math.sin(a)
        m.box(x - w, y - w, z0, x + w, y + w, z1, tex)


@item('power_unit')
def power_unit():
    """动力单元：黑色电池壳 + 橙色线圈环（电机绕组）+ 引线。"""
    m = Model()
    m.box(9.0, 9.0, 6.0, 14.0, 14.0, 10.0, M('323131'))             # 电池壳
    m.box(9.5, 13.0, 10.0, 13.5, 13.5, 10.3, M('505050'))
    ring_torus(m, 7.0, 7.0, 6.5, 9.5, 3.6, 1.3, M('f18000', 'metal'))  # 绕组
    disc_z(m, 7.0, 7.0, 7.0, 9.0, 1.4, M('434343'))
    m.box(10.0, 6.5, 7.5, 13.0, 7.5, 8.5, M('2e2d2d'))                 # 引线
    m.box(12.5, 6.5, 7.5, 13.5, 9.0, 8.5, M('2e2d2d'))
    m.box(10.5, 11.0, 10.0, 12.5, 12.0, 10.3, M('fb9000', 'glow'))    # 指示
    return m


@item('small_power_unit')
def small_power_unit():
    """小型动力单元：橙色钩形绕组 + 灰色接头。"""
    m = Model()
    m.box(4.0, 4.5, 7.0, 7.0, 9.0, 9.0, M('f07f02', 'metal'))
    m.box(7.0, 6.5, 7.0, 11.0, 9.0, 9.0, M('fa8e01', 'metal'))
    m.box(9.5, 4.0, 7.0, 12.0, 6.5, 9.0, M('dc7510', 'metal'))
    m.box(4.0, 9.0, 7.25, 6.0, 11.0, 8.75, M('5c5b5b'))
    m.box(3.0, 11.0, 7.0, 5.0, 12.5, 9.0, M('747474'))
    return m


# ================================================================ 其他

@item('jetpack_attachment_plate')
def jetpack_attachment_plate():
    """喷气背包附件板：黑色厚背板（左下圆角）+ 上方挂接支架。"""
    m = Model()
    m.box(3.5, 3.0, 6.5, 13.0, 11.0, 9.0, M('303030'))
    m.box(2.5, 4.5, 6.5, 3.5, 10.0, 9.0, M('303030'))
    m.box(5.0, 2.0, 6.5, 12.0, 3.0, 9.0, M('303030'))
    m.box(4.0, 3.5, 9.0, 12.5, 10.5, 9.5, M('383838'))
    m.box(9.0, 11.0, 7.0, 13.0, 14.0, 8.5, M('343434'))       # 支架
    m.box(10.0, 12.0, 8.5, 12.0, 13.0, 8.8, M('505050'))
    for (x, y) in ((5.0, 4.5), (11.0, 4.5), (5.0, 9.0)):
        m.box(x, y, 9.5, x + 0.8, y + 0.8, 9.8, M('5a5a5a'))
    return m


@item('pellet')
def pellet():
    """颗粒：一枚很小的灰色横置短圆柱。"""
    m = Model()
    m.prism('x', 5.5, 10.5, (8.0, 8.0), 2.0, 8, M('868686'), PH)
    m.prism('x', 5.0, 5.5, (8.0, 8.0), 1.5, 8, M('686868'), PH)
    m.prism('x', 10.5, 11.0, (8.0, 8.0), 1.5, 8, M('686868'), PH)
    m.box(6.5, 9.7, 7.5, 9.0, 10.1, 8.5, M('9b9b9b'))
    return m


@item('rubber')
def rubber():
    """橡胶：扁圆黑色胶块（扁球），顶部一处灰色高光。"""
    m = Model()
    lsphere(m, 8.0, 7.5, 8.0, 6.0, M('343434'), rings=5, segs=10, tex_top=M('3e3e3e'), tex_bottom=M('303030'), sy=0.8)
    m.box(6.5, 9.5, 12.6, 8.0, 10.5, 13.2, M('6a6a6a'))
    m.box(5.5, 8.5, 12.9, 6.5, 9.5, 13.5, M('484848'))
    return m


@item('scrap')
def scrap():
    """废料：扁平的褐色碎渣堆，夹杂几片翘起的灰色金属碎片。"""
    m = Model()
    mound(m, 8.0, 8.0, 3.0, 6.5, 6.8, [M('342301'), M('473603'), M('5e431f'), M('685a30')], layers=4, power=2.0)
    mound(m, 5.0, 9.5, 3.0, 3.5, 3.2, [M('473603'), M('5e431f')], layers=2)
    for (x0, y0, z0, x1, y1, z1) in ((3.0, 4.5, 11.5, 6.0, 6.0, 12.0), (9.5, 5.0, 11.0, 12.0, 6.0, 12.5),
                                     (7.0, 8.5, 10.5, 9.5, 9.5, 11.5), (11.5, 3.5, 6.0, 14.0, 5.0, 8.0)):
        m.box(x0, y0, z0, x1, y1, z1, M('6b6b6b'))
    return m


@item('scrap_box')
def scrap_box():
    """废料盒：黄色木箱——箱体 + 顶盖 + 正面压条。"""
    m = Model()
    m.box(2.5, 2.5, 3.5, 13.5, 11.5, 12.5, M('e5e562'))
    m.box(2.0, 11.5, 3.0, 14.0, 13.0, 13.0, M('f2f278'))
    m.box(2.0, 2.0, 12.5, 14.0, 3.0, 13.0, M('a5a513'))
    for x in (2.0, 13.0):
        m.box(x, 3.0, 12.5, x + 1.0, 11.5, 13.0, M('a5a513'))
    m.box(3.0, 6.5, 12.5, 13.0, 7.5, 12.9, M('c2c22b'))
    m.box(6.0, 9.0, 12.5, 8.0, 10.0, 12.8, M('99b947'))
    m.box(2.0, 13.0, 7.5, 14.0, 13.5, 8.5, M('5b5b00'))   # 盖缝
    return m


@item('tin_can')
def tin_can():
    """锡罐：立式圆罐，上下卷边 + 灰白标签环带。"""
    m = Model()
    m.prism('y', 2.0, 14.0, (8.0, 8.0), 4.4, 8, M('7c7c7c', 'metal'), PH)
    m.prism('y', 1.5, 2.5, (8.0, 8.0), 4.8, 8, M('505050'), PH)
    m.prism('y', 13.5, 14.5, (8.0, 8.0), 4.8, 8, M('505050'), PH)
    m.prism('y', 5.0, 11.0, (8.0, 8.0), 4.6, 8, M('d9d9d9'), PH)
    m.prism('y', 7.5, 8.5, (8.0, 8.0), 4.7, 8, M('989898'), PH)
    m.prism('y', 14.5, 14.8, (8.0, 8.0), 3.8, 8, M('a9a9a9'), PH)
    return m


@item('steam_turbine')
def steam_turbine():
    """蒸汽涡轮：朝南的叶轮——轮缘 + 16 片斜置叶片（交错前后） + 中心轮毂。"""
    m = Model()
    disc_z(m, 8.0, 8.0, 6.5, 9.0, 2.6, M('595c68'))
    disc_z(m, 8.0, 8.0, 9.0, 9.5, 1.4, M('2e3733'))
    for k in range(12):
        a = 2 * math.pi * k / 12
        for rr, s in ((3.6, 1.0), (5.2, 0.9), (6.6, 0.8)):
            x, y = 8.0 + rr * math.cos(a), 8.0 + rr * math.sin(a)
            z = 7.0 if k % 2 else 7.5
            m.box(x - s, y - s, z, x + s, y + s, z + 1.5, M('b9d6d9' if rr > 6 else '91a3aa', 'metal'))
    return m


@item('steam_turbine_blade')
def steam_turbine_blade():
    """蒸汽涡轮叶片：四片连续宽叶片十字辐射于轮毂，叶片前后两层错位（扭角），叶尖亮色。"""
    m = Model()
    for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        # 叶片主体：从轮毂到叶尖连续，两层错位表现扭角
        x0, x1 = (8.0, 8.0 + dx * 6.0) if dx > 0 else (8.0 + dx * 6.0, 8.0) if dx < 0 else (7.0, 9.0)
        y0, y1 = (8.0, 8.0 + dy * 6.0) if dy > 0 else (8.0 + dy * 6.0, 8.0) if dy < 0 else (7.0, 9.0)
        if dx == 0:
            m.box(5.8, y0, 7.6, 8.4, y1, 8.6, M('91a3aa', 'metal'))
            m.box(7.6, y0, 7.0, 10.2, y1, 8.0, M('6f7983', 'metal'))
        else:
            m.box(x0, 7.6, 7.6, x1, 10.2, 8.6, M('91a3aa', 'metal'))
            m.box(x0, 5.8, 7.0, x1, 8.4, 8.0, M('6f7983', 'metal'))
        tx, ty = 8.0 + dx * 6.0, 8.0 + dy * 6.0
        m.box(tx - 1.2, ty - 1.2, 7.0, tx + 1.2, ty + 1.2, 8.8, M('b9d6d9', 'metal'))  # 叶尖
    m.prism('z', 6.0, 9.5, (8.0, 8.0), 2.2, 8, M('595c68'), PH)
    m.box(7.5, 7.5, 9.5, 8.5, 8.5, 9.8, M('fafafa'))
    return m