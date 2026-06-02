package buildcraft_addon.content.block

import buildcraft_addon.content.blockentity.OilSpringBlockEntity
import buildcraft_addon.content.fluid.ModFluids
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluid
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.World
import net.minecraft.world.WorldView

enum class SpringType(val fluid: Fluid) : StringIdentifiable {
    WATER(net.minecraft.fluid.Fluids.WATER),
    OIL(ModFluids.CRUDE_OIL_STILL);

    override fun asString(): String = name.lowercase()
}

@ModBlock(name = "spring", registerItem = true, tab = CreativeTab.BUILDCRAFT)
class SpringBlock : Block(
    AbstractBlock.Settings.copy(Blocks.BEDROCK).strength(-1.0f, 6000000.0f)
), BlockEntityProvider {

    companion object {
        val SPRING_TYPE: EnumProperty<SpringType> = EnumProperty.of("spring_type", SpringType::class.java)
    }

    init {
        defaultState = stateManager.defaultState.with(SPRING_TYPE, SpringType.WATER)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(SPRING_TYPE)
    }

    override fun hasRandomTicks(state: BlockState): Boolean = false

    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        if (!world.isClient) {
            world.scheduleBlockTick(pos, this, 20)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        generateSpringFluid(world, pos, state)
    }

    private fun generateSpringFluid(world: World, pos: BlockPos, state: BlockState) {
        val springType = state.get(SPRING_TYPE)
        val upPos = pos.up()
        val upState = world.getBlockState(upPos)

        // 上方已有流体或非空气方块 → 停止生成，不再重调度
        if (!upState.isAir) return

        world.setBlockState(upPos, springType.fluid.defaultState.blockState)
        // 成功后重新调度
        world.scheduleBlockTick(pos, this, 20)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient && state.get(SPRING_TYPE) == SpringType.OIL) {
            val held = player.getStackInHand(Hand.MAIN_HAND)
            if (held.isOf(net.minecraft.item.Items.GLASS_BOTTLE)) {
                held.decrement(1)
                player.giveItemStack(ItemStack(net.minecraft.item.Items.EMERALD))
                world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f)
                return ActionResult.SUCCESS
            }
        }
        return ActionResult.PASS
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return OilSpringBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? = null

    override fun getPickStack(world: WorldView, pos: BlockPos, state: BlockState): ItemStack {
        return ItemStack(this)
    }
}
