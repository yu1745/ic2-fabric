package ic2_120.content.block.storage

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.item.BronzeCasing
import ic2_120.content.item.BronzePlate
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.IronCasing
import ic2_120.content.item.IronPlate
import ic2_120.content.item.SteelCasing
import ic2_120.content.item.SteelPlate
import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.level.BlockGetter
import java.util.function.Consumer

/**
 * 储物箱基类
 *
 * 类似潜影盒，破坏时保留物品内容。
 */
abstract class StorageBoxBlock(settings: BlockBehaviour.Properties) : BaseEntityBlock(settings) {

    /**
     * 储物箱物品禁止堆叠：内容物保存在 BlockEntityTag NBT 中，堆叠会丢失/复制内容。
     * 所有储物箱子类通过父类链继承该设置。
     */
    class StorageBoxBlockItem(block: Block, settings: Item.Properties) : BlockItem(block, settings.maxStackSize(1))

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return StorageBoxBlockEntity(pos, state)
    }

    /**
     * 确保方块使用模型渲染（而不是实体渲染）
     */
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    /**
     * 添加物品提示信息
     */
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(
        stack: ItemStack,
        world: BlockGetter?,
        tooltip: MutableList<Component>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, context, tooltip, type)

        val blockEntityTag = stack.getSubNbt("BlockEntityTag")
        if (blockEntityTag == null || !blockEntityTag.contains("Container")) {
            tooltip.add(Component.literal("物品数量: 0").withStyle(ChatFormatting.GRAY))
            return
        }

        val inventoryNbt = blockEntityTag.getCompound("Container")
        if (inventoryNbt.isEmpty) {
            tooltip.add(Component.literal("物品数量: 0").withStyle(ChatFormatting.GRAY))
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
            tooltip.add(Component.literal("物品数量: 0").withStyle(ChatFormatting.GRAY))
        } else {
            tooltip.add(Component.literal("物品数量: $totalItems ($filledSlots 格)").withStyle(ChatFormatting.GRAY))
        }
    }

    /**
     * 玩家右键点击打开 GUI
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is StorageBoxBlockEntity) {
                player.openMenu(blockEntity)
            }
        }
        return InteractionResult.SUCCESS
    }

    /**
     * 方块被替换/破坏时掉落带有物品栏的物品（类似潜影盒）
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.`is`(newState.block)) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is StorageBoxBlockEntity && (moved || blockEntity.shouldDropOnBreak)) {
                // 创建带有 BlockEntity NBT 的物品
                val itemStack = ItemStack(this.asItem())
                val nbt = blockEntity.saveWithFullMetadata()
                if (!nbt.isEmpty) {
                    itemStack.orCreateNbt.put("BlockEntityTag", nbt)
                }

                // 掉落物品
                if (!itemStack.isEmpty) {
                    val itemEntity = net.minecraft.world.entity.item.ItemEntity(
                        world,
                        pos.x.toDouble() + 0.5,
                        pos.y.toDouble() + 0.5,
                        pos.z.toDouble() + 0.5,
                        itemStack
                    )
                    itemEntity.setToDefaultPickupDelay()
                    world.addFreshEntity(itemEntity)
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    override fun onBreak(world: Level, pos: BlockPos, state: BlockState, player: Player) {
        if (!world.isClient && player.abilities.creativeMode) {
            (world.getBlockEntity(pos) as? StorageBoxBlockEntity)?.shouldDropOnBreak = false
        }
        super.onBreak(world, pos, state, player)
    }

    override fun onStacksDropped(state: BlockState, world: ServerLevel, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        // 不调用 super，防止默认掉落行为
    }

    /**
     * 获取掉落的物品（包含 BlockEntity NBT 数据）- 用于创造模式拾取
     */
    override fun getPickStack(world: net.minecraft.world.level.BlockGetter, pos: BlockPos, state: BlockState): ItemStack {
        val itemStack = super.getPickStack(world, pos, state)
        val blockEntity = world.getBlockEntity(pos)

        if (blockEntity is StorageBoxBlockEntity) {
            val nbt = blockEntity.saveWithFullMetadata()
            if (!nbt.isEmpty) {
                itemStack.orCreateNbt.put("BlockEntityTag", nbt)
            }
        }

        return itemStack
    }

    override fun getComparatorOutput(state: BlockState, world: net.minecraft.world.level.Level, pos: BlockPos): Int {
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
class WoodenStorageBoxBlock : StorageBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.5f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val log = Items.OAK_LOG
            val plank = Items.OAK_PLANKS
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, WoodenStorageBoxBlock::class.item(), 1)
                .pattern("LPL")
                .pattern("P P")
                .pattern("LPL")
                .define('L', log)
                .define('P', plank)
                .unlockedBy(getHasName(plank), has(plank))
                .save(exporter, WoodenStorageBoxBlock::class.id())
        }
    }
}

/**
 * 青铜储物箱 - 45 格容量
 */
@ModBlock(name = "bronze_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class BronzeStorageBoxBlock : StorageBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val plate = BronzePlate::class.instance()
            val casing = BronzeCasing::class.instance()
            if (plate != Items.AIR && casing != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzeStorageBoxBlock::class.item(), 1)
                    .pattern("PCP")
                    .pattern("C C")
                    .pattern("PCP")
                    .define('P', Ingredient.of(ModTags.Compat.Items.PLATES_BRONZE))
                    .define('C', casing)
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, BronzeStorageBoxBlock::class.id())
            }
        }
    }
}

/**
 * 铁质储物箱 - 45 格容量
 */
@ModBlock(name = "iron_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class IronStorageBoxBlock : StorageBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val plate = IronPlate::class.instance()
            val casing = IronCasing::class.instance()
            if (plate != Items.AIR && casing != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IronStorageBoxBlock::class.item(), 1)
                    .pattern("PCP")
                    .pattern("C C")
                    .pattern("PCP")
                    .define('P', Ingredient.of(ModTags.Compat.Items.PLATES_IRON))
                    .define('C', casing)
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, IronStorageBoxBlock::class.id())
            }
        }
    }
}

/**
 * 钢制储物箱 - 63 格容量
 */
@ModBlock(name = "steel_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class SteelStorageBoxBlock : StorageBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val plate = SteelPlate::class.instance()
            val casing = SteelCasing::class.instance()
            if (plate != Items.AIR && casing != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteelStorageBoxBlock::class.item(), 1)
                    .pattern("PCP")
                    .pattern("C C")
                    .pattern("PCP")
                    .define('P', Ingredient.of(ModTags.Compat.Items.PLATES_STEEL))
                    .define('C', casing)
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, SteelStorageBoxBlock::class.id())
            }
        }
    }
}

/**
 * 铱储物箱 - 126 格容量
 */
@ModBlock(name = "iridium_storage_box", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "storage", generateBlockLootTable = false)
class IridiumStorageBoxBlock : StorageBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(6.0f, 8.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val iridium = IridiumPlate::class.instance()
            val steel = SteelPlate::class.instance()
            if (iridium != Items.AIR && steel != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IridiumStorageBoxBlock::class.item(), 1)
                    .pattern("ISI")
                    .pattern("S S")
                    .pattern("ISI")
                    .define('I', iridium)
                    .define('S', steel)
                    .unlockedBy(getHasName(iridium), has(iridium))
                    .save(exporter, IridiumStorageBoxBlock::class.id())
            }
        }
    }
}
