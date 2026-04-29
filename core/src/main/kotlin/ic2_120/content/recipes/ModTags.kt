package ic2_120.content.recipes

import ic2_120.Ic2_120
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier

/**
 * 配方输入端统一使用 `ic2_120:compat/...` 标签；datagen 中将其展开为 c: + forge: 的并集。
 */
object ModTags {

    object Compat {
        object Items {
            val INGOTS_IRON = item("ingots/iron")
            val INGOTS_COPPER = item("ingots/copper")
            val INGOTS_GOLD = item("ingots/gold")
            val INGOTS_TIN = item("ingots/tin")
            val INGOTS_BRONZE = item("ingots/bronze")
            val INGOTS_LEAD = item("ingots/lead")
            val INGOTS_STEEL = item("ingots/steel")
            val INGOTS_SILVER = item("ingots/silver")
            val INGOTS_URANIUM = item("ingots/uranium")
            val INGOTS_REFINED_IRON = item("ingots/refined_iron")

            val PLATES_IRON = item("plates/iron")
            val PLATES_COPPER = item("plates/copper")
            val PLATES_GOLD = item("plates/gold")
            val PLATES_TIN = item("plates/tin")
            val PLATES_BRONZE = item("plates/bronze")
            val PLATES_LEAD = item("plates/lead")
            val PLATES_STEEL = item("plates/steel")
            val PLATES_LAPIS = item("plates/lapis")
            val PLATES_OBSIDIAN = item("plates/obsidian")

            val DUSTS_IRON = item("dusts/iron")
            val DUSTS_COPPER = item("dusts/copper")
            val DUSTS_GOLD = item("dusts/gold")
            val DUSTS_TIN = item("dusts/tin")
            val DUSTS_BRONZE = item("dusts/bronze")
            val DUSTS_LEAD = item("dusts/lead")
            val DUSTS_SILVER = item("dusts/silver")
            val DUSTS_COAL = item("dusts/coal")
            val DUSTS_DIAMOND = item("dusts/diamond")
            val DUSTS_SULFUR = item("dusts/sulfur")
            val DUSTS_LITHIUM = item("dusts/lithium")
            val DUSTS_OBSIDIAN = item("dusts/obsidian")
            val DUSTS_CLAY = item("dusts/clay")
            val DUSTS_STONE = item("dusts/stone")
            val DUSTS_NETHERRACK = item("dusts/netherrack")
            val DUSTS_ENDER_PEARL = item("dusts/ender_pearl")
            val DUSTS_SILICON_DIOXIDE = item("dusts/silicon_dioxide")
            val DUSTS_REDSTONE = item("dusts/redstone")
            val DUSTS_LAPIS = item("dusts/lapis")

            val GEMS_DIAMOND = item("gems/diamond")
            val GEMS_LAPIS = item("gems/lapis")

            val RUBBER = item("rubber")

            val RAW_MATERIALS_TIN = item("raw_materials/tin")
            val RAW_MATERIALS_LEAD = item("raw_materials/lead")
            val RAW_MATERIALS_URANIUM = item("raw_materials/uranium")

            val ORES_IRIDIUM = item("ores/iridium")

            private fun item(path: String): TagKey<Item> =
                TagKey.of(RegistryKeys.ITEM, Identifier(Ic2_120.MOD_ID, "compat/$path"))
        }

        object Blocks {
            val ORES_TIN = block("ores/tin")
            val ORES_LEAD = block("ores/lead")
            val ORES_URANIUM = block("ores/uranium")

            val STORAGE_BLOCKS_TIN = block("storage_blocks/tin")
            val STORAGE_BLOCKS_LEAD = block("storage_blocks/lead")
            val STORAGE_BLOCKS_BRONZE = block("storage_blocks/bronze")
            val STORAGE_BLOCKS_STEEL = block("storage_blocks/steel")
            val STORAGE_BLOCKS_SILVER = block("storage_blocks/silver")
            val STORAGE_BLOCKS_URANIUM = block("storage_blocks/uranium")

            private fun block(path: String): TagKey<Block> =
                TagKey.of(RegistryKeys.BLOCK, Identifier(Ic2_120.MOD_ID, "compat/$path"))
        }
    }
}
