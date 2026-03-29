package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.pipes.BasePipeBlock
import ic2_120.registry.instance
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
import kotlin.reflect.KClass

/**
 * 生成方块标签：挖掘标签 + [@ModBlock] materialTags 对应的 c:/forge:/compat:。
 */
class ModBlockTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<RegistryWrapper.WrapperLookup>,
) : FabricTagProvider.BlockTagProvider(output, registriesFuture) {
    private val oreBlockIds = setOf(
        "lead_ore",
        "tin_ore",
        "uranium_ore",
        "iridium_ore",
        "deepslate_lead_ore",
        "deepslate_tin_ore",
        "deepslate_uranium_ore",
    )

    override fun configure(registries: RegistryWrapper.WrapperLookup) {
        val pickaxeBuilder = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).setReplace(false)
        val ironToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).setReplace(false)

        for (block in Registries.BLOCK) {
            val id = Registries.BLOCK.getId(block)
            if (id.namespace == Ic2_120.MOD_ID && (block is MachineBlock || block is BasePipeBlock || id.path in oreBlockIds)) {
                pickaxeBuilder.add(block)
                if (block is MachineBlock || id.path in oreBlockIds) {
                    ironToolBuilder.add(block)
                }
            }
        }

        for ((clazz, paths) in MaterialTagRegistry.blockEntries) {
            @Suppress("UNCHECKED_CAST")
            val block = (clazz as KClass<out Block>).instance()
            for (path in paths) {
                registerBlockPath(path, block)
            }
        }
    }

    private fun registerBlockPath(path: String, block: Block) {
        val cTag = cBlock(path)
        val forgeTag = forgeBlock(path)
        getOrCreateTagBuilder(cTag).setReplace(false).add(block)
        getOrCreateTagBuilder(forgeTag).setReplace(false).addTag(cTag)
        val compatTag = compatBlock(path)
        getOrCreateTagBuilder(compatTag).setReplace(false).addTag(cTag).addTag(forgeTag)
    }

    private fun cBlock(path: String): TagKey<Block> =
        TagKey.of(RegistryKeys.BLOCK, Identifier("c", path))

    private fun forgeBlock(path: String): TagKey<Block> =
        TagKey.of(RegistryKeys.BLOCK, Identifier("forge", path))

    private fun compatBlock(path: String): TagKey<Block> =
        TagKey.of(RegistryKeys.BLOCK, Identifier("ic2_120", "compat/$path"))
}
