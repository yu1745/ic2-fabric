"""船家族：参照 ic2 item/boat/*。

船身沿 X 方向（船头朝 -X），宽度沿 Z，立于物品平面中央；GUI 斜俯视可看到船舱内部。
- 橡胶艇：充气式，两侧与船头为八棱气囊管 + 舱底，橡胶灰褐色。
- 破损橡胶艇：同造型，一侧气囊瘪塌断开、船头下垂、带补丁。
- 碳纤维艇：硬质船壳（深蓝灰），尖船头 + 座板。
- 电动艇：灰色硬质船壳 + 白色上舷 + 船尾电动推进器。
"""
import math

from core import Model, item, mat

PH = math.pi / 8

RB, RB_L, RB_D = mat('#544a36'), mat('#5e533d'), mat('#3e3b34')


def rubber_floor(m, x0=3.0, x1=14.0):
    m.box(x0, 3.0, 4.4, x1, 4.4, 11.6, RB_D)                               # 舱底
    m.box(7.2, 4.4, 4.4, 8.8, 5.6, 11.6, mat('#494132'))                   # 座板


@item('rubber_boat')
def rubber_boat():
    m = Model()
    rubber_floor(m)
    for z in (3.6, 12.4):                                                  # 两侧气囊
        m.prism('x', 3.4, 13.4, (6.0, z), 2.5, 8, RB, PH)
    m.prism('z', 3.6, 12.4, (3.0, 6.6), 2.7, 8, RB_L, PH)                   # 船头横向气囊（略高）
    m.prism('z', 3.6, 12.4, (13.4, 6.0), 2.5, 8, RB, PH)                    # 船尾
    m.prism('x', 15.9 - 1.0, 16.0, (6.0, 8.0), 0.7, 8, mat('#3f382c'), PH)  # 充气阀
    for x in (5.6, 10.6):                                                  # 侧面拉绳扣
        for z in (1.0, 14.6):
            m.box(x, 6.4, z, x + 0.8, 7.4, z + 0.4, mat('#2e2b26'))
    return m


@item('broken_rubber_boat')
def broken_rubber_boat():
    m = Model()
    rubber_floor(m, 3.0, 14.0)
    m.prism('x', 3.4, 13.4, (6.0, 12.4), 2.5, 8, RB, PH)                    # 完好的一侧气囊
    m.prism('x', 3.4, 7.6, (5.0, 3.8), 1.8, 8, RB_D, PH)                    # 瘪掉的一侧（断成两截）
    m.prism('x', 9.4, 13.4, (4.6, 3.6), 1.5, 8, RB_D, PH)
    m.box(7.6, 3.2, 2.6, 9.4, 4.0, 4.6, mat('#403a2e'))                    # 塌在舱底的破皮
    m.prism('z', 3.6, 12.4, (3.0, 4.8), 1.9, 8, mat('#494132'), PH)         # 下垂的船头
    m.prism('z', 3.6, 12.4, (13.4, 6.0), 2.5, 8, RB, PH)
    m.box(9.6, 7.4, 11.6, 11.6, 8.0, 13.6, mat('#7a6a48'))                 # 补丁
    m.box(4.6, 3.0, 9.4, 6.2, 4.6, 10.4, mat('#2a2a2a'))                   # 破洞
    return m


def hard_hull(m, shell, rim, floor, bow):
    """硬质船壳：舱底 + 两舷 + 船尾板 + 两级收窄的尖船头。"""
    m.box(3.6, 2.2, 4.4, 14.0, 3.8, 11.6, shell)                           # 龙骨/船底
    m.box(3.6, 3.8, 4.4, 14.0, 4.2, 11.6, floor)                           # 舱底板
    m.box(3.6, 3.8, 3.0, 14.0, 9.0, 4.4, shell)                            # 左舷
    m.box(3.6, 3.8, 11.6, 14.0, 9.0, 13.0, shell)                          # 右舷
    m.box(14.0, 2.8, 3.0, 15.2, 9.0, 13.0, shell)                          # 船尾板
    m.box(2.0, 3.2, 4.0, 3.6, 9.4, 12.0, bow)                              # 船头一级
    m.box(0.8, 4.6, 5.8, 2.0, 10.2, 10.2, bow)                             # 船头尖
    m.box(3.4, 9.0, 2.8, 15.4, 9.6, 4.6, rim)                              # 上舷边
    m.box(3.4, 9.0, 11.4, 15.4, 9.6, 13.2, rim)
    m.box(13.8, 9.0, 4.6, 15.4, 9.6, 11.4, rim)
    m.box(7.4, 4.2, 4.4, 9.4, 6.2, 11.6, rim)                              # 座板


@item('carbon_boat')
def carbon_boat():
    m = Model()
    hard_hull(m, mat('#2c2d3a', 'metal'), mat('#3e3d48'), mat('#262529'), mat('#33343f', 'metal'))
    m.box(5.0, 6.0, 13.0, 12.6, 6.6, 13.2, mat('#4a4a5a'))                 # 舷侧碳纤维接缝
    m.box(5.0, 6.0, 2.8, 12.6, 6.6, 3.0, mat('#4a4a5a'))
    return m


@item('electric_boat')
def electric_boat():
    m = Model()
    hard_hull(m, mat('#5b5c62', 'metal'), mat('#bbbbbd'), mat('#3b3a3f'), mat('#737276', 'metal'))
    m.box(10.8, 4.2, 5.8, 13.8, 7.0, 10.2, mat('#a4a4a7'))                 # 电池舱
    m.box(11.4, 7.0, 7.0, 13.2, 7.4, 9.0, mat('#ffcc33', 'glow'))          # 电量指示
    m.box(15.2, 5.0, 7.0, 16.0, 8.0, 9.0, mat('#787878'))                  # 舷外推进器支架
    m.prism('y', 1.4, 5.0, (15.6, 8.0), 0.4, 8, mat('#787878'), PH)
    m.prism('x', 14.6, 16.0, (1.4, 8.0), 0.9, 8, mat('#a5a5a7', 'metal'), PH)  # 电机舱
    return m
