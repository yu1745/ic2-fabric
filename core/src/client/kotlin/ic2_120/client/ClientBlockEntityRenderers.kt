package ic2_120.client

import ic2_120.client.renderers.ComposeDebugBlockEntityRenderer
import ic2_120.client.renderers.Machine3dBlockEntityRenderer
import ic2_120.client.renderers.PipeBlockEntityRenderer
import ic2_120.client.renderers.TeleporterBlockEntityRenderer
import ic2_120.client.renderers.TransmissionBlockEntityRenderer
import ic2_120.client.renderers.WindGeneratorBlockEntityRenderer
import ic2_120.client.renderers.WindKineticGeneratorBlockEntityRenderer
import ic2_120.client.renderers.WaterKineticGeneratorBlockEntityRenderer
import ic2_120.client.renderers.ManualKineticGeneratorBlockEntityRenderer
import ic2_120.client.renderers.LeashKineticGeneratorBlockEntityRenderer
import ic2_120.client.renderers.MinerBlockEntityRenderer
import ic2_120.content.block.ComposeDebugBlockEntity
import ic2_120.content.block.machines.AdvancedMinerBlockEntity
import ic2_120.content.block.machines.PumpBlockEntity
import ic2_120.content.block.machines.FluidCannerBlockEntity
import ic2_120.content.block.machines.CondenserBlockEntity
import ic2_120.content.block.machines.CropHarvesterBlockEntity
import ic2_120.content.block.machines.CropmatronBlockEntity
import ic2_120.content.block.machines.AnimalSlaughtererBlockEntity
import ic2_120.content.block.machines.ChunkLoaderBlockEntity
import ic2_120.content.block.machines.StirlingGeneratorBlockEntity
import ic2_120.content.block.machines.WaterGeneratorBlockEntity
import ic2_120.content.block.machines.BlockCutterBlockEntity
import ic2_120.content.block.machines.CannerBlockEntity
import ic2_120.content.block.machines.CentrifugeBlockEntity
import ic2_120.content.block.machines.MetalFormerBlockEntity
import ic2_120.content.block.machines.OreWashingPlantBlockEntity
import ic2_120.content.block.machines.CompressorBlockEntity
import ic2_120.content.block.machines.ExtractorBlockEntity
import ic2_120.content.block.machines.MaceratorBlockEntity
import ic2_120.content.block.machines.RecyclerBlockEntity
import ic2_120.content.block.machines.SolidCannerBlockEntity
import ic2_120.content.block.machines.LeashKineticGeneratorBlockEntity
import ic2_120.content.block.machines.ManualKineticGeneratorBlockEntity
import ic2_120.content.block.machines.MinerBlockEntity
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
        // 3D 机器运动件（动画参数见 assets/ic2_120/machine3d/animations.json）
        BlockEntityRendererFactories.register(MaceratorBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(CompressorBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(ExtractorBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(RecyclerBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(CannerBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(SolidCannerBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(BlockCutterBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(CentrifugeBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(MetalFormerBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(OreWashingPlantBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(StirlingGeneratorBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(WaterGeneratorBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(PumpBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(FluidCannerBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(CondenserBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(CropHarvesterBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(CropmatronBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(AnimalSlaughtererBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
        BlockEntityRendererFactories.register(ChunkLoaderBlockEntity::class.type(), ::Machine3dBlockEntityRenderer)
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
            LeashKineticGeneratorBlockEntity::class.type(),
            ::LeashKineticGeneratorBlockEntityRenderer
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
        BlockEntityRendererFactories.register(
            MinerBlockEntity::class.type(),
            ::MinerBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            AdvancedMinerBlockEntity::class.type(),
            ::MinerBlockEntityRenderer
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
