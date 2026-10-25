package ic2_120.content.block

import ic2_120.compat.save

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.has

import ic2_120.compat.isClient
import ic2_120.compat.orCreateNbt

import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.CrystalMemory
import ic2_120.content.item.MiningLaserItem
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.item.TooltipFlag
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.Containers
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import java.util.function.Consumer

@ModBlock(name = "pattern_storage", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "uu")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class PatternStorageBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        PatternStorageBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = null

    override fun getMenuProvider(
        state: BlockState,
        world: Level,
        pos: BlockPos
    ): net.minecraft.world.MenuProvider? =
        world.getBlockEntity(pos) as? net.minecraft.world.MenuProvider

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { player.openMenu(it) }
        }
        return InteractionResult.SUCCESS
    }

    class PatternStorageBlockItem(block: net.minecraft.world.level.block.Block, properties: Item.Properties) : net.minecraft.world.item.BlockItem(block, properties) {
        @Environment(EnvType.CLIENT)
        override fun appendHoverText(
            stack: ItemStack,
            context: Item.TooltipContext,
            display: net.minecraft.world.item.component.TooltipDisplay, consumer: java.util.function.Consumer<Component>,
            type: TooltipFlag
        ) {
            val tooltip = ic2_120.compat.TooltipSinkList(consumer)
            super.appendHoverText(stack, context, display, consumer, type)
            // TODO(1.21.1): 原 yarn getSubNbt("BlockEntityTag")，组件时代暂以 CUSTOM_DATA NBT 桥接
            val blockEntityTag = stack.orCreateNbt.getCompoundOrEmpty("BlockEntityTag")
            val templateCount = blockEntityTag.getList("UuTemplates").orElse(net.minecraft.nbt.ListTag()).size
            tooltip.add(Component.literal("模板数量: $templateCount").withStyle(ChatFormatting.GRAY))
        }
    }

    override fun getCloneItemStack(world: net.minecraft.world.level.LevelReader, pos: BlockPos, state: BlockState, includeData: Boolean): ItemStack {
        val itemStack = super.getCloneItemStack(world, pos, state, includeData)
        val blockEntity = (world as? Level)?.getBlockEntity(pos) as? PatternStorageBlockEntity ?: return itemStack
        val provider = blockEntity.level?.registryAccess() ?: return itemStack
        val nbt = blockEntity.saveWithoutMetadata(provider)
        if (!nbt.isEmpty) {
            itemStack.orCreateNbt = itemStack.orCreateNbt.apply { put("BlockEntityTag", nbt) }
        }
        return itemStack
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun spawnAfterBreak(state: BlockState, world: ServerLevel, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        super.spawnAfterBreak(state, world, pos, tool, dropExperience)
        val be = world.getBlockEntity(pos) as? Container ?: return
        Containers.dropContents(world, pos, be)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val advancedMachine = AdvancedMachineCasingBlock::class.item()
            val crystalMemory = CrystalMemory::class.instance()
            val reinforcedStone = ReinforcedStoneBlock::class.item()
            val miningLaser = MiningLaserItem::class.instance()
            val advancedCircuit = AdvancedCircuit::class.instance()
            if (advancedMachine != Items.AIR && crystalMemory != Items.AIR && reinforcedStone != Items.AIR && miningLaser != Items.AIR && advancedCircuit != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, PatternStorageBlock::class.item(), 1)
                    .pattern("SSS")
                    .pattern("CMC")
                    .pattern("LAL")
                    .define('S', reinforcedStone)
                    .define('C', crystalMemory)
                    .define('M', advancedMachine)
                    .define('L', miningLaser)
                    .define('A', advancedCircuit)
                    .unlockedBy(getHasName(crystalMemory), has(crystalMemory))
                    .save(exporter, PatternStorageBlock::class.id())
            }
        }
    }
}
