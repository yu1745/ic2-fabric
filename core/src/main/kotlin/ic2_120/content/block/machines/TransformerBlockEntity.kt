package ic2_120.content.block.machines

import ic2_120.content.sync.TransformerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.TransformerBlock
import ic2_120.content.block.LvTransformerBlock
import ic2_120.content.block.MvTransformerBlock
import ic2_120.content.block.HvTransformerBlock
import ic2_120.content.block.EvTransformerBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
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
 * 变压器方块实体。
 *
 * 变压器只改变电压等级（EU/t 速率），不改变 EU 总量。
 *
 * 能量等级转换：
 * - LV变压器 (1级): 低级 32 EU/t <-> 高级 128 EU/t (2级)
 * - MV变压器 (2级): 低级 128 EU/t <-> 高级 512 EU/t (3级)
 * - HV变压器 (3级): 低级 512 EU/t <-> 高级 2048 EU/t (4级)
 * - EV变压器 (4级): 低级 2048 EU/t <-> 高级 8192 EU/t (5级)
 *
 * 工作模式（通过UI切换）：
 * - 升压 (STEP_UP): 正面接收低级能量，其他五面输出高级能量
 *   - 例：4 tick × 32 EU/t = 128 EU → 1 tick × 128 EU/t
 * - 降压 (STEP_DOWN): 其他五面接收高级能量，正面输出低级能量
 *   - 例：1 tick × 128 EU/t = 128 EU → 4 tick × 32 EU/t
 * - EU 总量始终守恒
 *
 * 注意：实际的能量输入/输出由能量网络通过 SidedEnergyContainer API 自动处理。
 * tick 方法中的转换逻辑用于清理/调整内部存储，确保能量状态同步。
 */
open class TransformerBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    private val transformerTier: Int
) : BlockEntity(type, pos, state), ITieredMachine, ExtendedScreenHandlerFactory {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(TransformerBlockEntity::class.java)
    }

    override val tier: Int = transformerTier

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = TransformerSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.FACING) ?: Direction.NORTH },
        { world?.time },
        transformerTier
    )

    /**
     * 实现分面电压等级。
     * 变压器不同面的电压等级不同：
     * - 正面为低级（tier），其他面为高级（tier+1）
     *
     * 注意：这个方法返回各面的物理电压等级，用于过压检测和电网输出等级计算。
     * 电网会通过检查 supportsInsertion() 来区分真正的输出面（机器向电网输出能量的面）。
     *
     * @param side 方向，null 表示查询整体等级（返回低级等级）
     * @return 该方向的物理电压等级
     */
    override fun getVoltageTierForSide(side: Direction?): Int {
        if (side == null) return tier

        val facing = world?.getBlockState(pos)?.get(Properties.FACING) ?: return tier
        val isFront = (side == facing)

        // 正面是低级，其他面都是高级
        return if (isFront) tier else tier + 1
    }

    // 注意：默认构造函数由具体子类提供，需要指定 BlockEntityType

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeVarInt(transformerTier)  // 传递变压器等级到客户端
    }

    override fun getDisplayName(): Text {
        val key = when (transformerTier) {
            1 -> "block.ic2_120.lv_transformer"
            2 -> "block.ic2_120.mv_transformer"
            3 -> "block.ic2_120.hv_transformer"
            4 -> "block.ic2_120.ev_transformer"
            else -> "block.ic2_120.lv_transformer"
        }
        return Text.translatable(key)
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        // 变压器使用通用的 TransformerScreenHandler
        return ic2_120.content.screen.TransformerScreenHandler(
            syncId,
            playerInventory,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            transformerTier  // 传递变压器等级
        )
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(TransformerSync.NBT_ENERGY_STORED).coerceIn(0L, sync.capacity)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.setMode(TransformerSync.Mode.fromId(nbt.getInt(TransformerSync.NBT_MODE)))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        syncedData.writeNbt(nbt)
        nbt.putLong(TransformerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(TransformerSync.NBT_MODE, sync.mode)
    }

    /**
     * 切换模式（由 GUI 按钮调用）
     */
    fun toggleMode() {
        sync.toggleMode()
        markDirty()
    }

    /**
     * 获取当前同步的模式（用于客户端 tooltip 显示）
     */
    fun getSyncedMode(): TransformerSync.Mode = sync.getMode()

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 更新同步能量值
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 在 tick 结束时同步当前 tick 的实际输入/输出
        sync.syncCurrentTickFlow()

        // 更新方块状态
        val currentMode = sync.getMode()
        val wasActive = state.get(TransformerBlock.ACTIVE)
        val isActive = sync.amount > 0

        if (wasActive != isActive) {
            world.setBlockState(pos, state.with(TransformerBlock.ACTIVE, isActive))
        }

        // 调试日志：记录能量流动
        val energyAfter = sync.amount
        val inputAfter = sync.getSyncedInsertedAmount()
        val outputAfter = sync.getSyncedExtractedAmount()

        if (inputAfter > 0 || outputAfter > 0) {
            val modeText = when (currentMode) {
                TransformerSync.Mode.STEP_UP -> "升压"
                TransformerSync.Mode.STEP_DOWN -> "降压"
            }
//            LOGGER.info(
//                "Transformer tick - Pos: {}, Mode: {}, Energy: {}, Input: {} EU/t, Output: {} EU/t",
//                pos,
//                modeText,
//                energyAfter,
//                inputAfter,
//                outputAfter
//            )
        }

        // 注意：实际的能量输入/输出由能量网络通过 SidedEnergyContainer API 自动处理
        // TransformerSync.getSideMaxInsert/getSideMaxExtract 已经根据模式正确配置了各面的输入输出能力
        // 因此这里不需要手动进行能量传输，只需要确保能量状态同步
    }
}

/**
 * LV 变压器方块实体 (1级)
 */
@ModBlockEntity(block = LvTransformerBlock::class)
class LvTransformerBlockEntity(pos: BlockPos, state: BlockState) :
    TransformerBlockEntity(LvTransformerBlockEntity::class.type(), pos, state, transformerTier = 1)

/**
 * MV 变压器方块实体 (2级)
 */
@ModBlockEntity(block = MvTransformerBlock::class)
class MvTransformerBlockEntity(pos: BlockPos, state: BlockState) :
    TransformerBlockEntity(MvTransformerBlockEntity::class.type(), pos, state, transformerTier = 2)

/**
 * HV 变压器方块实体 (3级)
 */
@ModBlockEntity(block = HvTransformerBlock::class)
class HvTransformerBlockEntity(pos: BlockPos, state: BlockState) :
    TransformerBlockEntity(HvTransformerBlockEntity::class.type(), pos, state, transformerTier = 3)

/**
 * EV 变压器方块实体 (4级)
 */
@ModBlockEntity(block = EvTransformerBlock::class)
class EvTransformerBlockEntity(pos: BlockPos, state: BlockState) :
    TransformerBlockEntity(EvTransformerBlockEntity::class.type(), pos, state, transformerTier = 4)

/**
 * 变压器工具类
 */
object TransformerUtils {
    /**
     * 根据能量等级获取每 tick 的 EU 值
     * 1级(LV): 32, 2级(MV): 128, 3级(HV): 512, 4级(EV): 2048, 5级(EV+): 8192
     */
    fun getEuForTier(tier: Int): Int = when (tier) {
        1 -> 32
        2 -> 128
        3 -> 512
        4 -> 2048
        5 -> 8192
        else -> 32 * (1 shl (tier - 1).coerceIn(0, 4))
    }
}


