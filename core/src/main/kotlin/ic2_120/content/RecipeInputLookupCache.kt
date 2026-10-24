package ic2_120.content

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.server.level.ServerLevel

/**
 * 按配方类型键控的「item → 是否存在任一匹配配方」全局布尔缓存（P0，修正版设计）。
 *
 * 用途：pulling 升级试探与机器 isXxxInput 的快速否定——避免对每个候选物品
 * 走 RecipeManager.getFirstMatch 的全配方表线性扫描（SimpleContainer 分配 +
 * stream findFirst + Ingredient.test）。
 *
 * 设计要点（2026-08-26 审查结论）：
 *  - 键必须是 (RecipeType, Item)，不能是裸 Item：不同机器用不同配方类型
 *    （SMELTING/MACERATING/...），共享裸 Item 缓存会跨机器改写 isValid 语义。
 *  - 不按 Level 分键：1.20.1 RecipeManager 是 server 级单例（三维度共用），
 *    按 Level 分键只是三份冗余；按 ServerLevel 弱键会随维度卸载丢缓存（无必要）。
 *  - 失效：复用 RecipeCacheEpoch（/reload 已接线），epoch 变化即整表作废重建。
 *    表按需重填，不主动清空旧表（旧条目被新表替代后自然不可达）。
 *  - 单线程（服务端主线程）访问，无并发控制。
 *  - 上限：Item 数量受注册表约束（数千级），每类型一表，内存可忽略。
 *
 * 注意：只缓存「是否存在配方」的布尔结果，不缓存配方实例——机器自身的
 * cachedRecipe（实例语义：产出/耗量）仍由各机器维护，两者互不干扰。
 */
object RecipeInputLookupCache {

    private var epoch = -1
    private val tables = HashMap<RecipeType<*>, Object2ObjectOpenHashMap<Item, Boolean>>()

    /** 惰性建表：配方类型首次出现时创建。 */
    private fun tableFor(type: RecipeType<*>): Object2ObjectOpenHashMap<Item, Boolean> =
        tables.getOrPut(type) { Object2ObjectOpenHashMap() }

    /**
     * 查询 item 在指定配方类型下是否有任一匹配配方（无输入数量语义，仅做形状匹配的最快路径）。
     *
     * miss 时执行一次 getFirstMatch 并写缓存；epoch 过期后首次调用重建。
     * 调用方需保证 world 为服务端世界。
     */
    fun hasAnyRecipe(type: RecipeType<*>, item: Item, world: ServerLevel): Boolean {
        val cur = RecipeCacheEpoch.current()
        if (cur != epoch) {
            tables.clear()
            epoch = cur
        }
        val table = tableFor(type)
        val cached = table.get(item)
        if (cached != null) return cached
        val probe = net.minecraft.world.SimpleContainer(net.minecraft.world.item.ItemStack(item, 1))
        @Suppress("UNCHECKED_CAST")
        val found = world.recipeManager
            .getFirstMatch(type as RecipeType<net.minecraft.world.item.crafting.Recipe<net.minecraft.world.SimpleContainer>>, probe, world)
            .isPresent
        table.put(item, found)
        return found
    }
}
