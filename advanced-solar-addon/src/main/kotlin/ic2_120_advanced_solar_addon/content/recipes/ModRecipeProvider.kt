package ic2_120_advanced_solar_addon.content.recipes

import ic2_120.registry.ClassScanner
import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import java.util.concurrent.CompletableFuture
import net.minecraft.core.HolderLookup

class ModRecipeProvider(output: FabricPackOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) : FabricRecipeProvider(output, registriesFuture) {
    override fun createRecipeProvider(registries: HolderLookup.Provider, exporter: RecipeOutput): RecipeProvider =
        object : RecipeProvider(registries, exporter) {
            override fun buildRecipes() {
                ClassScanner.generateRecipesForMod(IC2AdvancedSolarAddon.MOD_ID, exporter)
            }
        }

    override fun getName(): String = "Advanced Solar recipes"
}
