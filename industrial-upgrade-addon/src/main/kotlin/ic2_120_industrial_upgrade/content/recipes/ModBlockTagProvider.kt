package ic2_120_industrial_upgrade.content.recipes

import ic2_120.content.block.MachineBlock
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.tags.BlockTags
import java.util.concurrent.CompletableFuture

class ModBlockTagProvider(
    output: FabricPackOutput,
    registriesFuture: CompletableFuture<HolderLookup.Provider>,
) : FabricTagsProvider.BlockTagsProvider(output, registriesFuture) {

    private fun getOrCreateTagBuilder(key: net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block>) =
        ic2_120.compat.LegacyTagAppender26(BuiltInRegistries.BLOCK) { replace -> net.minecraft.data.tags.TagAppender.forBuilder(getOrCreateRawBuilder(key).setReplace(replace)) }

    override fun addTags(provider: HolderLookup.Provider) {
        val pickaxeBuilder = getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE).setReplace(false)
        val ironToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).setReplace(false)
        val stoneToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL).setReplace(false)

        for (block in BuiltInRegistries.BLOCK) {
            val id = BuiltInRegistries.BLOCK.getKey(block)
            if (id.namespace != IC2IndustrialUpgrade.MOD_ID) continue

            when {
                // 所有方块（6 太阳能板 + 中子制造机）均继承 MachineBlock，统一需铁镐
                block is MachineBlock -> {
                    pickaxeBuilder.add(block)
                    ironToolBuilder.add(block)
                }

                // 兜底：其它方块需镐
                else -> {
                    pickaxeBuilder.add(block)
                }
            }
        }
    }
}
