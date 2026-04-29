package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.recipeId
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.item.energy.BatteryItemBase
import ic2_120.content.item.energy.ReBatteryItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.FoodComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.world.World
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

// ========== 金属成型机制品 ==========

@ModItem(name = "tin_can", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class EmptyTinCanItem : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val tinIngot = TinIngot::class.instance()

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EmptyTinCanItem::class.instance(), 16)
                .pattern("TTT").pattern("T T").pattern("TTT")
                .input('T', tinIngot)
                .criterion(hasItem(tinIngot), conditionsFromItem(tinIngot))
                .offerTo(exporter, EmptyTinCanItem::class.recipeId("from_ingots_8"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EmptyTinCanItem::class.instance(), 4)
                .pattern(" T ").pattern("T T").pattern("TTT")
                .input('T', tinIngot)
                .criterion(hasItem(tinIngot), conditionsFromItem(tinIngot))
                .offerTo(exporter, EmptyTinCanItem::class.recipeId("from_ingots_6"))
        }
    }
}

/**
 * 满锡罐：由空锡罐在固体装罐机内灌装食物获得，也可在宝箱中开出。
 * 右击食用：生命未满时恢复 1 心生命值，生命满时恢复 1 格饥饿值。
 * 一秒能吃2个（10 tick），食用后返还空锡罐。
 */
@ModItem(name = "filled_tin_can", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class FilledTinCanItem : Item(
    FabricItemSettings()
        .food(
            FoodComponent.Builder()
                .hunger(2)
                .saturationModifier(0.2f)
                .snack()
                .alwaysEdible()
                .build()
        )
) {

    private val emptyTinCanItem: Item
        get() = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "tin_can"))

    /** 10 tick ≈ 1秒吃2个） */
    override fun getMaxUseTime(stack: ItemStack): Int = 10

    override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
        if (!world.isClient && user is PlayerEntity) {
            val player = user
            if (player.health < player.maxHealth) {
                player.heal(2f)
            } else {
                player.hungerManager.add(2, 0.4f)
            }
        }

        val remainder = ItemStack(emptyTinCanItem)
        if (stack.count > 1) {
            stack.decrement(1)
            if (user is PlayerEntity && !user.inventory.insertStack(remainder)) {
                user.dropItem(remainder, false)
            }
            return stack
        }
        return remainder
    }

    override fun getRecipeRemainder(stack: ItemStack): ItemStack = ItemStack(emptyTinCanItem)
}

/**
 * 小型驱动把手：合成原料中的充电电池电量会写入成品（[BatteryEnergyShapedRecipeDatagen]）。
 */
@ModItem(name = "small_power_unit", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class SmallPowerUnitItem : BatteryItemBase(
    name = "small_power_unit",
    tier = 1,
    maxCapacity = 30_000,
    canChargeWireless = false
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            //   C F
            // B X M
            //   C F
            // B=充电电池, C=铜线, F=铁质外壳, X=电路板, M=马达
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = SmallPowerUnitItem::class.recipeId("from_crafting"),
                result = SmallPowerUnitItem::class.instance(),
                pattern = listOf(" CF", "BXM", " CF"),
                keys = mapOf<Char, Item>(
                    'B' to ReBatteryItem::class.instance(),
                    'C' to InsulatedCopperCableBlock::class.item(),
                    'F' to IronCasing::class.instance(),
                    'X' to Circuit::class.instance(),
                    'M' to ElectricMotor::class.instance()
                )
            )
        }
    }
}

/**
 * 驱动把手：合成原料中的充电电池电量会写入成品（[BatteryEnergyShapedRecipeDatagen]）。
 */
@ModItem(name = "power_unit", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class PowerUnitItem : BatteryItemBase(
    name = "power_unit",
    tier = 1,
    maxCapacity = 10_000,
    canChargeWireless = false
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // B C F
            // B X M
            // B C F
            // B=充电电池, C=铜线, F=铁质外壳, X=电路板, M=马达
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = PowerUnitItem::class.recipeId("from_crafting"),
                result = PowerUnitItem::class.instance(),
                pattern = listOf("BCF", "BXM", "BCF"),
                keys = mapOf<Char, Item>(
                    'B' to ReBatteryItem::class.instance(),
                    'C' to InsulatedCopperCableBlock::class.item(),
                    'F' to IronCasing::class.instance(),
                    'X' to Circuit::class.instance(),
                    'M' to ElectricMotor::class.instance()
                )
            )
        }
    }
}

@ModItem(name = "fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class EmptyFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "iron_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleIronItem : Item(FabricItemSettings())

@ModItem(name = "steel_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleSteelItem : Item(FabricItemSettings())

@ModItem(name = "bronze_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleBronzeItem : Item(FabricItemSettings())

@ModItem(name = "coin", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class IndustrialCurrencyItem : Item(FabricItemSettings())
