package ic2_120.content.block.storage

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.compat.orCreateNbt

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

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
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.ItemTags
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
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
abstract class TankBlock(settings: BlockBehaviour.Properties) : BaseEntityBlock(settings) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return TankBlockEntity(pos, state)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    @Deprecated("Override without InteractionHand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS

        val be = world.getBlockEntity(pos) as? TankBlockEntity ?: return InteractionResult.PASS
        val storage = FluidStorage.SIDED.find(world, pos, hit.direction)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, player.usedItemHand)) {
            return InteractionResult.SUCCESS
        }

        player.openMenu(be)
        return InteractionResult.SUCCESS
    }

    /**
     * 仅在储罐内有有效流体数据时写入 [BlockEntityTag]。
     * 空罐不写 `FluidAmount: 0`，否则与无 NBT 的合成品无法堆叠。
     * [appendTooltip] 在无标签时按 0 / 空流体解读，行为一致。
     */
    private fun applyTankBlockEntityNbt(stack: ItemStack, be: TankBlockEntity) {
        if (be.fluidAmount <= 0L && be.fluidVariant.isBlank) return
        val nbt = CompoundTag()
        nbt.putLong(NBT_FLUID_AMOUNT, be.fluidAmount)
        if (!be.fluidVariant.isBlank) {
            nbt.put(NBT_FLUID_VARIANT, encodeFluidVariant(be.fluidVariant))
        }
        stack.orCreateNbt = stack.orCreateNbt.apply { put("BlockEntityTag", nbt) }
    }

    @Deprecated("override deprecated method", level = DeprecationLevel.WARNING)
    override fun onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.`is`(newState.block)) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is TankBlockEntity && (moved || blockEntity.shouldDropOnBreak)) {
                val itemStack = ItemStack(this.asItem())
                applyTankBlockEntityNbt(itemStack, blockEntity)
                val itemEntity = net.minecraft.world.entity.item.ItemEntity(
                    world,
                    pos.x.toDouble() + 0.5,
                    pos.y.toDouble() + 0.5,
                    pos.z.toDouble() + 0.5,
                    itemStack
                )
                itemEntity.setDefaultPickUpDelay()
                world.addFreshEntity(itemEntity)
            }
        }
        @Suppress("DEPRECATION")
        super.onRemove(state, world, pos, newState, moved)
    }

    @Deprecated("override deprecated member", level = DeprecationLevel.WARNING)
    override fun spawnAfterBreak(state: BlockState, world: ServerLevel, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        // 不调用 super，防止默认掉落行为
    }

    override fun playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos)
            if (be is TankBlockEntity) {
                val holdingWrench = ic2_120.content.WrenchHandler.isWrench(player.mainHandItem)
                if (player.abilities.instabuild) {
                    be.retainFluidPercent(0.0)
                    be.shouldDropOnBreak = false // 创造模式：不掉落
                } else if (holdingWrench || player.hasCorrectToolForDrops(state)) {
                    be.retainFluidPercent(1.0)  // 扳手/正确工具：保留流体并掉落
                } else {
                    be.retainFluidPercent(0.0)
                    be.shouldDropOnBreak = false // 错误工具：不掉落，直接销毁
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player)
    }

    override fun getCloneItemStack(world: net.minecraft.world.level.LevelReader, pos: BlockPos, state: BlockState): ItemStack {
        val itemStack = super.getCloneItemStack(world, pos, state)
        val blockEntity = (world as? Level)?.getBlockEntity(pos)
        if (blockEntity is TankBlockEntity) {
            applyTankBlockEntityNbt(itemStack, blockEntity)
        }
        return itemStack
    }

    @Deprecated("Override without BlockState param", level = DeprecationLevel.WARNING)
    override fun hasAnalogOutputSignal(state: BlockState): Boolean = true

    @Deprecated("Override without BlockState param", level = DeprecationLevel.WARNING)
    override fun getAnalogOutputSignal(state: BlockState, world: net.minecraft.world.level.Level, pos: BlockPos): Int {
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
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        val blockItem = stack.item as? BlockItem ?: return
        val block = blockItem.block
        if (block !is TankBlock) return

        val capMb = fluidCapacityMbForBlock(block)
        // TODO(1.21.1): 原 yarn getSubNbt("BlockEntityTag")，组件时代暂以 CUSTOM_DATA NBT 桥接
        val beTag = stack.orCreateNbt.getCompound("BlockEntityTag")
        val amountRaw = beTag.getLong(NBT_FLUID_AMOUNT)
        val variantTag = beTag.getCompound(NBT_FLUID_VARIANT)
        val variant = decodeFluidVariant(variantTag)
        val amountMb =
            amountRaw.toInt().coerceIn(0, capMb)

        if (variant.isBlank || amountRaw <= 0L) {
            tooltip.add(Component.literal("流体: 无").withStyle(ChatFormatting.GRAY))
        } else {
            tooltip.add(
                Component.literal("流体: ").append(FluidVariantAttributes.getName(variant)).withStyle(ChatFormatting.GRAY)
            )
        }
        tooltip.add(Component.literal("储量: $amountMb / $capMb mB").withStyle(ChatFormatting.GRAY))
    }

    companion object {

        // fabric 1.21.1：FluidVariant.fromNbt/toNbt 已移除，用 CODEC + NbtOps 桥接
        fun encodeFluidVariant(variant: net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant): net.minecraft.nbt.CompoundTag {
            val tag = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.CODEC
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, variant)
                .getOrThrow()
            return if (tag is net.minecraft.nbt.CompoundTag) tag else net.minecraft.nbt.CompoundTag()
        }

        fun decodeFluidVariant(tag: net.minecraft.nbt.CompoundTag): net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant {
            if (tag.isEmpty) return net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank()
            return net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag)
                .result()
                .orElse(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank())
        }

        private const val NBT_FLUID_AMOUNT = "FluidAmount"
        private const val NBT_FLUID_VARIANT = "FluidVariant"

        /** 与 [TankBlockEntity] 对应的容量（mB），用于 tooltip */
        private fun fluidCapacityMbForBlock(block: net.minecraft.world.level.block.Block): Int {
            val path = BuiltInRegistries.BLOCK.getKey(block)!!.path
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
class BronzeTankBlock : TankBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { BronzeTankBlock() }
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = BronzePlate::class.instance()
            val cell = EmptyCell::class.instance()
            if (plate != Items.AIR && cell != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzeTankBlock::class.item(), 1)
                    .pattern("PFP")
                    .pattern("F F")
                    .pattern("PFP")
                    .define('P', Ingredient.of(ModTags.Compat.Items.PLATES_BRONZE))
                    .define('F', cell)
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, BronzeTankBlock::class.id())
            }
        }
    }
}

/**
 * 铁储罐 - 32 桶容量 (32000 mB)
 */
@ModBlock(name = "iron_tank", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "tank", generateBlockLootTable = false)
class IronTankBlock : TankBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { IronTankBlock() }
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = IronPlate::class.instance()
            val cell = EmptyCell::class.instance()
            if (plate != Items.AIR && cell != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IronTankBlock::class.item(), 1)
                    .pattern("PFP")
                    .pattern("F F")
                    .pattern("PFP")
                    .define('P', Ingredient.of(ModTags.Compat.Items.PLATES_IRON))
                    .define('F', cell)
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, IronTankBlock::class.id())
            }
        }
    }
}

/**
 * 钢制储罐 - 128 桶容量 (128000 mB)
 */
@ModBlock(name = "steel_tank", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "tank", generateBlockLootTable = false)
class SteelTankBlock : TankBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).strength(6.0f, 7.0f)) {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { SteelTankBlock() }
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = SteelPlate::class.instance()
            val cell = EmptyCell::class.instance()
            if (plate != Items.AIR && cell != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteelTankBlock::class.item(), 1)
                    .pattern("PFP")
                    .pattern("F F")
                    .pattern("PFP")
                    .define('P', Ingredient.of(ModTags.Compat.Items.PLATES_STEEL))
                    .define('F', cell)
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, SteelTankBlock::class.id())
            }
        }
    }
}

/**
 * 铱储罐 - 1024 桶容量 (1024000 mB)
 */
@ModBlock(name = "iridium_tank", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "tank", generateBlockLootTable = false)
class IridiumTankBlock : TankBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).strength(8.0f, 10.0f)) {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { IridiumTankBlock() }
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = IridiumPlate::class.instance()
            val cell = EmptyCell::class.instance()
            if (plate != Items.AIR && cell != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IridiumTankBlock::class.item(), 1)
                    .pattern("PFP")
                    .pattern("F F")
                    .pattern("PFP")
                    .define('P', plate)
                    .define('F', cell)
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, IridiumTankBlock::class.id())
            }
        }
    }
}
