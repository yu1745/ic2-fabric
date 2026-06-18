#!/usr/bin/env python3
"""
预烤泥炭矿纹理（替代运行时 tint + ColorProvider）。

原理：泥炭矿复用锡矿石纹理（近灰度矿岩+白色斑点），运行时由 tintindex × tintColor 上色。
本脚本离线把 "灰度 × 深褐 tintColor(0x4A3728)" 这步乘法烤进 PNG，
模型即可不带 tintindex，运行时不再需要 PeatOreColorProvider。

数学：
    outR = inR * 74 / 255   (0x4A → 74)
    outG = inG * 55 / 255   (0x37 → 55)
    outB = inB * 40 / 255   (0x28 → 40)
    outA = inA               (保留 α)

注意：源纹理是项目自带的 assets/ic2/textures/block/resource/ore/tin_ore.png，
不在 MC jar 里，因此直接从仓库读取（与 generate_rubber_leaves.py 从 jar 提取不同）。

重新跑：改了 TINT 后直接 `python3 scripts/generate_peat_ore.py`，
产物覆盖 core/src/main/resources/assets/ic2_120/textures/block/resource/ore/peat_ore.png。
"""

import os
from PIL import Image

# 项目根目录（脚本在 <root>/scripts/ 下）
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 锡矿石纹理（项目自带的 ic2 上游引用，近灰度）
SRC_PATH = os.path.join(
    ROOT,
    "core/src/main/resources/assets/ic2/textures/block/resource/ore/tin_ore.png",
)

# 目标 tintColor（深褐，与历史 PeatOreColorProvider.PEAT_COLOR 一致）
TINT_R, TINT_G, TINT_B = 0x4A, 0x37, 0x28

# 输出路径
OUT_PATH = os.path.join(
    ROOT,
    "core/src/main/resources/assets/ic2_120/textures/block/resource/ore/peat_ore.png",
)


def main() -> int:
    if not os.path.isfile(SRC_PATH):
        print(f"错误：找不到源纹理：{SRC_PATH}", flush=True)
        return 1

    src = Image.open(SRC_PATH).convert("RGBA")
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
    print(
        f"已生成：{OUT_PATH}  ({w}x{h}, tint=#{TINT_R:02x}{TINT_G:02x}{TINT_B:02x})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
