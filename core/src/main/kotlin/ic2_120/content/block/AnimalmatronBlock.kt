package ic2_120.content.block

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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlock(name = "animalmatron", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class AnimalmatronBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AnimalmatronBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
<<<<<<< HEAD
        else checkType(type, AnimalmatronBlockEntity::class.type()) { w, p, s, be ->
=======
        else validateTicker(type, AnimalmatronBlockEntity::class.type()){ w, p, s, be ->
>>>>>>> backup/1.21.1
            be.tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: World,
        pos: BlockPos
    ): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

<<<<<<< HEAD
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
=======
    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
>>>>>>> backup/1.21.1
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return ActionResult.SUCCESS
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        /** 监管范围半径（与 BE runScan 的 Box(pos).expand(SCAN_RADIUS) 一致）。 */
        const val RANGE_RADIUS = 4.0

        /**
         * 统一的范围判定（P1/P4 修复）：Mixin 拦截自然生长/下蛋、JADE 展示与 BE 喂食扫描
         * 必须用同一几何，否则边界先上会出现"被拦截自然生长却不被喂食"的永久卡死幼崽。
         * 几何 = 机器方块 Box.expand(4) 与实体 bbox 相交（与 BE getEntitiesByClass 一致），
         * 并额外要求机器有电（sync.amount > 0，断电即不再拦截/JADE 不再显示，避免幼崽冻结）。
         *
         * 注：不使用方块 ACTIVE 属性——那是"本次扫描是否碰到动物"的瞬时状态，闪烁不定。
         *
         * P6（多机重叠）：档案每机独立，重叠区喂食/水耗/EU 会叠加，繁殖可能绕过单机 32 上限。
         * 已知限制，不修——有意让多台机器范围重叠时各自独立工作（文档化于此）。
         */
        @JvmStatic
        fun isManaged(entity: net.minecraft.entity.Entity): Boolean {
            val world = entity.world
            if (world !is net.minecraft.server.world.ServerWorld) return false
            val searchBox = entity.boundingBox.expand(RANGE_RADIUS)
            val minPos = net.minecraft.util.math.BlockPos(
                Math.floor(searchBox.minX).toInt(),
                Math.floor(searchBox.minY).toInt(),
                Math.floor(searchBox.minZ).toInt()
            )
            val maxPos = net.minecraft.util.math.BlockPos(
                Math.ceil(searchBox.maxX).toInt(),
                Math.ceil(searchBox.maxY).toInt(),
                Math.ceil(searchBox.maxZ).toInt()
            )
            for (bp in net.minecraft.util.math.BlockPos.iterate(minPos, maxPos)) {
                if (world.getBlockState(bp).block !is AnimalmatronBlock) continue
                val be = world.getBlockEntity(bp) as? AnimalmatronBlockEntity ?: continue
                if (be.sync.amount > 0L) return true
            }
            return false
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val emptyCell = EmptyCell::class.instance()
            if (machine == Items.AIR || circuit == Items.AIR || emptyCell == Items.AIR) return

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AnimalmatronBlock::class.item(), 1)
                .pattern("CHC")
                .pattern("EME")
                .pattern("WWW")
                .input('C', circuit)
                .input('H', Items.CHEST)
                .input('E', emptyCell)
                .input('M', machine)
                .input('W', Items.WHEAT)
                .criterion(hasItem(machine), conditionsFromItem(machine))
                .offerTo(exporter, AnimalmatronBlock::class.id())
        }
    }
}
