package ic2_120.content.block.storage

import ic2_120.content.item.BronzeCasing
import net.minecraft.item.Item
import net.minecraft.item.tooltip.TooltipType
import ic2_120.content.item.BronzePlate
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.IronCasing
import ic2_120.content.item.IronPlate
import ic2_120.content.item.SteelCasing
import ic2_120.content.item.SteelPlate
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import com.mojang.serialization.MapCodec
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.BlockRenderType
import net.minecraft.block.Blocks
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.BlockView
import net.minecraft.world.WorldView
import ic2_120.getCustomData
import ic2_120.getOrCreateCustomData

/**
 * 储物箱基类
 *
 * 类似潜影盒，破坏时保留物品内容。
 */
abstract class StorageBoxBlock(settings: AbstractBlock.Settings) : BlockWithEntity(settings) {
    companion object {
        val STORAGE_BOX_CODEC: MapCodec<StorageBoxBlock> = Block.createCodec { error("StorageBoxBlock cannot be deserialized from JSON") }
    }

    override fun getCodec(): MapCodec<out BlockWithEntity> = STORAGE_BOX_CODEC

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return StorageBoxBlockEntity(pos, state)
    }

    /**
     * 确保方块使用模型渲染（而不是实体渲染）
     */
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    /**
     * 添加物品提示信息
     */
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)

        val blockEntityTag = stack.getCustomData()?.getCompound("BlockEntityTag")
        if (blockEntityTag == null || !blockEntityTag.contains("Inventory")) {
            tooltip.add(Text.literal("物品数量: 0").formatted(Formatting.GRAY))
            return
        }

        val inventoryNbt = blockEntityTag.getCompound("Inventory")
        if (inventoryNbt.isEmpty) {
            tooltip.add(Text.literal("物品数量: 0").formatted(Formatting.GRAY))
            return
        }

        // 解析物品栏数据
        val items = inventoryNbt.getList("Items", 10)
        var totalItems = 0
        var filledSlots = 0

        for (i in 0 until items.size) {
            val itemNbt = items.getCompound(i)
            val count = itemNbt.getInt("Count")
            if (count > 0) {
                filledSlots++
                totalItems += count
            }
        }

        // 添加物品数量提示
        if (filledSlots == 0) {
            tooltip.add(Text.literal("物品数量: 0").formatted(Formatting.GRAY))
        } else {
            tooltip.add(Text.literal("物品数量: $totalItems ($filledSlots 格)").formatted(Formatting.GRAY))
        }
    }

    /**
     * 玩家右键点击打开 GUI
     */
    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is StorageBoxBlockEntity) {
                player.openHandledScreen(blockEntity)
            }
        }
        return ActionResult.SUCCESS
    }

    /**
     * 方块被替换/破坏时掉落带有物品栏的物品（类似潜影盒）
     */
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block)) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is StorageBoxBlockEntity) {
                // 创建带有 BlockEntity NBT 的物品
                val itemStack = ItemStack(this.asItem())
                val lookup = world.registryManager
                val nbt = blockEntity.createNbtWithIdentifyingData(lookup)
                if (!nbt.isEmpty) {
                    itemStack.getOrCreateCustomData().put("BlockEntityTag", nbt)
                }

                // 掉落物品
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

    /**
     * 获取掉落的物品（包含 BlockEntity NBT 数据）- 用于创造模式拾取
     */
    override fun getPickStack(world: WorldView, pos: BlockPos, state: BlockState): ItemStack {
        val itemStack = super.getPickStack(world, pos, state)
        val blockEntity = (world as BlockView).getBlockEntity(pos)

        if (blockEntity is StorageBoxBlockEntity) {
            val lookup = (world as World).registryManager
            val nbt = blockEntity.createNbtWithIdentifyingData(lookup)
            if (!nbt.isEmpty) {
                itemStack.getOrCreateCustomData().put("BlockEntityTag", nbt)
            }
        }

        return itemStack
    }

    override fun getComparatorOutput(state: BlockState, world: net.minecraft.world.World, pos: BlockPos): Int {
        // 类似箱子，根据物品栏填充度输出红石信号
        val blockEntity = world.getBlockEntity(pos)
        if (blockEntity is StorageBoxBlockEntity) {
            if (blockEntity.isEmpty()) return 0

            val inventory = blockEntity.getInventory()
            var filledSlots = 0

            for (stack in inventory) {
                if (!stack.isEmpty) {
                    filledSlots++
                }
            }

            // 计算填充度：0-15
            val filledRatio = filledSlots.toFloat() / inventory.size
            val strength = (filledRatio * 14).toInt() + if (filledSlots > 0) 1 else 0
            return strength.coerceIn(0, 15)
        }
        return 0
    }

    override fun hasComparatorOutput(state: BlockState): Boolean = true
}

// ========== 储物箱方块 ==========

/**
 * 木质储物箱 - 27 格容量
 */
@ModBlock(name = "wooden_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class WoodenStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val log = Items.OAK_LOG
            val plank = Items.OAK_PLANKS
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WoodenStorageBoxBlock::class.item(), 1)
                .pattern("LPL")
                .pattern("P P")
                .pattern("LPL")
                .input('L', log)
                .input('P', plank)
                .criterion(hasItem(plank), conditionsFromItem(plank))
                .offerTo(exporter, WoodenStorageBoxBlock::class.id())
        }
    }
}

/**
 * 青铜储物箱 - 45 格容量
 */
@ModBlock(name = "bronze_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class BronzeStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val plate = BronzePlate::class.instance()
            val casing = BronzeCasing::class.instance()
            if (plate != Items.AIR && casing != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeStorageBoxBlock::class.item(), 1)
                    .pattern("PCP")
                    .pattern("C C")
                    .pattern("PCP")
                    .input('P', plate)
                    .input('C', casing)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, BronzeStorageBoxBlock::class.id())
            }
        }
    }
}

/**
 * 铁质储物箱 - 45 格容量
 */
@ModBlock(name = "iron_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class IronStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val plate = IronPlate::class.instance()
            val casing = IronCasing::class.instance()
            if (plate != Items.AIR && casing != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronStorageBoxBlock::class.item(), 1)
                    .pattern("PCP")
                    .pattern("C C")
                    .pattern("PCP")
                    .input('P', plate)
                    .input('C', casing)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, IronStorageBoxBlock::class.id())
            }
        }
    }
}

/**
 * 钢制储物箱 - 63 格容量
 */
@ModBlock(name = "steel_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class SteelStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val plate = SteelPlate::class.instance()
            val casing = SteelCasing::class.instance()
            if (plate != Items.AIR && casing != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SteelStorageBoxBlock::class.item(), 1)
                    .pattern("PCP")
                    .pattern("C C")
                    .pattern("PCP")
                    .input('P', plate)
                    .input('C', casing)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, SteelStorageBoxBlock::class.id())
            }
        }
    }
}

/**
 * 铱储物箱 - 126 格容量
 */
@ModBlock(name = "iridium_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class IridiumStorageBoxBlock : StorageBoxBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(6.0f, 8.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val iridium = IridiumPlate::class.instance()
            val steel = SteelPlate::class.instance()
            if (iridium != Items.AIR && steel != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumStorageBoxBlock::class.item(), 1)
                    .pattern("ISI")
                    .pattern("S S")
                    .pattern("ISI")
                    .input('I', iridium)
                    .input('S', steel)
                    .criterion(hasItem(iridium), conditionsFromItem(iridium))
                    .offerTo(exporter, IridiumStorageBoxBlock::class.id())
            }
        }
    }
}
