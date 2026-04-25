package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.content.fluid.ModFluids
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
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
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import ic2_120.getCustomData
import ic2_120.getOrCreateCustomData
import net.minecraft.block.FluidDrainable
import net.minecraft.block.FluidFillable
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.item.FluidModificationItem
import net.minecraft.item.Item
import net.minecraft.item.tooltip.TooltipType
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
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
import net.minecraft.item.Items
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.util.Formatting
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.recipe.book.RecipeCategory
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.id
import ic2_120.registry.recipeId
import ic2_120.content.recipes.crafting.EmptyFluidCellToEmptyCellRecipeDatagen
import org.slf4j.LoggerFactory

/** 通用流体单元 NBT 键：存储 FluidVariant */
const val FLUID_CELL_NBT_KEY = "FluidVariant"

/** 从 NBT 读取 FluidVariant，空则返回 null */
fun ItemStack.getFluidCellVariant(): FluidVariant? {
    val nbt = getCustomData() ?: return null
    val fluidTag = nbt.getCompound(FLUID_CELL_NBT_KEY)
    if (fluidTag.isEmpty) return null
    return FluidVariant.CODEC.decode(NbtOps.INSTANCE, fluidTag).result().map { it.first }.orElse(FluidVariant.blank())
}

/** 将 FluidVariant 写入 NBT */
fun ItemStack.setFluidCellVariant(variant: FluidVariant) {
    getOrCreateCustomData().put(FLUID_CELL_NBT_KEY, FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, variant).result().orElse(NbtCompound()))
}

/** 判断流体单元是否为空 */
fun ItemStack.isFluidCellEmpty(): Boolean = getFluidCellVariant() == null || getFluidCellVariant() == FluidVariant.blank()

/** 判断物品是否为地热发电机的岩浆燃料：岩浆桶、岩浆单元、通用流体单元（NBT 为岩浆） */
fun ItemStack.isLavaFuel(): Boolean {
    return when (item) {
        Items.LAVA_BUCKET -> true
        Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "lava_cell")) -> true
        Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "fluid_cell")) -> {
            val fluid = getFluidCellVariant()?.fluid ?: return false
            fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA
        }
        else -> false
    }
}

/** 判断物品是否为水力发电机的水燃料：水桶、水单元、通用流体单元（NBT 为水） */
fun ItemStack.isWaterFuel(): Boolean {
    return when (item) {
        Items.WATER_BUCKET -> true
        Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "water_cell")) -> true
        Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "distilled_water_cell")) -> true
        Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "fluid_cell")) -> {
            val fluid = getFluidCellVariant()?.fluid ?: return false
            fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
                fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING
        }
        else -> false
    }
}

/**
 * 根据流体返回满流体单元 ItemStack。
 * 若存在注册的专用单元（如 water_cell、coolant_cell），则返回该物品；否则使用 fluid_cell + NBT。
 */
internal fun fluidToFilledCellStack(fluid: Fluid): ItemStack {
    val cellId = when (fluid) {
        Fluids.WATER, Fluids.FLOWING_WATER -> "water_cell"
        ModFluids.DISTILLED_WATER_STILL, ModFluids.DISTILLED_WATER_FLOWING -> "distilled_water_cell"
        Fluids.LAVA, Fluids.FLOWING_LAVA -> "lava_cell"
        ModFluids.COOLANT_STILL, ModFluids.COOLANT_FLOWING -> "coolant_cell"
        ModFluids.HOT_COOLANT_STILL, ModFluids.HOT_COOLANT_FLOWING -> "hot_coolant_cell"
        ModFluids.UU_MATTER_STILL, ModFluids.UU_MATTER_FLOWING -> "uu_matter_cell"
        ModFluids.WEED_EX_STILL, ModFluids.WEED_EX_FLOWING -> "weed_ex_cell"
        ModFluids.PAHOEHOE_LAVA_STILL, ModFluids.PAHOEHOE_LAVA_FLOWING -> "pahoehoe_lava_cell"
        ModFluids.BIOFUEL_STILL, ModFluids.BIOFUEL_FLOWING -> "biofuel_cell"
        ModFluids.BIOMASS_STILL, ModFluids.BIOMASS_FLOWING -> "biomass_cell"
        else -> null
    }
    return if (cellId != null) {
        ItemStack(cellItem(cellId))
    } else {
        ItemStack(cellItem("fluid_cell")).apply { setFluidCellVariant(FluidVariant.of(fluid)) }
    }
}

/** 将桶物品转为满流体单元 ItemStack（供 UseBlockCallback 等使用） */
internal fun bucketToFilledFluidCell(bucketStack: ItemStack): ItemStack? {
    val fluid = when (bucketStack.item) {
        Items.WATER_BUCKET -> Fluids.WATER
        Items.LAVA_BUCKET -> Fluids.LAVA
        else -> {
            val ctx = ContainerItemContext.withConstant(bucketStack)
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
    return fluidToFilledCellStack(fluid)
}

/**
 * 通用流体单元：在 NBT 中存储 FluidVariant，支持任意流体。
 * **仅用于其他 mod 的流体**，本模组的流体请使用 ModFluidCell 子类。
 *
 * 使用 Fabric Fluid API 获取流体颜色并渲染到物品中心（客户端 FluidCellColorProvider）。
 */
@ModItem(name = "fluid_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class FluidCellItem : Item(Item.Settings()), FluidModificationItem {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val player = context.player ?: return ActionResult.PASS
        val hand = context.hand
        val stack = player.getStackInHand(hand)
        if (world.isClient) return ActionResult.SUCCESS

        val fluid = stack.getFluidCellVariant()?.fluid ?: return ActionResult.PASS

        // 注意：与 FluidStorage 的交互已在 UseBlockCallback 中处理，这里只处理放置流体到世界
        // 放置流体到世界
        val hitResult = BlockHitResult(context.hitPos, context.side, pos, context.hitsInsideBlock())
        if (placeFluid(player, world, pos, hitResult)) {
            if (!player.abilities.creativeMode) {
                stack.decrement(1)
                val empty = ItemStack(cellItem("empty_cell"))
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
        val rawFluid = player?.getStackInHand(Hand.MAIN_HAND)?.getFluidCellVariant()?.fluid
            ?: player?.getStackInHand(Hand.OFF_HAND)?.getFluidCellVariant()?.fluid
            ?: return false
        // 蒸馏水接触世界后视为被污染，直接按普通水放置
        val fluid = if (rawFluid == ModFluids.DISTILLED_WATER_STILL || rawFluid == ModFluids.DISTILLED_WATER_FLOWING) {
            Fluids.WATER
        } else rawFluid

        // FluidFillable：如炼药锅等可注入液体的方块
        if (block is FluidFillable) {
            if (block.canFillWithFluid(player, world, pos, state, fluid)) {
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

    override fun getRecipeRemainder(stack: ItemStack): ItemStack = ItemStack(cellItem("empty_cell"))

    override fun getName(stack: ItemStack): Text {
        val variant = stack.getFluidCellVariant()
        if (variant != null && !variant.isBlank) {
            val fluid = variant.fluid
            val fluidName = fluid.defaultState.blockState.block.translationKey
            return Text.translatable("item.ic2_120.fluid_cell.filled", Text.translatable(fluidName))
        }
        return super.getName(stack)
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        if (stack.isFluidCellEmpty()) {
            tooltip.add(Text.translatable("tooltip.ic2_120.fluid_cell.empty_hint").formatted(Formatting.GRAY))
        }
    }
}


// ========== 容器类 - 单元 ==========

private fun cellItem(id: String): Item = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, id))

/**
 * 空单元：通过 Fabric FluidStorage 与世界/方块流体交互（收集与转移）。
 * 使用 use() + RaycastContext.FluidHandling.ANY 射线检测，才能命中流体方块（默认射线会穿透水/岩浆）。
 */
abstract class EmptyCellItem(settings: Item.Settings) : Item(settings) {

    private fun interactAt(world: World, user: net.minecraft.entity.player.PlayerEntity, hand: Hand, hit: BlockHitResult): Boolean {
        val sided = FluidStorage.SIDED.find(world, hit.blockPos, hit.side) ?: return false
        return FluidStorageUtil.interactWithFluidStorage(sided, user, hand)
    }

    private fun resolveFluidSourcePos(world: World, hit: BlockHitResult): BlockPos? {
        val pos = hit.blockPos
        val posState = world.getBlockState(pos)
        if (!posState.fluidState.isEmpty && posState.fluidState.isStill) return pos

        val adjacent = pos.offset(hit.side)
        val adjacentState = world.getBlockState(adjacent)
        if (!adjacentState.fluidState.isEmpty && adjacentState.fluidState.isStill) return adjacent

        return null
    }

    private fun pickupWorldFluid(
        world: World,
        user: net.minecraft.entity.player.PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): Boolean {
        val sourcePos = resolveFluidSourcePos(world, hit) ?: return false
        val state = world.getBlockState(sourcePos)
        val fluidState = state.fluidState
        if (fluidState.isEmpty || !fluidState.isStill) return false

        val fluid = fluidState.fluid
        val stack = user.getStackInHand(hand)
        val filled = fluidToFilledCellStack(fluid)

        if (state.block is FluidDrainable) {
            val drained = (state.block as FluidDrainable).tryDrainFluid(user, world, sourcePos, state)
            if (drained.isEmpty) return false
        } else {
            if (!world.setBlockState(sourcePos, net.minecraft.block.Blocks.AIR.defaultState)) return false
        }

        fluid.getBucketFillSound().ifPresent { world.playSound(user, sourcePos, it, SoundCategory.BLOCKS, 1f, 1f) }
        world.emitGameEvent(user, GameEvent.FLUID_PICKUP, sourcePos)

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
        return true
    }

    override fun use(world: World, user: net.minecraft.entity.player.PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        // 使用 FluidHandling.ANY 射线检测，才能命中流体方块（原版 useOnBlock 的射线会穿透水/岩浆）
        val hitResult = raycast(world, user, RaycastContext.FluidHandling.ANY)
        if (hitResult.type != HitResult.Type.BLOCK || hitResult !is BlockHitResult) {
            return TypedActionResult.pass(stack)
        }

        if (pickupWorldFluid(world, user, hand, hitResult)) {
            return TypedActionResult.success(user.getStackInHand(hand))
        }
        if (interactAt(world, user, hand, hitResult)) {
            return TypedActionResult.success(user.getStackInHand(hand))
        }
        return TypedActionResult.pass(stack)
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val player = context.player ?: return ActionResult.PASS
        val hand = context.hand
        if (world.isClient) return ActionResult.SUCCESS

        val hit = BlockHitResult(context.hitPos, context.side, context.blockPos, context.hitsInsideBlock())
        if (pickupWorldFluid(world, player, hand, hit)) {
            return ActionResult.SUCCESS
        }
        if (interactAt(world, player, hand, hit)) {
            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }

    /** 将桶物品映射为对应的满流体单元 ItemStack（带 NBT），无法映射则返回 null */
    protected abstract fun mapBucketToFilledCell(bucketStack: ItemStack): ItemStack?
}

// ========== 本模组流体单元基类 ==========

/**
 * 本模组流体单元基类。
 * 每个流体单元对应一个特定流体，可直接用于合成表。
 *
 * 子类只需实现 getFluid() 方法，其他功能（右键放置、与储罐交互）由基类提供。
 */
abstract class ModFluidCell(settings: Item.Settings) : Item(settings), FluidModificationItem {

    /** 子类实现：返回对应的流体 */
    abstract fun getFluid(): Fluid

    /** 子类实现：返回对应的空单元物品（用于配方剩余物） */
    internal abstract fun getEmptyCell(): Item

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val player = context.player ?: return ActionResult.PASS
        val hand = context.hand
        val stack = player.getStackInHand(hand)
        if (world.isClient) return ActionResult.SUCCESS

        // 注意：与 FluidStorage 的交互已在 UseBlockCallback 中处理，这里只处理放置流体到世界
        // 放置流体到世界
        val hitResult = BlockHitResult(context.hitPos, context.side, pos, context.hitsInsideBlock())
        if (placeFluid(player, world, pos, hitResult)) {
            if (!player.abilities.creativeMode) {
                stack.decrement(1)
                val empty = ItemStack(getEmptyCell())
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
        // 蒸馏水单元放出到世界时直接转为普通水
        val rawFluid = getFluid()
        val fluid = if (rawFluid == ModFluids.DISTILLED_WATER_STILL || rawFluid == ModFluids.DISTILLED_WATER_FLOWING) {
            Fluids.WATER
        } else rawFluid
        val state = world.getBlockState(pos)
        val block = state.block

        // FluidFillable：如炼药锅等可注入液体的方块
        if (block is FluidFillable) {
            if (block.canFillWithFluid(player, world, pos, state, fluid)) {
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

    override fun getRecipeRemainder(stack: ItemStack): ItemStack = ItemStack(getEmptyCell())
}

// ========== 空单元 ==========

@ModItem(name = "empty_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class EmptyCell : EmptyCellItem(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val tin = TinCasing::class.instance()
            if (tin != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EmptyCell::class.instance(), 1)
                    .pattern(" T ")
                    .pattern("TGT")
                    .pattern(" T ")
                    .input('T', tin)
                    .input('G', Items.GLASS_PANE)
                    .criterion(hasItem(tin), conditionsFromItem(tin))
                    .offerTo(exporter, EmptyCell::class.id())
            }

            val fluidCell = FluidCellItem::class.instance()
            if (fluidCell != Items.AIR) {
                EmptyFluidCellToEmptyCellRecipeDatagen.offer(
                    exporter = exporter,
                    recipeId = EmptyCell::class.recipeId("from_fluid_cell"),
                    input = fluidCell,
                    result = EmptyCell::class.instance(),
                    category = "misc"
                )
            }
        }
    }

    override fun mapBucketToFilledCell(bucketStack: ItemStack): ItemStack? {
        // 优先映射到本模组流体单元
        return when (bucketStack.item) {
            Items.WATER_BUCKET -> ItemStack(cellItem("water_cell"))
            ModFluids.DISTILLED_WATER_BUCKET -> ItemStack(cellItem("distilled_water_cell"))
            Items.LAVA_BUCKET -> ItemStack(cellItem("lava_cell"))
            ModFluids.COOLANT_BUCKET -> ItemStack(cellItem("coolant_cell"))
            ModFluids.HOT_COOLANT_BUCKET -> ItemStack(cellItem("hot_coolant_cell"))
            ModFluids.UU_MATTER_BUCKET -> ItemStack(cellItem("uu_matter_cell"))
            ModFluids.WEED_EX_BUCKET -> ItemStack(cellItem("weed_ex_cell"))
            ModFluids.PAHOEHOE_LAVA_BUCKET -> ItemStack(cellItem("pahoehoe_lava_cell"))
            ModFluids.BIOFUEL_BUCKET -> ItemStack(cellItem("biofuel_cell"))
            ModFluids.BIOMASS_BUCKET -> ItemStack(cellItem("biomass_cell"))
            else -> {
                // 其他mod流体使用通用fluid_cell
                bucketToFilledFluidCell(bucketStack)
            }
        }
    }
}

// ========== 本模组流体单元实现 ==========

/** 水单元 */
@ModItem(name = "water_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class WaterCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = Fluids.WATER
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

/** 蒸馏水单元（放出世界会污染成普通水） */
@ModItem(name = "distilled_water_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class DistilledWaterCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = ModFluids.DISTILLED_WATER_STILL
    override fun getEmptyCell(): Item = cellItem("empty_cell")

    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        tooltip.add(Text.translatable("tooltip.ic2_120.distilled_water_places_water").formatted(Formatting.GRAY))
    }
}

/** 岩浆单元 */
@ModItem(name = "lava_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class LavaCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = Fluids.LAVA
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

/** 冷却液单元 */
@ModItem(name = "coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class CoolantCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = ModFluids.COOLANT_STILL
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

/** 热冷却液单元 */
@ModItem(name = "hot_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class HotCoolantCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = ModFluids.HOT_COOLANT_STILL
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

/** UU物质单元 */
@ModItem(name = "uu_matter_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class UuMatterCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = ModFluids.UU_MATTER_STILL
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

/** 除草剂单元 */
@ModItem(name = "weed_ex_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class WeedExCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = ModFluids.WEED_EX_STILL
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

/** 熔岩岩浆单元 */
@ModItem(name = "pahoehoe_lava_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class PahoehoeLavaCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = ModFluids.PAHOEHOE_LAVA_STILL
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

/** 生物燃料单元 */
@ModItem(name = "biofuel_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class BiofuelCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = ModFluids.BIOFUEL_STILL
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

/** 生物质单元 */
@ModItem(name = "biomass_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class BiomassCell : ModFluidCell(Item.Settings()) {
    override fun getFluid(): Fluid = ModFluids.BIOMASS_STILL
    override fun getEmptyCell(): Item = cellItem("empty_cell")
}

// ========== 特殊单元（非流体） ==========

@ModItem(name = "air_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class AirCell : Item(Item.Settings())

@ModItem(name = "bio_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class BioCell : Item(Item.Settings())

// ========== 容器类 - 桶 ==========
// construction_foam_bucket 由 ModFluids.registerFluid("construction_foam") 注册为 Ic2BucketItem

// @ModItem(name = "construct_foam_bucket", tab = CreativeTab.IC2_MATERIALS, group = "buckets")
// class ConstructFoamBucket : Item(Item.Settings())

// coolant_bucket, hot_coolant_bucket, uu_matter_bucket, weed_ex_bucket, pahoehoe_lava_bucket,
// biofuel_bucket, biomass_bucket 由 ModFluids 注册为 BucketItem，此处不再重复注册

// ========== 建筑泡沫类 ==========
/** 建筑泡沫粉 */
@ModItem(name = "cf_powder", tab = CreativeTab.IC2_MATERIALS, group = "construction_foam")
class CfPowder : Item(Item.Settings()){
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 合成配方：6×石粉 + 2×沙子 + 1×黏土球 → 1×建筑泡沫粉（cf_powder），与 IC2 经典摆法一致
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CfPowder::class.instance(), 1)
                .pattern("dsd")
                .pattern("dcd")
                .pattern("dsd")
                .input('d', StoneDust::class.instance())
                .input('s', Items.SAND)
                .input('c', Items.CLAY_BALL)
                .criterion(hasItem(StoneDust::class.instance()), conditionsFromItem(StoneDust::class.instance()))
                .criterion(hasItem(Items.SAND), conditionsFromItem(Items.SAND))
                .criterion(hasItem(Items.CLAY_BALL), conditionsFromItem(Items.CLAY_BALL))
                .offerTo(exporter, CfPowder::class.id())
        }
    }
}

//todo 暂时不注册
// @ModItem(name = "pellet", tab = CreativeTab.IC2_MATERIALS, group = "construction_foam")
class Pellet : Item(Item.Settings())

/**
 * ModFluidCell 的 FluidStorage：满单元可倒出 1 桶对应流体，倒出后变为空单元。
 * 用于右键储罐等容器时，Fabric Transfer API 能识别并执行流体转移。
 */
private class ModFluidCellStorage(
    private val ctx: ContainerItemContext,
    private val modCell: ModFluidCell
) : Storage<FluidVariant> {

    private val fluid = modCell.getFluid()
    private val emptyCell = modCell.getEmptyCell()

    override fun supportsInsertion(): Boolean = false

    override fun supportsExtraction(): Boolean = true

    override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0

    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount < FluidConstants.BUCKET) return 0
        if (resource.fluid != fluid) return 0

        val emptyStack = ItemStack(emptyCell)
        val newVariant = ItemVariant.of(emptyStack)
        if (ctx.exchange(newVariant, 1, transaction) == 1L) return FluidConstants.BUCKET
        return 0
    }

    override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
        val variant = FluidVariant.of(fluid)
        return mutableListOf(object : StorageView<FluidVariant> {
            override fun getResource(): FluidVariant = variant
            override fun getAmount(): Long = FluidConstants.BUCKET
            override fun getCapacity(): Long = FluidConstants.BUCKET
            override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                this@ModFluidCellStorage.extract(resource, maxAmount, transaction)
            override fun isResourceBlank(): Boolean = false
        }).iterator() as MutableIterator<StorageView<FluidVariant>>
    }
}

/**
 * 通用流体单元的 FluidStorage：空单元可装任意流体，满单元可倒出对应流体。
 */
private class FluidCellStorage(private val ctx: ContainerItemContext) : Storage<FluidVariant> {

    private val fluidCell = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "fluid_cell"))
    private val emptyCell = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell"))

    override fun supportsInsertion(): Boolean = true

    override fun supportsExtraction(): Boolean = true

    override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount < FluidConstants.BUCKET) return 0

        val current = ctx.itemVariant
        if (current.item != emptyCell) return 0

        val filledStack = fluidToFilledCellStack(resource.fluid)
        val newVariant = ItemVariant.of(filledStack)
        if (ctx.exchange(newVariant, 1, transaction) == 1L) return FluidConstants.BUCKET
        return 0
    }

    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount < FluidConstants.BUCKET) return 0

        val current = ctx.itemVariant
        if (current.item != fluidCell) return 0

        val stored = (current.components?.get(DataComponentTypes.CUSTOM_DATA) as? NbtComponent)?.copyNbt()?.getCompound(FLUID_CELL_NBT_KEY)?.let { FluidVariant.CODEC.decode(NbtOps.INSTANCE, it).result().map { p -> p.first }.orElse(FluidVariant.blank()) } ?: return 0
        if (!resource.equals(stored)) return 0

        val emptyStack = ItemStack(emptyCell)
        val newVariant = ItemVariant.of(emptyStack)
        if (ctx.exchange(newVariant, 1, transaction) == 1L) return FluidConstants.BUCKET
        return 0
    }

    override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
        val current = ctx.itemVariant
        if (current.item != fluidCell) return mutableListOf<StorageView<FluidVariant>>().iterator() as MutableIterator<StorageView<FluidVariant>>
        val stored = (current.components?.get(DataComponentTypes.CUSTOM_DATA) as? NbtComponent)?.copyNbt()?.getCompound(FLUID_CELL_NBT_KEY)?.let { FluidVariant.CODEC.decode(NbtOps.INSTANCE, it).result().map { p -> p.first }.orElse(FluidVariant.blank()) } ?: return mutableListOf<StorageView<FluidVariant>>().iterator() as MutableIterator<StorageView<FluidVariant>>
        if (stored.isBlank()) return mutableListOf<StorageView<FluidVariant>>().iterator() as MutableIterator<StorageView<FluidVariant>>
        return mutableListOf(object : StorageView<FluidVariant> {
            override fun getResource(): FluidVariant = stored
            override fun getAmount(): Long = FluidConstants.BUCKET
            override fun getCapacity(): Long = FluidConstants.BUCKET
            override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                this@FluidCellStorage.extract(resource, maxAmount, transaction)
            override fun isResourceBlank(): Boolean = stored.isBlank()
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
        val emptyCell = Registries.ITEM.get(Identifier.of(modId, "empty_cell"))
        val fluidCell = Registries.ITEM.get(Identifier.of(modId, "fluid_cell"))

        // 空单元 + 满流体单元：通用 FluidStorage（支持任意流体）
        FluidStorage.ITEM.registerForItems({ _, ctx -> FluidCellStorage(ctx) }, emptyCell)
        FluidStorage.ITEM.registerForItems({ _, ctx -> FluidCellStorage(ctx) }, fluidCell)

        // ModFluidCell 子类：专用流体单元，需注册 FluidStorage 才能右键储罐添加流体
        val modFluidCellIds = listOf(
            "water_cell", "distilled_water_cell", "lava_cell", "coolant_cell", "hot_coolant_cell",
            "uu_matter_cell", "weed_ex_cell", "pahoehoe_lava_cell", "biofuel_cell", "biomass_cell"
        )
        for (id in modFluidCellIds) {
            val item = Registries.ITEM.get(Identifier.of(modId, id))
            if (item is ModFluidCell) {
                FluidStorage.ITEM.registerForItems({ _, ctx -> ModFluidCellStorage(ctx, item) }, item)
            }
        }

        val foamSprayer = Registries.ITEM.get(Identifier.of(modId, "foam_sprayer"))
        FluidStorage.ITEM.registerForItems({ _, ctx -> FoamSprayerFluidStorage(ctx) }, foamSprayer)

        // 遍历所有流体，将满流体单元注册到创造模式物品栏（IC2 材料）
        // 排除已有独立单元类的本mod流体
        val modFluidCells = setOf(
            Fluids.WATER,
            Fluids.LAVA,
            ModFluids.DISTILLED_WATER_STILL,
            ModFluids.COOLANT_STILL,
            ModFluids.HOT_COOLANT_STILL,
            ModFluids.UU_MATTER_STILL,
            ModFluids.WEED_EX_STILL,
            ModFluids.PAHOEHOE_LAVA_STILL,
            ModFluids.BIOFUEL_STILL,
            ModFluids.BIOMASS_STILL
        )

        val ic2MaterialsKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(modId, CreativeTab.IC2_MATERIALS.id))
        ItemGroupEvents.modifyEntriesEvent(ic2MaterialsKey).register { entries ->
            val fluids = Registries.FLUID.filter { fluid ->
                fluid != Fluids.EMPTY &&
                fluid != Fluids.FLOWING_LAVA &&
                fluid != Fluids.FLOWING_WATER &&
                fluid !in modFluidCells  // 排除已有独立单元类的流体
            }.sortedBy { Registries.FLUID.getId(it).toString() }
            for (fluid in fluids) {
                val fluidId = Registries.FLUID.getId(fluid)
                logger.info("注册通用流体单元到创造模式物品栏: {}", fluidId)
                val stack = ItemStack(fluidCell).apply { setFluidCellVariant(FluidVariant.of(fluid)) }
                entries.addAfter(fluidCell, stack)
            }
        }
    }
}
