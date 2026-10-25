package ic2_120.content.block

import ic2_120.compat.save

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import ic2_120.compat.has

import ic2_120.compat.isClient

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
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
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
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.level.BlockGetter
import java.util.function.Consumer

@ModBlock(name = "kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class KineticGeneratorBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        KineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, KineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    class KineticGeneratorBlockItem(block: net.minecraft.world.level.block.Block, properties: Item.Properties) : net.minecraft.world.item.BlockItem(block, properties) {
        @Environment(EnvType.CLIENT)
        override fun appendHoverText(
            stack: ItemStack,
            context: Item.TooltipContext,
            display: net.minecraft.world.item.component.TooltipDisplay, consumer: java.util.function.Consumer<Component>,
            type: TooltipFlag
        ) {
            val tooltip = ic2_120.compat.TooltipSinkList(consumer)
            super.appendHoverText(stack, context, display, consumer, type)
            tooltip.add(Component.translatable("tooltip.ic2_120.kinetic_generator.ratio").withStyle(ChatFormatting.GRAY))
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { factory ->
                player.openMenu(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val generator = GeneratorBlock::class.item()
            val motor = ElectricMotor::class.instance()
            val crank = IronCrankHandle::class.instance()
            if (generator != Items.AIR && motor != Items.AIR && crank != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, KineticGeneratorBlock::class.item(), 1)
                    .pattern("GMF")
                    .define('G', generator)
                    .define('M', motor)
                    .define('F', crank)
                    .unlockedBy(getHasName(generator), has(generator))
                    .save(exporter, KineticGeneratorBlock::class.id())
            }
        }
    }
}

