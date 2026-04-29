package ic2_120.content.block

import ic2_120.content.block.MachineCasingBlock
import ic2_120.content.block.cables.InsulatedTinCableBlock
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.block.cables.InsulatedGoldCableBlock
import ic2_120.content.block.cables.TripleInsulatedIronCableBlock
import ic2_120.content.block.machines.LvTransformerBlockEntity
import ic2_120.content.block.machines.MvTransformerBlockEntity
import ic2_120.content.block.machines.HvTransformerBlockEntity
import ic2_120.content.block.machines.EvTransformerBlockEntity
import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 变压器方块基类。
 *
 * 变压器只改变电压等级（EU/t 速率），不改变 EU 总量。
 *
 * 能量等级转换：
 * - LV变压器 (1级): 低级 32 EU/t <-> 高级 128 EU/t (2级)
 * - MV变压器 (2级): 低级 128 EU/t <-> 高级 512 EU/t (3级)
 * - HV变压器 (3级): 低级 512 EU/t <-> 高级 2048 EU/t (4级)
 * - EV变压器 (4级): 低级 2048 EU/t <-> 高级 8192 EU/t (5级)
 *
 * 工作模式（通过UI切换）：
 * - 升压模式：正面接收低级能量，其他五面输出高级能量
 *   - 例：4 tick × 32 EU/t = 128 EU → 1 tick × 128 EU/t
 * - 降压模式：其他五面接收高级能量，正面输出低级能量
 *   - 例：1 tick × 128 EU/t = 128 EU → 4 tick × 32 EU/t
 * - EU 总量始终守恒
 *
 * 方块支持六面朝向（上下南北西东），"正面"是方块朝向的那一面。
 * 继承自 [DirectionalMachineBlock] 以获得六面朝向支持。
 */
abstract class TransformerBlock : DirectionalMachineBlock() {

    abstract fun createTransformerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        createTransformerBlockEntity(pos, state)

    // 添加 ACTIVE 状态属性（六面朝向由 DirectionalMachineBlock 提供）
    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)  // 保留 FACING
        builder.add(ACTIVE)  // 添加激活状态
    }

    init {
        // 设置默认激活状态为 false（FACING 由父类处理）
        defaultState = stateManager.defaultState.with(ACTIVE, false)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        // 根据玩家面向的方向决定变压器正面朝向（水平方向）
        // 变压器通常放置在地面，正面应该朝向玩家面对的方向
        val facing = ctx.horizontalPlayerFacing.opposite
        return super.getPlacementState(ctx)?.with(ACTIVE, false)?.with(Properties.FACING, facing)
    }

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else {
            val beType = when (this) {
                is LvTransformerBlock -> LvTransformerBlockEntity::class.type()
                is MvTransformerBlock -> MvTransformerBlockEntity::class.type()
                is HvTransformerBlock -> HvTransformerBlockEntity::class.type()
                is EvTransformerBlock -> EvTransformerBlockEntity::class.type()
                else -> return null
            }
            checkType(type, beType) { world1, pos, state, be ->
                (be as ic2_120.content.block.machines.TransformerBlockEntity).tick(world1, pos, state)
            }
        }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}

@ModBlock(name = "lv_transformer", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "transformer")
class LvTransformerBlock : TransformerBlock() {
    override fun createTransformerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        LvTransformerBlockEntity(pos, state)

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val insulatedTinCable = InsulatedTinCableBlock::class.item()
            val coil = ic2_120.content.item.Coil::class.instance()
            val planks = Items.OAK_PLANKS
            if (coil != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LvTransformerBlock::class.item(), 1)
                    .pattern("PWP").pattern("PCP").pattern("PWP")
                    .input('P', planks).input('W', insulatedTinCable).input('C', coil)
                    .criterion(hasItem(coil), conditionsFromItem(coil))
                    .offerTo(exporter, LvTransformerBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "mv_transformer", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "transformer")
class MvTransformerBlock : TransformerBlock() {
    override fun createTransformerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        MvTransformerBlockEntity(pos, state)

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val insulatedCopperCable = InsulatedCopperCableBlock::class.item()
            val machine = MachineCasingBlock::class.item()
            if (machine != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MvTransformerBlock::class.item(), 1)
                    .pattern(" W ").pattern(" M ").pattern(" W ")
                    .input('W', insulatedCopperCable).input('M', machine)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, MvTransformerBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "hv_transformer", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "transformer")
class HvTransformerBlock : TransformerBlock() {
    override fun createTransformerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        HvTransformerBlockEntity(pos, state)

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val insulatedGoldCable = InsulatedGoldCableBlock::class.item()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            val advancedReBattery = ic2_120.content.item.energy.AdvancedReBatteryItem::class.instance()
            if (MvTransformerBlock::class.item() != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, HvTransformerBlock::class.item(), 1)
                    .pattern(" W ").pattern("CTB").pattern(" W ")
                    .input('W', insulatedGoldCable).input('C', circuit).input('T', MvTransformerBlock::class.item()).input('B', advancedReBattery)
                    .criterion(hasItem(MvTransformerBlock::class.item()), conditionsFromItem(MvTransformerBlock::class.item()))
                    .offerTo(exporter, HvTransformerBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "ev_transformer", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "transformer")
class EvTransformerBlock : TransformerBlock() {
    override fun createTransformerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        EvTransformerBlockEntity(pos, state)

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val insulatedIronCable = TripleInsulatedIronCableBlock::class.item()
            val advancedCircuit = ic2_120.content.item.AdvancedCircuit::class.instance()
            val lapotronCrystal = ic2_120.content.item.energy.LapotronCrystalItem::class.instance()
            if (HvTransformerBlock::class.item() != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EvTransformerBlock::class.item(), 1)
                    .pattern(" W ").pattern("CTL").pattern(" W ")
                    .input('W', insulatedIronCable).input('C', advancedCircuit).input('T', HvTransformerBlock::class.item()).input('L', lapotronCrystal)
                    .criterion(hasItem(HvTransformerBlock::class.item()), conditionsFromItem(HvTransformerBlock::class.item()))
                    .offerTo(exporter, EvTransformerBlock::class.id())
            }
        }
    }
}