# MC 1.20.1 → 1.21.1 API 迁移修复计划

## 验证方法

每步完成后：
```bash
./gradlew compileKotlin -x test 2>&1 | grep "^e:" | wc -l
```

最终验证：
```bash
./gradlew clean compileKotlin compileClientKotlin -x test
```

---

## 第 1 步：`Item.appendTooltip` 签名变更（~25 处）

### 新 API
```java
public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type)
```
- `context` 从 `TooltipContext?`（可空）变为 `TooltipContext`（非空）
- 第四个参数 `type` 类型从 enum → interface
- `TooltipType.BASIC` / `TooltipType.ADVANCED` 取代 enum 常量

### 修复模式
```kotlin
// 旧
override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext)
// 新
override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType)
```

### 涉及文件
- CarbonAndEnergyItems.kt ×2, Tools.kt ×2, Upgrades.kt ×2
- MiningLaserItem.kt, OdScannerItem.kt ×2, CropSeedItems.kt ×2
- ElectricArmorItem.kt, JetpackItem.kt
- NanoBoots.kt, NanoChestplate.kt, NanoHelmet.kt, NanoLeggings.kt
- QuantumBoots.kt, QuantumChestplate.kt, QuantumHelmet.kt, QuantumLeggings.kt
- BatteryItemBase.kt
- AbstractDamageableReactorComponent.kt, AbstractFiniteNeutronReflectorItem.kt
- StorageBoxBlocks.kt, TankBlocks.kt

---

## 第 2 步：`writeNbt`/`readNbt` 新增 `registries` 参数（~10 处）

Inventory NBT 方法需要 `RegistryWrapper.WrapperLookup` 参数。

### 修复模式
```kotlin
// 旧
Inventory.writeNbt(nbt, stacks)
// 新
Inventory.writeNbt(nbt, stacks, lookup)
```

### 涉及文件
- NuclearReactorBlockEntity.kt L311 L339
- ReactorFluidPortBlockEntity.kt L150 L155
- EnergyStorageBlockEntity.kt L118 L127
- StorageBoxBlockEntity.kt L189 L202 L206
- ContainmentBoxItem.kt L117 L128
- CokeKilnGrateBlockEntity.kt L104 (`FluidVariant.toNbt()` → 用 CODEC)

---

## 第 3 步：`ItemStack.canCombine` → `areItemsAndComponentsEqual`（~20 处）

### 涉及文件
AnimalSlaughtererBlockEntity, AnimalmatronBlockEntity, CokeKilnBlockEntity (L138 L174),
FluidHeatExchangerBlockEntity, FluidHeatGeneratorBlockEntity, GeoGeneratorBlockEntity,
MatterGeneratorBlockEntity, MinerBlockEntity, PumpBlockEntity, ReplicatorBlockEntity,
SemifluidGeneratorBlockEntity, SolarDistillerBlockEntity, WaterGeneratorBlockEntity,
NuclearReactorBlockEntity (L720)

---

## 第 4 步：`Item` 不可解析（~10 处）

Import 路径问题。Boats.kt, Ic2BoatItem.kt, ReactorHeatExchangers.kt, ReactorHeatVents.kt,
AbstractFiniteNeutronReflectorItem.kt, StorageBoxBlocks.kt, TankBlocks.kt

---

## 第 5 步：`checkType` / getTicker 类型推断修复（~3 处）

- ManualKineticGeneratorBlock.kt L71
- TransformerBlocks.kt L104
- TransmissionBlocks.kt L168

**修复**：显式指定 lambda 参数类型

---

## 第 6 步：`getPickStack` 签名变更（~3 处）

`BlockView` → `WorldView`
- PatternStorageBlock.kt, StorageBoxBlocks.kt, TankBlocks.kt

---

## 第 7 步：`Entity` 相关变更

- `initDataTracker()` → `initDataTracker(builder: DataTracker.Builder)`（LaserProjectileEntity.kt）
- `setDimensions()` → `dimensions()`（ModEntities.kt ×5）
- `canUsePortals()` → `canUsePortals(allowVehicles: Boolean)`（LaserProjectileEntity.kt）

---

## 第 8 步：`Fluid` API 变更

- `getFlowSpeed` → 移除（ModFluids.kt L285）
- `canFillWithFluid` → 新增 `player` 参数（ModFluids.kt L453）

---

## 第 9 步：`ToolMaterial` / `Enchantment`

- `getInverseTag()` → `getIncorrectBlocksForDrops()`（Tools.kt L94 L1044）
- Enchantment `RegistryKey` → `RegistryEntry`（Tools.kt L987）
- `ItemEnchantmentsComponent` 用 builder（Tools.kt L1002）

---

## 第 10 步：`StatusEffect.applyUpdateEffect` → 返回 `Boolean`

- RadiationStatusEffect.kt L19

---

## 第 11 步：`Block.onBreak` → 返回 `BlockState`

- ManualKineticGeneratorBlock.kt L86

---

## 第 12 步：其���一次性变更

- `FoodComponent` 包名变更（MetalFormerParts.kt）
- `isDamageable` 从 Item 移除（MiningLaserItem.kt, ElectricArmorItem.kt）
- `getMaxUseTime` 新增 LivingEntity 参数（MetalFormerParts.kt）
- TankBlocks.kt companion object 冲突合并
- `BlockRenderType` import 恢复（ComposeDebugBlock.kt）
- `Identifier` → `Identifier.of()`（BandwidthHudPacket.kt）
- `CokeKilnGrateBlock` 实现 `getCodec()`（CokeKilnBlocks.kt）
