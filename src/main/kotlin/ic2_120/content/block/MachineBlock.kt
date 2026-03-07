package ic2_120.content.block

import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.BlockPos

/**
 * 简单的机器方块基类。
 * 可被子类扩展以支持 BlockEntity。
 */
open class MachineBlock(settings: AbstractBlock.Settings) : BlockWithEntity(settings) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null

    /**
     * BlockWithEntity 默认可能不会使用 JSON 模型渲染，显式指定为 MODEL
     * 以确保像电炉这类机器方块按 blockstate/model 显示材质。
     */
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
}
