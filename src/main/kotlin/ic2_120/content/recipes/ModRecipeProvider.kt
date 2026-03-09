package ic2_120.content.recipes

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.Consumer

/**
 * 工作台配方数据生成器
 * 在构建时生成所有配方 JSON 文件
 */
class ModRecipeProvider(output: FabricDataOutput) : FabricRecipeProvider(output) {

    override fun generate(recipeExporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
        fun item(id: String): Item {
            val ident = Identifier.tryParse(id)
            return if (ident != null && Registries.ITEM.containsId(ident)) {
                Registries.ITEM.get(ident)
            } else {
                Items.AIR
            }
        }

        // ==================== 锻锤配方 ====================

        // 锭 -> 板
        createShapeless(recipeExporter, "iron_plate_from_hammer", RecipeCategory.MISC, item("ic2_120:iron_plate"), 1,
            item("ic2_120:forge_hammer"), Items.IRON_INGOT
        )

        createShapeless(recipeExporter, "gold_plate_from_hammer", RecipeCategory.MISC, item("ic2_120:gold_plate"), 1,
            item("ic2_120:forge_hammer"), Items.GOLD_INGOT
        )

        createShapeless(recipeExporter, "copper_plate_from_hammer", RecipeCategory.MISC, item("ic2_120:copper_plate"), 1,
            item("ic2_120:forge_hammer"), item("ic2_120:copper_ingot")
        )

        createShapeless(recipeExporter, "tin_plate_from_hammer", RecipeCategory.MISC, item("ic2_120:tin_plate"), 1,
            item("ic2_120:forge_hammer"), item("ic2_120:tin_ingot")
        )

        createShapeless(recipeExporter, "bronze_plate_from_hammer", RecipeCategory.MISC, item("ic2_120:bronze_plate"), 1,
            item("ic2_120:forge_hammer"), item("ic2_120:bronze_ingot")
        )

        createShapeless(recipeExporter, "lead_plate_from_hammer", RecipeCategory.MISC, item("ic2_120:lead_plate"), 1,
            item("ic2_120:forge_hammer"), item("ic2_120:lead_ingot")
        )

        createShapeless(recipeExporter, "steel_plate_from_hammer", RecipeCategory.MISC, item("ic2_120:steel_plate"), 1,
            item("ic2_120:forge_hammer"), item("ic2_120:steel_ingot")
        )

        createShapeless(recipeExporter, "lapis_plate_from_hammer", RecipeCategory.MISC, item("ic2_120:lapis_plate"), 1,
            item("ic2_120:forge_hammer"), Items.LAPIS_LAZULI
        )

        // 板 -> 外壳
        createShapeless(recipeExporter, "iron_casing_from_hammer", RecipeCategory.MISC, item("ic2_120:iron_casing"), 2,
            item("ic2_120:forge_hammer"), item("ic2_120:iron_plate")
        )

        createShapeless(recipeExporter, "gold_casing_from_hammer", RecipeCategory.MISC, item("ic2_120:gold_casing"), 2,
            item("ic2_120:forge_hammer"), item("ic2_120:gold_plate")
        )

        createShapeless(recipeExporter, "copper_casing_from_hammer", RecipeCategory.MISC, item("ic2_120:copper_casing"), 2,
            item("ic2_120:forge_hammer"), item("ic2_120:copper_plate")
        )

        createShapeless(recipeExporter, "tin_casing_from_hammer", RecipeCategory.MISC, item("ic2_120:tin_casing"), 2,
            item("ic2_120:forge_hammer"), item("ic2_120:tin_plate")
        )

        createShapeless(recipeExporter, "bronze_casing_from_hammer", RecipeCategory.MISC, item("ic2_120:bronze_casing"), 2,
            item("ic2_120:forge_hammer"), item("ic2_120:bronze_plate")
        )

        createShapeless(recipeExporter, "lead_casing_from_hammer", RecipeCategory.MISC, item("ic2_120:lead_casing"), 2,
            item("ic2_120:forge_hammer"), item("ic2_120:lead_plate")
        )

        createShapeless(recipeExporter, "steel_casing_from_hammer", RecipeCategory.MISC, item("ic2_120:steel_casing"), 2,
            item("ic2_120:forge_hammer"), item("ic2_120:steel_plate")
        )

        // ==================== 切割剪刀配方 ====================
        // 板 -> 导线
        createShapeless(recipeExporter, "copper_cable_from_plate", RecipeCategory.MISC, item("ic2_120:copper_cable"), 2,
            item("ic2_120:cutter"), item("ic2_120:copper_plate")
        )

        createShapeless(recipeExporter, "tin_cable_from_plate", RecipeCategory.MISC, item("ic2_120:tin_cable"), 2,
            item("ic2_120:cutter"), item("ic2_120:tin_plate")
        )

        createShapeless(recipeExporter, "gold_cable_from_plate", RecipeCategory.MISC, item("ic2_120:gold_cable"), 2,
            item("ic2_120:cutter"), item("ic2_120:gold_plate")
        )

        createShapeless(recipeExporter, "iron_cable_from_plate", RecipeCategory.MISC, item("ic2_120:iron_cable"), 2,
            item("ic2_120:cutter"), item("ic2_120:iron_plate")
        )

        createShapeless(recipeExporter, "glass_fibre_cable", RecipeCategory.MISC, item("ic2_120:glass_fibre_cable"), 2,
            item("ic2_120:cutter"), Items.GLASS
        )

        // ==================== 锡罐工作台配方 ====================
        val tinIngot = item("ic2_120:tin_ingot")
        val tinCan = item("ic2_120:tin_can")
        // 8 锡锭 -> 16 空锡罐
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, tinCan, 16)
            .pattern("TTT").pattern("T T").pattern("TTT")
            .input('T', tinIngot)
            .criterion(hasItem(tinIngot), conditionsFromItem(tinIngot))
            .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "tin_can_from_ingots_8"))
        // 6 锡锭 -> 4 空锡罐
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, tinCan, 4)
            .pattern(" T ").pattern("T T").pattern("TTT")
            .input('T', tinIngot)
            .criterion(hasItem(tinIngot), conditionsFromItem(tinIngot))
            .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "tin_can_from_ingots_6"))

        // ==================== 青铜工具配方 ====================
        val bronze = item("ic2_120:bronze_ingot")
        val stick = Items.STICK
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, item("ic2_120:bronze_sword"), 1)
            .pattern("M").pattern("M").pattern("S")
            .input('M', bronze).input('S', stick)
            .criterion(hasItem(bronze), conditionsFromItem(bronze))
            .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "bronze_sword"))
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, item("ic2_120:bronze_pickaxe"), 1)
            .pattern("MMM").pattern(" S ").pattern(" S ")
            .input('M', bronze).input('S', stick)
            .criterion(hasItem(bronze), conditionsFromItem(bronze))
            .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "bronze_pickaxe"))
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, item("ic2_120:bronze_axe"), 1)
            .pattern("MM").pattern("MS").pattern(" S")
            .input('M', bronze).input('S', stick)
            .criterion(hasItem(bronze), conditionsFromItem(bronze))
            .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "bronze_axe"))
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, item("ic2_120:bronze_shovel"), 1)
            .pattern("M").pattern("S").pattern("S")
            .input('M', bronze).input('S', stick)
            .criterion(hasItem(bronze), conditionsFromItem(bronze))
            .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "bronze_shovel"))
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, item("ic2_120:bronze_hoe"), 1)
            .pattern("MM").pattern(" S").pattern(" S")
            .input('M', bronze).input('S', stick)
            .criterion(hasItem(bronze), conditionsFromItem(bronze))
            .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "bronze_hoe"))

        // ==================== 基础机器配方 ====================
        val machine = item("ic2_120:machine")
        val circuit = item("ic2_120:circuit")
        val treetap = item("ic2_120:treetap")
        // 提取机：4 木龙头 + 1 基础机械外壳 + 1 电路板
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, item("ic2_120:extractor"), 1)
            .pattern("   ").pattern("TMT").pattern("TCT")
            .input('T', treetap).input('M', machine).input('C', circuit)
            .criterion(hasItem(machine), conditionsFromItem(machine))
            .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "extractor"))
    }

    private fun createShapeless(
        exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>,
        recipeId: String,
        category: RecipeCategory,
        output: Item,
        count: Int,
        vararg inputs: Item
    ) {
        val builder = ShapelessRecipeJsonBuilder.create(category, output, count)
        for (input in inputs) {
            builder.input(input)
        }
        builder.criterion(hasItem(output), conditionsFromItem(output))
            .offerTo(exporter, Identifier(Ic2_120.MOD_ID, recipeId))
    }
}
