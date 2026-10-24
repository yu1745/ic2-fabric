package ic2_120.content.block

import ic2_120.content.block.machines.LeashKineticGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
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
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val casing = MachineCasingBlock::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, LeashKineticGeneratorBlock::class.item(), 1)
                .pattern(" L ")
                .pattern(" C ")
                .input('L', Items.LEAD)
                .input('C', casing)
                .criterion(
                    hasItem(casing),
                    conditionsFromItem(casing)
                )
                .offerTo(exporter, LeashKineticGeneratorBlock::class.id())
        }
    }

    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState? {
        return defaultState.with(BlockStateProperties.FACING, ctx.horizontalPlayerFacing).with(ACTIVE, false)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        LeashKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, LeashKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val be = world.getBlockEntity(pos) as? LeashKineticGeneratorBlockEntity ?: return InteractionResult.PASS

        val heldStack = player.getStackInHand(hand)

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
                    player.giveItemStack(Items.LEAD.defaultStack)
                }
                player.sendMessage(Component.translatable("message.ic2_120.leash_kinetic.detached"), true)
                return InteractionResult.SUCCESS
            }

            // Find mobs leashed to the player
            val nearbyMobs = world.getEntitiesByClass(
                Mob::class.java,
                AABB(pos).expand(10.0)
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
                        serverWorld.spawnEntity(anchor)
                        mob.attachLeash(anchor, true)
                        be.setLeashedMob(mob.uuid, mob.name.string, anchor.uuid)
                    }
                }
                heldStack.decrement(1)
                player.sendMessage(
                    Component.translatable("message.ic2_120.leash_kinetic.attached", mob.name),
                    true
                )
            } else {
                player.sendMessage(Component.translatable("message.ic2_120.leash_kinetic.no_mob"), true)
            }
            return InteractionResult.SUCCESS
        }

        return InteractionResult.SUCCESS
    }

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: Level,
        pos: BlockPos
    ): MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? MenuProvider
    }

    override fun onBreak(world: Level, pos: BlockPos, state: BlockState, player: Player) {
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
        super.onBreak(world, pos, state, player)
    }
}
