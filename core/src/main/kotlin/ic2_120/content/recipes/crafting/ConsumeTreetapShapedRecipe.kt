package ic2_120.content.recipes.crafting

<<<<<<< HEAD
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.item.Treetap
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.inventory.RecipeInputInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import java.util.function.Consumer
=======
import com.mojang.serialization.MapCodec
import ic2_120.content.item.Treetap
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RawShapedRecipe
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import java.util.Optional
import java.util.stream.Stream

private val EMPTY_LOOKUP = RegistryWrapper.WrapperLookup.of(Stream.empty())
>>>>>>> backup/1.21.1

/**
 * 工作台有序配方：消耗参与合成的木龙头，而不是返回损耗后的余物。
 */
<<<<<<< HEAD
class ConsumeTreetapShapedRecipe(delegate: ShapedRecipe) : ShapedRecipe(
    delegate.id,
    delegate.group,
    delegate.category,
    delegate.width,
    delegate.height,
    delegate.ingredients,
    delegate.getOutput(DynamicRegistryManager.EMPTY),
    delegate.showNotification()
) {
    override fun getRemainder(inventory: RecipeInputInventory): DefaultedList<ItemStack> {
        val remainder = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY)
        for (slot in 0 until inventory.size()) {
            val stack = inventory.getStack(slot)
            if (stack.isEmpty) continue
            remainder[slot] = if (stack.item is Treetap) ItemStack.EMPTY else stack.item.getRecipeRemainder(stack)
=======
class ConsumeTreetapShapedRecipe(delegate: ShapedRecipe, raw: RawShapedRecipe = RawShapedRecipe(delegate.width, delegate.height, delegate.ingredients, Optional.empty())) : ShapedRecipe(
    delegate.group,
    delegate.category,
    raw,
    delegate.getResult(EMPTY_LOOKUP),
    delegate.showNotification()
) {
    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
        val remainder = DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY)
        for (slot in 0 until input.getSize()) {
            val stack = input.getStackInSlot(slot)
            if (stack.isEmpty) continue
            remainder[slot] = if (stack.item is Treetap) ItemStack.EMPTY
            else stack.item.recipeRemainder?.let { ItemStack(it) } ?: ItemStack.EMPTY
>>>>>>> backup/1.21.1
        }
        return remainder
    }

    override fun getSerializer(): RecipeSerializer<*> = ConsumeTreetapShapedRecipeSerializer
}

<<<<<<< HEAD
@ModMachineRecipe(id = "consume_treetap_shaped", recipeClass = ConsumeTreetapShapedRecipe::class)
object ConsumeTreetapShapedRecipeSerializer : RecipeSerializer<ConsumeTreetapShapedRecipe> {
    override fun read(id: Identifier, json: JsonObject): ConsumeTreetapShapedRecipe {
        val shaped = RecipeSerializer.SHAPED.read(id, json)
        return ConsumeTreetapShapedRecipe(shaped)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): ConsumeTreetapShapedRecipe {
        val shaped = RecipeSerializer.SHAPED.read(id, buf)
        return ConsumeTreetapShapedRecipe(shaped)
    }

    override fun write(buf: PacketByteBuf, recipe: ConsumeTreetapShapedRecipe) {
        RecipeSerializer.SHAPED.write(buf, recipe)
    }
=======
object ConsumeTreetapShapedRecipeSerializer : RecipeSerializer<ConsumeTreetapShapedRecipe> {
    override fun codec(): MapCodec<ConsumeTreetapShapedRecipe> =
        ShapedRecipe.Serializer.CODEC.xmap(
            { ConsumeTreetapShapedRecipe(it) },
            { it }
        )

    override fun packetCodec(): PacketCodec<RegistryByteBuf, ConsumeTreetapShapedRecipe> =
        ShapedRecipe.Serializer.PACKET_CODEC.xmap(
            { ConsumeTreetapShapedRecipe(it) },
            { it }
        )
>>>>>>> backup/1.21.1
}

object ConsumeTreetapShapedRecipeDatagen {
    fun offer(
<<<<<<< HEAD
        exporter: Consumer<RecipeJsonProvider>,
=======
        exporter: RecipeExporter,
>>>>>>> backup/1.21.1
        recipeId: Identifier,
        result: Item,
        pattern: List<String>,
        keys: Map<Char, Item>,
        count: Int = 1,
        category: String = "misc"
    ) {
<<<<<<< HEAD
        exporter.accept(
            Provider(
                recipeId = recipeId,
                result = result,
                pattern = pattern,
                keys = keys,
                count = count,
                category = category
            )
        )
    }

    private class Provider(
        private val recipeId: Identifier,
        private val result: Item,
        private val pattern: List<String>,
        private val keys: Map<Char, Item>,
        private val count: Int,
        private val category: String
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "ic2_120:consume_treetap_shaped")
            json.addProperty("category", category)

            val keyObj = JsonObject()
            for ((charKey, item) in keys) {
                val ingredient = JsonObject()
                ingredient.addProperty("item", Registries.ITEM.getId(item).toString())
                keyObj.add(charKey.toString(), ingredient)
            }
            json.add("key", keyObj)

            val patternArr = JsonArray()
            for (row in pattern) patternArr.add(row)
            json.add("pattern", patternArr)

            val resultObj = JsonObject()
            resultObj.addProperty("item", Registries.ITEM.getId(result).toString())
            if (count > 1) resultObj.addProperty("count", count)
            json.add("result", resultObj)
            json.addProperty("show_notification", true)
        }

        override fun getSerializer(): RecipeSerializer<*> = ConsumeTreetapShapedRecipeSerializer

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
=======
        val ingredientMap = keys.mapValues { (_, item) -> Ingredient.ofItems(item) }
        val raw = RawShapedRecipe.create(ingredientMap, pattern)
        val resultStack = ItemStack(result, count)
        val shaped = ShapedRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), raw, resultStack)
        val recipe = ConsumeTreetapShapedRecipe(shaped, raw)
        exporter.accept(recipeId, recipe, null)
>>>>>>> backup/1.21.1
    }
}
