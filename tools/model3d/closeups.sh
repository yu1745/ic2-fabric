#!/usr/bin/env bash
# 对 stage.sh 摆出的机器逐台近景截图并拼成总览图。
#   tools/model3d/closeups.sh <输出目录> <机器>...（顺序须与 stage.sh 一致）
set -euo pipefail
D=$(dirname "$0"); out=$1; shift
mkdir -p "$out"
x=0
for m in "$@"; do
  SNAP_DELAY=${SNAP_DELAY:-2.5} "$D/snap.sh" view "$(python3 -c "print($x - 0.3)")" 201.25 -0.85 "$x.5" 200.5 0.5 "$out/$m.png" >/dev/null 2>&1
  x=$((x + 2))
done
python3 - "$out" "$@" <<'PY'
import sys
from PIL import Image
out, ms = sys.argv[1], sys.argv[2:]
cols = 4
ims = [Image.open(f'{out}/{m}.png').crop((560, 100, 2000, 1340)).resize((480, 413)) for m in ms]
sh = Image.new('RGB', (cols * 480, ((len(ims) + cols - 1) // cols) * 413))
for i, im in enumerate(ims):
    sh.paste(im, ((i % cols) * 480, (i // cols) * 413))
sh.save(f'{out}/sheet.png')
PY
