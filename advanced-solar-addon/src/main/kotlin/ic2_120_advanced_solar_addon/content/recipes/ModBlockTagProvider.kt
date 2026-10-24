package ic2_120_advanced_solar_addon.content.recipes

import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import ic2_120.content.block.MachineBlock
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.world.level.block.Block
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture

class ModBlockTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<HolderLookup.WrapperLookup>,
) : FabricTagProvider.BlockTagProvider(output, registriesFuture) {

    override fun configure(registries: HolderLookup.WrapperLookup) {
        val pickaxeBuilder = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).setReplace(false)
        val ironToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).setReplace(false)
        val stoneToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL).setReplace(false)

        for (block in BuiltInRegistries.BLOCK) {
            val id = BuiltInRegistries.BLOCK.getId(block)
            if (id.namespace != IC2AdvancedSolarAddon.MOD_ID) continue

            when {
                // 机器方块需要铁镐
                block is MachineBlock -> {
                    pickaxeBuilder.add(block)
                    ironToolBuilder.add(block)
                }

                // 太阳能板等需要石镐
                id.path.endsWith("_solar_panel") ||
                id.path.contains("generator") ||
                id.path.contains("transformer") -> {
                    pickaxeBuilder.add(block)
                    stoneToolBuilder.add(block)
                }

                // 其他方块默认需要镐
                else -> {
                    pickaxeBuilder.add(block)
                }
            }
        }
    }
}
