package ic2_120_advanced_solar_addon.content.recipes

import ic2_120.registry.ClassScanner
import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.server.recipe.RecipeJsonProvider
import java.util.function.Consumer

class ModRecipeProvider(output: FabricDataOutput) : FabricRecipeProvider(output) {

    override fun generate(exporter: Consumer<RecipeJsonProvider>) {
        ClassScanner.generateRecipesForMod(IC2AdvancedSolarAddon.MOD_ID, exporter)
    }
}
