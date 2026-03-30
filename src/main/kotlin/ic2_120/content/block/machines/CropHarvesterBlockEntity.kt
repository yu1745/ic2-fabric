package ic2_120.content.block.machines

import ic2_120.content.block.CropBlock
import ic2_120.content.block.CropBlockEntity
import ic2_120.content.block.CropHarvesterBlock
import ic2_120.content.block.CropStickBlock
import ic2_120.content.crop.CropSystem
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.CropHarvesterScreenHandler
import ic2_120.content.sync.CropHarvesterSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

@ModBlockEntity(block = CropHarvesterBlock::class)
class CropHarvesterBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Inventory,
    IEnergyStorageUpgradeSupport,
    IEjectorUpgradeSupport,
    ITransformerUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CropHarvesterBlock.ACTIVE
    override val tier: Int = CROP_HARVESTER_TIER

    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0
    override var itemEjectorEnabled: Boolean = false
    override var itemEjectorFilter: Item? = null
    override var itemEjectorSide: Direction? = null

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = CropHarvesterSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(CROP_HARVESTER_TIER + voltageTierBonus) }
    )
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

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun markDirty() = super.markDirty()

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.crop_harvester")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CropHarvesterScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
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
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CropHarvesterSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NBT_SCAN_X, scanX)
        nbt.putInt(NBT_SCAN_Y, scanY)
        nbt.putInt(NBT_SCAN_Z, scanZ)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, SLOT_CONTENT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        var active = false
        if (world.time % WORK_INTERVAL_TICKS.toLong() == 0L) {
            val scanCost = ENERGY_PER_SCAN.toLong()
            val harvestCost = ENERGY_PER_HARVEST_STACK.toLong()
            if (sync.amount >= scanCost + harvestCost) {
                val report = runScan(world, scanCost, harvestCost)
                sync.checkedThisRun = report.checked
                sync.harvestedThisRun = report.harvested
                active = report.harvested > 0
            }
        }

        if (itemEjectorEnabled) {
            EjectorUpgradeComponent.ejectFromOutputSlots(
                world,
                pos,
                this,
                SLOT_CONTENT_INDICES,
                itemEjectorSide,
                itemEjectorFilter
            )
        }

        sync.scanX = scanX
        sync.scanY = scanY
        sync.scanZ = scanZ
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun runScan(world: World, scanCost: Long, harvestCost: Long): ScanReport {
        val report = ScanReport()
        advanceScanCursor()
        if (sync.consumeEnergy(scanCost) <= 0L) return report

        val targetPos = pos.add(scanX, scanY, scanZ)
        val be = world.getBlockEntity(targetPos) as? CropBlockEntity ?: return report
        report.checked++

        if (isOutputFull()) return report

        val cropState = world.getBlockState(targetPos)
        if (cropState.block !is CropBlock) return report
        if (!shouldHarvestNow(cropState)) return report

        val result = be.performHarvest(cropState) ?: return report
        if (result.ageAfterHarvest != null) {
            world.setBlockState(
                targetPos,
                cropState.with(CropBlock.AGE, result.ageAfterHarvest.coerceIn(0, 7)),
                net.minecraft.block.Block.NOTIFY_ALL
            )
        } else {
            world.setBlockState(targetPos, CropStickBlock.defaultStickState(), net.minecraft.block.Block.NOTIFY_ALL)
        }

        for (drop in result.drops) {
            if (drop.isEmpty) continue
            val remaining = insertIntoContentSlots(drop.copy())
            if (!remaining.isEmpty) {
                ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), remaining)
            }
            sync.consumeEnergy(harvestCost)
            report.harvested++
        }

        markDirty()
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
            val stack = getStack(slot)
            if (stack.isEmpty) return false
            val limit = minOf(stack.maxCount, maxCountPerStack)
            if (stack.count < limit) return false
        }
        return true
    }

    private fun insertIntoContentSlots(stack: ItemStack): ItemStack {
        var remaining = stack.copy()

        for (slot in SLOT_CONTENT_INDICES) {
            if (remaining.isEmpty) break
            val existing = getStack(slot)
            if (existing.isEmpty) continue
            if (!ItemStack.canCombine(existing, remaining)) continue
            val limit = minOf(existing.maxCount, maxCountPerStack)
            val room = (limit - existing.count).coerceAtLeast(0)
            if (room <= 0) continue
            val move = minOf(room, remaining.count)
            existing.increment(move)
            remaining.decrement(move)
        }

        for (slot in SLOT_CONTENT_INDICES) {
            if (remaining.isEmpty) break
            val existing = getStack(slot)
            if (!existing.isEmpty) continue
            val toInsert = minOf(remaining.count, maxCountPerStack)
            if (toInsert <= 0) break
            val inserted = remaining.copy()
            inserted.count = toInsert
            setStack(slot, inserted)
            remaining.decrement(toInsert)
        }

        return remaining
    }

    private fun shouldHarvestNow(cropState: BlockState): Boolean {
        val cropType = cropState.get(CropBlock.CROP_TYPE)
        val age = cropState.get(CropBlock.AGE)
        val maxAge = CropSystem.maxAge(cropType)
        val optimalAge = CropSystem.optimalHarvestAge(cropType)
        return age == optimalAge || age == maxAge
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
        private const val ENERGY_PER_SCAN = 1
        private const val ENERGY_PER_HARVEST_STACK = 20

        private const val SCAN_X_MIN = -4
        private const val SCAN_X_MAX = 4
        private const val SCAN_Y_MIN = -1
        private const val SCAN_Y_MAX = 1
        private const val SCAN_Z_MIN = -4
        private const val SCAN_Z_MAX = 4

        private const val NBT_SCAN_X = "ScanX"
        private const val NBT_SCAN_Y = "ScanY"
        private const val NBT_SCAN_Z = "ScanZ"
    }
}
