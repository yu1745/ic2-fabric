package ic2_120_advanced_solar_addon.content.block

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import net.minecraft.core.HolderLookup
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
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.MenuProvider
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import ic2_120.compat.*

// i18n: block.ic2_120_advanced_solar_addon.quantum_generator
// zh_cn: 量子发电机
// en_us: Quantum Generator
@ModBlock(name = "quantum_generator", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "machine")
class QuantumGeneratorBlock : MachineBlock() {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        QuantumGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, QuantumGeneratorBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    @Deprecated("Override without InteractionHand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { factory ->
                player.openMenu(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")
    }
}

@ModBlockEntity(block = QuantumGeneratorBlock::class)
class QuantumGeneratorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(QuantumGeneratorBlockEntity::class.type(), pos, state),
    IGenerator, ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    companion object {
        const val DEFAULT_ENERGY_MAC = 512
        const val DEFAULT_VARIABLE = 3
        const val VARIABLE_MIN = 1
        const val VARIABLE_MAX = 8
    }

    override val tier: Int get() = variable
    override val activeProperty = QuantumGeneratorBlock.ACTIVE

    @Suppress("unused")
    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = QuantumGeneratorSync(
        schema = syncedData,
        tier = DEFAULT_VARIABLE,
        getFacing = { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    var energyMac: Int = DEFAULT_ENERGY_MAC
    var variable: Int = DEFAULT_VARIABLE

    private var isActive: Boolean = true
        private set

    override fun getInventory(): Container? = null

    fun addEnergyMac(delta: Int) {
        // 允许 energyMac 调到 VARIABLE_MAX 对应上限，实际输出由 tick() 中按当前 tier 钳制
        energyMac = (energyMac + delta).coerceIn(1, EnergyTier.euPerTickFromTier(VARIABLE_MAX).toInt())
        setChanged()
    }

    fun applyVariable(value: Int) {
        variable = value.coerceIn(VARIABLE_MIN, VARIABLE_MAX)
        // 切换 tier 时立即重新钳制 energyMac
        energyMac = energyMac.coerceIn(1, EnergyTier.euPerTickFromTier(variable).toInt())
        setChanged()
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()

        var hasRedstoneSignal = false
        for (direction in Direction.entries) {
            if (world.getSignal(pos.relative(direction), direction) > 0) {
                hasRedstoneSignal = true
                break
            }
        }

        isActive = !hasRedstoneSignal

        // Clamp energyMac when tier changes externally
        energyMac = energyMac.coerceIn(1, EnergyTier.euPerTickFromTier(variable).toInt())

        if (isActive) {
            // 始终保持缓冲满，输出速度由 getSideMaxExtract(energyMac) 控制
            if (sync.amount < sync.capacity) {
                sync.amount = sync.capacity
            }
        }

        sync.production = energyMac
        sync.tierLevel = variable
        sync.energyMac = energyMac
        sync.variable = variable
        sync.isActive = if (isActive) 1 else 0
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        sync.syncCurrentTickFlow()
        setActiveState(world, pos, state, isActive)
        setChanged()
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120_advanced_solar_addon.quantum_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        QuantumGeneratorScreenHandler(syncId, playerInventory, ContainerLevelAccess.create(world!!, pos), syncedData)

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.count)
        return buf
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        energyMac = if (nbt.contains("energyMac"))
            nbt.getInt("energyMac").coerceIn(1, EnergyTier.euPerTickFromTier(VARIABLE_MAX).toInt()) else DEFAULT_ENERGY_MAC
        variable = if (nbt.contains("variable"))
            nbt.getInt("variable").coerceIn(VARIABLE_MIN, VARIABLE_MAX) else DEFAULT_VARIABLE
        isActive = if (nbt.contains("active")) nbt.getBoolean("active") else true
        sync.restoreEnergy(nbt.getLong(QuantumGeneratorSync.NBT_ENERGY).coerceIn(0L, sync.capacity))
        syncedData.readNbt(nbt)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        nbt.putInt("energyMac", energyMac)
        nbt.putInt("variable", variable)
        nbt.putBoolean("active", isActive)
        nbt.putLong(QuantumGeneratorSync.NBT_ENERGY, sync.amount)
        syncedData.writeNbt(nbt)
    }
}
