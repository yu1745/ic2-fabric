# 3D 机器模型管线

机器方块不再使用 `assets/ic2/**` 的 2D 贴图，改为 OBJ 模型 + 32px 共享材质。风格只遵循 MC 像素风（冷钢灰、划痕磨损），不沿用 IC2 外观。

## 组成

| 位置 | 作用 |
|---|---|
| `tools/model3d/models.py` | 长方体/棱柱 DSL → OBJ + MTL + 模型 JSON + blockstate + `machine3d/animations.json`（**唯一源头**，生成物勿手改） |
| `tools/model3d/textures.py` | 共享材质贴图（固定种子，可复现） |
| `tools/model3d/render_preview.py` | Blender 无头预览（借用 `~/Motive` 的着色辅助） |
| `tools/model3d/snap.sh` / `stage.sh` / `closeups.sh` | 游戏内闭环：开发服 + 客户端直连 + mdb 摆放 + 截图 |
| `client/model/obj/` | `ic2_120:obj` 加载器（Fabric Renderer API，依赖 Indigo/Indium） |
| `client/renderers/Machine3dBlockEntityRenderer.kt` | 运动件通用渲染器，读取 `animations.json` |

## 约定

- 坐标：模型单位 1 格 = 16，正面朝北（-Z）；UV 按世界坐标投影，32px = 1 格。
- 机身共享（对应原 IC2"多台机器共用一张外壳贴图"）：`basic_body` / `advanced_body` / `generator_body` / `storage_body` / `transformer_body` / `chargepad_body` / `batbox_body` / `iron_body`。blockstate 用 multipart 叠加"机身 + `<机器>_parts[_active]`"。
- 物品模型：`models/item/<机器>.json` 必须直接是 OBJ 声明（原版 JSON 不能以 OBJ 模型为 parent，否则资源加载失败、进世界崩溃）。
- 机身壳面位置：basic z=2、advanced/generator z=1.5；正面零件必须在壳面前方，否则被壳面挡住。
- 方块必须 `nonOpaque()`：模型不填满整格，不关剔除会让紧贴方块被剔面而透视；碰撞箱保持整格。
- 动画：`anim(机器, 部件序号, 'rotate'|'slide', ...)`；运动件为 `<机器>_rotor_<i>[_active]`，并在 `ClientBlockEntityRenderers` 注册对应 BE 类型。旋转对称件的转速不能整除其对称角（10 棱柱 36°），否则逐帧看起来静止。
- 自发光材质：`EMISSIVE` 集合（`lamp_on`、`coil_on`、`ember`、`uu_on`、`screen_on`）。
- 六向机器（储电箱、变压器）在 `SIX_DIR`；无 active / 无 facing 的分别在 `NO_ACTIVE` / `NO_FACING`。

## 工作流

```bash
python3 tools/model3d/models.py core/src/main/resources/assets/ic2_120
python3 tools/model3d/textures.py core/src/main/resources/assets/ic2_120/textures/block/machine3d
./gradlew :core:build -x test

# 游戏内闭环（开发服需 online-mode=false；客户端 options pauseOnLostFocus:false）
tools/model3d/snap.sh up                 # 起开发服 + 客户端直连，自动旁观模式
tools/model3d/stage.sh macerator compressor
tools/model3d/closeups.sh <输出目录> macerator compressor
tools/model3d/snap.sh reload             # 只改模型/贴图：重导出（含 3D 物品）+ processResources + 触发客户端重载
```

改 Kotlin（方块属性、渲染器注册）需重启服务端与客户端；只改资源用 `reload`。

- 资源重载不模拟 F3+T：桌面截图工具（如 Snipaste）会全局抢占 F1/F3。`reload` 在 `core/run/` 写 `.ic2_reload_resources`，
  开发客户端的 `DevResourceReloadTrigger` 检测到后自行重载。
- 截图直接从 X 显示 `:0` 截整屏再按游戏窗口几何裁剪（会包含遮挡在窗口上方的其他窗口）。

## 未纳入

- 多方块结构部件（反应堆、焦炉）保持整格立方体。
- 储物箱、储罐、线缆、矿石/金属块、橡胶建材、作物、发光板：非机器或形状特殊，后续按 2D 贴图处理。

## 物品贴图（2D 重制）

`tools/model3d/items.py` 以 `assets/ic2/textures/item/<路径>` 为结构参考，生成 `assets/ic2_120/textures/item/<同路径>`（32px），物品模型引用随之从 `ic2:item/…` 改为 `ic2_120:item/…`，共享关系不变。

- 色彩：低饱和像素 → 冷钢色阶；彩色像素保留色相、按 HSV 明度分级（按亮度会把纯红/纯蓝压暗）。
- 形体：1px 暗轮廓 + 内侧受光倒角；质感：金属区域按文件名种子加划痕/点蚀，可复现。
- 竖条多帧贴图逐帧处理，`.mcmeta` 原样复制。

```bash
python3 tools/model3d/items.py remaster core/src/main/resources/assets/ic2/textures/item <输出目录> <相对路径>...
python3 tools/model3d/items.py sheet <对比图.png> <原图目录> <新图目录> <相对路径>...
```

### 全量迁移与重新生成

```bash
tools/model3d/regen_textures.sh   # 删除并重建所有未跟踪的生成贴图，最后跑引用校验
python3 tools/model3d/check_refs.py core/src/main/resources/assets/ic2_120   # 单独校验
```

- `migrate.py`：从 ic2_120 的 blockstate/模型出发，递归复制被引用的 ic2 模型到 ic2_120 同路径（ic2_120 已有同名文件则不覆盖），模型链上的贴图全部重制；被 `tintindex` 染色的贴图用中性灰（冷钢偏蓝会和生物群系染色叠加变色）。
- `upgrades.py`：升级卡不走自动重制，按"芯片模块"全新设计（功能色带 + 屏幕图标，高级金边星标，方向角标）。
- `check_refs.py` 按字段区分：`model`/`parent` 只认模型文件，贴图槽位只认贴图；同时检查 parent 自引用。
- 特殊处理：ic2 的 `block/storage_box.png` 带损坏的 iCCP 块，先 `convert -strip` 再重制；`item/empty_fuel_rod` 在 ic2 中无原图，由铀燃料棒去掉燃料颜色生成。

踩过的坑：只凭"同路径贴图存在"改写引用，会把 `overrides` 里的**模型**引用也改掉（模型并不存在）；ic2_120 自有的包装模型若 parent 指向 ic2 同路径，改写后会自引用死循环。两者现已由 `migrate.py` / `check_refs.py` 防住。

未纳入：GUI 贴图（`textures/gui/*`）与橡胶船实体贴图仍引用 ic2。
