"""3D 机器模型：长方体/棱柱 DSL → OBJ + MTL + 模型 JSON（由 ic2_120:obj 加载器读取）。

坐标为原版模型单位（1 格 = 16），正面朝北（-Z，z 小的一侧）；导出时换算为方块空间 [0,1]。
UV 按世界坐标投影：同一材质在相邻长方体之间连续，像素密度处处一致（贴图 32px = 1 格）。

共享关系：通用机身（basic_body）独立成模型，各机器只导出自己的功能件，
blockstate 用 multipart 叠加两者；运动件（rotor）由方块实体渲染器绘制；物品模型合并全部部件。

用法：python3 models.py <assets/ic2_120 目录>
"""
import json
import sys
from pathlib import Path

NS = 'ic2_120'
TEX_DIR = 'block/machine3d'


class Model:
    def __init__(self):
        self.elements = []

    def box(self, x0, y0, z0, x1, y1, z1, tex, faces='nsewud', rot=None):
        """tex 可为字符串（全部面）或 {面: 材质} 字典；faces 控制输出哪些面。"""
        assert x0 < x1 and y0 < y1 and z0 < z1, (x0, y0, z0, x1, y1, z1)
        for v in (x0, y0, z0, x1, y1, z1):
            assert 0 <= v <= 16, v
        self.elements.append(dict(frm=(x0, y0, z0), to=(x1, y1, z1), tex=tex, faces=faces, rot=rot))
        return self

    def prism(self, axis, c0, c1, center, radius, sides, tex, phase=0.0):
        """沿 axis（'x'/'y'/'z'）的正多棱柱，c0..c1 为轴向范围，center 为另外两轴的中心。"""
        self.elements.append(dict(prism=(axis, c0, c1, center, radius, sides, phase), tex=tex))
        return self

    def poly(self, pts, tex):
        """任意平面凸多边形（顶点逆时针朝外，或由导出时按法线自动纠正）；用于刻面等斜面。"""
        for p in pts:
            for v in p:
                assert -0.001 <= v <= 16.001, v
        self.elements.append(dict(poly=[tuple(p) for p in pts], tex=tex))
        return self

    def extend(self, other):
        self.elements += other.elements
        return self


FACE_NAMES = dict(n='north', s='south', e='east', w='west', u='up', d='down')


def face_uv(face, frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if face == 'n':
        return [16 - x1, 16 - y1, 16 - x0, 16 - y0]
    if face == 's':
        return [x0, 16 - y1, x1, 16 - y0]
    if face == 'e':
        return [16 - z1, 16 - y1, 16 - z0, 16 - y0]
    if face == 'w':
        return [z0, 16 - y1, z1, 16 - y0]
    if face == 'u':
        return [x0, z0, x1, z1]
    return [x0, 16 - z1, x1, 16 - z0]


def r(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


def to_json(model, particle, parent=None):
    used = []
    elements = []
    for e in model.elements:
        faces = {}
        for f in e['faces']:
            tex = e['tex'] if isinstance(e['tex'], str) else e['tex'].get(f, e['tex'].get('*'))
            if tex is None:
                continue
            if tex not in used:
                used.append(tex)
            faces[FACE_NAMES[f]] = {'uv': [r(v) for v in face_uv(f, e['frm'], e['to'])], 'texture': '#' + tex}
        el = {'from': [r(v) for v in e['frm']], 'to': [r(v) for v in e['to']], 'faces': faces}
        if e['rot']:
            el['rotation'] = e['rot']
        elements.append(el)
    textures = {'particle': f'{NS}:{TEX_DIR}/{particle}'}
    for t in used:
        textures[t] = f'{NS}:{TEX_DIR}/{t}'
    out = {'parent': parent or 'minecraft:block/block', 'textures': textures, 'elements': elements}
    return out


# ---------------------------------------------------------------- 通用机身

def basic_body():
    """基础加工机共享机身：深色底座 + 钢壳 + 四角立柱 + 顶盖 + 侧百叶 + 背部电口。"""
    m = Model()
    # 底座与四只地脚
    m.box(1, 0.5, 1, 15, 2, 15, 'dark_steel')
    for (x, z) in ((0.5, 0.5), (12.5, 0.5), (0.5, 12.5), (12.5, 12.5)):
        m.box(x, 0, z, x + 3, 1, z + 3, 'rubber')
        m.box(x + 1, 1, z + 1, x + 2, 2.5, z + 2, 'steel', 'nsewu')  # 地脚螺栓
    # 主壳
    m.box(2, 2, 2, 14, 13, 14, 'steel')
    # 四角立柱
    for (x, z) in ((1.5, 1.5), (12.5, 1.5), (1.5, 12.5), (12.5, 12.5)):
        m.box(x, 2, z, x + 2, 13.5, z + 2, 'dark_steel')
    # 顶盖（略大于主壳，四边压一圈）
    m.box(1.5, 13, 1.5, 14.5, 14, 14.5, 'dark_steel')
    for (x, z) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        m.box(x, 14, z, x + 1, 14.5, z + 1, 'steel', 'nsewu')
    # 东西两侧百叶：5 条斜置散热片（伸出壳面 0.5）
    for side_x0, side_x1 in ((1.5, 2), (14, 14.5)):
        m.box(side_x0, 4, 4.5, side_x1, 11, 5, 'dark_steel')
        m.box(side_x0, 4, 11, side_x1, 11, 11.5, 'dark_steel')
        for i in range(5):
            y = 4.5 + i * 1.25
            m.box(side_x0, y, 5, side_x1, y + 0.75, 11, 'steel' if i % 2 == 0 else 'steel')
            m.box(side_x0 + (0.25 if side_x0 > 8 else 0), y - 0.25, 5, side_x1 - (0 if side_x0 > 8 else 0.25), y, 11, 'cavity')
    # 背部电口（南面）：黄铜接线盒 + 插孔
    m.box(5.5, 5, 14, 10.5, 10, 15, 'brass')
    m.box(6.5, 6, 15, 9.5, 9, 15.25, 'dark_steel', 'nsewu')
    m.box(7.5, 7, 15.25, 8.5, 8, 15.5, 'copper', 'sewud')
    # 侧板竖向加强筋（南北面）
    for x in (4.5, 11):
        m.box(x, 2.5, 14, x + 0.5, 12.5, 14.5, 'dark_steel')
    return m


# ---------------------------------------------------------------- 通用部件

def hopper(m, x0=3, z0=3, x1=13, z1=13):
    """顶部料斗：收口底座 + 四面围板 + 内腔。"""
    m.box(x0 + 1, 14, z0 + 1, x1 - 1, 15, z1 - 1, 'dark_steel')
    m.box(x0, 15, z0, x1, 16, z0 + 1, 'steel')
    m.box(x0, 15, z1 - 1, x1, 16, z1, 'steel')
    m.box(x0, 15, z0 + 1, x0 + 1, 16, z1 - 1, 'steel')
    m.box(x1 - 1, 15, z0 + 1, x1, 16, z1 - 1, 'steel')
    m.box(x0 + 1, 14.5, z0 + 1, x1 - 1, 15.25, z1 - 1, 'cavity', 'u')


def window(m, x0, y0, x1, y1, inner='cavity', frame='dark_steel'):
    """正面观察窗：0.75 宽外框（z 1.25–2）+ 贴在机身前的内衬。"""
    m.box(x0, y0, 1.25, x1, y0 + 0.75, 2, frame)
    m.box(x0, y1 - 0.75, 1.25, x1, y1, 2, frame)
    m.box(x0, y0 + 0.75, 1.25, x0 + 0.75, y1 - 0.75, 2, frame)
    m.box(x1 - 0.75, y0 + 0.75, 1.25, x1, y1 - 0.75, 2, frame)
    # 机身主壳正面在 z=2，内衬必须贴在它前面，否则被壳面挡住
    m.box(x0 + 0.75, y0 + 0.75, 1.9, x1 - 0.75, y1 - 0.75, 2, inner, 'n')


def front_trim(m, active, hazard=True):
    """铭牌 + 状态灯 + 下方警示条：所有基础加工机正面统一的识别元素。"""
    if hazard:
        m.box(3.5, 2.5, 1.5, 12.5, 3.5, 2, 'hazard', 'nud')
    m.box(4, 11.75, 1.75, 8, 12.5, 2, 'brass', 'nud')
    m.box(10.5, 11.5, 1.5, 11.5, 12.5, 2, 'lamp_on' if active else 'lamp_off', 'nsewud')


def pipe_y(m, x, z, y0, y1, r, tex, sides=8):
    m.prism('y', y0, y1, (x, z), r, sides, tex)


# 动画声明：导出到 machine3d/animations.json，由客户端通用渲染器读取，枢轴只在此处定义一次。
#   rotate: axis 绕轴、pivot 枢轴（模型单位）、speed 度/tick（正负为方向）
#   slide : axis 平移方向、amplitude 振幅（模型单位）、period 周期 tick
ANIMS = {}


def anim(machine, part, kind, **kw):
    ANIMS.setdefault(machine, []).append(dict(part=part, kind=kind, **kw))


# ---------------------------------------------------------------- 打粉机

def macerator(active):
    m = Model()
    hopper(m)
    window(m, 3.5, 4, 12.5, 11)
    front_trim(m, active)
    return m


# 研磨辊：两根横置 10 棱柱，前缘伸出观察窗
MACERATOR_ROTORS = ((6.5, 2.2), (9.0, 2.2))


def macerator_rotor(active, index):
    m = Model()
    y, z = MACERATOR_ROTORS[index]
    m.prism('x', 4.25, 11.75, (y, z), 1.15, 10, {'*': 'copper' if active else 'steel', 'cap': 'dark_steel'}, phase=0.31 * index)
    m.prism('x', 4.25, 11.75, (y, z), 0.55, 6, 'dark_steel')
    return m


# 10 棱柱 36° 旋转对称：转速不能整除 36，否则逐帧看起来静止
anim('macerator', 0, 'rotate', axis=[1, 0, 0], pivot=[8, *MACERATOR_ROTORS[0]], speed=7)
anim('macerator', 1, 'rotate', axis=[1, 0, 0], pivot=[8, *MACERATOR_ROTORS[1]], speed=-7)


# ---------------------------------------------------------------- 压缩机

def compressor(active):
    m = Model()
    window(m, 3.5, 4, 12.5, 11)
    # 砧座
    m.box(4.5, 4.75, 1.5, 11.5, 5.75, 1.9, 'dark_steel', 'nud')
    m.box(5.5, 5.75, 1.6, 10.5, 6, 1.9, 'steel', 'nu')
    front_trim(m, active)
    # 顶部液压缸：粗缸筒 + 端盖 + 两根油管
    m.prism('y', 14, 15.5, (8, 8), 3.2, 12, {'*': 'dark_steel', 'cap': 'dark_steel'})
    m.prism('y', 15.5, 16, (8, 8), 2.6, 12, {'*': 'steel', 'cap': 'steel'})
    m.prism('y', 15.5, 16, (8, 8), 0.8, 6, {'*': 'brass', 'cap': 'brass'})
    for x in (3, 12):
        m.box(x, 14, 11, x + 1, 15.5, 12, 'copper')
        m.box(min(x, 5), 15, 11, max(x + 1, 11), 15.5, 12, 'copper', 'nsud')
    # 压力表（顶面前沿）
    m.box(11, 14, 3, 13.5, 15, 4, 'dark_steel')
    m.box(11.25, 14.25, 2.9, 13.25, 14.75, 3, 'gauge', 'n')
    return m


def compressor_piston(active, index):
    m = Model()
    # 压头：顶杆 + 宽压板，静止位于窗口上部
    m.box(7.25, 8.5, 1.6, 8.75, 10.25, 1.9, 'steel', 'nsewd')
    m.box(5, 7.5, 1.4, 11, 8.5, 1.9, 'copper' if active else 'steel', 'nsewud')
    return m


anim('compressor', 0, 'slide', axis=[0, -1, 0], amplitude=1.5, period=20)


# ---------------------------------------------------------------- 提取机

def extractor(active):
    m = Model()
    window(m, 3.5, 3.75, 12.5, 11.25, inner='glass')
    # 下半腔液面
    m.box(4.25, 4.5, 1.85, 11.75, 6.5, 1.9, 'liquid', 'n')
    m.prism('z', 1.6, 1.9, (8, 7.5), 0.9, 8, {'*': 'dark_steel', 'cap': 'dark_steel'})
    front_trim(m, active, hazard=False)
    m.box(3.5, 2.5, 1.5, 12.5, 3.25, 2, 'dark_steel', 'nud')
    # 顶部两只收集罐（玻璃罐身 + 液体 + 钢箍）
    for x in (5, 11):
        pipe_y(m, x, 8, 14, 15.5, 1.8, {'*': 'glass', 'cap': 'steel'}, 10)
        pipe_y(m, x, 8, 14, 14.75, 1.85, 'liquid', 10)
        pipe_y(m, x, 8, 15.5, 16, 1.9, {'*': 'steel', 'cap': 'dark_steel'}, 10)
    m.box(6.5, 14.25, 7.5, 9.5, 14.75, 8.5, 'copper')
    return m


def extractor_impeller(active, index):
    m = Model()
    blade = 'copper' if active else 'steel'
    m.box(5, 7.25, 1.45, 11, 7.75, 1.6, blade, 'nsewud')
    m.box(7.75, 4.5, 1.45, 8.25, 10.5, 1.6, blade, 'nsewud')
    m.prism('z', 1.3, 1.6, (8, 7.5), 0.6, 6, {'*': 'brass', 'cap': 'brass'})
    return m


anim('extractor', 0, 'rotate', axis=[0, 0, 1], pivot=[8, 7.5, 1.5], speed=11)


# ---------------------------------------------------------------- 电炉

def electric_furnace(active):
    m = Model()
    # 炉门：整块深色门板 + 观察窗显示加热丝
    m.box(3, 3, 1.25, 13, 12, 2, 'dark_steel')
    m.box(4.5, 6, 1.15, 11.5, 10.5, 1.25, 'coil_on' if active else 'coil_off', 'n')
    m.box(4, 5.5, 1, 12, 6, 1.25, 'steel')
    m.box(4, 10.5, 1, 12, 11, 1.25, 'steel')
    m.box(4, 6, 1, 4.5, 10.5, 1.25, 'steel')
    m.box(11.5, 6, 1, 12, 10.5, 1.25, 'steel')
    # 门把手与铰链
    m.box(5, 4, 0.5, 11, 4.5, 1, 'brass')
    m.box(5, 4, 1, 5.5, 4.5, 1.25, 'brass')
    m.box(10.5, 4, 1, 11, 4.5, 1.25, 'brass')
    for y in (4, 10):
        m.box(12.5, y, 1, 13.25, y + 1.5, 1.75, 'steel')
    m.box(4, 12.5, 1.75, 8, 13, 2, 'brass', 'nud')
    m.box(10.5, 12.25, 1.5, 11.5, 12.9, 2, 'lamp_on' if active else 'lamp_off', 'nsewud')
    # 顶部排气：两根短烟囱 + 防雨帽
    for x in (5, 11):
        pipe_y(m, x, 10, 14, 15.5, 1.1, {'*': 'dark_steel', 'cap': 'cavity'})
        m.box(x - 1.5, 15.5, 8.5, x + 1.5, 16, 11.5, 'steel')
    m.box(4, 14, 3.5, 12, 14.5, 6.5, 'steel')
    for i in range(4):
        m.box(4.5 + i * 2, 14.5, 4, 5.5 + i * 2, 15, 6, 'dark_steel')
    return m


# ---------------------------------------------------------------- 回收机

def recycler(active):
    m = Model()
    # 加宽进料斗（偏后）
    hopper(m, 2.5, 4, 13.5, 13.5)
    window(m, 3, 4.5, 13, 11)
    # 下部出料口
    m.box(5, 2.25, 1, 11, 4, 2, 'dark_steel')
    m.box(5.75, 2.5, 0.9, 10.25, 3.5, 1, 'cavity', 'n')
    m.box(3, 2.5, 1.5, 5, 3.5, 2, 'hazard', 'nud')
    m.box(11, 2.5, 1.5, 13, 3.5, 2, 'hazard', 'nud')
    m.box(4, 11.75, 1.75, 8, 12.5, 2, 'brass', 'nud')
    m.box(10.5, 11.5, 1.5, 11.5, 12.5, 2, 'lamp_on' if active else 'lamp_off', 'nsewud')
    return m


def recycler_drum(active, index):
    m = Model()
    tooth = 'copper' if active else 'steel'
    m.prism('x', 3.75, 12.25, (7.75, 2.6), 1.6, 8, {'*': 'dark_steel', 'cap': 'steel'})
    # 交错刀齿：四列，每列在不同角度
    import math
    for i, x in enumerate((4.5, 6.5, 8.5, 10.5)):
        for k in range(2):
            a = (i * 45 + k * 180) * math.pi / 180
            cy, cz = 7.75 + 1.9 * math.sin(a), 2.6 - 1.9 * math.cos(a)
            m.box(x, cy - 0.4, cz - 0.4, x + 1, cy + 0.4, cz + 0.4, tooth)
    return m


anim('recycler', 0, 'rotate', axis=[1, 0, 0], pivot=[8, 7.75, 2.6], speed=-9)


# ---------------------------------------------------------------- 装罐机 / 固体装罐机

def canning_bay(m, active):
    window(m, 3.5, 3.5, 12.5, 11.5)
    # 输送平台 + 罐体
    m.box(4.25, 4.25, 1.4, 11.75, 5, 1.9, 'rubber', 'nud')
    pipe_y(m, 8, 2.75, 5, 7.5, 1.3, {'*': 'brass' if active else 'steel', 'cap': 'dark_steel'}, 10)
    m.box(4, 12, 1.75, 8, 12.75, 2, 'brass', 'nud')
    m.box(10.5, 11.9, 1.5, 11.5, 12.9, 2, 'lamp_on' if active else 'lamp_off', 'nsewud')
    m.box(3.5, 2.25, 1.5, 12.5, 3, 2, 'hazard', 'nud')


def canner(active):
    m = Model()
    canning_bay(m, active)
    # 顶部储液罐 + 液位窗
    pipe_y(m, 8, 8, 14, 16, 3.5, {'*': 'steel', 'cap': 'dark_steel'}, 12)
    m.box(7, 14.25, 4.45, 9, 15.75, 4.6, 'liquid', 'n')
    m.prism('y', 15.75, 16, (8, 8), 1, 6, {'*': 'brass', 'cap': 'brass'})
    return m


def canner_nozzle(active, index):
    m = Model()
    m.box(7.5, 8.5, 2.25, 8.5, 10.75, 3.25, 'steel', 'nsewd')
    m.box(6.75, 8, 2, 9.25, 8.75, 3.5, 'copper' if active else 'dark_steel', 'nsewud')
    return m


def solid_canner(active):
    m = Model()
    canning_bay(m, active)
    hopper(m, 4, 4, 12, 12)
    return m


def solid_canner_stamp(active, index):
    m = Model()
    m.box(7.25, 9, 2.25, 8.75, 10.75, 3.25, 'steel', 'nsewd')
    m.box(6.25, 8.25, 1.75, 9.75, 9, 3.75, 'dark_steel', 'nsewud')
    m.box(6.5, 8, 2, 9.5, 8.25, 3.5, 'copper' if active else 'steel', 'nsewd')
    return m


anim('canner', 0, 'slide', axis=[0, -1, 0], amplitude=0.75, period=30)
anim('solid_canner', 0, 'slide', axis=[0, -1, 0], amplitude=0.75, period=16)


# ---------------------------------------------------------------- 铁炉（独立铸铁机身）

def iron_body():
    m = Model()
    m.box(1, 0, 1, 15, 1.5, 15, 'cast_iron')
    m.box(1.5, 1.5, 1.5, 14.5, 13, 14.5, 'cast_iron')
    m.box(1, 13, 1, 15, 14, 15, 'cast_iron')
    # 加强箍
    for y in (3, 10.5):
        m.box(1.25, y, 1.25, 14.75, y + 0.75, 14.75, 'dark_steel', 'nsew')
    for (x, z) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        m.box(x, 0, z, x + 1, 14, z + 1, 'dark_steel')
    # 后部烟囱
    pipe_y(m, 8, 11.5, 14, 16, 1.6, {'*': 'dark_steel', 'cap': 'cavity'}, 10)
    m.prism('y', 15.25, 15.75, (8, 11.5), 1.9, 10, {'*': 'cast_iron', 'cap': 'cast_iron'})
    return m


def iron_furnace(active):
    m = Model()
    fire = 'ember' if active else 'cavity'
    # 火门：带格栅的炉门，格栅间露出火光
    m.box(4, 6, 1, 12, 11, 1.5, 'cast_iron')
    m.box(4.5, 6.5, 0.9, 11.5, 10.5, 1, fire, 'n')
    for i in range(6):
        m.box(4.75 + i * 1.15, 6.5, 0.6, 5.25 + i * 1.15, 10.5, 0.9, 'dark_steel')
    m.box(4, 10.5, 0.6, 12, 11, 0.9, 'dark_steel')
    m.box(4, 6, 0.6, 12, 6.5, 0.9, 'dark_steel')
    # 门把与铰链
    m.box(10.5, 8, 0.1, 11.5, 9, 0.6, 'brass')
    m.box(3.25, 6.5, 1, 4, 7.5, 1.5, 'dark_steel')
    m.box(3.25, 9.5, 1, 4, 10.5, 1.5, 'dark_steel')
    # 灰门
    m.box(5, 2, 1, 11, 4.5, 1.5, 'cast_iron')
    m.box(5.5, 2.5, 0.9, 10.5, 3, 1, 'ember' if active else 'cavity', 'n')
    m.box(7, 3.5, 0.5, 9, 4, 1, 'brass')
    return m


# ---------------------------------------------------------------- 高级机器共享机身

def advanced_body():
    """高级加工机共享机身：加厚底座、钴蓝涂装侧板、双立柱框架、加高顶盖、背部重型电口。"""
    m = Model()
    m.box(0.5, 0, 0.5, 15.5, 1.5, 15.5, 'dark_steel')
    for (x, z) in ((0.5, 0.5), (13.5, 0.5), (0.5, 13.5), (13.5, 13.5)):
        m.box(x, 0, z, x + 2, 0.5, z + 2, 'rubber', 'nsewd')
    m.box(1.5, 1.5, 1.5, 14.5, 13.5, 14.5, 'steel')
    # 涂装侧板（东西南三面），外凸 0.25
    for x0, x1 in ((1.25, 1.5), (14.5, 14.75)):
        m.box(x0, 3, 3.5, x1, 12, 12.5, 'painted', 'ewud')
    m.box(3.5, 3, 14.5, 12.5, 12, 14.75, 'painted', 'sud')
    # 侧板散热格栅（两列竖缝）
    for x0, x1 in ((1, 1.25), (14.75, 15)):
        for z in (5, 6.5, 8, 9.5, 11):
            m.box(x0, 4.5, z, x1, 10.5, z + 0.5, 'cavity', 'ew')
    # 四角双立柱
    for (x, z) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        m.box(x, 1.5, z, x + 2, 14, z + 2, 'dark_steel')
        m.box(x + 0.5, 1.5, z + 0.5, x + 1.5, 14.25, z + 1.5, 'steel', 'u')
    # 顶盖 + 顶边护栏
    m.box(1, 13.5, 1, 15, 14.5, 15, 'dark_steel')
    for (x, z) in ((2.5, 2.5), (12.5, 2.5), (2.5, 12.5), (12.5, 12.5)):
        m.box(x, 14.5, z, x + 1, 15, z + 1, 'brass', 'nsewu')
    # 背部重型电口：两组铜排
    m.box(5, 4.5, 14.75, 11, 10.5, 15.5, 'dark_steel')
    for x in (6, 9):
        m.box(x, 5.5, 15.5, x + 1, 9.5, 15.9, 'copper', 'sewud')
    m.box(5.5, 10.5, 14.75, 10.5, 11, 15.25, 'hazard', 'sud')
    return m


def adv_trim(m, active):
    """高级机正面识别：顶部双灯 + 黄铜铭牌 + 底部警示条（位置比基础机更高更宽）。"""
    m.box(3, 12, 1.25, 9, 13, 1.5, 'brass', 'nud')
    m.box(10.5, 12, 1, 11.5, 13, 1.5, 'lamp_on' if active else 'lamp_off', 'nsewud')
    m.box(12, 12, 1, 13, 13, 1.5, 'lamp_on' if active else 'lamp_red', 'nsewud')
    m.box(3, 2, 1.25, 13, 3, 1.5, 'hazard', 'nud')


def adv_window(m, x0, y0, x1, y1, inner='cavity'):
    # 高级机身正面在 z=1.5
    m.box(x0, y0, 0.75, x1, y0 + 0.75, 1.5, 'dark_steel')
    m.box(x0, y1 - 0.75, 0.75, x1, y1, 1.5, 'dark_steel')
    m.box(x0, y0 + 0.75, 0.75, x0 + 0.75, y1 - 0.75, 1.5, 'dark_steel')
    m.box(x1 - 0.75, y0 + 0.75, 0.75, x1, y1 - 0.75, 1.5, 'dark_steel')
    m.box(x0 + 0.75, y0 + 0.75, 1.4, x1 - 0.75, y1 - 0.75, 1.5, inner, 'n')


# ---------------------------------------------------------------- 高炉

def blast_furnace(active):
    m = Model()
    # 正面耐火炉膛：砖砌拱门 + 火口
    m.box(3, 3.5, 1, 13, 11.5, 1.5, 'refractory')
    m.box(5, 5, 0.9, 11, 10, 1, 'ember' if active else 'cavity', 'n')
    m.box(4.5, 10, 0.5, 11.5, 10.75, 1, 'refractory')
    m.box(4.5, 4.5, 0.5, 5, 10, 1, 'refractory')
    m.box(11, 4.5, 0.5, 11.5, 10, 1, 'refractory')
    adv_trim(m, active)
    # 顶部：耐火烟囱 + 热风管
    pipe_y(m, 8, 9, 14.5, 16, 2.6, {'*': 'refractory', 'cap': 'cavity'}, 12)
    m.prism('y', 15.5, 16, (8, 9), 2.9, 12, {'*': 'dark_steel', 'cap': 'dark_steel'})
    m.box(3, 14.5, 3, 5, 15.5, 11, 'copper')
    m.box(3, 15, 9, 5.5, 15.75, 10, 'copper')
    return m


# ---------------------------------------------------------------- 方块切割机

def block_cutter(active):
    m = Model()
    adv_window(m, 2.5, 3.5, 13.5, 11.5)
    # 工作台 + 待切方块
    m.box(3.25, 4.25, 1, 12.75, 5, 1.4, 'dark_steel', 'nud')
    m.box(4, 5, 1.1, 6.5, 7.5, 1.4, 'steel', 'nsewu')
    adv_trim(m, active)
    m.box(4, 14.5, 4, 12, 15, 12, 'steel')
    m.box(5, 15, 5, 11, 15.5, 11, 'painted')
    return m


def block_cutter_saw(active, index):
    m = Model()
    m.prism('z', 0.9, 1.05, (9.5, 7.5), 3.2, 16, {'*': 'steel', 'cap': 'copper' if active else 'steel'})
    m.prism('z', 0.75, 0.9, (9.5, 7.5), 0.9, 8, {'*': 'dark_steel', 'cap': 'dark_steel'})
    # 锯齿
    import math
    for k in range(8):
        a = k * math.pi / 4
        cx, cy = 9.5 + 3.4 * math.cos(a), 7.5 + 3.4 * math.sin(a)
        m.box(cx - 0.35, cy - 0.35, 0.9, cx + 0.35, cy + 0.35, 1.05, 'steel')
    return m


anim('block_cutter', 0, 'rotate', axis=[0, 0, 1], pivot=[9.5, 7.5, 1], speed=-23)


# ---------------------------------------------------------------- 离心机

def centrifuge(active):
    m = Model()
    adv_trim(m, active)
    # 正面：样品管架 + 压力表
    adv_window(m, 3, 4, 13, 11)
    for i, x in enumerate((4.5, 6.5, 8.5, 10.5)):
        m.box(x, 5, 1.1, x + 1, 9.5, 1.4, 'glass', 'nsewu')
        m.box(x, 5, 1.05, x + 1, 5 + 1 + i, 1.1, 'liquid', 'n')
    # 顶部：玻璃罩 + 转鼓底座
    m.prism('y', 14.5, 15, (8, 8), 5.5, 16, {'*': 'dark_steel', 'cap': 'dark_steel'})
    for k in range(4):
        import math
        a = k * math.pi / 2 + math.pi / 4
        x, z = 8 + 5 * math.cos(a), 8 + 5 * math.sin(a)
        m.box(x - 0.4, 15, z - 0.4, x + 0.4, 16, z + 0.4, 'brass')
    return m


def centrifuge_rotor(active, index):
    m = Model()
    m.prism('y', 15, 15.25, (8, 8), 4.5, 16, {'*': 'steel', 'cap': 'steel'})
    for k in range(4):
        import math
        a = k * math.pi / 2
        x, z = 8 + 3 * math.cos(a), 8 + 3 * math.sin(a)
        m.prism('y', 15.25, 16, (x, z), 0.8, 8, {'*': 'glass', 'cap': 'liquid' if active else 'steel'})
    m.prism('y', 15.25, 15.9, (8, 8), 0.9, 8, {'*': 'copper', 'cap': 'brass'})
    return m


anim('centrifuge', 0, 'rotate', axis=[0, 1, 0], pivot=[8, 15, 8], speed=29)


# ---------------------------------------------------------------- 发酵机

def fermenter(active):
    m = Model()
    adv_window(m, 3, 3.5, 13, 11.5, inner='glass')
    m.box(3.75, 4.25, 1.3, 12.25, 8.5 if active else 7, 1.4, 'biomass', 'nu')
    for x in (5, 8, 11):
        m.box(x, 8.75, 1.3, x + 0.5, 9.25, 1.4, 'biomass' if active else 'glass', 'n')
    adv_trim(m, active)
    # 顶部：两只发酵罐盖 + 弯管
    for x in (5, 11):
        pipe_y(m, x, 8, 14.5, 15.5, 2.2, {'*': 'steel', 'cap': 'dark_steel'}, 12)
        pipe_y(m, x, 8, 15.5, 16, 1, {'*': 'copper', 'cap': 'cavity'}, 8)
    m.box(5.5, 15.25, 7.5, 10.5, 15.75, 8.5, 'copper')
    return m


# ---------------------------------------------------------------- 感应炉

def induction_furnace(active):
    m = Model()
    coil = 'coil_on' if active else 'coil_off'
    for x0 in (2.5, 8.5):
        adv_window(m, x0, 4, x0 + 5, 11, inner=coil)
    adv_trim(m, active)
    # 顶部铜感应线圈：三圈叠环
    for k, y in enumerate((14.5, 15, 15.5)):
        m.prism('y', y, y + 0.4, (8, 8), 4.5, 12, {'*': 'copper', 'cap': 'copper'})
    m.prism('y', 14.5, 16, (8, 8), 3, 12, {'*': 'dark_steel', 'cap': coil})
    return m


# ---------------------------------------------------------------- 金属成型机

METAL_FORMER_ROLLS = ((6.25, 1.6), (9.25, 1.6))


def metal_former(active):
    m = Model()
    adv_window(m, 2.5, 3.5, 13.5, 11.5)
    # 出料导板
    m.box(3.25, 4.25, 0.8, 12.75, 4.75, 1.4, 'steel', 'nud')
    m.box(3.25, 7.6, 1.2, 12.75, 7.9, 1.4, 'copper' if active else 'steel', 'nud')
    adv_trim(m, active)
    m.box(3, 14.5, 3, 13, 15.25, 13, 'steel')
    for x in (4, 7, 10):
        m.box(x, 15.25, 4, x + 2, 16, 12, 'dark_steel')
    return m


def metal_former_roll(active, index):
    m = Model()
    y, z = METAL_FORMER_ROLLS[index]
    m.prism('x', 3.25, 12.75, (y, z), 1.3, 12, {'*': 'steel', 'cap': 'brass'}, phase=0.1 * index)
    for x in (4.5, 8, 11.5):
        m.prism('x', x - 0.25, x + 0.25, (y, z), 1.45, 12, {'*': 'dark_steel', 'cap': 'dark_steel'})
    return m


anim('metal_former', 0, 'rotate', axis=[1, 0, 0], pivot=[8, *METAL_FORMER_ROLLS[0]], speed=8)
anim('metal_former', 1, 'rotate', axis=[1, 0, 0], pivot=[8, *METAL_FORMER_ROLLS[1]], speed=-8)


# ---------------------------------------------------------------- 洗矿机

def ore_washing_plant(active):
    m = Model()
    adv_window(m, 2.5, 3.5, 13.5, 11.5, inner='glass')
    m.box(3.25, 4.25, 1.3, 12.75, 7.25, 1.4, 'liquid', 'nu')
    adv_trim(m, active)
    # 顶部进水管 + 阀门手轮
    pipe_y(m, 11, 5, 14.5, 16, 1.2, {'*': 'copper', 'cap': 'cavity'})
    m.prism('y', 15.25, 15.6, (11, 5), 2, 8, {'*': 'lamp_red', 'cap': 'lamp_red'})
    m.box(3, 14.5, 7, 9, 15.5, 13, 'steel')
    m.box(3.5, 15.5, 7.5, 8.5, 15.75, 12.5, 'cavity', 'u')
    return m


def ore_washing_wheel(active, index):
    m = Model()
    m.prism('x', 3.5, 12.5, (6.5, 1.8), 0.6, 8, {'*': 'dark_steel', 'cap': 'brass'})
    for k in range(6):
        import math
        a = k * math.pi / 3
        cy, cz = 6.5 + 1.5 * math.sin(a), 1.8 - 1.5 * math.cos(a)
        m.box(4, cy - 0.3, cz - 0.3, 12, cy + 0.3, cz + 0.3, 'copper' if active else 'steel')
    return m


anim('ore_washing_plant', 0, 'rotate', axis=[1, 0, 0], pivot=[8, 6.5, 1.8], speed=-5)


# ================================================================ 流体 / 农业 / 杂项（基础机身）

def round_window(m, cx, cy, r, inner, frame='dark_steel', sides=12, z_front=1.25):
    """圆形观察窗：外环 + 内衬（基础机身，壳面 z=2）。"""
    m.prism('z', z_front, 2, (cx, cy), r + 0.75, sides, {'*': frame, 'cap': frame})
    m.prism('z', z_front - 0.1, z_front, (cx, cy), r, sides, {'*': inner, 'cap': inner})


def fan(active, cx, cy, z0, z1, r, blades=4):
    m = Model()
    import math
    blade = 'copper' if active else 'steel'
    for k in range(blades):
        a = k * 2 * math.pi / blades
        for t in (0.35, 0.6, 0.85):
            x, y = cx + r * t * math.cos(a), cy + r * t * math.sin(a)
            w = 0.55 * (1.1 - t * 0.4)
            m.box(x - w, y - w, z0, x + w, y + w, z1, blade, 'nsewud')
    m.prism('z', z0 - 0.1, z1, (cx, cy), r * 0.25, 8, {'*': 'brass', 'cap': 'brass'})
    return m


def pump(active):
    m = Model()
    round_window(m, 8, 7.5, 3, 'cavity')
    front_trim(m, active)
    # 侧出水法兰（东侧）+ 顶部进水管
    m.prism('x', 14, 15.75, (7.5, 8), 1.6, 10, {'*': 'copper', 'cap': 'cavity'})
    m.prism('x', 15.25, 15.75, (7.5, 8), 2.1, 10, {'*': 'dark_steel', 'cap': 'dark_steel'})
    pipe_y(m, 8, 8, 14, 16, 1.6, {'*': 'copper', 'cap': 'cavity'}, 10)
    m.prism('y', 15.25, 15.75, (8, 8), 2.1, 10, {'*': 'dark_steel', 'cap': 'dark_steel'})
    return m


def pump_impeller(active, index):
    return fan(active, 8, 7.5, 1.0, 1.15, 3, blades=6)


anim('pump', 0, 'rotate', axis=[0, 0, 1], pivot=[8, 7.5, 1], speed=17)


def fluid_canner(active):
    m = Model()
    canning_bay(m, active)
    # 顶部双液罐（左输入、右输出）
    for x in (4.75, 11.25):
        pipe_y(m, x, 8, 14, 16, 2.4, {'*': 'steel', 'cap': 'dark_steel'}, 10)
        m.box(x - 0.75, 14.25, 5.55, x + 0.75, 15.75, 5.7, 'liquid', 'n')
    m.box(7, 15, 7.5, 9, 15.5, 8.5, 'copper')
    return m


anim('fluid_canner', 0, 'slide', axis=[0, -1, 0], amplitude=0.75, period=24)


def condenser(active):
    m = Model()
    round_window(m, 8, 7.5, 3.25, 'cavity')
    # 环绕风扇的散热翅片（左右两组竖片）
    for x in (3, 3.75, 11.75, 12.5):
        m.box(x, 4, 1.25, x + 0.4, 11, 2, 'steel', 'nsewu')
    front_trim(m, active)
    # 顶部冷凝盘管
    for z in (4, 6, 8, 10, 12):
        m.prism('x', 3, 13, (14.6, z), 0.6, 6, {'*': 'copper', 'cap': 'copper'})
    m.box(3, 14, 3.5, 4, 15.25, 12.5, 'copper')
    m.box(12, 14, 3.5, 13, 15.25, 12.5, 'copper')
    return m


def condenser_fan(active, index):
    return fan(active, 8, 7.5, 1.0, 1.15, 3.2, blades=4)


anim('condenser', 0, 'rotate', axis=[0, 0, 1], pivot=[8, 7.5, 1], speed=-21)


def solar_distiller(active):
    m = Model()
    window(m, 3.5, 4, 12.5, 10, inner='glass')
    m.box(4.25, 4.75, 1.85, 11.75, 6.5, 1.9, 'liquid', 'n')
    front_trim(m, active, hazard=False)
    # 顶部倾斜集光玻璃：阶梯近似斜面（前低后高）
    for k in range(5):
        z0 = 2 + k * 2.4
        m.box(2.5, 14, z0, 13.5, 14.5 + k * 0.3, z0 + 2.4, {'u': 'glass', 'n': 'steel', '*': 'dark_steel'}, 'nsewu')
    m.box(2, 14, 1.5, 14, 14.4, 2, 'steel')
    return m


def crop_harvester(active):
    m = Model()
    window(m, 3.5, 4, 12.5, 11, inner='biomass')
    front_trim(m, active)
    # 顶部收割刀头底座
    m.prism('y', 14, 14.75, (8, 8), 4, 12, {'*': 'dark_steel', 'cap': 'steel'})
    return m


def crop_harvester_head(active, index):
    m = Model()
    m.prism('y', 14.75, 15.25, (8, 8), 1, 8, {'*': 'brass', 'cap': 'brass'})
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        x0, x1 = sorted((8 + dx * 0.5, 8 + dx * 5)) if dx else (7.6, 8.4)
        z0, z1 = sorted((8 + dz * 0.5, 8 + dz * 5)) if dz else (7.6, 8.4)
        m.box(x0, 14.85, z0, x1, 15.15, z1, 'copper' if active else 'steel')
    return m


anim('crop_harvester', 0, 'rotate', axis=[0, 1, 0], pivot=[8, 15, 8], speed=13)


def cropmatron(active):
    m = Model()
    window(m, 3.5, 4, 12.5, 11, inner='glass')
    m.box(4.25, 4.75, 1.85, 11.75, 7.5, 1.9, 'biomass', 'n')
    front_trim(m, active)
    pipe_y(m, 8, 8, 14, 15.5, 1, {'*': 'copper', 'cap': 'copper'})
    for x in (4.5, 11.5):
        pipe_y(m, x, 11.5, 14, 15.75, 1.4, {'*': 'steel', 'cap': 'biomass'}, 8)
    return m


def cropmatron_sprinkler(active, index):
    m = Model()
    m.box(5, 15.5, 7.6, 11, 15.9, 8.4, 'brass' if active else 'steel')
    m.box(4.6, 15.4, 7.4, 5.2, 16, 8.6, 'copper')
    m.box(10.8, 15.4, 7.4, 11.4, 16, 8.6, 'copper')
    return m


anim('cropmatron', 0, 'rotate', axis=[0, 1, 0], pivot=[8, 15.5, 8], speed=9)


def animalmatron(active):
    m = Model()
    # 正面饲料槽
    m.box(3.5, 4, 0.75, 12.5, 7, 2, 'dark_steel')
    m.box(4.25, 6.5, 0.85, 11.75, 7.05, 1.9, 'biomass', 'u')
    window(m, 3.5, 7.5, 12.5, 11, inner='glass')
    front_trim(m, active)
    hopper(m, 4, 4, 12, 12)
    return m


def animal_slaughterer(active):
    m = Model()
    round_window(m, 8, 7.5, 3, 'cavity', frame='hazard')
    m.box(4, 11.75, 1.75, 8, 12.5, 2, 'brass', 'nud')
    m.box(10.5, 11.5, 1.5, 11.5, 12.5, 2, 'lamp_red' if active else 'lamp_off', 'nsewud')
    m.box(3.5, 2.5, 1.5, 12.5, 3.5, 2, 'hazard', 'nud')
    m.box(4, 14, 4, 12, 14.75, 12, 'dark_steel')
    return m


def animal_slaughterer_blade(active, index):
    m = Model()
    m.prism('z', 1.0, 1.15, (8, 7.5), 2.7, 12, {'*': 'steel', 'cap': 'lamp_red' if active else 'steel'})
    m.prism('z', 0.9, 1.0, (8, 7.5), 0.8, 6, {'*': 'dark_steel', 'cap': 'dark_steel'})
    return m


anim('animal_slaughterer', 0, 'rotate', axis=[0, 0, 1], pivot=[8, 7.5, 1], speed=-31)


def miner(active):
    m = Model()
    window(m, 3.5, 4, 12.5, 11)
    # 窗内钻管组
    for x in (5, 7, 9, 11):
        m.box(x - 0.6, 4.75, 1.55, x + 0.6, 10.25, 1.9, 'steel', 'nsewu')
    front_trim(m, active)
    # 顶部钻机马达
    m.prism('y', 14, 15.75, (8, 8), 2.6, 10, {'*': 'dark_steel', 'cap': 'steel'})
    m.prism('y', 15.75, 16, (8, 8), 1.2, 8, {'*': 'brass', 'cap': 'brass'})
    return m


def magnetizer(active):
    m = Model()
    window(m, 3.5, 4, 12.5, 11)
    for y in (5.5, 7, 8.5):
        m.box(4.25, y, 1.6, 11.75, y + 0.75, 1.9, 'copper', 'nud')
    front_trim(m, active)
    # 顶部电磁铁：铁芯 + 线圈
    for x in (5, 11):
        pipe_y(m, x, 8, 14, 15.5, 2, {'*': 'copper', 'cap': 'dark_steel'}, 10)
    m.box(4, 15.5, 7, 12, 16, 9, 'dark_steel')
    return m


def tesla_coil(active):
    m = Model()
    window(m, 4.5, 4, 11.5, 10, inner='screen_on' if active else 'screen_off')
    front_trim(m, active, hazard=True)
    # 顶部初级线圈 + 放电球
    m.prism('y', 14, 14.5, (8, 8), 4.5, 12, {'*': 'dark_steel', 'cap': 'dark_steel'})
    for k in range(4):
        m.prism('y', 14.5 + k * 0.25, 14.7 + k * 0.25, (8, 8), 3.5 - k * 0.6, 12, {'*': 'copper', 'cap': 'copper'})
    pipe_y(m, 8, 8, 14.5, 15.25, 0.8, {'*': 'steel', 'cap': 'steel'})
    m.prism('y', 15.25, 16, (8, 8), 1.8, 10, {'*': 'lamp_on' if active else 'steel', 'cap': 'lamp_on' if active else 'steel'})
    return m


def chunk_loader(active):
    m = Model()
    window(m, 3.5, 4, 12.5, 11, inner='screen_on' if active else 'screen_off')
    front_trim(m, active)
    m.prism('y', 14, 14.5, (8, 8), 2.5, 10, {'*': 'dark_steel', 'cap': 'dark_steel'})
    pipe_y(m, 8, 8, 14.5, 15, 0.6, {'*': 'steel', 'cap': 'steel'})
    return m


def chunk_loader_dish(active, index):
    m = Model()
    # 雷达天线：斜置碟面 + 馈源杆
    m.box(4.5, 15, 7, 11.5, 15.5, 9, 'steel')
    m.box(5.5, 15.5, 7, 10.5, 16, 9, 'steel')
    m.box(7.5, 15.5, 9, 8.5, 16, 11, 'brass')
    m.box(7.75, 15.25, 11, 8.25, 15.75, 11.5, 'lamp_on' if active else 'lamp_off')
    return m


anim('chunk_loader', 0, 'rotate', axis=[0, 1, 0], pivot=[8, 15, 8], speed=4)


# ================================================================ 蒸汽 / UU / 高级采矿（高级机身）

def steam_generator(active):
    m = Model()
    adv_window(m, 3, 3.5, 9.5, 10, inner='ember' if active else 'cavity')
    # 右侧两只压力表
    for y in (8, 4.5):
        m.prism('z', 0.9, 1.5, (11.25, y + 1.25), 1.5, 12, {'*': 'dark_steel', 'cap': 'gauge'})
    adv_trim(m, active)
    for x in (4.5, 11.5):
        pipe_y(m, x, 8, 14.5, 16, 1.2, {'*': 'steel', 'cap': 'cavity'})
    m.box(5.5, 15, 7.5, 10.5, 15.75, 8.5, 'steel')
    return m


def liquid_heat_exchanger(active):
    m = Model()
    adv_window(m, 2.5, 3.5, 13.5, 11.5)
    # 窗内蛇形换热管
    for k, y in enumerate((4.75, 6.5, 8.25, 10)):
        m.box(3.25, y, 1.1, 12.75, y + 0.75, 1.4, 'copper', 'nud')
        x = 3.25 if k % 2 else 12
        if k < 3:
            m.box(x, y + 0.75, 1.1, x + 0.75, y + 1.75, 1.4, 'copper', 'nsew')
    adv_trim(m, active)
    for x in (4, 12):
        m.prism('y', 14.5, 16, (x, 8), 1.4, 10, {'*': 'liquid' if x < 8 else 'ember' if active else 'cavity', 'cap': 'dark_steel'})
    return m


def advanced_miner(active):
    m = Model()
    adv_window(m, 3, 3.5, 13, 11)
    for x in (4.5, 6.5, 8.5, 10.5):
        m.box(x, 4.25, 1.05, x + 1, 10.25, 1.4, 'steel', 'nsewu')
    adv_trim(m, active)
    m.prism('y', 14.5, 16, (8, 8), 3.5, 12, {'*': 'painted', 'cap': 'dark_steel'})
    m.prism('y', 15.5, 16, (8, 8), 1.5, 8, {'*': 'brass', 'cap': 'lamp_on' if active else 'lamp_off'})
    return m


def matter_generator(active):
    m = Model()
    adv_window(m, 3, 3.5, 13, 11.5, inner='glass')
    m.box(3.75, 4.25, 1.3, 12.25, 9 if active else 6, 1.4, 'uu_on' if active else 'uu_off', 'nu')
    adv_trim(m, active)
    # 顶部场发生器：四柱 + 中心球
    for (x, z) in ((4, 4), (12, 4), (4, 12), (12, 12)):
        pipe_y(m, x, z, 14.5, 16, 0.7, {'*': 'copper', 'cap': 'brass'})
    m.prism('y', 14.5, 15.75, (8, 8), 2, 10, {'*': 'uu_on' if active else 'uu_off', 'cap': 'uu_on' if active else 'uu_off'})
    return m


def replicator(active):
    m = Model()
    adv_window(m, 3, 3.5, 13, 11.5, inner='cavity')
    # 舱内：底座 + 发射器
    m.box(5, 4.25, 1.1, 11, 5, 1.4, 'dark_steel')
    m.box(6.5, 5, 1.2, 9.5, 5.5, 1.4, 'uu_on' if active else 'uu_off')
    m.box(5.5, 10, 1.1, 10.5, 10.75, 1.4, 'uu_on' if active else 'uu_off')
    adv_trim(m, active)
    m.prism('y', 14.5, 15.5, (8, 8), 4.5, 12, {'*': 'steel', 'cap': 'glass'})
    m.prism('y', 15.5, 16, (8, 8), 2.5, 12, {'*': 'glass', 'cap': 'uu_on' if active else 'uu_off'})
    return m


def uu_scanner(active):
    m = Model()
    adv_window(m, 3, 6.5, 13, 11.5, inner='screen_on' if active else 'screen_off')
    # 扫描床
    m.box(3, 3.5, 0.5, 13, 5.5, 1.5, 'dark_steel')
    m.box(3.75, 5.5, 0.75, 12.25, 5.75, 1.5, 'lamp_on' if active else 'steel', 'nu')
    adv_trim(m, active)
    m.box(4, 14.5, 4, 12, 15, 12, 'steel')
    m.box(5, 15, 5, 11, 15.4, 11, 'screen_on' if active else 'screen_off', 'u')
    return m


def pattern_storage(active):
    m = Model()
    # 正面晶体抽屉：3 层
    for k, y in enumerate((3.5, 6.5, 9.5)):
        m.box(3, y, 0.75, 13, y + 2.5, 1.5, 'dark_steel')
        m.box(3.75, y + 0.5, 0.65, 12.25, y + 2, 0.75, 'uu_off', 'n')
        m.box(7, y + 1, 0.35, 9, y + 1.5, 0.65, 'brass')
    m.box(3, 12, 1.25, 9, 13, 1.5, 'brass', 'nud')
    m.box(4, 14.5, 4, 12, 15.25, 12, 'painted')
    return m


# ================================================================ 发电机共享机身

def generator_body():
    """发电机共享机身：低重心铸铁底座 + 钢壳 + 黄铜输出端子条（顶部前沿）+ 两侧大散热栅。"""
    m = Model()
    m.box(0.5, 0, 0.5, 15.5, 2, 15.5, 'cast_iron')
    for x in (0.5, 14.5):
        m.box(x, 2, 1, x + 1, 2.5, 15, 'dark_steel', 'nsewu')
    m.box(1.5, 2, 1.5, 14.5, 13, 14.5, 'steel')
    # 四角斜撑立柱
    for (x, z) in ((1, 1), (13.5, 1), (1, 13.5), (13.5, 13.5)):
        m.box(x, 2, z, x + 1.5, 13.5, z + 1.5, 'dark_steel')
    m.box(1, 13, 1, 15, 14, 15, 'dark_steel')
    # 两侧大散热栅：横向深槽
    for x0, x1 in ((1.25, 1.5), (14.5, 14.75)):
        m.box(x0, 3.5, 3, x1, 11.5, 13, 'dark_steel', 'ew')
        for y in range(4, 11, 2):
            m.box(x0 - 0.05 if x0 < 8 else x1, y, 3.5, x0 if x0 < 8 else x1 + 0.05, y + 1, 12.5, 'cavity', 'ew')
    # 背部输出端子：黄铜接线柱 ×2 + 警示标
    m.box(4, 6, 14.5, 12, 11, 15.25, 'dark_steel')
    for x in (5.5, 9.5):
        m.prism('z', 15.25, 15.9, (x + 0.5, 8.5), 0.9, 8, {'*': 'brass', 'cap': 'copper'})
    m.box(4.5, 11, 14.5, 11.5, 11.75, 15, 'hazard', 'sud')
    return m


def gen_trim(m, active):
    """发电机正面：输出指示灯（工作亮绿）+ 铭牌，放在左上（与加工机的右上区分）。"""
    m.box(8, 11.5, 1.25, 12, 12.25, 1.5, 'brass', 'nud')
    m.box(4.5, 11.25, 1, 5.5, 12.25, 1.5, 'lamp_on' if active else 'lamp_off', 'nsewud')
    m.box(6, 11.25, 1, 7, 12.25, 1.5, 'lamp_on' if active else 'lamp_off', 'nsewud')


def gen_window(m, x0, y0, x1, y1, inner):
    # 发电机壳面在 z=1.5
    m.box(x0, y0, 0.75, x1, y0 + 0.75, 1.5, 'dark_steel')
    m.box(x0, y1 - 0.75, 0.75, x1, y1, 1.5, 'dark_steel')
    m.box(x0, y0 + 0.75, 0.75, x0 + 0.75, y1 - 0.75, 1.5, 'dark_steel')
    m.box(x1 - 0.75, y0 + 0.75, 0.75, x1, y1 - 0.75, 1.5, 'dark_steel')
    m.box(x0 + 0.75, y0 + 0.75, 1.4, x1 - 0.75, y1 - 0.75, 1.5, inner, 'n')


def generator(active):
    m = Model()
    # 火力发电机：火箱门 + 燃料投料口
    gen_window(m, 4, 4.5, 12, 10, 'ember' if active else 'cavity')
    for x in (5.5, 7.5, 9.5):
        m.box(x, 5.25, 1, x + 0.6, 9.25, 1.4, 'dark_steel')
    m.box(4, 2.5, 1, 12, 4, 1.5, 'cast_iron')
    m.box(10, 3, 0.6, 11.5, 3.5, 1, 'brass')
    gen_trim(m, active)
    pipe_y(m, 11, 10.5, 14, 16, 1.3, {'*': 'dark_steel', 'cap': 'cavity'})
    m.prism('y', 15.5, 16, (11, 10.5), 1.7, 10, {'*': 'cast_iron', 'cap': 'cast_iron'})
    return m


def geo_generator(active):
    m = Model()
    gen_window(m, 3.5, 3, 12.5, 10.5, 'glass')
    m.box(4.25, 3.75, 1.3, 11.75, 7.5 if active else 5, 1.4, 'ember', 'nu')
    gen_trim(m, active)
    # 顶部岩浆注入口
    pipe_y(m, 8, 8, 14, 15.25, 2.4, {'*': 'refractory', 'cap': 'ember' if active else 'cavity'}, 12)
    m.prism('y', 15.25, 16, (8, 8), 2.8, 12, {'*': 'dark_steel', 'cap': 'cavity'})
    return m


def kinetic_generator(active):
    m = Model()
    # 正面动能输入轴座
    m.prism('z', 0.5, 1.5, (8, 7.5), 2.6, 12, {'*': 'dark_steel', 'cap': 'dark_steel'})
    m.prism('z', 0, 0.5, (8, 7.5), 1, 8, {'*': 'brass', 'cap': 'steel'})
    for k in range(4):
        import math
        a = k * math.pi / 2 + math.pi / 4
        m.box(8 + 2 * math.cos(a) - 0.3, 7.5 + 2 * math.sin(a) - 0.3, 0.3, 8 + 2 * math.cos(a) + 0.3, 7.5 + 2 * math.sin(a) + 0.3, 0.5, 'steel')
    gen_trim(m, active)
    m.box(4, 14, 4, 12, 14.75, 12, 'steel')
    for x in (5, 7.5, 10):
        m.box(x, 14.75, 5, x + 1, 15.5, 11, 'copper')
    return m


def semifluid_generator(active):
    m = Model()
    gen_window(m, 3.5, 3, 8, 10.5, 'glass')
    m.box(4.25, 3.75, 1.3, 7.25, 8, 1.4, 'biomass', 'nu')
    m.prism('z', 0.9, 1.5, (10.5, 7), 1.5, 12, {'*': 'dark_steel', 'cap': 'gauge'})
    gen_trim(m, active)
    # 顶部双缸机头
    for x in (5, 11):
        m.box(x - 2, 14, 5, x + 2, 15.5, 11, 'dark_steel')
        for y in (14.5, 15):
            m.box(x - 2.25, y, 4.75, x + 2.25, y + 0.25, 11.25, 'steel')
        pipe_y(m, x, 8, 15.5, 16, 0.8, {'*': 'brass', 'cap': 'brass'})
    return m


def solar_generator(active):
    m = Model()
    gen_trim(m, active)
    m.box(4, 4, 1.25, 12, 9, 1.5, 'dark_steel', 'nud')
    m.box(4.5, 4.5, 1.15, 11.5, 8.5, 1.25, 'screen_on' if active else 'screen_off', 'n')
    # 顶部电池板（铝框 + 电池片），略高出顶盖
    m.box(0.5, 14, 0.5, 15.5, 14.75, 15.5, 'steel')
    m.box(1, 14.75, 1, 15, 15, 15, 'solar', 'u')
    return m


def stirling_generator(active):
    m = Model()
    gen_window(m, 3.5, 3, 12.5, 7.5, 'ember' if active else 'cavity')
    gen_trim(m, active)
    # 顶部斯特林缸 + 飞轮轴承座
    pipe_y(m, 5, 8, 14, 16, 1.8, {'*': 'steel', 'cap': 'dark_steel'}, 10)
    m.box(9, 14, 6.5, 13, 15, 9.5, 'dark_steel')
    return m


def stirling_flywheel(active, index):
    m = Model()
    m.prism('x', 10, 10.75, (16 - 2.6, 8), 2.4, 14, {'*': 'copper' if active else 'steel', 'cap': 'dark_steel'})
    m.prism('x', 9.5, 11.25, (16 - 2.6, 8), 0.6, 8, {'*': 'brass', 'cap': 'brass'})
    return m


anim('stirling_generator', 0, 'rotate', axis=[1, 0, 0], pivot=[10.4, 13.4, 8], speed=11)


def water_generator(active):
    m = Model()
    gen_window(m, 3, 3, 13, 11, 'glass')
    m.box(3.75, 3.75, 1.3, 12.25, 6.5, 1.4, 'liquid', 'nu')
    gen_trim(m, active)
    pipe_y(m, 4.5, 8, 14, 15.5, 1.2, {'*': 'copper', 'cap': 'cavity'})
    pipe_y(m, 11.5, 8, 14, 15.5, 1.2, {'*': 'copper', 'cap': 'cavity'})
    return m


def water_generator_wheel(active, index):
    # 水轮机转轮：正对玻璃窗的 8 叶涡轮（发电机壳面 z=1.5，转轮必须在其前方）
    return fan(active, 8, 7, 1.15, 1.3, 3.2, blades=8)


anim('water_generator', 0, 'rotate', axis=[0, 0, 1], pivot=[8, 7, 1.2], speed=-6)


def wind_generator(active):
    m = Model()
    gen_trim(m, active)
    m.box(4, 4, 1.25, 12, 9.5, 1.5, 'dark_steel', 'nud')
    m.prism('z', 0.9, 1.25, (8, 6.75), 1.8, 12, {'*': 'steel', 'cap': 'gauge'})
    # 顶部塔筒法兰（风机转子由既有渲染器绘制）
    pipe_y(m, 8, 8, 14, 16, 2.2, {'*': 'steel', 'cap': 'dark_steel'}, 12)
    m.prism('y', 15.5, 16, (8, 8), 3, 12, {'*': 'dark_steel', 'cap': 'dark_steel'})
    return m


def creative_generator(active):
    m = Model()
    gen_window(m, 3, 3, 13, 11, 'uu_on')
    gen_trim(m, True)
    m.prism('y', 14, 16, (8, 8), 2.5, 12, {'*': 'uu_on', 'cap': 'uu_on'})
    for (x, z) in ((3, 3), (13, 3), (3, 13), (13, 13)):
        pipe_y(m, x, z, 14, 16, 0.6, {'*': 'brass', 'cap': 'lamp_on'})
    return m


def rt_generator(active):
    m = Model()
    gen_window(m, 3.5, 3.5, 12.5, 10.5, 'glass')
    # 放射性同位素芯柱：4 根，工作时发光
    for x in (5, 7, 9, 11):
        m.box(x - 0.5, 4.25, 1.25, x + 0.5, 9.75, 1.4, 'lamp_on' if active else 'lamp_off', 'nsewu')
    gen_trim(m, active)
    m.box(3, 14, 3, 13, 15, 13, 'painted')
    m.box(5, 15, 5, 11, 15.5, 11, 'hazard', 'nsewu')
    return m


# ---------------------------------------------------------------- 热能发生器（发电机机身 + 铜导热板）

def heat_face(m, active):
    """热能输出面：正面整块铜导热板 + 散热鳍，替代电力端子的识别。"""
    m.box(3, 2.5, 1.25, 13, 4, 1.5, 'copper', 'nud')
    for x in (3.5, 6, 8.5, 11):
        m.box(x, 2.75, 0.9, x + 1.5, 3.75, 1.25, 'copper' if not active else 'ember')


def electric_heat_generator(active):
    m = Model()
    gen_window(m, 3.5, 4.5, 12.5, 10.5, 'coil_on' if active else 'coil_off')
    heat_face(m, active)
    gen_trim(m, active)
    for k in range(3):
        m.prism('y', 14 + k * 0.6, 14.4 + k * 0.6, (8, 8), 4 - k, 12, {'*': 'copper', 'cap': 'copper'})
    return m


def fluid_heat_generator(active):
    m = Model()
    gen_window(m, 3.5, 4.5, 12.5, 10.5, 'glass')
    m.box(4.25, 5.25, 1.3, 11.75, 8.5, 1.4, 'biomass', 'nu')
    heat_face(m, active)
    gen_trim(m, active)
    pipe_y(m, 8, 8, 14, 15.75, 2.2, {'*': 'steel', 'cap': 'biomass'}, 10)
    return m


def rt_heat_generator(active):
    m = Model()
    gen_window(m, 3.5, 4.5, 12.5, 10.5, 'glass')
    for x in (5.5, 8, 10.5):
        m.box(x - 0.5, 5.25, 1.25, x + 0.5, 9.75, 1.4, 'lamp_on' if active else 'lamp_off', 'nsewu')
    heat_face(m, active)
    gen_trim(m, active)
    m.box(4, 14, 4, 12, 15, 12, 'hazard')
    return m


def solid_heat_generator(active):
    m = Model()
    gen_window(m, 4, 4.5, 12, 10.5, 'ember' if active else 'cavity')
    for x in (5.5, 7.5, 9.5):
        m.box(x, 5.25, 1, x + 0.6, 9.75, 1.4, 'dark_steel')
    heat_face(m, active)
    gen_trim(m, active)
    pipe_y(m, 11, 10.5, 14, 16, 1.3, {'*': 'refractory', 'cap': 'cavity'})
    return m


# ---------------------------------------------------------------- 动能发生器（正面为转子/输出轴）

def kinetic_front_hub(m):
    """正面输出轴承座：既有渲染器在正前方画转子/叶片，这里只做轴承与法兰。"""
    m.prism('z', 0.5, 1.5, (8, 8), 2.4, 12, {'*': 'dark_steel', 'cap': 'steel'})
    m.prism('z', 0, 0.5, (8, 8), 1.2, 8, {'*': 'brass', 'cap': 'dark_steel'})


def steam_kinetic_generator(active):
    m = Model()
    kinetic_front_hub(m)
    gen_trim(m, active)
    m.box(4, 3, 1.25, 12, 4.5, 1.5, 'copper', 'nud')
    for x in (5, 11):
        pipe_y(m, x, 8, 14, 16, 1.3, {'*': 'steel', 'cap': 'cavity'})
    m.box(5, 15, 7.5, 11, 15.75, 8.5, 'steel')
    return m


def water_kinetic_generator(active):
    m = Model()
    kinetic_front_hub(m)
    gen_trim(m, active)
    m.box(3, 14, 3, 13, 14.75, 13, 'steel')
    m.box(4, 14.75, 4, 12, 15, 12, 'liquid', 'u')
    return m


def wind_kinetic_generator(active):
    m = Model()
    kinetic_front_hub(m)
    gen_trim(m, active)
    pipe_y(m, 8, 10, 14, 15.5, 1, {'*': 'steel', 'cap': 'dark_steel'})
    m.box(7.5, 15.5, 9, 8.5, 16, 13, 'brass')
    return m


# ================================================================ 储电 / 变压 / 充电座

# 等级配色：输出环与色带材质
TIER = {'batbox': 'copper', 'cesu': 'brass', 'mfe': 'painted', 'mfsu': 'uu_off',
        'lv': 'copper', 'mv': 'brass', 'hv': 'painted', 'ev': 'uu_off'}


def socket(m, face, cx, cy, size, tex='dark_steel', pin='copper'):
    """电口：face 为 's'/'e'/'w'/'u'/'d'，在对应壳面（z/x/y=1 或 15）外贴方形插座。"""
    h = size / 2
    if face == 's':
        m.box(cx - h, cy - h, 15, cx + h, cy + h, 15.5, tex)
        m.box(cx - h / 2, cy - h / 2, 15.5, cx + h / 2, cy + h / 2, 15.75, pin, 'sewud')
    elif face in 'ew':
        x0, x1, p0, p1 = (15, 15.5, 15.5, 15.75) if face == 'e' else (0.5, 1, 0.25, 0.5)
        m.box(x0, cy - h, cx - h, x1, cy + h, cx + h, tex)
        m.box(p0, cy - h / 2, cx - h / 2, p1, cy + h / 2, cx + h / 2, pin)
    elif face in 'ud':
        y0, y1, p0, p1 = (15, 15.5, 15.5, 15.75) if face == 'u' else (0.5, 1, 0.25, 0.5)
        m.box(cx - h, y0, cy - h, cx + h, y1, cy + h, tex)
        m.box(cx - h / 2, p0, cy - h / 2, cx + h / 2, p1, cy + h / 2, pin)


def storage_body():
    """CESU/MFE/MFSU 共享机身：整体立方钢壳 + 深色棱边框 + 五面输入电口（正面为输出，由功能件提供）。"""
    m = Model()
    m.box(1, 1, 1, 15, 15, 15, 'steel')
    for a in ((0.5, 0.5), (14, 0.5), (0.5, 14), (14, 14)):
        x, z = a
        m.box(x, 0.5, z, x + 1.5, 15.5, z + 1.5, 'dark_steel')
    for y in (0.5, 14):
        m.box(0.5, y, 0.5, 15.5, y + 1.5, 2, 'dark_steel')
        m.box(0.5, y, 14, 15.5, y + 1.5, 15.5, 'dark_steel')
        m.box(0.5, y, 2, 2, y + 1.5, 14, 'dark_steel')
        m.box(14, y, 2, 15.5, y + 1.5, 14, 'dark_steel')
    for f in 'sewud':
        socket(m, f, 8, 8, 3.5)
    return m


def storage_front(tier):
    def build(active):
        m = Model()
        ring = TIER[tier]
        # 正面输出：大型等级色环 + 中心插座 + 电量条
        m.box(4, 6, 0.5, 12, 14, 1, ring)
        m.box(5, 7, 0.25, 11, 13, 0.5, 'dark_steel')
        m.box(6.5, 8.5, 0, 9.5, 11.5, 0.25, 'copper', 'nsewud')
        m.box(3, 2.5, 0.5, 13, 4.5, 1, 'dark_steel')
        m.box(3.5, 3, 0.4, 12.5, 4, 0.5, 'screen_on' if active else 'screen_off', 'n')
        return m
    build.__name__ = f'{tier}_front'
    return build


def batbox_body():
    """BatBox：木箱 + 钢包角（低阶储电，保留木质识别）。"""
    m = Model()
    m.box(1, 1, 1, 15, 15, 15, 'wood')
    for (x, z) in ((0.5, 0.5), (14, 0.5), (0.5, 14), (14, 14)):
        m.box(x, 0.5, z, x + 1.5, 15.5, z + 1.5, 'steel')
    for y in (0.5, 14):
        m.box(0.5, y, 0.5, 15.5, y + 1.5, 15.5, 'steel', 'nsew')
    for f in 'sewud':
        socket(m, f, 8, 8, 3.5)
    return m


def transformer_body():
    """变压器共享机身：深色壳 + 散热鳍 + 五面单点输入（正面多点输出由功能件提供）。"""
    m = Model()
    m.box(1.5, 1.5, 1.5, 14.5, 14.5, 14.5, 'dark_steel')
    for k in range(6):
        y = 2.5 + k * 2
        for (x0, x1) in ((1, 1.5), (14.5, 15)):
            m.box(x0, y, 3, x1, y + 1, 13, 'steel')
    for (x, z) in ((1, 1), (13.5, 1), (1, 13.5), (13.5, 13.5)):
        m.box(x, 1, z, x + 1.5, 15, z + 1.5, 'steel')
    m.box(1, 14.5, 1, 15, 15, 15, 'steel')
    m.box(1, 1, 1, 15, 1.5, 15, 'steel')
    for f in 'sud':
        socket(m, f, 8, 8, 3)
    return m


def transformer_front(tier):
    def build(active):
        m = Model()
        band = TIER[tier]
        m.box(2.5, 2.5, 1, 13.5, 13.5, 1.5, band)
        # 3×3 输出点阵
        for x in (4.5, 7.25, 10):
            for y in (4.5, 7.25, 10):
                m.box(x, y, 0.5, x + 1.5, y + 1.5, 1, 'dark_steel')
                m.box(x + 0.4, y + 0.4, 0.3, x + 1.1, y + 1.1, 0.5, 'lamp_on' if active else 'copper', 'nsewud')
        return m
    build.__name__ = f'{tier}_transformer_front'
    return build


def chargepad_body():
    """充电座共享机身：矮壳（顶面 y=15 与碰撞箱一致）+ 侧面输入口。"""
    m = Model()
    m.box(0.5, 0, 0.5, 15.5, 1.5, 15.5, 'dark_steel')
    m.box(1, 1.5, 1, 15, 13, 15, 'steel')
    for (x, z) in ((0.5, 0.5), (14, 0.5), (0.5, 14), (14, 14)):
        m.box(x, 1.5, z, x + 1.5, 14, z + 1.5, 'dark_steel')
    m.box(0.5, 13, 0.5, 15.5, 14, 15.5, 'dark_steel')
    socket(m, 's', 8, 7, 3.5)
    return m


def chargepad_front(tier):
    def build(active):
        m = Model()
        ring = TIER[tier]
        # 顶部充电感应板
        m.box(2, 14, 2, 14, 14.5, 14, ring)
        m.box(3, 14.5, 3, 13, 15, 13, 'lamp_on' if active else 'dark_steel', 'nsewu')
        for k in range(3):
            m.box(4 + k * 3, 15, 4, 5 + k * 3, 15.0625, 12, 'steel', 'u')
        # 正面电量条
        m.box(3, 4, 0.5, 13, 7, 1, 'dark_steel')
        m.box(3.5, 4.5, 0.4, 12.5, 6.5, 0.5, 'screen_on' if active else 'screen_off', 'n')
        m.box(4, 9, 0.75, 12, 11, 1, ring, 'nud')
        return m
    build.__name__ = f'{tier}_chargepad_front'
    return build


# ---------------------------------------------------------------- 交易机（基础机身）

def omat_panel(m, active):
    # 售货面板：显示屏 + 投币口 + 按键排
    window(m, 3.5, 7.5, 12.5, 12, inner='screen_on' if active else 'screen_off')
    m.box(4, 5.5, 1.5, 7, 6.5, 2, 'dark_steel')
    m.box(4.5, 5.9, 1.4, 6.5, 6.1, 1.5, 'cavity', 'n')
    for x in (8.5, 9.75, 11):
        m.box(x, 5.5, 1.5, x + 0.75, 6.25, 2, 'lamp_red' if x == 8.5 else 'brass', 'nsewud')
    m.box(3.5, 2.5, 1, 12.5, 4.5, 2, 'dark_steel')
    m.box(4.5, 3, 0.9, 11.5, 4, 1, 'cavity', 'n')


def energy_o_mat(active):
    m = Model()
    omat_panel(m, active)
    m.box(4, 14, 4, 12, 14.75, 12, 'copper')
    socket(m, 'u', 8, 8, 3)
    return m


def trade_o_mat(active):
    m = Model()
    omat_panel(m, active)
    # 顶部玻璃展示柜
    m.box(3, 14, 3, 13, 14.5, 13, 'dark_steel')
    m.box(3.5, 14.5, 3.5, 12.5, 16, 12.5, {'*': 'glass', 'd': 'dark_steel'})
    m.box(6.5, 14.5, 6.5, 9.5, 15.25, 9.5, 'brass')
    return m


MACHINES = {
    # 名称: (机身, 功能件, 运动件, 运动件数量)
    'macerator': (basic_body, macerator, macerator_rotor, 2),
    'compressor': (basic_body, compressor, compressor_piston, 1),
    'extractor': (basic_body, extractor, extractor_impeller, 1),
    'electric_furnace': (basic_body, electric_furnace, None, 0),
    'recycler': (basic_body, recycler, recycler_drum, 1),
    'canner': (basic_body, canner, canner_nozzle, 1),
    'solid_canner': (basic_body, solid_canner, solid_canner_stamp, 1),
    'iron_furnace': (iron_body, iron_furnace, None, 0),
    'blast_furnace': (advanced_body, blast_furnace, None, 0),
    'block_cutter': (advanced_body, block_cutter, block_cutter_saw, 1),
    'centrifuge': (advanced_body, centrifuge, centrifuge_rotor, 1),
    'fermenter': (advanced_body, fermenter, None, 0),
    'induction_furnace': (advanced_body, induction_furnace, None, 0),
    'metal_former': (advanced_body, metal_former, metal_former_roll, 2),
    'ore_washing_plant': (advanced_body, ore_washing_plant, ore_washing_wheel, 1),
    'pump': (basic_body, pump, pump_impeller, 1),
    'fluid_canner': (basic_body, fluid_canner, canner_nozzle, 1),
    'condenser': (basic_body, condenser, condenser_fan, 1),
    'solar_distiller': (basic_body, solar_distiller, None, 0),
    'crop_harvester': (basic_body, crop_harvester, crop_harvester_head, 1),
    'cropmatron': (basic_body, cropmatron, cropmatron_sprinkler, 1),
    'animalmatron': (basic_body, animalmatron, None, 0),
    'animal_slaughterer': (basic_body, animal_slaughterer, animal_slaughterer_blade, 1),
    'miner': (basic_body, miner, None, 0),
    'magnetizer': (basic_body, magnetizer, None, 0),
    'tesla_coil': (basic_body, tesla_coil, None, 0),
    'chunk_loader': (basic_body, chunk_loader, chunk_loader_dish, 1),
    'steam_generator': (advanced_body, steam_generator, None, 0),
    'liquid_heat_exchanger': (advanced_body, liquid_heat_exchanger, None, 0),
    'advanced_miner': (advanced_body, advanced_miner, None, 0),
    'matter_generator': (advanced_body, matter_generator, None, 0),
    'replicator': (advanced_body, replicator, None, 0),
    'uu_scanner': (advanced_body, uu_scanner, None, 0),
    'pattern_storage': (advanced_body, pattern_storage, None, 0),
    'generator': (generator_body, generator, None, 0),
    'geo_generator': (generator_body, geo_generator, None, 0),
    'kinetic_generator': (generator_body, kinetic_generator, None, 0),
    'semifluid_generator': (generator_body, semifluid_generator, None, 0),
    'solar_generator': (generator_body, solar_generator, None, 0),
    'stirling_generator': (generator_body, stirling_generator, stirling_flywheel, 1),
    'water_generator': (generator_body, water_generator, water_generator_wheel, 1),
    'wind_generator': (generator_body, wind_generator, None, 0),
    'creative_generator': (generator_body, creative_generator, None, 0),
    'rt_generator': (generator_body, rt_generator, None, 0),
    'electric_heat_generator': (generator_body, electric_heat_generator, None, 0),
    'fluid_heat_generator': (generator_body, fluid_heat_generator, None, 0),
    'rt_heat_generator': (generator_body, rt_heat_generator, None, 0),
    'solid_heat_generator': (generator_body, solid_heat_generator, None, 0),
    'steam_kinetic_generator': (generator_body, steam_kinetic_generator, None, 0),
    'water_kinetic_generator': (generator_body, water_kinetic_generator, None, 0),
    'wind_kinetic_generator': (generator_body, wind_kinetic_generator, None, 0),
    'batbox': (batbox_body, storage_front('batbox'), None, 0),
    'cesu': (storage_body, storage_front('cesu'), None, 0),
    'mfe': (storage_body, storage_front('mfe'), None, 0),
    'mfsu': (storage_body, storage_front('mfsu'), None, 0),
    'lv_transformer': (transformer_body, transformer_front('lv'), None, 0),
    'mv_transformer': (transformer_body, transformer_front('mv'), None, 0),
    'hv_transformer': (transformer_body, transformer_front('hv'), None, 0),
    'ev_transformer': (transformer_body, transformer_front('ev'), None, 0),
    'batbox_chargepad': (chargepad_body, chargepad_front('batbox'), None, 0),
    'cesu_chargepad': (chargepad_body, chargepad_front('cesu'), None, 0),
    'mfe_chargepad': (chargepad_body, chargepad_front('mfe'), None, 0),
    'mfsu_chargepad': (chargepad_body, chargepad_front('mfsu'), None, 0),
    'energy_o_mat': (basic_body, energy_o_mat, None, 0),
    'trade_o_mat': (basic_body, trade_o_mat, None, 0),
}

# 六向朝向（可朝上/下）：up/down 用 x 旋转
SIX_DIR = {'batbox', 'cesu', 'mfe', 'mfsu', 'lv_transformer', 'mv_transformer', 'hv_transformer', 'ev_transformer'}

# 没有 active 属性的机器：blockstate 只按 facing 叠加功能件
NO_ACTIVE = {'pattern_storage'}
# 没有 facing 属性的机器：不旋转
NO_FACING = {'rt_generator'}


def blockstate(name, body):
    parts = []
    if name in NO_FACING:
        parts.append({'apply': {'model': f'{NS}:{TEX_DIR}/{body}'}})
        for active in ('false', 'true'):
            suffix = '_active' if active == 'true' else ''
            parts.append({'when': {'active': active}, 'apply': {'model': f'{NS}:{TEX_DIR}/{name}_parts{suffix}'}})
        return {'multipart': parts}
    dirs = [('north', 0, 0), ('east', 0, 90), ('south', 0, 180), ('west', 0, 270)]
    if name in SIX_DIR:
        # 与原版发射器一致：朝上 x=270、朝下 x=90（模型正面为北）
        dirs += [('up', 270, 0), ('down', 90, 0)]
    for facing, x, y in dirs:
        def apply(model):
            d = {'model': f'{NS}:{TEX_DIR}/{model}'}
            if x:
                d['x'] = x
            if y:
                d['y'] = y
            return d
        parts.append({'when': {'facing': facing}, 'apply': apply(body)})
        if name in NO_ACTIVE:
            parts.append({'when': {'facing': facing}, 'apply': apply(f'{name}_parts')})
            continue
        for active in ('false', 'true'):
            suffix = '_active' if active == 'true' else ''
            parts.append({'when': {'facing': facing, 'active': active}, 'apply': apply(f'{name}_parts{suffix}')})
    return {'multipart': parts}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, str):
        path.write_text(data, encoding='utf-8')
    else:
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


# 法线、四个角（按逆时针，从外侧看）
QUADS = {
    'n': ((0, 0, -1), lambda a, b: [(b[0], a[1], a[2]), (a[0], a[1], a[2]), (a[0], b[1], a[2]), (b[0], b[1], a[2])]),
    's': ((0, 0, 1), lambda a, b: [(a[0], a[1], b[2]), (b[0], a[1], b[2]), (b[0], b[1], b[2]), (a[0], b[1], b[2])]),
    'w': ((-1, 0, 0), lambda a, b: [(a[0], a[1], a[2]), (a[0], a[1], b[2]), (a[0], b[1], b[2]), (a[0], b[1], a[2])]),
    'e': ((1, 0, 0), lambda a, b: [(b[0], a[1], b[2]), (b[0], a[1], a[2]), (b[0], b[1], a[2]), (b[0], b[1], b[2])]),
    'u': ((0, 1, 0), lambda a, b: [(a[0], b[1], b[2]), (b[0], b[1], b[2]), (b[0], b[1], a[2]), (a[0], b[1], a[2])]),
    'd': ((0, -1, 0), lambda a, b: [(a[0], a[1], a[2]), (b[0], a[1], a[2]), (b[0], a[1], b[2]), (a[0], a[1], b[2])]),
}


def proj_uv(face, p):
    x, y, z = p
    if face == 'n':
        return (16 - x, 16 - y)
    if face == 's':
        return (x, 16 - y)
    if face == 'e':
        return (16 - z, 16 - y)
    if face == 'w':
        return (z, 16 - y)
    if face == 'u':
        return (x, z)
    return (x, 16 - z)


def wrap01(vals):
    # 投影坐标落在 [0,16]，个别面超出时整体平移回一张贴图内
    lo = min(vals)
    shift = (lo // 16) * 16
    return [v - shift for v in vals]


def polys(model):
    """展开为 (材质, [(pos16, uv01)], normal) 多边形列表。"""
    import math
    out = []
    for e in model.elements:
        if 'poly' in e:
            pts = e['poly']
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = pts[0], pts[1], pts[2]
            ux, uy, uz = x1 - x0, y1 - y0, z1 - z0
            vx, vy, vz = x2 - x0, y2 - y0, z2 - z0
            nrm = (uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
            # UV：投影到法线主轴的垂直平面（与长方体面投影一致）
            ax = max(range(3), key=lambda i: abs(nrm[i]))
            uvs = [(p[2], 16 - p[1]) if ax == 0 else (p[0], p[2]) if ax == 1 else (p[0], 16 - p[1]) for p in pts]
            out.append((e['tex'], pts, uvs, nrm))
            continue
        if 'prism' in e:
            axis, c0, c1, (ca, cb), rad, n, phase = e['prism']
            tex = e['tex'] if isinstance(e['tex'], str) else e['tex']['*']
            cap = e['tex'].get('cap', tex) if isinstance(e['tex'], dict) else tex

            def P(t, a, b):
                if axis == 'x':
                    return (t, a, b)
                if axis == 'y':
                    return (a, t, b)
                return (a, b, t)
            ring = [(ca + rad * math.cos(phase + 2 * math.pi * k / n), cb + rad * math.sin(phase + 2 * math.pi * k / n)) for k in range(n)]
            side = 2 * rad * math.sin(math.pi / n)
            for k in range(n):
                (a0, b0), (a1, b1) = ring[k], ring[(k + 1) % n]
                u0, u1 = (k * side) % 16, (k * side) % 16 + side
                if u1 > 16:
                    u0, u1 = 0, side
                pts = [P(c0, a0, b0), P(c0, a1, b1), P(c1, a1, b1), P(c1, a0, b0)]
                uvs = [(u0, c0), (u1, c0), (u1, c1), (u0, c1)]
                mid = ((a0 + a1) / 2 - ca, (b0 + b1) / 2 - cb)
                nrm = P(0, *mid)
                out.append((tex, pts, uvs, nrm))
            for end, sign in ((c0, -1), (c1, 1)):
                pts = [P(end, a, b) for a, b in ring]
                uvs = [(a, b) for a, b in ring]
                out.append((cap, pts, uvs, P(sign, 0, 0)))
            continue
        a, b = e['frm'], e['to']
        for f in e['faces']:
            tex = e['tex'] if isinstance(e['tex'], str) else e['tex'].get(f, e['tex'].get('*'))
            if tex is None:
                continue
            nrm, corners = QUADS[f]
            pts = corners(a, b)
            uvs = [proj_uv(f, p) for p in pts]
            us, vs = wrap01([u for u, _ in uvs]), wrap01([v for _, v in uvs])
            out.append((tex, pts, list(zip(us, vs)), nrm))
    return out


def fix_winding(pts, nrm):
    # 保证多边形绕序与法线同向（逆时针从外侧看），否则反转
    (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = pts[0], pts[1], pts[2]
    ux, uy, uz = x1 - x0, y1 - y0, z1 - z0
    vx, vy, vz = x2 - x0, y2 - y0, z2 - z0
    c = (uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
    return sum(c[i] * nrm[i] for i in range(3)) >= 0


def to_obj(model, name):
    lines = [f'# ic2_120 tools/model3d 生成，勿手改', f'mtllib {name}.mtl']
    vi = 0
    used = []
    for tex, pts, uvs, nrm in polys(model):
        if tex not in used:
            used.append(tex)
        if not fix_winding(pts, nrm):
            pts, uvs = pts[::-1], uvs[::-1]
        lines.append(f'usemtl {tex}')
        for (x, y, z), (u, v) in zip(pts, uvs):
            lines.append(f'v {r(x / 16)} {r(y / 16)} {r(z / 16)}')
            lines.append(f'vt {r(u / 16)} {r(v / 16)}')
        ln = (sum(c * c for c in nrm)) ** 0.5
        lines.append('vn ' + ' '.join(str(r(c / ln)) for c in nrm))
        lines.append('f ' + ' '.join(f'{vi + k + 1}/{vi + k + 1}/-1' for k in range(len(pts))))
        vi += len(pts)
    mtl = []
    for t in used:
        mtl += [f'newmtl {t}', 'Kd 1 1 1', f'map_Kd #{t}']
        if t in EMISSIVE:
            mtl.append('Ke 1 1 1')
        mtl.append('')
    return '\n'.join(lines) + '\n', '\n'.join(mtl), used


EMISSIVE = {'lamp_on', 'coil_on', 'ember', 'uu_on', 'screen_on'}


def export(mdir, name, model, particle='steel', stub_paths=()):
    obj, mtl, used = to_obj(model, name)
    write(mdir / f'{name}.obj', obj)
    write(mdir / f'{name}.mtl', mtl)
    textures = {'particle': f'{NS}:{TEX_DIR}/{particle}'}
    for t in used:
        textures[t] = f'{NS}:{TEX_DIR}/{t}'
    stub = {
        'loader': 'ic2_120:obj',
        'model': f'{NS}:models/{TEX_DIR}/{name}.obj',
        'parent': 'minecraft:block/block',
        'textures': textures,
    }
    # 原版 JSON 模型不能以 OBJ 模型为 parent，物品模型须直接写成 OBJ 声明
    for p in (mdir / f'{name}.json', *stub_paths):
        write(p, stub)


def main(assets):
    assets = Path(assets)
    mdir = assets / 'models' / TEX_DIR
    done = set()
    for name, (body_fn, parts_fn, rotor_fn, rotors) in MACHINES.items():
        body = body_fn.__name__
        if body not in done:
            export(mdir, body, body_fn())
            done.add(body)
        for active in (False, True):
            suffix = '_active' if active else ''
            export(mdir, f'{name}_parts{suffix}', parts_fn(active))
        for i in range(rotors):
            export(mdir, f'{name}_rotor_{i}', rotor_fn(False, i))
            export(mdir, f'{name}_rotor_{i}_active', rotor_fn(True, i))
        item = Model().extend(body_fn()).extend(parts_fn(False))
        for i in range(rotors):
            item.extend(rotor_fn(False, i))
        export(mdir, f'{name}_item', item, stub_paths=[assets / 'models' / 'item' / f'{name}.json'])
        write(assets / 'blockstates' / f'{name}.json', blockstate(name, body))
    write(assets / TEX_DIR.replace('block/', '') / 'animations.json', ANIMS)


if __name__ == '__main__':
    main(sys.argv[1])
