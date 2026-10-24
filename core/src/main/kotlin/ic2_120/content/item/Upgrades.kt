package ic2_120.content.item

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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
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
import net.minecraft.client.item.TooltipContext
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Container
import net.minecraft.network.FriendlyByteBuf
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

abstract class FluidFilterUpgradeItem : Item(Item.Settings()), IUpgradeItem {
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: Level?, tooltip: MutableList<Component>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val filter = FluidPipeUpgradeComponent.readFilter(stack)
        if (filter != null) {
            val fluidName = filter.defaultState.blockState.block.name
            tooltip.add(Component.literal("过滤: ").append(fluidName).formatted(ChatFormatting.GRAY))
        } else {
            tooltip.add(Component.literal("过滤: 未设置").formatted(ChatFormatting.GRAY))
        }
        val sides = FluidPipeUpgradeComponent.readDirections(stack)
        tooltip.add(Component.literal("方向: ${directionLabel(sides)}").formatted(ChatFormatting.GRAY))
    }

    override fun use(world: Level, user: net.minecraft.world.entity.player.Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return InteractionResultHolder.success(stack)

        if (user.isSneaking) {
            val current = FluidPipeUpgradeComponent.readDirections(stack)
            val next = FluidPipeUpgradeComponent.nextDirections(current)
            FluidPipeUpgradeComponent.writeDirections(stack, next)
            if (user is ServerPlayer) {
                user.sendMessage(Component.literal("已设置工作方向: ${directionLabel(next)}"), true)
            }
            return InteractionResultHolder.success(stack)
        }

        // 打开配置 GUI
        if (user is ServerPlayer) {
            user.openHandledScreen(object : ExtendedScreenHandlerFactory {
                override fun getDisplayName(): Component = Component.translatable("gui.ic2_120.fluid_upgrade.title")
                override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
                    buf.writeEnumConstant(hand)
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

abstract class ItemFilterUpgradeItem : Item(Item.Settings()), IUpgradeItem {

    /** 子类可重写此属性以改变方向设置消息，如 "弹出方向" 或 "抽入方向" */
    protected open val directionActionLabel: String = "弹出方向"

    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: Level?, tooltip: MutableList<Component>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val filterItem = EjectorUpgradeComponent.readFilter(stack)
        if (filterItem != null) {
            tooltip.add(Component.literal("过滤物品: ").append(filterItem.name).formatted(ChatFormatting.GRAY))
        } else {
            tooltip.add(Component.literal("过滤物品: 全部").formatted(ChatFormatting.GRAY))
        }
        val sides = EjectorUpgradeComponent.readDirections(stack)
        tooltip.add(Component.literal("方向: ${directionLabel(sides)}").formatted(ChatFormatting.GRAY))
    }

    override fun use(world: Level, user: net.minecraft.world.entity.player.Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return InteractionResultHolder.success(stack)

        if (user.isSneaking) {
            val current = EjectorUpgradeComponent.readDirections(stack)
            val next = nextDirections(current)
            EjectorUpgradeComponent.writeDirections(stack, next)
            if (user is ServerPlayer) {
                user.sendMessage(Component.literal("已设置$directionActionLabel: ${directionLabel(next)}"), true)
            }
            return InteractionResultHolder.success(stack)
        }

        // 打开配置 GUI
        if (user is ServerPlayer) {
            user.openHandledScreen(object : ExtendedScreenHandlerFactory {
                override fun getDisplayName(): Component = Component.translatable("gui.ic2_120.item_upgrade.title")
                override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
                    buf.writeEnumConstant(hand)
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
class OverclockerUpgrade : Item(Item.Settings()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val cable = InsulatedCopperCableBlock::class.item()
            val circuit = Circuit::class.instance()
            fun offer(coolant: Item, count: Int, suffix: String) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, OverclockerUpgrade::class.instance(), count)
                    .pattern("CCC")
                    .pattern("WRW")
                    .input('C', coolant)
                    .input('W', cable)
                    .input('R', circuit)
                    .criterion(hasItem(coolant), conditionsFromItem(coolant))
                    .offerTo(exporter, OverclockerUpgrade::class.recipeId(suffix))
            }
            offer(ReactorCoolantCellItem::class.instance(), 2, "from_10k_coolant")
            offer(TripleReactorCoolantCellItem::class.instance(), 6, "from_30k_coolant")
            offer(SextupleReactorCoolantCellItem::class.instance(), 12, "from_60k_coolant")
        }
    }
}

@ModItem(name = "transformer_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class TransformerUpgrade : Item(Item.Settings()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val mv = MvTransformerBlock::class.item()
            val goldCable = DoubleInsulatedGoldCableBlock::class.item()
            val circuit = Circuit::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, TransformerUpgrade::class.instance(), 1)
                .pattern("GGG")
                .pattern("WTW")
                .pattern("GRG")
                .input('G', Items.GLASS)
                .input('W', goldCable)
                .input('T', mv)
                .input('R', circuit)
                .criterion(hasItem(mv), conditionsFromItem(mv))
                .offerTo(exporter)
        }
    }
}

@ModItem(name = "energy_storage_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EnergyStorageUpgrade : Item(Item.Settings()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val cable = InsulatedCopperCableBlock::class.item()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, EnergyStorageUpgrade::class.instance(), 1)
                .pattern("PPP")
                .pattern("WBW")
                .pattern("PRP")
                .input('P', ItemTags.PLANKS)
                .input('W', cable)
                .input('B', ReBatteryItem::class.instance())
                .input('R', Circuit::class.instance())
                .criterion(hasItem(ReBatteryItem::class.instance()), conditionsFromItem(ReBatteryItem::class.instance()))
                .offerTo(exporter)
        }
    }
}

@ModItem(name = "redstone_inverter_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class RedstoneInverterUpgrade : Item(Item.Settings()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, RedstoneInverterUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" L ")
                .pattern("T T")
                .input('T', Ingredient.fromTag(ModTags.Compat.Items.PLATES_TIN))
                .input('L', Items.LEVER)
                .criterion(hasItem(tin), conditionsFromItem(tin))
                .offerTo(exporter)
            ShapedRecipeBuilder.create(RecipeCategory.MISC, RedstoneInverterUpgrade::class.instance(), 9)
                .pattern("T T")
                .pattern(" L ")
                .pattern("T T")
                .input('T', dense)
                .input('L', Items.LEVER)
                .criterion(hasItem(dense), conditionsFromItem(dense))
                .offerTo(exporter, RedstoneInverterUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}

@ModItem(name = "ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EjectorUpgrade : ItemFilterUpgradeItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, EjectorUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" P ")
                .pattern("T T")
                .input('T', Ingredient.fromTag(ModTags.Compat.Items.PLATES_TIN))
                .input('P', Items.PISTON)
                .criterion(hasItem(tin), conditionsFromItem(tin))
                .offerTo(exporter)
            ShapedRecipeBuilder.create(RecipeCategory.MISC, EjectorUpgrade::class.instance(), 9)
                .pattern("T T")
                .pattern(" P ")
                .pattern("T T")
                .input('T', dense)
                .input('P', Items.PISTON)
                .criterion(hasItem(dense), conditionsFromItem(dense))
                .offerTo(exporter, EjectorUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}

@ModItem(name = "pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class PullingUpgrade : ItemFilterUpgradeItem() {
    override val directionActionLabel: String = "抽入方向"

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            val hopper = Items.HOPPER
            ShapedRecipeBuilder.create(RecipeCategory.MISC, PullingUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" H ")
                .pattern("T T")
                .input('T', Ingredient.fromTag(ModTags.Compat.Items.PLATES_TIN))
                .input('H', hopper)
                .criterion(hasItem(hopper), conditionsFromItem(hopper))
                .offerTo(exporter)
            ShapedRecipeBuilder.create(RecipeCategory.MISC, PullingUpgrade::class.instance(), 9)
                .pattern("T T")
                .pattern(" H ")
                .pattern("T T")
                .input('T', dense)
                .input('H', hopper)
                .criterion(hasItem(dense), conditionsFromItem(dense))
                .offerTo(exporter, PullingUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}

@ModItem(name = "fluid_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidEjectorUpgrade : FluidFilterUpgradeItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            val motor = ElectricMotor::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, FluidEjectorUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" M ")
                .pattern("T T")
                .input('T', Ingredient.fromTag(ModTags.Compat.Items.PLATES_TIN))
                .input('M', motor)
                .criterion(hasItem(motor), conditionsFromItem(motor))
                .offerTo(exporter)
            ShapedRecipeBuilder.create(RecipeCategory.MISC, FluidEjectorUpgrade::class.instance(), 9)
                .pattern("T T")
                .pattern(" M ")
                .pattern("T T")
                .input('T', dense)
                .input('M', motor)
                .criterion(hasItem(dense), conditionsFromItem(dense))
                .offerTo(exporter, FluidEjectorUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}

@ModItem(name = "fluid_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidPullingUpgrade : FluidFilterUpgradeItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
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
