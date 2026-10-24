#!/usr/bin/env bash
# 在 (0,200,0) 起每隔 2 格摆一台 3D 机器（朝北），供截图检查。
#   tools/model3d/stage.sh <机器>... ；不给参数则摆全部
set -euo pipefail
S=$(dirname "$0")/snap.sh
machines=("$@")
[ ${#machines[@]} -eq 0 ] && machines=(macerator compressor extractor electric_furnace recycler canner solid_canner iron_furnace)
cmds=('gamemode spectator @a' 'time set noon' 'weather clear 999999' 'gamerule doDaylightCycle false' 'gamerule doWeatherCycle false'
      'fill -4 199 -6 44 199 6 minecraft:smooth_stone' 'fill -4 200 -6 44 206 6 minecraft:air')
x=0
for m in "${machines[@]}"; do
  # 没有 active 属性的机器（如 pattern_storage）带 active 会失败，再补一条只带 facing 的
  cmds+=("setblock $x 200 0 ic2_120:$m[facing=north,active=false]" "setblock $x 200 0 ic2_120:$m[facing=north]")
  x=$((x + 2))
done
"$S" cmd "${cmds[@]}" | grep -v '"success":true' || true
