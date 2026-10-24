package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.block.DirectionalMachineBlock
import ic2_120.content.block.Ic2ScaffoldBlock
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.pipes.BasePipeBlock
import ic2_120.registry.instance
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.world.level.block.Block
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

/**
 * 生成方块标签：挖掘标签 + [@ModBlock] materialTags 对应的 c:/forge:/compat:。
 *
 * 注意：除 [MachineBlock] 外，导线 [BaseCableBlock]、变压器 [DirectionalMachineBlock]、
 * 储物箱、储罐、以及大量装饰/金属方块均为普通 [net.minecraft.world.level.block.Block]，必须在下方分支中显式覆盖，
 * 否则不会进入 mineable 类标签，表现为镐挖极慢。
 */
class ModBlockTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<HolderLookup.WrapperLookup>,
) : FabricTagProvider.BlockTagProvider(output, registriesFuture) {
    private val compatPathsBuilt = mutableSetOf<String>()

    private val oreBlockIds = setOf(
        "lead_ore",
        "tin_ore",
        "uranium_ore",
        "deepslate_lead_ore",
        "deepslate_tin_ore",
        "deepslate_uranium_ore",
    )

    /** 原版橡胶木标签：原木、去皮原木和木材都视为可燃烧原木。 */
    private val rubberLogPaths = setOf(
        "rubber_wood",
        "rubber_log",
        "stripped_rubber_log",
        "stripped_rubber_wood",
    )

    /** 与原版橡木系列一致：斧为主工具，镐亦加入以便习惯用镐的玩家正常速度 */
    private val rubberWoodAxePaths = rubberLogPaths + setOf(
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

    override fun configure(registries: HolderLookup.WrapperLookup) {
        val pickaxeBuilder = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).setReplace(false)
        val ironToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).setReplace(false)
        val stoneToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL).setReplace(false)
        val axeBuilder = getOrCreateTagBuilder(BlockTags.AXE_MINEABLE).setReplace(false)
        val hoeBuilder = getOrCreateTagBuilder(BlockTags.HOE_MINEABLE).setReplace(false)
        val logsBuilder = getOrCreateTagBuilder(BlockTags.LOGS).setReplace(false)
        val logsThatBurnBuilder = getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN).setReplace(false)
        val saplingBuilder = getOrCreateTagBuilder(BlockTags.SAPLINGS).setReplace(false)
        val climbableBuilder = getOrCreateTagBuilder(BlockTags.CLIMBABLE).setReplace(false)

        // 将橡胶原木添加到原版 logs 标签，使橡胶树叶能正确识别原木
        val logsBuilder = getOrCreateTagBuilder(BlockTags.LOGS)
        logsBuilder.add(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_log"))!!)
        logsBuilder.add(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "stripped_rubber_log"))!!)
        logsBuilder.add(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_wood"))!!)

        for (block in BuiltInRegistries.BLOCK) {
            val id = BuiltInRegistries.BLOCK.getId(block)
            if (id.namespace != Ic2_120.MOD_ID) continue

            if (block is Ic2ScaffoldBlock) climbableBuilder.add(block)
            if (id.path in rubberLogPaths) {
                logsBuilder.add(block)
                logsThatBurnBuilder.add(block)
            }
            if (id.path == "rubber_sapling") {
                saplingBuilder.add(block)
            }

            when {
                id.path == "rubber_leaves" -> {
                    hoeBuilder.add(block)
                    getOrCreateTagBuilder(BlockTags.LEAVES).add(block)
                }

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
        // c: 和 forge: 各自直接放入方块，不交叉引用，避免与 connector 的 c:→forge: 桥接产生循环依赖
        getOrCreateTagBuilder(cTag).setReplace(false).add(block)
        getOrCreateTagBuilder(forgeTag).setReplace(false).add(block)
        if (compatPathsBuilt.add(path)) {
            val compatTag = compatBlock(path)
            getOrCreateTagBuilder(compatTag).setReplace(false).addTag(cTag).addTag(forgeTag)
        }
    }

    private fun cBlock(path: String): TagKey<Block> =
        TagKey.of(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path))

    private fun forgeBlock(path: String): TagKey<Block> =
        TagKey.of(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", path))

    private fun compatBlock(path: String): TagKey<Block> =
        TagKey.of(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("ic2_120", "compat/$path"))
}
