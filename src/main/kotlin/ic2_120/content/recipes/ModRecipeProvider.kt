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
            item("ic2_120:forge_hammer"), Items.COPPER_INGOT
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

        // ==================== 绝缘导线配方（导线 + 橡胶，1 橡胶=1 倍绝缘，2 橡胶=2 倍，3 橡胶=3 倍） ====================
        val rubber = item("ic2_120:rubber")
        createShapeless(recipeExporter, "insulated_copper_cable", RecipeCategory.MISC, item("ic2_120:insulated_copper_cable"), 1,
            item("ic2_120:copper_cable"), rubber
        )
        createShapeless(recipeExporter, "insulated_tin_cable", RecipeCategory.MISC, item("ic2_120:insulated_tin_cable"), 1,
            item("ic2_120:tin_cable"), rubber
        )
        createShapeless(recipeExporter, "insulated_gold_cable", RecipeCategory.MISC, item("ic2_120:insulated_gold_cable"), 1,
            item("ic2_120:gold_cable"), rubber
        )
        createShapeless(recipeExporter, "double_insulated_gold_cable", RecipeCategory.MISC, item("ic2_120:double_insulated_gold_cable"), 1,
            item("ic2_120:gold_cable"), rubber, rubber
        )
        createShapeless(recipeExporter, "insulated_iron_cable", RecipeCategory.MISC, item("ic2_120:insulated_iron_cable"), 1,
            item("ic2_120:iron_cable"), rubber
        )
        createShapeless(recipeExporter, "double_insulated_iron_cable", RecipeCategory.MISC, item("ic2_120:double_insulated_iron_cable"), 1,
            item("ic2_120:iron_cable"), rubber, rubber
        )
        createShapeless(recipeExporter, "triple_insulated_iron_cable", RecipeCategory.MISC, item("ic2_120:triple_insulated_iron_cable"), 1,
            item("ic2_120:iron_cable"), rubber, rubber, rubber
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

        // ==================== 变压器配方 ====================
        val lvTransformer = item("ic2_120:lv_transformer")
        val mvTransformer = item("ic2_120:mv_transformer")
        val insulatedTinCable = item("ic2_120:insulated_tin_cable")
        val coil = item("ic2_120:coil")
        val insulatedCopperCable = item("ic2_120:insulated_copper_cable")
        val insulatedGoldCable = item("ic2_120:insulated_gold_cable")
        val insulatedIronCable = item("ic2_120:insulated_iron_cable")
        val advancedCircuit = item("ic2_120:advanced_circuit")
        val advancedReBattery = item("ic2_120:advanced_re_battery")
        val lapotronCrystal = item("ic2_120:lapotron_crystal")
        val hvTransformer = item("ic2_120:hv_transformer")
        val planks = Items.OAK_PLANKS

        // 低压变压器：6 木板 + 2 绝缘锡质导线 + 1 线圈
        if (lvTransformer != Items.AIR && insulatedTinCable != Items.AIR && coil != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, lvTransformer, 1)
                .pattern("PWP").pattern("PCP").pattern("PWP")
                .input('P', planks).input('W', insulatedTinCable).input('C', coil)
                .criterion(hasItem(coil), conditionsFromItem(coil))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "lv_transformer"))
        }

        // 中压变压器：2 绝缘铜质导线 + 1 基础机械外壳（中间一列）
        if (mvTransformer != Items.AIR && insulatedCopperCable != Items.AIR && machine != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, mvTransformer, 1)
                .pattern(" W ").pattern(" M ").pattern(" W ")
                .input('W', insulatedCopperCable).input('M', machine)
                .criterion(hasItem(machine), conditionsFromItem(machine))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "mv_transformer"))
        }

        // 高压变压器：2 绝缘金质导线 + 1 电路板 + 1 中压变压器 + 1 高级充电电池
        if (hvTransformer != Items.AIR && insulatedGoldCable != Items.AIR && circuit != Items.AIR &&
            mvTransformer != Items.AIR && advancedReBattery != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, hvTransformer, 1)
                .pattern(" W ").pattern("CTB").pattern(" W ")
                .input('W', insulatedGoldCable).input('C', circuit).input('T', mvTransformer).input('B', advancedReBattery)
                .criterion(hasItem(mvTransformer), conditionsFromItem(mvTransformer))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "hv_transformer"))
        }

        // 超高压变压器：2 绝缘高压导线 + 1 高级电路 + 1 高压变压器 + 1 拉普顿晶体
        val evTransformer = item("ic2_120:ev_transformer")
        if (evTransformer != Items.AIR && insulatedIronCable != Items.AIR && advancedCircuit != Items.AIR &&
            hvTransformer != Items.AIR && lapotronCrystal != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, evTransformer, 1)
                .pattern(" W ").pattern("CTL").pattern(" W ")
                .input('W', insulatedIronCable).input('C', advancedCircuit).input('T', hvTransformer).input('L', lapotronCrystal)
                .criterion(hasItem(hvTransformer), conditionsFromItem(hvTransformer))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "ev_transformer"))
        }

        // ==================== 水力发电机配方 ====================
        // 4 木棍（四角）+ 4 橡木木板（四边）+ 1 火力发电机（中心）-> 2 水力发电机
        val waterGenerator = item("ic2_120:water_generator")
        val generator = item("ic2_120:generator")
        if (waterGenerator != Items.AIR && generator != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, waterGenerator, 2)
                .pattern("SPS").pattern("PGP").pattern("SPS")
                .input('S', stick).input('P', planks).input('G', generator)
                .criterion(hasItem(generator), conditionsFromItem(generator))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "water_generator"))
        }

        // ==================== 特斯拉线圈配方 ====================
        // 配方一：5 红石粉 + 1 中压变压器 + 2 铁质外壳 + 1 电路板
        // 配方二：5 红石粉 + 1 中压变压器 + 2 钢锭 + 1 电路板
        val teslaCoil = item("ic2_120:tesla_coil")
        val redstone = Items.REDSTONE
        val ironCasing = item("ic2_120:iron_casing")
        val steelIngot = item("ic2_120:steel_ingot")
        if (teslaCoil != Items.AIR) {
            val centerItem = if (mvTransformer != Items.AIR) mvTransformer else machine
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, teslaCoil, 1)
                .pattern("RRR").pattern("RTR").pattern("ICI")
                .input('R', redstone).input('T', centerItem).input('I', ironCasing).input('C', circuit)
                .criterion(hasItem(ironCasing), conditionsFromItem(ironCasing))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "tesla_coil_iron"))
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, teslaCoil, 1)
                .pattern("RRR").pattern("RTR").pattern("SCS")
                .input('R', redstone).input('T', centerItem).input('S', steelIngot).input('C', circuit)
                .criterion(hasItem(steelIngot), conditionsFromItem(steelIngot))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "tesla_coil_steel"))
        }

        // ==================== 风力发电机配方 ====================
        // 4 铁锭（四角）+ 1 火力发电机（中心）-> 1 风力发电机
        val windGenerator = item("ic2_120:wind_generator")
        if (windGenerator != Items.AIR && generator != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, windGenerator, 1)
                .pattern("I I").pattern(" G ").pattern("I I")
                .input('I', Items.IRON_INGOT).input('G', generator)
                .criterion(hasItem(generator), conditionsFromItem(generator))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "wind_generator"))
        }

        // ==================== 太阳能发电机配方 ====================
        // 煤粉 x3 + 玻璃 x3 + 电路板 x2 + 火力发电机 x1
        val solarGenerator = item("ic2_120:solar_generator")
        val coalDust = item("ic2_120:coal_dust")
        if (solarGenerator != Items.AIR && generator != Items.AIR && coalDust != Items.AIR && circuit != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, solarGenerator, 1)
                .pattern("CGC").pattern("GTG").pattern("BB ")
                .input('C', coalDust).input('G', Items.GLASS).input('T', generator).input('B', circuit)
                .criterion(hasItem(generator), conditionsFromItem(generator))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "solar_generator"))
        }

        // ==================== 核反应仓配方 ====================
        // 4 铅板（四角）+ 1 基础机械外壳（中心）-> 1 核反应仓
        val reactorChamber = item("ic2_120:reactor_chamber")
        val leadPlate = item("ic2_120:lead_plate")
        if (reactorChamber != Items.AIR && leadPlate != Items.AIR && machine != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, reactorChamber, 1)
                .pattern("L L").pattern(" M ").pattern("L L")
                .input('L', leadPlate).input('M', machine)
                .criterion(hasItem(leadPlate), conditionsFromItem(leadPlate))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "reactor_chamber"))
        }

        // ==================== 核反应堆配方 ====================
        // 4 致密铅板（上下两行）+ 1 高级电路（上中）+ 3 核反应仓（中行）+ 1 火力发电机（下中）
        val nuclearReactor = item("ic2_120:nuclear_reactor")
        val denseLeadPlate = item("ic2_120:dense_lead_plate")
        if (nuclearReactor != Items.AIR && denseLeadPlate != Items.AIR && advancedCircuit != Items.AIR &&
            reactorChamber != Items.AIR && generator != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, nuclearReactor, 1)
                .pattern("DCD").pattern("RRR").pattern("DGD")
                .input('D', denseLeadPlate).input('C', advancedCircuit).input('R', reactorChamber).input('G', generator)
                .criterion(hasItem(reactorChamber), conditionsFromItem(reactorChamber))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "nuclear_reactor"))
        }

        // ==================== 放射性同位素温差发电机配方 ====================
        // 7 铁质外壳 + 1 核反应仓 + 1 火力发电机
        val rtGenerator = item("ic2_120:rt_generator")
        if (rtGenerator != Items.AIR && ironCasing != Items.AIR && reactorChamber != Items.AIR && generator != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, rtGenerator, 1)
                .pattern("III").pattern("IGI").pattern("IRI")
                .input('I', ironCasing).input('G', generator).input('R', reactorChamber)
                .criterion(hasItem(reactorChamber), conditionsFromItem(reactorChamber))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "rt_generator"))
        }

        // ==================== 日光灯配方 ====================
        // 绝缘铜质导线（上中）+ 锡质导线（中中）+ 5 玻璃 -> 8 日光灯
        val luminatorFlat = item("ic2_120:luminator_flat")
        val glass = Items.GLASS
        val tinCable = item("ic2_120:tin_cable")
        if (luminatorFlat != Items.AIR && insulatedCopperCable != Items.AIR && tinCable != Items.AIR) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, luminatorFlat, 8)
                .pattern(" C ").pattern("GTG").pattern("GGG")
                .input('C', insulatedCopperCable).input('G', glass).input('T', tinCable)
                .criterion(hasItem(insulatedCopperCable), conditionsFromItem(insulatedCopperCable))
                .offerTo(recipeExporter, Identifier(Ic2_120.MOD_ID, "luminator_flat"))
        }
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
