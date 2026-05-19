# 1.20.1 → 1.21.1 API 迁移指南

> 基于 125 个 commit 的逐 diff 对比经验，按实际代码变化频率从高到低排列。所有示例均来自本项目的真实迁移 diff。

**建议流程：** 先全局搜索替换高频模式（Identifier、FabricItemSettings、TypedActionResult），再逐一处理需手工调整的项目（CustomPayload、onUse 拆分）。

---

## 1. `Identifier` 构造函数 → `Identifier.of` / `Identifier.ofVanilla`

**频率：★★★★★（全仓约 40+ 处）**

### 变化

`Identifier(String, String)` 构造函数在 1.21.1 中移除，改用静态工厂方法。

### 1.20.1 → 1.21.1

```kotlin
// 旧
Identifier("ic2_120", "some_key")
Identifier("minecraft", "stone")
Identifier(Ic2_120.MOD_ID, "toggle_night_vision_goggles")

// 新
Identifier.of("ic2_120", "some_key")
Identifier.ofVanilla("stone")
Identifier.of(Ic2_120.MOD_ID, "toggle_night_vision_goggles")
```

### 批量替换

```
Identifier("ic2_120", → Identifier.of("ic2_120",
Identifier("minecraft", → Identifier.ofVanilla(
Identifier(MOD_ID, → Identifier.of(MOD_ID,
Identifier(Ic2_120.MOD_ID, → Identifier.of(Ic2_120.MOD_ID,
new Identifier( → Identifier.of(
```

### 不变的情况

- `Identifier.tryParse()` — 仍可用
- `Identifier.toString()`、`Identifier.toTranslationKey()` — 调用链不改

---

## 2. `FabricItemSettings()` → `Item.Settings()`

**频率：★★★★★（全仓约 30+ 处）**

### 变化

Fabric API 的 `FabricItemSettings` 被合并到 MC 原版的 `Item.Settings`。

### 1.20.1 → 1.21.1

```kotlin
// 旧
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
class SomeItem : Item(FabricItemSettings().maxDamage(80))
class SomeItem : Item(FabricItemSettings().maxCount(1))

// 新
import net.minecraft.item.Item
class SomeItem : Item(Item.Settings().maxDamage(80))
class SomeItem : Item(Item.Settings().maxCount(1))
```

### 全局替换

```
FabricItemSettings() → Item.Settings()
import net.fabricmc.fabric.api.item.v1.FabricItemSettings → (删除该行)
```

---

## 3. `TypedActionResult.success(stack, true)` → `TypedActionResult.success(stack)`

**频率：★★★★☆（约 15+ 处）**

### 变化

`TypedActionResult.success(stack, playClientSound: Boolean)` 的第二个参数在 1.21.1 中被移除。

### 1.20.1 → 1.21.1

```kotlin
// 旧
return TypedActionResult.success(stack, true)

// 新
return TypedActionResult.success(stack)
```

---

## 4. `ExtendedScreenHandlerFactory` 泛型 + `getScreenOpeningData`

**频率：★★★★☆（约 12 处）**

### 变化

1.21.1 的 `ExtendedScreenHandlerFactory` 增加了 `<T extends PacketByteBuf>` 泛型参数，`writeScreenOpeningData` 改为返回 `PacketByteBuf` 的 `getScreenOpeningData`。

### 1.20.1 → 1.21.1

```kotlin
// 旧
class MyBlockEntity : ... , ExtendedScreenHandlerFactory {
    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }
}

// 新
class MyBlockEntity : ... , ExtendedScreenHandlerFactory<PacketByteBuf> {
    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }
}
```

### 新增 import

```kotlin
import io.netty.buffer.Unpooled
import net.minecraft.network.PacketByteBuf        // 可能要加（原通过 Fabric API 间接引用）
```

---

## 5. `checkType` → `validateTicker`

**频率：★★★★☆（约 15 处）**

### 变化

`checkType(type, class)` 改为 `validateTicker(world, type, class)`，新增 `world` 参数。

### 1.20.1 → 1.21.1

```kotlin
// 旧
override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>):
        BlockEntityTicker<T>? =
    if (world.isClient) null
    else checkType(type, MyBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

// 新
override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>):
        BlockEntityTicker<T>? =
    if (world.isClient) null
    else validateTicker(world, type, MyBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }
```

### 批量替换

```
checkType(type, → validateTicker(world, type,
```

---

## 6. `onUse` 移除 `Hand` + 新增 `onUseWithItem`

**频率：★★★★☆（约 15 处）**

### 变化

`Block.onUse` 签名移除了 `hand: Hand` 参数。如果原来在 `onUse` 中需要根据 `hand` 取物品判断，需改用新的 `onUseWithItem`。

### 场景 A：纯打开 GUI，不需要 hand

```kotlin
// 旧
override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity,
                   hand: Hand, hit: BlockHitResult): ActionResult {
    if (!world.isClient) player.openHandledScreen(state.createScreenHandlerFactory(world, pos))
    return ActionResult.SUCCESS
}

// 新
override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity,
                   hit: BlockHitResult): ActionResult {
    if (!world.isClient) player.openHandledScreen(state.createScreenHandlerFactory(world, pos))
    return ActionResult.SUCCESS
}
```

### 场景 B：需要判断手持物品

```kotlin
// 旧 — 在 onUse 中通过 player.getStackInHand(hand) 取物品
override fun onUse(... hand: Hand, ...): ActionResult {
    val stack = player.getStackInHand(hand)
    // 处理物品交互...
}

// 新 — 拆分为两个方法
// 空手交互
override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity,
                   hit: BlockHitResult): ActionResult {
    // 非物品交互逻辑
    return ActionResult.PASS   // 返回 PASS 让 onUseWithItem 有机会处理
}

// 手持物品交互
override fun onUseWithItem(stack: ItemStack, state: BlockState, world: World, pos: BlockPos,
                           player: PlayerEntity, hand: Hand, hit: BlockHitResult): ItemActionResult {
    // 处理物品交互...
    return ItemActionResult.SUCCESS
}
```

### 新增 import

```kotlin
import net.minecraft.util.ItemActionResult
```

---

## 7. `appendTooltip` 签名变更

**频率：★★★★☆（约 12 处）**

### 1.20.1 → 1.21.1

```kotlin
// 旧
@Environment(EnvType.CLIENT)
override fun appendTooltip(
    stack: ItemStack,
    world: World?,
    tooltip: MutableList<Text>,
    context: TooltipContext
) {
    super.appendTooltip(stack, world, tooltip, context)
}

// 新 — @Environment 移除，参数顺序改变，类型不同
override fun appendTooltip(
    stack: ItemStack,
    context: Item.TooltipContext,
    tooltip: MutableList<Text>,
    type: TooltipType
) {
    super.appendTooltip(stack, context, tooltip, type)
}
```

### 变化点总结

| 1.20.1 | 1.21.1 |
|--------|--------|
| `world: World?` | 移除 |
| `context: TooltipContext` | `context: Item.TooltipContext`（不同包）|
| 无对应参数 | 新增 `type: TooltipType` |
| `@Environment(EnvType.CLIENT)` | 移除（不再需要） |

### 批量替换

```
override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext)
→ override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType)

super.appendTooltip(stack, world, tooltip, context)
→ super.appendTooltip(stack, context, tooltip, type)

@Environment(EnvType.CLIENT)\n    override fun appendTooltip
→ override fun appendTooltip
```

---

## 8. C2S 网络包：CustomPayload + PacketCodec 体系

**频率：★★★☆☆（约 8 处，含所有 toggle 包）**

### 变化

旧 `PacketByteBuf` + `Identifier` 的 C2S 网络包体系被 `CustomPayload` + `PacketCodec` 替代。

### 新旧对比

#### 服务端接收

```kotlin
// 旧
ServerPlayNetworking.registerGlobalReceiver(TOGGLE_NIGHT_VISION_PACKET) { server, player, _, _, _ ->
    server.execute {
        val stack = player.getEquippedStack(EquipmentSlot.HEAD)
        // ...
    }
}

// 新
ServerPlayNetworking.registerGlobalReceiver(ToggleNightVisionGogglesPayload.ID) { _, context ->
    val player = context.player()
    val stack = player.getEquippedStack(EquipmentSlot.HEAD)
    // ...
}
```

注意：新的回调中 `context.player()` 已经在服务端线程中，不需要 `server.execute {}`。

#### 客户端发送

```kotlin
// 旧
ClientPlayNetworking.send(TOGGLE_NIGHT_VISION_GOGGLES_PACKET, PacketByteBuf(Unpooled.buffer()))

// 新
ClientPlayNetworking.send(ToggleNightVisionGogglesPayload)
```

#### 无参数 Payload（用于 toggle 按键）

```kotlin
package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

object ToggleNightVisionGogglesPayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleNightVisionGogglesPayload>(Ic2_120.id("toggle_night_vision_goggles"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleNightVisionGogglesPayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleNightVisionGogglesPayload }
    )
    override fun getId() = ID
}
```

#### 有参数 Payload（如选择模板）

```kotlin
class SelectTemplatePayload(val pos: BlockPos, val index: Int) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<SelectTemplatePayload>(Ic2_120.id("select_template"))
        val CODEC: PacketCodec<PacketByteBuf, SelectTemplatePayload> = PacketCodec.of(
            { value, buf ->
                buf.writeBlockPos(value.pos)
                buf.writeVarInt(value.index)
            },
            { buf ->
                SelectTemplatePayload(buf.readBlockPos(), buf.readVarInt())
            }
        )
    }
}
```

#### 注册 Payload

```kotlin
// 在 NetworkManager.registerPayloadTypes() 中
PayloadTypeRegistry.playC2S().register(ToggleNightVisionGogglesPayload.ID, ToggleNightVisionGogglesPayload.CODEC)
PayloadTypeRegistry.playC2S().register(SelectTemplatePayload.ID, SelectTemplatePayload.CODEC)

// S2C 同理
PayloadTypeRegistry.playS2C().register(ReactorHeatInfoPacket.ID, ReactorHeatInfoPacket.CODEC)
```

### 汇总变化

| 1.20.1 | 1.21.1 |
|--------|--------|
| `val ID = Identifier("mod", "name")` | `val ID = CustomPayload.Id<T>(...)` |
| `ServerPlayNetworking.registerGlobalReceiver(id) { server, player, handler, buf, sender ->` | `ServerPlayNetworking.registerGlobalReceiver(payloadId) { payload, context ->` |
| `ClientPlayNetworking.send(id, buf)` | `ClientPlayNetworking.send(payloadObject)` |
| `client.execute {}` 包裹 UI 线程逻辑 | 不需要，已在线程中 |
| `server.execute {}` 包裹服务端逻辑 | `context.player()` 已在线程中 |

### 新增 import

```kotlin
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.codec.PacketCodec
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
```

---

## 9. `Consumer<RecipeJsonProvider>` → `RecipeExporter`

**频率：★★★☆☆（约 6 处）**

### 1.20.1 → 1.21.1

```kotlin
// 旧 — 改两个地方
import net.minecraft.data.server.recipe.RecipeJsonProvider
import java.util.function.Consumer

companion object {
    @RecipeProvider
    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) { ... }
}

// 新
import net.minecraft.data.server.recipe.RecipeExporter

companion object {
    @RecipeProvider
    fun generateRecipes(exporter: RecipeExporter) { ... }
}
```

### 配套：`ClassScanner` 调用签名

```kotlin
// 旧
fun generateAllRecipes(recipeExporter: Consumer<RecipeJsonProvider>) { ... }

// 新
fun generateAllRecipes(recipeExporter: RecipeExporter) { ... }
```

### 配套：`RecipeCategory.TOOLS` → `RecipeCategory.EQUIPMENT`

```kotlin
// 旧
RecipeCategory.TOOLS

// 新
RecipeCategory.EQUIPMENT
```

---

## 10. `readNbt` / `writeNbt` 增加 `RegistryWrapper.WrapperLookup` 参数

**频率：★★★☆☆（所有 BlockEntity，约 20+ 处）**

### 变化

`readNbt(nbt)` → `readNbt(nbt, lookup)`，`writeNbt(nbt)` → `writeNbt(nbt, lookup)`。连带 `Inventories.readNbt` / `writeNbt` 也需要 lookup。

### 1.20.1 → 1.21.1

```kotlin
// 旧
override fun readNbt(nbt: NbtCompound) {
    super.readNbt(nbt)
    Inventories.readNbt(nbt, inventory)
}

override fun writeNbt(nbt: NbtCompound) {
    super.writeNbt(nbt)
    Inventories.writeNbt(nbt, inventory)
}

// 新
override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
    super.readNbt(nbt, lookup)
    Inventories.readNbt(nbt, inventory, lookup)
}

override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
    super.writeNbt(nbt, lookup)
    Inventories.writeNbt(nbt, inventory, lookup)
}
```

### 新增 import

```kotlin
import net.minecraft.registry.RegistryWrapper
```

---

## 11. `SingleStackRecipeInput` 替代 `SimpleInventory` 单格检测

**频率：★★★☆☆（约 6 处）**

### 变化

对于只需要一个物品输入来匹配配方的场景，`SimpleInventory(stack)` 改为 `SingleStackRecipeInput(stack)`。

### 1.20.1 → 1.21.1

```kotlin
// 旧
import net.minecraft.inventory.SimpleInventory
val inv = SimpleInventory(stack.copyWithCount(stack.maxCount))
val match = w.recipeManager.getFirstMatch(recipeType, inv, w)

// 新
import net.minecraft.recipe.input.SingleStackRecipeInput
val match = w.recipeManager.getFirstMatch(recipeType, SingleStackRecipeInput(stack.copyWithCount(stack.maxCount)), w)
```

### 注意：`getFirstMatch` 返回值变化

1.21.1 的 `getFirstMatch` 返回 `Optional<RecipeHolder<T>>` 而非 `Optional<T>`，取 recipe 时需 `.value`：

```kotlin
// 旧
val recipe = match.get()
val recipe = match.orElse(null)

// 新
val recipe = match.get().value
val recipe = match.orElse(null)?.value
```

---

## 12. `FluidVariant.fromNbt` / `toNbt` → `FluidVariant.CODEC`

**频率：★★★☆☆（约 4 处）**

### 1.20.1 → 1.21.1

```kotlin
// 旧 — 读取
leftTankInternal.variant = FluidVariant.fromNbt(leftFluidTag)

// 新 — 读取
leftTankInternal.variant = FluidVariant.CODEC.decode(NbtOps.INSTANCE, leftFluidTag)
    .result().map { it.first }.orElse(FluidVariant.blank())

// 旧 — 写入
nbt.put(NBT_LEFT_FLUID_VARIANT, leftTankInternal.variant.toNbt())

// 新 — 写入
nbt.put(NBT_LEFT_FLUID_VARIANT, FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, leftTankInternal.variant)
    .result().orElse(NbtCompound()))
```

### 新增 import

```kotlin
import net.minecraft.nbt.NbtOps
```

---

## 13. `ItemStack.fromNbt` / `encode` 增加 `WrapperLookup`

**频率：★★☆☆☆（约 2 处）**

### 1.20.1 → 1.21.1

```kotlin
// 旧
val stack = ItemStack.fromNbt(nbt)
val outNbt = stack.writeNbt(NbtCompound())

// 新
val stack = ItemStack.fromNbt(nbt, lookup)
val outNbt: NbtCompound = stack.encode(lookup) as NbtCompound
```

---

## 14. `AxeItem` / `HoeItem` / `SwordItem` 等构造参数简化

**频率：★★☆☆☆（约 3 处）**

### 1.20.1 → 1.21.1

```kotlin
// 旧
class BronzeAxe : AxeItem(BronzeToolMaterial, 5.0f, -3.0f, FabricItemSettings().maxCount(1))

// 新
class BronzeAxe : AxeItem(BronzeToolMaterial, Item.Settings())
```

MC 1.21.1 的工具类构造函数改为从 `ToolMaterial` 中读取攻击伤害和速度，不再需要手写 attackDamage 和 attackSpeed。

---

## 15. Block `getCodec()` / `MapCodec` 要求

**频率：★★☆☆☆（所有 Block）**

### 变化

1.21.1 要求每个 Block 提供一个 `getCodec()` 方法用于数据驱动。

### 1.20.1 → 1.21.1

```kotlin
// 旧 — 什么都不需要

// 新 — 需要在 companion 中添加 CODEC，在类中添加 getCodec()
class MyBlock(...) : BlockWithEntity(...) {
    override fun getCodec(): MapCodec<out BlockWithEntity> = MY_BLOCK_CODEC

    companion object {
        val MY_BLOCK_CODEC: MapCodec<MyBlock> = Block.createCodec {
            error("MyBlock cannot be deserialized from JSON")
        }
    }
}
```

对于非 `BlockWithEntity` 的普通 `Block`，同样需要：

```kotlin
val MY_BLOCK_CODEC: MapCodec<MyBlock> = Block.createCodec { MyBlock(...) }
```

### 新增 import

```kotlin
import com.mojang.serialization.MapCodec
```

---

## 16. `data` 目录单数化（影响 datagen）

**频率：★★☆☆☆（datagen 配置变化）**

### 路径变化

| 1.20.1（复数） | 1.21.1（单数） |
|----------------|----------------|
| `data/mod/recipes/` | `data/mod/recipe/` |
| `data/mod/advancements/` | `data/mod/advancement/` |
| `data/mod/loot_tables/` | `data/mod/loot_table/` |
| `data/mod/tags/items/` | `data/mod/tag/item/` |
| `data/mod/tags/blocks/` | `data/mod/tag/block/` |

### 影响

- 运行 `./gradlew runDatagen` 后 datagen 会自动输出到新路径
- 旧路径的文件不会被 1.21.1 读取，必须清理旧 JSON（`rm -rf generated/`）
- Tag 的 `replace` 语义也已移除，JSON 中的 `replace: true/false` 字段需要删除

---

## 17. `@Inject` 替代 `@Redirect`（Mixin 写法变化）

**频率：★★☆☆☆（约 1 处）**

### 变化

1.21.1 没有 refMap，`@Redirect` 不可靠，推荐改用 `@Inject`。

```kotlin
// 旧
@Redirect(method = "tickEgg", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/ChickenEntity;dropItem(Lnet/minecraft/item/ItemStack;)V"))
fun redirectChickenDropItem(chicken: ChickenEntity, stack: ItemStack) { /* no-op */ }

// 新
@Inject(method = "tickEgg", at = @At("HEAD"), cancellable = true)
fun onTickEgg(chicken: ChickenEntity, callbackInfo: CallbackInfo) {
    // 在 tickEgg 中阻止下蛋逻辑
}
```

---

## 18. 其他零散变化

| # | 变化 | 旧 | 新 | 频率 |
|---|------|----|----|:----:|
| 1 | `ServerPlayNetworking.send(player, id, buf)` | 传 Identifier | `ServerPlayNetworking.send(player, payload)` | ★★★ |
| 2 | `ClientPlayNetworking.registerGlobalReceiver(id) { client, handler, buf, sender ->` | Identifier | `ClientPlayNetworking.registerGlobalReceiver(payloadId) { payload, context ->` | ★★★ |
| 3 | `RecipeCategory.TOOLS` | `TOOLS` | `EQUIPMENT` | ★★ |
| 4 | `ItemStack.areItemsEqual(a, b)` → `ItemStack.areEqual(a, b)` | `areItemsEqual` | `areEqual` | ★★ |
| 5 | `ItemTooltipCallback.EVENT.register { player, stack, context, lines ->` | `TooltipContext` | `Item.TooltipContext` | ★★ |
| 6 | `getLeashHolder()` → `getLeashHolder(player)` | 无参 | `player` 参数 | ★ |
| 7 | `onBreak(world, pos, state, player)` 返回值 | `void` | `BlockState` | ★ |
| 8 | tag JSON `replace` 字段 | `"replace": true` | 移除该字段 | ★ |
| 9 | `SemifluidGeneratorFuelStatePacket.read(buf)` | 手写 read | `PacketCodec` 自动 | ★ |

---

## 附：一表速查

| 变更 | 1.20.1 | 1.21.1 | 批量搜索路径 |
|------|--------|--------|:------------|
| Identifier | `Identifier("mod","path")` | `Identifier.of("mod","path")` | `Identifier("` |
| Item 构造 | `FabricItemSettings()` | `Item.Settings()` | `FabricItemSettings` |
| TypedActionResult | `.success(stack, true)` | `.success(stack)` | `success(stack,` |
| getTicker | `checkType(type, cls)` | `validateTicker(world, type, cls)` | `checkType` |
| onUse | `onUse(..., hand, hit)` | `onUse(..., hit)` + `onUseWithItem(...)` | `hand: Hand` |
| appendTooltip | `(stack, world, tooltip, ctx)` | `(stack, ctx, tooltip, type)` | `appendTooltip` |
| ScreenHandlerFactory | `writeScreenOpeningData(buf)` | `getScreenOpeningData(): buf` | `writeScreenOpeningData` |
| ExtendedScreenHandlerFactory | 无泛型 | `<PacketByteBuf>` | `ExtendedScreenHandlerFactory` |
| C2S 包 | `Identifier` + 手写读写 | `CustomPayload` + `PacketCodec` | `registerGlobalReceiver` |
| 配方 | `Consumer<RecipeJsonProvider>` | `RecipeExporter` | `RecipeJsonProvider` |
| readNbt | `readNbt(nbt)` | `readNbt(nbt, lookup)` | `readNbt\(` |
| writeNbt | `writeNbt(nbt)` | `writeNbt(nbt, lookup)` | `writeNbt\(` |
| 单格配方输入 | `SimpleInventory(stack)` | `SingleStackRecipeInput(stack)` | `SimpleInventory` |
| FluidVariant | `.fromNbt(nbt)` / `.toNbt()` | `.CODEC.decode/encode` | `fromNbt`/`toNbt` |
| ItemStack NBT | `.fromNbt(nbt)` | `.fromNbt(nbt, lookup)` | `fromNbt(` |
| Block codec | 不需要 | `getCodec()` + `CODEC` | `BlockWithEntity` |
| data 目录 | `recipes/`,`advancements/` | `recipe/`,`advancement/` | datagen 输出路径 |
| 工具类构造 | `AxeItem(mat, dmg, spd, settings)` | `AxeItem(mat, settings)` | `AxeItem(` |
