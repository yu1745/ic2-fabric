package ic2_120.content.worldgen

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
// import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.gen.GenerationStep

/**
 * 矿石世界生成。
 * 锡矿照搬铁矿、铅矿照搬金矿、铀矿照搬钻石，铜矿已移除（原版有）。
 * ConfiguredFeature 和 PlacedFeature 通过 data/ic2_120/worldgen/ 下的 JSON 定义。
 */
object OreGeneration {

    // private val logger = LoggerFactory.getLogger("ic2_120.oregen")

    val ORE_TIN_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier(Ic2_120.MOD_ID, "ore_tin"))
    val ORE_TIN_DEEP_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier(Ic2_120.MOD_ID, "ore_tin_deep"))
    val ORE_TIN_UPPER_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier(Ic2_120.MOD_ID, "ore_tin_upper"))
    val ORE_LEAD_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier(Ic2_120.MOD_ID, "ore_lead"))
    val ORE_URANIUM_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier(Ic2_120.MOD_ID, "ore_uranium"))

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
    }

}
