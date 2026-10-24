package ic2_120.content.block.machines

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.block.CropBlock
import ic2_120.content.block.CropBlockEntity
import ic2_120.content.block.CropHarvesterBlock
import ic2_120.content.block.CropStickBlock
import ic2_120.content.crop.CropSystem
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.screen.CropHarvesterScreenHandler
import ic2_120.content.sync.CropHarvesterSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level

@ModBlockEntity(block = CropHarvesterBlock::class)
class CropHarvesterBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Container,
    IRoutedSidedInventory,
    IEnergyStorageUpgradeSupport,
    IEjectorUpgradeSupport,
    ITransformerUpgradeSupport,
    IOverclockerUpgradeSupport,
    ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = CropHarvesterBlock.ACTIVE
    override val tier: Int = CROP_HARVESTER_TIER

    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0
    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f

    private var workOffset: Int = random.nextInt(0, (WORK_INTERVAL_TICKS - 1) + 1)

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        // 升级格仅在 insertRoutes（玩家 GUI 可放可取），不在 extractSlots：物流/自动化不可抽走升级卡（同 Macerator 等标准机器模式）。
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && (it.item is IBatteryItem || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust) }, maxPerSlot = 1)
        ),
        extractSlots = SLOT_CONTENT_INDICES + intArrayOf(SLOT_DISCHARGING),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage
    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = CropHarvesterSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(CROP_HARVESTER_TIER + voltageTierBonus) }
    )
    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { CROP_HARVESTER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    private var scanX: Int = SCAN_X_MIN
    private var scanY: Int = SCAN_Y_MIN
    private var scanZ: Int = SCAN_Z_MIN

    constructor(pos: BlockPos, state: BlockState) : this(CropHarvesterBlockEntity::class.type(), pos, state)

    override fun getInventory(): Container = this
    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)
    override fun setChanged() = super.setChanged()

    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        setChanged()
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        SLOT_CONTENT_INDICES.contains(slot) -> false
        slot == SLOT_DISCHARGING -> (stack.item is IBatteryItem || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust)
        SLOT_UPGRADE_INDICES.contains(slot) -> stack.item is IUpgradeItem
        else -> false
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.getCount())
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.crop_harvester")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        CropHarvesterScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CropHarvesterSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        scanX = if (nbt.contains(NBT_SCAN_X)) nbt.getInt(NBT_SCAN_X).coerceIn(SCAN_X_MIN, SCAN_X_MAX) else SCAN_X_MIN
        scanY = if (nbt.contains(NBT_SCAN_Y)) nbt.getInt(NBT_SCAN_Y).coerceIn(SCAN_Y_MIN, SCAN_Y_MAX) else SCAN_Y_MIN
        scanZ = if (nbt.contains(NBT_SCAN_Z)) nbt.getInt(NBT_SCAN_Z).coerceIn(SCAN_Z_MIN, SCAN_Z_MAX) else SCAN_Z_MIN
        sync.scanX = scanX
        sync.scanY = scanY
        sync.scanZ = scanZ
        workOffset = nbt.getInt(NBT_WORK_OFFSET).coerceIn(0, WORK_INTERVAL_TICKS - 1)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(CropHarvesterSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NBT_SCAN_X, scanX)
        nbt.putInt(NBT_SCAN_Y, scanY)
        nbt.putInt(NBT_SCAN_Z, scanZ)
        nbt.putInt(NBT_WORK_OFFSET, workOffset)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_CONTENT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        var active = false
        val interval = (WORK_INTERVAL_TICKS.toFloat() / speedMultiplier).toInt().coerceAtLeast(1)
        if ((world.time + workOffset) % interval.toLong() == 0L) {
            val scanCost = (ENERGY_PER_SCAN * energyMultiplier).toLong()
            val harvestCost = (ENERGY_PER_HARVEST_STACK * energyMultiplier).toLong()
            if (sync.amount >= scanCost + harvestCost) {
                val report = runScan(world, scanCost, harvestCost)
                sync.checkedThisRun = report.checked
                sync.harvestedThisRun = report.harvested
                active = report.harvested > 0
            }
        }

        sync.scanX = scanX
        sync.scanY = scanY
        sync.scanZ = scanZ
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun runScan(world: Level, scanCost: Long, harvestCost: Long): ScanReport {
        val report = ScanReport()
        var steps = 0
        var targetPos: BlockPos? = null
        var be: CropBlockEntity? = null
        // 跳跃式推进：每次工作最多走 SCAN_STEPS_PER_TICK 格，命中作物架即停。
        // 空气/非作物格不消耗扫描能量，扫描费只在真实检查作物架时收取。
        while (steps < SCAN_STEPS_PER_TICK) {
            advanceScanCursor()
            steps++
            val p = pos.offset(scanX, scanY, scanZ)
            val candidate = world.getBlockEntity(p) as? CropBlockEntity ?: continue
            targetPos = p
            be = candidate
            break
        }
        if (be == null || targetPos == null) return report
        if (sync.consumeEnergy(scanCost) <= 0L) return report
        report.checked++

        if (isOutputFull()) return report

        val cropState = world.getBlockState(targetPos)
        if (cropState.block !is CropBlock) return report
        if (!shouldHarvestNow(cropState)) return report

        val result = be.performHarvest(cropState) ?: return report
        if (result.ageAfterHarvest != null) {
            world.setBlock(
                targetPos,
                cropState.setValue(CropBlock.AGE, result.ageAfterHarvest.coerceIn(0, 7)),
                net.minecraft.world.level.block.Block.UPDATE_ALL
            )
        } else {
            world.setBlock(targetPos, CropStickBlock.defaultStickState(), net.minecraft.world.level.block.Block.UPDATE_ALL)
        }

        for (drop in result.drops) {
            if (drop.isEmpty) continue
            val remaining = insertIntoContentSlots(drop.copy())
            if (!remaining.isEmpty) {
                Containers.dropItemStack(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), remaining)
            }
            sync.consumeEnergy(harvestCost)
            report.harvested++
        }

        setChanged()
        return report
    }

    private fun advanceScanCursor() {
        scanX++
        if (scanX > SCAN_X_MAX) {
            scanX = SCAN_X_MIN
            scanZ++
            if (scanZ > SCAN_Z_MAX) {
                scanZ = SCAN_Z_MIN
                scanY++
                if (scanY > SCAN_Y_MAX) {
                    scanY = SCAN_Y_MIN
                }
            }
        }
    }

    private fun isOutputFull(): Boolean {
        for (slot in SLOT_CONTENT_INDICES) {
            val stack = getItem(slot)
            if (stack.isEmpty) return false
            val limit = minOf(stack.maxStackSize, net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE)
            if (stack.count < limit) return false
        }
        return true
    }

    private fun insertIntoContentSlots(stack: ItemStack): ItemStack {
        var remaining = stack.copy()

        for (slot in SLOT_CONTENT_INDICES) {
            if (remaining.isEmpty) break
            val existing = getItem(slot)
            if (existing.isEmpty) continue
            if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue
            val limit = minOf(existing.maxStackSize, net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE)
            val room = (limit - existing.count).coerceAtLeast(0)
            if (room <= 0) continue
            val move = minOf(room, remaining.count)
            existing.grow(move)
            remaining.shrink(move)
        }

        for (slot in SLOT_CONTENT_INDICES) {
            if (remaining.isEmpty) break
            val existing = getItem(slot)
            if (!existing.isEmpty) continue
            val toInsert = minOf(remaining.count, net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE)
            if (toInsert <= 0) break
            val inserted = remaining.copy()
            inserted.count = toInsert
            setItem(slot, inserted)
            remaining.shrink(toInsert)
        }

        return remaining
    }

    private fun shouldHarvestNow(cropState: BlockState): Boolean {
        val cropType = cropState.getValue(CropBlock.CROP_TYPE)
        val age = cropState.getValue(CropBlock.AGE)
        val maxAge = CropSystem.maxAge(cropType)
        val optimalAge = CropSystem.optimalHarvestAge(cropType)
        return age == optimalAge || age == maxAge
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        setChanged()
    }

    data class ScanReport(
        var checked: Int = 0,
        var harvested: Int = 0
    )

    companion object {
        const val CROP_HARVESTER_TIER = 1

        const val SLOT_CONTENT_0 = 0
        const val SLOT_CONTENT_1 = 1
        const val SLOT_CONTENT_2 = 2
        const val SLOT_CONTENT_3 = 3
        const val SLOT_CONTENT_4 = 4
        const val SLOT_CONTENT_5 = 5
        const val SLOT_CONTENT_6 = 6
        const val SLOT_CONTENT_7 = 7
        const val SLOT_CONTENT_8 = 8
        const val SLOT_CONTENT_9 = 9
        const val SLOT_CONTENT_10 = 10
        const val SLOT_CONTENT_11 = 11
        const val SLOT_CONTENT_12 = 12
        const val SLOT_CONTENT_13 = 13
        const val SLOT_CONTENT_14 = 14

        const val SLOT_UPGRADE_0 = 15
        const val SLOT_UPGRADE_1 = 16
        const val SLOT_UPGRADE_2 = 17
        const val SLOT_UPGRADE_3 = 18
        const val SLOT_DISCHARGING = 19

        val SLOT_CONTENT_INDICES = intArrayOf(
            SLOT_CONTENT_0, SLOT_CONTENT_1, SLOT_CONTENT_2, SLOT_CONTENT_3, SLOT_CONTENT_4,
            SLOT_CONTENT_5, SLOT_CONTENT_6, SLOT_CONTENT_7, SLOT_CONTENT_8, SLOT_CONTENT_9,
            SLOT_CONTENT_10, SLOT_CONTENT_11, SLOT_CONTENT_12, SLOT_CONTENT_13, SLOT_CONTENT_14
        )
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)

        const val INVENTORY_SIZE = 20

        private const val WORK_INTERVAL_TICKS = 10
        private const val ENERGY_PER_SCAN = 1f
        private const val ENERGY_PER_HARVEST_STACK = 20f

        private const val SCAN_X_MIN = -4
        private const val SCAN_X_MAX = 4
        private const val SCAN_Y_MIN = -1
        private const val SCAN_Y_MAX = 1
        private const val SCAN_Z_MIN = -4
        private const val SCAN_Z_MAX = 4

        /** 每次工作最多推进的游标格数（防全空区域时每 tick 走满整个扫描体） */
        private const val SCAN_STEPS_PER_TICK = 8

        private val random: RandomSource = RandomSource.create()

        private const val NBT_SCAN_X = "ScanX"
        private const val NBT_SCAN_Y = "ScanY"
        private const val NBT_SCAN_Z = "ScanZ"
        private const val NBT_WORK_OFFSET = "WorkOffset"
    }
}
