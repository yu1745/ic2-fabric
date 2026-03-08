package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.minecraft.block.FluidDrainable
import net.minecraft.block.FluidFillable
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.item.FluidModificationItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundCategory
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
import net.minecraft.item.Items

/**
 * 流体单元基类：实现 FluidModificationItem，支持与世界中的水/岩浆方块交互（放置、收集）。
 * 同时保留 FluidStorage 以支持储罐等方块实体。
 */
abstract class FluidCellItem(
    settings: FabricItemSettings,
    private val fluid: Fluid,
    private val emptyItem: Item
) : Item(settings), FluidModificationItem {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val player = context.player ?: return ActionResult.PASS
        val hand = context.hand
        if (world.isClient) return ActionResult.SUCCESS

        // 1. 先尝试 Fabric FluidStorage（储罐、地热发电机等）
        val storage = FluidStorage.SIDED.find(world, pos, context.side)
            ?: FluidStorage.SIDED.find(world, pos, null)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
            return ActionResult.SUCCESS
        }

        // 2. 使用原版 FluidModificationItem 放置流体到世界
        val hitResult = BlockHitResult(context.hitPos, context.side, pos, context.hitsInsideBlock())
        if (placeFluid(player, world, pos, hitResult)) {
            val stack = player.getStackInHand(hand)
            if (!player.abilities.creativeMode) {
                stack.decrement(1)
                val empty = ItemStack(emptyItem)
                if (stack.isEmpty) {
                    player.setStackInHand(hand, empty)
                } else if (!player.inventory.insertStack(empty)) {
                    player.dropItem(empty, false)
                }
            }
            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }

    override fun placeFluid(
        player: net.minecraft.entity.player.PlayerEntity?,
        world: World,
        pos: BlockPos,
        hitResult: BlockHitResult?
    ): Boolean {
        val state = world.getBlockState(pos)
        val block = state.block

        // FluidFillable：如炼药锅等可注入液体的方块
        if (block is FluidFillable) {
            if (block.canFillWithFluid(world, pos, state, fluid)) {
                block.tryFillWithFluid(world, pos, state, fluid.defaultState)
                fluid.getBucketFillSound().ifPresent { world.playSound(player, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                world.emitGameEvent(player, GameEvent.FLUID_PLACE, pos)
                return true
            }
        }

        // 普通方块：在点击位置放置流体（需可替换，如空气、流体本身）
        if (state.isReplaceable || state.fluidState.isStill) {
            if (world.setBlockState(pos, fluid.defaultState.blockState)) {
                fluid.getBucketFillSound().ifPresent { world.playSound(player, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                world.emitGameEvent(player, GameEvent.FLUID_PLACE, pos)
                return true
            }
        }
        // 固体方块：在点击面朝向的相邻格放置（如点击泥土顶部 → 在上方空气格放水）
        val adjacentPos = hitResult?.side?.let { pos.offset(it) } ?: pos.up()
        val adjacentState = world.getBlockState(adjacentPos)
        if (adjacentState.isReplaceable || adjacentState.fluidState.isStill) {
            if (world.setBlockState(adjacentPos, fluid.defaultState.blockState)) {
                fluid.getBucketFillSound().ifPresent { world.playSound(player, adjacentPos, it, SoundCategory.BLOCKS, 1f, 1f) }
                world.emitGameEvent(player, GameEvent.FLUID_PLACE, adjacentPos)
                return true
            }
        }
        return false
    }

    override fun getRecipeRemainder(stack: ItemStack): ItemStack = ItemStack(emptyItem)
}

/**
 * 空单元：可从 FluidDrainable（水/岩浆方块）收集液体，也可与 FluidStorage 储罐交互。
 * 使用 use() + RaycastContext.FluidHandling.ANY 射线检测，才能命中流体方块（默认射线会穿透水/岩浆）。
 */
abstract class EmptyCellItem(settings: FabricItemSettings) : Item(settings) {

    override fun use(world: World, user: net.minecraft.entity.player.PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        // 使用 FluidHandling.ANY 射线检测，才能命中流体方块（原版 useOnBlock 的射线会穿透水/岩浆）
        val hitResult = raycast(world, user, RaycastContext.FluidHandling.ANY)
        if (hitResult.type != HitResult.Type.BLOCK || hitResult !is BlockHitResult) {
            return TypedActionResult.pass(stack)
        }

        val pos = hitResult.blockPos

        // 1. 先尝试 Fabric FluidStorage（储罐等）
        val storage = FluidStorage.SIDED.find(world, pos, hitResult.side)
            ?: FluidStorage.SIDED.find(world, pos, null)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, user, hand)) {
            return TypedActionResult.success(user.getStackInHand(hand))
        }

        // 2. 从世界流体方块收集（FluidDrainable：水、岩浆等）
        val state = world.getBlockState(pos)
        if (state.block is FluidDrainable) {
            val drainable = state.block as FluidDrainable
            val drained = drainable.tryDrainFluid(world, pos, state)
            if (!drained.isEmpty) {
                val filledCell = mapBucketToCell(drained) ?: return TypedActionResult.pass(stack)
                drainable.getBucketFillSound().ifPresent { world.playSound(user, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                world.emitGameEvent(user, GameEvent.FLUID_PICKUP, pos)
                if (!user.abilities.creativeMode) {
                    stack.decrement(1)
                    val filled = ItemStack(filledCell)
                    if (stack.isEmpty) {
                        user.setStackInHand(hand, filled)
                    } else if (!user.inventory.insertStack(filled)) {
                        user.dropItem(filled, false)
                    }
                } else {
                    if (!user.inventory.insertStack(ItemStack(filledCell))) {
                        user.dropItem(ItemStack(filledCell), false)
                    }
                }
                return TypedActionResult.success(user.getStackInHand(hand))
            }
        }
        return TypedActionResult.pass(stack)
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val player = context.player ?: return ActionResult.PASS
        val hand = context.hand
        if (world.isClient) return ActionResult.SUCCESS

        // 1. 先尝试 Fabric FluidStorage（储罐等）
        val storage = FluidStorage.SIDED.find(world, pos, context.side)
            ?: FluidStorage.SIDED.find(world, pos, null)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
            return ActionResult.SUCCESS
        }

        // 2. 从世界流体方块收集（FluidDrainable：水、岩浆等）
        val state = world.getBlockState(pos)
        if (state.block is FluidDrainable) {
            val drainable = state.block as FluidDrainable
            val drained = drainable.tryDrainFluid(world, pos, state)
            if (!drained.isEmpty) {
                val filledCell = mapBucketToCell(drained) ?: return ActionResult.PASS
                drainable.getBucketFillSound().ifPresent { world.playSound(player, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos)
                val stack = player.getStackInHand(hand)
                if (!player.abilities.creativeMode) {
                    stack.decrement(1)
                    val filled = ItemStack(filledCell)
                    if (stack.isEmpty) {
                        player.setStackInHand(hand, filled)
                    } else if (!player.inventory.insertStack(filled)) {
                        player.dropItem(filled, false)
                    }
                } else {
                    if (!player.inventory.insertStack(ItemStack(filledCell))) {
                        player.dropItem(ItemStack(filledCell), false)
                    }
                }
                return ActionResult.SUCCESS
            }
        }
        return ActionResult.PASS
    }

    /** 将桶物品映射为对应的满单元物品 */
    protected abstract fun mapBucketToCell(bucketStack: ItemStack): Item?
}

// ========== 容器类 - 单元 ==========

private fun cellItem(id: String): Item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, id))

@ModItem(name = "empty_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class EmptyCell : EmptyCellItem(FabricItemSettings()) {
    override fun mapBucketToCell(bucketStack: ItemStack): Item? = when (bucketStack.item) {
        net.minecraft.item.Items.WATER_BUCKET -> cellItem("water_cell")
        net.minecraft.item.Items.LAVA_BUCKET -> cellItem("lava_cell")
        else -> null
    }
}

@ModItem(name = "water_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class WaterCell : FluidCellItem(
    FabricItemSettings(),
    Fluids.WATER,
    cellItem("empty_cell")
)

@ModItem(name = "lava_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class LavaCell : FluidCellItem(
    FabricItemSettings(),
    Fluids.LAVA,
    cellItem("empty_cell")
)

@ModItem(name = "air_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class AirCell : Item(FabricItemSettings())

@ModItem(name = "biofuel_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class BiofuelCell : Item(FabricItemSettings())

@ModItem(name = "bio_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class BioCell : Item(FabricItemSettings())

@ModItem(name = "weed_ex_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class WeedExCell : Item(FabricItemSettings())

// ========== 容器类 - 桶 ==========

@ModItem(name = "construction_foam_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class ConstructionFoamBucket : Item(FabricItemSettings())

@ModItem(name = "biofuel_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class BiofuelBucket : Item(FabricItemSettings())

@ModItem(name = "biomass_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class BiomassBucket : Item(FabricItemSettings())

@ModItem(name = "construct_foam_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class ConstructFoamBucket : Item(FabricItemSettings())

@ModItem(name = "coolant_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
class CoolantBucket : Item(FabricItemSettings())

/**
 * 注册单元与桶的 FluidStorage.ITEM，使其可与流体方块/储罐交互。
 * 单元等效 1 桶（1000 mB）。
 */
object CellAndBucketFluidRegistration {

    fun register() {
        val modId = Ic2_120.MOD_ID
        val emptyCell = Registries.ITEM.get(Identifier(modId, "empty_cell"))
        val waterCell = Registries.ITEM.get(Identifier(modId, "water_cell"))
        val lavaCell = Registries.ITEM.get(Identifier(modId, "lava_cell"))

        // 水单元：可倒出 → 空单元
        FluidStorage.combinedItemApiProvider(waterCell).register { ctx ->
            net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage(
                ctx, emptyCell,
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.fluid.Fluids.WATER),
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET
            )
        }

        // 岩浆单元：可倒出 → 空单元
        FluidStorage.combinedItemApiProvider(lavaCell).register { ctx ->
            net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage(
                ctx, emptyCell,
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.fluid.Fluids.LAVA),
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET
            )
        }

        // 空单元：可装水 → 水单元
        FluidStorage.combinedItemApiProvider(emptyCell).register { ctx ->
            net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage(
                ctx, waterCell,
                net.minecraft.fluid.Fluids.WATER,
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET
            )
        }

        // 空单元：可装岩浆 → 岩浆单元
        FluidStorage.combinedItemApiProvider(emptyCell).register { ctx ->
            net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage(
                ctx, lavaCell,
                net.minecraft.fluid.Fluids.LAVA,
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET
            )
        }

        // UseBlockCallback：在默认交互前拦截，用 FluidHandling.ANY 射线检测流体方块
        // （默认 useOnBlock 的射线会穿透水/岩浆，导致无法收集）
        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val stack = player.getStackInHand(hand)
            if (stack.item != emptyCell) return@register ActionResult.PASS

            if (world.isClient) return@register ActionResult.PASS

            val start = player.eyePos
            val end = start.add(player.rotationVector.multiply(5.0))
            val ctx = RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player)
            val hitResult = world.raycast(ctx)
            val pos = hitResult.blockPos

            // 1. FluidStorage（储罐等）
            val storage = FluidStorage.SIDED.find(world, pos, hitResult.side)
                ?: FluidStorage.SIDED.find(world, pos, null)
            if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
                return@register ActionResult.SUCCESS
            }

            // 2. FluidDrainable（水、岩浆方块）
            val state = world.getBlockState(pos)
            if (state.block is FluidDrainable) {
                val drainable = state.block as FluidDrainable
                val drained = drainable.tryDrainFluid(world, pos, state)
                if (!drained.isEmpty) {
                    val filledCell = when (drained.item) {
                        Items.WATER_BUCKET -> waterCell
                        Items.LAVA_BUCKET -> lavaCell
                        else -> return@register ActionResult.PASS
                    }
                    drainable.getBucketFillSound().ifPresent { world.playSound(player, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                    world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos)
                    if (!player.abilities.creativeMode) {
                        stack.decrement(1)
                        val filled = ItemStack(filledCell)
                        if (stack.isEmpty) {
                            player.setStackInHand(hand, filled)
                        } else if (!player.inventory.insertStack(filled)) {
                            player.dropItem(filled, false)
                        }
                    } else {
                        if (!player.inventory.insertStack(ItemStack(filledCell))) {
                            player.dropItem(ItemStack(filledCell), false)
                        }
                    }
                    return@register ActionResult.SUCCESS
                }
            }
            ActionResult.PASS
        }
    }
}
