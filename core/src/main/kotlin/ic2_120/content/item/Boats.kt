package ic2_120.content.item

import ic2_120.compat.save

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.has

import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.entity.ModEntities
import ic2_120.content.recipes.ModTags
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.CreativeTab
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory

/** 破损的橡皮艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "broken_rubber_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class BrokenRubberBoatItem : Ic2BoatItem(ModEntities.BROKEN_RUBBER_BOAT, Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.TRANSPORTATION, RubberBoatItem::class.instance(), 1)
                .requires(BrokenRubberBoatItem::class.instance())
                .requires(RubberItem::class.instance())
                .unlockedBy(getHasName(BrokenRubberBoatItem::class.instance()), has(BrokenRubberBoatItem::class.instance()))
                .save(exporter, BrokenRubberBoatItem::class.recipeId("repair_to_rubber_boat"))
        }
    }
}

/** 碳纤维轻艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "carbon_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class CarbonBoatItem : Ic2BoatItem(ModEntities.CARBON_BOAT, Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = CarbonPlate::class.instance()
            if (plate != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.TRANSPORTATION, CarbonBoatItem::class.instance(), 1)
                    .pattern("   ")
                    .pattern("P P")
                    .pattern("PPP")
                    .define('P', plate)
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, CarbonBoatItem::class.id())
            }
        }
    }
}

/** 橡皮艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "rubber_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class RubberBoatItem : Ic2BoatItem(ModEntities.RUBBER_BOAT, Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val rubber = RubberItem::class.instance()
            if (rubber != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.TRANSPORTATION, RubberBoatItem::class.instance(), 1)
                    .pattern("   ")
                    .pattern("R R")
                    .pattern("RRR")
                    .define('R', ic2_120.compat.tagIngredient(ModTags.Compat.Items.RUBBER))
                    .unlockedBy(getHasName(rubber), has(rubber))
                    .save(exporter, RubberBoatItem::class.id())
            }
        }
    }
}

/** 电动艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "electric_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class ElectricBoatItem : Ic2BoatItem(ModEntities.ELECTRIC_BOAT, Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val cable = InsulatedCopperCableBlock::class.item()
            val plate = IronPlate::class.instance()
            val motor = ElectricMotor::class.instance()
            val rotor = IronRotor::class.instance()
            if (cable != Items.AIR && plate != Items.AIR && motor != Items.AIR && rotor != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.TRANSPORTATION, ElectricBoatItem::class.instance(), 1)
                    .pattern("CCC")
                    .pattern("IMI")
                    .pattern("IRI")
                    .define('C', cable)
                    .define('I', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_IRON))
                    .define('M', motor)
                    .define('R', rotor)
                    .unlockedBy(getHasName(cable), has(cable))
                    .save(exporter, ElectricBoatItem::class.id())
            }
        }
    }
}
