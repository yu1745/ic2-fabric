package ic2_120.content.block.nuclear

import ic2_120.content.upgrade.IRedstoneControlSupport
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 反应堆红石接口方块实体。
 * 提供红石控制功能，可以禁用/反转红石信号。
 * 将红石状态传递给中心反应堆。
 */
@ModBlockEntity(block = ReactorRedstonePortBlock::class)
class ReactorRedstonePortBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), IRedstoneControlSupport {

    // 次构造函数：仅接受 pos 和 state，自动获取 type
    constructor(pos: BlockPos, state: BlockState) : this(
        ReactorRedstonePortBlockEntity::class.type(),
        pos,
        state
    )

    override var redstoneInverted: Boolean = false

    // 当前是否允许运行（基于红石信号和反转设置）
    var redstoneAllowsRun: Boolean = true
        private set

    // 获取中心反应堆
    private fun getCentralReactor(): NuclearReactorBlockEntity? {
        val w = world ?: return null
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val be = w.getBlockEntity(pos.add(dx, dy, dz))
                    if (be is NuclearReactorBlockEntity) return be
                }
            }
        }
        return null
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 检查红石信号并更新状态
        redstoneAllowsRun = RedstoneControlComponent.canRun(world, pos, this)

        // 将红石状态传递给中心反应堆
        val reactor = getCentralReactor()
        if (reactor != null) {
            reactor.updateRedstonePortState(this, redstoneAllowsRun)
        }
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        redstoneInverted = nbt.getBoolean("RedstoneInverted")
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
    }
}
