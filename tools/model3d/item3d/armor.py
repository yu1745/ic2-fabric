"""护甲与背包家族（只做物品形态）：参照 ic2 item/armor/*。

部件函数：
- helmet：顶盖 + 后壁 + 两侧护颊 + 前额，正面留出面部开口（能看到内壁）。
- chestplate：躯干 + 两肩甲 + 领口，正面加胸甲板。
- leggings：腰带 + 两条腿筒（中间留裆缝）。
- boots：左右两只靴子，靴尖向外（与原图剪影一致）。
- pack：背包类，两只竖直储能/燃料舱 + 中间连接架 + 背带。
纳米（原图近黑）统一提亮到 #343434 一档，避免大面积过暗。
"""
import math

from core import Model, item, mat

PH = math.pi / 8


# ---------------------------------------------------------------- 部件

def helmet(m, shell, edge, inner, top=None):
    t = top or shell
    m.box(2.5, 11.0, 3.0, 13.5, 12.6, 13.0, t)                            # 顶盖（三级收分成圆顶）
    m.box(3.3, 12.6, 3.8, 12.7, 13.6, 12.4, t)
    m.box(4.6, 13.6, 5.0, 11.4, 14.4, 11.2, t)
    m.box(2.5, 4.0, 3.0, 13.5, 11.0, 5.0, {'*': shell, 's': inner})       # 后壁（内面为深色）
    m.box(2.5, 4.0, 5.0, 4.5, 11.0, 13.0, {'*': shell, 'e': inner})       # 左护颊
    m.box(11.5, 4.0, 5.0, 13.5, 11.0, 13.0, {'*': shell, 'w': inner})     # 右护颊
    m.box(4.5, 9.4, 11.0, 11.5, 11.0, 13.0, {'*': shell, 'd': inner})     # 前额
    m.box(2.3, 3.4, 2.8, 13.7, 4.0, 13.2, edge)                           # 下沿包边


def chestplate(m, main, dark, light):
    m.box(3.5, 1.0, 5.5, 12.5, 12.0, 10.5, main)                          # 躯干
    m.box(0.8, 8.6, 5.0, 4.2, 13.0, 11.0, main)                           # 左肩甲
    m.box(11.8, 8.6, 5.0, 15.2, 13.0, 11.0, main)                         # 右肩甲
    m.box(0.6, 8.2, 4.8, 4.4, 8.6, 11.2, dark)                            # 肩甲下沿
    m.box(11.6, 8.2, 4.8, 15.4, 8.6, 11.2, dark)
    m.box(4.2, 12.0, 6.0, 6.4, 13.6, 10.0, main)                          # 领口两侧
    m.box(9.6, 12.0, 6.0, 11.8, 13.6, 10.0, main)
    m.box(6.4, 11.2, 6.2, 9.6, 12.2, 9.8, dark)                           # 领口凹
    m.box(4.4, 7.2, 10.5, 7.7, 11.2, 10.9, light)                         # 左右胸甲板
    m.box(8.3, 7.2, 10.5, 11.6, 11.2, 10.9, light)
    m.box(5.0, 2.6, 10.5, 11.0, 6.4, 10.8, light)                         # 腹甲
    m.box(3.3, 0.6, 5.3, 12.7, 1.6, 10.7, dark)                           # 下摆


def leggings(m, main, dark, light):
    m.box(2.5, 12.0, 5.5, 13.5, 14.6, 10.5, main)                         # 腰带区
    m.box(2.3, 14.0, 5.3, 13.7, 15.0, 10.7, dark)                         # 腰带
    m.box(2.5, 1.0, 5.5, 7.4, 12.0, 10.5, main)                           # 左腿
    m.box(8.6, 1.0, 5.5, 13.5, 12.0, 10.5, main)                          # 右腿
    m.box(7.4, 9.0, 5.5, 8.6, 12.0, 10.5, main)                           # 裆部
    m.box(3.0, 5.0, 10.5, 6.9, 7.4, 11.0, light)                          # 护膝
    m.box(9.1, 5.0, 10.5, 13.0, 7.4, 11.0, light)
    m.box(2.3, 0.6, 5.3, 7.6, 1.6, 10.7, dark)                            # 裤脚
    m.box(8.4, 0.6, 5.3, 13.7, 1.6, 10.7, dark)


def boots(m, main, dark, light, sole=None):
    for s in (-1, 1):
        c = 8 + s * 3.4                                                    # 靴筒中心
        m.box(c - 1.8, 3.6, 6.0, c + 1.8, 12.8, 10.0, main)               # 靴筒
        m.box(c - 2.0, 12.0, 5.8, c + 2.0, 13.0, 10.2, light)             # 靴口
        x0, x1 = (c - 4.2, c + 1.8) if s < 0 else (c - 1.8, c + 4.2)
        m.box(x0, 1.4, 6.0, x1, 4.6, 10.0, main)                           # 脚掌（靴尖向外）
        m.box(x0 - 0.2, 0.6, 5.8, x1 + 0.2, 1.4, 10.2, sole or dark)       # 鞋底


def pack(m, shell, edge, window, frame=None, y0=1.5, y1=14.5):
    """背包：两只储能舱（正面窗口）+ 连接架 + 背后两条背带。"""
    for x0 in (1.8, 9.6):
        m.box(x0, y0, 5.0, x0 + 4.6, y1, 10.6, shell)                     # 舱体
        m.box(x0 - 0.2, y1 - 1.2, 4.8, x0 + 4.8, y1 - 0.4, 10.8, edge)    # 上箍
        m.box(x0 - 0.2, y0 + 0.4, 4.8, x0 + 4.8, y0 + 1.2, 10.8, edge)    # 下箍
        m.box(x0 + 1.4, y0 + 3.0, 10.6, x0 + 3.2, y1 - 3.0, 11.0, window)  # 窗口
    m.box(6.4, 6.0, 5.8, 9.6, 10.0, 9.8, frame or edge)                    # 连接架
    for x0 in (2.8, 10.6):
        m.box(x0, y0 + 1.0, 4.2, x0 + 2.6, y1 - 1.0, 5.0, edge)           # 背带


# ---------------------------------------------------------------- 青铜

BZ_L, BZ, BZ_M = mat('#ffa34f', 'metal'), mat('#ee7a39', 'metal'), mat('#e65423', 'metal')
BZ_D, BZ_DD = mat('#a33c17'), mat('#792d10')


@item('bronze_helmet')
def bronze_helmet():
    m = Model()
    helmet(m, BZ_M, BZ_D, BZ_DD, top=BZ)
    m.box(7.2, 5.6, 11.6, 8.8, 9.4, 12.8, BZ_D)                            # 护鼻
    m.box(7.4, 14.4, 5.4, 8.6, 14.9, 10.8, BZ_L)                           # 顶脊
    return m


@item('bronze_chestplate')
def bronze_chestplate():
    m = Model()
    chestplate(m, BZ_M, BZ_D, BZ)
    m.box(1.2, 12.0, 5.4, 3.8, 13.4, 10.6, BZ_L)                           # 肩甲高光
    m.box(12.2, 12.0, 5.4, 14.8, 13.4, 10.6, BZ_L)
    return m


@item('bronze_leggings')
def bronze_leggings():
    m = Model()
    leggings(m, BZ_M, BZ_DD, BZ)
    m.box(7.0, 13.8, 10.7, 9.0, 15.2, 11.0, BZ_L)                          # 腰扣
    return m


@item('bronze_boots')
def bronze_boots():
    m = Model()
    boots(m, BZ_M, BZ_DD, BZ)
    return m


# ---------------------------------------------------------------- 复合胸甲（暗橄榄色）

@item('alloy_chestplate')
def alloy_chestplate():
    m = Model()
    OL, OL_D, OL_L = mat('#515044', 'metal'), mat('#3e3e35'), mat('#5f5e50')
    chestplate(m, OL, OL_D, OL_L)
    m.box(6.6, 11.6, 9.8, 9.4, 12.6, 10.6, mat('#c65c35'))                 # 橙色领扣
    for y in (7.0, 9.0):                                                   # 胸甲横向装甲条
        m.box(4.6, y, 11.1, 11.4, y + 0.8, 11.4, mat('#9b9988'))
    return m


# ---------------------------------------------------------------- 防化服（橙色）

HZ, HZ_D, HZ_L = mat('#cc4714'), mat('#a13810'), mat('#fb5a1b')


@item('hazmat_helmet')
def hazmat_helmet():
    m = Model()
    helmet(m, HZ, HZ_D, HZ_D, top=HZ_L)
    m.box(4.5, 4.4, 11.0, 11.5, 9.4, 11.6, mat('#3c4a5a', 'glass'))        # 面罩
    m.box(5.0, 7.6, 11.6, 7.0, 8.8, 11.9, mat('#a8c0d8', 'glass'))         # 面罩反光
    m.prism('z', 11.6, 13.4, (8, 5.0), 1.4, 8, mat('#3a3a3a'), PH)         # 呼吸滤罐
    m.prism('z', 13.4, 13.8, (8, 5.0), 0.9, 8, mat('#4d4d4d'), PH)
    return m


@item('hazmat_chestplate')
def hazmat_chestplate():
    m = Model()
    chestplate(m, HZ, HZ_D, HZ_L)
    m.box(7.5, 1.6, 10.5, 8.5, 11.4, 10.9, mat('#d8d8d8'))                 # 拉链
    return m


@item('hazmat_leggings')
def hazmat_leggings():
    m = Model()
    leggings(m, HZ, HZ_D, HZ_L)
    return m


# ---------------------------------------------------------------- 橡胶靴

@item('rubber_boots')
def rubber_boots():
    m = Model()
    boots(m, mat('#383838'), mat('#2c2c2c'), mat('#454545'))
    for s in (-1, 1):                                                      # 侧面浅灰标记
        c = 8 + s * 3.4
        m.box(c - 0.6, 8.0, 10.0, c + 0.6, 10.0, 10.3, mat('#696969'))
    return m


# ---------------------------------------------------------------- 纳米（深灰 + 绿色指示灯）

NA, NA_D, NA_L = mat('#343434', 'metal'), mat('#2a2a2a'), mat('#3e3e3e')
GREEN = mat('#00ff00', 'glow')
GREEN_D = mat('#008000', 'glow')


def green_dot(m, x, y, z, s=1.2):
    m.box(x - s / 2, y - s / 2, z, x + s / 2, y + s / 2, z + 0.4, GREEN)


@item('nano_helmet')
def nano_helmet():
    m = Model()
    helmet(m, NA, NA_D, NA_D, top=NA_L)
    m.box(4.5, 5.2, 11.4, 11.5, 7.6, 12.2, mat('#1e2e9a', 'glass'))        # 蓝色目镜
    green_dot(m, 8, 12.2, 13.0)                                            # 额部指示灯
    return m


@item('nano_chestplate')
def nano_chestplate():
    m = Model()
    chestplate(m, NA, NA_D, NA_L)
    m.box(6.6, 7.0, 11.1, 9.4, 9.8, 11.4, GREEN_D)                         # 胸口能量核心
    green_dot(m, 8, 8.4, 11.4, 1.6)
    return m


@item('nano_leggings')
def nano_leggings():
    m = Model()
    leggings(m, NA, NA_D, NA_L)
    green_dot(m, 8, 13.4, 10.5)
    green_dot(m, 4.95, 6.2, 11.0)
    green_dot(m, 11.05, 6.2, 11.0)
    return m


@item('nano_boots')
def nano_boots():
    m = Model()
    boots(m, NA, NA_D, NA_L)
    for s in (-1, 1):
        green_dot(m, 8 + s * 3.4, 6.0, 10.0)
    return m


# ---------------------------------------------------------------- 量子（白色 + 深灰包边 + 绿色指示灯）

QU, QU_D, QU_L = mat('#e5e5e5', 'metal'), mat('#3c3c3c'), mat('#f2f2f2')


@item('quantum_helmet')
def quantum_helmet():
    m = Model()
    helmet(m, QU, QU_D, QU_D, top=QU_L)
    m.box(4.5, 5.0, 11.4, 11.5, 7.8, 12.4, mat('#3366ff', 'glass'))        # 蓝色面罩
    m.box(2.3, 10.6, 12.8, 13.7, 11.2, 13.2, QU_D)                         # 前额包边
    green_dot(m, 8, 12.4, 13.0)
    return m


@item('quantum_chestplate')
def quantum_chestplate():
    m = Model()
    chestplate(m, QU, QU_D, QU_L)
    m.box(6.4, 6.8, 11.1, 9.6, 10.0, 11.4, QU_D)                           # 胸口核心座
    green_dot(m, 8, 8.4, 11.4, 1.8)
    for x in (4.4, 11.0):                                                  # 正面深色分割线
        m.box(x, 1.6, 10.5, x + 0.6, 6.6, 10.7, QU_D)
    return m


@item('quantum_leggings')
def quantum_leggings():
    m = Model()
    leggings(m, QU, QU_D, QU_L)
    m.box(7.0, 1.6, 10.5, 7.4, 9.0, 10.7, QU_D)                            # 腿筒内侧包边
    m.box(8.6, 1.6, 10.5, 9.0, 9.0, 10.7, QU_D)
    green_dot(m, 8, 13.4, 10.7)
    green_dot(m, 4.95, 6.2, 11.0)
    green_dot(m, 11.05, 6.2, 11.0)
    return m


@item('quantum_boots')
def quantum_boots():
    m = Model()
    boots(m, QU, QU_D, QU_L, sole=QU_D)
    for s in (-1, 1):
        green_dot(m, 8 + s * 3.4, 6.4, 10.0)
    return m


# ---------------------------------------------------------------- 夜视镜 / 太阳能头盔

@item('night_vision_goggles')
def night_vision_goggles():
    m = Model()
    STRAP, BODY = mat('#343434'), mat('#3e3e3e', 'metal')
    m.box(1.0, 9.0, 4.0, 15.0, 10.6, 5.0, STRAP)                           # 头带（后）
    m.box(1.0, 9.0, 5.0, 2.0, 10.6, 9.0, STRAP)                            # 头带（侧）
    m.box(14.0, 9.0, 5.0, 15.0, 10.6, 9.0, STRAP)
    m.box(2.0, 5.0, 8.0, 14.0, 11.6, 10.6, BODY)                           # 镜体
    m.box(6.8, 5.0, 10.6, 9.2, 8.6, 11.2, STRAP)                           # 鼻梁
    for cx in (4.8, 11.2):                                                 # 双目镜筒
        m.prism('z', 10.6, 12.8, (cx, 8.2), 2.4, 8, BODY, PH)
        m.prism('z', 12.8, 13.2, (cx, 8.2), 2.0, 8, mat('#009b07', 'glow'), PH)
        m.prism('z', 13.0, 13.3, (cx, 8.6), 0.8, 8, mat('#7fe08a', 'glow'), PH)
    m.box(5.6, 11.6, 8.4, 10.4, 12.6, 10.2, mat('#4a4a4a'))                # 顶部电池盒
    return m


@item('solar_helmet')
def solar_helmet():
    m = Model()
    helmet(m, mat('#c6c6c6', 'metal'), mat('#6b6b6b'), mat('#4a4a4a'), top=mat('#969696'))
    m.box(2.0, 14.2, 2.6, 14.0, 15.0, 13.4, mat('#4a4a4a'))                # 太阳能板框
    m.box(2.6, 15.0, 3.2, 13.4, 15.4, 12.8, mat('#412340', 'glass'))       # 紫黑色光伏面
    for x in (5.4, 8.0, 10.6):
        m.box(x - 0.1, 15.0, 3.2, x + 0.1, 15.5, 12.8, mat('#6b6b6b'))     # 栅线
    m.box(4.5, 4.4, 11.0, 11.5, 8.6, 11.4, mat('#3a3a3a'))                 # 深色面罩
    return m


# ---------------------------------------------------------------- 背包类

@item('batpack')
def batpack():
    m = Model()
    pack(m, mat('#6c502e'), mat('#543d1f'), mat('#3a3a3a'))
    for x0 in (1.8, 9.6):                                                  # 电池端子
        m.box(x0 + 1.6, 14.5, 7.0, x0 + 3.0, 15.2, 8.6, mat('#9a9a9a'))
    return m


@item('advanced_batpack')
def advanced_batpack():
    m = Model()
    pack(m, BZ_M, BZ_DD, mat('#3c3c3c'), frame=BZ_D)
    for x0 in (1.8, 9.6):
        m.box(x0 + 0.4, 6.0, 10.6, x0 + 1.2, 10.0, 10.9, BZ_L)             # 青铜高光条
        m.box(x0 + 1.6, 14.5, 7.0, x0 + 3.0, 15.2, 8.6, mat('#9a9a9a'))
    return m


ST, ST_D, ST_L = mat('#979797', 'metal'), mat('#505050'), mat('#d9d9d9')


@item('energy_pack')
def energy_pack():
    m = Model()
    pack(m, ST, ST_D, mat('#ff1200', 'glow'))
    for x0 in (1.8, 9.6):
        m.box(x0 + 0.6, 12.0, 10.6, x0 + 1.6, 13.0, 10.9, ST_L)
    return m


@item('lappack')
def lappack():
    m = Model()
    pack(m, ST, ST_D, mat('#0669b2', 'glow'))
    for x0 in (1.8, 9.6):
        m.box(x0 + 1.6, 6.0, 11.0, x0 + 3.0, 8.0, 11.2, mat('#2a50ff', 'glow'))  # 青金石光点
        m.box(x0 + 0.6, 12.0, 10.6, x0 + 1.6, 13.0, 10.9, ST_L)
    return m


@item('cf_pack')
def cf_pack():
    m = Model()
    pack(m, ST, ST_D, mat('#ececec', 'glass'))
    for x0 in (1.8, 9.6):                                                  # 泡沫喷口
        m.prism('y', 0.4, 1.5, (x0 + 2.3, 7.8), 1.0, 8, ST_D, PH)
    return m


def jet_pod(m, x0, shell, edge, window):
    """喷气背包舱：尖锥顶 + 舱身 + 窗口 + 底部喷口。"""
    cx = x0 + 2.4
    m.box(x0, 3.4, 5.2, x0 + 4.8, 12.0, 10.4, shell)
    m.box(x0 + 0.6, 12.0, 5.8, x0 + 4.2, 13.6, 9.8, shell)               # 锥顶两级
    m.box(x0 + 1.4, 13.6, 6.6, x0 + 3.4, 15.0, 9.0, mat('#c4c4c4'))
    m.box(x0 + 1.2, 5.0, 10.4, x0 + 3.6, 10.8, 10.8, window)
    m.box(x0 - 0.2, 3.0, 5.0, x0 + 5.0, 3.6, 10.6, edge)
    m.prism('y', 1.6, 3.0, (cx, 7.8), 1.7, 8, edge, PH)                    # 喷口
    m.prism('y', 0.8, 1.6, (cx, 7.8), 1.2, 8, mat('#606060'), PH)


@item('jetpack')
def jetpack():
    m = Model()
    SH, ED = mat('#c0c0c0', 'metal'), mat('#808080')
    for x0 in (1.6, 9.6):
        jet_pod(m, x0, SH, ED, mat('#e8f070', 'glow'))                     # 黄色燃料窗
    m.box(6.4, 6.0, 6.0, 9.6, 10.6, 9.6, ED)                               # 中间燃料管架
    m.box(6.4, 7.6, 9.6, 9.6, 8.6, 10.0, SH)
    return m


@item('electric_jetpack')
def electric_jetpack():
    m = Model()
    SH, ED = mat('#c0c0c0', 'metal'), mat('#808080')
    for x0 in (1.6, 9.6):
        jet_pod(m, x0, SH, ED, mat('#ff1200', 'glow'))                     # 红色能量窗
    m.box(6.4, 6.0, 6.0, 9.6, 10.6, 9.6, SH)
    m.box(7.0, 7.0, 9.6, 9.0, 9.6, 10.0, mat('#b72015'))                   # 中部电池指示
    return m
