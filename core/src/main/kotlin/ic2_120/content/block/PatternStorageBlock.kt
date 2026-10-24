package ic2_120.content.block

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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.client.item.TooltipContext
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
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
class PatternStorageBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        PatternStorageBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = null

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: Level,
        pos: BlockPos
    ): net.minecraft.world.MenuProvider? =
        world.getBlockEntity(pos) as? net.minecraft.world.MenuProvider

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return InteractionResult.SUCCESS
    }

    @Environment(EnvType.CLIENT)
    override fun appendTooltip(
        stack: ItemStack,
        world: BlockGetter?,
        tooltip: MutableList<Component>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        val blockEntityTag = stack.getSubNbt("BlockEntityTag")
        val templateCount = blockEntityTag?.getList("UuTemplates", 10)?.size ?: 0
        tooltip.add(Component.literal("模板数量: $templateCount").formatted(ChatFormatting.GRAY))
    }

    override fun getPickStack(world: BlockGetter, pos: BlockPos, state: BlockState): ItemStack {
        val itemStack = super.getPickStack(world, pos, state)
        val blockEntity = world.getBlockEntity(pos) as? PatternStorageBlockEntity ?: return itemStack
        val nbt = blockEntity.createNbt()
        if (!nbt.isEmpty) {
            itemStack.orCreateNbt.put("BlockEntityTag", nbt)
        }
        return itemStack
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStacksDropped(state: BlockState, world: ServerLevel, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        super.onStacksDropped(state, world, pos, tool, dropExperience)
        val be = world.getBlockEntity(pos) as? Container ?: return
        Containers.spawn(world, pos, be)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        super.getPlacementState(ctx)

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val advancedMachine = AdvancedMachineCasingBlock::class.item()
            val crystalMemory = CrystalMemory::class.instance()
            val reinforcedStone = ReinforcedStoneBlock::class.item()
            val miningLaser = MiningLaserItem::class.instance()
            val advancedCircuit = AdvancedCircuit::class.instance()
            if (advancedMachine != Items.AIR && crystalMemory != Items.AIR && reinforcedStone != Items.AIR && miningLaser != Items.AIR && advancedCircuit != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, PatternStorageBlock::class.item(), 1)
                    .pattern("SSS")
                    .pattern("CMC")
                    .pattern("LAL")
                    .input('S', reinforcedStone)
                    .input('C', crystalMemory)
                    .input('M', advancedMachine)
                    .input('L', miningLaser)
                    .input('A', advancedCircuit)
                    .criterion(hasItem(crystalMemory), conditionsFromItem(crystalMemory))
                    .offerTo(exporter, PatternStorageBlock::class.id())
            }
        }
    }
}
