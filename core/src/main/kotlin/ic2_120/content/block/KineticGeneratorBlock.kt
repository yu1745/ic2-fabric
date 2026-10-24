package ic2_120.content.block

import ic2_120.content.block.machines.KineticGeneratorBlockEntity
import ic2_120.content.item.ElectricMotor
import ic2_120.content.item.IronCrankHandle
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.item.TooltipContext
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.level.BlockGetter
import java.util.function.Consumer

@ModBlock(name = "kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class KineticGeneratorBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        KineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, KineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun createScreenHandlerFactory(state: BlockState, world: Level, pos: BlockPos): net.minecraft.world.MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.world.MenuProvider
    }

    @Environment(EnvType.CLIENT)
    override fun appendTooltip(
        stack: ItemStack,
        world: BlockGetter?,
        tooltip: MutableList<Component>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(Component.translatable("tooltip.ic2_120.kinetic_generator.ratio").formatted(ChatFormatting.GRAY))
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val generator = GeneratorBlock::class.item()
            val motor = ElectricMotor::class.instance()
            val crank = IronCrankHandle::class.instance()
            if (generator != Items.AIR && motor != Items.AIR && crank != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, KineticGeneratorBlock::class.item(), 1)
                    .pattern("GMF")
                    .input('G', generator)
                    .input('M', motor)
                    .input('F', crank)
                    .criterion(hasItem(generator), conditionsFromItem(generator))
                    .offerTo(exporter, KineticGeneratorBlock::class.id())
            }
        }
    }
}

