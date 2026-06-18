#!/usr/bin/env python3
"""
预烤橡胶树叶纹理（替代运行时 tint + ColorProvider）。

原理：原版 oak_leaves.png 是灰度纹理，运行时由 tintindex × tintColor 上色。
本脚本离线把 "灰度 × 金黄绿 tintColor(0xc4b848)" 这步乘法烤进 PNG，
模型即可不带 tintindex，运行时不再需要 ColorProvider。

数学：
    outR = inR * 196 / 255   (0xc4 → 196)
    outG = inG * 184 / 255   (0xb8 → 184)
    outB = inB *  72 / 255   (0x48 →  72)
    outA = inA               (保留镂空)
视觉等价于把 "当前物品栏里看到的金黄绿橡叶" 固化到整张图。

重新跑：改了 TINT 后直接 `python3 scripts/generate_rubber_leaves.py`，
产物覆盖 core/src/main/resources/assets/ic2_120/textures/block/rubber_leaves.png。
"""

import os
import sys
import zipfile
from PIL import Image

# 项目根目录（脚本在 <root>/scripts/ 下）
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 1.20.1 client jar（loom 缓存）。若路径变动，按报错信息更新。
JAR_PATH = "/home/wangyu/server/.gradle-tmp-fabric/caches/fabric-loom/1.20.1/minecraft-merged.jar"
JAR_ENTRY = "assets/minecraft/textures/block/oak_leaves.png"

# 目标 tintColor（金黄绿，与历史 RubberLeavesColorProvider.DEFAULT_RUBBER_LEAVES_COLOR 一致）
TINT_R, TINT_G, TINT_B = 0xC4, 0xB8, 0x48

# 输出路径
OUT_PATH = os.path.join(
    ROOT,
    "core/src/main/resources/assets/ic2_120/textures/block/rubber_leaves.png",
)


def main() -> int:
    if not os.path.isfile(JAR_PATH):
        print(f"错误：找不到 MC jar：{JAR_PATH}", file=sys.stderr)
        print("请在 ~/.gradle 或项目的 loom 缓存里定位 1.20.1 minecraft-merged.jar / client.jar。", file=sys.stderr)
        return 1

    with zipfile.ZipFile(JAR_PATH) as zf:
        with zf.open(JAR_ENTRY) as f:
            src = Image.open(f).convert("RGBA")

    w, h = src.size
    src_px = src.load()
    out = Image.new("RGBA", (w, h))
    out_px = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src_px[x, y]
            nr = (r * TINT_R) // 255
            ng = (g * TINT_G) // 255
            nb = (b * TINT_B) // 255
            out_px[x, y] = (nr, ng, nb, a)  # α 原样保留

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    out.save(OUT_PATH)
    print(f"已生成：{OUT_PATH}  ({w}x{h}, tint=#{TINT_R:02x}{TINT_G:02x}{TINT_B:02x})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
