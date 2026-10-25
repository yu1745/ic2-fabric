package ic2_120.content.block

import ic2_120.compat.save

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import ic2_120.compat.has

import ic2_120.compat.isClient

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
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.tags.ItemTags
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import ic2_120.registry.id
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
 * - 升压模式：其他五面接收低级能量，正面输出高级能量
 *   - 例：4 tick × 32 EU/t = 128 EU → 1 tick × 128 EU/t
 * - 降压模式：正面接收高级能量，其他五面输出低级能量
 *   - 例：1 tick × 128 EU/t = 128 EU → 4 tick × 32 EU/t
 * - EU 总量始终守恒
 *
 * 方块支持六面朝向（上下南北西东），"正面"是方块朝向的那一面。
 * 继承自 [DirectionalMachineBlock] 以获得六面朝向支持。
 */
abstract class TransformerBlock : DirectionalMachineBlock(MachineBlock.defaultMachineSettings() .noOcclusion()) { // 3D 模型：关闭相邻面剔除

    abstract fun createTransformerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        createTransformerBlockEntity(pos, state)

    // 添加 ACTIVE 状态属性（六面朝向由 DirectionalMachineBlock 提供）
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)  // 保留 FACING
        builder.add(ACTIVE)  // 添加激活状态
    }

    init {
        // 设置默认激活状态为 false（FACING 由父类处理）
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false))
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        // 根据玩家面向的方向决定变压器正面朝向（水平方向）
        // 变压器通常放置在地面，正面应该朝向玩家面对的方向
        val facing = ctx.horizontalDirection.opposite
        return super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)?.setValue(BlockStateProperties.FACING, facing)
    }

    override fun <T : BlockEntity> getTicker(
        world: Level,
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
            validateTicker(type, beType) { world1, pos, state, be ->
                be.tick(world1, pos, state)
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

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")
    }
}

@ModBlock(name = "lv_transformer", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "transformer")
class LvTransformerBlock : TransformerBlock() {
    override fun createTransformerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        LvTransformerBlockEntity(pos, state)

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val insulatedTinCable = InsulatedTinCableBlock::class.item()
            val coil = ic2_120.content.item.Coil::class.instance()
            if (coil != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, LvTransformerBlock::class.item(), 1)
                    .pattern("PWP").pattern("PCP").pattern("PWP")
                    .define('P', ic2_120.compat.tagIngredient(ItemTags.PLANKS)).define('W', insulatedTinCable).define('C', coil)
                    .unlockedBy(getHasName(coil), has(coil))
                    .save(exporter, LvTransformerBlock::class.id())
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
        fun generateRecipes(exporter: RecipeOutput) {
            val insulatedCopperCable = InsulatedCopperCableBlock::class.item()
            val machine = MachineCasingBlock::class.item()
            if (machine != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, MvTransformerBlock::class.item(), 1)
                    .pattern(" W ").pattern(" M ").pattern(" W ")
                    .define('W', insulatedCopperCable).define('M', machine)
                    .unlockedBy(getHasName(machine), has(machine))
                    .save(exporter, MvTransformerBlock::class.id())
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
        fun generateRecipes(exporter: RecipeOutput) {
            val insulatedGoldCable = InsulatedGoldCableBlock::class.item()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            val advancedReBattery = ic2_120.content.item.energy.AdvancedReBatteryItem::class.instance()
            if (MvTransformerBlock::class.item() != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, HvTransformerBlock::class.item(), 1)
                    .pattern(" W ").pattern("CTB").pattern(" W ")
                    .define('W', insulatedGoldCable).define('C', circuit).define('T', MvTransformerBlock::class.item()).define('B', advancedReBattery)
                    .unlockedBy(getHasName(MvTransformerBlock::class.item()), has(MvTransformerBlock::class.item()))
                    .save(exporter, HvTransformerBlock::class.id())
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
        fun generateRecipes(exporter: RecipeOutput) {
            val insulatedIronCable = TripleInsulatedIronCableBlock::class.item()
            val advancedCircuit = ic2_120.content.item.AdvancedCircuit::class.instance()
            val lapotronCrystal = ic2_120.content.item.energy.LapotronCrystalItem::class.instance()
            if (HvTransformerBlock::class.item() != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, EvTransformerBlock::class.item(), 1)
                    .pattern(" W ").pattern("CTL").pattern(" W ")
                    .define('W', insulatedIronCable).define('C', advancedCircuit).define('T', HvTransformerBlock::class.item()).define('L', lapotronCrystal)
                    .unlockedBy(getHasName(HvTransformerBlock::class.item()), has(HvTransformerBlock::class.item()))
                    .save(exporter, EvTransformerBlock::class.id())
            }
        }
    }
}
