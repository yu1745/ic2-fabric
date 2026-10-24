"""升级卡家族：参照 ic2 textures/item/upgrade/*。

原版造型：斜 45° 的圆角芯片卡（左上端一排金色触点），卡面中部是功能图形：
- 白卡：超频（蓝色箭头）、高压（红色升压尖塔）、储能（红色电池块）、红石反转（暗红火把环）；
- 深蓝卡：弹出/抽入（箭头朝外/朝内）+ 右下 3×3 小立方体（物品黄、流体青）；高级版箭头为绿/红。
方向子模型（upgrade/<族>_<方向>）：原图把右下小立方体对应方向的面点亮（物品族橙底黄面、流体族蓝底青面），
这里做成真实的小立方体，被选方向的那个面发光：north=-Z（背面）、south=+Z（正面）、east=+X、west=-X、up=+Y、down=-Y。
"""
import math

from core import Model, item, mat, obox

ANG = -math.pi / 4          # 卡片长轴方向（从左上到右下）
CX, CY = 8.7, 7.7           # 卡片中心（x, y）
LEN, WID = 11.6, 8.2        # 长轴、短轴
Z0, Z1 = 7.0, 9.0           # 卡片厚度


def along(t, s=0.0):
    """卡片局部坐标 → 模型 x/y：t 沿长轴（负为左上端），s 沿短轴。"""
    ux, uy = math.cos(ANG), math.sin(ANG)
    return CX + t * ux - s * uy, CY + t * uy + s * ux


def card(dark):
    m = Model()
    if dark:
        body, edge, face = mat('#27263d'), mat('#100d14'), mat('#383751')
    else:
        body, edge, face = mat('#bfbfbf'), mat('#727272'), mat('#e8e8e8')
    obox(m, CX, CY, LEN, WID, ANG, Z0, Z1, body, edge)                   # 卡体
    obox(m, CX + 0.35, CY - 0.35, LEN - 2.2, WID - 1.6, ANG, Z1, Z1 + 0.3, face)  # 卡面略凸
    obox(m, CX, CY, LEN - 1.0, WID - 1.0, ANG, Z0 - 0.3, Z0, edge)       # 背面
    # 左上端金色触点：五齿，深浅交替
    for i, s in enumerate((-2.6, -1.3, 0.0, 1.3, 2.6)):
        x, y = along(-LEN / 2 + 1.3, s)
        obox(m, x, y, 2.4, 0.9, ANG, Z1 - 0.2, Z1 + 0.5, mat('#ffde75') if i % 2 == 0 else mat('#c29b38'), mat('#9b7826'))
    return m


def arrow(m, inward, light, dark, t0=-1.8, s0=-0.6):
    """功能箭头：沿卡片长轴，弹出朝左上（外）、抽入朝右下（内）。"""
    d = 1 if inward else -1
    x, y = along(t0, s0)
    obox(m, x, y, 3.2, 1.1, ANG, Z1 + 0.3, Z1 + 1.0, light, dark)       # 箭杆
    hx, hy = along(t0 + d * 1.9, s0)
    for k in (-1, 1):                                                     # 箭头两翼
        ang = ANG + (math.pi if d < 0 else 0) + k * 2.4
        ox, oy = hx + math.cos(ang) * 0.9, hy + math.sin(ang) * 0.9
        obox(m, ox, oy, 2.0, 1.0, ang, Z1 + 0.3, Z1 + 1.0, light, dark)


def mini_cube(m, base, hi, lit=None):
    """右下 3×3 小立方体；lit 为被点亮的面（n/s/e/w/u/d），None 表示整块高亮（无方向的基础卡）。"""
    x, y = along(2.6, 0.4)
    h = 1.35
    tex = {f: (hi if (lit is None or f == lit) else base) for f in 'nsewud'}
    m.box(x - h, y - h, Z1 + 0.3, x + h, y + h, Z1 + 0.3 + 2 * h, tex)


ITEM_CUBE = (mat('#bd2f00'), mat('#ffff00', 'glow'), mat('#ffce00'))
FLUID_CUBE = (mat('#0039a9'), mat('#00ffff', 'glow'), mat('#00c0ff'))
DIRS = {'north': 'n', 'south': 's', 'east': 'e', 'west': 'w', 'up': 'u', 'down': 'd'}


def transfer(inward, fluid, advanced, direction=None):
    m = card(dark=True)
    base, hi, plain = FLUID_CUBE if fluid else ITEM_CUBE
    if advanced:
        light, dark = (mat('#ff0000'), mat('#aa0f01')) if inward else (mat('#7aff00'), mat('#00be06'))
    elif fluid:
        light, dark = mat('#00ffff'), mat('#00c0ff')
    else:
        light, dark = mat('#ffff00'), mat('#ffce00')
    arrow(m, inward, light, dark)
    if direction is None:
        mini_cube(m, plain, hi if not advanced else mat('#ffe400'), None)
    else:
        mini_cube(m, base, hi, DIRS[direction])
    return m


FAMILIES = {  # 物品名前缀 → (抽入, 流体, 高级)
    'ejector': (False, False, False),
    'pulling': (True, False, False),
    'fluid_ejector': (False, True, False),
    'fluid_pulling': (True, True, False),
    'advanced_ejector': (False, False, True),
    'advanced_pulling': (True, False, True),
}

for _fam, (_in, _fl, _adv) in FAMILIES.items():
    item(f'{_fam}_upgrade')(lambda a=_in, b=_fl, c=_adv: transfer(a, b, c))
    for _d in DIRS:
        item(f'upgrade/{_fam}_{_d}')(lambda a=_in, b=_fl, c=_adv, d=_d: transfer(a, b, c, d))


# ---------------------------------------------------------------- 白卡

@item('overclocker_upgrade')
def overclocker_upgrade():
    m = card(dark=False)
    # 蓝色加速箭头：朝左上的双折箭（原图 A/B/C 三色块）
    arrow(m, False, mat('#0452c7'), mat('#0039a9'), t0=0.6, s0=0.0)
    x, y = along(2.4, 0.0)
    obox(m, x, y, 1.6, 2.6, ANG, Z1 + 0.3, Z1 + 0.9, mat('#072592'))
    return m


@item('transformer_upgrade')
def transformer_upgrade():
    m = card(dark=False)
    # 红色升压尖塔：三级台阶向上收窄
    for i, (w, h) in enumerate(((4.2, 1.0), (2.8, 1.0), (1.4, 1.2))):
        y = 4.6 + i * 1.1
        m.box(9.2 - w / 2, y, Z1 + 0.3, 9.2 + w / 2, y + h, Z1 + 0.9 + i * 0.25,
              mat(('#720000', '#aa0f01', '#ff0000')[i]))
    return m


@item('energy_storage_upgrade')
def energy_storage_upgrade():
    m = card(dark=False)
    # 红色储能块：小电池（壳 + 正极 + 亮色电量条）
    x, y = along(1.2, 0.2)
    obox(m, x, y, 3.6, 2.6, ANG, Z1 + 0.3, Z1 + 1.1, mat('#aa0f01'), mat('#410500'))
    px, py = along(-0.9, 0.2)
    obox(m, px, py, 0.7, 1.2, ANG, Z1 + 0.4, Z1 + 0.9, mat('#720000'))
    obox(m, x, y, 2.4, 1.2, ANG, Z1 + 1.1, Z1 + 1.3, mat('#ff0000', 'glow'))
    return m


@item('redstone_inverter_upgrade')
def redstone_inverter_upgrade():
    m = card(dark=False)
    # 红石反转：暗红环座 + 中央亮红火把头
    x, y = along(1.4, 0.2)
    obox(m, x, y, 3.2, 3.2, ANG, Z1 + 0.3, Z1 + 0.8, mat('#2d0000'))
    obox(m, x, y, 2.0, 2.0, ANG, Z1 + 0.8, Z1 + 1.2, mat('#720000'))
    obox(m, x, y, 1.0, 1.0, ANG, Z1 + 1.2, Z1 + 1.8, mat('#ff0000', 'glow'), mat('#aa0f01'))
    return m
