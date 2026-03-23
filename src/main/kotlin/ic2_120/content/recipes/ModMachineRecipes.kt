package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipe
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipeSerializer
import ic2_120.content.recipes.blockcutter.BlockCutterRecipe
import ic2_120.content.recipes.blockcutter.BlockCutterRecipeSerializer
import ic2_120.content.recipes.centrifuge.CentrifugeRecipe
import ic2_120.content.recipes.centrifuge.CentrifugeRecipeSerializer
import ic2_120.content.recipes.compressor.CompressorRecipe
import ic2_120.content.recipes.compressor.CompressorRecipeSerializer
import ic2_120.content.recipes.extractor.ExtractorRecipe
import ic2_120.content.recipes.extractor.ExtractorRecipeSerializer
import ic2_120.content.recipes.macerator.MaceratorRecipe
import ic2_120.content.recipes.macerator.MaceratorRecipeSerializer
import ic2_120.content.recipes.metalformer.MetalFormerRecipe
import ic2_120.content.recipes.metalformer.MetalFormerRecipeSerializer
import ic2_120.content.recipes.orewashing.OreWashingRecipe
import ic2_120.content.recipes.orewashing.OreWashingRecipeSerializer
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

object ModMachineRecipes {
    // Macerator
    val MACERATOR_TYPE: RecipeType<MaceratorRecipe> = object : RecipeType<MaceratorRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:macerating"
    }
    val MACERATOR_SERIALIZER: RecipeSerializer<MaceratorRecipe> = MaceratorRecipeSerializer

    // Compressor
    val COMPRESSOR_TYPE: RecipeType<CompressorRecipe> = object : RecipeType<CompressorRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:compressing"
    }
    val COMPRESSOR_SERIALIZER: RecipeSerializer<CompressorRecipe> = CompressorRecipeSerializer

    // Extractor
    val EXTRACTOR_TYPE: RecipeType<ExtractorRecipe> = object : RecipeType<ExtractorRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:extracting"
    }
    val EXTRACTOR_SERIALIZER: RecipeSerializer<ExtractorRecipe> = ExtractorRecipeSerializer

    // Centrifuge
    val CENTRIFUGE_TYPE: RecipeType<CentrifugeRecipe> = object : RecipeType<CentrifugeRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:centrifuging"
    }
    val CENTRIFUGE_SERIALIZER: RecipeSerializer<CentrifugeRecipe> = CentrifugeRecipeSerializer

    // BlockCutter
    val BLOCK_CUTTER_TYPE: RecipeType<BlockCutterRecipe> = object : RecipeType<BlockCutterRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:cutting"
    }
    val BLOCK_CUTTER_SERIALIZER: RecipeSerializer<BlockCutterRecipe> = BlockCutterRecipeSerializer

    // BlastFurnace
    val BLAST_FURNACE_TYPE: RecipeType<BlastFurnaceRecipe> = object : RecipeType<BlastFurnaceRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:blast_furnacing"
    }
    val BLAST_FURNACE_SERIALIZER: RecipeSerializer<BlastFurnaceRecipe> = BlastFurnaceRecipeSerializer

    // OreWashing
    val ORE_WASHING_TYPE: RecipeType<OreWashingRecipe> = object : RecipeType<OreWashingRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:ore_washing"
    }
    val ORE_WASHING_SERIALIZER: RecipeSerializer<OreWashingRecipe> = OreWashingRecipeSerializer

    // MetalFormer (3 modes share one type/serializer, dispatch via 'mode' field)
    val METAL_FORMER_TYPE: RecipeType<MetalFormerRecipe> = object : RecipeType<MetalFormerRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:metal_forming"
    }
    val METAL_FORMER_SERIALIZER: RecipeSerializer<MetalFormerRecipe> = MetalFormerRecipeSerializer

    fun register() {
        // Register Macerator
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("macerating"), MACERATOR_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("macerating"), MACERATOR_SERIALIZER)

        // Register Compressor
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("compressing"), COMPRESSOR_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("compressing"), COMPRESSOR_SERIALIZER)

        // Register Extractor
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("extracting"), EXTRACTOR_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("extracting"), EXTRACTOR_SERIALIZER)

        // Register Centrifuge
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("centrifuging"), CENTRIFUGE_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("centrifuging"), CENTRIFUGE_SERIALIZER)

        // Register BlockCutter
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("cutting"), BLOCK_CUTTER_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("cutting"), BLOCK_CUTTER_SERIALIZER)

        // Register BlastFurnace
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("blast_furnacing"), BLAST_FURNACE_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("blast_furnacing"), BLAST_FURNACE_SERIALIZER)

        // Register OreWashing
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("ore_washing"), ORE_WASHING_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("ore_washing"), ORE_WASHING_SERIALIZER)

        // Register MetalFormer
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("metal_forming"), METAL_FORMER_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("metal_forming"), METAL_FORMER_SERIALIZER)
    }
}
