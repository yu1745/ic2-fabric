#!/usr/bin/env bash
# 重新生成全部 ic2 → ic2_120 重制贴图（物品、方块、转子皮肤与两张需特殊处理的贴图）。
# 只删除并重建"未被 git 跟踪"的生成贴图，ic2_120 自有（已跟踪）资产不动。
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
A=$ROOT/core/src/main/resources/assets
T=$(mktemp -d)
trap 'rm -rf "$T"' EXIT
cd "$ROOT"

git status --porcelain --untracked-files=all "$A/ic2_120/textures" | awk '$1=="??"{print $2}' \
  | { grep -vE '/(machine3d|item3d|cell3d)/' || true; } | while read -r f; do rm -f "$ROOT/$f"; done

# 1) 物品模型直接引用的贴图（含已改为 ic2_120 的引用）
grep -rhoE '"ic2(_120)?:item/[^"]+"' "$A/ic2_120/models/item" | tr -d '"' | sed -E 's/^ic2(_120)?:item\///' | sort -u \
  | while read -r rel; do if [ -f "$A/ic2/textures/item/$rel.png" ]; then echo "$rel"; fi; done > "$T/items.txt"
xargs python3 tools/model3d/items.py remaster "$A/ic2/textures/item" "$A/ic2_120/textures/item" < "$T/items.txt"

# 1b) 升级卡：全新设计，覆盖自动重制结果
python3 tools/model3d/upgrades.py "$A/ic2_120/textures/item/upgrade" "$A/ic2/textures/item/upgrade"

# 1c) 流体单元：3D 胶囊（物品模型与 cell3d 贴图一并生成）
(cd tools/model3d && python3 cells.py "$A")

# 1d) 3D 物品：OBJ/MTL/物品模型/材质贴图（tools/model3d/item3d，规范见 docs/guides/item3d-modeling.md）
python3 tools/model3d/item3d/build.py "$A"

# 2) 模型链上其余 ic2 贴图（方块、多方块部件、子模型）
python3 tools/model3d/migrate.py "$A"

# 3) ic2 原图损坏：去掉损坏的 iCCP 块后重制
convert "$A/ic2/textures/block/storage_box.png" -strip "$T/storage_box.png" 2>/dev/null
# 4) ic2 无原图：由铀燃料棒去掉燃料颜色得到空燃料棒；转子皮肤由渲染器直接引用
python3 - "$A" "$T" <<'PY'
import colorsys, sys
sys.path.insert(0, 'tools/model3d')
import items
from PIL import Image
A, T = sys.argv[1], sys.argv[2]
items.remaster_image(Image.open(f'{T}/storage_box.png'), 'block/storage_box').save(f'{A}/ic2_120/textures/block/storage_box.png')
src = Image.open(f'{A}/ic2/textures/item/reactor/fuel_rod/uranium.png').convert('RGBA')
for y in range(src.height):
    for x in range(src.width):
        r, g, b, a = src.getpixel((x, y))
        h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        if a and s > 0.25:
            l = int(64 + 64 * v)
            src.putpixel((x, y), (l, l, l + 6, a))
items.remaster_image(src, 'item/empty_fuel_rod').save(f'{A}/ic2_120/textures/item/empty_fuel_rod.png')
for n in ('wood', 'bronze', 'iron', 'steel', 'carbon'):
    rel = f'item/rotor/{n}_rotor_model'
    items.remaster_image(Image.open(f'{A}/ic2/textures/{rel}.png'), rel).save(f'{A}/ic2_120/textures/{rel}.png')
PY
python3 tools/model3d/check_refs.py "$A/ic2_120"
