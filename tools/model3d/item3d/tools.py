"""手持工具家族：参照 ic2 item/tool/*（青铜工具、扳手、树脂提取器、剪线钳、锻造锤、泡沫喷枪等）。

约定：
- 原图为斜向工具（工具头在右上）的物品一律 handheld=True，沿 +Y 竖直建模、工具头朝上，
  -X 侧在 GUI 中对应原图的左上方、+X 侧对应右下方。
- 原图本身竖直/正放的物品（剪线钳、测量仪、遥控器、工具箱等）按普通物品立于 x/y 平面建模。
"""
import math

from core import Model, item, mat

PH = math.pi / 8

# 青铜（原图 ffa34f / ee7a39 / e65423 / a33c17 / 792d10）
BZ_L = mat('#ffa34f', 'metal')
BZ = mat('#ee7a39', 'metal')
BZ_M = mat('#e65423', 'metal')
BZ_D = mat('#a33c17')
BZ_DD = mat('#792d10')
# 木柄（896727 / 684e1e / 4a3615）
WOOD_L = mat('#896727')
WOOD = mat('#684e1e')
WOOD_D = mat('#4a3615')
# 钢件
STEEL_L = mat('#dbeff1', 'metal')
STEEL = mat('#b9d6d9', 'metal')
STEEL_M = mat('#91a3aa', 'metal')
DARK = mat('#373737')
DARK2 = mat('#505050')


def wood_handle(m, y0, y1, r=0.85):
    """木柄：八棱杆 + 底端深色端帽 + 中段缠绕。"""
    m.prism('y', y0 + 0.8, y1, (8, 8), r, 8, WOOD, PH)
    m.prism('y', y0, y0 + 0.8, (8, 8), r * 1.15, 8, WOOD_D, PH)
    m.prism('y', y0 + 2.2, y0 + 4.2, (8, 8), r * 1.12, 8, WOOD_L, PH)  # 握把缠绕


# ---------------------------------------------------------------- 青铜工具

@item('bronze_pickaxe', handheld=True)
def bronze_pickaxe():
    m = Model()
    wood_handle(m, 0.5, 12.5)
    m.box(6.6, 11.0, 6.6, 9.4, 14.2, 9.4, BZ_D)             # 镐头套筒
    m.box(3.5, 12.0, 7.0, 12.5, 14.0, 9.0, BZ_M)            # 横梁
    m.box(4.0, 14.0, 7.3, 12.0, 14.6, 8.7, BZ_L)            # 上脊高光
    for x0, x1 in ((1.8, 3.5), (12.5, 14.2)):              # 两端下弯
        m.box(x0, 10.0, 7.2, x1, 13.6, 8.8, BZ)
    m.box(1.4, 8.0, 7.5, 2.8, 10.0, 8.5, BZ_L)              # 镐尖
    m.box(13.2, 8.0, 7.5, 14.6, 10.0, 8.5, BZ_L)
    return m


@item('bronze_axe', handheld=True)
def bronze_axe():
    m = Model()
    wood_handle(m, 0.5, 13.5)
    m.box(7.0, 9.8, 6.7, 9.6, 14.8, 9.3, BZ_D)              # 斧头套筒
    m.box(9.6, 11.0, 7.0, 11.0, 13.8, 9.0, BZ_DD)           # 斧背锤面
    m.box(4.6, 10.6, 7.3, 7.0, 14.2, 8.7, BZ_M)             # 斧身
    m.box(3.0, 9.4, 7.5, 4.6, 15.2, 8.5, BZ)                # 扩张的刃部
    m.box(1.6, 8.4, 7.65, 3.0, 15.9, 8.35, BZ)
    m.box(1.0, 8.8, 7.8, 1.6, 15.5, 8.2, BZ_L)              # 刃口
    return m


@item('bronze_hoe', handheld=True)
def bronze_hoe():
    m = Model()
    wood_handle(m, 0.5, 13.0)
    m.box(7.0, 12.0, 7.0, 9.4, 14.6, 9.0, BZ_D)             # 套筒
    m.box(3.8, 12.8, 7.2, 7.0, 14.4, 8.8, BZ_M)             # 横臂
    m.box(3.8, 14.4, 7.4, 8.6, 14.9, 8.6, BZ_L)             # 上沿高光
    m.box(2.0, 9.4, 7.4, 3.8, 14.6, 8.6, BZ)                # 下垂锄刃
    m.box(2.2, 8.6, 7.6, 3.6, 9.4, 8.4, BZ_L)               # 刃口
    return m


@item('bronze_shovel', handheld=True)
def bronze_shovel():
    m = Model()
    wood_handle(m, 0.5, 9.5)
    m.prism('y', 8.6, 10.2, (8, 8), 1.15, 8, BZ_D, PH)       # 套箍
    m.box(5.4, 10.0, 7.4, 10.6, 14.4, 8.6, BZ_M)            # 铲面
    m.box(6.2, 14.4, 7.5, 9.8, 15.4, 8.5, BZ)               # 圆头
    m.box(6.0, 10.4, 8.6, 10.0, 14.2, 8.9, BZ_L)            # 正面高光凸面
    m.box(7.6, 9.6, 8.6, 8.4, 13.4, 9.2, BZ_D)              # 背筋
    return m


@item('bronze_sword', handheld=True)
def bronze_sword():
    m = Model()
    m.box(7.2, 0.6, 7.2, 8.8, 2.0, 8.8, BZ_DD)              # 剑首
    m.prism('y', 2.0, 5.2, (8, 8), 0.75, 8, WOOD, PH)        # 握把
    m.prism('y', 3.0, 4.0, (8, 8), 0.85, 8, WOOD_L, PH)
    m.box(4.0, 5.2, 7.0, 12.0, 6.4, 9.0, BZ_D)              # 护手
    m.box(3.4, 5.0, 7.3, 4.2, 6.8, 8.7, BZ_DD)
    m.box(11.8, 5.0, 7.3, 12.6, 6.8, 8.7, BZ_DD)
    m.box(6.8, 6.4, 7.6, 9.2, 14.4, 8.4, BZ_M)              # 剑身
    m.box(7.6, 6.4, 7.35, 8.4, 14.6, 8.65, BZ_L)            # 中脊
    m.box(7.3, 14.4, 7.7, 8.7, 15.3, 8.3, BZ)               # 剑尖
    m.box(7.7, 15.3, 7.8, 8.3, 15.9, 8.2, BZ_L)
    return m


# ---------------------------------------------------------------- 扳手（青铜）

@item('wrench', handheld=True)
def wrench():
    m = Model()
    m.box(7.0, 0.8, 7.3, 9.0, 10.0, 8.7, BZ_M)              # 手柄
    m.box(6.7, 1.2, 7.1, 9.3, 5.6, 8.9, BZ_D)               # 握持段
    for y in (2.0, 3.4, 4.8):
        m.box(6.6, y, 7.0, 9.4, y + 0.4, 9.0, BZ_DD)        # 防滑纹
    m.box(5.0, 10.0, 7.1, 11.0, 12.2, 8.9, BZ)              # 开口扳头底座
    m.box(5.0, 12.2, 7.1, 6.9, 15.2, 8.9, BZ)               # 长钳口
    m.box(9.1, 12.2, 7.1, 11.0, 14.4, 8.9, BZ)              # 短钳口
    m.box(5.3, 14.6, 7.3, 6.6, 15.5, 8.7, BZ_L)
    m.box(9.4, 13.8, 7.3, 10.7, 14.7, 8.7, BZ_L)
    m.box(7.4, 1.0, 8.7, 8.6, 9.6, 8.95, BZ_L)              # 手柄正面高光
    return m


# ---------------------------------------------------------------- 树脂提取器（木制龙头）

@item('treetap')
def treetap():
    m = Model()
    m.prism('x', 5.0, 14.5, (10.5, 8), 1.0, 8, mat('#7d4a31'), PH)    # 插入树干的长管
    m.prism('x', 13.6, 14.8, (10.5, 8), 0.7, 8, mat('#643014'), PH)   # 尖端
    m.box(3.0, 5.0, 6.8, 5.6, 12.0, 9.2, mat('#643014'))              # 弯头（向下出液）
    m.box(3.3, 4.0, 7.1, 5.3, 5.0, 8.9, mat('#4d250f'))               # 出液口
    m.box(3.4, 12.0, 7.2, 5.2, 13.2, 8.8, mat('#4d250f'))             # 顶部塞
    m.box(3.7, 13.2, 7.5, 4.9, 14.2, 8.5, mat('#7d4a31'))
    m.box(3.7, 2.9, 7.5, 4.9, 4.0, 8.5, mat('#3a3a40', 'glass'))     # 挂在出液口的树脂滴
    return m


# ---------------------------------------------------------------- 剪线钳（竖放 V 形）

@item('cutter')
def cutter():
    m = Model()
    # 两片钳刃（浅钢色），根部在枢轴处
    for x0, x1, xt0, xt1 in ((5.6, 7.7, 4.6, 6.4), (8.3, 10.4, 9.6, 11.4)):
        m.box(x0, 9.5, 7.3, x1, 12.5, 8.7, STEEL)
        m.box(xt0, 12.5, 7.45, xt1, 15.2, 8.55, STEEL_L)
        m.box(xt0 + 0.3, 12.0, 7.25, xt1 - 0.3, 13.0, 8.75, STEEL_M)
    m.prism('z', 6.8, 9.2, (8, 9.0), 1.1, 8, DARK, PH)                # 枢轴铆钉
    # 两根手柄自枢轴向下张开（三段折线）
    for s in (-1, 1):
        for (ya, yb, dx) in ((6.4, 8.8, 1.3), (3.4, 6.4, 2.4), (0.6, 3.4, 3.5)):
            cx = 8 + s * dx
            m.box(cx - 0.95, ya, 7.2, cx + 0.95, yb, 8.8, DARK2 if ya > 6 else mat('#696969'))
        cx = 8 + s * 3.5
        m.box(cx - 1.1, 0.4, 7.0, cx + 1.1, 2.2, 9.0, DARK)            # 手柄末端
    return m


# ---------------------------------------------------------------- 锻造锤

@item('forge_hammer', handheld=True)
def forge_hammer():
    m = Model()
    m.prism('y', 0.6, 2.0, (8, 8), 1.05, 8, mat('#3d3d3d'), PH)       # 柄尾
    m.prism('y', 2.0, 6.0, (8, 8), 0.95, 8, mat('#595c68'), PH)       # 胶套握把
    m.prism('y', 6.0, 11.5, (8, 8), 0.8, 8, mat('#896727'), PH)       # 木柄
    m.box(3.0, 11.0, 6.2, 13.0, 15.0, 9.8, STEEL)                     # 锤头
    m.box(3.4, 11.4, 9.8, 12.6, 14.6, 10.1, STEEL_L)                  # 正面高光
    m.box(2.0, 11.4, 6.6, 3.0, 14.6, 9.4, mat('#fafafa', 'metal'))    # 打击面
    m.box(13.0, 11.6, 6.8, 14.2, 14.4, 9.2, STEEL_M)                  # 后锤面
    m.box(6.6, 10.4, 6.6, 9.4, 11.0, 9.4, mat('#616772'))             # 楔箍
    return m


# ---------------------------------------------------------------- 泡沫喷枪

@item('foam_sprayer', handheld=True)
def foam_sprayer():
    m = Model()
    G1, G2, G3 = mat('#acacac', 'metal'), mat('#8f8f8f'), mat('#7d7f7f')
    m.prism('y', 6.0, 14.2, (8, 8), 1.6, 8, G1, PH)                   # 枪管
    m.prism('y', 14.2, 15.6, (8, 8), 1.0, 8, mat('#646464'), PH)      # 喷嘴
    m.prism('y', 9.0, 10.0, (8, 8), 1.8, 8, G2, PH)                   # 箍
    m.prism('y', 12.0, 12.8, (8, 8), 1.8, 8, G2, PH)
    m.box(5.6, 1.5, 6.2, 10.4, 6.6, 9.8, G3)                          # 机匣
    m.prism('y', 0.6, 6.0, (8, 8), 2.2, 8, mat('#9d9e9e'), PH)        # 泡沫罐
    m.prism('y', 0.3, 0.6, (8, 8), 1.6, 8, mat('#646464'), PH)
    m.box(2.2, 3.4, 7.1, 5.6, 5.2, 8.9, G2)                           # 握把（向 -X 伸出）
    m.box(2.2, 2.0, 7.2, 3.6, 3.4, 8.8, mat('#646464'))
    m.box(4.6, 5.2, 7.5, 5.6, 6.6, 8.5, mat('#646464'))               # 扳机
    return m


# ---------------------------------------------------------------- 调试物品（三个紫色方块）

@item('debug_item')
def debug_item():
    m = Model()
    EDGE, FACE = mat('#6b3f7f'), mat('#d67fff', 'glow')
    for (x, y, s) in ((2.0, 8.5, 6.0), (7.4, 4.8, 4.4), (11.0, 1.8, 3.4)):
        m.box(x, y, 8 - s / 2, x + s, y + s, 8 + s / 2, EDGE)
        i = s * 0.18
        m.box(x + i, y + i, 8 + s / 2, x + s - i, y + s - i, 8 + s / 2 + 0.3, FACE)  # 正面发光面板
        m.box(x + i, y + s, 8 - s / 2 + i, x + s - i, y + s + 0.3, 8 + s / 2 - i, FACE)  # 顶面
    return m


# ---------------------------------------------------------------- 防辐射容纳箱

@item('containment_box')
def containment_box():
    m = Model()
    FRAME, PANEL = mat('#4a564a'), mat('#d5d9b9')
    m.box(1.5, 1.5, 4.0, 14.5, 14.5, 12.0, FRAME)                     # 箱体
    m.box(2.8, 2.8, 12.0, 13.2, 12.0, 12.4, PANEL)                    # 正面面板
    m.box(2.8, 12.4, 12.0, 13.2, 13.4, 12.6, mat('#fafafa'))          # 标签条
    m.box(1.2, 14.5, 3.7, 14.8, 15.2, 12.3, mat('#384238'))           # 箱盖边
    YEL, BLK = mat('#e4e510'), mat('#202020')
    m.prism('z', 12.4, 12.9, (8, 7.4), 4.3, 16, YEL, 0)              # 辐射标志底盘
    m.prism('z', 12.9, 13.3, (8, 7.4), 0.9, 8, BLK, PH)              # 中心
    m.box(4.8, 7.9, 12.9, 7.2, 10.8, 13.3, BLK)                       # 三瓣
    m.box(8.8, 7.9, 12.9, 11.2, 10.8, 13.3, BLK)
    m.box(6.8, 3.6, 12.9, 9.2, 6.2, 13.3, BLK)
    return m


# ---------------------------------------------------------------- 频率发射器（遥控器）

@item('frequency_transmitter')
def frequency_transmitter():
    m = Model()
    m.box(5.0, 1.5, 6.4, 11.0, 12.0, 9.6, mat('#7a7a7a'))             # 外壳
    m.box(5.6, 2.2, 9.6, 10.4, 11.4, 9.9, mat('#aaaaaa'))             # 正面面板
    m.box(6.8, 4.6, 9.9, 9.2, 8.6, 10.4, mat('#2244ff', 'glow'))      # 蓝色按键
    m.box(7.4, 12.0, 7.4, 8.6, 12.6, 8.6, mat('#707070'))
    m.prism('y', 12.6, 15.6, (8, 8), 0.4, 8, mat('#969696', 'metal'), PH)  # 天线
    m.prism('y', 15.4, 15.9, (8, 8), 0.6, 8, mat('#707070'), PH)
    return m


# ---------------------------------------------------------------- 测量仪（万用表）

@item('meter')
def meter():
    m = Model()
    m.box(1.2, 4.0, 6.0, 14.8, 13.4, 10.0, mat('#bdbdbd'))            # 机身
    m.box(1.2, 3.4, 6.2, 14.8, 4.0, 9.8, mat('#909090'))              # 底边
    m.box(2.6, 8.0, 10.0, 9.6, 12.2, 10.3, mat('#727272'))            # 屏框
    m.box(3.0, 8.4, 10.3, 9.2, 11.8, 10.5, mat('#819d7a', 'glass'))   # 绿色液晶
    m.box(2.6, 5.4, 10.0, 9.6, 7.0, 10.4, mat('#909090'))             # 按键条
    m.prism('z', 10.0, 10.9, (12.0, 9.0), 1.9, 8, mat('#909090'), PH)  # 旋钮
    m.prism('z', 10.9, 11.3, (12.0, 9.0), 1.2, 8, mat('#bebebe'), PH)
    m.box(11.8, 9.2, 11.3, 12.2, 10.6, 11.5, mat('#ee2222', 'glow'))  # 指针
    m.box(12.0, 5.2, 10.0, 13.6, 6.6, 10.4, mat('#ee2222', 'glow'))   # 指示灯
    # 表笔引线
    for x, c in ((3.6, '#d02020'), (12.4, '#3a3ad0')):
        m.box(x - 0.6, 3.0, 7.4, x + 0.6, 3.4, 8.6, mat('#525050'))
        m.box(x - 0.4, 0.8, 7.6, x + 0.4, 3.0, 8.4, mat(c))
    return m


# ---------------------------------------------------------------- 工具箱

@item('tool_box')
def tool_box():
    m = Model()
    OR, OR_D, OR_L = mat('#ff9000'), mat('#d56409'), mat('#ffa030')
    TRIM = mat('#5a3410')
    m.box(1.0, 2.0, 4.5, 15.0, 9.6, 11.5, OR)                          # 箱体
    m.box(1.5, 1.4, 5.0, 14.5, 2.0, 11.0, OR_D)                        # 底沿
    m.box(0.8, 6.2, 4.3, 15.2, 6.8, 11.7, mat('#9e5419'))              # 盖缝
    m.box(1.4, 9.6, 5.0, 14.6, 10.6, 11.0, OR_L)                       # 箱盖顶
    m.box(6.6, 5.2, 11.5, 9.4, 7.4, 12.0, TRIM)                        # 锁扣
    m.box(3.0, 3.4, 11.5, 13.0, 4.2, 11.8, mat('#e26602'))             # 正面压条
    for x in (4.2, 11.0):                                              # 提手立柱
        m.box(x, 10.6, 7.3, x + 0.8, 12.6, 8.7, TRIM)
    m.box(4.2, 12.6, 7.3, 11.8, 13.6, 8.7, TRIM)                       # 提手
    return m


# ================================================================ 电动工具（tool/electric/*）
# 共同结构：橙色机壳（e66c03 / f88c00）+ 深灰提环 + 灰色卡盘 + 工具头朝上。

EL_OR = mat('#f88c00')
EL_OR_D = mat('#e66c03')
EL_OR_L = mat('#fb9a20')
EL_DK = mat('#3a3a3a')
EL_DK2 = mat('#4e4e4e')
EL_GRAY = mat('#565656')


def drill_body(m):
    """钻机机身：橙色壳体（正面开槽）+ -X 侧 D 形提环 + 顶部卡盘座。"""
    m.box(5.2, 1.2, 5.8, 10.8, 7.0, 10.2, EL_OR)                  # 壳体
    m.box(5.6, 0.6, 6.2, 10.4, 1.2, 9.8, EL_OR_D)                  # 底盖
    m.box(6.8, 2.4, 10.2, 9.2, 5.8, 10.5, EL_DK)                   # 正面散热槽
    m.box(6.8, 2.4, 5.5, 9.2, 5.8, 5.8, EL_DK)                     # 背面散热槽
    m.box(5.0, 7.0, 6.0, 11.0, 7.6, 10.0, EL_OR_D)                 # 肩部
    m.box(1.6, 1.8, 7.2, 2.8, 6.8, 8.8, EL_DK2)                    # 提环
    m.box(1.6, 1.0, 7.2, 5.2, 2.2, 8.8, EL_DK2)
    m.box(1.6, 6.4, 7.2, 5.2, 7.6, 8.8, EL_DK2)
    m.box(1.4, 3.2, 7.0, 3.0, 5.4, 9.0, EL_DK)                     # 握把胶套
    m.prism('y', 7.6, 8.8, (8, 8), 2.1, 8, EL_GRAY, PH)             # 卡盘


def drill_bit(m, c1, c2, tip, tip_d=None):
    """锥形钻头：三段收窄 + 螺旋槽环。"""
    m.prism('y', 8.8, 11.6, (8, 8), 1.55, 8, c1, PH)
    m.prism('y', 11.6, 13.8, (8, 8), 1.1, 8, c1, PH)
    m.prism('y', 13.8, 15.2, (8, 8), 0.7, 8, tip, PH)
    m.prism('y', 15.2, 16.0, (8, 8), 0.32, 8, tip_d or tip, PH)
    for y, rr in ((9.8, 1.62), (10.8, 1.62), (12.6, 1.17)):
        m.prism('y', y, y + 0.4, (8, 8), rr, 8, c2, PH)


@item('drill', handheld=True)
def drill():
    m = Model()
    drill_body(m)
    drill_bit(m, mat('#aaaaaa', 'metal'), mat('#8e8e8e'), mat('#a5a5a5', 'metal'))
    return m


@item('diamond_drill', handheld=True)
def diamond_drill():
    m = Model()
    drill_body(m)
    drill_bit(m, mat('#aaaaaa', 'metal'), mat('#8e8e8e'), mat('#33e0e0', 'glass'), mat('#00b4b4', 'glass'))
    m.prism('y', 13.5, 13.9, (8, 8), 1.2, 8, mat('#006e6e'), PH)    # 金刚石镶环
    return m


@item('iridium_drill', handheld=True)
def iridium_drill():
    m = Model()
    drill_body(m)
    drill_bit(m, mat('#dedede', 'metal'), mat('#9a9a9a'), mat('#33e0e0', 'glass'), mat('#00b4b4', 'glass'))
    m.prism('y', 13.5, 13.9, (8, 8), 1.2, 8, mat('#009898'), PH)
    # +X 侧挂载的红色铱电池组
    m.box(10.8, 0.8, 6.4, 13.6, 5.0, 9.6, mat('#fa1818'))
    m.box(11.2, 1.2, 9.6, 13.2, 4.6, 9.9, mat('#ff6060', 'glass'))
    m.box(10.8, 5.0, 6.8, 13.2, 5.6, 9.2, mat('#8a1414'))
    return m


@item('chainsaw', handheld=True)
def chainsaw():
    m = Model()
    drill_body(m)
    m.box(6.3, 8.8, 7.3, 9.7, 15.2, 8.7, mat('#949494', 'metal'))   # 导板
    m.box(6.8, 15.2, 7.4, 9.2, 15.8, 8.6, mat('#838383'))           # 圆头
    m.box(7.3, 9.6, 8.7, 8.7, 14.6, 8.9, mat('#686868'))            # 导板铆槽
    for i in range(7):                                              # 两侧锯齿链
        y = 8.9 + i * 0.9
        m.box(5.8, y, 7.6, 6.3, y + 0.5, 8.4, EL_DK2)
        m.box(9.7, y + 0.45, 7.6, 10.2, y + 0.95, 8.4, EL_DK2)
    return m


def pistol_grip(m):
    """电动扳手/树脂提取器/风速计共用的橙色手枪式机身：竖直枪身 + 向 +X 伸出的握把。"""
    m.box(6.2, 4.0, 6.4, 9.8, 9.6, 9.6, EL_OR)                     # 枪身
    m.box(6.0, 9.0, 6.6, 10.0, 9.6, 9.4, EL_OR_D)
    m.box(9.8, 3.2, 6.8, 13.8, 6.0, 9.2, EL_OR)                    # 握把
    m.box(12.6, 1.0, 6.8, 14.8, 3.2, 9.2, EL_OR_D)                 # 握把末端
    m.box(9.8, 6.0, 7.4, 11.0, 7.0, 8.6, EL_DK2)                   # 扳机
    m.box(6.6, 4.6, 9.6, 9.4, 8.6, 9.9, EL_OR_L)                   # 正面高光
    m.box(6.8, 2.6, 7.0, 9.2, 4.0, 9.0, mat('#616161'))            # 底部电池座
    m.prism('y', 9.6, 10.6, (8, 8), 1.2, 8, mat('#616161'), PH)     # 输出轴套


@item('electric_treetap', handheld=True)
def electric_treetap():
    m = Model()
    pistol_grip(m)
    m.prism('y', 10.6, 15.0, (8, 8), 0.75, 8, mat('#6a3600'), PH)   # 木质导管
    m.prism('y', 15.0, 15.8, (8, 8), 0.5, 8, mat('#5c2e00'), PH)
    m.prism('y', 12.0, 12.6, (8, 8), 0.9, 8, mat('#5c2e00'), PH)
    return m


@item('electric_wrench', handheld=True)
def electric_wrench():
    m = Model()
    pistol_grip(m)
    WH, WH_L = mat('#d5d5d5', 'metal'), mat('#e7e7e7', 'metal')
    m.prism('y', 10.6, 13.0, (8, 8), 0.8, 8, WH, PH)
    m.box(5.6, 13.0, 7.2, 10.4, 14.2, 8.8, WH_L)                   # 扳头
    m.box(5.6, 14.2, 7.2, 6.8, 15.8, 8.8, WH_L)
    m.box(9.2, 14.2, 7.2, 10.4, 15.8, 8.8, WH_L)
    return m


@item('wind_meter', handheld=True)
def wind_meter():
    m = Model()
    pistol_grip(m)
    RING = mat('#b6b6b6', 'metal')
    m.prism('y', 10.6, 11.6, (8, 8), 0.6, 8, mat('#9d9d9d'), PH)
    m.prism('z', 7.4, 8.6, (8, 13.6), 2.8, 8, RING, PH)             # 测风环
    m.prism('z', 7.2, 8.8, (8, 13.6), 1.9, 8, mat('#8a8a8a'), PH)
    m.prism('z', 7.0, 9.0, (8, 13.6), 0.9, 8, mat('#fa8e01', 'glow'), PH)  # 橙色叶轮心
    for dx, dy in ((0, 2.6), (0, -2.6), (2.6, 0), (-2.6, 0)):       # 四根支脚
        m.box(8 + dx - 0.4, 13.6 + dy - 0.4, 7.6, 8 + dx + 0.4, min(16, 13.6 + dy + 0.4), 8.4, mat('#9d9d9d'))
    return m


@item('mining_laser', 'tab_icon_ic2_tools', handheld=True)
def mining_laser():
    """采矿镭射枪（创造标签图标同此造型）。黑色枪身 + 红色能量条。"""
    m = Model()
    BK, BK2, RED = mat('#333333'), mat('#3e3e3e', 'metal'), mat('#d20000', 'glow')
    m.prism('y', 6.6, 14.6, (8, 8), 1.2, 8, BK2, PH)                # 枪管
    m.prism('y', 14.6, 15.8, (8, 8), 0.8, 8, BK, PH)                # 枪口
    m.prism('y', 15.2, 15.9, (8, 8), 0.45, 8, RED, PH)
    for y in (8.4, 10.2, 12.0, 13.6):                               # 红色线圈
        m.prism('y', y, y + 0.6, (8, 8), 1.4, 8, RED, PH)
    m.box(6.2, 1.6, 6.4, 9.8, 6.6, 9.6, BK)                         # 机匣
    m.box(6.8, 2.6, 9.6, 9.2, 5.6, 9.9, mat('#be0a0a', 'glow'))     # 侧面能量窗
    m.box(9.8, 2.8, 7.1, 12.6, 4.4, 8.9, BK)                        # 握把
    m.box(11.4, 0.8, 7.1, 13.0, 2.8, 8.9, BK2)
    m.box(4.8, 0.4, 7.0, 6.2, 3.4, 9.0, BK2)                        # 枪托
    m.box(5.8, 6.6, 7.2, 6.8, 12.4, 8.8, BK)                        # 上导轨
    return m


def scanner_body(m):
    """扫描仪：浅蓝半透明平板 + 绿色矿脉显示 + 红/蓝指示块。"""
    m.box(2.0, 3.0, 6.8, 14.0, 13.6, 9.2, mat('#819faa'))           # 外框
    m.box(2.8, 3.8, 9.2, 13.2, 12.8, 9.5, mat('#aac7d3', 'glass'))  # 屏幕
    m.box(5.6, 8.0, 9.5, 11.0, 11.4, 9.8, mat('#86a07f'))           # 矿脉显示
    m.box(6.4, 8.6, 9.8, 10.2, 10.8, 10.0, mat('#9bd08c', 'glow'))
    m.box(3.6, 4.6, 9.5, 5.4, 6.8, 9.9, mat('#d06060', 'glow'))     # 红色指示块
    m.box(7.2, 4.6, 9.5, 12.4, 5.8, 9.8, mat('#98bcd2', 'glow'))    # 蓝色读数条


@item('scanner')
def scanner():
    m = Model()
    scanner_body(m)
    return m


@item('advanced_scanner')
def advanced_scanner():
    m = Model()
    scanner_body(m)
    m.box(10.4, 13.6, 7.2, 12.8, 14.4, 8.8, mat('#819faa'))         # 高级型：顶部探测头
    m.prism('x', 10.4, 12.8, (14.8, 8), 0.7, 8, mat('#aac7d3', 'glass'), PH)
    return m


@item('obscurator')
def obscurator():
    m = Model()
    WH = mat('#dbdbdb')
    m.box(1.0, 4.0, 6.0, 12.4, 15.2, 10.0, WH)                      # 白色外框
    m.box(2.2, 5.2, 10.0, 11.2, 14.0, 10.2, mat('#303030', 'glass'))  # 黑色屏幕
    m.box(2.2, 5.2, 5.8, 11.2, 14.0, 6.0, mat('#303030'))
    m.box(11.8, 3.4, 6.6, 15.2, 6.6, 9.4, WH)                       # 喷头罐
    m.box(12.6, 6.6, 7.0, 14.6, 7.6, 9.0, mat('#c10000'))           # 红色喷头
    m.box(13.2, 2.2, 7.4, 14.0, 3.4, 8.6, mat('#c10000'))
    m.box(4.0, 1.6, 7.6, 4.8, 4.0, 8.4, mat('#c8c8c8'))             # 线缆
    m.box(4.0, 0.8, 7.6, 4.8, 1.6, 8.4, mat('#c10000'))
    return m


# ================================================================ 纳米剑（含 10 帧激活动画）

SABER_DK = mat('#4d4c4c', 'metal')
SABER_CYAN = mat('#12e0c0', 'glow')
SABER_YEL = mat('#ffee2d', 'glow')
SABER_CORE = mat('#0e7a68', 'glow')


def saber_hilt(m):
    """剑柄：深灰柄身 + 绿色能量窗 + 橙色护环 + 发射口。"""
    m.prism('y', 0.4, 1.2, (8, 8), 1.0, 8, mat('#383838'), PH)
    m.prism('y', 1.2, 6.0, (8, 8), 0.9, 8, mat('#444444', 'metal'), PH)
    m.box(7.5, 1.8, 8.75, 8.5, 5.4, 9.0, mat('#1c8a22', 'glow'))     # 绿色能量窗
    m.prism('y', 6.0, 6.7, (8, 8), 1.15, 8, mat('#ef7c01'), PH)     # 橙色护环
    m.prism('y', 6.7, 7.4, (8, 8), 0.8, 8, mat('#525151'), PH)      # 发射口


BLADE_Y0, BLADE_Y1 = 7.4, 15.2


def saber_rails(m, segs):
    """能量刃：两条发光棱边 + 刃尖 + 暗青内芯；segs 为 [(y0, y1, 材质)]，按段着色以表现流光。"""
    m.box(7.4, BLADE_Y0, 7.85, 8.6, BLADE_Y1, 8.15, SABER_CORE)
    for y0, y1, t in segs:
        m.box(6.8, y0, 7.6, 7.4, y1, 8.4, t)
        m.box(8.6, y0, 7.6, 9.2, y1, 8.4, t)


def saber_active(m, frame):
    saber_hilt(m)
    L = BLADE_Y1 - BLADE_Y0
    tip = SABER_CYAN
    if frame == 10:
        segs = [(BLADE_Y0, BLADE_Y1, SABER_YEL)]
        tip = SABER_YEL
    elif frame == 9:
        segs = [(BLADE_Y0, BLADE_Y1, SABER_CYAN)]
        tip = SABER_YEL
    else:
        # 黄色光带从护手处沿刃向上流动（原图第 1~8 帧逐帧上移）
        c = BLADE_Y0 + L * (frame - 0.5) / 8.4
        a, b = max(BLADE_Y0, c - 0.6), min(BLADE_Y1, c + 0.6)
        segs = [(a, b, SABER_YEL)]
        if a > BLADE_Y0:
            segs.append((BLADE_Y0, a, SABER_CYAN))
        if b < BLADE_Y1:
            segs.append((b, BLADE_Y1, SABER_CYAN))
    saber_rails(m, segs)
    m.box(7.4, BLADE_Y1 - 0.6, 7.6, 8.6, BLADE_Y1 + 0.6, 8.4, tip)   # 刃尖
    return m


@item('nano_saber', 'tool/nano_saber/inactive', handheld=True)
def nano_saber_inactive():
    """未激活：灰色收拢刃身。"""
    m = Model()
    saber_hilt(m)
    m.box(7.1, BLADE_Y0, 7.5, 8.9, BLADE_Y1, 8.5, SABER_DK)
    m.box(7.6, BLADE_Y0, 7.3, 8.4, BLADE_Y1 - 0.4, 8.7, mat('#5a5959'))
    m.box(7.4, BLADE_Y1, 7.6, 8.6, BLADE_Y1 + 0.6, 8.4, SABER_DK)
    return m


@item('tool/nano_saber/active_1', handheld=True)
def nano_saber_active_1():
    return saber_active(Model(), 1)


@item('tool/nano_saber/active_2', handheld=True)
def nano_saber_active_2():
    return saber_active(Model(), 2)


@item('tool/nano_saber/active_3', handheld=True)
def nano_saber_active_3():
    return saber_active(Model(), 3)


@item('tool/nano_saber/active_4', handheld=True)
def nano_saber_active_4():
    return saber_active(Model(), 4)


@item('tool/nano_saber/active_5', handheld=True)
def nano_saber_active_5():
    return saber_active(Model(), 5)


@item('tool/nano_saber/active_6', handheld=True)
def nano_saber_active_6():
    return saber_active(Model(), 6)


@item('tool/nano_saber/active_7', handheld=True)
def nano_saber_active_7():
    return saber_active(Model(), 7)


@item('tool/nano_saber/active_8', handheld=True)
def nano_saber_active_8():
    return saber_active(Model(), 8)


@item('tool/nano_saber/active_9', handheld=True)
def nano_saber_active_9():
    return saber_active(Model(), 9)


@item('tool/nano_saber/active_10', handheld=True)
def nano_saber_active_10():
    return saber_active(Model(), 10)


# ================================================================ 刷子（滚筒刷，16 色 + 无色）

def painter_model(main, light, dark):
    """滚筒刷：灰色手柄 + 折臂 + 横向滚筒（-X 侧支撑）。"""
    m = Model()
    m.prism('y', 0.4, 1.2, (8, 8), 1.0, 8, mat('#353535'), PH)
    m.prism('y', 1.2, 6.4, (8, 8), 0.9, 8, mat('#525252'), PH)       # 握把
    m.prism('y', 6.4, 7.2, (8, 8), 1.0, 8, mat('#777777'), PH)       # 握把箍
    m.prism('y', 7.2, 10.4, (8, 8), 0.45, 8, mat('#c9c9c9', 'metal'), PH)  # 钢杆
    m.box(2.4, 10.0, 7.6, 8.4, 10.8, 8.4, mat('#c9c9c9', 'metal'))   # 折臂
    m.box(2.4, 10.8, 7.6, 3.2, 13.0, 8.4, mat('#c9c9c9', 'metal'))
    m.prism('x', 3.2, 3.8, (13.0, 8), 0.7, 8, mat('#777777'), PH)    # 轴端
    m.prism('x', 3.8, 13.6, (13.0, 8), 2.1, 8, main, PH)             # 滚筒
    m.prism('x', 5.2, 12.2, (13.0, 8), 2.25, 8, light, PH)           # 滚筒中段（吸饱颜料，略鼓）
    m.prism('x', 13.6, 14.0, (13.0, 8), 1.6, 8, dark, PH)            # 端帽
    m.prism('x', 3.8, 4.2, (13.0, 8), 2.2, 8, dark, PH)
    return m


PAINTER_COLORS = {
    # 名称: (滚筒主色, 亮色, 端帽暗色)，取自 ic2 painter_<色>.png
    'painter': ('#f0e6d2', '#fffaee', '#a59f92'),
    'white': ('#eaeaea', '#ffffff', '#b9b9cb'),
    'orange': ('#db8b2a', '#e69e34', '#92460c'),
    'magenta': ('#ae4fa8', '#be5cb8', '#6b2166'),
    'light_blue': ('#5082ce', '#6f9bdb', '#3d669a'),
    'yellow': ('#d5b223', '#f2d72c', '#765d1a'),
    'lime': ('#5a9a08', '#65ac09', '#336728'),
    'pink': ('#cb78a3', '#db8bb4', '#80365c'),
    'gray': ('#5e5e5e', '#848484', '#414141'),
    'light_gray': ('#babac1', '#d9d9d9', '#9f9fb3'),
    'cyan': ('#236f8f', '#3c8eb0', '#175369'),
    'purple': ('#8230b2', '#a453ce', '#511a71'),
    'blue': ('#14499a', '#345ec3', '#082e79'),
    'brown': ('#7a4621', '#995d33', '#632c04'),
    'green': ('#36510d', '#4a6b18', '#2c420b'),
    'red': ('#d2443f', '#e74d48', '#942626'),
    'black': ('#383751', '#4a4968', '#27263d'),
}


def painter_of(color):
    main, light, dark = PAINTER_COLORS[color]
    return painter_model(mat(main), mat(light), mat(dark))


@item('painter', handheld=True)
def painter():
    return painter_of('painter')


@item('white_painter', handheld=True)
def white_painter():
    return painter_of('white')


@item('orange_painter', handheld=True)
def orange_painter():
    return painter_of('orange')


@item('magenta_painter', handheld=True)
def magenta_painter():
    return painter_of('magenta')


@item('light_blue_painter', handheld=True)
def light_blue_painter():
    return painter_of('light_blue')


@item('yellow_painter', handheld=True)
def yellow_painter():
    return painter_of('yellow')


@item('lime_painter', handheld=True)
def lime_painter():
    return painter_of('lime')


@item('pink_painter', handheld=True)
def pink_painter():
    return painter_of('pink')


@item('gray_painter', handheld=True)
def gray_painter():
    return painter_of('gray')


@item('light_gray_painter', handheld=True)
def light_gray_painter():
    return painter_of('light_gray')


@item('cyan_painter', handheld=True)
def cyan_painter():
    return painter_of('cyan')


@item('purple_painter', handheld=True)
def purple_painter():
    return painter_of('purple')


@item('blue_painter', handheld=True)
def blue_painter():
    return painter_of('blue')


@item('brown_painter', handheld=True)
def brown_painter():
    return painter_of('brown')


@item('green_painter', handheld=True)
def green_painter():
    return painter_of('green')


@item('red_painter', handheld=True)
def red_painter():
    return painter_of('red')


@item('black_painter', handheld=True)
def black_painter():
    return painter_of('black')
