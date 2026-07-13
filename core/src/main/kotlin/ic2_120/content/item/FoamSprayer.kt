package ic2_120.content.item

import ic2_120.content.block.FoamBlock
import ic2_120.content.block.IronScaffoldBlock
import ic2_120.content.block.ReinforcedFoamBlock
import ic2_120.content.block.ReinforcedIronScaffoldBlock
import ic2_120.content.block.ReinforcedWoodenScaffoldBlock
import ic2_120.content.block.WoodenScaffoldBlock
import ic2_120.content.block.IronFenceBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.Block
import net.minecraft.block.ScaffoldingBlock
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import java.util.ArrayDeque
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

/**
 * 建筑泡沫喷枪：仅储存建筑泡沫流体，容量 8 桶；每喷涂一格消耗 0.1 桶（100mb），满罐约 80 格。
 * 按住 Alt 并右键切换单格 / 多格喷涂模式。
 */
@ModItem(name = "foam_sprayer", tab = CreativeTab.IC2_MATERIALS, group = "construction_foam")
class FoamSprayerItem : Item(FabricItemSettings().maxCount(1)), ICreativeFullVariant {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val player = context.player ?: return ActionResult.PASS
        val hand = context.hand
        if (world.isClient) return ActionResult.SUCCESS

        val hit = BlockHitResult(
            context.hitPos,
            context.side,
            context.blockPos,
            context.hitsInsideBlock()
        )
        val sided = FluidStorage.SIDED.find(world, hit.blockPos, hit.side)
        if (sided != null && FluidStorageUtil.interactWithFluidStorage(sided, player, hand)) {
            return ActionResult.SUCCESS
        }

        val stack = player.getStackInHand(hand)
        if (stack.item != this) return ActionResult.PASS

        val placed = tryPlaceFoam(world, player, stack, hit)
        return if (placed) ActionResult.SUCCESS else ActionResult.PASS
    }

    override fun isItemBarVisible(stack: ItemStack): Boolean = true

    override fun getItemBarStep(stack: ItemStack): Int {
        val amt = getFluidAmount(stack)
        if (amt <= 0L) return 0
        return (13.0 * amt.toDouble() / CAPACITY_DROPLETS.toDouble()).toInt().coerceIn(1, 13)
    }

    override fun getItemBarColor(stack: ItemStack): Int = 0xFF_6B8E9F.toInt()

    override fun createFullVariant(): ItemStack {
        return ItemStack(this).also { setFluidAmount(it, CAPACITY_DROPLETS) }
    }

    companion object {
        private const val NBT_FLUID_DROPLETS = "FoamFluidDroplets"
        private const val NBT_MULTI_MODE = "FoamMultiMode"

        val CAPACITY_DROPLETS: Long = FluidConstants.BUCKET * 8L

        /** 每成功喷涂一格（含加固脚手架）消耗 0.1 桶。 */
        val DROPLETS_PER_BLOCK: Long = FluidConstants.BUCKET / 10L

        fun getFluidAmount(stack: ItemStack): Long {
            if (stack.isEmpty || stack.item !is FoamSprayerItem) return 0L
            return stack.nbt?.getLong(NBT_FLUID_DROPLETS)?.coerceIn(0L, CAPACITY_DROPLETS) ?: 0L
        }

        fun setFluidAmount(stack: ItemStack, amount: Long) {
            if (stack.isEmpty || stack.item !is FoamSprayerItem) return
            val v = amount.coerceIn(0L, CAPACITY_DROPLETS)
            if (v <= 0L) {
                stack.orCreateNbt.remove(NBT_FLUID_DROPLETS)
            } else {
                stack.orCreateNbt.putLong(NBT_FLUID_DROPLETS, v)
            }
        }

        fun isMultiMode(stack: ItemStack): Boolean {
            if (stack.isEmpty || stack.item !is FoamSprayerItem) return false
            return stack.orCreateNbt.getBoolean(NBT_MULTI_MODE)
        }

        /** @return 切换后的模式：true = 多格 */
        fun toggleMultiMode(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val next = !nbt.getBoolean(NBT_MULTI_MODE)
            nbt.putBoolean(NBT_MULTI_MODE, next)
            return next
        }

        private fun canPlaceFoamAt(world: World, pos: BlockPos): Boolean {
            @Suppress("DEPRECATION")
            if (!world.isChunkLoaded(pos)) return false
            if (!world.worldBorder.contains(pos)) return false
            val state = world.getBlockState(pos)
            return state.isAir
        }

        /**
         * 喷涂空间锚点：点在脚手架上时以该格为目标；否则为点击面外侧邻格。
         */
        private fun foamSprayAnchorPos(world: World, hit: BlockHitResult): BlockPos =
            if (isAnyScaffold(world.getBlockState(hit.blockPos).block)) hit.blockPos
            else hit.blockPos.offset(hit.side)

        private fun isScaffold(block: Block): Boolean =
            block is WoodenScaffoldBlock ||
                block is ReinforcedWoodenScaffoldBlock ||
                block is IronScaffoldBlock ||
                block is ReinforcedIronScaffoldBlock

        private fun isAnyScaffold(block: Block): Boolean =
            isScaffold(block) || block is ScaffoldingBlock

        private fun isWoodScaffold(block: Block): Boolean =
            block is WoodenScaffoldBlock || block is ReinforcedWoodenScaffoldBlock

        private fun isReinforcedIronScaffold(block: Block): Boolean =
            block is ReinforcedIronScaffoldBlock

    private fun tryPlaceFoam(world: World, player: PlayerEntity, stack: ItemStack, hit: BlockHitResult): Boolean {
            val foamState = FoamBlock::class.instance().defaultState
            val reinforcedFoamState = ReinforcedFoamBlock::class.instance().defaultState
            val cfPackStack = player.getEquippedStack(EquipmentSlot.CHEST)
            val useCfPack = cfPackStack.item is CfPack

            var budget = if (useCfPack) {
                CfPack.getFluidAmount(cfPackStack)
            } else {
                getFluidAmount(stack)
            }
            if (!player.abilities.creativeMode && budget < DROPLETS_PER_BLOCK) return false

            val multi = isMultiMode(stack)
            val positions = if (multi) {
                val scaffoldAnchor = isAnyScaffold(world.getBlockState(hit.blockPos).block)
                if (scaffoldAnchor) {
                    val maxBlocks = if (player.abilities.creativeMode) {
                        SCAFFOLD_MULTI_MAX_BLOCKS
                    } else {
                        (budget / DROPLETS_PER_BLOCK).toInt()
                    }
                    buildScaffoldMultiSprayPositions(world, hit.blockPos, maxBlocks)
                } else {
                    buildMultiSprayPositions(world, hit)
                }
            } else {
                listOf(foamSprayAnchorPos(world, hit))
            }

            var placedAny = false
            for (pos in positions) {
                if (!player.abilities.creativeMode && budget < DROPLETS_PER_BLOCK) break
                val stateAt = world.getBlockState(pos)
                val placed = when {
                    isWoodScaffold(stateAt.block) -> {
                        // 原版喷枪会先按脚手架自身掉落表掉落，再替换为普通泡沫。
                        Block.dropStacks(stateAt, world, pos)
                        world.setBlockState(pos, foamState, Block.NOTIFY_ALL)
                    }
                    isReinforcedIronScaffold(stateAt.block) -> {
                        // 强化铁脚手架只额外掉落铁栅栏，随后变为强化泡沫。
                        Block.dropStack(world, pos, ItemStack(IronFenceBlock::class.item()))
                        world.setBlockState(pos, reinforcedFoamState, Block.NOTIFY_ALL)
                    }
                    stateAt.block is IronScaffoldBlock ->
                        world.setBlockState(pos, reinforcedFoamState, Block.NOTIFY_ALL)
                    canPlaceFoamAt(world, pos) ->
                        world.setBlockState(pos, foamState, Block.NOTIFY_ALL)
                    else -> false
                }
                if (!placed) continue
                placedAny = true
                if (!player.abilities.creativeMode) {
                    budget -= DROPLETS_PER_BLOCK
                }
            }

            if (placedAny) {
                if (!player.abilities.creativeMode) {
                    if (useCfPack) {
                        CfPack.setFluidAmount(cfPackStack, budget)
                    } else {
                        setFluidAmount(stack, budget)
                    }
                }
                val soundPos = hit.blockPos.offset(hit.side)
                world.playSound(null, soundPos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 0.55f, 0.95f + world.random.nextFloat() * 0.1f)
                world.emitGameEvent(player, GameEvent.BLOCK_PLACE, soundPos)
            }
            return placedAny
        }

        /**
         * 多格模式命中脚手架时，沿相连的 IC2/原版脚手架做 BFS。
         * 这样喷涂结果会保留脚手架的实际结构，不再生成随机球形区域。
         */
        private fun buildScaffoldMultiSprayPositions(
            world: World,
            start: BlockPos,
            maxBlocks: Int,
        ): List<BlockPos> {
            if (maxBlocks <= 0 || !isAnyScaffold(world.getBlockState(start).block)) return emptyList()

            val result = ArrayList<BlockPos>(maxBlocks)
            val queue = ArrayDeque<BlockPos>()
            val visited = mutableSetOf<BlockPos>()
            queue.add(start)

            while (queue.isNotEmpty() && result.size < maxBlocks) {
                val pos = queue.removeFirst()
                if (!visited.add(pos)) continue
                if (!isAnyScaffold(world.getBlockState(pos).block)) continue

                result.add(pos)
                for (direction in net.minecraft.util.math.Direction.values()) {
                    val next = pos.offset(direction)
                    if (!visited.contains(next) && isAnyScaffold(world.getBlockState(next).block)) {
                        queue.addLast(next)
                    }
                }
            }
            return result
        }

        /**
         * 以点击面的外法向为轴，在半径 1–3 的球与半球交集内取候选格，并做随机镂空形成不规则边缘。
         */
        private fun buildMultiSprayPositions(world: World, hit: BlockHitResult): List<BlockPos> {
            val side = hit.side
            val normal = side.vector
            val center = foamSprayAnchorPos(world, hit)
            val r = world.random.nextInt(3) + 1
            val rSq = r * r
            val candidates = ArrayList<BlockPos>(64)
            for (dx in -r..r) {
                for (dy in -r..r) {
                    for (dz in -r..r) {
                        val d2 = dx * dx + dy * dy + dz * dz
                        if (d2 > rSq) continue
                        val dot = dx * normal.x + dy * normal.y + dz * normal.z
                        if (dot < 0) continue
                        // 不规则：外缘随机剔除
                        if (d2 >= rSq - 2 * r + 1 && world.random.nextFloat() < 0.28f) continue
                        candidates.add(BlockPos(center.x + dx, center.y + dy, center.z + dz))
                    }
                }
            }
            for (i in candidates.lastIndex downTo 1) {
                val j = world.random.nextInt(i + 1)
                val t = candidates[i]
                candidates[i] = candidates[j]
                candidates[j] = t
            }
            return candidates
        }

        private const val SCAFFOLD_MULTI_MAX_BLOCKS = 256

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val iron = IronCasing::class.instance()
            val emptyCell = EmptyCell::class.instance()
            if (iron != Items.AIR && emptyCell != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, FoamSprayerItem::class.instance(), 1)
                    .pattern("I  ")
                    .pattern(" I ")
                    .pattern(" FI")
                    .input('I', iron)
                    .input('F', emptyCell)
                    .criterion(hasItem(iron), conditionsFromItem(iron))
                    .offerTo(exporter, FoamSprayerItem::class.id())
            }
        }
    }
}

internal class FoamSprayerFluidStorage(
    private val ctx: ContainerItemContext
) : Storage<FluidVariant> {

    override fun supportsInsertion(): Boolean = true

    override fun supportsExtraction(): Boolean = true

    override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (ctx.itemVariant.item !is FoamSprayerItem) return 0L
        val fluid = resource.fluid
        if (!isConstructionFoamFluid(fluid)) return 0L
        val stack = ctx.itemVariant.toStack(1)
        val current = FoamSprayerItem.getFluidAmount(stack)
        val space = FoamSprayerItem.CAPACITY_DROPLETS - current
        if (space <= 0L) return 0L
        val inserted = minOf(maxAmount, space)
        if (inserted <= 0L) return 0L
        FoamSprayerItem.setFluidAmount(stack, current + inserted)
        return if (ctx.exchange(ItemVariant.of(stack), 1, transaction) == 1L) inserted else 0L
    }

    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (ctx.itemVariant.item !is FoamSprayerItem) return 0L
        val stack = ctx.itemVariant.toStack(1)
        val current = FoamSprayerItem.getFluidAmount(stack)
        if (current <= 0L) return 0L
        if (!resource.isBlank && !isConstructionFoamFluid(resource.fluid)) return 0L
        val extracted = minOf(maxAmount, current)
        if (extracted <= 0L) return 0L
        FoamSprayerItem.setFluidAmount(stack, current - extracted)
        return if (ctx.exchange(ItemVariant.of(stack), 1, transaction) == 1L) extracted else 0L
    }

    override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
        if (ctx.itemVariant.item !is FoamSprayerItem) {
            return mutableListOf<StorageView<FluidVariant>>().iterator()
        }
        val stack = ctx.itemVariant.toStack(1)
        val amt = FoamSprayerItem.getFluidAmount(stack)
        if (amt <= 0L) {
            return mutableListOf<StorageView<FluidVariant>>().iterator()
        }
        val variant = FluidVariant.of(ModFluids.CONSTRUCTION_FOAM_STILL)
        val view = object : StorageView<FluidVariant> {
            override fun getResource(): FluidVariant = variant
            override fun getAmount(): Long = amt
            override fun getCapacity(): Long = FoamSprayerItem.CAPACITY_DROPLETS
            override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                this@FoamSprayerFluidStorage.extract(resource, maxAmount, transaction)
            override fun isResourceBlank(): Boolean = false
        }
        return mutableListOf(view).iterator() as MutableIterator<StorageView<FluidVariant>>
    }
}

private fun isConstructionFoamFluid(fluid: net.minecraft.fluid.Fluid): Boolean =
    fluid == ModFluids.CONSTRUCTION_FOAM_STILL || fluid == ModFluids.CONSTRUCTION_FOAM_FLOWING
