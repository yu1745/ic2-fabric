#!/usr/bin/env bash
# 3D 模型闭环预览：开发服 + 客户端直连 + mdb 摆放机器 + 截图。
#   tools/model3d/snap.sh up                  # 启动服务端与客户端（已在跑则跳过）
#   tools/model3d/snap.sh cmd '<命令>'...      # 经 mcdebug 执行服务端命令（不带 /）
#   tools/model3d/snap.sh shot <输出.png>      # 截取 Minecraft 窗口
#   tools/model3d/snap.sh reload               # 重新导出模型/贴图 → processResources → 触发客户端重载资源
#   tools/model3d/snap.sh view <x y z> <看向 x y z> <输出.png>  # 旁观模式传送后截图
# 开发服需 online-mode=false（dev 账号离线）。
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
RUN=$ROOT/core/run
MDB=${MDB:-$ROOT/../../tools/mdb}
SOCK=$RUN/mcdebug/socket
LOGDIR=${SNAP_LOGDIR:-/tmp}

mdb() { "$MDB" --socket "$SOCK" "$@"; }

wait_for() { # <描述> <超时秒> <检测命令...>
  local what=$1 t=$2; shift 2
  for ((i = 0; i < t; i++)); do "$@" >/dev/null 2>&1 && return 0; sleep 1; done
  echo "等待超时：$what" >&2; return 1
}

client_running() { pgrep -f '[d]evlaunchinjector.*client|[K]notClient' >/dev/null; }
# 直接从 X 显示 :0 截整屏，再按游戏窗口几何裁剪（不依赖窗口焦点或截图工具）
grab() { # <输出.png>
  local win; win=$(xdotool search --name '^Minecraft' | head -1)
  eval "$(xdotool getwindowgeometry --shell "$win")"
  import -display :0 -window root -crop "${WIDTH}x${HEIGHT}+${X}+${Y}" +repage "$1"
}
player_online() { mdb raw server.runCommand '{"command":"list"}' | grep -qv 'There are 0'; }

case ${1:-} in
  up)
    if ! [ -S "$SOCK" ] || ! mdb status >/dev/null 2>&1; then
      (cd "$ROOT" && nohup ./gradlew :core:runServer --args=nogui >"$LOGDIR/ic2-server.out" 2>&1 &)
      wait_for "mcdebug socket" 300 mdb status
    fi
    if ! client_running; then
      (cd "$ROOT" && nohup ./gradlew :core:runClient --args='--quickPlayMultiplayer localhost:25565' >"$LOGDIR/ic2-client.out" 2>&1 &)
    fi
    wait_for "玩家进服" 300 player_online
    # dev 客户端每次启动都是新的离线玩家：立即切旁观，避免摔死；死亡则自动重生
    mdb raw server.runCommand '{"command":"gamerule doImmediateRespawn true"}' >/dev/null
    mdb raw server.runCommand '{"command":"gamemode spectator @a"}' >/dev/null
    ;;
  cmd)
    shift
    for c in "$@"; do
      mdb raw server.runCommand "$(python3 -c 'import json,sys;print(json.dumps({"command":sys.argv[1]}))' "$c")"
    done
    ;;
  shot)
    grab "$2"
    ;;
  reload)
    A=$ROOT/core/src/main/resources/assets/ic2_120
    python3 "$ROOT/tools/model3d/models.py" "$A"
    python3 "$ROOT/tools/model3d/textures.py" "$A/textures/block/machine3d"
    python3 "$ROOT/tools/model3d/item3d/build.py" "$ROOT/core/src/main/resources/assets"
    (cd "$ROOT" && ./gradlew :core:processResources -q)
    n=$(grep -c 'OBJ 模型' "$RUN/logs/latest.log")
    # 不模拟 F3+T（桌面截图工具会全局抢占 F 键）：写触发文件，由客户端 DevResourceReloadTrigger 重载
    touch "$RUN/.ic2_reload_resources"
    wait_for "资源重载" 120 bash -c "[ \$(grep -c 'OBJ 模型' '$RUN/logs/latest.log') -gt $n ]"
    sleep 3
    ;;
  view)
    # 参数是"眼睛位置"：tp 设的是脚，眼高 1.62；target 同步下移，使朝向不变
    read -r fy ty < <(python3 -c "import sys;print(float(sys.argv[1])-1.62, float(sys.argv[2])-1.62)" "$3" "$6")
    mdb raw server.runCommand '{"command":"gamemode spectator @a"}' >/dev/null
    mdb raw server.runCommand "{\"command\":\"tp @a $2 $fy $4 facing $5 $ty $7\"}" >/dev/null
    sleep "${SNAP_DELAY:-2}"
    grab "$8"
    ;;
  *) sed -n 2,8p "$0"; exit 1 ;;
esac
