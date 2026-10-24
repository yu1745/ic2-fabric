package ic2_120.content.recipes

import ic2_120.content.fluid.ModFluids
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.fluid.Fluid
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

class ModFluidTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<RegistryWrapper.WrapperLookup>,
) : FabricTagProvider.FluidTagProvider(output, registriesFuture) {

    override fun configure(registries: RegistryWrapper.WrapperLookup) {
        registerCommonFluidTags()
        registerBiofuelEquivalent("semifluid_generator/biofuel_equivalent")
        registerCreosoteEquivalent("semifluid_generator/creosote_equivalent")
        // UU 物质专属 tag：用于实体浸入检测（UU 物质液池中每秒回复生命），见 UUFluidEffectHandler
        // 仅注册到本 mod 命名空间，不污染 c:/forge: 全局 tag
        getOrCreateTagBuilder(ic2Fluid("uu_matter"))
            .setReplace(false)
            .add(ModFluids.UU_MATTER_STILL)
            .add(ModFluids.UU_MATTER_FLOWING)
    }

    private fun registerCommonFluidTags() {
        registerFluidPair("biofuel", ModFluids.BIOFUEL_STILL, ModFluids.BIOFUEL_FLOWING)
        registerFluidPair("biofuels", ModFluids.BIOFUEL_STILL, ModFluids.BIOFUEL_FLOWING)
        registerFluidPair("fuels/biofuel", ModFluids.BIOFUEL_STILL, ModFluids.BIOFUEL_FLOWING)
        registerFluidPair("biomass", ModFluids.BIOMASS_STILL, ModFluids.BIOMASS_FLOWING)
        registerFluidPair("biomasses", ModFluids.BIOMASS_STILL, ModFluids.BIOMASS_FLOWING)
        registerFluidPair("creosote", ModFluids.CREOSOTE_STILL, ModFluids.CREOSOTE_FLOWING)
        registerFluidPair("creosote_oil", ModFluids.CREOSOTE_STILL, ModFluids.CREOSOTE_FLOWING)
        registerFluidPair("coolant", ModFluids.COOLANT_STILL, ModFluids.COOLANT_FLOWING)
        registerFluidPair("coolants", ModFluids.COOLANT_STILL, ModFluids.COOLANT_FLOWING)
        registerFluidPair("hot_coolant", ModFluids.HOT_COOLANT_STILL, ModFluids.HOT_COOLANT_FLOWING)
        registerFluidPair("hot_coolants", ModFluids.HOT_COOLANT_STILL, ModFluids.HOT_COOLANT_FLOWING)
        registerFluidPair("distilled_water", ModFluids.DISTILLED_WATER_STILL, ModFluids.DISTILLED_WATER_FLOWING)
        registerFluidPair("steam", ModFluids.STEAM_STILL, ModFluids.STEAM_FLOWING)
        registerFluidPair("steam/superheated", ModFluids.SUPERHEATED_STEAM_STILL, ModFluids.SUPERHEATED_STEAM_FLOWING)
        registerFluidPair("superheated_steam", ModFluids.SUPERHEATED_STEAM_STILL, ModFluids.SUPERHEATED_STEAM_FLOWING)
    }

    private fun registerBiofuelEquivalent(path: String) {
        val cTag = cFluid(path)
        val forgeTag = forgeFluid(path)

        getOrCreateTagBuilder(cTag)
            .setReplace(false)
            .add(ModFluids.BIOFUEL_STILL)
            .add(ModFluids.BIOFUEL_FLOWING)
            .addOptionalTag(Identifier("c", "fuel"))
            .addOptionalTag(Identifier("c", "fuel_oil"))
            .addOptionalTag(Identifier("c", "diesel"))
            .addOptionalTag(Identifier("c", "gasoline"))
            .addOptionalTag(Identifier("c", "refined_fuel"))
            .addOptionalTag(Identifier("c", "biofuels"))
            .addOptionalTag(Identifier("c", "biofuel"))
            .addOptionalTag(Identifier("c", "biodiesel"))
            .addOptionalTag(Identifier("c", "ethanol"))
            .addOptionalTag(Identifier("c", "plant_oil"))
            .addOptionalTag(Identifier("c", "seed_oil"))

        getOrCreateTagBuilder(forgeTag)
            .setReplace(false)
            .add(ModFluids.BIOFUEL_STILL)
            .add(ModFluids.BIOFUEL_FLOWING)
            .addOptionalTag(Identifier("forge", "fuel"))
            .addOptionalTag(Identifier("forge", "fuel_oil"))
            .addOptionalTag(Identifier("forge", "diesel"))
            .addOptionalTag(Identifier("forge", "gasoline"))
            .addOptionalTag(Identifier("forge", "refined_fuel"))
            .addOptionalTag(Identifier("forge", "fuels/biofuel"))
            .addOptionalTag(Identifier("forge", "biofuels"))
            .addOptionalTag(Identifier("forge", "fuels/biodiesel"))
            .addOptionalTag(Identifier("forge", "fuels/ethanol"))
            .addOptionalTag(Identifier("forge", "biofuel"))
            .addOptionalTag(Identifier("forge", "biodiesel"))
            .addOptionalTag(Identifier("forge", "ethanol"))
            .addOptionalTag(Identifier("forge", "plant_oil"))
            .addOptionalTag(Identifier("forge", "seed_oil"))

        getOrCreateTagBuilder(compatFluid(path))
            .setReplace(false)
            .addTag(cTag)
            .addTag(forgeTag)
    }

    private fun registerCreosoteEquivalent(path: String) {
        val cTag = cFluid(path)
        val forgeTag = forgeFluid(path)

        getOrCreateTagBuilder(cTag)
            .setReplace(false)
            .add(ModFluids.CREOSOTE_STILL)
            .add(ModFluids.CREOSOTE_FLOWING)
            .addOptionalTag(Identifier("c", "oil"))
            .addOptionalTag(Identifier("c", "crude_oil"))
            .addOptionalTag(Identifier("c", "creosote"))

        getOrCreateTagBuilder(forgeTag)
            .setReplace(false)
            .add(ModFluids.CREOSOTE_STILL)
            .add(ModFluids.CREOSOTE_FLOWING)
            .addOptionalTag(Identifier("forge", "oil"))
            .addOptionalTag(Identifier("forge", "crude_oil"))
            .addOptionalTag(Identifier("forge", "creosote"))
            .addOptionalTag(Identifier("forge", "creosote_oil"))

        getOrCreateTagBuilder(compatFluid(path))
            .setReplace(false)
            .addTag(cTag)
            .addTag(forgeTag)
    }

    private fun ic2Fluid(path: String): TagKey<Fluid> =
        TagKey.of(RegistryKeys.FLUID, Identifier("ic2_120", path))

    private fun cFluid(path: String): TagKey<Fluid> =
        TagKey.of(RegistryKeys.FLUID, Identifier("c", path))

    private fun forgeFluid(path: String): TagKey<Fluid> =
        TagKey.of(RegistryKeys.FLUID, Identifier("forge", path))

    private fun compatFluid(path: String): TagKey<Fluid> =
        TagKey.of(RegistryKeys.FLUID, Identifier("ic2_120", "compat/$path"))

    private fun registerFluidPair(path: String, still: Fluid, flowing: Fluid) {
        getOrCreateTagBuilder(cFluid(path))
            .setReplace(false)
            .add(still)
            .add(flowing)
        getOrCreateTagBuilder(forgeFluid(path))
            .setReplace(false)
            .add(still)
            .add(flowing)
    }
}
