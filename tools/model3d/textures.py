"""3D 机器共享材质贴图（32×32，1 格 = 1 次平铺）。

固定随机种子，可复现。所有机器共用这一套材质，模型 UV 按世界坐标投影，
因此像素密度在所有面上一致（1 px = 1/32 格）。

用法：python3 textures.py <输出目录>
"""
import random
import sys
from pathlib import Path

from PIL import Image

N = 32


LIFT = 0.75  # 整体提亮：伽马 <1 抬暗部，保留高光


def lift(v):
    return int(round(255 * (v / 255) ** LIFT))


def hx(s):
    return tuple(lift(int(s[i:i + 2], 16)) for i in (0, 2, 4)) + (255,)


class Tex:
    def __init__(self, ramp, base):
        self.ramp = [hx(c) for c in ramp]
        self.p = [[base] * N for _ in range(N)]

    def get(self, x, y):
        return self.p[y % N][x % N]

    def set(self, x, y, v):
        self.p[y % N][x % N] = max(0, min(len(self.ramp) - 1, v))

    def add(self, x, y, d):
        self.set(x, y, self.get(x, y) + d)

    def image(self):
        im = Image.new('RGBA', (N, N))
        for y in range(N):
            for x in range(N):
                im.putpixel((x, y), self.ramp[self.p[y][x]])
        return im


CLEAN = 0  # 不做磨损：划痕/点蚀/拉丝全部关闭


def brushed(t, rnd, rows=0.35):
    rows *= CLEAN
    # 横向拉丝：断续的暗/亮细线，平铺无缝（坐标取模）
    for y in range(N):
        if rnd.random() < rows:
            x0 = rnd.randrange(N)
            for i in range(rnd.randint(6, 20)):
                t.add(x0 + i, y, rnd.choice((-1, -1, 1)))


def mottle(t, rnd, n, d, size=(2, 4)):
    # 低频斑驳：几个小团块整体提亮/压暗
    for _ in range(n):
        cx, cy = rnd.randrange(N), rnd.randrange(N)
        r = rnd.randint(*size)
        for y in range(-r, r + 1):
            for x in range(-r, r + 1):
                if x * x + y * y <= r * r and rnd.random() < 0.7:
                    t.add(cx + x, cy + y, d)


def scratches(t, rnd, n, hi=2, length=(4, 9)):
    # 划痕：1px 斜线，亮色裸金属，下方一像素暗影
    for _ in range(int(n * CLEAN)):
        x, y = rnd.randrange(N), rnd.randrange(N)
        slope = rnd.choice((0.0, 0.25, 0.5, -0.5, 1.0))
        for i in range(rnd.randint(*length)):
            px, py = x + i, y + int(round(i * slope))
            t.add(px, py, hi if i % 3 else hi - 1)
            if rnd.random() < 0.6:
                t.add(px, py + 1, -1)


def pits(t, rnd, n):
    # 点蚀/凹坑：暗点 + 右下亮边
    for _ in range(int(n * CLEAN)):
        x, y = rnd.randrange(N), rnd.randrange(N)
        t.add(x, y, -2)
        t.add(x + 1, y + 1, 1)


STEEL = ['22262f', '30353f', '3f4551', '4e5562', '5e6573', '6f7685', '808898', '9299a8', 'a5acb9', 'b8becb', 'ced3dc', 'e4e8ee']
DARK = ['232830', '2d333d', '38404b', '444c58', '505966', '5d6774', '6b7582', '7a8491', '8d97a4']
COPPER = ['3a160d', '5c2513', '7e351b', 'a04824', 'bf5f30', 'd67a44', 'e8985f', 'f5bd8a', 'ffdcb5']
BRASS = ['3b2a0c', '5a4214', '7a5b1c', '9a7524', 'b8902e', 'd3ad44', 'e6c869', 'f4e3a0']
RUBBER = ['0e0f11', '15171a', '1c1f23', '24272c', '2d3137', '383c43']
HAZARD_Y = ['5e4a0c', '8a6c12', 'b58f1a', 'd9ad22', 'efc93c', 'fbe27a']


def steel(seed=1):
    rnd = random.Random(seed)
    t = Tex(STEEL, 9)
    brushed(t, rnd, 0.4)
    mottle(t, rnd, 3, -1)
    scratches(t, rnd, 4, hi=2)
    pits(t, rnd, 3)
    return t


def dark_steel(seed=2):
    rnd = random.Random(seed)
    t = Tex(DARK, 5)
    brushed(t, rnd, 0.3)
    mottle(t, rnd, 2, -1)
    scratches(t, rnd, 3, hi=2)
    pits(t, rnd, 3)
    return t


def copper(seed=3):
    rnd = random.Random(seed)
    t = Tex(COPPER, 5)
    brushed(t, rnd, 0.5)
    mottle(t, rnd, 4, -1)
    scratches(t, rnd, 4, hi=2)
    return t


def brass(seed=4):
    rnd = random.Random(seed)
    t = Tex(BRASS, 4)
    brushed(t, rnd, 0.4)
    mottle(t, rnd, 3, -1)
    scratches(t, rnd, 3, hi=2)
    return t


def rubber(seed=5):
    rnd = random.Random(seed)
    t = Tex(RUBBER, 3)
    for _ in range(60):
        t.add(rnd.randrange(N), rnd.randrange(N), rnd.choice((-1, 1)))
    return t


def hazard(seed=6):
    # 黄黑警示斜纹，8px 周期，四方连续
    rnd = random.Random(seed)
    ramp = HAZARD_Y + ['121418', '1b1e23', '262a31']
    t = Tex(ramp, 3)
    for y in range(N):
        for x in range(N):
            black = ((x + y) // 2) % 2 == 1
            t.set(x, y, 7 if black else 3)
    for y in range(N):
        for x in range(N):
            v = t.get(x, y)
            if v <= 5 and rnd.random() < 0.18:
                t.add(x, y, -1)
    # 刮花：黄色上露出暗色，黑色上露出黄色
    for _ in range(8):
        x, y = rnd.randrange(N), rnd.randrange(N)
        for i in range(rnd.randint(2, 5)):
            t.set(x + i, y, 1 if t.get(x + i, y) < 6 else 8)
    return t


def flat(ramp, base, seed, noise=40):
    def make():
        rnd = random.Random(seed)
        t = Tex(ramp, base)
        for _ in range(noise):
            t.add(rnd.randrange(N), rnd.randrange(N), rnd.choice((-1, 1)))
        return t
    return make


def gauge(seed=11):
    # 奶油色表盘：外圈刻度 + 红色超压区（指针为几何体，不画在贴图上）
    import math
    ramp = ['2a2a2a', '5a5650', 'cfc8b4', 'e8e1cc', 'f5f0e0', 'b8321e', 'e0552e']
    t = Tex(ramp, 3)
    rnd = random.Random(seed)
    for _ in range(30):
        t.add(rnd.randrange(N), rnd.randrange(N), -1)
    c = (N - 1) / 2
    for k in range(13):
        a = math.radians(225 - k * 22.5)
        for rr in (12, 13):
            x, y = int(round(c + rr * math.cos(a))), int(round(c - rr * math.sin(a)))
            t.set(x, y, 5 if k >= 10 else 0)
    for k in range(40):
        a = math.radians(225 - k * 6.75)
        x, y = int(round(c + 14.5 * math.cos(a))), int(round(c - 14.5 * math.sin(a)))
        t.set(x, y, 1)
    return t


def glass(seed=12):
    # 观察窗玻璃（不透明近似）：深青底 + 斜向反光
    rnd = random.Random(seed)
    t = Tex(['14232a', '1b2f38', '233c47', '2e4c59', '4f7484', '86aab8'], 2)
    for y in range(N):
        for x in range(N):
            d = (x + y) % 24
            if d in (3, 4):
                t.set(x, y, 4)
            elif d == 5:
                t.set(x, y, 5)
            elif rnd.random() < 0.08:
                t.add(x, y, rnd.choice((-1, 1)))
    return t


def coil(on, seed=13):
    # 加热丝：横向波纹，工作态为炽热橙色
    rnd = random.Random(seed)
    ramp = ['3a1a10', '6a2a14', 'a2401a', 'e0702a', 'ffb04a', 'ffe6a0'] if on else ['262a31', '343a44', '454c58', '5a6270', '6e7684', '858d9a']
    t = Tex(ramp, 1)
    for y in range(N):
        for x in range(N):
            ph = (x + (2 if (y // 4) % 2 else 0)) % 4
            if y % 4 in (1, 2):
                t.set(x, y, 4 if ph < 2 else 3)
            if y % 4 == 1 and ph == 0:
                t.set(x, y, 5)
    for _ in range(20):
        t.add(rnd.randrange(N), rnd.randrange(N), -1)
    return t


def liquid(seed=14):
    rnd = random.Random(seed)
    t = Tex(['0d3a3e', '135158', '1a6b73', '24878f', '3aa8ad', '7ad3d2'], 3)
    for y in range(N):
        for x in range(N):
            if rnd.random() < 0.15:
                t.add(x, y, rnd.choice((-1, 1)))
        if y % 8 == 2:
            for x in range(N):
                if (x + y) % 6 < 3:
                    t.set(x, y, 4)
    return t


def cast_iron(seed=15):
    # 铸铁：暖灰黑，粗砂眼
    rnd = random.Random(seed)
    t = Tex(['1d1b20', '25232a', '2e2b33', '38353d', '434047', '4f4b53', '5d5961', '6e6a72'], 4)
    for y in range(N):
        for x in range(N):
            if rnd.random() < 0.12:
                t.add(x, y, -1)
    mottle(t, rnd, 3, -1, (2, 3))
    pits(t, rnd, 4)
    scratches(t, rnd, 2, hi=2)
    return t


def ember(seed=16):
    rnd = random.Random(seed)
    t = Tex(['2a0a04', '5a1508', '9a2a0c', 'd2501a', 'f2902e', 'ffcf6a', 'fff3c4'], 3)
    for y in range(N):
        for x in range(N):
            t.set(x, y, max(0, min(6, 5 - (y * 5) // N + rnd.choice((-1, 0, 0, 1)))))
    return t


def painted(seed=17):
    # 高级机器涂装钢：深钴蓝，边角磨出底层钢色
    rnd = random.Random(seed)
    t = Tex(['17202f', '1e2a3d', '26354c', '2f415c', '3a4f6d', '4a6080', '6a7d96', '9aa6b6'], 4)
    brushed(t, rnd, 0.25)
    mottle(t, rnd, 2, -1)
    for _ in range(5):
        x, y = rnd.randrange(N), rnd.randrange(N)
        for i in range(rnd.randint(2, 5)):
            t.set(x + i, y, 6 if i % 2 else 7)
    pits(t, rnd, 3)
    return t


def biomass(seed=18):
    rnd = random.Random(seed)
    t = Tex(['1c2a0c', '2a3d12', '3a541a', '4c6c22', '62862e', '8aae4a'], 3)
    for y in range(N):
        for x in range(N):
            if rnd.random() < 0.2:
                t.add(x, y, rnd.choice((-1, 1)))
    for _ in range(12):
        x, y = rnd.randrange(N), rnd.randrange(N)
        t.set(x, y, 5)
        t.set(x + 1, y, 4)
    return t


def refractory(seed=19):
    # 耐火砖：暖灰砖缝，用于高炉
    rnd = random.Random(seed)
    t = Tex(['2a1f1c', '4a3a34', '6a5248', '86685a', '9e7e6c', 'b49482'], 3)
    for y in range(N):
        for x in range(N):
            row = y // 4
            off = 4 if row % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7:
                t.set(x, y, 1)
            elif rnd.random() < 0.25:
                t.add(x, y, rnd.choice((-1, 1)))
    return t


def uu(on, seed=20):
    # UU 物质：紫色流体，工作态更亮
    rnd = random.Random(seed)
    ramp = ['2a0f3a', '42185a', '5e237e', '7d32a4', 'a24ccc', 'd08cf0'] if on else ['1d1226', '2a1a38', '3a234c', '4b2e62', '5e3c78', '7a5296']
    t = Tex(ramp, 3)
    for y in range(N):
        for x in range(N):
            if rnd.random() < 0.25:
                t.add(x, y, rnd.choice((-1, 1)))
    for _ in range(10):
        t.set(rnd.randrange(N), rnd.randrange(N), 5)
    return t


def screen(on, seed=21):
    # 显示屏：绿色扫描线 + 波形，停机为暗屏
    rnd = random.Random(seed)
    ramp = ['0a1a10', '0f2a18', '154022', '1f6a34', '3aa858', '8af0a4'] if on else ['0c0f12', '11151a', '161b21', '1c2229', '232a33', '2c3440']
    t = Tex(ramp, 1)
    import math
    for y in range(N):
        for x in range(N):
            if y % 2 == 0:
                t.set(x, y, 2)
        wy = int(16 + 6 * math.sin(y * 0.6))
    for x in range(N):
        y = int(16 + 7 * math.sin(x * 0.45))
        t.set(x, y, 5)
        t.set(x, y + 1, 4)
    for _ in range(12):
        t.add(rnd.randrange(N), rnd.randrange(N), 1)
    return t


def solar(seed=22):
    # 太阳能电池片：深蓝 4×4 片，银色栅线，斜向反光
    rnd = random.Random(seed)
    t = Tex(['0d1530', '142048', '1c2c60', '263a78', '3a5294', 'b8c4d8', 'e4eaf2'], 2)
    for y in range(N):
        for x in range(N):
            if x % 8 == 0 or y % 8 == 0:
                t.set(x, y, 5)
            elif x % 8 == 4:
                t.set(x, y, 4)
            elif rnd.random() < 0.15:
                t.add(x, y, rnd.choice((-1, 1)))
            if (x + y) % 32 in (10, 11) and x % 8 and y % 8:
                t.set(x, y, 3)
    return t


def wood(seed=23):
    # 木板：横向木纹，4px 一块
    rnd = random.Random(seed)
    t = Tex(['3a2414', '553620', '6e4a2c', '87603a', '9e7448', 'b48a5a'], 3)
    for y in range(N):
        for x in range(N):
            if y % 8 == 7:
                t.set(x, y, 0)
            elif rnd.random() < 0.3:
                t.add(x, y, rnd.choice((-1, 1)))
        if rnd.random() < 0.4:
            x0 = rnd.randrange(N)
            for i in range(rnd.randint(4, 12)):
                t.set(x0 + i, y, 2)
    return t


TEXTURES = {
    'wood': wood,
    'solar': solar,
    'uu_off': lambda: uu(False),
    'uu_on': lambda: uu(True),
    'screen_off': lambda: screen(False),
    'screen_on': lambda: screen(True),
    'painted': painted,
    'biomass': biomass,
    'refractory': refractory,
    'gauge': gauge,
    'glass': glass,
    'coil_off': lambda: coil(False),
    'coil_on': lambda: coil(True),
    'liquid': liquid,
    'cast_iron': cast_iron,
    'ember': ember,
    'lamp_off': flat(['1f3324', '2a4530', '36593d', '43704b'], 1, 7),
    'lamp_on': flat(['5fe07a', '86f59a', 'b4ffc0', 'e6ffea'], 1, 8),
    'lamp_red': flat(['5a1612', '8c2218', 'c23a22', 'ef6a3a'], 2, 9),
    'cavity': flat(['1a1d23', '22262e', '2b3039'], 1, 10, 30),
    'steel': steel,
    'dark_steel': dark_steel,
    'copper': copper,
    'brass': brass,
    'rubber': rubber,
    'hazard': hazard,
}


def main(out):
    out = Path(out)
    out.mkdir(parents=True, exist_ok=True)
    for name, fn in TEXTURES.items():
        fn().image().save(out / f'{name}.png')


if __name__ == '__main__':
    main(sys.argv[1])
