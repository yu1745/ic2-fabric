package ic2_120.content.recipes

import ic2_120.registry.instance
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

/**
 * 根据 [@ModItem] materialTags 与原版兼容项生成 c:/forge:/ic2_120:compat/ 物品标签。
 * 同时根据 [@ModItem] tags 注册到指定的原版/第三方标签。
 */
class ModItemTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<net.minecraft.registry.RegistryWrapper.WrapperLookup>,
) : FabricTagProvider.ItemTagProvider(output, registriesFuture) {

    private val compatPathsBuilt = mutableSetOf<String>()

    override fun configure(registries: net.minecraft.registry.RegistryWrapper.WrapperLookup) {
        for ((clazz, paths) in MaterialTagRegistry.itemEntries) {
            @Suppress("UNCHECKED_CAST")
            val item = (clazz as KClass<out Item>).instance()
            for (path in paths) {
                registerModItemPath(path, item)
            }
        }

        for ((clazz, paths) in MaterialTagRegistry.blockEntries) {
            @Suppress("UNCHECKED_CAST")
            val blockItem = (clazz as KClass<out Block>).instance().asItem()
            if (blockItem === Items.AIR) continue
            for (path in paths) {
                registerModItemPath(path, blockItem)
            }
        }

        for ((clazz, tags) in MaterialTagRegistry.itemTagEntries) {
            @Suppress("UNCHECKED_CAST")
            val item = (clazz as KClass<out Item>).instance()
            for (tagId in tags) {
                val parts = tagId.split(":", limit = 2)
                val namespace = parts[0]
                val path = if (parts.size == 2) parts[1] else parts[0]
                val tagKey = TagKey.of(RegistryKeys.ITEM, Identifier.of(namespace, path))
                getOrCreateTagBuilder(tagKey).add(item)
            }
        }

        registerVanillaCompat("ingots/iron", Items.IRON_INGOT)
        registerVanillaCompat("ingots/copper", Items.COPPER_INGOT)
        registerVanillaCompat("ingots/gold", Items.GOLD_INGOT)
        registerVanillaCompat("dusts/redstone", Items.REDSTONE)
        registerVanillaCompat("gems/diamond", Items.DIAMOND)
        registerVanillaCompat("gems/lapis", Items.LAPIS_LAZULI)

        // 原版矿石（包含 deepslate 变体）
        registerVanillaCompat("ores/iron", Items.IRON_ORE)
        registerVanillaCompat("ores/iron", Items.DEEPSLATE_IRON_ORE)
        registerVanillaCompat("ores/gold", Items.GOLD_ORE)
        registerVanillaCompat("ores/gold", Items.DEEPSLATE_GOLD_ORE)
        registerVanillaCompat("ores/copper", Items.COPPER_ORE)
        registerVanillaCompat("ores/copper", Items.DEEPSLATE_COPPER_ORE)
        registerVanillaCompat("ores/coal", Items.COAL_ORE)
        registerVanillaCompat("ores/coal", Items.DEEPSLATE_COAL_ORE)

        // 原版原矿
        registerVanillaCompat("raw_materials/iron", Items.RAW_IRON)
        registerVanillaCompat("raw_materials/gold", Items.RAW_GOLD)
        registerVanillaCompat("raw_materials/copper", Items.RAW_COPPER)
    }

    private fun registerModItemPath(path: String, item: Item) {
        val cTag = cItem(path)
        val forgeTag = forgeItem(path)
        // c: 和 forge: 各自直接放入物品，不交叉引用，避免与 connector 的 c:→forge: 桥接产生循环依赖
        getOrCreateTagBuilder(cTag).setReplace(false).add(item)
        getOrCreateTagBuilder(forgeTag).setReplace(false).add(item)
        if (compatPathsBuilt.add(path)) {
            buildCompatItem(path, cTag, forgeTag)
        }
    }

    private fun registerVanillaCompat(path: String, item: Item) {
        val cTag = cItem(path)
        val forgeTag = forgeItem(path)
        getOrCreateTagBuilder(cTag).setReplace(false).add(item)
        getOrCreateTagBuilder(forgeTag).setReplace(false).add(item)
        if (compatPathsBuilt.add(path)) {
            buildCompatItem(path, cTag, forgeTag)
        }
    }

    private fun buildCompatItem(path: String, cTag: TagKey<Item>, forgeTag: TagKey<Item>) {
        val compatTag = compatItem(path)
        getOrCreateTagBuilder(compatTag).addTag(cTag).addTag(forgeTag)
    }

    private fun cItem(path: String): TagKey<Item> =
        TagKey.of(RegistryKeys.ITEM, Identifier.of("c", path))

    private fun forgeItem(path: String): TagKey<Item> =
        TagKey.of(RegistryKeys.ITEM, Identifier.of("forge", path))

    private fun compatItem(path: String): TagKey<Item> =
        TagKey.of(RegistryKeys.ITEM, Identifier.of("ic2_120", "compat/$path"))
}
