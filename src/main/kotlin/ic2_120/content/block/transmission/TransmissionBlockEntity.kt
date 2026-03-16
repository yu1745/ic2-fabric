package ic2_120.content.block.transmission

import ic2_120.registry.type
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

/**
 * 传动系统方块实体（仅用于客户端 BER 动画渲染）。
 *
 * 不参与电力系统，不存储额外状态。
 */
class TransmissionBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(TYPE, pos, state) {
    companion object {
        lateinit var TYPE: BlockEntityType<TransmissionBlockEntity>
            private set

        fun register(modId: String) {
            val blockIds = listOf(
                Identifier(modId, "wood_transmission_shaft"),
                Identifier(modId, "iron_transmission_shaft"),
                Identifier(modId, "steel_transmission_shaft"),
                Identifier(modId, "carbon_transmission_shaft"),
                Identifier(modId, "bevel_gear")
            )

            val blocks = blockIds.mapNotNull { id -> Registries.BLOCK.getOrEmpty(id).orElse(null) }
            require(blocks.size == blockIds.size) {
                "注册传动系统 BlockEntity 失败：找不到部分方块。需要: ${blockIds.joinToString()}"
            }

            val factory = FabricBlockEntityTypeBuilder.Factory { pos: BlockPos, state: BlockState ->
                TransmissionBlockEntity(pos, state)
            }

            @Suppress("UNCHECKED_CAST")
            val type = FabricBlockEntityTypeBuilder.create(factory, *blocks.toTypedArray())
                .build() as BlockEntityType<TransmissionBlockEntity>

            TYPE = type
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier(modId, "transmission"), type)
            ic2_120.registry.ClassScanner.registerBlockEntityType(TransmissionBlockEntity::class, type)
        }
    }
}

