"""燃料棒家族：参照 ic2 reactor/fuel_rod/*、resource/nuclear/depleted_*、crafting/fuel_rod。

两种原版造型：
- 经典棒（铀、枯竭铀、枯竭 MOX、空燃料棒）：白色顶帽 + 灰色外管 + 正面彩色窗口 + 深色端头；
  双联为两根并排、中段白色夹箍；四联为 2×2 方阵。
- 灰管棒（MOX、锂、近衰变铀）：灰色外管，管身中段渐变芯料，端头细；双联 MOX 两根之间一根连杆。
"""
import math

from core import Model, item, mat

PH = math.pi / 8
DARK = mat('#353535')
TUBE = mat('#b5b5b5', 'metal')
TUBE_MID = mat('#727272')
CAP = mat('#e7e7e7')
GRAY_TUBE = mat('#7c7c7c', 'metal')
GRAY_DARK = mat('#4c4c4c')


def rails(m, cx, cz, rr, half, y0, y1, tex):
    """八根立柱，位于八棱柱管身的八条棱上（芯料从相邻立柱之间的八面窗口露出）。"""
    for k in range(8):
        a = PH + k * math.pi / 4
        x, z = cx + rr * math.cos(a), cz + rr * math.sin(a)
        m.box(x - half, y0, z - half, x + half, y1, z + half, tex)


def classic_rod(m, cx, cz, core, core_dark, r=2.2, y0=1.0, y1=15.0):
    """经典棒：深色端头 / 灰管 / 正面窗口（芯料）/ 白色顶帽。"""
    m.prism('y', y0, y0 + 1.0, (cx, cz), r * 0.55, 8, DARK, PH)                 # 底端头
    m.prism('y', y0 + 1.0, y0 + 2.0, (cx, cz), r, 8, TUBE_MID, PH)            # 底箍
    # 管身 = 芯料 + 八棱立柱
    m.prism('y', y0 + 2.0, y1 - 3.5, (cx, cz), r * 0.82, 8, {'*': core, 'cap': core_dark}, PH)
    rails(m, cx, cz, r * 0.9, 0.2, y0 + 2.0, y1 - 3.5, TUBE)
    m.prism('y', y1 - 3.5, y1 - 1.0, (cx, cz), r, 8, CAP, PH)                  # 白色顶帽
    m.prism('y', y1 - 1.0, y1, (cx, cz), r * 0.55, 8, DARK, PH)                # 顶端头


def clamp_band(m, x0, x1, z0, z1, y0, y1):
    """多联棒的中段夹箍：白色环带。"""
    m.box(x0, y0, z0, x1, y1, z1, CAP)


def gray_rod(m, cx, cz, top, mid, bottom, r=2.0, y0=1.0, y1=15.0):
    """灰管棒：灰色外管，芯料自上而下分三段渐变，两端细端头。"""
    m.prism('y', y0, y0 + 1.2, (cx, cz), r * 0.6, 8, GRAY_DARK, PH)
    m.prism('y', y0 + 1.2, y0 + 2.2, (cx, cz), r * 0.95, 8, GRAY_TUBE, PH)   # 下箍
    m.prism('y', y1 - 2.2, y1 - 1.2, (cx, cz), r * 0.95, 8, GRAY_TUBE, PH)   # 上箍
    h = (y1 - y0 - 4.4) / 3
    ys = y1 - 2.2
    for c in (top, mid, bottom):  # 芯料三段，从立柱之间露出
        m.prism('y', ys - h, ys, (cx, cz), r * 0.82, 8, {'*': c, 'cap': c}, PH)
        ys -= h
    rails(m, cx, cz, r * 0.88, 0.2, y0 + 2.2, y1 - 2.2, GRAY_TUBE)
    m.prism('y', y1 - 1.2, y1, (cx, cz), r * 0.6, 8, GRAY_DARK, PH)


# ---------------------------------------------------------------- 铀

U = (mat('#52950f'), mat('#437a0c'))


@item('uranium_fuel_rod')
def uranium_fuel_rod():
    m = Model()
    classic_rod(m, 8, 8, *U)
    return m


@item('dual_uranium_fuel_rod')
def dual_uranium_fuel_rod():
    m = Model()
    for cx in (5.4, 10.6):
        classic_rod(m, cx, 8, *U, r=2.1)
    clamp_band(m, 3.0, 13.0, 6.4, 9.6, 6.8, 7.8)
    return m


@item('quad_uranium_fuel_rod')
def quad_uranium_fuel_rod():
    m = Model()
    for cx in (5.4, 10.6):
        for cz in (5.4, 10.6):
            classic_rod(m, cx, cz, *U, r=2.1)
    clamp_band(m, 3.2, 12.8, 3.2, 12.8, 6.8, 7.8)
    return m


# ---------------------------------------------------------------- 枯竭铀（青绿窗口）

DU = (mat('#418469'), mat('#356c56'))


@item('depleted_uranium_fuel_rod')
def depleted_uranium_fuel_rod():
    m = Model()
    classic_rod(m, 8, 8, *DU)
    return m


@item('depleted_dual_uranium_fuel_rod')
def depleted_dual_uranium_fuel_rod():
    m = Model()
    for cx in (5.4, 10.6):
        classic_rod(m, cx, 8, *DU, r=2.1)
    clamp_band(m, 3.0, 13.0, 6.4, 9.6, 6.8, 7.8)
    return m


@item('depleted_quad_uranium_fuel_rod')
def depleted_quad_uranium_fuel_rod():
    m = Model()
    for cx in (5.4, 10.6):
        for cz in (5.4, 10.6):
            classic_rod(m, cx, cz, *DU, r=2.1)
    clamp_band(m, 3.2, 12.8, 3.2, 12.8, 6.8, 7.8)
    return m


# ---------------------------------------------------------------- 枯竭 MOX（浅灰绿窗口）

DM = (mat('#d4decb'), mat('#9da595'))


@item('depleted_mox_fuel_rod')
def depleted_mox_fuel_rod():
    m = Model()
    classic_rod(m, 8, 8, *DM)
    return m


@item('depleted_dual_mox_fuel_rod')
def depleted_dual_mox_fuel_rod():
    m = Model()
    for cx in (5.4, 10.6):
        classic_rod(m, cx, 8, *DM, r=2.1)
    clamp_band(m, 3.0, 13.0, 6.4, 9.6, 6.8, 7.8)
    return m


@item('depleted_quad_mox_fuel_rod')
def depleted_quad_mox_fuel_rod():
    m = Model()
    for cx in (5.4, 10.6):
        for cz in (5.4, 10.6):
            classic_rod(m, cx, cz, *DM, r=2.1)
    clamp_band(m, 3.2, 12.8, 3.2, 12.8, 6.8, 7.8)
    return m


# ---------------------------------------------------------------- 空燃料棒（窗口内为空腔）

@item('fuel_rod')
def fuel_rod():
    m = Model()
    classic_rod(m, 8, 8, mat('#2a2a2a'), mat('#1e1e1e'))
    return m


# ---------------------------------------------------------------- MOX（灰管 + 绿色渐变芯）

MOX = (mat('#82ac58'), mat('#9eae8e'), mat('#759a50'))


@item('mox_fuel_rod')
def mox_fuel_rod():
    m = Model()
    gray_rod(m, 8, 8, *MOX)
    return m


@item('dual_mox_fuel_rod')
def dual_mox_fuel_rod():
    m = Model()
    for cx in (4.6, 11.4):
        gray_rod(m, cx, 8, *MOX, r=1.9)
    m.box(6.2, 9.0, 7.4, 9.8, 10.2, 8.6, GRAY_TUBE)  # 连杆
    return m


@item('quad_mox_fuel_rod')
def quad_mox_fuel_rod():
    m = Model()
    for cx in (4.6, 11.4):
        for cz in (4.6, 11.4):
            gray_rod(m, cx, cz, *MOX, r=1.9)
    m.box(6.2, 9.0, 4.0, 9.8, 10.2, 12.0, GRAY_TUBE)   # 十字连杆
    m.box(4.0, 9.0, 7.4, 12.0, 10.2, 8.6, GRAY_TUBE)
    return m


# ---------------------------------------------------------------- 锂（灰管 + 冰蓝芯）

@item('lithium_fuel_rod')
def lithium_fuel_rod():
    m = Model()
    gray_rod(m, 8, 8, mat('#edf2f3', 'glass'), mat('#d3e2e3', 'glass'), mat('#b6d0d3', 'glass'))
    return m


# ---------------------------------------------------------------- 近衰变铀（上段暗灰，下段青绿）

@item('depleted_isotope_fuel_rod')
def depleted_isotope_fuel_rod():
    m = Model()
    gray_rod(m, 8, 8, mat('#535353'), mat('#55615b'), mat('#5c8973'))
    return m
