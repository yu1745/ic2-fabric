package ic2_120_advanced_solar_addon.content.recipes

import ic2_120.registry.ClassScanner
import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.registry.RegistryWrapper
import java.util.concurrent.CompletableFuture

class ModRecipeProvider(output: FabricDataOutput, registriesFuture: CompletableFuture<RegistryWrapper.WrapperLookup>) : FabricRecipeProvider(output, registriesFuture) {

    override fun generate(recipeExporter: RecipeExporter) {
        ClassScanner.generateRecipesForMod(IC2AdvancedSolarAddon.MOD_ID, recipeExporter)
    }
}
