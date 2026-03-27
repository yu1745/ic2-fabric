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
