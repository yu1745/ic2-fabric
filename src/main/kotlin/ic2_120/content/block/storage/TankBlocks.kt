package ic2_120.content.block.storage

import ic2_120.content.item.BronzePlate
import ic2_120.content.item.EmptyCell
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.IronPlate
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.SteelPlate
import ic2_120.content.item.fluidToFilledCellStack
import ic2_120.content.item.getFluidCellVariant
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World
import java.util.function.Consumer

/**
 * 储罐基类
 *
 * 用于存储流体的方块，支持：
 * - 流体管道自动输入输出
 * - 右键用桶或流体单元手动交互
 * - 扳手拆卸保留流体
 * - 不同材质容量不同
 */
abstract class TankBlock(settings: AbstractBlock.Settings) : BlockWithEntity(settings) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return TankBlockEntity(pos, state)
    }

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS

        val be = world.getBlockEntity(pos) as? TankBlockEntity ?: return ActionResult.PASS
        val held = player.getStackInHand(hand)

        if (held.isEmpty) return ActionResult.PASS

        val heldItem = held.item
        val modId = "ic2_120"
        val emptyCell = Registries.ITEM.get(Identifier(modId, "empty_cell"))
        val fluidCellItem = Registries.ITEM.get(Identifier(modId, "fluid_cell"))

        // 空单元（empty_cell / empty fluid_cell）：取出 1 桶流体
        if (heldItem === emptyCell || (heldItem === fluidCellItem && held.getFluidCellVariant() == null)) {
            return extractFromTank(world, be, player, hand, held)
        }

        // 满单元（filled fluid_cell 或 ModFluidCell 如 water_cell）：插入流体到储罐
        if (heldItem === fluidCellItem || heldItem is ModFluidCell) {
            return insertIntoTank(world, be, player, hand, held)
        }

        // 桶：取出或放入流体
        return interactWithBucket(world, pos, be, player, hand, held)
    }

    /**
     * 空单元交互：从储罐取出 1 桶流体，获得对应的满单元
     */
    private fun extractFromTank(
        world: World,
        be: TankBlockEntity,
        player: PlayerEntity,
        hand: Hand,
        held: ItemStack
    ): ActionResult {
        val current = be.fluidVariant
        if (current.isBlank || be.fluidAmount < FluidConstants.BUCKET) {
            return ActionResult.PASS
        }

        val fluid = current.fluid
        Transaction.openOuter().use { tx ->
            val extracted = be.fluidTank.extract(current, FluidConstants.BUCKET, tx)
            if (extracted < FluidConstants.BUCKET) {
                tx.abort()
                return ActionResult.PASS
            }

            val filled = fluidToFilledCellStack(fluid)

            if (!player.abilities.creativeMode) {
                held.decrement(1)
                if (held.isEmpty) {
                    player.setStackInHand(hand, filled)
                } else if (!player.inventory.insertStack(filled)) {
                    player.dropItem(filled, false)
                    player.setStackInHand(hand, held)
                }
            }
            tx.commit()
            be.markDirty()
        }
        return ActionResult.SUCCESS
    }

    /**
     * 满单元（filled fluid_cell / ModFluidCell）交互：将单元内 1 桶流体推入储罐
     */
    private fun insertIntoTank(
        world: World,
        be: TankBlockEntity,
        player: PlayerEntity,
        hand: Hand,
        held: ItemStack
    ): ActionResult {
        // 获取单元内的流体：fluid_cell 用 NBT，ModFluidCell 用 getFluid()
        val cellFluidVariant = when (val item = held.item) {
            is ModFluidCell -> FluidVariant.of(item.getFluid())
            else -> held.getFluidCellVariant()
        }
        if (cellFluidVariant == null || cellFluidVariant.isBlank) {
            return ActionResult.PASS
        }

        val tankVariant = be.fluidVariant
        val tankAmount = be.fluidAmount
        val tankCapacity = be.getCapacity()

        // 已有不同流体，拒绝放入
        if (!tankVariant.isBlank && !tankVariant.equals(cellFluidVariant)) {
            return ActionResult.PASS
        }

        // 没有空间
        if (tankAmount >= tankCapacity) {
            return ActionResult.PASS
        }

        // 只能插入 1 桶
        val canAccept = (tankCapacity - tankAmount).coerceAtMost(FluidConstants.BUCKET)

        Transaction.openOuter().use { tx ->
            val inserted = be.fluidTank.insert(cellFluidVariant, canAccept, tx)
            if (inserted <= 0) {
                tx.abort()
                return ActionResult.PASS
            }

            // 消耗 1 桶流体：变为空单元
            if (!player.abilities.creativeMode) {
                held.decrement(1)
                val emptyStack = ItemStack(Registries.ITEM.get(Identifier("ic2_120", "empty_cell")))
                if (held.isEmpty) {
                    player.setStackInHand(hand, emptyStack)
                } else if (!player.inventory.insertStack(emptyStack)) {
                    player.dropItem(emptyStack, false)
                    player.setStackInHand(hand, held)
                }
            }
            tx.commit()
            be.markDirty()
        }
        return ActionResult.SUCCESS
    }

    /**
     * 桶交互
     * - 空桶（bucket）：有流体则取出
     * - 满桶：有空间且同种流体则放入，已有不同流体或无空间则 PASS
     */
    private fun interactWithBucket(
        world: World,
        pos: BlockPos,
        be: TankBlockEntity,
        player: PlayerEntity,
        hand: Hand,
        held: ItemStack
    ): ActionResult {
        val bucketItem = held.item

        // 空桶：取出流体
        if (bucketItem === Items.BUCKET) {
            return drainTankIntoBucket(world, pos, be, player, hand, held)
        }

        // 满桶：放入流体
        return fillTankFromBucket(world, pos, be, player, hand, held)
    }

    /**
     * 空桶取出 1 桶流体
     */
    private fun drainTankIntoBucket(
        world: World,
        pos: BlockPos,
        be: TankBlockEntity,
        player: PlayerEntity,
        hand: Hand,
        held: ItemStack
    ): ActionResult {
        if (be.fluidAmount < FluidConstants.BUCKET) {
            return ActionResult.PASS
        }

        val current = be.fluidVariant
        Transaction.openOuter().use { tx ->
            val extracted = be.fluidTank.extract(current, FluidConstants.BUCKET, tx)
            if (extracted < FluidConstants.BUCKET) {
                tx.abort()
                return ActionResult.PASS
            }

            if (!player.abilities.creativeMode) {
                held.decrement(1)
                val filledBucket = ItemStack(Items.BUCKET)
                if (held.isEmpty) {
                    player.setStackInHand(hand, filledBucket)
                } else if (!player.inventory.insertStack(filledBucket)) {
                    player.dropItem(filledBucket, false)
                    player.setStackInHand(hand, held)
                }
            }
            tx.commit()
            be.markDirty()
        }
        return ActionResult.SUCCESS
    }

    /**
     * 满桶放入储罐
     * - 已有流体：必须是同种流体且有空间
     * - 无流体：必须有空间
     */
    private fun fillTankFromBucket(
        world: World,
        pos: BlockPos,
        be: TankBlockEntity,
        player: PlayerEntity,
        hand: Hand,
        held: ItemStack
    ): ActionResult {
        // 通过 FluidStorage.ITEM 获取桶内流体
        val bucketStorage = try {
            FluidStorage.ITEM.find(held, null)
        } catch (_: NullPointerException) {
            null
        } ?: return ActionResult.PASS

        var bucketFluid: FluidVariant? = null
        var bucketAmount: Long = 0
        for (view in bucketStorage) {
            if (!view.isResourceBlank && view.amount >= FluidConstants.BUCKET) {
                bucketFluid = view.resource
                bucketAmount = view.amount
                break
            }
        }

        if (bucketFluid == null || bucketFluid.isBlank || bucketAmount < FluidConstants.BUCKET) {
            return ActionResult.PASS
        }

        val tankVariant = be.fluidVariant
        val tankAmount = be.fluidAmount
        val tankCapacity = be.getCapacity()

        // 检查：已有不同流体，拒绝放入
        if (!tankVariant.isBlank && !tankVariant.equals(bucketFluid)) {
            return ActionResult.PASS
        }

        // 检查：没有空间
        if (tankAmount >= tankCapacity) {
            return ActionResult.PASS
        }

        // 计算可放入量
        val canAccept = (tankCapacity - tankAmount).coerceAtMost(FluidConstants.BUCKET)

        Transaction.openOuter().use { tx ->
            val inserted = be.fluidTank.insert(bucketFluid, canAccept, tx)
            if (inserted <= 0) {
                tx.abort()
                return ActionResult.PASS
            }

            // 消耗 1 桶流体：等效替换为空桶
            if (!player.abilities.creativeMode) {
                held.decrement(1)
                val emptyBucket = ItemStack(Items.BUCKET)
                if (held.isEmpty) {
                    player.setStackInHand(hand, emptyBucket)
                } else if (!player.inventory.insertStack(emptyBucket)) {
                    player.dropItem(emptyBucket, false)
                    player.setStackInHand(hand, held)
                }
            }
            tx.commit()
            be.markDirty()
        }
        return ActionResult.SUCCESS
    }

    /**
     * 仅在储罐内有有效流体数据时写入 [BlockEntityTag]。
     * 空罐不写 `FluidAmount: 0`，否则与无 NBT 的合成品无法堆叠。
     * [appendTooltip] 在无标签时按 0 / 空流体解读，行为一致。
     */
    private fun applyTankBlockEntityNbt(stack: ItemStack, be: TankBlockEntity) {
        if (be.fluidAmount <= 0L && be.fluidVariant.isBlank) return
        val nbt = NbtCompound()
        nbt.putLong(NBT_FLUID_AMOUNT, be.fluidAmount)
        if (!be.fluidVariant.isBlank) {
            nbt.put(NBT_FLUID_VARIANT, be.fluidVariant.toNbt())
        }
        stack.orCreateNbt.put("BlockEntityTag", nbt)
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block)) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is TankBlockEntity) {
                val itemStack = ItemStack(this.asItem())
                applyTankBlockEntityNbt(itemStack, blockEntity)
                if (!itemStack.isEmpty) {
                    val itemEntity = net.minecraft.entity.ItemEntity(
                        world,
                        pos.x.toDouble() + 0.5,
                        pos.y.toDouble() + 0.5,
                        pos.z.toDouble() + 0.5,
                        itemStack
                    )
                    itemEntity.setToDefaultPickupDelay()
                    world.spawnEntity(itemEntity)
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    override fun onStacksDropped(state: BlockState, world: ServerWorld, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        // 不调用 super，防止默认掉落行为
    }

    override fun getPickStack(world: net.minecraft.world.BlockView, pos: BlockPos, state: BlockState): ItemStack {
        val itemStack = super.getPickStack(world, pos, state)
        val blockEntity = world.getBlockEntity(pos)
        if (blockEntity is TankBlockEntity) {
            applyTankBlockEntityNbt(itemStack, blockEntity)
        }
        return itemStack
    }

    override fun hasComparatorOutput(state: BlockState): Boolean = true

    override fun getComparatorOutput(state: BlockState, world: net.minecraft.world.World, pos: BlockPos): Int {
        val blockEntity = world.getBlockEntity(pos)
        if (blockEntity is TankBlockEntity) {
            return blockEntity.comparatorOutput
        }
        return 0
    }

    /**
     * 手持物品提示：内部流体种类与储量（mB），与 [TankBlockEntity] 容量一致。
     */
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(
        stack: ItemStack,
        world: BlockView?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        val blockItem = stack.item as? BlockItem ?: return
        val block = blockItem.block
        if (block !is TankBlock) return

        val capMb = fluidCapacityMbForBlock(block)
        val beTag = stack.getSubNbt("BlockEntityTag")
        val amountRaw = beTag?.getLong(NBT_FLUID_AMOUNT) ?: 0L
        val variantTag = beTag?.getCompound(NBT_FLUID_VARIANT)
        val variant =
            if (variantTag == null || variantTag.isEmpty) FluidVariant.blank()
            else FluidVariant.fromNbt(variantTag)
        val amountMb =
            (amountRaw * 1000L / FluidConstants.BUCKET).toInt().coerceIn(0, capMb)

        if (variant.isBlank || amountRaw <= 0L) {
            tooltip.add(Text.literal("流体: 无").formatted(Formatting.GRAY))
        } else {
            tooltip.add(
                Text.literal("流体: ").append(FluidVariantAttributes.getName(variant)).formatted(Formatting.GRAY)
            )
        }
        tooltip.add(Text.literal("储量: $amountMb / $capMb mB").formatted(Formatting.GRAY))
    }

    companion object {
        private const val NBT_FLUID_AMOUNT = "FluidAmount"
        private const val NBT_FLUID_VARIANT = "FluidVariant"

        /** 与 [TankBlockEntity.getCapacity] 对应的容量（mB），用于 tooltip */
        private fun fluidCapacityMbForBlock(block: net.minecraft.block.Block): Int {
            val path = Registries.BLOCK.getId(block).path
            val buckets = when (path) {
                "steel_tank" -> 128
                "iridium_tank" -> 1024
                else -> 32
            }
            return buckets * 1000
        }
    }
}

// ========== 储罐方块 ==========

/**
 * 青铜储罐 - 32 桶容量 (32000 mB)
 */
@ModBlock(name = "bronze_tank", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "tank", generateBlockLootTable = false)
class BronzeTankBlock : TankBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val plate = BronzePlate::class.instance()
            val cell = EmptyCell::class.instance()
            if (plate != Items.AIR && cell != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeTankBlock::class.item(), 1)
                    .pattern("PFP")
                    .pattern("F F")
                    .pattern("PFP")
                    .input('P', plate)
                    .input('F', cell)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, BronzeTankBlock::class.id())
            }
        }
    }
}

/**
 * 铁储罐 - 32 桶容量 (32000 mB)
 */
@ModBlock(name = "iron_tank", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "tank", generateBlockLootTable = false)
class IronTankBlock : TankBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val plate = IronPlate::class.instance()
            val cell = EmptyCell::class.instance()
            if (plate != Items.AIR && cell != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronTankBlock::class.item(), 1)
                    .pattern("PFP")
                    .pattern("F F")
                    .pattern("PFP")
                    .input('P', plate)
                    .input('F', cell)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, IronTankBlock::class.id())
            }
        }
    }
}

/**
 * 钢制储罐 - 128 桶容量 (128000 mB)
 */
@ModBlock(name = "steel_tank", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "tank", generateBlockLootTable = false)
class SteelTankBlock : TankBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.IRON_BLOCK).strength(6.0f, 7.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val plate = SteelPlate::class.instance()
            val cell = EmptyCell::class.instance()
            if (plate != Items.AIR && cell != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SteelTankBlock::class.item(), 1)
                    .pattern("PFP")
                    .pattern("F F")
                    .pattern("PFP")
                    .input('P', plate)
                    .input('F', cell)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, SteelTankBlock::class.id())
            }
        }
    }
}

/**
 * 铱储罐 - 1024 桶容量 (1024000 mB)
 */
@ModBlock(name = "iridium_tank", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "tank", generateBlockLootTable = false)
class IridiumTankBlock : TankBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.IRON_BLOCK).strength(8.0f, 10.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val plate = IridiumPlate::class.instance()
            val cell = EmptyCell::class.instance()
            if (plate != Items.AIR && cell != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumTankBlock::class.item(), 1)
                    .pattern("PFP")
                    .pattern("F F")
                    .pattern("PFP")
                    .input('P', plate)
                    .input('F', cell)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, IridiumTankBlock::class.id())
            }
        }
    }
}
