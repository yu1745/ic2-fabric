package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.sync.QuantumGeneratorSync
import ic2_120_advanced_solar_addon.content.screen.QuantumGeneratorScreenHandler
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.machines.MachineBlockEntity
import ic2_120.content.energy.EnergyTier
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import io.netty.buffer.Unpooled
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

@ModBlockEntity(block = QuantumGeneratorBlock::class)
class QuantumGeneratorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(QuantumGeneratorBlockEntity::class.type(), pos, state),
    IGenerator, ExtendedScreenHandlerFactory<PacketByteBuf> {

    companion object {
        const val GENERATOR_TIER = 3
        const val PRODUCTION = 512
    }

    override val tier: Int = GENERATOR_TIER
    override val activeProperty = QuantumGeneratorBlock.ACTIVE

    @Suppress("unused")
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = QuantumGeneratorSync(
        schema = syncedData,
        tier = tier,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    var production: Int = PRODUCTION
        private set

    private var isActive: Boolean = true
        private set

    override fun getInventory(): Inventory? = null

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        var hasRedstoneSignal = false
        for (direction in Direction.entries) {
            if (world.getEmittedRedstonePower(pos.offset(direction), direction) > 0) {
                hasRedstoneSignal = true
                break
            }
        }

        isActive = !hasRedstoneSignal

        if (isActive) {
            sync.generateEnergy(production.toLong())
        }

        sync.production = production
        sync.tierLevel = tier
        sync.isActive = if (isActive) 1 else 0
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        sync.syncCurrentTickFlow()
        setActiveState(world, pos, state, isActive)
        markDirty()
    }

    // ExtendedScreenHandlerFactory
    override fun getDisplayName(): Text = Text.translatable("block.ic2_120_advanced_solar_addon.quantum_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        QuantumGeneratorScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        production = nbt.getInt("production")
        isActive = nbt.getBoolean("active")
        sync.restoreEnergy(nbt.getLong(QuantumGeneratorSync.NBT_ENERGY).coerceIn(0L, sync.capacity))
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putInt("production", production)
        nbt.putBoolean("active", isActive)
        nbt.putLong(QuantumGeneratorSync.NBT_ENERGY, sync.amount)
        syncedData.writeNbt(nbt)
    }
}
