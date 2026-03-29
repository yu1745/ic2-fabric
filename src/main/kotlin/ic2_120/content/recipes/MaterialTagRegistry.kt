package ic2_120.content.recipes

import kotlin.reflect.KClass

/**
 * 由 [ic2_120.registry.ClassScanner] 在注册物品/方块时根据 [@ModItem]/[@ModBlock] 的 materialTags 填充，
 * 供 datagen（[ModItemTagProvider] / [ModBlockTagProvider]）生成 c:/forge:/compat: 标签。
 */
object MaterialTagRegistry {
    val itemEntries: MutableList<Pair<KClass<*>, List<String>>> = mutableListOf()
    val blockEntries: MutableList<Pair<KClass<*>, List<String>>> = mutableListOf()

    fun clear() {
        itemEntries.clear()
        blockEntries.clear()
    }
}
