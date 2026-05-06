# 物品 NBT → 动态模型切换

通过 `ModelPredicateProvider` + 模型 `overrides`，让物品贴图随 NBT 数据动态变化。

## 适用场景

物品外观需要根据运行时状态变化，例如：

- 绑定/未绑定（传送卷轴）
- 开关状态（纳米剑）
- 模式切换

## 实现步骤

### 1. 注册 Predicate（客户端）

在客户端初始化时，为物品注册一个 `ModelPredicateProvider`：

```kotlin
// BatteryModelPredicates.kt
val SCROLL_BOUND_ID = Identifier(Ic2_120.MOD_ID, "bound")

ModelPredicateProviderRegistry.register(
    scrollItem,
    SCROLL_BOUND_ID
) { stack: ItemStack, _, _, _ ->
    // 根据 NBT 返回 0.0f（未绑定）或 1.0f（已绑定）
    if (stack.nbt?.getBoolean("HasBind") == true) 1.0f else 0.0f
}
```

**关键点**：
- predicate 返回 `Float`（0.0f ~ 1.0f），对应模型 `overrides` 中的值
- 调用位置：`Ic2_120Client.onInitializeClient()` → `BatteryModelPredicates.register()`

### 2. 模型 JSON（resources）

基础模型 `recall_scroll.json` 定义默认贴图，`overrides` 数组定义切换规则：

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/map"
  },
  "overrides": [
    {
      "predicate": {
        "ic2_120:bound": 1.0
      },
      "model": "ic2_120:item/recall_scroll_bound"
    }
  ]
}
```

切换模型 `recall_scroll_bound.json`：

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/filled_map"
  }
}
```

**关键点**：
- `predicate` 键为 `命名空间:predicate名`，必须与代码中 `Identifier` 一致
- `predicate` 值为 1.0，对应 provider 返回的数值
- `model` 值为 `mod_id:item/model_name`，指向另一个模型 JSON

### 3. 设置 NBT

在物品逻辑中写入 NBT，provider 读取后返回对应 float：

```kotlin
// RecallScrollItem.useOnBlock()
stack.orCreateNbt.putBoolean("HasBind", true)
```

## 参考

| 组件 | 位置 |
|------|------|
| Predicate 注册 | `BatteryModelPredicates.kt` |
| 模型 JSON | `assets/ic2_120/models/item/recall_scroll.json` |
| 切换模型 JSON | `assets/ic2_120/models/item/recall_scroll_bound.json` |
| NBT 写入 | `RecallScrollItem.useOnBlock()` |
