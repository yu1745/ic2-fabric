package addon_template.content.recipes

import ic2_120.registry.ClassScanner
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.recipes.RecipeOutput
import java.util.function.Consumer
import java.util.concurrent.CompletableFuture
import net.minecraft.core.HolderLookup

class ModRecipeProvider(output: FabricDataOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) : FabricRecipeProvider(output, registriesFuture) {
    override fun buildRecipes(exporter: RecipeOutput) {
        ClassScanner.generateRecipesForMod("addon_template", exporter)
    }
}
