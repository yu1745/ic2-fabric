package ic2_120.content.item

import ic2_120.compat.save

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.has

import ic2_120.compat.isClient

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
import ic2_120.content.recipes.ModTags
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import ic2_120.registry.annotation.RecipeProvider

// ========== 金属成型机制品 ==========

@ModItem(name = "tin_can", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class EmptyTinCanItem : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val tinIngot = TinIngot::class.instance()

            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, EmptyTinCanItem::class.instance(), 16)
                .pattern("TTT").pattern("T T").pattern("TTT")
                .define('T', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_TIN))
                .unlockedBy(getHasName(tinIngot), has(tinIngot))
                .save(exporter, EmptyTinCanItem::class.recipeId("from_ingots_8"))

            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, EmptyTinCanItem::class.instance(), 4)
                .pattern(" T ").pattern("T T").pattern("TTT")
                .define('T', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_TIN))
                .unlockedBy(getHasName(tinIngot), has(tinIngot))
                .save(exporter, EmptyTinCanItem::class.recipeId("from_ingots_6"))
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
    Item.Properties()
        .food(
            FoodProperties.Builder()
                .nutrition(2)
                .saturationModifier(0.2f)
                .alwaysEdible()
                .build(),
            net.minecraft.world.item.component.Consumable.builder().consumeSeconds(0.5f).build()
        )
) {

    private val emptyTinCanItem: Item
        get() = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "tin_can"))

    /** 10 tick ≈ 1秒吃2个） */
    override fun getUseDuration(stack: ItemStack, user: LivingEntity): Int = 10

    override fun finishUsingItem(stack: ItemStack, world: Level, user: LivingEntity): ItemStack {
        if (!world.isClient && user is Player) {
            val player = user
            if (player.health < player.maxHealth) {
                player.heal(2f)
            } else {
                player.foodData.eat(2, 0.4f)
            }
        }

        val remainder = ItemStack(emptyTinCanItem)
        if (stack.count > 1) {
            stack.shrink(1)
            if (user is Player && !user.inventory.add(remainder)) {
                (world as? net.minecraft.server.level.ServerLevel)?.let { user.spawnAtLocation(it, remainder, 0f) }
            }
            return stack
        }
        return remainder
    }

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
        fun generateRecipes(exporter: RecipeOutput) {
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
        fun generateRecipes(exporter: RecipeOutput) {
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
class EmptyFuelRodItem : Item(Item.Properties())

@ModItem(name = "iron_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleIronItem : Item(Item.Properties())

@ModItem(name = "steel_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleSteelItem : Item(Item.Properties())

@ModItem(name = "bronze_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleBronzeItem : Item(Item.Properties())

@ModItem(name = "coin", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class IndustrialCurrencyItem : Item(Item.Properties())
