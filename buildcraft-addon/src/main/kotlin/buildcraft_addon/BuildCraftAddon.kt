package buildcraft_addon

import buildcraft_addon.content.fluid.ModFluids
import buildcraft_addon.content.worldgen.ModWorldgen
import ic2_120.registry.ClassScanner
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.util.Identifier
import net.minecraft.world.gen.GenerationStep
import org.slf4j.LoggerFactory

object BuildCraftAddon : ModInitializer {
    const val MOD_ID = "buildcraft_addon"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun id(path: String): Identifier = Identifier(MOD_ID, path)

    override fun onInitialize() {
        // Register fluids first
        ModFluids.register()

        // Register worldgen features
        ModWorldgen.register()

        // Scan and register blocks, items, block entities, screens
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "buildcraft_addon.content.tab",
                "buildcraft_addon.content.block",
                "buildcraft_addon.content.blockentity",
                "buildcraft_addon.content.screen",
            )
        )

        // Attach features to biomes via Fabric API
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Feature.UNDERGROUND_DECORATION,
            ModWorldgen.WATER_SPRING_PLACED_KEY
        )
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Feature.UNDERGROUND_DECORATION,
            ModWorldgen.OIL_WELL_PLACED_KEY
        )

        LOGGER.info("BuildCraft Addon initialized")
    }
}
