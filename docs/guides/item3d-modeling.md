# 物品 3D 建模规范

所有物品（流体单元除外，它由 `FluidCellItemRenderer` 动态绘制；线缆保持原版 2D）逐个**手工建模**为 3D，
参照对象是 **ic2 原版 2D 贴图**（`assets/ic2/textures/item/**`），不是 ic2_120 的重制贴图。
禁止"按像素自动挤出/体素化"之类的自动生成：每个物品都要有自己的建模函数，看过原图后设计结构。

## 工具链

| 文件 | 作用 |
|---|---|
| `tools/model3d/item3d/core.py` | 注册装饰器 `@item(...)`、材质 `mat()`、导出（OBJ/MTL/物品 JSON/材质贴图）、显示变换 |
| `tools/model3d/item3d/<家族>.py` | 各家族建模模块（导入即注册），如 `fuel_rods.py` |
| `tools/model3d/item3d/build.py` | `python3 tools/model3d/item3d/build.py <assets> [模型名...]` 导出 |
| `tools/model3d/item3d/preview.py` | 离线对照图：原版 2D 放大 / GUI 视角 / 正视，`preview.py <assets> <out.png> <模型名...>` |
| `core/src/client/.../model/obj/` | `ic2_120:obj` 加载器，支持 `overrides`（电量档位、开关状态等）、`tintindex`、自发光材质 |

几何 DSL 复用 `tools/model3d/models.py` 的 `Model`：`box(x0,y0,z0,x1,y1,z1, tex)`、
`prism(axis, c0, c1, (ca, cb), radius, sides, tex, phase)`；坐标 0~16，1 格 = 16。

## 约定

- **朝向**：物品立于 x/y 平面，正面朝南（+Z）；中心约 (8, 8, 8)。普通物品高度占满 2~15 左右。
- **工具/武器**（原版 `item/handheld` 父模型）：`@item('name', handheld=True)`，沿 +Y 竖直建模、工具头朝上；
  显示变换 `display_handheld` 负责 GUI 斜 45° 与斜握。
- **扁平物品**（板、卡、模板、硬币等）：给出真实厚度（1~3 单位），做出倒角/凸起/凹槽等结构，不要做成一张纸。
- **配色**：直接取原图色块（`mat('#52950f')`）。大面积不要用低于 `#303030` 的暗色（用户反馈"太暗沉"）。
  无磨损、无噪点；材质样式只有 `flat` / `metal`（细竖条反光）/ `glass`（高光条）/ `glow`（自发光，用于指示灯、能量芯、刀刃）。
- **结构优先于贴图**：原图里的分区（外壳、窗口、端帽、指示灯、刻度）拆成独立几何件，前后错开有层次。
- **多边形棱柱**：圆柱类用 8 棱（`sides=8, phase=math.pi/8`，平面朝向坐标轴）；立柱/护条也按 8 棱分布，不要 4 棱。
- **overrides 子模型**（如 `battery/re_battery_1`、`upgrade/ejector_north`、`tool/nano_saber/active_3`）：
  同样注册为独立模型名，导出时物品 JSON 原有的 `overrides` 会被保留。同族变体可用带参数的辅助函数，
  但每个模型名都要显式注册、逐个过目。
- **每个物品一个函数**；家族内共享"部件函数"（如 `classic_rod`），不共享整件模型。
- 原图有多层（`layer1` 染色层，如刷子颜色）时，把染色部分做成独立材质；需要按物品染色时告知负责加载器的人（主代理）。

## 流程（每个物品）

1. 放大看原图：`convert <原图>.png -filter point -resize 256x256 ...`，记下色值（PIL 取 `getdata()` 去重）。
2. 写建模函数并注册。
3. `preview.py` 出对照图，自查：轮廓比例、配色、层次、没有穿模/浮空件、GUI 视角能看出体积。
4. 迭代到满意后 `build.py <assets> <名字...>` 导出。
5. 游戏内验证由主代理统一进行（客户端只有一个，子代理不要启动或操作游戏）。

## 分工

| 负责人 | 家族模块 |
|---|---|
| 主代理 | `fuel_rods`、`reactor`（反应堆元件、冷却单元）、`battery`（电池/水晶及电量档位）、`upgrade`（升级卡及方向子模型）、`rotor`、`bucket`、`brewing`、`tfbp` |
| 子代理 A | `tools`（`tool/*`、`tool/electric/*`、纳米剑全部档位、刷子全部颜色）、`armor`（护甲与背包的物品形态）、`boat`、`crop` |
| 子代理 B | `crafting`、`resource`（外壳、粉碎/纯净矿石、粉、锭、核材料、板、粗矿、其他资源）、`item`（`item` 目录下零散物品） |

模块文件按家族拆分（一个负责人可拥有多个模块），**不要修改他人的模块，也不要改 `core.py`**；
需要框架改动（新材质样式、显示变换、加载器能力）时提出，由主代理统一修改。

## 不在范围

- 盔甲**穿戴**外观（`textures/models/armor/*`）不变，只做物品形态。
- 线缆、流体单元、3D 机器的物品形态（已有）。
