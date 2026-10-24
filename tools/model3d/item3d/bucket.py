"""流体桶家族：参照 ic2 textures/item/bucket/*（原版铁桶 + 桶口液面颜色）。

3D：八棱台形铁桶（上宽下窄，叠层收窄）+ 加厚桶沿 + 桶口内液面 + 提梁。
biofuel_bucket 与 biomass_bucket 在 ic2 原版共用同一贴图，保持一致。
"""
import math

from core import Model, item, mat

PH = math.pi / 8
RIM = mat('#d8d8d8', 'metal')
WALL = mat('#a8a8a8', 'metal')
WALL_DARK = mat('#969696')
BASE = mat('#727272')
HANDLE = mat('#353535')


def bucket(surface, surface_dark, extra=None):
    m = Model()
    m.prism('y', 1.5, 3.0, (8, 8), 4.0, 8, {'*': BASE, 'cap': BASE}, PH)
    m.prism('y', 3.0, 7.0, (8, 8), 4.6, 8, {'*': WALL_DARK, 'cap': WALL_DARK}, PH)
    m.prism('y', 7.0, 11.0, (8, 8), 5.2, 8, {'*': WALL, 'cap': WALL}, PH)
    m.prism('y', 11.0, 12.4, (8, 8), 5.8, 8, {'*': RIM, 'cap': RIM}, PH)       # 桶沿
    m.prism('y', 12.4, 12.6, (8, 8), 4.9, 8, {'*': surface_dark, 'cap': surface}, PH)  # 液面
    # 提梁：两侧耳 + 拱形横梁
    for x in (1.8, 13.4):
        m.box(x, 10.0, 7.4, x + 0.8, 12.2, 8.6, HANDLE)
    m.box(1.8, 12.2, 7.4, 2.6, 13.8, 8.6, HANDLE)
    m.box(13.4, 12.2, 7.4, 14.2, 13.8, 8.6, HANDLE)
    m.box(2.6, 13.8, 7.4, 13.4, 14.6, 8.6, HANDLE)
    if extra:
        extra(m)
    return m


@item('biomass_bucket')
def biomass_bucket():
    return bucket(mat('#57af16'), mat('#377612'))


@item('biofuel_bucket')
def biofuel_bucket():
    return bucket(mat('#57af16'), mat('#377612'))


def _foam(m):
    # 建筑泡沫：液面上隆起的泡沫团
    for x, z, h in ((6.0, 7.0, 13.4), (8.8, 9.0, 13.6), (9.2, 6.2, 13.2)):
        m.box(x - 1.2, 12.6, z - 1.2, x + 1.2, h, z + 1.2, mat('#9b9b9b'))


@item('construction_foam_bucket')
def construction_foam_bucket():
    return bucket(mat('#9b9b9b'), mat('#868686'), _foam)


@item('construct_foam_bucket')
def construct_foam_bucket():
    return bucket(mat('#9b9b9b'), mat('#868686'), _foam)


@item('coolant_bucket')
def coolant_bucket():
    return bucket(mat('#0f6471'), mat('#073345'))


@item('creosote_bucket')
def creosote_bucket():
    return bucket(mat('#846b00'), mat('#5e4800'))


@item('hot_coolant_bucket')
def hot_coolant_bucket():
    return bucket(mat('#d63e49', 'glow'), mat('#9f131c'))


def _lava_crust(m):
    # 熔岩岩浆：灰褐色结壳 + 橙色裂缝（原图的斑驳）
    m.box(5.0, 12.6, 5.0, 8.0, 12.9, 8.0, mat('#a06051'))
    m.box(8.5, 12.6, 8.5, 11.0, 12.9, 11.0, mat('#b68c7b'))


@item('pahoehoe_lava_bucket')
def pahoehoe_lava_bucket():
    return bucket(mat('#e08829', 'glow'), mat('#a06051'), _lava_crust)


@item('uu_matter_bucket')
def uu_matter_bucket():
    return bucket(mat('#7f007d', 'glow'), mat('#50084f'))


@item('weed_ex_bucket')
def weed_ex_bucket():
    return bucket(mat('#1c4924'), mat('#1b3c21'))
