package ic2_120.content.item

import net.minecraft.core.registries.BuiltInRegistries

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.Ic2_120
import ic2_120.content.block.CompressedCoalBall
import ic2_120.content.block.cables.CopperCableBlock
import ic2_120.content.uu.appendUuTemplateTooltip
import ic2_120.registry.CreativeTab
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.content.recipes.ModTags
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.recipeId
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

// ========== 碳材料类 ==========

@ModItem(name = "carbon_fibre", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonFibre : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonFibre::class.instance(), 1)
                .pattern("CC")
                .pattern("CC")
                .define('C', CoalDust::class.instance())
                .unlockedBy(has(CoalDust::class.instance()), has(CoalDust::class.instance()))
                .save(exporter, CarbonFibre::class.id())
        }
    }
}

@ModItem(name = "carbon_mesh", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonMesh : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonMesh::class.instance(), 1)
                .pattern("CC")
                .define('C', CarbonFibre::class.instance())
                .unlockedBy(has(CarbonFibre::class.instance()), has(CarbonFibre::class.instance()))
                .save(exporter, CarbonMesh::class.id())
        }
    }
}

@ModItem(name = "carbon_plate", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonPlate : Item(Item.Properties()) {
    // companion object {
    //     @RecipeProvider
    //     fun generateRecipes(exporter: RecipeOutput) {
    //         ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonPlate::class.instance(), 1)
    //             .pattern("   ").pattern("   ").pattern(" MM")
    //             .define('M', CarbonMesh::class.instance())
    //             .unlockedBy(has(CarbonMesh::class.instance()), has(CarbonMesh::class.instance()))
    //             .save(exporter, CarbonPlate::class.id())
    //     }
    // }
}

@ModItem(name = "wooden_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class WoodenRotorBlade : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, WoodenRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .define('x', ItemTags.PLANKS)
                .define('y', ItemTags.LOGS)
                .unlockedBy(getHasName(Items.OAK_PLANKS), has(Items.OAK_PLANKS))
                .save(exporter, WoodenRotorBlade::class.id())
        }
    }
}

@ModItem(name = "bronze_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class BronzeRotorBlade : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = BronzePlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzeRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .define('x', Ingredient.of(ModTags.Compat.Items.PLATES_BRONZE))
                .define('y', Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE))
                .unlockedBy(getHasName(plate), has(plate))
                .save(exporter, BronzeRotorBlade::class.id())
        }
    }
}

@ModItem(name = "iron_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class IronRotorBlade : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = IronPlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IronRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .define('x', Ingredient.of(ModTags.Compat.Items.PLATES_IRON))
                .define('y', Ingredient.of(ModTags.Compat.Items.INGOTS_IRON))
                .unlockedBy(getHasName(plate), has(plate))
                .save(exporter, IronRotorBlade::class.id())
        }
    }
}

@ModItem(name = "steel_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class SteelRotorBlade : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = SteelPlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteelRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .define('x', Ingredient.of(ModTags.Compat.Items.PLATES_STEEL))
                .define('y', Ingredient.of(ModTags.Compat.Items.INGOTS_STEEL))
                .unlockedBy(getHasName(plate), has(plate))
                .save(exporter, SteelRotorBlade::class.id())
        }
    }
}

@ModItem(name = "carbon_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class CarbonRotorBlade : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val mesh = CarbonMesh::class.instance()
            val plate = CarbonPlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonRotorBlade::class.instance(), 1)
                .pattern("xxx").pattern("yyy").pattern("xxx")
                .define('x', plate)
                .define('y', mesh)
                .unlockedBy(getHasName(plate), has(plate))
                .save(exporter, CarbonRotorBlade::class.id())
        }
    }
}

abstract class RotorItem(private val lifetimeHours: Int) : Item(
    Item.Properties()
        .stacksTo(1)
        .durability(lifetimeHours * 60 * 60 * 20)
) {
    companion object {
        private const val ROTOR_WEAR_REMAINDER_KEY = "RotorWearRemainder"
        private const val TICKS_PER_HOUR = 72_000.0
    }

    override fun appendHoverText(stack: net.minecraft.world.item.ItemStack, world: Level?, tooltip: MutableList<Component>, context: TooltipContext) {
        super.appendHoverText(stack, context, tooltip, type)
        val remainder = stack.orCreateNbt.getDouble(ROTOR_WEAR_REMAINDER_KEY)
        val remainingHours = ((stack.maxDamage - stack.damage).toDouble() - remainder).coerceAtLeast(0.0) / TICKS_PER_HOUR
        tooltip.add(
            Component.translatable(
                "tooltip.ic2_120.rotor_lifetime",
                String.format("%.1f", remainingHours),
                lifetimeHours
            ).withStyle(ChatFormatting.GRAY)
        )
    }
}

@ModItem(name = "wooden_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class WoodenRotor : RotorItem(3) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val blade = WoodenRotorBlade::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, WoodenRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .define('B', blade)
                .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_IRON))
                .unlockedBy(getHasName(blade), has(blade))
                .save(exporter, WoodenRotor::class.id())
        }
    }
}

@ModItem(name = "bronze_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class BronzeRotor : RotorItem(24) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val blade = BronzeRotorBlade::class.instance()
            val shaft = ToolHandleBronzeItem::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzeRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .define('B', blade)
                .define('I', shaft)
                .unlockedBy(getHasName(blade), has(blade))
                .save(exporter, BronzeRotor::class.id())
        }
    }
}

@ModItem(name = "iron_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class IronRotor : RotorItem(24) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val blade = IronRotorBlade::class.instance()
            val shaft = ToolHandleIronItem::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IronRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .define('B', blade)
                .define('I', shaft)
                .unlockedBy(getHasName(blade), has(blade))
                .save(exporter, IronRotor::class.id())
        }
    }
}

@ModItem(name = "steel_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class SteelRotor : RotorItem(48) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val blade = SteelRotorBlade::class.instance()
            val shaft = ToolHandleIronItem::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteelRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .define('B', blade)
                .define('I', shaft)
                .unlockedBy(getHasName(blade), has(blade))
                .save(exporter, SteelRotor::class.id())
        }
    }
}

@ModItem(name = "carbon_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class CarbonRotor : RotorItem(168) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val blade = CarbonRotorBlade::class.instance()
            val shaft = ToolHandleSteelItem::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonRotor::class.instance(), 1)
                .pattern(" B ").pattern("BIB").pattern(" B ")
                .define('B', blade)
                .define('I', shaft)
                .unlockedBy(getHasName(blade), has(blade))
                .save(exporter, CarbonRotor::class.id())
        }
    }
}

// ========== 电路与机械部件 ==========

//电路板
@ModItem(name = "circuit", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Circuit : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Circuit::class.instance(), 1)
                .pattern("xxx").pattern("rfr").pattern("xxx")
                .define('x', ic2_120.content.block.cables.InsulatedCopperCableBlock::class.instance())
                .define('r', Items.REDSTONE)
                .define('f', IronPlate::class.instance())
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(exporter, Circuit::class.id())
        }
    }
}
//高级电路板
@ModItem(name = "advanced_circuit", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class AdvancedCircuit : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AdvancedCircuit::class.instance(), 1)
                .pattern("rlr").pattern("bxb").pattern("rlr")
                .define('r', Items.REDSTONE)
                .define('l', Items.GLOWSTONE_DUST)
                .define('b', Items.LAPIS_LAZULI)
                .define('x', Circuit::class.instance())
                .unlockedBy(has(Circuit::class.instance()), has(Circuit::class.instance()))
                .save(exporter, AdvancedCircuit::class.id())
        }
    }
}

@ModItem(name = "alloy", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Alloy : Item(Item.Properties())

@ModItem(name = "iridium_shard", tab = CreativeTab.IC2_MATERIALS, group = "materials")
class IridiumShard : Item(Item.Properties()){
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, IridiumShard::class.instance(), 9)
                .requires(IridiumOreItem::class.instance())
                .unlockedBy(has(IridiumOreItem::class.instance()), has(IridiumOreItem::class.instance()))
                .save(exporter, IridiumShard::class.recipeId("from_ore_item"))
        }
    }
}

@ModItem(name = "iridium_ore_item", tab = CreativeTab.IC2_MATERIALS, group = "materials", materialTags = ["ores/iridium"])
class IridiumOreItem : Item(Item.Properties())

@ModItem(name = "iridium", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class IridiumPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 四个铱矿石和四个高级合金交替摆放围成一圈，中间一个钻石
            // I A I
            // A D A
            // I A I
            // I=铱矿石, A=高级合金, D=钻石
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IridiumPlate::class.instance(), 1)
                .pattern("IAI").pattern("ADA").pattern("IAI")
                .define('I', IridiumOreItem::class.instance())
                .define('A', Alloy::class.instance())
                .define('D', Ingredient.of(ModTags.Compat.Items.GEMS_DIAMOND))
                .unlockedBy(has(IridiumOreItem::class.instance()), has(IridiumOreItem::class.instance()))
                .save(exporter, IridiumPlate::class.id())

            // 9个铱碎片合成1个铱矿石
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, IridiumOreItem::class.instance(), 1)
                .requires(IridiumShard::class.instance(), 9)
                .unlockedBy(has(IridiumShard::class.instance()), has(IridiumShard::class.instance()))
                .save(exporter, IridiumOreItem::class.recipeId("from_shards"))
        }
    }
}

@ModItem(name = "coil", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Coil : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val cable = CopperCableBlock::class.instance()
            if (cable != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Coil::class.instance(), 1)
                    .pattern("CCC")
                    .pattern("CIC")
                    .pattern("CCC")
                    .define('C', cable)
                    .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_IRON))
                    .unlockedBy(getHasName(cable), has(cable))
                    .save(exporter, Coil::class.id())
            }
        }
    }
}

@ModItem(name = "electric_motor", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class ElectricMotor : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ElectricMotor::class.instance(), 1)
                .pattern(" C ").pattern("TFT").pattern(" C ")
                .define('T', TinCasing::class.instance())
                .define('C', Coil::class.instance())
                .define('F', Ingredient.of(ModTags.Compat.Items.INGOTS_IRON))
                .unlockedBy(has(Coil::class.instance()), has(Coil::class.instance()))
                .save(exporter, ElectricMotor::class.id())
        }
    }
}

@ModItem(name = "heat_conductor", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class HeatConductor : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val rubber = Ingredient.of(ModTags.Compat.Items.RUBBER)
            val plate = Ingredient.of(ModTags.Compat.Items.PLATES_COPPER)
            if (RubberItem::class.instance() != Items.AIR && CopperPlate::class.instance() != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HeatConductor::class.instance(), 1)
                    .pattern("RPR")
                    .pattern("RPR")
                    .pattern("RPR")
                    .define('R', rubber)
                    .define('P', plate)
                    .unlockedBy(has(CopperPlate::class.instance()), has(CopperPlate::class.instance()))
                    .save(exporter, HeatConductor::class.id())
            }
        }
    }
}

@ModItem(name = "copper_boiler", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class CopperBoiler : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 8个铜外壳围成一圈，中间空
            // C C C
            // C   C
            // C C C
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CopperBoiler::class.instance(), 1)
                .pattern("CCC").pattern("C C").pattern("CCC")
                .define('C', CopperCasing::class.instance())
                .unlockedBy(has(CopperCasing::class.instance()), has(CopperCasing::class.instance()))
                .save(exporter, CopperBoiler::class.id())
        }
    }
}

// ========== 能源球类 ==========

@ModItem(name = "coal_ball", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CoalBall : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CoalBall::class.instance(), 1)
                .pattern("DCD").pattern("CFC").pattern("DCD")
                .define('D', CoalDust::class.instance())
                .define('C', CoalDust::class.instance())
                .define('F', Items.FLINT)
                .unlockedBy(has(CoalDust::class.instance()), has(CoalDust::class.instance()))
                .save(exporter, CoalBall::class.id())
        }
    }
}

@ModItem(name = "coal_chunk", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CoalChunk : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CoalChunk::class.instance(), 1)
                .pattern("CCC")
                .pattern("COC")
                .pattern("CCC")
                .define('C', CompressedCoalBall::class.instance())
                .define('O', Items.OBSIDIAN)
                .unlockedBy(has(CompressedCoalBall::class.instance()), has(Items.OBSIDIAN))
                .save(exporter, CoalChunk::class.recipeId())
        }
    }
}

@ModItem(name = "industrial_diamond", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class IndustrialDiamond : Item(Item.Properties())

@ModItem(name = "plant_ball", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class PlantBall : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 甘蔗配方
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("RRR").pattern("R R").pattern("RRR")
                .define('R', Items.SUGAR_CANE)
                .unlockedBy(getHasName(Items.SUGAR_CANE), has(Items.SUGAR_CANE))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "plant_ball_from_sugar_cane"))

            // 仙人掌配方
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("CCC").pattern("C C").pattern("CCC")
                .define('C', Items.CACTUS)
                .unlockedBy(getHasName(Items.CACTUS), has(Items.CACTUS))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "plant_ball_from_cactus"))

            // 小麦种子配方
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("SSS").pattern("S S").pattern("SSS")
                .define('S', Items.WHEAT_SEEDS)
                .unlockedBy(getHasName(Items.WHEAT_SEEDS), has(Items.WHEAT_SEEDS))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "plant_ball_from_wheat_seeds"))

            // 小麦配方
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("WWW").pattern("W W").pattern("WWW")
                .define('W', Items.WHEAT)
                .unlockedBy(getHasName(Items.WHEAT), has(Items.WHEAT))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "plant_ball_from_wheat"))

            // 蕨配方
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("FFF").pattern("F F").pattern("FFF")
                .define('F', Items.FERN)
                .unlockedBy(getHasName(Items.FERN), has(Items.FERN))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "plant_ball_from_fern"))

            // 草配方（使用 Tag，因为 1.20.1 中草可能有多个变体）
            val grassIngredient = Ingredient.of(TagKey.create(BuiltInRegistries.ITEM.key, ResourceLocation.fromNamespaceAndPath("minecraft", "tall_grass")))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("GGG").pattern("G G").pattern("GGG")
                .define('G', grassIngredient)
                .unlockedBy(getHasName(Items.WHEAT), has(Items.WHEAT))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "plant_ball_from_grass"))

            // 树叶配方（使用 Tag）
            val leavesIngredient = Ingredient.of(ItemTags.LEAVES)
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("LLL").pattern("L L").pattern("LLL")
                .define('L', leavesIngredient)
                .unlockedBy(getHasName(Items.OAK_LEAVES), has(Items.OAK_LEAVES))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "plant_ball_from_leaves"))

            // 树苗配方（使用 Tag）
            val saplingIngredient = Ingredient.of(ItemTags.SAPLINGS)
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlantBall::class.instance(), 1)
                .pattern("SSS").pattern("S S").pattern("SSS")
                .define('S', saplingIngredient)
                .unlockedBy(getHasName(Items.OAK_SAPLING), has(Items.OAK_SAPLING))
                .save(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "plant_ball_from_saplings"))
        }
    }
}

//已删除
// @ModItem(name = "compressed_plants", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CompressedPlants : Item(Item.Properties())

@ModItem(name = "bio_chaff", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class BioChaff : Item(Item.Properties())

//@ModItem(name = "compressed_hydrated_coal", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
//class CompressedHydratedCoal : Item(Item.Properties())

@ModItem(name = "scrap", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class Scrap : Item(Item.Properties())

@ModItem(name = "scrap_box", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class ScrapBox : ScrapBoxItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ScrapBox::class.instance(), 1)
                .pattern("SSS").pattern("SSS").pattern("SSS")
                .define('S', Scrap::class.instance())
                .unlockedBy(has(Scrap::class.instance()), has(Scrap::class.instance()))
                .save(exporter, ScrapBox::class.id())
        }
    }
}

// ========== 柄与涡轮类 ==========

@ModItem(name = "steam_turbine_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class SteamTurbineBlade : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val steelPlate = ic2_120.content.item.SteelPlate::class.instance()
            val steelIngot = ic2_120.content.item.SteelIngot::class.instance()
            if (steelPlate != net.minecraft.world.item.Items.AIR && steelIngot != net.minecraft.world.item.Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteamTurbineBlade::class.instance(), 1)
                    .pattern("PPP").pattern("PIP").pattern("PPP")
                    .define('P', steelPlate)
                    .define('I', steelIngot)
                    .unlockedBy(getHasName(steelPlate), has(steelPlate))
                    .save(exporter, SteamTurbineBlade::class.id())
            }
        }
    }
}

@ModItem(name = "steam_turbine", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class SteamTurbine : RotorItem(48) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val blade = SteamTurbineBlade::class.instance()
            if (blade != net.minecraft.world.item.Items.AIR) {
                // 水平排列 3 个叶片
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteamTurbine::class.instance(), 1)
                    .pattern("BBB")
                    .define('B', blade)
                    .unlockedBy(getHasName(blade), has(blade))
                    .save(exporter, SteamTurbine::class.id())
                // 垂直排列 3 个叶片（备用配方）
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteamTurbine::class.instance(), 1)
                    .pattern("B").pattern("B").pattern("B")
                    .define('B', blade)
                    .unlockedBy(getHasName(blade), has(blade))
                    .save(exporter, SteamTurbine::class.recipeId("vertical"))
            }
        }
    }
}

// @ModItem(name = "jetpack_attachment_plate", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class JetpackAttachmentPlate : Item(Item.Properties())

// ========== 货币与特殊物品 ==========

@ModItem(name = "resin", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Resin : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 熔炉烧炼：粘性树脂 -> 橡胶
            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(Resin::class.instance()),
                RecipeCategory.MISC,
                RubberItem::class.instance(),
                0.1f,
                200
            ).unlockedBy(has(Resin::class.instance()), has(Resin::class.instance()))
                .save(exporter, Resin::class.recipeId("to_rubber_smelting"))
            // 粘性树脂 + 活塞 -> 粘性活塞
            ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, Items.STICKY_PISTON, 1)
                .requires(Items.PISTON)
                .requires(Resin::class.instance())
                .unlockedBy(getHasName(Items.PISTON), has(Items.PISTON))
                .save(exporter, Resin::class.recipeId("sticky_piston"))
        }
    }
}

@ModItem(name = "slag", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Slag : Item(Item.Properties())

@ModItem(name = "iodine", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Iodine : Item(Item.Properties())

@ModItem(name = "iodine_tablet", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class IodineTablet : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IodineTablet::class.instance(), 1)
                .pattern("IT")
                .define('I', Iodine::class.instance())
                .define('T', Ingredient.of(ModTags.Compat.Items.PLATES_TIN))
                .unlockedBy(has(Iodine::class.instance()), has(Iodine::class.instance()))
                .save(exporter, IodineTablet::class.id())
        }
    }
}

// ========== 模式存储类 ==========

@ModItem(name = "raw_crystal_memory", tab = CreativeTab.IC2_MATERIALS, group = "pattern_storage")
class RawCrystalMemory : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 交叉摆法：5个二氧化硅粉 + 4个黑曜石粉
            // O S O
            // S O S
            // O S O
            // O=黑曜石粉, S=二氧化硅粉
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RawCrystalMemory::class.instance(), 1)
                .pattern("OSO").pattern("SOS").pattern("OSO")
                .define('O', Ingredient.of(ModTags.Compat.Items.DUSTS_OBSIDIAN))
                .define('S', Ingredient.of(ModTags.Compat.Items.DUSTS_SILICON_DIOXIDE))
                .unlockedBy(has(SiliconDioxideDust::class.instance()), has(SiliconDioxideDust::class.instance()))
                .save(exporter, RawCrystalMemory::class.id())
        }
    }
}

@ModItem(name = "crystal_memory", tab = CreativeTab.IC2_MATERIALS, group = "pattern_storage")
class CrystalMemory : Item(Item.Properties().stacksTo(1)) {
    override fun appendHoverText(stack: net.minecraft.world.item.ItemStack, world: Level?, tooltip: MutableList<Component>, context: TooltipContext) {
        super.appendHoverText(stack, context, tooltip, type)
        stack.appendUuTemplateTooltip(tooltip)
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 熔炉烧制：粗制模式存储水晶 -> 模式存储水晶 (200 tick = 10秒, 0.1f 经验)
            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(RawCrystalMemory::class.instance()),
                RecipeCategory.MISC,
                CrystalMemory::class.instance(),
                0.1f,
                200
            ).unlockedBy(has(RawCrystalMemory::class.instance()), has(RawCrystalMemory::class.instance()))
                .save(exporter, CrystalMemory::class.recipeId("from_smelting"))
        }
    }
}
