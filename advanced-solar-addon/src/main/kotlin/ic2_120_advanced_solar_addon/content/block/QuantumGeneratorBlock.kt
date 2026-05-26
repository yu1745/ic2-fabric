package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.sync.QuantumGeneratorSync
import ic2_120_advanced_solar_addon.content.screen.QuantumGeneratorScreenHandler
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.machines.MachineBlockEntity
import ic2_120.content.energy.EnergyTier
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

// i18n: block.ic2_120_advanced_solar_addon.quantum_generator
// zh_cn: 量子发电机
// en_us: Quantum Generator
@ModBlock(name = "quantum_generator", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "machine")
class QuantumGeneratorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        QuantumGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, QuantumGeneratorBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? NamedScreenHandlerFactory
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}

@ModBlockEntity(block = QuantumGeneratorBlock::class)
class QuantumGeneratorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(QuantumGeneratorBlockEntity::class.type(), pos, state),
    IGenerator, ExtendedScreenHandlerFactory {

    companion object {
        const val DEFAULT_ENERGY_MAC = 512
        const val DEFAULT_VARIABLE = 3
        const val VARIABLE_MIN = 1
        const val VARIABLE_MAX = 8
    }

    override val tier: Int get() = variable
    override val activeProperty = QuantumGeneratorBlock.ACTIVE

    @Suppress("unused")
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = QuantumGeneratorSync(
        schema = syncedData,
        tier = DEFAULT_VARIABLE,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    var energyMac: Int = DEFAULT_ENERGY_MAC
    var variable: Int = DEFAULT_VARIABLE

    private var isActive: Boolean = true
        private set

    override fun getInventory(): Inventory? = null

    fun addEnergyMac(delta: Int) {
        // 允许 energyMac 调到 VARIABLE_MAX 对应上限，实际输出由 tick() 中按当前 tier 钳制
        energyMac = (energyMac + delta).coerceIn(1, EnergyTier.euPerTickFromTier(VARIABLE_MAX).toInt())
        markDirty()
    }

    fun applyVariable(value: Int) {
        variable = value.coerceIn(VARIABLE_MIN, VARIABLE_MAX)
        // 切换 tier 时立即重新钳制 energyMac
        energyMac = energyMac.coerceIn(1, EnergyTier.euPerTickFromTier(variable).toInt())
        markDirty()
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()

        var hasRedstoneSignal = false
        for (direction in Direction.entries) {
            if (world.getEmittedRedstonePower(pos.offset(direction), direction) > 0) {
                hasRedstoneSignal = true
                break
            }
        }

        isActive = !hasRedstoneSignal

        // Clamp energyMac when tier changes externally
        energyMac = energyMac.coerceIn(1, EnergyTier.euPerTickFromTier(variable).toInt())

        if (isActive) {
            sync.generateEnergy(energyMac.toLong())
        }

        sync.production = energyMac
        sync.tierLevel = variable
        sync.energyMac = energyMac
        sync.variable = variable
        sync.isActive = if (isActive) 1 else 0
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        sync.syncCurrentTickFlow()
        setActiveState(world, pos, state, isActive)
        markDirty()
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120_advanced_solar_addon.quantum_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        QuantumGeneratorScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        energyMac = if (nbt.contains("energyMac"))
            nbt.getInt("energyMac").coerceIn(1, EnergyTier.euPerTickFromTier(VARIABLE_MAX).toInt()) else DEFAULT_ENERGY_MAC
        variable = if (nbt.contains("variable"))
            nbt.getInt("variable").coerceIn(VARIABLE_MIN, VARIABLE_MAX) else DEFAULT_VARIABLE
        isActive = if (nbt.contains("active")) nbt.getBoolean("active") else true
        sync.restoreEnergy(nbt.getLong(QuantumGeneratorSync.NBT_ENERGY).coerceIn(0L, sync.capacity))
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putInt("energyMac", energyMac)
        nbt.putInt("variable", variable)
        nbt.putBoolean("active", isActive)
        nbt.putLong(QuantumGeneratorSync.NBT_ENERGY, sync.amount)
        syncedData.writeNbt(nbt)
    }
}
