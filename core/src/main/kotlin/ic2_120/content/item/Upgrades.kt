package ic2_120.content.item

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.isClient

import ic2_120.content.block.MvTransformerBlock
import ic2_120.content.block.cables.DoubleInsulatedGoldCableBlock
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.item.energy.ReBatteryItem
import ic2_120.content.recipes.ModTags
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipeDatagen
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.screen.FluidUpgradeScreenHandler
import ic2_120.content.screen.ItemUpgradeScreenHandler
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.recipes.RecipeProvider.has
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.tags.ItemTags
import java.util.function.Consumer
import net.fabricmc.api.Environment
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.core.Direction
import net.minecraft.world.level.Level


// ========== 升级物品接口 ==========

/**
 * 机器升级物品标记接口。
 * 实现此接口的物品可放入机器的升级槽位。
 * 各升级的实际效果由机器在 tick 中自行读取并应用，此处不定义。
 */
interface IUpgradeItem

abstract class FluidFilterUpgradeItem : Item(Item.Properties()), IUpgradeItem {
    @Environment(EnvType.CLIENT)
    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, type)
        val filter = FluidPipeUpgradeComponent.readFilter(stack)
        if (filter != null) {
            val fluidName: Component = Component.translatable(filter.defaultFluidState().createLegacyBlock().block.descriptionId)
            tooltip.add(Component.literal("过滤: ").append(fluidName).withStyle(ChatFormatting.GRAY))
        } else {
            tooltip.add(Component.literal("过滤: 未设置").withStyle(ChatFormatting.GRAY))
        }
        val sides = FluidPipeUpgradeComponent.readDirections(stack)
        tooltip.add(Component.literal("方向: ${directionLabel(sides)}").withStyle(ChatFormatting.GRAY))
    }

    override fun use(world: Level, user: net.minecraft.world.entity.player.Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)
        if (world.isClient) return InteractionResultHolder.success(stack)

        if (user.isCrouching) {
            val current = FluidPipeUpgradeComponent.readDirections(stack)
            val next = FluidPipeUpgradeComponent.nextDirections(current)
            FluidPipeUpgradeComponent.writeDirections(stack, next)
            if (user is ServerPlayer) {
                user.displayClientMessage(Component.literal("已设置工作方向: ${directionLabel(next)}"), true)
            }
            return InteractionResultHolder.success(stack)
        }

        // 打开配置 GUI
        if (user is ServerPlayer) {
            user.openMenu(object : ExtendedScreenHandlerFactory<FriendlyByteBuf> {
                override fun getDisplayName(): Component = Component.translatable("gui.ic2_120.fluid_upgrade.title")
                override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
                    val buf = FriendlyByteBuf(Unpooled.buffer())
                        buf.writeEnum(hand)
                    return buf
                }
                override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
                    return FluidUpgradeScreenHandler(syncId, playerInventory, hand)
                }
            })
        }
        return InteractionResultHolder.success(stack)
    }

    private fun directionLabel(sides: Set<Direction>): String = if (sides.isEmpty()) {
        "任意"
    } else {
        sides.joinToString(", ") { when (it) {
            Direction.DOWN -> "下"
            Direction.UP -> "上"
            Direction.NORTH -> "北"
            Direction.SOUTH -> "南"
            Direction.WEST -> "西"
            Direction.EAST -> "东"
        }}
    }
}

abstract class ItemFilterUpgradeItem : Item(Item.Properties()), IUpgradeItem {

    /** 子类可重写此属性以改变方向设置消息，如 "弹出方向" 或 "抽入方向" */
    protected open val directionActionLabel: String = "弹出方向"

    @Environment(EnvType.CLIENT)
    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, type)
        val filterItem = EjectorUpgradeComponent.readFilter(stack)
        if (filterItem != null) {
            tooltip.add(Component.literal("过滤物品: ").append(Component.translatable(filterItem.descriptionId)).withStyle(ChatFormatting.GRAY))
        } else {
            tooltip.add(Component.literal("过滤物品: 全部").withStyle(ChatFormatting.GRAY))
        }
        val sides = EjectorUpgradeComponent.readDirections(stack)
        tooltip.add(Component.literal("方向: ${directionLabel(sides)}").withStyle(ChatFormatting.GRAY))
    }

    override fun use(world: Level, user: net.minecraft.world.entity.player.Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)
        if (world.isClient) return InteractionResultHolder.success(stack)

        if (user.isCrouching) {
            val current = EjectorUpgradeComponent.readDirections(stack)
            val next = nextDirections(current)
            EjectorUpgradeComponent.writeDirections(stack, next)
            if (user is ServerPlayer) {
                user.displayClientMessage(Component.literal("已设置$directionActionLabel: ${directionLabel(next)}"), true)
            }
            return InteractionResultHolder.success(stack)
        }

        // 打开配置 GUI
        if (user is ServerPlayer) {
            user.openMenu(object : ExtendedScreenHandlerFactory<FriendlyByteBuf> {
                override fun getDisplayName(): Component = Component.translatable("gui.ic2_120.item_upgrade.title")
                override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
                    val buf = FriendlyByteBuf(Unpooled.buffer())
                        buf.writeEnum(hand)
                    return buf
                }
                override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
                    return ItemUpgradeScreenHandler(syncId, playerInventory, hand)
                }
            })
        }
        return InteractionResultHolder.success(stack)
    }

    private fun directionLabel(sides: Set<Direction>): String = if (sides.isEmpty()) {
        "任意"
    } else {
        sides.joinToString(", ") { side -> when (side) {
            Direction.DOWN -> "下"
            Direction.UP -> "上"
            Direction.NORTH -> "北"
            Direction.SOUTH -> "南"
            Direction.WEST -> "西"
            Direction.EAST -> "东"
        }}
    }

    private fun nextDirections(current: Set<Direction>): Set<Direction> {
        val order = listOf(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)
        if (current.size >= order.size) return emptySet()
        return current + order.first { it !in current }
    }
}

// ========== 工具升级类 ==========

@ModItem(name = "overclocker_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class OverclockerUpgrade : Item(Item.Properties()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val cable = InsulatedCopperCableBlock::class.item()
            val circuit = Circuit::class.instance()
            fun offer(coolant: Item, count: Int, suffix: String) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OverclockerUpgrade::class.instance(), count)
                    .pattern("CCC")
                    .pattern("WRW")
                    .define('C', coolant)
                    .define('W', cable)
                    .define('R', circuit)
                    .unlockedBy(getHasName(coolant), has(coolant))
                    .save(exporter, OverclockerUpgrade::class.recipeId(suffix))
            }
            offer(ReactorCoolantCellItem::class.instance(), 2, "from_10k_coolant")
            offer(TripleReactorCoolantCellItem::class.instance(), 6, "from_30k_coolant")
            offer(SextupleReactorCoolantCellItem::class.instance(), 12, "from_60k_coolant")
        }
    }
}

@ModItem(name = "transformer_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class TransformerUpgrade : Item(Item.Properties()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val mv = MvTransformerBlock::class.item()
            val goldCable = DoubleInsulatedGoldCableBlock::class.item()
            val circuit = Circuit::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TransformerUpgrade::class.instance(), 1)
                .pattern("GGG")
                .pattern("WTW")
                .pattern("GRG")
                .define('G', Items.GLASS)
                .define('W', goldCable)
                .define('T', mv)
                .define('R', circuit)
                .unlockedBy(getHasName(mv), has(mv))
                .save(exporter)
        }
    }
}

@ModItem(name = "energy_storage_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EnergyStorageUpgrade : Item(Item.Properties()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val cable = InsulatedCopperCableBlock::class.item()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EnergyStorageUpgrade::class.instance(), 1)
                .pattern("PPP")
                .pattern("WBW")
                .pattern("PRP")
                .define('P', ItemTags.PLANKS)
                .define('W', cable)
                .define('B', ReBatteryItem::class.instance())
                .define('R', Circuit::class.instance())
                .unlockedBy(getHasName(ReBatteryItem::class.instance()), has(ReBatteryItem::class.instance()))
                .save(exporter)
        }
    }
}

@ModItem(name = "redstone_inverter_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class RedstoneInverterUpgrade : Item(Item.Properties()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RedstoneInverterUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" L ")
                .pattern("T T")
                .define('T', Ingredient.of(ModTags.Compat.Items.PLATES_TIN))
                .define('L', Items.LEVER)
                .unlockedBy(getHasName(tin), has(tin))
                .save(exporter)
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RedstoneInverterUpgrade::class.instance(), 9)
                .pattern("T T")
                .pattern(" L ")
                .pattern("T T")
                .define('T', dense)
                .define('L', Items.LEVER)
                .unlockedBy(getHasName(dense), has(dense))
                .save(exporter, RedstoneInverterUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}

@ModItem(name = "ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EjectorUpgrade : ItemFilterUpgradeItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EjectorUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" P ")
                .pattern("T T")
                .define('T', Ingredient.of(ModTags.Compat.Items.PLATES_TIN))
                .define('P', Items.PISTON)
                .unlockedBy(getHasName(tin), has(tin))
                .save(exporter)
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EjectorUpgrade::class.instance(), 9)
                .pattern("T T")
                .pattern(" P ")
                .pattern("T T")
                .define('T', dense)
                .define('P', Items.PISTON)
                .unlockedBy(getHasName(dense), has(dense))
                .save(exporter, EjectorUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}

@ModItem(name = "pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class PullingUpgrade : ItemFilterUpgradeItem() {
    override val directionActionLabel: String = "抽入方向"

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            val hopper = Items.HOPPER
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PullingUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" H ")
                .pattern("T T")
                .define('T', Ingredient.of(ModTags.Compat.Items.PLATES_TIN))
                .define('H', hopper)
                .unlockedBy(getHasName(hopper), has(hopper))
                .save(exporter)
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PullingUpgrade::class.instance(), 9)
                .pattern("T T")
                .pattern(" H ")
                .pattern("T T")
                .define('T', dense)
                .define('H', hopper)
                .unlockedBy(getHasName(dense), has(dense))
                .save(exporter, PullingUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}

@ModItem(name = "fluid_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidEjectorUpgrade : FluidFilterUpgradeItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            val motor = ElectricMotor::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, FluidEjectorUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" M ")
                .pattern("T T")
                .define('T', Ingredient.of(ModTags.Compat.Items.PLATES_TIN))
                .define('M', motor)
                .unlockedBy(getHasName(motor), has(motor))
                .save(exporter)
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, FluidEjectorUpgrade::class.instance(), 9)
                .pattern("T T")
                .pattern(" M ")
                .pattern("T T")
                .define('T', dense)
                .define('M', motor)
                .unlockedBy(getHasName(dense), has(dense))
                .save(exporter, FluidEjectorUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}

@ModItem(name = "fluid_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidPullingUpgrade : FluidFilterUpgradeItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            val tap = Treetap::class.instance()
            val motor = ElectricMotor::class.instance()
            ConsumeTreetapShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = FluidPullingUpgrade::class.id(),
                result = FluidPullingUpgrade::class.instance(),
                pattern = listOf("TWT", " M ", "T T"),
                keys = mapOf('T' to tin, 'W' to tap, 'M' to motor)
            )
            ConsumeTreetapShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = FluidPullingUpgrade::class.recipeId("from_dense_tin"),
                result = FluidPullingUpgrade::class.instance(),
                pattern = listOf("TWT", " M ", "T T"),
                keys = mapOf('T' to dense, 'W' to tap, 'M' to motor),
                count = 9
            )
        }
    }
}
