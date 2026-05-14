package ic2_120.content.worldgen

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.GenerationStep
import org.slf4j.LoggerFactory

/**
 * 矿石世界生成。
 * 锡矿照搬铁矿、铅矿照搬金矿、铀矿照搬钻石，铜矿已移除（原版有）。
 * 泥炭矿只在雨林和蘑菇岛群系生成，可通过配置文件覆盖。
 * ConfiguredFeature 和 PlacedFeature 通过 data/ic2_120/worldgen/ 下的 JSON 定义。
 */
object OreGeneration {

    private val logger = LoggerFactory.getLogger("ic2_120.oregen")

    val ORE_TIN_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Ic2_120.MOD_ID, "ore_tin"))
    val ORE_TIN_DEEP_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Ic2_120.MOD_ID, "ore_tin_deep"))
    val ORE_TIN_UPPER_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Ic2_120.MOD_ID, "ore_tin_upper"))
    val ORE_LEAD_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Ic2_120.MOD_ID, "ore_lead"))
    val ORE_URANIUM_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Ic2_120.MOD_ID, "ore_uranium"))
    val ORE_PEAT_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Ic2_120.MOD_ID, "ore_peat"))

    fun register() {
        BiomeModifications.addFeature(
            BiomeSelectors.all(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            ORE_TIN_PLACED_KEY
        )
        BiomeModifications.addFeature(
            BiomeSelectors.all(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            ORE_TIN_DEEP_PLACED_KEY
        )
        BiomeModifications.addFeature(
            BiomeSelectors.all(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            ORE_TIN_UPPER_PLACED_KEY
        )
        BiomeModifications.addFeature(
            BiomeSelectors.all(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            ORE_LEAD_PLACED_KEY
        )
        BiomeModifications.addFeature(
            BiomeSelectors.all(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            ORE_URANIUM_PLACED_KEY
        )

        registerPeatOre()
    }

    private fun registerPeatOre() {
        val config = Ic2Config.current.worldgen.peatOre.normalized()

        if (!config.enabled) {
            logger.info("Peat ore worldgen disabled by config")
            return
        }

        val biomeKeys = config.biomes.mapNotNull(::toBiomeKey)
        if (biomeKeys.isEmpty()) {
            logger.warn("Peat ore worldgen skipped because no valid biomes were configured")
            return
        }

        logger.info("Peat ore will generate in {} biomes: {}", biomeKeys.size, config.biomes)
        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(*biomeKeys.toTypedArray()),
            GenerationStep.Feature.UNDERGROUND_ORES,
            ORE_PEAT_PLACED_KEY
        )
    }

    private fun toBiomeKey(rawId: String): RegistryKey<Biome>? {
        val biomeId = Identifier.tryParse(rawId)
        if (biomeId == null) {
            logger.warn("Ignoring invalid peat ore biome id in config: {}", rawId)
            return null
        }
        return RegistryKey.of(RegistryKeys.BIOME, biomeId)
    }

}
