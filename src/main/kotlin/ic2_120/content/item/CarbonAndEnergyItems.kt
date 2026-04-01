package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.content.block.CompressedCoalBall
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.uu.appendUuTemplateTooltip
import ic2_120.registry.CreativeTab
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.content.recipes.ModTags
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.recipeId
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.world.World
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

// ========== 碳材料类 ==========

@ModItem(name = "carbon_fibre", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonFibre : Item(FabricItemSettings())

@ModItem(name = "carbon_mesh", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonMesh : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CarbonMesh::class.instance(), 1)
                .pattern("CC")
                .input('C', CarbonFibre::class.instance())
                .criterion(hasItem(CarbonFibre::class.instance()), conditionsFromItem(CarbonFibre::class.instance()))
                .offerTo(exporter, CarbonMesh::class.id())
        }
    }
}

@ModItem(name = "carbon_plate", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonPlate : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CarbonPlate::class.instance(), 1)
                .pattern("   ").pattern("   ").pattern(" MM")
                .input('M', CarbonMesh::class.instance())
                .criterion(hasItem(CarbonMesh::class.instance()), conditionsFromItem(CarbonMesh::class.instance()))
                .offerTo(exporter, CarbonPlate::class.id())
        }
    }
}

@ModItem(name = "wooden_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class WoodenRotorBlade : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WoodenRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .input('x', ItemTags.PLANKS)
                .input('y', ItemTags.LOGS)
                .criterion(hasItem(Items.OAK_PLANKS), conditionsFromItem(Items.OAK_PLANKS))
                .offerTo(exporter, WoodenRotorBlade::class.id())
        }
    }
}

@ModItem(name = "bronze_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class BronzeRotorBlade : Item(FabricItemSettings())

@ModItem(name = "iron_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class IronRotorBlade : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val plate = IronPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .input('x', plate)
                .input('y', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_IRON))
                .criterion(hasItem(plate), conditionsFromItem(plate))
                .offerTo(exporter, IronRotorBlade::class.id())
        }
    }
}

@ModItem(name = "steel_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class SteelRotorBlade : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val plate = SteelPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SteelRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .input('x', plate)
                .input('y', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_STEEL))
                .criterion(hasItem(plate), conditionsFromItem(plate))
                .offerTo(exporter, SteelRotorBlade::class.id())
        }
    }
}

@ModItem(name = "carbon_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class CarbonRotorBlade : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val mesh = CarbonMesh::class.instance()
            val plate = CarbonPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CarbonRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .input('x', plate)
                .input('y', mesh)
                .criterion(hasItem(plate), conditionsFromItem(plate))
                .offerTo(exporter, CarbonRotorBlade::class.id())
        }
    }
}

abstract class RotorItem(private val lifetimeHours: Int) : Item(
    FabricItemSettings()
        .maxCount(1)
        .maxDamage(lifetimeHours * 60 * 60 * 20)
) {
    companion object {
        private const val ROTOR_WEAR_REMAINDER_KEY = "RotorWearRemainder"
        private const val TICKS_PER_HOUR = 72_000.0
    }

    override fun appendTooltip(stack: net.minecraft.item.ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val remainder = stack.nbt?.getDouble(ROTOR_WEAR_REMAINDER_KEY) ?: 0.0
        val remainingHours = ((stack.maxDamage - stack.damage).toDouble() - remainder).coerceAtLeast(0.0) / TICKS_PER_HOUR
        tooltip.add(
            Text.translatable(
                "tooltip.ic2_120.rotor_lifetime",
                String.format("%.1f", remainingHours),
                lifetimeHours
            ).formatted(Formatting.GRAY)
        )
    }
}

@ModItem(name = "wooden_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class WoodenRotor : RotorItem(3) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val blade = WoodenRotorBlade::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WoodenRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .input('B', blade)
                .input('I', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_IRON))
                .criterion(hasItem(blade), conditionsFromItem(blade))
                .offerTo(exporter, WoodenRotor::class.id())
        }
    }
}

@ModItem(name = "iron_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class IronRotor : RotorItem(24) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val blade = IronRotorBlade::class.instance()
            val plate = IronPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .input('B', blade)
                .input('I', plate)
                .criterion(hasItem(blade), conditionsFromItem(blade))
                .offerTo(exporter, IronRotor::class.id())
        }
    }
}

@ModItem(name = "steel_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class SteelRotor : RotorItem(48) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val blade = SteelRotorBlade::class.instance()
            val plate = SteelPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SteelRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .input('B', blade)
                .input('I', plate)
                .criterion(hasItem(blade), conditionsFromItem(blade))
                .offerTo(exporter, SteelRotor::class.id())
        }
    }
}

@ModItem(name = "carbon_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class CarbonRotor : RotorItem(168) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val blade = CarbonRotorBlade::class.instance()
            val plate = SteelPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CarbonRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .input('B', blade)
                .input('I', plate)
                .criterion(hasItem(blade), conditionsFromItem(blade))
                .offerTo(exporter, CarbonRotor::class.id())
        }
    }
}

// ========== 电路与机械部件 ==========

//电路板
@ModItem(name = "circuit", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Circuit : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Circuit::class.instance(), 1)
                .pattern("xxx").pattern("rfr").pattern("xxx")
                .input('x', ic2_120.content.block.cables.InsulatedCopperCableBlock::class.instance())
                .input('r', Items.REDSTONE)
                .input('f', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_IRON))
                .criterion(hasItem(Items.REDSTONE), conditionsFromItem(Items.REDSTONE))
                .offerTo(exporter, Circuit::class.id())
        }
    }
}
//高级电路板
@ModItem(name = "advanced_circuit", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class AdvancedCircuit : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedCircuit::class.instance(), 1)
                .pattern("rlr").pattern("bxb").pattern("rlr")
                .input('r', Items.REDSTONE)
                .input('l', Items.GLOWSTONE_DUST)
                .input('b', Items.LAPIS_LAZULI)
                .input('x', Circuit::class.instance())
                .criterion(hasItem(Circuit::class.instance()), conditionsFromItem(Circuit::class.instance()))
                .offerTo(exporter, AdvancedCircuit::class.id())
        }
    }
}

@ModItem(name = "alloy", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Alloy : Item(FabricItemSettings())

@ModItem(name = "iridium_shard", tab = CreativeTab.IC2_MATERIALS, group = "materials")
class IridiumShard : Item(FabricItemSettings()){
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumShard::class.instance(), 9)
                .input(IridiumOreItem::class.instance())
                .criterion(hasItem(IridiumOreItem::class.instance()), conditionsFromItem(IridiumOreItem::class.instance()))
                .offerTo(exporter, IridiumShard::class.recipeId("from_ore_item"))
        }
    }
}

@ModItem(name = "iridium_ore_item", tab = CreativeTab.IC2_MATERIALS, group = "materials", materialTags = ["ores/iridium"])
class IridiumOreItem : Item(FabricItemSettings())

@ModItem(name = "iridium", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class IridiumPlate : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 四个铱矿石和四个高级合金交替摆放围成一圈，中间一个钻石
            // I A I
            // A D A
            // I A I
            // I=铱矿石, A=高级合金, D=钻石
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumPlate::class.instance(), 1)
                .pattern("IAI").pattern("ADA").pattern("IAI")
                .input('I', IridiumOreItem::class.instance())
                .input('A', Alloy::class.instance())
                .input('D', Ingredient.fromTag(ModTags.Compat.Items.GEMS_DIAMOND))
                .criterion(hasItem(IridiumOreItem::class.instance()), conditionsFromItem(IridiumOreItem::class.instance()))
                .offerTo(exporter, IridiumPlate::class.id())

            // 9个铱碎片合成1个铱矿石
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumOreItem::class.instance(), 1)
                .input(IridiumShard::class.instance(), 9)
                .criterion(hasItem(IridiumShard::class.instance()), conditionsFromItem(IridiumShard::class.instance()))
                .offerTo(exporter, IridiumOreItem::class.recipeId("from_shards"))
        }
    }
}

@ModItem(name = "coil", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Coil : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val cable = InsulatedCopperCableBlock::class.instance()
            if (cable != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Coil::class.instance(), 1)
                    .pattern("CCC")
                    .pattern("CIC")
                    .pattern("CCC")
                    .input('C', cable)
                    .input('I', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_IRON))
                    .criterion(hasItem(cable), conditionsFromItem(cable))
                    .offerTo(exporter, Coil::class.id())
            }
        }
    }
}

@ModItem(name = "electric_motor", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class ElectricMotor : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ElectricMotor::class.instance(), 1)
                .pattern(" Y ").pattern("YFY").pattern(" Y ")
                .input('Y', Coil::class.instance())
                .input('F', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_IRON))
                .criterion(hasItem(Coil::class.instance()), conditionsFromItem(Coil::class.instance()))
                .offerTo(exporter, ElectricMotor::class.id())
        }
    }
}

@ModItem(name = "heat_conductor", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class HeatConductor : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val rubber = Ingredient.fromTag(ModTags.Compat.Items.RUBBER)
            val plate = Ingredient.fromTag(ModTags.Compat.Items.PLATES_COPPER)
            if (RubberItem::class.instance() != Items.AIR && CopperPlate::class.instance() != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, HeatConductor::class.instance(), 1)
                    .pattern("RPR")
                    .pattern("RPR")
                    .pattern("RPR")
                    .input('R', rubber)
                    .input('P', plate)
                    .criterion(hasItem(CopperPlate::class.instance()), conditionsFromItem(CopperPlate::class.instance()))
                    .offerTo(exporter, HeatConductor::class.id())
            }
        }
    }
}

@ModItem(name = "copper_boiler", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class CopperBoiler : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 8个铜外壳围成一圈，中间空
            // C C C
            // C   C
            // C C C
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CopperBoiler::class.instance(), 1)
                .pattern("CCC").pattern("C C").pattern("CCC")
                .input('C', CopperCasing::class.instance())
                .criterion(hasItem(CopperCasing::class.instance()), conditionsFromItem(CopperCasing::class.instance()))
                .offerTo(exporter, CopperBoiler::class.id())
        }
    }
}

// ========== 能源球类 ==========

@ModItem(name = "coal_ball", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CoalBall : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CoalBall::class.instance(), 1)
                .pattern("DCD").pattern("CFC").pattern("DCD")
                .input('D', CoalDust::class.instance())
                .input('C', CoalDust::class.instance())
                .input('F', Items.FLINT)
                .criterion(hasItem(CoalDust::class.instance()), conditionsFromItem(CoalDust::class.instance()))
                .offerTo(exporter, CoalBall::class.id())
        }
    }
}

@ModItem(name = "coal_chunk", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CoalChunk : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CoalChunk::class.instance(), 1)
                .pattern("CCC")
                .pattern("COC")
                .pattern("CCC")
                .input('C', CompressedCoalBall::class.instance())
                .input('O', Items.OBSIDIAN)
                .criterion(hasItem(CompressedCoalBall::class.instance()), conditionsFromItem(Items.OBSIDIAN))
                .offerTo(exporter, CoalChunk::class.recipeId())
        }
    }
}

@ModItem(name = "industrial_diamond", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class IndustrialDiamond : Item(FabricItemSettings())

@ModItem(name = "plant_ball", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class PlantBall : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 甘蔗配方
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("RRR").pattern("R R").pattern("RRR")
                .input('R', Items.SUGAR_CANE)
                .criterion(hasItem(Items.SUGAR_CANE), conditionsFromItem(Items.SUGAR_CANE))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "plant_ball_from_sugar_cane"))

            // 仙人掌配方
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("CCC").pattern("C C").pattern("CCC")
                .input('C', Items.CACTUS)
                .criterion(hasItem(Items.CACTUS), conditionsFromItem(Items.CACTUS))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "plant_ball_from_cactus"))

            // 小麦种子配方
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("SSS").pattern("S S").pattern("SSS")
                .input('S', Items.WHEAT_SEEDS)
                .criterion(hasItem(Items.WHEAT_SEEDS), conditionsFromItem(Items.WHEAT_SEEDS))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "plant_ball_from_wheat_seeds"))

            // 小麦配方
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("WWW").pattern("W W").pattern("WWW")
                .input('W', Items.WHEAT)
                .criterion(hasItem(Items.WHEAT), conditionsFromItem(Items.WHEAT))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "plant_ball_from_wheat"))

            // 蕨配方
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("FFF").pattern("F F").pattern("FFF")
                .input('F', Items.FERN)
                .criterion(hasItem(Items.FERN), conditionsFromItem(Items.FERN))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "plant_ball_from_fern"))

            // 草配方（使用 Tag，因为 1.20.1 中草可能有多个变体）
            val grassIngredient = Ingredient.fromTag(TagKey.of(Registries.ITEM.key, Identifier.of("minecraft", "tall_grass")))
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("GGG").pattern("G G").pattern("GGG")
                .input('G', grassIngredient)
                .criterion(hasItem(Items.WHEAT), conditionsFromItem(Items.WHEAT))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "plant_ball_from_grass"))

            // 树叶配方（使用 Tag）
            val leavesIngredient = Ingredient.fromTag(ItemTags.LEAVES)
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("LLL").pattern("L L").pattern("LLL")
                .input('L', leavesIngredient)
                .criterion(hasItem(Items.OAK_LEAVES), conditionsFromItem(Items.OAK_LEAVES))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "plant_ball_from_leaves"))

            // 树苗配方（使用 Tag）
            val saplingIngredient = Ingredient.fromTag(ItemTags.SAPLINGS)
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("SSS").pattern("S S").pattern("SSS")
                .input('S', saplingIngredient)
                .criterion(hasItem(Items.OAK_SAPLING), conditionsFromItem(Items.OAK_SAPLING))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "plant_ball_from_saplings"))
        }
    }
}

//已删除
// @ModItem(name = "compressed_plants", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CompressedPlants : Item(FabricItemSettings())

@ModItem(name = "bio_chaff", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class BioChaff : Item(FabricItemSettings())

//@ModItem(name = "compressed_hydrated_coal", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
//class CompressedHydratedCoal : Item(FabricItemSettings())

@ModItem(name = "scrap", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class Scrap : Item(FabricItemSettings())

@ModItem(name = "scrap_box", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class ScrapBox : ScrapBoxItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ScrapBox::class.instance(), 1)
                .pattern("SSS").pattern("SSS").pattern("SSS")
                .input('S', Scrap::class.instance())
                .criterion(hasItem(Scrap::class.instance()), conditionsFromItem(Scrap::class.instance()))
                .offerTo(exporter, ScrapBox::class.id())
        }
    }
}

// ========== 柄与涡轮类 ==========

@ModItem(name = "steam_turbine_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class SteamTurbineBlade : Item(FabricItemSettings())

@ModItem(name = "steam_turbine", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class SteamTurbine : Item(FabricItemSettings())

@ModItem(name = "jetpack_attachment_plate", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class JetpackAttachmentPlate : Item(FabricItemSettings())

// ========== 货币与特殊物品 ==========

@ModItem(name = "resin", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Resin : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 熔炉烧炼：粘性树脂 -> 橡胶
            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(Resin::class.instance()),
                RecipeCategory.MISC,
                RubberItem::class.instance(),
                0.1f,
                200
            ).criterion(hasItem(Resin::class.instance()), conditionsFromItem(Resin::class.instance()))
                .offerTo(exporter, Resin::class.recipeId("to_rubber_smelting"))
        }
    }
}

@ModItem(name = "slag", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Slag : Item(FabricItemSettings())

@ModItem(name = "iodine", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Iodine : Item(FabricItemSettings())

@ModItem(name = "iodine_tablet", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class IodineTablet : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IodineTablet::class.instance(), 1)
                .pattern("IT")
                .input('I', Iodine::class.instance())
                .input('T', Ingredient.fromTag(ModTags.Compat.Items.PLATES_TIN))
                .criterion(hasItem(Iodine::class.instance()), conditionsFromItem(Iodine::class.instance()))
                .offerTo(exporter, IodineTablet::class.id())
        }
    }
}

// ========== 模式存储类 ==========

@ModItem(name = "raw_crystal_memory", tab = CreativeTab.IC2_MATERIALS, group = "pattern_storage")
class RawCrystalMemory : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 交叉摆法：5个二氧化硅粉 + 4个黑曜石粉
            // O S O
            // S O S
            // O S O
            // O=黑曜石粉, S=二氧化硅粉
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RawCrystalMemory::class.instance(), 1)
                .pattern("OSO").pattern("SOS").pattern("OSO")
                .input('O', Ingredient.fromTag(ModTags.Compat.Items.DUSTS_OBSIDIAN))
                .input('S', Ingredient.fromTag(ModTags.Compat.Items.DUSTS_SILICON_DIOXIDE))
                .criterion(hasItem(SiliconDioxideDust::class.instance()), conditionsFromItem(SiliconDioxideDust::class.instance()))
                .offerTo(exporter, RawCrystalMemory::class.id())
        }
    }
}

@ModItem(name = "crystal_memory", tab = CreativeTab.IC2_MATERIALS, group = "pattern_storage")
class CrystalMemory : Item(FabricItemSettings().maxCount(1)) {
    override fun appendTooltip(stack: net.minecraft.item.ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        stack.appendUuTemplateTooltip(tooltip)
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 熔炉烧制：粗制模式存储水晶 -> 模式存储水晶 (200 tick = 10秒, 0.1f 经验)
            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(RawCrystalMemory::class.instance()),
                RecipeCategory.MISC,
                CrystalMemory::class.instance(),
                0.1f,
                200
            ).criterion(hasItem(RawCrystalMemory::class.instance()), conditionsFromItem(RawCrystalMemory::class.instance()))
                .offerTo(exporter, CrystalMemory::class.recipeId("from_smelting"))
        }
    }
}
