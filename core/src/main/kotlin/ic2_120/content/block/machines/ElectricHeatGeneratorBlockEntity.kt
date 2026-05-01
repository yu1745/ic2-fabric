package ic2_120.content.block.machines

import ic2_120.content.block.ElectricHeatGeneratorBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.sync.ElectricHeatGeneratorSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.IRedstoneControlSupport
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.screen.ElectricHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

/**
 * 电力加热机：
 * - 10 个线圈槽，每槽 1 个线圈
 * - 1 EU -> 1 HU
 * - 单槽 10HU/t，满槽 100HU/t
 * - Tier 4，缓存 10k EU
 * - 支持红石控制
 */
@ModBlockEntity(block = ElectricHeatGeneratorBlock::class)
class ElectricHeatGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatGeneratorBlockEntityBase(type, pos, state), Inventory, IRedstoneControlSupport, ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = ElectricHeatGeneratorBlock.ACTIVE

    companion object {
        const val SLOT_COUNT = 11
        const val SLOT_DISCHARGING = 10
        private const val HU_PER_COIL_PER_TICK = 10L
    }

    override val tier: Int = 4
    override var redstoneInverted: Boolean = false

    private val inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY)
    private val coilItem by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "coil")) }

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { tier },
        canDischargeNow = { sync.amount < ElectricHeatGeneratorSync.ENERGY_CAPACITY }
    )

    val syncedData = SyncedData(this)
    override val heatFlow = HeatFlowSync(syncedData, this)
    @RegisterEnergy
    val sync = ElectricHeatGeneratorSync(
        schema = syncedData,
        currentTickProvider = { world?.time },
        heatFlow = heatFlow
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ElectricHeatGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.electric_heat_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ElectricHeatGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun getInventory(): Inventory = this
    override fun size(): Int = SLOT_COUNT
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: net.minecraft.entity.player.PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        in 0 until SLOT_DISCHARGING -> !stack.isEmpty && stack.item == coilItem
        SLOT_DISCHARGING -> !stack.isEmpty && stack.item is IBatteryItem
        else -> false
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING) {
            if (stack.isEmpty || stack.item !is IBatteryItem) return
            val single = stack.copy()
            single.count = 1
            inventory[slot] = single
            markDirty()
            return
        }
        if (!stack.isEmpty && stack.item != coilItem) return
        val stackToSet = if (!stack.isEmpty && stack.count > 1) {
            val single = stack.copy()
            single.count = 1
            single
        } else {
            stack
        }
        inventory[slot] = stackToSet
        markDirty()
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ElectricHeatGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, ElectricHeatGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(ElectricHeatGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
    }

    override fun preGenerate(world: World, pos: BlockPos, state: BlockState) {
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    private fun extractFromDischargingSlot() {
        val space = (ElectricHeatGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return
        val request = minOf(space, ElectricHeatGeneratorSync.MAX_INSERT)
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    override fun generateHeat(world: World, pos: BlockPos, state: BlockState): Long {
        val redstoneAllowsRun = RedstoneControlComponent.canRun(world, pos, this)
        val coilCount = inventory.count { !it.isEmpty && it.item == coilItem }
        val maxHuPerTick = coilCount * HU_PER_COIL_PER_TICK

        var generatedThisTick = 0L
        if (redstoneAllowsRun && maxHuPerTick > 0L) {
            val euNeed = maxHuPerTick
            val consumed = sync.consumeEnergy(minOf(euNeed, sync.amount))
            if (consumed > 0L) {
                generatedThisTick = consumed
            }
        }
        return generatedThisTick
    }

    override fun syncAdditionalData() {
        sync.syncCurrentTickFlow()
    }

    override fun shouldActivate(generatedHeat: Long, hasValidConsumer: Boolean): Boolean =
        generatedHeat > 0L && hasValidConsumer

    override fun getActiveState(state: BlockState): Boolean =
        state.get(ElectricHeatGeneratorBlock.ACTIVE)

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        tickHeatMachine(world, pos, state)
    }
}
