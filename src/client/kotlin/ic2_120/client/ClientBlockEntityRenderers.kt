package ic2_120.client

import ic2_120.client.renderers.ComposeDebugBlockEntityRenderer
import ic2_120.client.renderers.PipeBlockEntityRenderer
import ic2_120.client.renderers.TeleporterBlockEntityRenderer
import ic2_120.client.renderers.TransmissionBlockEntityRenderer
import ic2_120.client.renderers.WindGeneratorBlockEntityRenderer
import ic2_120.client.renderers.WindKineticGeneratorBlockEntityRenderer
import ic2_120.client.renderers.WaterKineticGeneratorBlockEntityRenderer
import ic2_120.client.renderers.ManualKineticGeneratorBlockEntityRenderer
import ic2_120.content.block.ComposeDebugBlockEntity
import ic2_120.content.block.machines.ManualKineticGeneratorBlockEntity
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.transmission.BevelGearBlock
import ic2_120.content.block.transmission.IronTransmissionShaftBlock
import ic2_120.content.block.transmission.TransmissionBlockEntity
import ic2_120.content.block.transmission.WoodTransmissionShaftBlock
import ic2_120.content.block.transmission.SteelTransmissionShaftBlock
import ic2_120.content.block.transmission.CarbonTransmissionShaftBlock
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.util.math.BlockPos

object ClientBlockEntityRenderers {
    fun register() {
        BlockEntityRendererFactories.register(
            WindGeneratorBlockEntity::class.type(),
            ::WindGeneratorBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            WindKineticGeneratorBlockEntity::class.type(),
            ::WindKineticGeneratorBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            WaterKineticGeneratorBlockEntity::class.type(),
            ::WaterKineticGeneratorBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            ManualKineticGeneratorBlockEntity::class.type(),
            ::ManualKineticGeneratorBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            TransmissionBlockEntity::class.type(),
            ::TransmissionBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            ComposeDebugBlockEntity::class.type(),
            ::ComposeDebugBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            TeleporterBlockEntity::class.type(),
            ::TeleporterBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            PipeBlockEntity::class.type(),
            ::PipeBlockEntityRenderer
        )

        // 为传动轴和伞齿轮注册物品渲染器（使用方块实体渲染器在物品栏中渲染 3D 模型）
        val shaftBlocks = listOf(
            WoodTransmissionShaftBlock::class.instance(),
            IronTransmissionShaftBlock::class.instance(),
            SteelTransmissionShaftBlock::class.instance(),
            CarbonTransmissionShaftBlock::class.instance(),
            BevelGearBlock::class.instance()
        )
        for (block in shaftBlocks) {
            val dummyEntity = TransmissionBlockEntity(BlockPos.ORIGIN, block.defaultState)
            BuiltinItemRendererRegistry.INSTANCE.register(block.asItem()) { _, _, matrices, vertexConsumers, light, overlay ->
                MinecraftClient.getInstance().blockEntityRenderDispatcher.renderEntity(
                    dummyEntity,
                    matrices,
                    vertexConsumers,
                    light,
                    overlay
                )
            }
        }
    }
}
