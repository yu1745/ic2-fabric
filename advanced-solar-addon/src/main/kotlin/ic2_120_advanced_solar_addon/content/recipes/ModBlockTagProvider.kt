package ic2_120_advanced_solar_addon.content.recipes

import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import ic2_120.content.block.MachineBlock
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

class ModBlockTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<RegistryWrapper.WrapperLookup>,
) : FabricTagProvider.BlockTagProvider(output, registriesFuture) {

    override fun configure(registries: RegistryWrapper.WrapperLookup) {
        val pickaxeBuilder = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).setReplace(false)
        val ironToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).setReplace(false)
        val stoneToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL).setReplace(false)

        for (block in Registries.BLOCK) {
            val id = Registries.BLOCK.getId(block)
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
