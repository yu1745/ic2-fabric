"""item 目录零散物品：硬币、桶装流体、空燃料棒、调试棒、锡罐、碘片、传送卷轴、门、告示牌。

参照：ic2 item/{coin,filled_tin_can,iodine_tablet,reinforced_door,rubber_door,rubber_sign}；
压缩空气桶/蒸馏水桶原为原版水桶贴图；能量调试棒原为原版木棍；传送卷轴原为原版地图（绑定态为已填充地图）；
空燃料棒参照 fuel_rods.classic_rod 造型保持一致。
"""
import math

from core import Model, item, mat, obox
from b_parts import PH, disc_z
from fuel_rods import classic_rod


def M(c, s='flat'):
    return mat(c, s)


# ---------------------------------------------------------------- 硬币

@item('coin')
def coin():
    """工业币：朝南八棱厚币，外圈凸缘 + 内凹币面 + 凸起的 "I C" 字样。"""
    m = Model()
    disc_z(m, 8.0, 8.0, 6.5, 9.0, 6.6, M('a8a8a8', 'metal'))
    disc_z(m, 8.0, 8.0, 9.0, 9.5, 6.6, M('d8d8d8', 'metal'))
    disc_z(m, 8.0, 8.0, 9.5, 9.6, 5.2, M('c0c0c0'))
    m.box(4.5, 5.0, 9.5, 5.5, 11.0, 10.2, M('ffffff'))           # I
    m.box(4.0, 10.5, 9.5, 6.0, 11.0, 10.2, M('ffffff'))
    m.box(4.0, 5.0, 9.5, 6.0, 5.5, 10.2, M('ffffff'))
    m.box(7.5, 5.0, 9.5, 8.5, 11.0, 10.2, M('ffffff'))           # C
    m.box(8.5, 10.0, 9.5, 11.5, 11.0, 10.2, M('ffffff'))
    m.box(8.5, 5.0, 9.5, 11.5, 6.0, 10.2, M('ffffff'))
    m.box(3.0, 3.0, 9.0, 5.0, 4.0, 9.4, M('727272'))             # 外缘暗角
    return m


# ---------------------------------------------------------------- 桶装流体

def bucket(m, fluid, fluid_hi, fluid_style='glass'):
    """铁桶：下窄上宽的八棱桶身（三段放大）+ 桶口卷边 + 液面 + 提梁。"""
    m.prism('y', 2.0, 3.0, (8.0, 8.0), 4.4, 8, M('727272'), PH)
    m.prism('y', 3.0, 7.0, (8.0, 8.0), 4.9, 8, M('a8a8a8', 'metal'), PH)
    m.prism('y', 7.0, 11.0, (8.0, 8.0), 5.4, 8, M('c0c0c0', 'metal'), PH)
    m.prism('y', 11.0, 12.0, (8.0, 8.0), 6.0, 8, M('d8d8d8'), PH)
    m.prism('y', 12.0, 12.3, (8.0, 8.0), 5.0, 8, M(fluid, fluid_style), PH)
    m.box(6.0, 12.3, 6.0, 8.0, 12.5, 8.0, M(fluid_hi, fluid_style))
    m.prism('y', 6.5, 7.0, (8.0, 8.0), 5.0, 8, M('969696'), PH)
    # 提梁：两侧耳 + 顶部横梁
    m.box(1.8, 11.0, 7.5, 2.6, 14.5, 8.5, M('353535'))
    m.box(13.4, 11.0, 7.5, 14.2, 14.5, 8.5, M('353535'))
    m.box(1.8, 14.5, 7.5, 14.2, 15.3, 8.5, M('353535'))


@item('compressed_air_bucket')
def compressed_air_bucket():
    m = Model()
    bucket(m, 'dfefff', 'ffffff')
    return m


@item('distilled_water_bucket')
def distilled_water_bucket():
    m = Model()
    bucket(m, '6fb4ff', 'c8e6ff')
    return m


# ---------------------------------------------------------------- 空燃料棒

@item('empty_fuel_rod')
def empty_fuel_rod():
    """空燃料棒：与燃料棒家族同款经典棒，窗口内为浅灰蓝空腔（ic2_120 贴图的灰蓝玻璃质感）。"""
    m = Model()
    classic_rod(m, 8, 8, M('6a7281', 'glass'), M('41444c'))
    return m


# ---------------------------------------------------------------- 调试棒

@item('energy_debug_stick')
def energy_debug_stick():
    """能量调试棒：原版木棍般左下→右上斜放的杆身 + 铜箍 + 顶端发光能量晶体。"""
    m = Model()
    a = math.pi / 4
    c, s_ = math.cos(a), math.sin(a)
    P = lambda t: (8.0 + t * c, 8.0 + t * s_)  # noqa: E731
    x, y = P(-2.0)
    obox(m, x, y, 12.0, 1.8, a, 7.1, 8.9, M('684e1e'), M('493615'))
    x, y = P(-2.0)
    obox(m, x - 0.3, y + 0.3, 10.0, 0.5, a, 8.9, 9.1, M('896727'))
    x, y = P(4.4)
    obox(m, x, y, 1.0, 2.8, a, 6.6, 9.4, M('bc7945', 'metal'))
    x, y = P(6.0)
    obox(m, x, y, 2.4, 3.0, a, 6.5, 9.5, M('ffcc33', 'glow'))
    x, y = P(7.7)
    obox(m, x, y, 1.2, 1.8, a, 7.1, 8.9, M('fff2a0', 'glow'))
    return m


# ---------------------------------------------------------------- 锡罐（装满）

@item('filled_tin_can')
def filled_tin_can():
    """装满的锡罐：同 tin_can 罐体，顶部露出橙色食物。"""
    m = Model()
    m.prism('y', 2.0, 13.0, (8.0, 8.0), 4.4, 8, M('7c7c7c', 'metal'), PH)
    m.prism('y', 1.5, 2.5, (8.0, 8.0), 4.8, 8, M('505050'), PH)
    m.prism('y', 12.5, 13.5, (8.0, 8.0), 4.8, 8, M('505050'), PH)
    m.prism('y', 5.0, 10.5, (8.0, 8.0), 4.6, 8, M('d9d9d9'), PH)
    m.prism('y', 7.5, 8.5, (8.0, 8.0), 4.7, 8, M('989898'), PH)
    m.prism('y', 13.0, 14.0, (8.0, 8.0), 3.9, 8, M('bd790f'), PH)
    m.prism('y', 14.0, 14.6, (8.0, 8.0), 2.4, 8, M('e0a040'), PH)
    return m


# ---------------------------------------------------------------- 碘片

@item('iodine_tablet')
def iodine_tablet():
    """碘片：灰色圆形泡罩板，中心棕色圈内一粒紫色药片（凸起）。"""
    m = Model()
    disc_z(m, 8.0, 8.0, 7.0, 8.5, 6.6, M('808080'))
    disc_z(m, 8.0, 8.0, 8.5, 8.8, 6.0, M('999999'))
    disc_z(m, 8.0, 8.0, 8.5, 9.6, 3.8, M('644f45'))
    disc_z(m, 8.0, 8.0, 9.6, 10.4, 2.8, M('4a456e'))
    disc_z(m, 8.0, 8.0, 10.4, 10.8, 1.6, M('5c4a9c', 'glass'))
    m.box(3.5, 9.5, 8.8, 5.0, 12.0, 9.0, M('b0b0b0'))
    return m


# ---------------------------------------------------------------- 传送卷轴

def scroll(m, paper, edge):
    """卷轴：上下两根木轴 + 中间纸面（略弧：中段前凸）。"""
    for y in (2.0, 13.0):
        m.prism('x', 2.5, 13.5, (y + 0.5, 8.0), 1.3, 8, M('896727'), PH)
        m.prism('x', 1.5, 2.5, (y + 0.5, 8.0), 0.8, 8, M('493615'), PH)
        m.prism('x', 13.5, 14.5, (y + 0.5, 8.0), 0.8, 8, M('493615'), PH)
    m.box(3.5, 3.0, 7.6, 12.5, 13.0, 8.4, M(paper))
    m.box(4.0, 5.0, 8.4, 12.0, 11.0, 8.7, M(paper))
    m.box(3.5, 3.0, 8.4, 4.0, 13.0, 8.6, M(edge))
    m.box(12.0, 3.0, 8.4, 12.5, 13.0, 8.6, M(edge))


@item('recall_scroll')
def recall_scroll():
    """未绑定：空白羊皮纸卷轴。"""
    m = Model()
    scroll(m, 'eaeac7', 'a7a848')
    return m


@item('recall_scroll_bound')
def recall_scroll_bound():
    """已绑定：纸面绘有地图纹路与发光的定位标记。"""
    m = Model()
    scroll(m, 'eaeac7', 'a7a848')
    for (x0, y0, x1, y1, c) in ((5.0, 9.0, 8.0, 9.5, '878839'), (7.5, 7.0, 8.0, 9.5, '878839'),
                                (8.0, 7.0, 11.0, 7.5, '878839'), (5.0, 5.5, 6.5, 6.0, 'bbc177')):
        m.box(x0, y0, 8.7, x1, y1, 8.9, M(c))
    m.box(9.5, 9.0, 8.7, 11.0, 10.5, 9.0, M('c03030', 'glow'))   # 定位标记
    return m


# ---------------------------------------------------------------- 门

def door(m, face, frame, dark, panels, win=None, hinge=None):
    """门：立放厚门板（两层：门框 + 门芯），门芯上凸起的嵌板、可选窗口与合页。"""
    m.box(3.5, 0.5, 7.0, 12.5, 15.5, 9.0, M(frame))
    m.box(4.25, 1.25, 9.0, 11.75, 14.75, 9.4, M(face))
    for (x0, y0, x1, y1) in panels:
        m.box(x0, y0, 9.4, x1, y1, 9.9, M(dark))
        m.box(x0 + 0.5, y0 + 0.5, 9.9, x1 - 0.5, y1 - 0.5, 10.1, M(face))
    if win:
        x0, y0, x1, y1 = win
        m.box(x0, y0, 8.9, x1, y1, 9.45, M('303030'))
    if hinge:
        for y in (2.5, 12.0):
            m.box(2.8, y, 7.5, 3.5, y + 1.5, 8.5, M(hinge))


@item('reinforced_door')
def reinforced_door():
    """加固门：灰色石质厚门，上部观察窗（凹入），下部两条横向加强带 + 铆点。"""
    m = Model()
    door(m, '6b695e', '515148', '4c4b43', ((5.0, 2.0, 11.0, 4.5), (5.0, 5.5, 11.0, 8.0)), win=(5.0, 10.0, 11.0, 13.5))
    for (x, y) in ((4.6, 1.6), (10.9, 1.6), (4.6, 14.0), (10.9, 14.0)):
        m.box(x, y, 9.4, x + 0.5, y + 0.5, 9.7, M('8a887c'))
    return m


@item('rubber_door')
def rubber_door():
    """橡胶木门：黄褐木门，上下两块回字形嵌板，左侧灰色合页。"""
    m = Model()
    door(m, 'c99c3b', '976c1c', '886117', ((5.0, 2.0, 11.0, 7.0), (5.0, 9.0, 11.0, 14.0)), hinge='808b95')
    for y in (4.0, 11.0):
        m.box(6.5, y, 10.1, 9.5, y + 1.0, 10.3, M('886117'))
    return m


@item('rubber_sign')
def rubber_sign():
    """橡胶木告示牌：木板（边框 + 刻字横纹）立在深色木桩上。"""
    m = Model()
    m.box(1.5, 7.0, 7.0, 14.5, 14.5, 9.0, M('a07728'))
    m.box(2.0, 7.5, 9.0, 14.0, 14.0, 9.3, M('bb9137'))
    for (x0, y, x1) in ((3.5, 12.0, 7.0), (8.0, 12.0, 12.5), (3.5, 10.0, 6.0), (7.0, 10.0, 11.0), (4.5, 8.5, 9.5)):
        m.box(x0, y, 9.3, x1, y + 0.6, 9.5, M('6d4b0b'))
    m.box(7.0, 1.0, 7.25, 9.0, 7.0, 8.75, M('42301d'))
    return m
