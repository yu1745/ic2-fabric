package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.FluidDrainable
import net.minecraft.block.FluidFillable
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.item.FluidModificationItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryKey
import net.minecraft.sound.SoundCategory
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
import net.minecraft.item.Items
import org.slf4j.LoggerFactory

/** 通用流体单元 NBT 键：存储 FluidVariant */
const val FLUID_CELL_NBT_KEY = "FluidVariant"

/** 从 NBT 读取 FluidVariant，空则返回 null */
fun ItemStack.getFluidCellVariant(): FluidVariant? {
    val nbt = nbt ?: return null
    val fluidTag = nbt.getCompound(FLUID_CELL_NBT_KEY)
    if (fluidTag.isEmpty) return null
    return FluidVariant.fromNbt(fluidTag)
}

/** 将 FluidVariant 写入 NBT */
fun ItemStack.setFluidCellVariant(variant: FluidVariant) {
    orCreateNbt.put(FLUID_CELL_NBT_KEY, variant.toNbt())
}

/** 判断流体单元是否为空 */
fun ItemStack.isFluidCellEmpty(): Boolean = getFluidCellVariant() == null || getFluidCellVariant() == FluidVariant.blank()

/** 将桶物品转为满流体单元 ItemStack（供 UseBlockCallback 等使用） */
internal fun bucketToFilledFluidCell(bucketStack: ItemStack): ItemStack? {
    val fluid = when (bucketStack.item) {
        Items.WATER_BUCKET -> Fluids.WATER
        Items.LAVA_BUCKET -> Fluids.LAVA
        else -> {
            val ctx = ContainerItemContext.withInitial(bucketStack)
            val storage = ctx.find(FluidStorage.ITEM) ?: return null
            var found: Fluid? = null
            for (view in storage) {
                if (view.amount >= FluidConstants.BUCKET) {
                    found = view.resource.fluid
                    break
                }
            }
            found ?: return null
        }
    }
    return ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell"))).apply {
        setFluidCellVariant(FluidVariant.of(fluid))
    }
}

/**
 * 通用流体单元：在 NBT 中存储 FluidVariant，支持任意流体。
 * 使用 Fabric Fluid API 获取流体颜色并渲染到物品中心（客户端 FluidCellColorProvider）。
 */
@ModItem(name = "fluid_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class FluidCellItem : Item(FabricItemSettings()), FluidModificationItem {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val player = context.player ?: return ActionResult.PASS
        val hand = context.hand
        val stack = player.getStackInHand(hand)
        if (world.isClient) return ActionResult.SUCCESS

        val fluid = stack.getFluidCellVariant()?.fluid ?: return ActionResult.PASS

        // 1. 先尝试 Fabric FluidStorage（储罐、地热发电机等）
        val storage = FluidStorage.SIDED.find(world, pos, context.side)
            ?: FluidStorage.SIDED.find(world, pos, null)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
            return ActionResult.SUCCESS
        }

        // 2. 放置流体到世界
        val hitResult = BlockHitResult(context.hitPos, context.side, pos, context.hitsInsideBlock())
        if (placeFluid(player, world, pos, hitResult)) {
            if (!player.abilities.creativeMode) {
                stack.decrement(1)
                val empty = ItemStack(emptyCell)
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
        val fluid = player?.getStackInHand(Hand.MAIN_HAND)?.getFluidCellVariant()?.fluid
            ?: player?.getStackInHand(Hand.OFF_HAND)?.getFluidCellVariant()?.fluid
            ?: return false

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

    override fun getRecipeRemainder(stack: ItemStack): ItemStack = ItemStack(emptyCell)
}

private val emptyCell: Item get() = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell"))

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
                val filled = mapBucketToFilledCell(drained) ?: return TypedActionResult.pass(stack)
                drainable.getBucketFillSound().ifPresent { world.playSound(user, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                world.emitGameEvent(user, GameEvent.FLUID_PICKUP, pos)
                if (!user.abilities.creativeMode) {
                    stack.decrement(1)
                    if (stack.isEmpty) {
                        user.setStackInHand(hand, filled)
                    } else if (!user.inventory.insertStack(filled)) {
                        user.dropItem(filled, false)
                    }
                } else {
                    if (!user.inventory.insertStack(filled)) {
                        user.dropItem(filled, false)
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
                val filled = mapBucketToFilledCell(drained) ?: return ActionResult.PASS
                drainable.getBucketFillSound().ifPresent { world.playSound(player, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos)
                val stack = player.getStackInHand(hand)
                if (!player.abilities.creativeMode) {
                    stack.decrement(1)
                    if (stack.isEmpty) {
                        player.setStackInHand(hand, filled)
                    } else if (!player.inventory.insertStack(filled)) {
                        player.dropItem(filled, false)
                    }
                } else {
                    if (!player.inventory.insertStack(filled)) {
                        player.dropItem(filled, false)
                    }
                }
                return ActionResult.SUCCESS
            }
        }
        return ActionResult.PASS
    }

    /** 将桶物品映射为对应的满流体单元 ItemStack（带 NBT），无法映射则返回 null */
    protected abstract fun mapBucketToFilledCell(bucketStack: ItemStack): ItemStack?
}

// ========== 容器类 - 单元 ==========

private fun cellItem(id: String): Item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, id))

@ModItem(name = "empty_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class EmptyCell : EmptyCellItem(FabricItemSettings()) {
    override fun mapBucketToFilledCell(bucketStack: ItemStack): ItemStack? = bucketToFilledFluidCell(bucketStack)
}

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

// coolant_bucket、hot_coolant_bucket、uu_matter_bucket、weed_ex_bucket、pahoehoe_lava_bucket
// 由 ModFluids 注册为 BucketItem，此处不再重复注册

// ========== 建筑泡沫类 ==========

@ModItem(name = "cf_powder", tab = CreativeTab.IC2_MATERIALS, group = "construction_foam")
class CfPowder : Item(FabricItemSettings())

@ModItem(name = "pellet", tab = CreativeTab.IC2_MATERIALS, group = "construction_foam")
class Pellet : Item(FabricItemSettings())

/**
 * 通用流体单元的 FluidStorage：空单元可装任意流体，满单元可倒出对应流体。
 */
private class FluidCellStorage(private val ctx: ContainerItemContext) : Storage<FluidVariant> {

    private val fluidCell = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell"))
    private val emptyCell = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell"))

    override fun supportsInsertion(): Boolean = true

    override fun supportsExtraction(): Boolean = true

    override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount < FluidConstants.BUCKET) return 0

        val current = ctx.itemVariant
        if (current.item != emptyCell) return 0

        val filledStack = ItemStack(fluidCell).apply { setFluidCellVariant(resource) }
        val newVariant = ItemVariant.of(filledStack)
        if (ctx.exchange(newVariant, 1, transaction) == 1L) return FluidConstants.BUCKET
        return 0
    }

    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount < FluidConstants.BUCKET) return 0

        val current = ctx.itemVariant
        if (current.item != fluidCell) return 0

        val stored = current.nbt?.getCompound(FLUID_CELL_NBT_KEY)?.let { FluidVariant.fromNbt(it) } ?: return 0
        if (!resource.equals(stored)) return 0

        val emptyStack = ItemStack(emptyCell)
        val newVariant = ItemVariant.of(emptyStack)
        if (ctx.exchange(newVariant, 1, transaction) == 1L) return FluidConstants.BUCKET
        return 0
    }

    override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
        val current = ctx.itemVariant
        if (current.item != fluidCell) return mutableListOf<StorageView<FluidVariant>>().iterator() as MutableIterator<StorageView<FluidVariant>>
        val stored = current.nbt?.getCompound(FLUID_CELL_NBT_KEY)?.let { FluidVariant.fromNbt(it) } ?: return mutableListOf<StorageView<FluidVariant>>().iterator() as MutableIterator<StorageView<FluidVariant>>
        if (stored.isBlank) return mutableListOf<StorageView<FluidVariant>>().iterator() as MutableIterator<StorageView<FluidVariant>>
        return mutableListOf(object : StorageView<FluidVariant> {
            override fun getResource(): FluidVariant = stored
            override fun getAmount(): Long = FluidConstants.BUCKET
            override fun getCapacity(): Long = FluidConstants.BUCKET
            override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                this@FluidCellStorage.extract(resource, maxAmount, transaction)
            override fun isResourceBlank(): Boolean = stored.isBlank
        }).iterator() as MutableIterator<StorageView<FluidVariant>>
    }
}

/**
 * 注册单元与桶的 FluidStorage.ITEM，使其可与流体方块/储罐交互。
 * 单元等效 1 桶（1000 mB）。
 */
object CellAndBucketFluidRegistration {

    private val logger = LoggerFactory.getLogger("ic2_120/CellAndBucketFluidRegistration")

    fun register() {
        val modId = Ic2_120.MOD_ID
        val emptyCell = Registries.ITEM.get(Identifier(modId, "empty_cell"))
        val fluidCell = Registries.ITEM.get(Identifier(modId, "fluid_cell"))

        // 空单元 + 满流体单元：通用 FluidStorage（支持任意流体）
        FluidStorage.combinedItemApiProvider(emptyCell).register { ctx ->
            FluidCellStorage(ctx)
        }
        FluidStorage.combinedItemApiProvider(fluidCell).register { ctx ->
            FluidCellStorage(ctx)
        }

        // UseBlockCallback：在默认交互前拦截，用 FluidHandling.ANY 射线检测流体方块
        // （默认 useOnBlock 的射线会穿透水/岩浆，导致无法收集）
        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val stack = player.getStackInHand(hand)
            if (stack.item != emptyCell) return@register ActionResult.PASS

            if (world.isClient) return@register ActionResult.PASS

            val start = player.eyePos
            val end = start.add(player.rotationVector.multiply(5.0))
            val rayCtx = RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player)
            val hitResult = world.raycast(rayCtx)
            val pos = hitResult.blockPos

            // 1. FluidStorage（储罐等）
            val storage = FluidStorage.SIDED.find(world, pos, hitResult.side)
                ?: FluidStorage.SIDED.find(world, pos, null)
            if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
                return@register ActionResult.SUCCESS
            }

            // 2. FluidDrainable（水、岩浆方块）- 需用 FluidHandling.ANY 射线才能命中
            val state = world.getBlockState(pos)
            if (state.block is FluidDrainable) {
                val drainable = state.block as FluidDrainable
                val drained = drainable.tryDrainFluid(world, pos, state)
                if (!drained.isEmpty) {
                    val filled = bucketToFilledFluidCell(drained) ?: return@register ActionResult.PASS
                    drainable.getBucketFillSound().ifPresent { world.playSound(player, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                    world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos)
                    if (!player.abilities.creativeMode) {
                        stack.decrement(1)
                        if (stack.isEmpty) {
                            player.setStackInHand(hand, filled)
                        } else if (!player.inventory.insertStack(filled)) {
                            player.dropItem(filled, false)
                        }
                    } else {
                        if (!player.inventory.insertStack(filled)) {
                            player.dropItem(filled, false)
                        }
                    }
                    return@register ActionResult.SUCCESS
                }
            }
            ActionResult.PASS
        }

        // 遍历所有流体，将满流体单元注册到创造模式物品栏（IC2 材料）
        val ic2MaterialsKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(modId, CreativeTab.IC2_MATERIALS.id))
        ItemGroupEvents.modifyEntriesEvent(ic2MaterialsKey).register { entries ->
            val fluids = Registries.FLUID.filter { fluid ->
                fluid != Fluids.EMPTY && fluid != Fluids.FLOWING_LAVA && fluid != Fluids.FLOWING_WATER
            }.sortedBy { Registries.FLUID.getId(it).toString() }
            for (fluid in fluids) {
                val fluidId = Registries.FLUID.getId(fluid)
                logger.info("注册流体单元到创造模式物品栏: {}", fluidId)
                val stack = ItemStack(fluidCell).apply { setFluidCellVariant(FluidVariant.of(fluid)) }
                entries.addAfter(fluidCell, stack)
            }
        }
    }
}
