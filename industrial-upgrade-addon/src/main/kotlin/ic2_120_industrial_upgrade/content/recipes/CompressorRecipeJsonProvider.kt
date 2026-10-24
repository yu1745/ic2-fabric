package ic2_120_industrial_upgrade.content.recipes

import com.google.gson.JsonObject
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.recipe.Ingredient
import net.minecraft.util.Identifier

/**
 * 产出 ic2-fabric core 压缩机配方 JSON。
 * type 字段固定为 "ic2_120:compressing"（core 的 @ModMachineRecipe(id="compressing")）。
 */
class CompressorRecipeJsonProvider(
    private val recipeId: Identifier,
    private val input: Ingredient,
    private val inputCount: Int,
    private val outputIdStr: String,
    private val containerReturnId: String?
) : RecipeExporter {

    override fun serialize(json: JsonObject) {
        json.addProperty("type", "ic2_120:compressing")
        json.add("ingredient", input.toJson())
        json.addProperty("input_count", inputCount)

        val result = JsonObject()
        result.addProperty("item", outputIdStr)
        result.addProperty("count", 1)
        json.add("result", result)

        if (containerReturnId != null) {
            val container = JsonObject()
            container.addProperty("item", containerReturnId)
            container.addProperty("count", 1)
            json.add("container_return", container)
        }
    }

    override fun getSerializer() = ic2_120.content.recipes.ModMachineRecipes.recipeSerializer(
        ic2_120.content.recipes.compressor.CompressorRecipe::class
    )

    override fun getRecipeId(): Identifier = recipeId

    override fun toAdvancementJson(): JsonObject? = null

    override fun getAdvancementId(): Identifier? = null
}
