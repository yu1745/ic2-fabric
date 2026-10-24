"""咖啡杯家族：参照 ic2 textures/item/brewing/mug_*（石杯 + 侧把手，杯中咖啡，热咖啡冒蒸汽）。"""
import math

from core import Model, item, mat

PH = math.pi / 8
STONE = mat('#888788')
STONE_DARK = mat('#696969')
STONE_HI = mat('#a6a6a6')


def mug(liquid=None, steam=False):
    m = Model()
    m.prism('y', 2.0, 3.0, (7.4, 8), 3.8, 8, {'*': STONE_DARK, 'cap': STONE_DARK}, PH)
    m.prism('y', 3.0, 10.4, (7.4, 8), 4.2, 8, {'*': STONE, 'cap': STONE}, PH)
    m.prism('y', 10.4, 11.2, (7.4, 8), 4.5, 8, {'*': STONE_HI, 'cap': STONE_HI}, PH)   # 杯沿
    m.prism('y', 11.0, 11.25, (7.4, 8), 3.6, 8, {'*': mat('#3e3e3e'), 'cap': liquid or mat('#3e3e3e')}, PH)
    # 把手（右侧，+X）
    m.box(11.4, 4.2, 7.3, 13.6, 5.2, 8.7, STONE_DARK)
    m.box(11.4, 8.6, 7.3, 13.6, 9.6, 8.7, STONE_DARK)
    m.box(12.8, 5.2, 7.3, 13.8, 8.6, 8.7, STONE_DARK)
    if steam:
        for x, y, z in ((6.0, 12.2, 7.4), (7.0, 13.4, 8.2), (6.2, 14.6, 7.8), (8.4, 12.6, 8.8)):
            m.box(x, y, z, x + 0.9, y + 0.9, z + 0.9, mat('#e8e7e7'))
    return m


@item('empty_mug')
def empty_mug():
    return mug()


@item('coffee_mug')
def coffee_mug():
    return mug(mat('#59310a'), steam=True)


@item('cold_coffee_mug')
def cold_coffee_mug():
    return mug(mat('#3e240b'))


@item('dark_coffee_mug')
def dark_coffee_mug():
    return mug(mat('#2f1700'), steam=True)
