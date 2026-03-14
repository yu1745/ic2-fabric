# 普通机器实现流程（ic2_120）

本文档整合了实现一台**普通加工机器**（如粉碎机、压缩机、金属成型机）的完整流程。

**目标**：新机器可加工物品、消耗能量、支持电池供电、支持三种升级（加速/储能/高压）。

---

## 目录

1. [前置决策](#1-前置决策)
2. [创建 Block 方块](#2-创建-block-方块)
3. [创建 BlockEntity](#3-创建-blockentity)
4. [创建 ScreenHandler](#4-创建-screenhandler)
5. [创建 Screen（客户端 UI）](#5-创建-screen客户端-ui)
6. [添加模型资源](#6-添加模型资源)
7. [添加翻译](#7-添加翻译)
8. [注册](#8-注册)
9. [验证](#9-验证)

---

## 1. 前置决策

在开始之前，确定以下信息：

| 项目 | 值 | 说明 |
|------|-----|------|
| **机器 ID** | `xxx` | 如 `macerator`、`compressor` |
| **中文名称** | `粉碎机` | 用于翻译和显示 |
| **能量等级** | `1-4` | 1=LV(32 EU/t), 2=MV(128 EU/t), 3=HV(512 EU/t), 4=EV(2048 EU/t) |
| **能量容量** | `xxx EU` | 建议为 4-10 秒的耗能 |
| **每 tick 耗能** | `x EU/t` | 默认耗能速率 |
| **加工时间** | `xxx ticks` | 完成一次加工所需 ticks |

---

## 2. 创建 Block 方块

**文件**：`src/main/kotlin/ic2_120/content/block/XxxBlock.kt`

```kotlin
package ic2_120.content.block

import ic2_120.content.block.machines.XxxBlockEntity
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.AbstractBlock.Settings
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier

@ModBlock(name = "xxx", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class XxxBlock : MachineBlock(Settings.create()) {

    companion object {
        val ACTIVE = Properties.FACING_HOPPER // 或者使用 Properties.POWERED
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        XxxBlockEntity(pos, state)

    override fun getTicker(world: World?, state: BlockState?, type: BlockEntityType<XxxBlockEntity>?): BlockEntityTicker<XxxBlockEntity>? =
        BlockEntityTicker { world, pos, state, blockEntity -> blockEntity.tick(world, pos, state) }
}
```

**要点**：
- 继承 `MachineBlock`（提供了基础的方块行为）
- 使用 `@ModBlock` 注解注册方块和物品
- `ACTIVE` 属性用于显示激活状态（工作时有动画/模型变化）

---

## 3. 创建 BlockEntity

**文件**：`src/main/kotlin/ic2_120/content/block/machines/XxxBlockEntity.kt`

### 3.1 类声明与接口

```kotlin
@ModBlockEntity(block = XxxBlock::class)
class XxxBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state),
    Inventory,
    ITieredMachine,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    ExtendedScreenHandlerFactory {

    // 升级属性
    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val XXX_TIER = 1           // 能量等级
        const val SLOT_INPUT = 0          // 输入槽
        const val SLOT_OUTPUT = 1         // 输出槽
        const val SLOT_DISCHARGING = 2    // 电池放电槽
        const val SLOT_UPGRADE_0 = 3      // 升级槽 0
        const val SLOT_UPGRADE_1 = 4      // 升级槽 1
        const val SLOT_UPGRADE_2 = 5      // 升级槽 2
        const val SLOT_UPGRADE_3 = 6      // 升级槽 3
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 7       // 总槽位数
    }

    override val tier: Int = XXX_TIER
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    // 同步数据
    val syncedData = SyncedData(this)

    // 能量存储（支持升级）
    @RegisterEnergy
    val sync = XxxSync(
        syncedData,
        { world?.time },
        { capacityBonus },                                              // 储能升级
        { TransformerUpgradeComponent.maxInsertForTier(XXX_TIER + voltageTierBonus) }  // 高压升级
    )

    // 电池放电组件
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { XXX_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )
}
```

### 3.2 Inventory 接口实现

```kotlin
override fun size(): Int = INVENTORY_SIZE
override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
override fun setStack(slot: Int, stack: ItemStack) {
    // 电池槽限制单堆叠
    if (slot == SLOT_DISCHARGING && stack.count > 1) {
        stack.count = 1
    }
    inventory[slot] = stack
    if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
    markDirty()
}
override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
override fun clear() = inventory.clear()
override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
override fun markDirty() { super.markDirty() }
override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
```

### 3.3 ScreenHandler 工厂方法

```kotlin
override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
    buf.writeBlockPos(pos)
    buf.writeVarInt(syncedData.size())
}

override fun getDisplayName(): Text = Text.translatable("block.ic2_120.xxx")

override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
    XxxScreenHandler(syncId, playerInventory, this, ScreenHandlerContext.create(world!!, pos), syncedData)
```

### 3.4 NBT 持久化

```kotlin
override fun readNbt(nbt: NbtCompound) {
    super.readNbt(nbt)
    Inventories.readNbt(nbt, inventory)
    syncedData.readNbt(nbt)
    sync.amount = nbt.getLong(XxxSync.NBT_ENERGY_STORED)
    sync.syncCommittedAmount()
    sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
}

override fun writeNbt(nbt: NbtCompound) {
    super.writeNbt(nbt)
    Inventories.writeNbt(nbt, inventory)
    syncedData.writeNbt(nbt)
    nbt.putLong(XxxSync.NBT_ENERGY_STORED, sync.amount)
}
```

### 3.5 核心逻辑：tick()

```kotlin
fun tick(world: World, pos: BlockPos, state: BlockState) {
    if (world.isClient) return
    sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

    // 1. 应用升级效果
    OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

    // 2. 从相邻导线或电池槽提取能量
    pullEnergyFromNeighbors(world, pos, sync)
    extractFromDischargingSlot()

    // 3. 检查输入物品
    val input = getStack(SLOT_INPUT)
    if (input.isEmpty) {
        if (sync.progress != 0) sync.progress = 0
        setActiveState(world, pos, state, false)
        sync.syncCurrentTickFlow()
        return
    }

    // 4. 检查配方
    val result = XxxRecipes.getOutput(input) ?: run {
        if (sync.progress != 0) sync.progress = 0
        setActiveState(world, pos, state, false)
        sync.syncCurrentTickFlow()
        return
    }

    // 5. 检查输出槽是否有空间
    val outputSlot = getStack(SLOT_OUTPUT)
    val maxStack = result.maxCount
    val canAccept = outputSlot.isEmpty() ||
        (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

    if (!canAccept) {
        if (sync.progress != 0) sync.progress = 0
        setActiveState(world, pos, state, false)
        sync.syncCurrentTickFlow()
        return
    }

    // 6. 加工完成
    if (sync.progress >= XxxSync.PROGRESS_MAX) {
        input.decrement(1)
        if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result)
        else outputSlot.increment(result.count)
        sync.progress = 0
        markDirty()
        setActiveState(world, pos, state, false)
        sync.syncCurrentTickFlow()
        return
    }

    // 7. 消耗能量并增加进度
    val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
    val need = (XxxSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
    if (sync.consumeEnergy(need) > 0L) {
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.progress += progressIncrement
        markDirty()
        setActiveState(world, pos, state, true)
    } else {
        setActiveState(world, pos, state, false)
    }

    sync.syncCurrentTickFlow()
}

private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
    if (state.get(XxxBlock.ACTIVE) != active) {
        world.setBlockState(pos, state.with(XxxBlock.ACTIVE, active))
    }
}

private fun extractFromDischargingSlot() {
    val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
    if (space <= 0L) return

    val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
    val extracted = batteryDischarger.tick(request)
    if (extracted <= 0L) return

    sync.insertEnergy(extracted)
    sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    markDirty()
}
```

---

## 4. 创建 Sync（能量与进度同步）

**文件**：`src/main/kotlin/ic2_120/content/sync/XxxSync.kt`

```kotlin
package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

class XxxSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null },
    capacityBonusProvider: () -> Long = { 0L },
    maxInsertPerTickProvider: (() -> Long)? = null
) : UpgradeableTickLimitedSidedEnergyContainer(
    ENERGY_CAPACITY,
    capacityBonusProvider,
    MAX_INSERT,
    MAX_EXTRACT,
    currentTickProvider,
    maxInsertPerTickProvider
) {

    companion object {
        const val ENERGY_CAPACITY = 416L      // 基础容量
        const val MAX_INSERT = 32L             // 最大输入
        const val MAX_EXTRACT = 0L             // 最大输出（0=不输出）
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val PROGRESS_MAX = 130           // 加工所需 ticks
        const val ENERGY_PER_TICK = 2L         // 每 tick 耗能
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())

    private val flow = EnergyFlowSync(schema, this)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()
    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()
}
```

---

## 5. 创建 ScreenHandler

**文件**：`src/main/kotlin/ic2_120/content/screen/XxxScreenHandler.kt`

```kotlin
package ic2_120.content.screen

import ic2_120.content.sync.XxxSync
import ic2_120.content.block.XxxBlock
import ic2_120.content.block.machines.XxxBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = XxxBlock::class)
class XxxScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ModScreenHandlers.getType(XxxScreenHandler::class), syncId) {

    val sync = XxxSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, XxxBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 输入槽
        addSlot(PredicateSlot(blockInventory, XxxBlockEntity.SLOT_INPUT, INPUT_SLOT_X, INPUT_SLOT_Y, INPUT_SLOT_SPEC))

        // 放电槽（电池）
        addSlot(PredicateSlot(
            blockInventory,
            XxxBlockEntity.SLOT_DISCHARGING,
            DISCHARGING_SLOT_X,
            DISCHARGING_SLOT_Y,
            DISCHARGING_SLOT_SPEC
        ))

        // 输出槽
        addSlot(PredicateSlot(blockInventory, XxxBlockEntity.SLOT_OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y, OUTPUT_SLOT_SPEC))

        // 4 个升级槽
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    XxxBlockEntity.SLOT_UPGRADE_INDICES[i],
                    UpgradeSlotLayout.SLOT_X,
                    UpgradeSlotLayout.slotY(i),
                    upgradeSlotSpec
                )
            )
        }

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }

        // 快捷栏
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                else -> {
                    if (index in PLAYER_INV_START..HOTBAR_END) {
                        val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                            SlotTarget(slots[it], upgradeSlotSpec)
                        }
                        val moved = SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_INDEX], INPUT_SLOT_SPEC)
                            ) + upgradeTargets
                        )
                        if (!moved) return ItemStack.EMPTY
                    } else if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY
                    }
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is XxxBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        // 槽位位置
        const val INPUT_SLOT_X = 56
        const val INPUT_SLOT_Y = 35
        const val DISCHARGING_SLOT_X = 56
        const val DISCHARGING_SLOT_Y = 59
        const val OUTPUT_SLOT_X = 116
        const val OUTPUT_SLOT_Y = 47
        const val SLOT_SIZE = 18

        // 玩家物品栏
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166

        // 槽位规则
        private val INPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item !is IBatteryItem }  // 避免电池进入输入槽
        )
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }  // 仅电池可放入
        )
        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        // 槽位索引
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_DISCHARGING_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): XxxScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(XxxBlockEntity.INVENTORY_SIZE)
            return XxxScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
```

---

## 6. 创建 Screen（客户端 UI）

**文件**：`src/client/kotlin/ic2_120/client/XxxScreen.kt`

```kotlin
package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.XxxSync
import ic2_120.content.block.XxxBlock
import ic2_120.content.screen.XxxScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = XxxBlock::class)
class XxxScreen(
    handler: XxxScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<XxxScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            XxxScreenHandler.PLAYER_INV_Y,
            XxxScreenHandler.HOTBAR_Y,
            XxxScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = XxxScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 绘制机器槽位边框
        val inputSlot = handler.slots[XxxScreenHandler.SLOT_INPUT_INDEX]
        val dischargingSlot = handler.slots[XxxScreenHandler.SLOT_DISCHARGING_INDEX]
        val outputSlot = handler.slots[XxxScreenHandler.SLOT_OUTPUT_INDEX]

        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 绘制升级槽边框
        for (i in XxxScreenHandler.SLOT_UPGRADE_INDEX_START..XxxScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        // 绘制进度条
        val progress = handler.sync.progress.coerceIn(0, XxxSync.PROGRESS_MAX)
        val progressFrac = if (XxxSync.PROGRESS_MAX > 0) (progress.toFloat() / XxxSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
        val barX = x + inputSlot.x + slotSize + 2
        val barW = outputSlot.x - (inputSlot.x + slotSize) - 4
        val barH = 8
        val barY = y + inputSlot.y + (slotSize - barH) / 2
        ProgressBar.draw(context, barX, barY, barW, barH, progressFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)

        // 在UI左侧绘制速度文本
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val consumeTextWidth = consumeText.length * 6
        val textX = left - maxOf(inputTextWidth, consumeTextWidth) - 4
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, textX, top + 20, 0xAAAAAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(contentW)
                ) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(
                        energyFraction,
                        barWidth = 0,
                        barHeight = 9,
                        modifier = Modifier.EMPTY.width(barW)
                    )
                }
                Text("$energy / $cap EU", color = 0xCCCCCC, shadow = false)
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 184
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
```

---

## 7. 添加模型资源

说明：本项目里**实际机器模型与贴图在 `assets/ic2`**，`assets/ic2_120` 主要放索引（`blockstates`、`models/item`、`lang` 等）。

### 7.1 方块状态索引（ic2_120）

**文件**：`src/main/resources/assets/ic2_120/blockstates/xxx.json`

```json
{
  "variants": {
    "facing=north,active=false": { "model": "ic2:block/machine/processing/basic/xxx" },
    "facing=north,active=true":  { "model": "ic2:block/machine/processing/basic/xxx_active" },
    "facing=east,active=false":  { "model": "ic2:block/machine/processing/basic/xxx", "y": 90 },
    "facing=east,active=true":   { "model": "ic2:block/machine/processing/basic/xxx_active", "y": 90 },
    "facing=south,active=false": { "model": "ic2:block/machine/processing/basic/xxx", "y": 180 },
    "facing=south,active=true":  { "model": "ic2:block/machine/processing/basic/xxx_active", "y": 180 },
    "facing=west,active=false":  { "model": "ic2:block/machine/processing/basic/xxx", "y": 270 },
    "facing=west,active=true":   { "model": "ic2:block/machine/processing/basic/xxx_active", "y": 270 }
  }
}
```

### 7.2 物品模型索引（ic2_120）

**文件**：`src/main/resources/assets/ic2_120/models/item/xxx.json`

```json
{
  "parent": "ic2:block/machine/processing/basic/xxx"
}
```

### 7.3 实际模型与贴图（ic2）

- 方块模型：`src/main/resources/assets/ic2/models/block/machine/processing/basic/xxx.json`
- 激活模型：`src/main/resources/assets/ic2/models/block/machine/processing/basic/xxx_active.json`
- 贴图目录：`src/main/resources/assets/ic2/textures/block/machine/processing/basic/`

---

## 8. 添加翻译

### 8.1 中文翻译

**文件**：`src/main/resources/assets/ic2_120/lang/zh_cn.json`

```json
{
  "block.ic2_120.xxx": "粉碎机"
}
```

### 8.2 英文翻译

**文件**：`src/main/resources/assets/ic2_120/lang/en_us.json`

```json
{
  "block.ic2_120.xxx": "Macerator"
}
```

---

## 9. 注册检查

确保以下地方都正确注册了：

- ✅ `@ModBlockEntity(block = XxxBlock::class)` 注解在 BlockEntity 上
- ✅ `@ModScreenHandler(block = XxxBlock::class)` 注解在 ScreenHandler 上
- ✅ `@ModScreen(block = XxxBlock::class)` 注解在 Screen 上
- ✅ 方块模型 JSON 文件存在
- ✅ 物品模型 JSON 文件存在
- ✅ 翻译键存在

---

## 10. 配方系统

如果你的机器需要配方，创建配方类：

**文件**：`src/main/kotlin/ic2_120/content/recipes/XxxRecipes.kt`

```kotlin
package ic2_120.content.recipes

import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object XxxRecipes {
    private val recipes = mapOf<Identifier, ItemStack>(
        // 输入物品 ID -> 输出物品
        Identifier("minecraft",iron_ore") to ItemStack(Registries.ITEM.get(Identifier("ic2_120", "iron_dust"))),
        Identifier("minecraft",gold_ore) to ItemStack(Registries.ITEM.get(Identifier("ic2_120", "gold_dust"))),
    )

    fun getOutput(input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        val itemId = Registries.ITEM.getId(input.item)
        return recipes[itemId]?.copy()
    }
}
```

---

## 11. 编译验证

```bash
./gradlew clean compileKotlin compileClientKotlin
```

确保编译无错误。

---

## 附录：槽位位置参考

### 标准布局（带升级槽）

```
┌─────────────────────────────────────────────┐
│  标题  能量 ████████████████████ 1000/4000 EU  │
│                                              │
│  输入 [██] ━━━━━━━━━━━━ 输出 [██]  │ 升级槽1 [██] │
│  电池 [██]                      │  升级槽2 [██] │
│                                  │  升级槽3 [██] │
│  玩家物品栏 (27 格)              │  升级槽4 [██] │
│                                  │              │
└─────────────────────────────────────────────┘
```

**坐标参考**：
- 输入槽：(56, 35)
- 电池槽：(56, 59)
- 输出槽：(116, 47)
- 升级槽：x=176, y 从 17 开始，间隔 18
- 玩家物品栏：y=108
- 快捷栏：y=166

---

## 常见问题

### Q: 机器不工作？
- 检查能量是否充足（使用电池或导线供电）
- 检查配方是否正确注册
- 检查输入槽是否有物品
- 检查输出槽是否有空间

### Q: 电池不供电？
- 确保使用 `BatteryDischargerComponent` 而不是 `BatteryChargerComponent`
- 电池槽限制单堆叠（`count > 1` 时设为 `1`）
- 检查 `SLOT_DISCHARGING` 是否正确

### Q: 升级不生效？
- 确保机器实现了升级接口（`IOverclockerUpgradeSupport` 等）
- 确保在 `tick()` 开始时调用了升级组件的 `apply()`
- 检查升级槽索引是否正确

### Q: UI 显示错误？
- 检查 `PANEL_WIDTH` 是否包含升级槽宽度
- 确保槽位索引常量与实际添加顺序一致
- 确保客户端使用 `SyncedDataView` 而不是 `SyncedData`

---

## 相关文档

- [升级系统详细说明](upgrade-system.md)
- [同步系统详细说明](sync-system.md)
- [机器能力组合复用](machine-composition-reuse.md)
- [能量网络系统](energy-network.md)
