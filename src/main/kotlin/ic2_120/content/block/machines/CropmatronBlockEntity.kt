package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.CropmatronBlock
import ic2_120.content.crop.CropCareTarget
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.Fertilizer
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.CropmatronScreenHandler
import ic2_120.content.sync.CropmatronSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.World

@ModBlockEntity(block = CropmatronBlock::class)
class CropmatronBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Inventory,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CropmatronBlock.ACTIVE
    override val tier: Int = CROPMATRON_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    val syncedData = SyncedData(this)
    private val discharger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { CROPMATRON_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    @RegisterEnergy
    val sync = CropmatronSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(CROPMATRON_TIER + voltageTierBonus) }
    )

    private var waterAmountMb: Int = 0
    private var weedExAmountMb: Int = 0
    private var workOffset: Int = random.nextBetween(0, WORK_INTERVAL_TICKS - 1)

    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")) }
    private val fluidCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell")) }
    private val waterCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "water_cell")) }
    private val distilledWaterCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "distilled_water_cell")) }
    private val weedExCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "weed_ex_cell")) }

    constructor(pos: BlockPos, state: BlockState) : this(CropmatronBlockEntity::class.type(), pos, state)

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

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

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.cropmatron")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CropmatronScreenHandler(
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
        sync.amount = nbt.getLong(CropmatronSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        waterAmountMb = nbt.getInt(NBT_WATER_MB).coerceIn(0, CropmatronSync.WATER_TANK_CAPACITY_MB)
        weedExAmountMb = nbt.getInt(NBT_WEED_EX_MB).coerceIn(0, CropmatronSync.WEED_EX_TANK_CAPACITY_MB)
        workOffset = nbt.getInt(NBT_WORK_OFFSET).coerceIn(0, WORK_INTERVAL_TICKS - 1)
        sync.waterAmountMb = waterAmountMb
        sync.weedExAmountMb = weedExAmountMb
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CropmatronSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NBT_WATER_MB, waterAmountMb)
        nbt.putInt(NBT_WEED_EX_MB, weedExAmountMb)
        nbt.putInt(NBT_WORK_OFFSET, workOffset)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()
        processWaterInputContainer()
        processWeedExInputContainer()

        var active = false
        val interval = (WORK_INTERVAL_TICKS.toFloat() / speedMultiplier).toInt().coerceAtLeast(1)
        if ((world.time + workOffset) % interval.toLong() == 0L) {
            val report = runScan(world)
            sync.touchedThisRun = report.touched
            sync.fertilizedThisRun = report.fertilized
            sync.hydratedThisRun = report.hydrated
            sync.weedExAppliedThisRun = report.weedExApplied
            sync.farmlandHydratedThisRun = report.farmlandHydrated
            active = report.fertilized > 0 || report.hydrated > 0 || report.weedExApplied > 0 || report.farmlandHydrated > 0
        }

        sync.waterAmountMb = waterAmountMb
        sync.weedExAmountMb = weedExAmountMb
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun runScan(world: World): ScanReport {
        val report = ScanReport()
        val basePos = this.pos
        val scanEnergyCost = (ENERGY_PER_SCAN * energyMultiplier).toLong().coerceAtLeast(1L)
        val applyEnergyCost = (ENERGY_PER_APPLY * energyMultiplier).toLong().coerceAtLeast(1L)

        for (dx in -SCAN_RADIUS..SCAN_RADIUS) {
            for (dz in -SCAN_RADIUS..SCAN_RADIUS) {
                if (sync.amount < scanEnergyCost) return report
                sync.consumeEnergy(scanEnergyCost)

                // 监管机作用在“作物层”（与机器同 Y）；耕地在作物层下方一格。
                val cropPos = basePos.add(dx, 0, dz)
                val be = world.getBlockEntity(cropPos)
                if (be is CropCareTarget) {
                    report.touched++
                    tryApplyFertilizer(be, applyEnergyCost, report)
                    tryApplyHydration(be, applyEnergyCost, report)
                    tryApplyWeedEx(be, applyEnergyCost, report)
                } else if (waterAmountMb > 0 && sync.amount >= applyEnergyCost && tryHydrateFarmland(world, cropPos.down())) {
                    if (sync.consumeEnergy(applyEnergyCost) > 0L) {
                        report.farmlandHydrated++
                    }
                }
            }
        }

        return report
    }

    private fun tryApplyFertilizer(target: CropCareTarget, applyEnergyCost: Long, report: ScanReport) {
        if (sync.amount < applyEnergyCost) return
        if (!hasFertilizer()) return
        if (target.applyFertilizer(1, simulate = true) <= 0) return

        if (!consumeOneFertilizer()) return
        if (sync.consumeEnergy(applyEnergyCost) <= 0L) {
            refundOneFertilizer()
            return
        }

        val used = target.applyFertilizer(1, simulate = false)
        if (used > 0) {
            report.fertilized += used
        } else {
            refundOneFertilizer()
        }
    }

    private fun tryApplyHydration(target: CropCareTarget, applyEnergyCost: Long, report: ScanReport) {
        if (sync.amount < applyEnergyCost) return
        if (waterAmountMb <= 0) return

        val request = waterAmountMb.coerceAtMost(200)
        val simulated = target.applyHydration(request, simulate = true)
        if (simulated <= 0) return

        if (sync.consumeEnergy(applyEnergyCost) <= 0L) return
        val used = target.applyHydration(request, simulate = false).coerceAtMost(waterAmountMb)
        if (used <= 0) return
        waterAmountMb -= used
        report.hydrated += used
        markDirty()
    }

    private fun tryApplyWeedEx(target: CropCareTarget, applyEnergyCost: Long, report: ScanReport) {
        if (sync.amount < applyEnergyCost) return
        if (weedExAmountMb <= 0) return

        val request = weedExAmountMb.coerceAtMost(200)
        val simulated = target.applyWeedEx(request, simulate = true)
        if (simulated <= 0) return

        if (sync.consumeEnergy(applyEnergyCost) <= 0L) return
        val used = target.applyWeedEx(request, simulate = false).coerceAtMost(weedExAmountMb)
        if (used <= 0) return
        weedExAmountMb -= used
        report.weedExApplied += used
        markDirty()
    }

    private fun tryHydrateFarmland(world: World, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        if (state.block !is FarmlandBlock) return false
        val moisture = state.get(FarmlandBlock.MOISTURE)
        if (moisture >= 7) return false

        val add = minOf(7 - moisture, waterAmountMb)
        if (add <= 0) return false

        waterAmountMb -= add
        world.setBlockState(pos, state.with(FarmlandBlock.MOISTURE, moisture + add), 2)
        markDirty()
        return true
    }

    private fun processWaterInputContainer() {
        if (waterAmountMb > CropmatronSync.WATER_TANK_CAPACITY_MB - 1000) return

        val input = getStack(SLOT_WATER_INPUT)
        if (input.isEmpty) return

        val emptyRemainder = when {
            input.item == Items.WATER_BUCKET || input.item == ModFluids.DISTILLED_WATER_BUCKET -> ItemStack(Items.BUCKET)
            input.item == waterCellItem || input.item == distilledWaterCellItem -> ItemStack(emptyCellItem)
            input.item == fluidCellItem && input.item is FluidCellItem -> {
                val fluid = input.getFluidCellVariant()?.fluid
                if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
                    fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING
                ) ItemStack(emptyCellItem) else ItemStack.EMPTY
            }
            else -> ItemStack.EMPTY
        }
        if (emptyRemainder.isEmpty) return

        val out = getStack(SLOT_WATER_OUTPUT)
        if (!canMergeIntoSlot(out, emptyRemainder)) return

        waterAmountMb = (waterAmountMb + 1000).coerceAtMost(CropmatronSync.WATER_TANK_CAPACITY_MB)
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_WATER_INPUT, ItemStack.EMPTY)
        if (out.isEmpty) setStack(SLOT_WATER_OUTPUT, emptyRemainder.copy()) else out.increment(1)
        markDirty()
    }

    private fun processWeedExInputContainer() {
        if (weedExAmountMb > CropmatronSync.WEED_EX_TANK_CAPACITY_MB - 1000) return

        val input = getStack(SLOT_WEED_EX_INPUT)
        if (input.isEmpty) return

        val emptyRemainder = when {
            input.item == ModFluids.WEED_EX_BUCKET -> ItemStack(Items.BUCKET)
            input.item == weedExCellItem -> ItemStack(emptyCellItem)
            input.item == fluidCellItem && input.item is FluidCellItem -> {
                val fluid = input.getFluidCellVariant()?.fluid
                if (fluid == ModFluids.WEED_EX_STILL || fluid == ModFluids.WEED_EX_FLOWING) ItemStack(emptyCellItem)
                else ItemStack.EMPTY
            }
            else -> ItemStack.EMPTY
        }
        if (emptyRemainder.isEmpty) return

        val out = getStack(SLOT_WEED_EX_OUTPUT)
        if (!canMergeIntoSlot(out, emptyRemainder)) return

        weedExAmountMb = (weedExAmountMb + 1000).coerceAtMost(CropmatronSync.WEED_EX_TANK_CAPACITY_MB)
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_WEED_EX_INPUT, ItemStack.EMPTY)
        if (out.isEmpty) setStack(SLOT_WEED_EX_OUTPUT, emptyRemainder.copy()) else out.increment(1)
        markDirty()
    }

    private fun hasFertilizer(): Boolean {
        for (slot in SLOT_FERTILIZER_INDICES) {
            val stack = getStack(slot)
            if (!stack.isEmpty && stack.item == Fertilizer::class.instance()) return true
        }
        return false
    }

    private fun consumeOneFertilizer(): Boolean {
        for (slot in SLOT_FERTILIZER_INDICES) {
            val stack = getStack(slot)
            if (!stack.isEmpty && stack.item == Fertilizer::class.instance()) {
                stack.decrement(1)
                if (stack.isEmpty) setStack(slot, ItemStack.EMPTY)
                markDirty()
                return true
            }
        }
        return false
    }

    private fun refundOneFertilizer() {
        val fertilizer = ItemStack(Fertilizer::class.instance())
        for (slot in SLOT_FERTILIZER_INDICES) {
            val stack = getStack(slot)
            if (stack.isEmpty) {
                setStack(slot, fertilizer)
                return
            }
            if (ItemStack.canCombine(stack, fertilizer) && stack.count < stack.maxCount) {
                stack.increment(1)
                markDirty()
                return
            }
        }
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)
    }

    private fun extractFromDischargingSlot() {
        val capacity = sync.getEffectiveCapacity().coerceAtLeast(0L)
        val maxDemand = (capacity - sync.amount).coerceAtLeast(0L)
        if (maxDemand <= 0L) return
        val extracted = discharger.tick(maxDemand)
        if (extracted <= 0L) return
        sync.amount = (sync.amount + extracted).coerceAtMost(capacity)
        sync.energy = sync.amount.toInt().coerceAtMost(Int.MAX_VALUE)
        markDirty()
    }

    data class ScanReport(
        var touched: Int = 0,
        var fertilized: Int = 0,
        var hydrated: Int = 0,
        var weedExApplied: Int = 0,
        var farmlandHydrated: Int = 0
    )

    companion object {
        const val CROPMATRON_TIER = 1

        const val SLOT_WATER_INPUT = 0
        const val SLOT_WATER_OUTPUT = 1
        const val SLOT_WEED_EX_INPUT = 2
        const val SLOT_WEED_EX_OUTPUT = 3

        const val SLOT_FERTILIZER_0 = 4
        const val SLOT_FERTILIZER_1 = 5
        const val SLOT_FERTILIZER_2 = 6
        const val SLOT_FERTILIZER_3 = 7
        const val SLOT_FERTILIZER_4 = 8
        const val SLOT_FERTILIZER_5 = 9
        const val SLOT_FERTILIZER_6 = 10

        const val SLOT_UPGRADE_0 = 11
        const val SLOT_UPGRADE_1 = 12
        const val SLOT_UPGRADE_2 = 13
        const val SLOT_UPGRADE_3 = 14
        const val SLOT_DISCHARGING = 15

        val SLOT_FERTILIZER_INDICES = intArrayOf(
            SLOT_FERTILIZER_0,
            SLOT_FERTILIZER_1,
            SLOT_FERTILIZER_2,
            SLOT_FERTILIZER_3,
            SLOT_FERTILIZER_4,
            SLOT_FERTILIZER_5,
            SLOT_FERTILIZER_6
        )
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)

        const val INVENTORY_SIZE = 16

        private const val WORK_INTERVAL_TICKS = 10
        private const val SCAN_RADIUS = 4
        private const val ENERGY_PER_SCAN = 1
        private const val ENERGY_PER_APPLY = 10

        private const val NBT_WATER_MB = "WaterMb"
        private const val NBT_WEED_EX_MB = "WeedExMb"
        private const val NBT_WORK_OFFSET = "WorkOffset"

        private val random: Random = Random.create()
    }
}
