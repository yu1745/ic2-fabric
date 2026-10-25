package ic2_120.integration.jei

import net.minecraft.world.item.crafting.RecipeHolder

/**
 * 提供运行时 RecipeManager 的抽象（供 JEI plugin 收集实际加载的配方）。
 *
 * main sourceSet 无法直接引用 net.minecraft.client.Minecraft（client-only 类），
 * 故由 client sourceSet 在 [ic2_120.Ic2_120Client.onInitializeClient] 注入实现。
 *
 * 这样 JEI 的配方展示与机器实际判定逻辑（均基于 vanilla recipeManager）使用同一数据源，
 * 可覆盖 core + 所有附属命名空间的配方，而不仅限于 core 硬编码的 Entry 列表。
 */
interface LiveRecipeSource {
    /** 客户端当前世界同步下来的配方；level 未就绪时返回 null。 */
    fun clientRecipes(): Collection<RecipeHolder<*>>?

    companion object {
        @Volatile
        var instance: LiveRecipeSource? = null

        /**
         * 由 client source set 注入：把给定 action 调度到客户端主线程执行。
         * 返回 true=已调度（调用方应 return）；false=当前已在主线程（调用方继续同步执行）。
         *
         * 刻意放在 [LiveRecipeSource] 而非 [Ic2JeiPlugin]：本接口只引用 vanilla
         * `RecipeManager`，不引用任何 JEI 类；client source set（[ic2_120.Ic2_120Client]）
         * 在 `onInitializeClient` 设置本字段时，不会触发 `Ic2JeiPlugin`（继承
         * `mezz.jei.api.IModPlugin`）的类加载。Forge + Sinytra Connector 环境下若
         * 客户端未装 JEI，从 [Ic2JeiPlugin] 静态字段会引发 NoClassDefFoundError。
         *
         * 不直接引用 `Minecraft`：本文件在 main source set，而 `Minecraft`
         * 是 client-only 类，loom splitEnvironmentSourceSets 下 main 编译期不可见，故用注入。
         */
        @Volatile
        var scheduleOnClientThread: ((() -> Unit) -> Boolean)? = null

        @Volatile
        var refreshJeiRecipes: (() -> Unit)? = null
    }
}
