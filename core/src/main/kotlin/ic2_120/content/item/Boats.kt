package ic2_120.content.item

import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.entity.ModEntities
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.CreativeTab
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory

/** 破损的橡皮艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "broken_rubber_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class BrokenRubberBoatItem : Ic2BoatItem(ModEntities.BROKEN_RUBBER_BOAT, Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.TRANSPORTATION, RubberBoatItem::class.instance(), 1)
                .input(BrokenRubberBoatItem::class.instance())
                .input(RubberItem::class.instance())
                .criterion(hasItem(BrokenRubberBoatItem::class.instance()), conditionsFromItem(BrokenRubberBoatItem::class.instance()))
                .offerTo(exporter, BrokenRubberBoatItem::class.recipeId("repair_to_rubber_boat"))
        }
    }
}

/** 碳纤维轻艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "carbon_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class CarbonBoatItem : Ic2BoatItem(ModEntities.CARBON_BOAT, Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val plate = CarbonPlate::class.instance()
            if (plate != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.TRANSPORTATION, CarbonBoatItem::class.instance(), 1)
                    .pattern("   ")
                    .pattern("P P")
                    .pattern("PPP")
                    .input('P', plate)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, CarbonBoatItem::class.id())
            }
        }
    }
}

/** 橡皮艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "rubber_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class RubberBoatItem : Ic2BoatItem(ModEntities.RUBBER_BOAT, Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val rubber = RubberItem::class.instance()
            if (rubber != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.TRANSPORTATION, RubberBoatItem::class.instance(), 1)
                    .pattern("   ")
                    .pattern("R R")
                    .pattern("RRR")
                    .input('R', rubber)
                    .criterion(hasItem(rubber), conditionsFromItem(rubber))
                    .offerTo(exporter, RubberBoatItem::class.id())
            }
        }
    }
}

/** 电动艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "electric_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class ElectricBoatItem : Ic2BoatItem(ModEntities.ELECTRIC_BOAT, Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val cable = InsulatedCopperCableBlock::class.item()
            val plate = IronPlate::class.instance()
            val motor = ElectricMotor::class.instance()
            val rotor = IronRotor::class.instance()
            if (cable != Items.AIR && plate != Items.AIR && motor != Items.AIR && rotor != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.TRANSPORTATION, ElectricBoatItem::class.instance(), 1)
                    .pattern("CCC")
                    .pattern("IMI")
                    .pattern("IRI")
                    .input('C', cable)
                    .input('I', plate)
                    .input('M', motor)
                    .input('R', rotor)
                    .criterion(hasItem(cable), conditionsFromItem(cable))
                    .offerTo(exporter, ElectricBoatItem::class.id())
            }
        }
    }
}
