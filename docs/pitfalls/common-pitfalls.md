# 踩坑记录

用于记录项目开发过程中确认过的高频坑点，后续新增条目按序号追加。

## 1. `BlockWithEntity` 未显式返回 `MODEL` 导致方块透明/不可见

- 现象：
  - 方块能放置、状态也在变化，但世界里看起来透明或完全不可见。
  - 资源日志不一定有 `Missing model/texture` 报错，容易误判成贴图路径问题。
- 根因：
  - 方块继承 `BlockWithEntity`，但未覆写 `getRenderType`。
  - 在这种情况下可能走到不可见渲染路径，而不是 JSON 模型渲染。
- 修复方式：
  - 在方块类中显式添加：

```kotlin
override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
```

- 适用范围：
  - 所有继承 `BlockWithEntity` 且依赖 blockstate/model JSON 渲染的方块（作物、机器、容器等）。
- 本项目案例：
  - `CropBlock` 种植 `nether_wart` 后“看不见”。
  - 最终修复点：`src/main/kotlin/ic2_120/content/block/CropBlock.kt`。

## 2. 边界 chunk 上的自动化邻居查询可能形成保活链

- 现象：
  - 两台机器分别放在相邻 chunk 边界两侧，并都安装抽入升级。
  - 两边先被外部加载源“点火”（例如玩家、forceload、区块加载器）后，只解除其中一侧的强制加载，另一侧仍可能让两边机器继续 tick。
  - 实测中，解除单侧 forceload 后，左右两侧机器的抽入逻辑仍持续产生跨 chunk lookup；两边都解除加载源并等待卸载后，计数停止。
- 根因：
  - 加载互锁的直接原因是：自动化逻辑会在 tick 中跨 chunk 调用邻居方块/能力，并且调用前没有统一的“是否允许跨区块访问”的保护。
  - 当 A chunk 的机器 tick 时，它会查询 B chunk 边界机器；B chunk 的机器 tick 时，又反向查询 A chunk。两边都被点火加载过之后，这种跨区块邻居访问可能互相维持对方处于 loaded/ticking 状态。
  - BE tick / 网络 tick 中主动访问邻居能力，例如：

```kotlin
ItemStorage.SIDED.find(world, pos.offset(dir), dir.opposite)
FluidStorage.SIDED.find(world, pos.offset(dir), dir.opposite)
EnergyStorage.SIDED.find(world, neighborPos, side)
world.getBlockEntity(neighborPos)
world.getBlockState(neighborPos)
```

  - 这类访问若跨 chunk 且没有保护，可能让边界另一侧保持 loaded/ticking，形成“已点火后单侧自愈”的保活链。
  - 抽入升级只是实测样本；同类风险也存在于物品弹出、流体抽入/弹出、电网、管道、动能网络、贴脸能量传输、热/蒸汽邻接传输等自动系统中。
- 重要边界：
  - 没有外部加载源时，两台机器不会凭空开始 tick。
  - 仅加 `isChunkLoaded` 不一定能完全阻断已点火后的单侧自愈，因为实测中被解除 forceload 的一侧在 lookup 前已经是 loaded/ticking。
  - 若未来要修，应抽出统一 helper，明确“自动化 tick 是否允许跨 chunk 访问”，而不是只在单个升级组件里零散补判断。
- 当前状态：
  - 本条仅记录已验证洞察与风险面，当前不修。
