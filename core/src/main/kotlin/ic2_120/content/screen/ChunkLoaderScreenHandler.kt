package ic2_120.content.screen

import ic2_120.content.block.ChunkLoaderBlock
import ic2_120.content.block.machines.ChunkLoaderBlockEntity
import ic2_120.content.sync.ChunkLoaderSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos

@ModScreenHandler(block = ChunkLoaderBlock::class)
class ChunkLoaderScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ChunkLoaderScreenHandler::class.type(), syncId) {

    val sync = ChunkLoaderSync(SyncedDataView(propertyDelegate))

    /** 机器位置，客户端通过 [fromBuffer] 传入 */
    val machinePos: BlockPos = run {
        val ref = object { var pos: BlockPos = BlockPos.ORIGIN }
        context.get({ _, p -> ref.pos = p })
        ref.pos
    }

    init {
        addProperties(propertyDelegate)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    /**
     * 本 GUI 只有玩家背包槽、没有机器物品槽位（区块加载器不存储物品），shift+左键不做任何转移。
     *
     * 之前这里把整个玩家背包区间 [0,36) 当作插入目标，而源槽自身也在该区间内：
     * insertItem 合并遍历到源槽时（stack 与槽内 stack 是同一实例）会计算 newCount = count + count，
     * 在 count ≤ 最大堆叠数/2 时直接把槽内数量翻倍——凭空复制物品。
     * 与其它无槽位 GUI（Steam/KineticGenerator 等）保持一致：返回 EMPTY 即 no-op。
     */
    override fun quickMove(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id < 0 || id >= ChunkLoaderSync.CHUNK_COUNT) return false
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? ChunkLoaderBlockEntity ?: return@get
            be.toggleChunk(id)
        }, true)
        return true
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is ChunkLoaderBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 0
        const val HOTBAR_END = 36

    }
}
