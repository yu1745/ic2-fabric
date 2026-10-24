package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.LeashKineticGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.MenuProvider
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.level.Level
import java.util.function.Consumer

@ModBlock(name = "leash_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class LeashKineticGeneratorBlock : DirectionalMachineBlock() {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val casing = MachineCasingBlock::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LeashKineticGeneratorBlock::class.item(), 1)
                .pattern(" L ")
                .pattern(" C ")
                .define('L', Items.LEAD)
                .define('C', casing)
                .unlockedBy(
                    has(casing),
                    has(casing)
                )
                .save(exporter, LeashKineticGeneratorBlock::class.id())
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(BlockStateProperties.FACING, ctx.horizontalDirection).setValue(ACTIVE, false)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        LeashKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, LeashKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        val be = world.getBlockEntity(pos) as? LeashKineticGeneratorBlockEntity ?: return InteractionResult.PASS

        val heldStack = player.getItemInHand(player.usedItemHand)

        if (heldStack.item === Items.LEAD) {
            if (world.isClient) return InteractionResult.SUCCESS

            if (be.hasLeashedMob()) {
                // Unleash: detach mob, kill anchor, return lead
                val mob = be.findLeashedMob()
                val knot = be.findKnot()
                mob?.detachLeash(true, true)
                knot?.kill()
                knot?.remove(Entity.RemovalReason.DISCARDED)
                be.setLeashedMob(null, "")
                if (!player.isCreative) {
                    player.getInventory().add(Items.LEAD.defaultInstance)
                }
                player.displayClientMessage(Component.translatable("message.ic2_120.leash_kinetic.detached"), true)
                return InteractionResult.SUCCESS
            }

            // Find mobs leashed to the player
            val nearbyMobs = world.getEntitiesOfClass(
                Mob::class.java,
                AABB(pos).inflate(10.0)
            ) { mob ->
                mob.isAlive && mob.holdingEntity === player
            }

            val mob = nearbyMobs.firstOrNull()
            if (mob != null) {
                mob.detachLeash(true, false)
                val serverWorld = world as? ServerLevel
                if (serverWorld != null) {
                    val anchor = EntityType.ARMOR_STAND.create(serverWorld)
                    if (anchor != null) {
                        // Armor stand getLeashPos adds eyeHeight*0.7 offset (~1.24),
                        // compensate so the leash visually ends at the knot position (y+1.4)
                        anchor.setPos(pos.x + 0.5, pos.y + 0.16, pos.z + 0.5)
                        anchor.isInvisible = true
                        anchor.setNoGravity(true)
                        serverWorld.addFreshEntity(anchor)
                        mob.attachLeash(anchor, true)
                        be.setLeashedMob(mob.uuid, mob.name.string, anchor.uuid)
                    }
                }
                heldStack.shrink(1)
                player.displayClientMessage(
                    Component.translatable("message.ic2_120.leash_kinetic.attached", mob.name),
                    true
                )
            } else {
                player.displayClientMessage(Component.translatable("message.ic2_120.leash_kinetic.no_mob"), true)
            }
            return InteractionResult.SUCCESS
        }

        return InteractionResult.SUCCESS
    }

    override fun playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player) {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? LeashKineticGeneratorBlockEntity
            be?.let {
                val mob = it.findLeashedMob()
                if (mob != null && mob.isAlive) {
                    mob.detachLeash(true, true)
                }
                val knot = it.findKnot()
                if (knot != null && knot.isAlive) {
                    knot.kill()
                    knot.remove(Entity.RemovalReason.DISCARDED)
                }
            }
        }
        super.playerWillDestroy(world, pos, state, player)
    }
}
