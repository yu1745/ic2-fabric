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
            .addOptionalTag(Identifier.of("c", "fuel"))
            .addOptionalTag(Identifier.of("c", "fuel_oil"))
            .addOptionalTag(Identifier.of("c", "diesel"))
            .addOptionalTag(Identifier.of("c", "gasoline"))
            .addOptionalTag(Identifier.of("c", "refined_fuel"))
            .addOptionalTag(Identifier.of("c", "biofuels"))
            .addOptionalTag(Identifier.of("c", "biofuel"))
            .addOptionalTag(Identifier.of("c", "biodiesel"))
            .addOptionalTag(Identifier.of("c", "ethanol"))
            .addOptionalTag(Identifier.of("c", "plant_oil"))
            .addOptionalTag(Identifier.of("c", "seed_oil"))

        getOrCreateTagBuilder(forgeTag)
            .setReplace(false)
            .add(ModFluids.BIOFUEL_STILL)
            .add(ModFluids.BIOFUEL_FLOWING)
            .addOptionalTag(Identifier.of("forge", "fuel"))
            .addOptionalTag(Identifier.of("forge", "fuel_oil"))
            .addOptionalTag(Identifier.of("forge", "diesel"))
            .addOptionalTag(Identifier.of("forge", "gasoline"))
            .addOptionalTag(Identifier.of("forge", "refined_fuel"))
            .addOptionalTag(Identifier.of("forge", "fuels/biofuel"))
            .addOptionalTag(Identifier.of("forge", "biofuels"))
            .addOptionalTag(Identifier.of("forge", "fuels/biodiesel"))
            .addOptionalTag(Identifier.of("forge", "fuels/ethanol"))
            .addOptionalTag(Identifier.of("forge", "biofuel"))
            .addOptionalTag(Identifier.of("forge", "biodiesel"))
            .addOptionalTag(Identifier.of("forge", "ethanol"))
            .addOptionalTag(Identifier.of("forge", "plant_oil"))
            .addOptionalTag(Identifier.of("forge", "seed_oil"))

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
            .addOptionalTag(Identifier.of("c", "oil"))
            .addOptionalTag(Identifier.of("c", "crude_oil"))
            .addOptionalTag(Identifier.of("c", "creosote"))

        getOrCreateTagBuilder(forgeTag)
            .setReplace(false)
            .add(ModFluids.CREOSOTE_STILL)
            .add(ModFluids.CREOSOTE_FLOWING)
            .addOptionalTag(Identifier.of("forge", "oil"))
            .addOptionalTag(Identifier.of("forge", "crude_oil"))
            .addOptionalTag(Identifier.of("forge", "creosote"))
            .addOptionalTag(Identifier.of("forge", "creosote_oil"))

        getOrCreateTagBuilder(compatFluid(path))
            .setReplace(false)
            .addTag(cTag)
            .addTag(forgeTag)
    }

    private fun cFluid(path: String): TagKey<Fluid> =
        TagKey.of(RegistryKeys.FLUID, Identifier.of("c", path))

    private fun forgeFluid(path: String): TagKey<Fluid> =
        TagKey.of(RegistryKeys.FLUID, Identifier.of("forge", path))

    private fun compatFluid(path: String): TagKey<Fluid> =
        TagKey.of(RegistryKeys.FLUID, Identifier.of("ic2_120", "compat/$path"))

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
