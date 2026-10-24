package ic2_120.content.block.storage

import ic2_120.content.item.BronzePlate
import ic2_120.content.item.EmptyCell
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.IronPlate
import ic2_120.content.item.SteelPlate
import ic2_120.content.recipes.ModTags
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
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes
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
import net.minecraft.recipe.Ingredient
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

    @Deprecated("Override without Hand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
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
        val storage = FluidStorage.SIDED.find(world, pos, hit.side)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
            return ActionResult.SUCCESS
        }

        player.openHandledScreen(be)
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

    @Deprecated("override deprecated method", level = DeprecationLevel.WARNING)
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block)) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is TankBlockEntity && (moved || blockEntity.shouldDropOnBreak)) {
                val itemStack = ItemStack(this.asItem())
                applyTankBlockEntityNbt(itemStack, blockEntity)
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
        @Suppress("DEPRECATION")
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    @Deprecated("override deprecated member", level = DeprecationLevel.WARNING)
    override fun onStacksDropped(state: BlockState, world: ServerWorld, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        // 不调用 super，防止默认掉落行为
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos)
            if (be is TankBlockEntity) {
                val holdingWrench = ic2_120.content.WrenchHandler.isWrench(player.mainHandStack)
                if (player.abilities.creativeMode) {
                    be.retainFluidPercent(0.0)
                    be.shouldDropOnBreak = false // 创造模式：不掉落
                } else if (holdingWrench || player.canHarvest(state)) {
                    be.retainFluidPercent(1.0)  // 扳手/正确工具：保留流体并掉落
                } else {
                    be.retainFluidPercent(0.0)
                    be.shouldDropOnBreak = false // 错误工具：不掉落，直接销毁
                }
            }
        }
        super.onBreak(world, pos, state, player)
    }

    override fun getPickStack(world: net.minecraft.world.BlockView, pos: BlockPos, state: BlockState): ItemStack {
        val itemStack = super.getPickStack(world, pos, state)
        val blockEntity = world.getBlockEntity(pos)
        if (blockEntity is TankBlockEntity) {
            applyTankBlockEntityNbt(itemStack, blockEntity)
        }
        return itemStack
    }

    @Deprecated("Override without BlockState param", level = DeprecationLevel.WARNING)
    override fun hasComparatorOutput(state: BlockState): Boolean = true

    @Deprecated("Override without BlockState param", level = DeprecationLevel.WARNING)
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
            amountRaw.toInt().coerceIn(0, capMb)

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

        /** 与 [TankBlockEntity] 对应的容量（mB），用于 tooltip */
        private fun fluidCapacityMbForBlock(block: net.minecraft.block.Block): Int {
            val path = Registries.BLOCK.getId(block).path
            return when (path) {
                "steel_tank" -> TankBlockEntity.STEEL_CAPACITY_MB
                "iridium_tank" -> TankBlockEntity.IRIDIUM_CAPACITY_MB
                else -> TankBlockEntity.BRONZE_IRON_CAPACITY_MB
            }
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
                    .input('P', Ingredient.fromTag(ModTags.Compat.Items.PLATES_BRONZE))
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
                    .input('P', Ingredient.fromTag(ModTags.Compat.Items.PLATES_IRON))
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
                    .input('P', Ingredient.fromTag(ModTags.Compat.Items.PLATES_STEEL))
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
