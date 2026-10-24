package ic2_120.integration.jei

import ic2_120.compat.world

import net.minecraft.client.Minecraft

/**
 * [LiveRecipeSource] 的客户端实现：从当前 client world 获取 RecipeManager。
 *
 * 放在 client sourceSet 以便引用 net.minecraft.client.Minecraft（client-only）。
 * 由 [ic2_120.Ic2_120Client.onInitializeClient] 注入到 [LiveRecipeSource.instance]。
 */
object ClientLiveRecipeSource : LiveRecipeSource {
    override fun clientRecipeManager() =
        Minecraft.getInstance().world?.recipeManager
}
