package ic2_120.integration.jei

import mezz.jei.api.recipe.RecipeType
import net.minecraft.util.Identifier

object Ic2JeiRecipeTypes {
    val MACERATOR: RecipeType<MaceratorJeiRecipe> = RecipeType.create(
        "ic2_120",
        "macerating",
        MaceratorJeiRecipe::class.java
    )

    val MACERATOR_UID: Identifier = Identifier("ic2_120", "macerating")

    val COMPRESSOR: RecipeType<CompressorJeiRecipe> = RecipeType.create(
        "ic2_120",
        "compressing",
        CompressorJeiRecipe::class.java
    )

    val COMPRESSOR_UID: Identifier = Identifier("ic2_120", "compressing")

    val EXTRACTOR: RecipeType<ExtractorJeiRecipe> = RecipeType.create(
        "ic2_120",
        "extracting",
        ExtractorJeiRecipe::class.java
    )

    val EXTRACTOR_UID: Identifier = Identifier("ic2_120", "extracting")

    val CENTRIFUGE: RecipeType<CentrifugeJeiRecipe> = RecipeType.create(
        "ic2_120",
        "centrifuging",
        CentrifugeJeiRecipe::class.java
    )

    val CENTRIFUGE_UID: Identifier = Identifier("ic2_120", "centrifuging")

    val BLAST_FURNACE: RecipeType<BlastFurnaceJeiRecipe> = RecipeType.create(
        "ic2_120",
        "blast_furnacing",
        BlastFurnaceJeiRecipe::class.java
    )

    val BLAST_FURNACE_UID: Identifier = Identifier("ic2_120", "blast_furnacing")

    val BLOCK_CUTTER: RecipeType<BlockCutterJeiRecipe> = RecipeType.create(
        "ic2_120",
        "cutting",
        BlockCutterJeiRecipe::class.java
    )

    val ORE_WASHING: RecipeType<OreWashingJeiRecipe> = RecipeType.create(
        "ic2_120",
        "ore_washing",
        OreWashingJeiRecipe::class.java
    )

    val ORE_WASHING_UID: Identifier = Identifier("ic2_120", "ore_washing")

    val METAL_FORMER_ROLLING: RecipeType<MetalFormerRollingJeiRecipe> = RecipeType.create(
        "ic2_120",
        "metal_forming_rolling",
        MetalFormerRollingJeiRecipe::class.java
    )

    val METAL_FORMER_CUTTING: RecipeType<MetalFormerCuttingJeiRecipe> = RecipeType.create(
        "ic2_120",
        "metal_forming_cutting",
        MetalFormerCuttingJeiRecipe::class.java
    )

    val METAL_FORMER_EXTRUDING: RecipeType<MetalFormerExtrudingJeiRecipe> = RecipeType.create(
        "ic2_120",
        "metal_forming_extruding",
        MetalFormerExtrudingJeiRecipe::class.java
    )

    val SOLID_CANNER: RecipeType<SolidCannerJeiRecipe> = RecipeType.create(
        "ic2_120",
        "solid_canning",
        SolidCannerJeiRecipe::class.java
    )

    val RECYCLER: RecipeType<RecyclerJeiRecipe> = RecipeType.create(
        "ic2_120",
        "recycling",
        RecyclerJeiRecipe::class.java
    )
}

