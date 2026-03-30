package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.block.DirectionalMachineBlock
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.cables.BaseCableBlock
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
 *
 * 注意：除 [MachineBlock] 外，导线 [BaseCableBlock]、变压器 [DirectionalMachineBlock]、
 * 储物箱、储罐、以及大量装饰/金属方块均为普通 [net.minecraft.block.Block]，必须在下方分支中显式覆盖，
 * 否则不会进入 mineable 类标签，表现为镐挖极慢。
 */
class ModBlockTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<RegistryWrapper.WrapperLookup>,
) : FabricTagProvider.BlockTagProvider(output, registriesFuture) {
    private val oreBlockIds = setOf(
        "lead_ore",
        "tin_ore",
        "uranium_ore",
        "deepslate_lead_ore",
        "deepslate_tin_ore",
        "deepslate_uranium_ore",
    )

    /** 与原版橡木系列一致：斧为主工具，镐亦加入以便习惯用镐的玩家正常速度 */
    private val rubberWoodAxePaths = setOf(
        "rubber_wood",
        "rubber_log",
        "stripped_rubber_log",
        "stripped_rubber_wood",
        "rubber_planks",
        "rubber_slab",
        "rubber_stairs",
        "rubber_fence",
        "rubber_fence_gate",
        "rubber_door",
        "rubber_trapdoor",
        "rubber_button",
        "rubber_pressure_plate",
        "rubber_sapling",
    )

    private val metalStorageAndTanks = setOf(
        "bronze_storage_box",
        "iron_storage_box",
        "steel_storage_box",
        "iridium_storage_box",
        "bronze_tank",
        "iron_tank",
        "steel_tank",
        "iridium_tank",
    )

    override fun configure(registries: RegistryWrapper.WrapperLookup) {
        val pickaxeBuilder = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).setReplace(false)
        val ironToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).setReplace(false)
        val stoneToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL).setReplace(false)
        val axeBuilder = getOrCreateTagBuilder(BlockTags.AXE_MINEABLE).setReplace(false)
        val hoeBuilder = getOrCreateTagBuilder(BlockTags.HOE_MINEABLE).setReplace(false)

        for (block in Registries.BLOCK) {
            val id = Registries.BLOCK.getId(block)
            if (id.namespace != Ic2_120.MOD_ID) continue

            when {
                id.path == "rubber_leaves" ->
                    hoeBuilder.add(block)

                id.path == "reinforced_door" -> {
                    pickaxeBuilder.add(block)
                    stoneToolBuilder.add(block)
                }

                // 创造发电机：不加入 pickaxe / needs_iron，便于空手挖掘；掉落由 loot 表固定为本体
                id.path == "creative_generator" -> {
                }

                id.path == "wooden_storage_box" -> {
                    axeBuilder.add(block)
                    pickaxeBuilder.add(block)
                }

                id.path in metalStorageAndTanks -> {
                    pickaxeBuilder.add(block)
                    ironToolBuilder.add(block)
                }

                block is MachineBlock || block is DirectionalMachineBlock || id.path in oreBlockIds -> {
                    pickaxeBuilder.add(block)
                    ironToolBuilder.add(block)
                }

                block is BasePipeBlock || block is BaseCableBlock -> {
                    pickaxeBuilder.add(block)
                }

                id.path in rubberWoodAxePaths -> {
                    axeBuilder.add(block)
                    pickaxeBuilder.add(block)
                }

                else -> {
                    pickaxeBuilder.add(block)
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
