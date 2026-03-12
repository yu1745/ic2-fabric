package ic2_120.content.block.machines

import ic2_120.content.sync.MfsuSync
import ic2_120.content.ModBlockEntities
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MfsuBlock
import ic2_120.content.screen.MfsuScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * MFSU 方块实体。仅能量存储，通过 Energy API 与电网交互。
 */
@ModBlockEntity(block = MfsuBlock::class)
class MfsuBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ExtendedScreenHandlerFactory, ITieredMachine {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(MfsuBlockEntity::class.java)
    }

    override val tier: Int = 4

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = MfsuSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        { world?.time },
        tier = tier
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(MfsuBlockEntity::class),
        pos,
        state
    )

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text =
        Text.translatable("container.ic2_120.mfsu")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MfsuScreenHandler(syncId, playerInventory, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(MfsuSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        syncedData.writeNbt(nbt)
        nbt.putLong(MfsuSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        // 记录 tick 开始时的状态
//        val energyBefore = sync.amount
//        val inputBefore = sync.lastInsertedAmount
//        val outputBefore = sync.lastExtractedAmount

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 在 tick 结束时同步当前 tick 的实际输入/输出
        sync.syncCurrentTickFlow()

//        // 调试日志：记录能量流动
//        val energyAfter = sync.amount
//        val inputAfter = sync.lastInsertedAmount
//        val outputAfter = sync.lastExtractedAmount
//
//        if (inputAfter > 0 || outputAfter > 0) {
//            LOGGER.info(
//                "MFSU tick - Pos: {}, Energy:  {}, Input: {} EU/t, Output: {} EU/t",
//                pos,
//                energyAfter,
//                inputAfter,
//                outputAfter
//            )
//        }
    }
}

