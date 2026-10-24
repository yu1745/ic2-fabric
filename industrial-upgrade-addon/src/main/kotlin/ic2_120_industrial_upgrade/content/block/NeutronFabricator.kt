package ic2_120_industrial_upgrade.content.block

import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import ic2_120_industrial_upgrade.content.item.EnderQuantumComponent
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
import net.minecraft.world.MenuProvider
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer

// i18n: block.ic2_120_industrial_upgrade.neutron_fabricator
// zh_cn: 中子制造机
// en_us: Neutron Fabricator
@ModBlock(name = "neutron_fabricator", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "machine")
class NeutronFabricatorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = NeutronFabricatorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, NeutronFabricatorBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: Level, pos: BlockPos): MenuProvider? =
        world.getBlockEntity(pos) as? MenuProvider

    @Deprecated("Override without InteractionHand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
    override fun onUse(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) createScreenHandlerFactory(state, world, pos)?.let { player.openHandledScreen(it) }
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
            // 1.4.0: " A "/"BCB"/" A "：2×末影量子组件 + 1×ic2 物质制造机 + 2×(1.4.0 QuantumItems5)
            // 简化：用末影量子组件 + 高级机器外壳 + 高级电路
            val enderComponent = EnderQuantumComponent::class.instance()
            val advancedMachine = ic2_120.content.block.AdvancedMachineCasingBlock::class.item()

            ShapedRecipeBuilder.create(RecipeCategory.MISC, NeutronFabricatorBlock::class.item(), 1)
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" A ")
                .input('A', enderComponent)
                .input('B', advancedMachine)
                .input('C', Items.NETHER_STAR)
                .criterion(hasItem(enderComponent), conditionsFromItem(enderComponent))
                .offerTo(exporter, IC2IndustrialUpgrade.id("neutron_fabricator"))
        }
    }
}
