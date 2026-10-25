package ic2_120.content.block

import ic2_120.compat.save

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import ic2_120.compat.has

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.content.block.machines.AnimalmatronBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.EmptyCell
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.id
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
import java.util.function.Consumer

@ModBlock(name = "animalmatron", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class AnimalmatronBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AnimalmatronBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, AnimalmatronBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let(player::openMenu)
        }
        return InteractionResult.SUCCESS
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        /** 监管范围半径（与 BE runScan 的 AABB(pos).expand(SCAN_RADIUS) 一致）。 */
        const val RANGE_RADIUS = 4.0

        /**
         * 统一的范围判定（P1/P4 修复）：Mixin 拦截自然生长/下蛋、JADE 展示与 BE 喂食扫描
         * 必须用同一几何，否则边界先上会出现"被拦截自然生长却不被喂食"的永久卡死幼崽。
         * 几何 = 机器方块 AABB.expand(4) 与实体 bbox 相交（与 BE getEntitiesByClass 一致），
         * 并额外要求机器有电（sync.amount > 0，断电即不再拦截/JADE 不再显示，避免幼崽冻结）。
         *
         * 注：不使用方块 ACTIVE 属性——那是"本次扫描是否碰到动物"的瞬时状态，闪烁不定。
         *
         * P6（多机重叠）：档案每机独立，重叠区喂食/水耗/EU 会叠加，繁殖可能绕过单机 32 上限。
         * 已知限制，不修——有意让多台机器范围重叠时各自独立工作（文档化于此）。
         */
        @JvmStatic
        fun isManaged(entity: net.minecraft.world.entity.Entity): Boolean {
            val world = entity.level()
            if (world !is net.minecraft.server.level.ServerLevel) return false
            val searchBox = entity.boundingBox.inflate(RANGE_RADIUS)
            val minPos = net.minecraft.core.BlockPos(
                Math.floor(searchBox.minX).toInt(),
                Math.floor(searchBox.minY).toInt(),
                Math.floor(searchBox.minZ).toInt()
            )
            val maxPos = net.minecraft.core.BlockPos(
                Math.ceil(searchBox.maxX).toInt(),
                Math.ceil(searchBox.maxY).toInt(),
                Math.ceil(searchBox.maxZ).toInt()
            )
            for (bp in net.minecraft.core.BlockPos.betweenClosed(minPos, maxPos)) {
                if (world.getBlockState(bp).block !is AnimalmatronBlock) continue
                val be = world.getBlockEntity(bp) as? AnimalmatronBlockEntity ?: continue
                if (be.sync.amount > 0L) return true
            }
            return false
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val emptyCell = EmptyCell::class.instance()
            if (machine == Items.AIR || circuit == Items.AIR || emptyCell == Items.AIR) return

            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, AnimalmatronBlock::class.item(), 1)
                .pattern("CHC")
                .pattern("EME")
                .pattern("WWW")
                .define('C', circuit)
                .define('H', Items.CHEST)
                .define('E', emptyCell)
                .define('M', machine)
                .define('W', Items.WHEAT)
                .unlockedBy(getHasName(machine), has(machine))
                .save(exporter, AnimalmatronBlock::class.id())
        }
    }
}
