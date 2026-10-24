"""升级卡重新设计（不沿用 ic2 原图造型）：32px 冷钢芯片模块。

卡体：横置芯片外壳（倒角 + 磨损）、左侧深色屏幕窗、右侧功能色带、底部铜金手指。
功能：屏幕图标 + 色带颜色区分；物品类画方块、流体类画水滴；弹出=箭头朝外、抽取=箭头朝内。
高级：金色包边 + 右上角星标。方向：右下角小标（上下为三角，东南西北为像素字母）。

用法：python3 upgrades.py <输出目录 textures/item/upgrade>
"""
import random
import sys
from pathlib import Path

from PIL import Image

N = 32
STEEL = ['1c1e23', '292c33', '3a3f4a', '4f5767', '666f80', '7f8899', '9aa3b2', 'b8c0cc', 'dde2e8']
GOLD = ['4a3208', '7a5614', 'b08428', 'd8b048', 'f4dc8c']
COPPER = ['4a1f14', '86391f', 'c0602f', 'e5925a']
SCREEN = ['0b0e12', '12161c', '1a2029']

FUNC = {
    # 名称: (色带三级色, 图标)
    'ejector': (['7a3a0c', 'c8641a', 'f0a050'], 'item_out'),
    'pulling': (['0c4a5a', '1a8aa0', '5ad0e0'], 'item_in'),
    'fluid_ejector': (['0c2a6a', '2456c0', '6a9cf0'], 'fluid_out'),
    'fluid_pulling': (['0c4a3a', '1a9a72', '5ae0b0'], 'fluid_in'),
    'overclocker': (['6a0c10', 'c0242a', 'f06a5a'], 'chevrons'),
    'transformer': (['6a5208', 'd0a818', 'f8e070'], 'bolt'),
    'energy_storage': (['1c5a10', '3aa424', '8ae060'], 'battery'),
    'redstone_inverter': (['5a0c0c', 'b01c1c', 'ff5040'], 'invert'),
    'remote_interface': (['3a1a6a', '7040c0', 'b088f0'], 'antenna'),
}

# 3×5 像素字母
GLYPH = {
    'N': ['101', '111', '111', '111', '101'],
    'E': ['111', '100', '111', '100', '111'],
    'S': ['111', '100', '111', '001', '111'],
    'W': ['101', '101', '111', '111', '101'],
}


LIFT = 0.75  # 整体提亮：伽马 <1 抬暗部，保留高光


def lift(v):
    return int(round(255 * (v / 255) ** LIFT))


def hx(s):
    return tuple(lift(int(s[i:i + 2], 16)) for i in (0, 2, 4)) + (255,)


class Canvas:
    def __init__(self):
        self.im = Image.new('RGBA', (N, N), (0, 0, 0, 0))

    def px(self, x, y, c):
        if 0 <= x < N and 0 <= y < N:
            self.im.putpixel((x, y), hx(c) if isinstance(c, str) else c)

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def get(self, x, y):
        return self.im.getpixel((x, y))


def body(cv, rnd, advanced):
    # 卡体：x 2..29，y 7..24；1px 暗轮廓 + 倒角
    x0, y0, x1, y1 = 2, 7, 29, 24
    cv.rect(x0, y0, x1, y1, STEEL[0])
    cv.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, STEEL[6])
    cv.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 1, STEEL[8])
    cv.rect(x0 + 1, y0 + 1, x0 + 1, y1 - 1, STEEL[7])
    cv.rect(x0 + 1, y1 - 1, x1 - 1, y1 - 1, STEEL[3])
    cv.rect(x1 - 1, y0 + 1, x1 - 1, y1 - 1, STEEL[4])
    # 四角切角
    for (x, y) in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
        cv.px(x, y, (0, 0, 0, 0))
    # 拉丝与划痕（只落在钢面上）
    for _ in range(14):
        x, y = rnd.randint(x0 + 2, x1 - 2), rnd.randint(y0 + 2, y1 - 2)
        cv.px(x, y, STEEL[5])
    for _ in range(3):
        x, y = rnd.randint(x0 + 3, x1 - 6), rnd.randint(y0 + 2, y1 - 3)
        for i in range(rnd.randint(2, 4)):
            cv.px(x + i, y + i // 2, STEEL[8])
    # 固定螺钉
    for (x, y) in ((x0 + 2, y0 + 2), (x1 - 2, y1 - 2)):
        cv.px(x, y, STEEL[3])
    if advanced:
        gold_frame(cv, x0, y0, x1, y1)
    # 金手指：底部伸出的 9 根引脚
    for i in range(9):
        x = 5 + i * 3
        cv.rect(x, y1 + 1, x + 1, y1 + 3, COPPER[2])
        cv.px(x, y1 + 1, COPPER[3])
        cv.px(x + 1, y1 + 3, COPPER[1])


def gold_frame(cv, x0, y0, x1, y1):
    for x in range(x0 + 1, x1):
        cv.px(x, y0, GOLD[3] if x % 4 else GOLD[4])
        cv.px(x, y1, GOLD[1])
    for y in range(y0 + 1, y1):
        cv.px(x0, y, GOLD[3])
        cv.px(x1, y, GOLD[1])


def screen(cv):
    # 屏幕窗 x 5..19，y 10..21，内凹（上/左暗边）
    cv.rect(5, 10, 19, 21, SCREEN[1])
    cv.rect(5, 10, 19, 10, STEEL[2])
    cv.rect(5, 10, 5, 21, STEEL[2])
    cv.rect(6, 21, 19, 21, STEEL[7])
    cv.rect(19, 11, 19, 21, STEEL[7])
    for y in range(12, 21, 2):  # 扫描线
        for x in range(6, 19):
            if cv.get(x, y)[:3] == hx(SCREEN[1])[:3]:
                cv.px(x, y, SCREEN[0])


def band(cv, colors):
    # 右侧功能色带 x 22..26，y 10..21，带高光
    cv.rect(22, 10, 26, 21, colors[1])
    cv.rect(22, 10, 26, 10, colors[2])
    cv.rect(22, 10, 22, 21, colors[2])
    cv.rect(22, 21, 26, 21, colors[0])
    cv.rect(26, 11, 26, 21, colors[0])
    for y in (13, 16, 19):  # 色带上的散热刻槽
        cv.rect(23, y, 25, y, colors[0])


def icon(cv, kind, colors):
    c, hi, lo = colors[1], colors[2], colors[0]
    cx, cy = 12, 15

    def arrow(x0, y, right=True):
        # 横向箭头：杆 + 箭头头
        for x in range(x0, x0 + 4):
            cv.px(x, y, hi)
            cv.px(x, y + 1, c)
        tip = x0 + 4 if right else x0 - 1
        d = 1 if right else -1
        for k in range(3):
            cv.px(tip + d * k, y - 2 + k, hi)
            cv.px(tip + d * k, y + 3 - k, c)
        cv.px(tip + d * 2, y, hi)
        cv.px(tip + d * 2, y + 1, c)

    def cube(x, y):
        cv.rect(x, y, x + 4, y + 4, c)
        cv.rect(x, y, x + 4, y, hi)
        cv.rect(x, y, x, y + 4, hi)
        cv.rect(x + 1, y + 4, x + 4, y + 4, lo)

    def drop(x, y):
        rows = ['00100', '01110', '01110', '11111', '11111', '01110']
        for j, r in enumerate(rows):
            for i, ch in enumerate(r):
                if ch == '1':
                    cv.px(x + i, y + j, hi if (i + j) < 4 else c)
        cv.px(x + 1, y + 3, (255, 255, 255, 255))

    if kind in ('item_out', 'fluid_out'):
        (cube if kind == 'item_out' else drop)(7, 13)
        arrow(13, 15, right=True)
    elif kind in ('item_in', 'fluid_in'):
        (cube if kind == 'item_in' else drop)(13, 13)
        arrow(11, 15, right=False)
    elif kind == 'chevrons':
        for k in range(3):
            x = 7 + k * 4
            for j in range(4):
                cv.px(x + j, 12 + j, hi if k == 2 else c)
                cv.px(x + j, 19 - j, hi if k == 2 else c)
    elif kind == 'bolt':
        pts = [(13, 11), (12, 12), (11, 13), (10, 14), (11, 15), (12, 15), (13, 15), (14, 15),
               (13, 16), (12, 17), (11, 18), (10, 19)]
        for (x, y) in pts:
            cv.px(x, y, hi)
            cv.px(x + 1, y, c)
        cv.rect(9, 14, 14, 14, hi)
    elif kind == 'battery':
        cv.rect(8, 12, 16, 19, lo)
        cv.rect(9, 13, 15, 18, SCREEN[0])
        cv.rect(17, 14, 17, 17, lo)
        for k, x in enumerate((10, 12, 14)):
            cv.rect(x, 14, x + 1, 17, hi if k < 2 else c)
    elif kind == 'invert':
        # 方波 + 反相横线
        wave = [(7, 17), (8, 17), (9, 17), (9, 16), (9, 15), (9, 14), (10, 14), (11, 14), (12, 14),
                (12, 15), (12, 16), (12, 17), (13, 17), (14, 17), (15, 17), (15, 16), (15, 15), (15, 14),
                (16, 14), (17, 14)]
        for (x, y) in wave:
            cv.px(x, y, hi)
        cv.rect(7, 12, 17, 12, c)
    elif kind == 'antenna':
        cv.rect(12, 14, 12, 20, c)
        cv.px(12, 13, hi)
        for r in (2, 4):
            for k in (-1, 1):
                cv.px(12 + k * r, 13 - r // 2, hi if r == 2 else c)
                cv.px(12 + k * r, 14 - r // 2, c)
        cv.rect(10, 20, 14, 20, lo)


def direction_mark(cv, d):
    # 右下角方向标（覆盖在色带下端）：底板 + 字母/三角
    x0, y0 = 22, 16
    cv.rect(x0, y0, x0 + 4, y0 + 5, SCREEN[0])
    if d in GLYPH:
        for j, r in enumerate(GLYPH[d]):
            for i, ch in enumerate(r):
                if ch == '1':
                    cv.px(x0 + 1 + i, y0 + j, STEEL[8])
    elif d == 'up':
        for k in range(3):
            cv.rect(x0 + 2 - k, y0 + 1 + k, x0 + 2 + k, y0 + 1 + k, STEEL[8])
    elif d == 'down':
        for k in range(3):
            cv.rect(x0 + k, y0 + 2 + k, x0 + 4 - k, y0 + 2 + k, STEEL[8])


def star(cv):
    # 高级标记：右上角金色四角星
    x, y = 27, 6
    for (dx, dy) in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
        cv.px(x + dx, y + dy, GOLD[4] if (dx, dy) == (0, 0) else GOLD[3])
    cv.px(x - 2, y, GOLD[2])
    cv.px(x + 2, y, GOLD[2])
    cv.px(x, y - 2, GOLD[2])
    cv.px(x, y + 2, GOLD[2])


DIRS = {'down': 'down', 'up': 'up', 'north': 'N', 'south': 'S', 'east': 'E', 'west': 'W'}


def parse(name):
    adv = name.startswith('advanced_')
    base = name[len('advanced_'):] if adv else name
    d = None
    for suffix, mark in DIRS.items():
        if base.endswith('_' + suffix):
            base, d = base[:-len(suffix) - 1], mark
    return adv, base, d


def make(name):
    adv, base, d = parse(name)
    colors, kind = FUNC[base]
    rnd = random.Random(sum(map(ord, name)))
    cv = Canvas()
    body(cv, rnd, adv)
    screen(cv)
    band(cv, colors)
    icon(cv, kind, colors)
    if d:
        direction_mark(cv, d)
    if adv:
        star(cv)
    return cv.im


def main(out, names):
    out = Path(out)
    out.mkdir(parents=True, exist_ok=True)
    for n in names:
        make(n).save(out / f'{n}.png')
    print(f'升级卡 {len(names)} 张')


if __name__ == '__main__':
    out = sys.argv[1]
    src = Path(sys.argv[2]) if len(sys.argv) > 2 else None
    names = sorted(p.stem for p in src.glob('*.png')) if src else sorted(
        p.stem for p in Path(out).glob('*.png'))
    main(out, names)
