"""电池与能量水晶家族：参照 ic2 textures/item/battery/*。

电量档位（overrides 子模型 battery/<名>_<1..4>，无线版 _<0..4>）逐档建模：
- 充电电池：银色八棱电池，正面竖窗内红色电量格逐档点亮；
- 高级充电电池：铜色方盒 + 两个接线柱，窗口内暗紫色电量自下而上填满（空时为原图的棕色木纹底）；
- 能量水晶：红色宝石，随电量由深红渐变为粉白；
- 蓝波顿水晶：蓝色宝石 + 正面指示灯（红→橙→黄→黄绿→绿）；
- 无线版（原图 charging_*）：左侧电池管显示电量液位，右侧小发射器（双天线 + 彩色屏）；
- 一次性电池：黑色小电池 + 绿色指示灯。
"""
import math

from core import Model, item, mat

PH = math.pi / 8


def lerp_hex(a, b, t):
    ca = [int(a[i:i + 2], 16) for i in (1, 3, 5)]
    cb = [int(b[i:i + 2], 16) for i in (1, 3, 5)]
    return '#' + ''.join('%02x' % round(x + (y - x) * t) for x, y in zip(ca, cb))


# ---------------------------------------------------------------- 充电电池

SILVER = mat('#d9d9d9', 'metal')
SILVER_DARK = mat('#979797')
BAT_DARK = mat('#505050')
BAT_BLACK = mat('#353535')


def re_battery(level):
    m = Model()
    m.prism('y', 1.5, 2.5, (8, 8), 2.6, 8, BAT_DARK, PH)                 # 底端
    m.prism('y', 2.5, 12.5, (8, 8), 3.1, 8, SILVER, PH)                  # 筒身
    m.prism('y', 12.5, 13.5, (8, 8), 2.6, 8, SILVER_DARK, PH)            # 肩
    m.prism('y', 13.5, 14.8, (8, 8), 1.3, 8, mat('#ffffff'), PH)         # 正极帽
    m.box(6.9, 4.0, 10.6, 9.1, 11.6, 11.3, BAT_BLACK)                    # 电量窗（嵌在筒身正面）
    for k in range(4):
        y = 4.6 + k * 1.75
        on = k < level
        m.box(7.3, y, 11.3 if on else 11.1, 8.7, y + 1.2, 11.6,
              mat('#e02818', 'glow') if on else mat('#5a1410'))
    return m


@item('re_battery')
def re_battery_0():
    return re_battery(0)


@item('battery/re_battery_1')
def re_battery_1():
    return re_battery(1)


@item('battery/re_battery_2')
def re_battery_2():
    return re_battery(2)


@item('battery/re_battery_3')
def re_battery_3():
    return re_battery(3)


@item('battery/re_battery_4')
def re_battery_4():
    return re_battery(4)


# ---------------------------------------------------------------- 高级充电电池

COPPER = mat('#cf8825', 'metal')
COPPER_DARK = mat('#9a3300')
COPPER_HI = mat('#ffa34e')


def advanced_re_battery(level):
    m = Model()
    m.box(3.0, 1.5, 5.0, 13.0, 12.5, 11.0, COPPER)                        # 盒体
    m.box(3.0, 1.5, 11.0, 13.0, 2.3, 11.4, COPPER_DARK)                   # 正面下沿
    m.box(3.0, 11.7, 11.0, 13.0, 12.5, 11.4, COPPER_HI)                   # 正面上沿高光
    for x in (4.0, 10.0):                                                 # 接线柱
        m.box(x, 12.5, 6.8, x + 2.0, 14.5, 9.2, COPPER_DARK)
        m.box(x + 0.4, 14.5, 7.2, x + 1.6, 15.0, 8.8, COPPER_HI)
    frame = mat('#5c1b00')                                                # 窗框（四边，中间留窗）
    m.box(4.8, 3.2, 10.6, 11.2, 4.0, 11.6, frame)
    m.box(4.8, 10.0, 10.6, 11.2, 10.8, 11.6, frame)
    m.box(4.8, 4.0, 10.6, 5.6, 10.0, 11.6, frame)
    m.box(10.4, 4.0, 10.6, 11.2, 10.0, 11.6, frame)
    # 窗内：自下而上的暗紫电量，其余为空时的棕色底（带横纹）
    top = 4.0 + 6.0 * level / 4
    if level > 0:
        m.box(5.6, 4.0, 11.0, 10.4, top, 11.5, mat('#2b1531', 'glow') if level == 4 else mat('#2b1531'))
    if level < 4:
        for k in range(4):
            y = 4.0 + k * 1.5
            if y >= top:
                m.box(5.6, y, 11.0, 10.4, min(10.0, y + 1.5), 11.5, mat('#966f4d') if k % 2 == 0 else mat('#7e5d41'))
    return m


@item('advanced_re_battery')
def advanced_re_battery_0():
    return advanced_re_battery(0)


@item('battery/advanced_re_battery_1')
def advanced_re_battery_1():
    return advanced_re_battery(1)


@item('battery/advanced_re_battery_2')
def advanced_re_battery_2():
    return advanced_re_battery(2)


@item('battery/advanced_re_battery_3')
def advanced_re_battery_3():
    return advanced_re_battery(3)


@item('battery/advanced_re_battery_4')
def advanced_re_battery_4():
    return advanced_re_battery(4)


# ---------------------------------------------------------------- 宝石（能量水晶 / 蓝波顿水晶）

def facet(m, pts, tex, center):
    """平面刻面：按几何中心把顶点顺序纠正为朝外。"""
    (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = pts[0], pts[1], pts[2]
    n = ((y1 - y0) * (z2 - z0) - (z1 - z0) * (y2 - y0),
         (z1 - z0) * (x2 - x0) - (x1 - x0) * (z2 - z0),
         (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0))
    c = [sum(p[i] for p in pts) / len(pts) - center[i] for i in range(3)]
    if sum(n[i] * c[i] for i in range(3)) < 0:
        pts = pts[::-1]
    m.poly(pts, tex)


def gem(m, crown, girdle, pavilion, table, cy=8.0, r=5.4):
    """圆钻型宝石：台面 + 八个冠部刻面 + 腰棱 + 八个亭部刻面收尖。"""
    def ring(rad, y, ph=PH):
        return [(8 + rad * math.cos(ph + k * math.pi / 4), y, 8 + rad * math.sin(ph + k * math.pi / 4)) for k in range(8)]
    c = (8, cy, 8)
    top, crown_lo, gir_lo = cy + 5.0, cy + 1.6, cy + 0.6
    t, g1, g0 = ring(r * 0.48, top), ring(r, crown_lo), ring(r, gir_lo)
    tip = (8, cy - 6.0, 8)
    facet(m, t, table, c)                                       # 台面
    for k in range(8):
        j = (k + 1) % 8
        facet(m, [t[k], t[j], g1[j], g1[k]], crown, c)          # 冠部
        facet(m, [g1[k], g1[j], g0[j], g0[k]], girdle, c)       # 腰棱
        facet(m, [g0[k], g0[j], tip], pavilion, c)              # 亭部


EC = [('#dd001a', '#a20025', '#ff4050', '#ff7e7e'),   # 能量水晶 0 档（冠/腰亭/台面/满电台面）
      ('#ff3636', '#ba0025', '#ff7676', '#ffd0d0'),   # 2 档
      ('#ff89a6', '#d75d7a', '#ffc8d8', '#ffffff')]   # 4 档


def energy_crystal(level):
    t = level / 4
    if t <= 0.5:
        cols = [lerp_hex(a, b, t * 2) for a, b in zip(EC[0], EC[1])]
    else:
        cols = [lerp_hex(a, b, (t - 0.5) * 2) for a, b in zip(EC[1], EC[2])]
    face, side, top, hi = cols
    m = Model()
    gem(m, mat(face, 'glass'), mat(side), mat(side), mat(hi if level == 4 else top))
    return m


@item('energy_crystal')
def energy_crystal_0():
    return energy_crystal(0)


@item('battery/energy_crystal_1')
def energy_crystal_1():
    return energy_crystal(1)


@item('battery/energy_crystal_2')
def energy_crystal_2():
    return energy_crystal(2)


@item('battery/energy_crystal_3')
def energy_crystal_3():
    return energy_crystal(3)


@item('battery/energy_crystal_4')
def energy_crystal_4():
    return energy_crystal(4)


LAPO_LIGHT = ['#e0201a', '#ff8a1a', '#f0e020', '#a8f020', '#30e030']


def lapotron_crystal(level):
    m = Model()
    gem(m, mat('#0055de', 'glass'), mat('#001cad'), mat('#0022ca'), mat('#3a9eff'))
    # 正面电路指示片：灰色底座 + 档位指示灯
    m.box(6.2, 8.1, 12.9, 9.8, 9.9, 13.5, mat('#777777'))                # 腰部正面的电路片
    m.box(7.0, 8.5, 13.5, 9.0, 9.5, 13.9, mat(LAPO_LIGHT[level], 'glow'))
    return m


@item('lapotron_crystal')
def lapotron_crystal_0():
    return lapotron_crystal(0)


@item('battery/lapotron_crystal_1')
def lapotron_crystal_1():
    return lapotron_crystal(1)


@item('battery/lapotron_crystal_2')
def lapotron_crystal_2():
    return lapotron_crystal(2)


@item('battery/lapotron_crystal_3')
def lapotron_crystal_3():
    return lapotron_crystal(3)


@item('battery/lapotron_crystal_4')
def lapotron_crystal_4():
    return lapotron_crystal(4)


# ---------------------------------------------------------------- 无线版（电池管 + 发射器）

def wireless(level, fill, fill_dark):
    m = Model()
    # 左：电池管（深色框 + 玻璃窗内液位）
    m.box(1.8, 1.5, 5.6, 7.2, 2.5, 10.4, mat('#393939'))
    m.box(1.8, 13.0, 5.6, 7.2, 14.0, 10.4, mat('#d9d9d9'))
    m.box(3.3, 14.0, 7.0, 5.7, 14.8, 9.0, mat('#8a8a8a'))
    for x in (1.8, 6.4):
        m.box(x, 2.5, 6.2, x + 0.8, 13.0, 9.8, mat('#686868'))
    m.box(2.6, 2.5, 6.4, 6.4, 13.0, 9.6, mat('#262626'))                  # 空腔
    if level > 0:
        top = 2.5 + 10.5 * level / 4
        m.box(2.7, 2.5, 6.3, 6.3, top, 9.7, mat(fill, 'glass'))
        m.box(2.7, top - 0.4, 6.25, 6.3, top, 9.75, mat(fill_dark))
    # 右：发射器（盒体 + 双天线 + 彩色屏）
    m.box(8.4, 2.5, 6.0, 14.4, 9.5, 10.0, mat('#787878'))
    m.box(8.4, 9.5, 6.0, 14.4, 10.0, 10.0, mat('#d9d9d9'))
    for x in (9.4, 12.6):
        m.box(x, 10.0, 7.6, x + 0.8, 13.6, 8.4, mat('#8a8a8a'))
        m.box(x - 0.1, 13.6, 7.5, x + 0.9, 14.2, 8.5, mat('#ffffff'))
    m.box(9.2, 4.2, 10.0, 13.6, 7.6, 10.4, mat('#393939'))                # 屏框
    m.box(9.7, 4.7, 10.4, 13.1, 7.1, 10.7, mat(fill, 'glow'))              # 屏
    return m


WIRELESS = {
    're_battery_wireless': ('#50b425', '#2d970e'),
    'advanced_re_battery_wireless': ('#d7d73d', '#bebe2c'),
    'energy_crystal_wireless': ('#c00000', '#880000'),
    'lapotron_crystal_wireless': ('#1020d0', '#00009a'),
}


for _name in WIRELESS:
    # 基础模型（物品本体）取 0 档外观；overrides 子模型 _0.._4 各自一档
    _fill, _dark = WIRELESS[_name]
    item(_name)(lambda f=_fill, d=_dark: wireless(0, f, d))
    for _lv in range(5):
        item(f'battery/{_name}_{_lv}')(lambda lv=_lv, f=_fill, d=_dark: wireless(lv, f, d))


# ---------------------------------------------------------------- 一次性电池

@item('single_use_battery')
def single_use_battery():
    m = Model()
    m.box(5.0, 2.5, 6.0, 11.0, 12.5, 10.0, mat('#363636'))
    m.box(5.0, 12.5, 6.0, 11.0, 13.3, 10.0, mat('#545454'))
    m.box(6.8, 13.3, 7.2, 9.2, 14.3, 8.8, mat('#414141'))
    m.box(5.0, 1.7, 6.0, 11.0, 2.5, 10.0, mat('#1d1d1d'))
    m.box(7.2, 6.0, 10.0, 8.8, 9.4, 10.5, mat('#2fff00', 'glow'))          # 绿色指示灯
    return m
