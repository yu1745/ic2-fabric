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
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

@ModBlock(name = "leash_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class LeashKineticGeneratorBlock : DirectionalMachineBlock() {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val casing = MachineCasingBlock::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LeashKineticGeneratorBlock::class.item(), 1)
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

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: net.minecraft.item.ItemPlacementContext): BlockState? {
        return defaultState.with(Properties.FACING, ctx.horizontalPlayerFacing).with(ACTIVE, false)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        LeashKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, LeashKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as LeashKineticGeneratorBlockEntity).tick(w, p, s)
        }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hit: BlockHitResult
    ): ActionResult {
        val be = world.getBlockEntity(pos) as? LeashKineticGeneratorBlockEntity ?: return ActionResult.PASS

        val heldStack = player.mainHandStack

        if (heldStack.item === Items.LEAD) {
            if (world.isClient) return ActionResult.SUCCESS

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
                player.sendMessage(Text.translatable("message.ic2_120.leash_kinetic.detached"), true)
                return ActionResult.SUCCESS
            }

            // Find mobs leashed to the player
            val nearbyMobs = world.getEntitiesByClass(
                MobEntity::class.java,
                Box(pos).expand(10.0)
            ) { mob ->
                mob.isAlive && mob.leashHolder === player
            }

            val mob = nearbyMobs.firstOrNull()
            if (mob != null) {
                mob.detachLeash(true, false)
                val serverWorld = world as? ServerWorld
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
                    Text.translatable("message.ic2_120.leash_kinetic.attached", mob.name),
                    true
                )
            } else {
                player.sendMessage(Text.translatable("message.ic2_120.leash_kinetic.no_mob"), true)
            }
            return ActionResult.SUCCESS
        }

        if (!world.isClient) {
            val factory = be as? NamedScreenHandlerFactory
            if (factory != null) {
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: World,
        pos: BlockPos
    ): NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? NamedScreenHandlerFactory
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity): BlockState {
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
        return super.onBreak(world, pos, state, player)
    }
}
