package buildcraft_addon.content.block

import buildcraft_addon.content.blockentity.OilSpringBlockEntity
import buildcraft_addon.content.fluid.ModFluids
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.util.StringRepresentable
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level

enum class SpringType(val fluid: Fluid) : StringRepresentable {
    WATER(net.minecraft.world.level.material.Fluids.WATER),
    OIL(ModFluids.CRUDE_OIL_STILL);

    override fun asString(): String = name.lowercase()
}

@ModBlock(name = "spring", registerItem = true, tab = CreativeTab.BUILDCRAFT)
class SpringBlock : Block(
    BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).strength(-1.0f, 6000000.0f)
), EntityBlock {

    companion object {
        val SPRING_TYPE: EnumProperty<SpringType> = EnumProperty.create("spring_type", SpringType::class.java)
    }

    init {
        defaultState = stateManager.defaultBlockState.setValue(SPRING_TYPE, SpringType.WATER)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(SPRING_TYPE)
    }

    override fun hasRandomTicks(state: BlockState): Boolean = false

    override fun onBlockAdded(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        if (!world.isClient) {
            world.scheduleBlockTick(pos, this, 20)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun scheduledTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        generateSpringFluid(world, pos, state)
    }

    private fun generateSpringFluid(world: Level, pos: BlockPos, state: BlockState) {
        val springType = state.getValue(SPRING_TYPE)
        val upPos = pos.up()
        val upState = world.getBlockState(upPos)

        // 上方已有流体或非空气方块 → 停止生成，不再重调度
        if (!upState.isAir) return

        world.setBlock(upPos, springType.fluid.defaultBlockState.blockState)
        // 成功后重新调度
        world.scheduleBlockTick(pos, this, 20)
    }

    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient && state.getValue(SPRING_TYPE) == SpringType.OIL) {
            val held = player.getItemInHand(hand)
            if (held.`is`(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
                held.shrink(1)
                player.giveItemStack(ItemStack(net.minecraft.world.item.Items.EMERALD))
                world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f)
                return InteractionResult.SUCCESS
            }
        }
        return InteractionResult.PASS
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return OilSpringBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? = null

    override fun getPickStack(world: net.minecraft.world.level.BlockGetter, pos: BlockPos, state: BlockState): ItemStack {
        return ItemStack(this)
    }
}
