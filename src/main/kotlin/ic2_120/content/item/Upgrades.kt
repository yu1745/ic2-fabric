package ic2_120.content.item

import ic2_120.content.block.MvTransformerBlock
import ic2_120.content.block.cables.DoubleInsulatedGoldCableBlock
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.item.energy.ReBatteryItem
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.tag.ItemTags
import java.util.function.Consumer
import net.fabricmc.api.Environment
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.Direction
import net.minecraft.world.World

private const val PULLING_UPGRADE_NOT_IMPLEMENTED_TOOLTIP = "item.ic2_120.tooltip.pulling_upgrade_not_implemented"
private const val ITEM_EJECTOR_UPGRADE_MINER_ONLY_TOOLTIP = "item.ic2_120.tooltip.item_ejector_upgrade_miner_only"

// ========== 升级物品接口 ==========

/**
 * 机器升级物品标记接口。
 * 实现此接口的物品可放入机器的升级槽位。
 * 各升级的实际效果由机器在 tick 中自行读取并应用，此处不定义。
 */
interface IUpgradeItem

abstract class FluidFilterUpgradeItem : Item(FabricItemSettings()), IUpgradeItem {
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val filter = FluidPipeUpgradeComponent.readFilter(stack)
        if (filter != null) {
            val fluidName = filter.defaultState.blockState.block.name
            tooltip.add(Text.literal("过滤: ").append(fluidName).formatted(Formatting.GRAY))
        } else {
            tooltip.add(Text.literal("过滤: 未设置").formatted(Formatting.GRAY))
        }
        val side = FluidPipeUpgradeComponent.readDirection(stack)
        tooltip.add(Text.literal("方向: ${directionLabel(side)}").formatted(Formatting.GRAY))
    }

    override fun use(world: World, user: net.minecraft.entity.player.PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        if (user.isSneaking) {
            val next = FluidPipeUpgradeComponent.nextDirection(FluidPipeUpgradeComponent.readDirection(stack))
            FluidPipeUpgradeComponent.writeDirection(stack, next)
            if (user is ServerPlayerEntity) {
                user.sendMessage(Text.literal("已设置工作方向: ${directionLabel(next)}"), true)
            }
            return TypedActionResult.success(stack)
        }

        val offhand = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
        val storage = FluidStorage.ITEM.find(offhand, net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(offhand))
        val view = storage?.iterator()?.asSequence()?.firstOrNull { !it.resource.isBlank && it.amount > 0L }
        val fluid = view?.resource?.fluid

        if (fluid == null) {
            if (user is ServerPlayerEntity) {
                user.sendMessage(Text.literal("副手放入含流体容器后右键设置过滤；潜行右键清除"), true)
            }
            return TypedActionResult.fail(stack)
        }

        FluidPipeUpgradeComponent.writeFilter(stack, fluid)
        if (user is ServerPlayerEntity) {
            user.sendMessage(Text.literal("已设置过滤流体: ${fluid.defaultState.blockState.block.name.string}"), true)
        }
        return TypedActionResult.success(stack)
    }

    private fun directionLabel(side: Direction?): String = when (side) {
        null -> "任意"
        Direction.DOWN -> "下"
        Direction.UP -> "上"
        Direction.NORTH -> "北"
        Direction.SOUTH -> "南"
        Direction.WEST -> "西"
        Direction.EAST -> "东"
    }
}

abstract class ItemFilterUpgradeItem : Item(FabricItemSettings()), IUpgradeItem {
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val filterItem = EjectorUpgradeComponent.readFilter(stack)
        if (filterItem != null) {
            tooltip.add(Text.literal("过滤物品: ").append(filterItem.name).formatted(Formatting.GRAY))
        } else {
            tooltip.add(Text.literal("过滤物品: 全部").formatted(Formatting.GRAY))
        }
        val side = EjectorUpgradeComponent.readDirection(stack)
        tooltip.add(Text.literal("方向: ${directionLabel(side)}").formatted(Formatting.GRAY))
        tooltip.add(Text.translatable(ITEM_EJECTOR_UPGRADE_MINER_ONLY_TOOLTIP).formatted(Formatting.GRAY))
    }

    override fun use(world: World, user: net.minecraft.entity.player.PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        if (user.isSneaking) {
            val next = EjectorUpgradeComponent.nextDirection(EjectorUpgradeComponent.readDirection(stack))
            EjectorUpgradeComponent.writeDirection(stack, next)
            if (user is ServerPlayerEntity) {
                user.sendMessage(Text.literal("已设置弹出方向: ${directionLabel(next)}"), true)
            }
            return TypedActionResult.success(stack)
        }

        val offhand = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
        if (offhand.isEmpty) {
            EjectorUpgradeComponent.writeFilter(stack, null)
            if (user is ServerPlayerEntity) {
                user.sendMessage(Text.literal("已清除物品过滤（当前为全部物品）"), true)
            }
            return TypedActionResult.success(stack)
        }

        EjectorUpgradeComponent.writeFilter(stack, offhand.item)
        if (user is ServerPlayerEntity) {
            user.sendMessage(Text.literal("已设置过滤物品: ${offhand.name.string}"), true)
        }
        return TypedActionResult.success(stack)
    }

    private fun directionLabel(side: Direction?): String = when (side) {
        null -> "任意"
        Direction.DOWN -> "下"
        Direction.UP -> "上"
        Direction.NORTH -> "北"
        Direction.SOUTH -> "南"
        Direction.WEST -> "西"
        Direction.EAST -> "东"
    }
}

// ========== 工具升级类 ==========

@ModItem(name = "overclocker_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class OverclockerUpgrade : Item(FabricItemSettings()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val cable = InsulatedCopperCableBlock::class.item()
            val circuit = Circuit::class.instance()
            fun offer(coolant: Item, count: Int, suffix: String) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, OverclockerUpgrade::class.instance(), count)
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
class TransformerUpgrade : Item(FabricItemSettings()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val mv = MvTransformerBlock::class.item()
            val goldCable = DoubleInsulatedGoldCableBlock::class.item()
            val circuit = Circuit::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, TransformerUpgrade::class.instance(), 1)
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
class EnergyStorageUpgrade : Item(FabricItemSettings()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val cable = InsulatedCopperCableBlock::class.item()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EnergyStorageUpgrade::class.instance(), 1)
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
class RedstoneInverterUpgrade : Item(FabricItemSettings()), IUpgradeItem {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RedstoneInverterUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" L ")
                .pattern("T T")
                .input('T', tin)
                .input('L', Items.LEVER)
                .criterion(hasItem(tin), conditionsFromItem(tin))
                .offerTo(exporter)
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RedstoneInverterUpgrade::class.instance(), 9)
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
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EjectorUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" P ")
                .pattern("T T")
                .input('T', tin)
                .input('P', Items.PISTON)
                .criterion(hasItem(tin), conditionsFromItem(tin))
                .offerTo(exporter)
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EjectorUpgrade::class.instance(), 9)
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

@ModItem(name = "advanced_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class AdvancedEjectorUpgrade : ItemFilterUpgradeItem()

@ModItem(name = "pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class PullingUpgrade : Item(FabricItemSettings()), IUpgradeItem {
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(Text.translatable(PULLING_UPGRADE_NOT_IMPLEMENTED_TOOLTIP).formatted(Formatting.GRAY))
    }
}

@ModItem(name = "advanced_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class AdvancedPullingUpgrade : Item(FabricItemSettings()), IUpgradeItem {
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(Text.translatable(PULLING_UPGRADE_NOT_IMPLEMENTED_TOOLTIP).formatted(Formatting.GRAY))
    }
}

@ModItem(name = "fluid_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidEjectorUpgrade : FluidFilterUpgradeItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            val motor = ElectricMotor::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, FluidEjectorUpgrade::class.instance(), 1)
                .pattern("T T")
                .pattern(" M ")
                .pattern("T T")
                .input('T', tin)
                .input('M', motor)
                .criterion(hasItem(motor), conditionsFromItem(motor))
                .offerTo(exporter)
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, FluidEjectorUpgrade::class.instance(), 9)
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
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val tin = TinPlate::class.instance()
            val dense = DenseTinPlate::class.instance()
            val tap = Treetap::class.instance()
            val motor = ElectricMotor::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, FluidPullingUpgrade::class.instance(), 1)
                .pattern("TWT")
                .pattern(" M ")
                .pattern("T T")
                .input('T', tin)
                .input('W', tap)
                .input('M', motor)
                .criterion(hasItem(motor), conditionsFromItem(motor))
                .offerTo(exporter)
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, FluidPullingUpgrade::class.instance(), 9)
                .pattern("TWT")
                .pattern(" M ")
                .pattern("T T")
                .input('T', dense)
                .input('W', tap)
                .input('M', motor)
                .criterion(hasItem(dense), conditionsFromItem(dense))
                .offerTo(exporter, FluidPullingUpgrade::class.recipeId("from_dense_tin"))
        }
    }
}
