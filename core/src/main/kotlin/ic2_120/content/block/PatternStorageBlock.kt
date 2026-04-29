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
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.item.TooltipContext
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World
import java.util.function.Consumer

@ModBlock(name = "pattern_storage", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "uu")
class PatternStorageBlock : MachineBlock() {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        PatternStorageBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = null

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: World,
        pos: BlockPos
    ): net.minecraft.screen.NamedScreenHandlerFactory? =
        world.getBlockEntity(pos) as? net.minecraft.screen.NamedScreenHandlerFactory

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return ActionResult.SUCCESS
    }

    @Environment(EnvType.CLIENT)
    override fun appendTooltip(
        stack: ItemStack,
        world: BlockView?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        val blockEntityTag = stack.getSubNbt("BlockEntityTag")
        val templateCount = blockEntityTag?.getList("UuTemplates", 10)?.size ?: 0
        tooltip.add(Text.literal("模板数量: $templateCount").formatted(Formatting.GRAY))
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val blockEntity = world.getBlockEntity(pos) as? PatternStorageBlockEntity
            if (blockEntity != null) {
                val stack = ItemStack(asItem())
                val nbt = blockEntity.createNbt()
                if (!nbt.isEmpty) {
                    stack.orCreateNbt.put("BlockEntityTag", nbt)
                }
                val itemEntity = ItemEntity(
                    world,
                    pos.x + 0.5,
                    pos.y + 0.5,
                    pos.z + 0.5,
                    stack
                )
                itemEntity.setToDefaultPickupDelay()
                world.spawnEntity(itemEntity)
            }
            world.removeBlockEntity(pos)
        }
    }

    override fun getPickStack(world: BlockView, pos: BlockPos, state: BlockState): ItemStack {
        val itemStack = super.getPickStack(world, pos, state)
        val blockEntity = world.getBlockEntity(pos) as? PatternStorageBlockEntity ?: return itemStack
        val nbt = blockEntity.createNbt()
        if (!nbt.isEmpty) {
            itemStack.orCreateNbt.put("BlockEntityTag", nbt)
        }
        return itemStack
    }

    override fun onStacksDropped(state: BlockState, world: ServerWorld, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        // 已在 onStateReplaced 中自定义掉落带 NBT 的方块物品，这里不再执行默认掉落逻辑。
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val advancedMachine = AdvancedMachineCasingBlock::class.item()
            val crystalMemory = CrystalMemory::class.instance()
            val reinforcedStone = ReinforcedStoneBlock::class.item()
            val miningLaser = MiningLaserItem::class.instance()
            val advancedCircuit = AdvancedCircuit::class.instance()
            if (advancedMachine != Items.AIR && crystalMemory != Items.AIR && reinforcedStone != Items.AIR && miningLaser != Items.AIR && advancedCircuit != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PatternStorageBlock::class.item(), 1)
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
