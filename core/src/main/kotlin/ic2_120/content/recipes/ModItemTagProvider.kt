package ic2_120.content.recipes

import ic2_120.registry.instance
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

/**
 * 根据 [@ModItem] materialTags 与原版兼容项生成 c:/forge:/ic2_120:compat/ 物品标签。
 */
class ModItemTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<net.minecraft.registry.RegistryWrapper.WrapperLookup>,
) : FabricTagProvider.ItemTagProvider(output, registriesFuture) {

    override fun configure(registries: net.minecraft.registry.RegistryWrapper.WrapperLookup) {
        for ((clazz, paths) in MaterialTagRegistry.itemEntries) {
            @Suppress("UNCHECKED_CAST")
            val item = (clazz as KClass<out Item>).instance()
            for (path in paths) {
                registerModItemPath(path, item)
            }
        }

        registerVanillaCompat("ingots/iron", Items.IRON_INGOT)
        registerVanillaCompat("ingots/copper", Items.COPPER_INGOT)
        registerVanillaCompat("ingots/gold", Items.GOLD_INGOT)
        registerVanillaCompat("dusts/redstone", Items.REDSTONE)
        registerVanillaCompat("gems/diamond", Items.DIAMOND)
        registerVanillaCompat("gems/lapis", Items.LAPIS_LAZULI)
    }

    private fun registerModItemPath(path: String, item: Item) {
        val cTag = cItem(path)
        val forgeTag = forgeItem(path)
        // c: 和 forge: 各自直接放入物品，不交叉引用，避免与 connector 的 c:→forge: 桥接产生循环依赖
        getOrCreateTagBuilder(cTag).setReplace(false).add(item)
        getOrCreateTagBuilder(forgeTag).setReplace(false).add(item)
        buildCompatItem(path, cTag, forgeTag)
    }

    private fun registerVanillaCompat(path: String, item: Item) {
        val cTag = cItem(path)
        val forgeTag = forgeItem(path)
        getOrCreateTagBuilder(cTag).setReplace(false).add(item)
        getOrCreateTagBuilder(forgeTag).setReplace(false).add(item)
        buildCompatItem(path, cTag, forgeTag)
    }

    private fun buildCompatItem(path: String, cTag: TagKey<Item>, forgeTag: TagKey<Item>) {
        val compatTag = compatItem(path)
        getOrCreateTagBuilder(compatTag).setReplace(false).addTag(cTag).addTag(forgeTag)
    }

    private fun cItem(path: String): TagKey<Item> =
        TagKey.of(RegistryKeys.ITEM, Identifier("c", path))

    private fun forgeItem(path: String): TagKey<Item> =
        TagKey.of(RegistryKeys.ITEM, Identifier("forge", path))

    private fun compatItem(path: String): TagKey<Item> =
        TagKey.of(RegistryKeys.ITEM, Identifier("ic2_120", "compat/$path"))
}
