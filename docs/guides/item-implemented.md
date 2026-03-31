# Item 实现流程（ic2_120）

本文档只描述“如何实现一个 item”，用于后续批量补全 `docs/inventory/assets-inventory.md`。

## 1) 前置检查

1. 从 `docs/inventory/assets-inventory.md` 选定要实现的 ID（如 `item.ic2.xxx`）。
2. 检查是否已存在同名注册（尤其是和方块物品同名时）：
   - Kotlin 注解注册：`@ModItem(name = "...")` 或 `@ModBlock(name = "...", registerItem = true)`
   - 资源文件：`assets/ic2_120/models/item/<id>.json`
   - 语言键：`item.ic2_120.<id>`
3. 若同名已被 `block item` 占用，不要直接重复注册，先记录为“冲突项”。

## 2) 添加物品注册

在 `src/main/kotlin/ic2_120/content/item/` 下的对应文件添加：

- `@ModItem(name = "<id>", tab = CreativeTab.IC2_MATERIALS, group = "<group>")`
- `class Xxx : Item(FabricItemSettings())`

说明：
- `name` 必须与目标 ID 的 path 一致（`item.ic2.<id>` -> `name = "<id>"`）。
- `group` 用于创造栏排序，同类物品保持一致即可。

## 3) 添加模型资源

优先复用 `ic2` 现有资源：

1. 找到源文件：`src/main/resources/assets/ic2/models/item/<id>.json`
2. 复制到：`src/main/resources/assets/ic2_120/models/item/<id>.json`

若 `ic2` 没有该文件，再手写模型并确认贴图路径有效。

## 4) 添加翻译

在以下文件各加一条：

- `src/main/resources/assets/ic2_120/lang/zh_cn.json`
- `src/main/resources/assets/ic2_120/lang/en_us.json`

键格式固定：

- `item.ic2_120.<id>`

值可先沿用 `ic2` 对应翻译，后续再微调。

## 5) 更新清单文档

在 `docs/inventory/assets-inventory.md` 对应条目把 `[ ]` 改成 `[x]`。

注意：清单中是 `item.ic2.<id>`，代码里是 `item.ic2_120.<id>`，这是预期差异（命名空间不同）。

## 6) 带 GUI 的方块物品（机器等）

UI 部分需要计算各组件宽高，给出精确的互不遮挡的 x、y 坐标。

## 6.1) 焦炉四方块材质说明（更正）

以下是 `ic2_120` 当前实现中四个焦炉相关方块的材质/贴图来源：

- `refractory_bricks`（耐火砖）  
  - 方块模型：`assets/ic2_120/models/block/refractory_bricks.json`  
  - 贴图：`ic2:block/refractory_brick`

- `coke_kiln`（焦炭窑主体）  
  - 方块模型：`assets/ic2_120/models/block/coke_kiln.json`  
  - 父模型：`ic2:block/machine/steam/coke_kiln`

- `coke_kiln_hatch`（焦炉窑口）  
  - 方块模型：`assets/ic2_120/models/block/coke_kiln_hatch.json`  
  - 父模型：`ic2:block/machine/steam/coke_kiln_hatch`

- `coke_kiln_grate`（焦炉炉篦）  
  - 方块模型：`assets/ic2_120/models/block/coke_kiln_grate.json`  
  - 父模型：`ic2:block/machine/steam/coke_kiln_grate`

## 7) 验证

最少执行：

- `./gradlew.bat compileKotlin`

通过标准：
- 无重复注册崩溃
- 无 Kotlin 编译错误
- 语言 JSON 语法正确

## 常见坑

- `item` 与 `block item` 同名冲突（例如 `coal_block`）。
- 只改了 `assets/ic2/lang`，忘记改实际使用的 `assets/ic2_120/lang`。
- 新增了通用名，但实际资源只有材质化版本（如扇叶应使用 `wooden/iron/..._rotor_blade`）。
