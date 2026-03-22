# 声音系统实现计划

## Context

`MachineSoundConfig` 已定义但完全未使用，`sounds.json` 定义了 59 个声音事件但无机器播放。用户要求：
1. 在 `setActiveState` 里直接调用 `playSound`（服务端调用会自动发包到客户端）
2. 将 `setActiveState` 提升到基类 `MachineBlockEntity`，消除重复定义
3. 更新 `docs/sounds-inventory.md` 标记实现状态

---

## 声音 → 机器映射

| 声音事件 | 声音资源 | 对应 BlockEntity | 状态 |
|---|---|---|---|
| generator.generator.loop | generators/generator | GeneratorBlockEntity | ✅ |
| generator.geothermal.loop | generators/geothermal | ？（地热未独立实现） | ❌ |
| generator.nuclear.loop | generators/nuclear/reactor | ？（核电未独立实现） | ❌ |
| generator.water.loop | generators/water | WaterGeneratorBlockEntity | ✅ |
| generator.wind.loop | generators/wind | WindGeneratorBlockEntity | ✅ |
| machine.furnace.iron.operate | machines/furnace/iron/operate | IronFurnaceBlockEntity | ✅ |
| machine.furnace.electric/start | machines/furnace/electric/start | ElectricFurnaceBlockEntity | ✅ |
| machine.furnace.electric/stop | machines/furnace/electric/stop | ElectricFurnaceBlockEntity | ✅ |
| machine.furnace.electric/loop | machines/furnace/electric/loop | ElectricFurnaceBlockEntity | ✅ |
| machine.furnace.induction/start | machines/furnace/induction/start | InductionFurnaceBlockEntity | ✅ |
| machine.furnace.induction/stop | machines/furnace/induction/stop | InductionFurnaceBlockEntity | ✅ |
| machine.furnace.induction/loop | machines/furnace/induction/loop | InductionFurnaceBlockEntity | ✅ |
| machine.macerator.operate | machines/macerator/operate | MaceratorBlockEntity | ✅ |
| machine.compressor.operate | machines/compressor/operate | CompressorBlockEntity | ✅ |
| machine.extractor.operate | machines/extractor/operate | ExtractorBlockEntity | ✅ |
| machine.canner.operate | machines/canner/operate | CannerBlockEntity | ✅ |
| machine.canner.reverse | machines/canner/reverse | CannerBlockEntity | ✅ |
| machine.recycler.operate | machines/recycler/operate | RecyclerBlockEntity | ✅ |
| machine.pump.operate | machines/pump/operate | PumpBlockEntity | ✅ |
| machine.electrolyzer.loop | machines/electrolyzer/loop | ？（不存在） | ❌ |
| machine.fabricator.loop | machines/fabricator/loop | ？（不存在） | ❌ |
| machine.fabricator.scrap | machines/fabricator/scrap | ？（不存在） | ❌ |
| machine.miner.operate | machines/miner/operate | ？（不存在） | ❌ |
| machine.o_mat.operate | machines/o_mat/operate | ？（不存在） | ❌ |
| machine.teleporter.charge | machines/teleporter/charge | ？（不存在） | ❌ |
| machine.teleporter.use | machines/teleporter/use | ？（不存在） | ❌ |
| machine.terraformer.loop | machines/terraformer/loop | ？（不存在） | ❌ |

---

## 修改文件清单

### 需要声音的 Block（13 个）

| Block | 声音配置 |
|---|---|
| GeneratorBlock | `SoundEvent.of(Identifier("ic2", "generators/generator"))` |
| WindGeneratorBlock | `SoundEvent.of(Identifier("ic2", "generators/wind"))` |
| WaterGeneratorBlock | `SoundEvent.of(Identifier("ic2", "generators/water"))` |
| IronFurnaceBlock | `SoundEvent.of(Identifier("ic2", "machines/furnace/iron/operate"))` |
| ElectricFurnaceBlock | start+stop+loop 三个事件 |
| InductionFurnaceBlock | start+stop+loop 三个事件 |
| MaceratorBlock | `SoundEvent.of(Identifier("ic2", "machines/macerator/operate"))` |
| CompressorBlock | `SoundEvent.of(Identifier("ic2", "machines/compressor/operate"))` |
| ExtractorBlock | `SoundEvent.of(Identifier("ic2", "machines/extractor/operate"))` |
| CannerBlock | operate+reverse 两个事件 |
| RecyclerBlock | `SoundEvent.of(Identifier("ic2", "machines/recycler/operate"))` |
| PumpBlock | `SoundEvent.of(Identifier("ic2", "machines/pump/operate"))` |

### 需要修改的 BlockEntity（13 个有声音 + 6 个无声音）

有声音：`GeneratorBlockEntity`、`WindGeneratorBlockEntity`、`WaterGeneratorBlockEntity`、`IronFurnaceBlockEntity`、`ElectricFurnaceBlockEntity`、`InductionFurnaceBlockEntity`、`MaceratorBlockEntity`、`CompressorBlockEntity`、`ExtractorBlockEntity`、`CannerBlockEntity`、`RecyclerBlockEntity`、`PumpBlockEntity`

无声音（删除私有 setActiveState）：`CentrifugeBlockEntity`、`MetalFormerBlockEntity`、`OreWashingPlantBlockEntity`、`BlockCutterBlockEntity`、`FluidBottlerBlockEntity`、`BlastFurnaceBlockEntity`

---

## 详细实现步骤

### Step 1: 修改 `MachineBlockEntity` 基类

`src/main/kotlin/ic2_120/content/block/machines/MachineBlockEntity.kt`：

```kotlin
package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.item.energy.IBatteryItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

abstract class MachineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ITieredMachine {

    companion object {
        const val FUEL_SLOT = 0
        const val BATTERY_SLOT = 1
    }

    abstract override val tier: Int

    /** 子类需提供 ACTIVE 属性（BooleanProperty） */
    protected abstract val activeProperty: BooleanProperty

    /** 子类需提供声音事件（无声音返回 null） */
    protected open val soundEvent: SoundEvent? = null

    /** 子类需提供声音音量，默认 0.5 */
    protected open val soundVolume: Float = 0.5f

    /** 子类需提供声音音调，默认 1.0 */
    protected open val soundPitch: Float = 1.0f

    /**
     * 统一的 setActiveState 实现：
     * - 更新 BlockState 的 ACTIVE 属性
     * - 在 ACTIVE 变化时播放声音（服务端播放会自动同步到客户端）
     */
    protected fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(activeProperty) != active) {
            world.setBlockState(pos, state.with(activeProperty, active))
            // 服务端播放声音会自动同步到客户端
            soundEvent?.let { sound ->
                if (!world.isClient) {
                    world.playSound(null, pos, sound, SoundCategory.BLOCKS, soundVolume, soundPitch)
                }
            }
        }
    }

    protected abstract fun getInventory(): net.minecraft.inventory.Inventory?
}
```

### Step 2: 修改有声音的 Block 类

每个 Block 在 companion object 中添加声音常量：

```kotlin
// 例：GeneratorBlock.kt
companion object {
    val ACTIVE = Properties.POWERED  // 或 BooleanProperty.of("active")
    val SOUND_EVENT = SoundEvent.of(Identifier("ic2", "generators/generator"))
    const val SOUND_VOLUME = 0.5f
    const val SOUND_PITCH = 1.0f
}
```

### Step 3: 修改 BlockEntity

每个 BlockEntity：
1. 删除私有的 `setActiveState` 方法
2. 继承基类的 `setActiveState`
3. 实现 `activeProperty` 属性
4. 实现 `soundEvent` / `soundVolume` / `soundPitch` 属性
5. 已有 `setActiveState` 调用处保持不变

**ElectricFurnaceBlockEntity / InductionFurnaceBlockEntity**（三个声音 start/stop/loop）：

```kotlin
override val soundEvent: SoundEvent? = null  // 禁用基类声音

private fun playMachineSound(sound: SoundEvent) {
    world?.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.5f, 1.0f)
}

// 在 tick 中手动播放：
val wasActive = state.get(ElectricFurnaceBlock.ACTIVE)
setActiveState(world, pos, state, true)
if (!wasActive) playMachineSound(ElectricFurnaceBlock.START_SOUND)
```

**GeneratorBlockEntity / WindGeneratorBlockEntity**（内联了 setActiveState）：
- 将内联代码改为调用 `setActiveState`

**MaceratorBlockEntity**（内联了 active 计算）：
- 在 tick 末尾添加 `setActiveState` 调用

**无声音的 BlockEntity**：
- 删除私有 `setActiveState` 方法
- 实现 `activeProperty` 返回 null 或覆盖 `soundEvent = null`

### Step 4: 更新 `docs/sounds-inventory.md`

根据实现状态打 ✅/❌，不存在机器标记 `[N/A]`

---

## 编译验证

```bash
./gradlew clean compileKotlin compileClientKotlin
```

游戏内测试：放置各机器观察声音是否正确播放。
