package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.item.QuantumCore
import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
<<<<<<< HEAD
import net.minecraft.data.server.recipe.RecipeJsonProvider
=======
import net.minecraft.data.server.recipe.RecipeExporter
>>>>>>> backup/1.21.1
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
<<<<<<< HEAD
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer
=======
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
>>>>>>> backup/1.21.1

// i18n: block.ic2_120_advanced_solar_addon.quantum_solar_panel
// zh_cn: 量子太阳能发电机
// en_us: Quantum Solar Panel
@ModBlock(name = "quantum_solar_panel", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "solar_panel")
<<<<<<< HEAD
open class QuantumSolarPanelBlock : MachineBlock() {
=======
class QuantumSolarPanelBlock : MachineBlock() {
>>>>>>> backup/1.21.1

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        QuantumSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
<<<<<<< HEAD
        else checkType(type, QuantumSolarPanelBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }
=======
        else validateTicker(type, QuantumSolarPanelBlockEntity::class.type()) { w: World, p: BlockPos, s: BlockState, be -> be.tick(w, p, s) }
>>>>>>> backup/1.21.1

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? NamedScreenHandlerFactory
    }

<<<<<<< HEAD
    @Deprecated("Override without Hand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
=======
>>>>>>> backup/1.21.1
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
<<<<<<< HEAD
        hand: Hand,
=======
>>>>>>> backup/1.21.1
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
<<<<<<< HEAD
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
=======
        fun generateRecipes(exporter: RecipeExporter) {
>>>>>>> backup/1.21.1
            val ultimateSolar = UltimateSolarPanelBlock::class.item()
            val quantumCore = QuantumCore::class.instance()

            // 8个终极混合太阳能 + 1个量子核心
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuantumSolarPanelBlock::class.item(), 1)
                .pattern("UUU")
                .pattern("UQU")
                .pattern("UUU")
                .input('U', ultimateSolar)
                .input('Q', quantumCore)
                .criterion(hasItem(ultimateSolar), conditionsFromItem(ultimateSolar))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("quantum_solar_panel"))
        }
    }
}

@ModBlockEntity(block = QuantumSolarPanelBlock::class)
class QuantumSolarPanelBlockEntity(pos: BlockPos, state: BlockState) :
    SolarPanelBlockEntity(QuantumSolarPanelBlockEntity::class.type(), pos, state, dayPower = 4096, nightPower = 2048, maxStorage = 10000000, tier = 5, activeProperty = QuantumSolarPanelBlock.ACTIVE) {

    override fun getBlockName(): String = "quantum_solar_panel"
}
