package ic2_120.content.recipes

import ic2_120.content.fluid.ModFluids
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.world.level.material.Fluid
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.tags.TagKey
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture

class ModFluidTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<HolderLookup.WrapperLookup>,
) : FabricTagProvider.FluidTagProvider(output, registriesFuture) {

    override fun configure(registries: HolderLookup.WrapperLookup) {
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
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "fuel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "fuel_oil"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "diesel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "gasoline"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "refined_fuel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "biofuels"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "biofuel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "biodiesel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "ethanol"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "plant_oil"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "seed_oil"))

        getOrCreateTagBuilder(forgeTag)
            .setReplace(false)
            .add(ModFluids.BIOFUEL_STILL)
            .add(ModFluids.BIOFUEL_FLOWING)
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "fuel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "fuel_oil"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "diesel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "gasoline"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "refined_fuel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "fuels/biofuel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "biofuels"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "fuels/biodiesel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "fuels/ethanol"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "biofuel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "biodiesel"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "ethanol"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "plant_oil"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "seed_oil"))

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
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "oil"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "crude_oil"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "creosote"))

        getOrCreateTagBuilder(forgeTag)
            .setReplace(false)
            .add(ModFluids.CREOSOTE_STILL)
            .add(ModFluids.CREOSOTE_FLOWING)
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "oil"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "crude_oil"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "creosote"))
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", "creosote_oil"))

        getOrCreateTagBuilder(compatFluid(path))
            .setReplace(false)
            .addTag(cTag)
            .addTag(forgeTag)
    }

    private fun ic2Fluid(path: String): TagKey<Fluid> =
        TagKey.of(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("ic2_120", path))

    private fun cFluid(path: String): TagKey<Fluid> =
        TagKey.of(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", path))

    private fun forgeFluid(path: String): TagKey<Fluid> =
        TagKey.of(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("forge", path))

    private fun compatFluid(path: String): TagKey<Fluid> =
        TagKey.of(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("ic2_120", "compat/$path"))

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
