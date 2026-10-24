package ic2_120_advanced_solar_addon.content.recipes

import ic2_120.registry.ClassScanner
import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.recipes.RecipeOutput
import java.util.function.Consumer
import java.util.concurrent.CompletableFuture
import net.minecraft.core.HolderLookup

class ModRecipeProvider(output: FabricDataOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) : FabricRecipeProvider(output, registriesFuture) {
    override fun buildRecipes(exporter: RecipeOutput) {
        ClassScanner.generateRecipesForMod("ic2_120_advanced_solar_addon", exporter)
    }
}
