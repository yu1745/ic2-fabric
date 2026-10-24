"""反应堆元件家族：参照 ic2 textures/item/reactor/*。

原版造型分七类：
- 圆盘热交换器（heat/component/advanced/reactor_heat_exchanger）：八边形圆盘 + 四向凸耳，配色区分等级；
- 方形散热片（heat/advanced/overclocked/reactor_heat_vent）：方框 + 凹陷格栅 + X 形撑条；元件散热片为大圆盘；
- 隔板（plating/containment/heat_plating）：厚板 + 螺栓；
- 中子反射板（neutron/thick/iridium）：方框内凸起白块阵列；
- 冷凝模块（lzh/rsh）：方框 + 彩色环；
- 冷却单元（10k/30k/60k）：深色框架内的蓝色冷却液管；
- 加热元件（heatpack）：小型电池状。
平面类元件立在 x/y 平面，厚度沿 z（正面 +Z）。
"""
import math

from core import Model, item, mat

PH = math.pi / 8

G_DARK = mat('#707070')
G_MID = mat('#969696')
G_LIGHT = mat('#aaaaaa', 'metal')
G_HI = mat('#d9d9d9')
G_WHITE = mat('#ececec')
G_INSET = mat('#686868')


def stepped_diag(m, x0, y0, x1, y1, z0, z1, tex, size=1.2):
    """像素风斜线：沿 (x0,y0)→(x1,y1) 等距放置小方块（坐标为方块中心）。"""
    n = max(2, int(round(max(abs(x1 - x0), abs(y1 - y0)) / size)) + 1)
    h = size / 2
    for i in range(n):
        t = i / (n - 1)
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        m.box(x - h, y - h, z0, x + h, y + h, z1, tex)


# ---------------------------------------------------------------- 圆盘热交换器

def exchanger(nub, center, cross=None):
    m = Model()
    m.prism('z', 6.5, 9.5, (8, 8), 5.2, 8, {'*': G_MID, 'cap': G_MID}, PH)    # 圆盘本体
    m.prism('z', 9.5, 10.2, (8, 8), 4.4, 8, {'*': G_HI, 'cap': G_LIGHT}, PH)  # 正面凸台
    m.prism('z', 5.8, 6.5, (8, 8), 4.4, 8, {'*': G_HI, 'cap': G_LIGHT}, PH)   # 背面凸台
    m.prism('z', 10.2, 10.9, (8, 8), 1.8, 8, {'*': G_DARK, 'cap': center}, PH)  # 中心毂
    for x0, y0, x1, y1 in ((7, 12.6, 9, 14.8), (7, 1.2, 9, 3.4), (12.6, 7, 14.8, 9), (1.2, 7, 3.4, 9)):
        m.box(x0, y0, 6.8, x1, y1, 9.2, nub)                                    # 四向凸耳
    if cross:
        for (ax, ay), (bx, by) in (((5.2, 5.2), (6.6, 6.6)), ((10.8, 10.8), (9.4, 9.4)),
                                   ((5.2, 10.8), (6.6, 9.4)), ((10.8, 5.2), (9.4, 6.6))):
            stepped_diag(m, ax, ay, bx, by, 10.2, 10.7, cross, 0.9)
    return m


@item('heat_exchanger')
def heat_exchanger():
    return exchanger(G_WHITE, G_LIGHT)


@item('component_heat_exchanger')
def component_heat_exchanger():
    return exchanger(mat('#f0f000'), G_LIGHT)


@item('advanced_heat_exchanger')
def advanced_heat_exchanger():
    return exchanger(mat('#6edcdc'), mat('#9bffff'), cross=mat('#89d3d3'))


@item('reactor_heat_exchanger')
def reactor_heat_exchanger():
    return exchanger(G_WHITE, mat('#8b5b2b'))


# ---------------------------------------------------------------- 方形散热片

def vent(bar, hub=None):
    m = Model()
    m.box(2, 2, 6.5, 14, 14, 9.5, G_MID)              # 外框板
    m.box(2, 2, 9.5, 14, 3.2, 10.3, G_LIGHT)          # 框沿（四边凸起）
    m.box(2, 12.8, 9.5, 14, 14, 10.3, G_LIGHT)
    m.box(2, 3.2, 9.5, 3.2, 12.8, 10.3, G_LIGHT)
    m.box(12.8, 3.2, 9.5, 14, 12.8, 10.3, G_LIGHT)
    for k in range(4):                                 # 凹陷格栅：横向散热鳍
        y = 4.2 + k * 2.2
        m.box(3.6, y, 9.0, 12.4, y + 1.0, 9.8, G_INSET)
    stepped_diag(m, 4.0, 4.0, 12.0, 12.0, 9.8, 10.6, bar)  # X 撑条
    stepped_diag(m, 4.0, 12.0, 12.0, 4.0, 9.8, 10.6, bar)
    if hub:
        m.box(6.8, 6.8, 10.4, 9.2, 9.2, 11.0, hub)
    m.box(3.4, 3.4, 5.9, 12.6, 12.6, 6.5, G_DARK)     # 背板
    return m


@item('heat_vent')
def heat_vent():
    return vent(mat('#cdcdcd'))


@item('advanced_heat_vent')
def advanced_heat_vent():
    return vent(mat('#34dada'), mat('#3bb7b7'))


@item('overclocked_heat_vent')
def overclocked_heat_vent():
    return vent(mat('#b3b13b'), mat('#e8e45a'))


@item('reactor_heat_vent')
def reactor_heat_vent():
    return vent(mat('#cdcdcd'), mat('#7c654d'))


@item('component_heat_vent')
def component_heat_vent():
    # 原图：大圆盘 + 四向白色凸耳 + 中央格栅
    m = Model()
    m.prism('z', 6.5, 9.5, (8, 8), 6.0, 8, {'*': mat('#a7a7a7'), 'cap': mat('#a7a7a7')}, PH)
    m.prism('z', 9.5, 10.2, (8, 8), 5.0, 8, {'*': G_HI, 'cap': mat('#c1c1c1')}, PH)
    for k in range(3):                                   # 中央格栅
        y = 5.6 + k * 1.8
        m.box(5.4, y, 10.2, 10.6, y + 0.9, 10.8, mat('#787878'))
    m.box(7.5, 5.2, 10.2, 8.5, 10.8, 10.9, mat('#787878'))
    for x0, y0, x1, y1 in ((6.6, 13.4, 9.4, 15.4), (6.6, 0.6, 9.4, 2.6), (13.4, 6.6, 15.4, 9.4), (0.6, 6.6, 2.6, 9.4)):
        m.box(x0, y0, 6.8, x1, y1, 9.2, G_WHITE)
    return m


# ---------------------------------------------------------------- 隔板

def plating(face, rim, dark, bolts=8):
    m = Model()
    m.box(1.5, 1.5, 6.6, 14.5, 14.5, 9.4, face)
    m.box(1.5, 1.5, 9.4, 14.5, 2.4, 9.8, rim)          # 上下左右倒角边
    m.box(1.5, 13.6, 9.4, 14.5, 14.5, 9.8, rim)
    m.box(1.5, 2.4, 9.4, 2.4, 13.6, 9.8, rim)
    m.box(13.6, 2.4, 9.4, 14.5, 13.6, 9.8, rim)
    pts = [(4, 4), (12, 4), (4, 12), (12, 12)] + ([(8, 4), (8, 12), (4, 8), (12, 8)] if bolts == 8 else [])
    for x, y in pts:                                     # 螺栓
        m.box(x - 0.6, y - 0.6, 9.4, x + 0.6, y + 0.6, 10.0, dark)
    return m


@item('reactor_plating')
def reactor_plating():
    return plating(G_MID, G_LIGHT, mat('#707070'))


@item('containment_reactor_plating')
def containment_reactor_plating():
    m = plating(mat('#868574'), mat('#989784'), mat('#5b5b4f'))
    m.box(2.4, 2.4, 5.8, 13.6, 13.6, 6.6, mat('#646357'))  # 背面加厚的密封层
    return m


@item('reactor_heat_plating')
def reactor_heat_plating():
    m = plating(mat('#c18548'), mat('#cd9751'), mat('#9f5c28'), bolts=4)
    # 原图的放射状散热纹：铜色凸筋
    rib = mat('#aa6336')
    stepped_diag(m, 4.5, 11.5, 7.2, 8.8, 9.4, 9.9, rib, 0.9)
    stepped_diag(m, 11.5, 11.5, 8.8, 8.8, 9.4, 9.9, rib, 0.9)
    stepped_diag(m, 4.5, 4.5, 7.2, 7.2, 9.4, 9.9, rib, 0.9)
    stepped_diag(m, 11.5, 4.5, 8.8, 7.2, 9.4, 9.9, rib, 0.9)
    m.box(7.2, 7.2, 9.4, 8.8, 8.8, 10.1, mat('#b4713d'))
    return m


# ---------------------------------------------------------------- 中子反射板

def reflector(n, block, frame_depth=0.6, frame=G_LIGHT):
    m = Model()
    m.box(1.5, 1.5, 6.8, 14.5, 14.5, 9.2, frame)
    m.box(2.5, 2.5, 9.2, 13.5, 13.5, 9.5, mat('#898989'))   # 凹槽底
    cell = 11.0 / n
    for i in range(n):
        for j in range(n):
            x0, y0 = 2.5 + i * cell + 0.35, 2.5 + j * cell + 0.35
            m.box(x0, y0, 9.5, x0 + cell - 0.7, y0 + cell - 0.7, 9.5 + frame_depth + 0.6, block)
    return m


@item('neutron_reflector')
def neutron_reflector():
    return reflector(3, mat('#efefef'))


@item('thick_neutron_reflector')
def thick_neutron_reflector():
    m = reflector(3, mat('#efefef'), frame_depth=1.0, frame=mat('#c1c1c1'))
    m.box(2.0, 2.0, 5.8, 14.0, 14.0, 6.8, mat('#777777'))   # 加厚背板
    return m


@item('iridium_neutron_reflector')
def iridium_neutron_reflector():
    m = reflector(4, mat('#efefef'), frame_depth=1.2, frame=mat('#c7c7c7'))
    m.box(1.0, 1.0, 5.6, 15.0, 15.0, 6.8, mat('#777777'))
    return m


# ---------------------------------------------------------------- 冷凝模块

def condensator(light, dark):
    m = Model()
    m.box(2, 2, 6.6, 14, 14, 9.4, G_MID)
    m.box(2, 2, 9.4, 14, 3, 9.9, G_LIGHT)
    m.box(2, 13, 9.4, 14, 14, 9.9, G_LIGHT)
    m.box(2, 3, 9.4, 3, 13, 9.9, G_LIGHT)
    m.box(13, 3, 9.4, 14, 13, 9.9, G_LIGHT)
    m.prism('z', 9.4, 10.6, (8, 8), 4.2, 8, {'*': dark, 'cap': light}, PH)  # 彩色冷凝环
    m.prism('z', 9.4, 10.9, (8, 8), 2.4, 8, {'*': G_DARK, 'cap': G_INSET}, PH)
    m.prism('z', 10.9, 11.3, (8, 8), 1.0, 8, {'*': dark, 'cap': light}, PH)  # 中心芯
    return m


@item('lzh_condensator')
def lzh_condensator():
    return condensator(mat('#2d2dff'), mat('#0000d0'))


@item('rsh_condensator')
def rsh_condensator():
    return condensator(mat('#ff2d2d'), mat('#d00000'))


# ---------------------------------------------------------------- 冷却单元

COOL = mat('#4279ff', 'glass')
COOL_DARK = mat('#2f5ad0')
CELL_BODY = mat('#505050')
CELL_DARK = mat('#323232')
CELL_CAP = mat('#d9d9d9')


def coolant_vial(m, cx, cz, y0, y1, r=1.6):
    """单根冷却液管：深色端座 + 蓝色玻璃管 + 浅色端帽。"""
    m.prism('y', y0, y0 + 1.0, (cx, cz), r * 1.15, 8, CELL_DARK, PH)
    m.prism('y', y0 + 1.0, y1 - 1.0, (cx, cz), r, 8, {'*': COOL, 'cap': COOL_DARK}, PH)
    m.prism('y', y1 - 1.0, y1, (cx, cz), r * 1.15, 8, CELL_CAP, PH)


@item('reactor_coolant_cell')
def reactor_coolant_cell():
    m = Model()
    m.box(4.5, 1.5, 5.5, 11.5, 3.0, 10.5, CELL_BODY)   # 底座
    m.box(4.5, 13.0, 5.5, 11.5, 14.5, 10.5, CELL_BODY)  # 顶座
    for x in (4.5, 10.5):                               # 两侧立板
        m.box(x, 3.0, 6.0, x + 1.0, 13.0, 10.0, CELL_DARK)
    coolant_vial(m, 8, 8, 3.0, 13.0, 2.0)
    return m


@item('triple_reactor_coolant_cell')
def triple_reactor_coolant_cell():
    m = Model()
    m.box(1.5, 1.5, 5.5, 14.5, 3.0, 10.5, mat('#c4c4c4', 'metal'))
    m.box(1.5, 13.0, 5.5, 14.5, 14.5, 10.5, mat('#c4c4c4', 'metal'))
    for x in (1.5, 13.5):
        m.box(x, 3.0, 6.0, x + 1.0, 13.0, 10.0, mat('#a6a6a6'))
    for cx in (4.6, 8.0, 11.4):
        coolant_vial(m, cx, 8, 3.0, 13.0, 1.5)
    return m


@item('sextuple_reactor_coolant_cell')
def sextuple_reactor_coolant_cell():
    m = Model()
    m.box(1.5, 0.8, 4.0, 14.5, 2.2, 12.0, mat('#c4c4c4', 'metal'))
    m.box(1.5, 13.8, 4.0, 14.5, 15.2, 12.0, mat('#c4c4c4', 'metal'))
    m.box(1.5, 7.4, 4.0, 14.5, 8.6, 12.0, mat('#a6a6a6'))  # 中隔板：上下两层各三根
    for x in (1.5, 13.5):
        m.box(x, 2.2, 4.5, x + 1.0, 13.8, 11.5, mat('#a6a6a6'))
    for cx in (4.6, 8.0, 11.4):
        for cz in (6.0, 10.0):
            coolant_vial(m, cx, cz, 2.2, 7.4, 1.3)
            coolant_vial(m, cx, cz, 8.6, 13.8, 1.3)
    return m


# ---------------------------------------------------------------- 加热元件

@item('heatpack')
def heatpack():
    m = Model()
    m.box(5.5, 3.0, 6.5, 10.5, 13.0, 9.5, CELL_BODY)
    m.box(5.5, 13.0, 6.5, 10.5, 14.0, 9.5, CELL_CAP)     # 顶帽
    m.box(6.5, 14.0, 7.2, 9.5, 14.8, 8.8, mat('#979797'))  # 端子
    m.box(5.5, 2.2, 6.5, 10.5, 3.0, 9.5, CELL_DARK)
    m.box(6.5, 5.0, 9.5, 9.5, 11.0, 10.1, mat('#d91919', 'glow'))  # 红色加热窗
    m.box(6.2, 4.6, 9.4, 9.8, 5.0, 10.1, mat('#979797'))
    m.box(6.2, 11.0, 9.4, 9.8, 11.4, 10.1, mat('#979797'))
    return m
