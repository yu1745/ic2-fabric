"""转子家族：参照 ic2 textures/item/rotor/*（X 形四叶，按材质配色）。

3D：中心毂（八棱短柱，轴向 z）+ 四片斜 45° 的叶片，叶片绕自身长轴略扭转（前后两片错开厚度）形成桨距。
"""
import math

from core import Model, item, mat, obox

PH = math.pi / 8


def rotor(light, mid, dark, edge, hub):
    m = Model()
    for k in range(4):
        ang = math.pi / 4 + k * math.pi / 2
        cx, cy = 8 + math.cos(ang) * 4.1, 8 + math.sin(ang) * 4.1
        # 叶片：主体 + 前缘高光条，前后错位做出桨距
        obox(m, cx, cy, 7.4, 1.8, ang, 7.3, 8.3, mid, dark)
        ox, oy = math.cos(ang + math.pi / 2) * 0.7, math.sin(ang + math.pi / 2) * 0.7
        obox(m, cx + ox, cy + oy, 7.0, 0.7, ang, 8.0, 8.7, light, edge)
        tx, ty = 8 + math.cos(ang) * 7.4, 8 + math.sin(ang) * 7.4
        obox(m, tx, ty, 1.0, 1.9, ang, 7.2, 8.6, edge)                      # 叶尖
    m.prism('z', 6.6, 9.6, (8, 8), 2.0, 8, {'*': dark, 'cap': hub}, PH)     # 毂
    m.prism('z', 9.6, 10.2, (8, 8), 1.0, 8, {'*': edge, 'cap': light}, PH)  # 轴头
    return m


@item('wooden_rotor')
def wooden_rotor():
    return rotor(mat('#c5a678'), mat('#9f844d'), mat('#6f5a38'), mat('#59482d'), mat('#836c3d'))


@item('bronze_rotor')
def bronze_rotor():
    return rotor(mat('#ffa34e'), mat('#c89042', 'metal'), mat('#99591c'), mat('#772500'), mat('#b26a29'))


@item('iron_rotor')
def iron_rotor():
    return rotor(mat('#ffffff'), mat('#d8d8d8', 'metal'), mat('#a8a8a8'), mat('#5e5e5e'), mat('#828282'))


@item('steel_rotor')
def steel_rotor():
    return rotor(mat('#fafafa'), mat('#b9d6d9', 'metal'), mat('#91a3aa'), mat('#595c68'), mat('#6f7983'))


@item('carbon_rotor')
def carbon_rotor():
    # 碳纤维原图近乎纯黑，按"大面积不低于 #303030"略提亮并保留编织感的明暗差
    return rotor(mat('#5a5a5a'), mat('#3a3a3a', 'metal'), mat('#303030'), mat('#262626'), mat('#2d2d2d'))
