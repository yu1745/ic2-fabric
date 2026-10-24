package ic2_120.content.recipes.crafting

<<<<<<< HEAD
import com.google.gson.JsonArray
import com.google.gson.JsonObject
=======
import com.mojang.serialization.MapCodec
>>>>>>> backup/1.21.1
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
<<<<<<< HEAD
import ic2_120.registry.annotation.ModMachineRecipe
import ic2_120.content.sync.EnergyStorageSync
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
import ic2_120.content.sync.EnergyStorageSync
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
import ic2_120.editCustomData
import ic2_120.getCustomData

private val EMPTY_LOOKUP = RegistryWrapper.WrapperLookup.of(Stream.empty())
>>>>>>> backup/1.21.1

/**
 * 工作台有序配方：消耗参与合成的扳手/电动工具，完全不返回余物，并继承电量累加功能。
 */
<<<<<<< HEAD
class ConsumeWrenchShapedRecipe(delegate: ShapedRecipe) : ShapedRecipe(
    delegate.id,
    delegate.group,
    delegate.category,
    delegate.width,
    delegate.height,
    delegate.ingredients,
    delegate.getOutput(DynamicRegistryManager.EMPTY),
    delegate.showNotification()
) {
    override fun craft(inventory: RecipeInputInventory, registryManager: DynamicRegistryManager): ItemStack {
        val result = super.craft(inventory, registryManager)

        val totalEnergy = sumEnergyFromIngredients(inventory)
=======
class ConsumeWrenchShapedRecipe(delegate: ShapedRecipe, raw: RawShapedRecipe = RawShapedRecipe(delegate.width, delegate.height, delegate.ingredients, Optional.empty())) : ShapedRecipe(
    delegate.group,
    delegate.category,
    raw,
    delegate.getResult(EMPTY_LOOKUP),
    delegate.showNotification()
) {
    override fun craft(input: CraftingRecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        val result = super.craft(input, lookup)

        val totalEnergy = sumEnergyFromIngredients(input)
>>>>>>> backup/1.21.1
        if (totalEnergy <= 0L) return result

        when (val resultItem = result.item) {
            is IBatteryItem -> {
                resultItem.setCurrentCharge(result, totalEnergy)
            }

            is IElectricTool -> {
                resultItem.setEnergy(result, totalEnergy.coerceIn(0L, resultItem.maxCapacity))
            }

            is EnergyStorageBlock.EnergyStorageBlockItem -> {
                setStorageEnergy(result, totalEnergy)
            }
        }

        return result
    }

<<<<<<< HEAD
    override fun getRemainder(inventory: RecipeInputInventory): DefaultedList<ItemStack> {
        // 完全消耗所有材料，不返回任何东西
        return DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY)
=======
    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
        return DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY)
>>>>>>> backup/1.21.1
    }

    override fun getSerializer(): RecipeSerializer<*> = ConsumeWrenchShapedRecipeSerializer
}

<<<<<<< HEAD
private fun sumEnergyFromIngredients(inventory: RecipeInputInventory): Long {
    var total = 0L
    for (slot in 0 until inventory.size()) {
        val stack = inventory.getStack(slot)
=======
private fun sumEnergyFromIngredients(input: CraftingRecipeInput): Long {
    var total = 0L
    for (slot in 0 until input.getSize()) {
        val stack = input.getStackInSlot(slot)
>>>>>>> backup/1.21.1
        if (stack.isEmpty) continue
        total = total.saturatingAdd(getEnergyFromStack(stack))
    }
    return total
}

private fun getEnergyFromStack(stack: ItemStack): Long {
    val item = stack.item

    if (item is IBatteryItem) {
        return item.getCurrentCharge(stack).coerceAtLeast(0L)
    }

    if (item is IElectricTool) {
        return item.getEnergy(stack).coerceAtLeast(0L)
    }

    if (item is EnergyStorageBlock.EnergyStorageBlockItem) {
        val id = Registries.ITEM.getId(item)
        val config = EnergyStorageConfig.fromBlockPath(id.path) ?: return 0L

<<<<<<< HEAD
        if (stack.nbt?.getBoolean(EnergyStorageBlock.NBT_FULL) == true) {
            return config.capacity
        }

        val blockEntityTag = stack.getSubNbt(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG) ?: return 0L
=======
        if (stack.getCustomData()?.getBoolean(EnergyStorageBlock.NBT_FULL) == true) {
            return config.capacity
        }

        val blockEntityTag = stack.getCustomData()?.getCompound(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG) ?: return 0L
>>>>>>> backup/1.21.1
        return blockEntityTag
            .getLong(EnergyStorageSync.NBT_ENERGY_STORED)
            .coerceIn(0L, config.capacity)
    }

    return 0L
}

private fun setStorageEnergy(result: ItemStack, energy: Long) {
    val id = Registries.ITEM.getId(result.item)
    val config = EnergyStorageConfig.fromBlockPath(id.path) ?: return
    val clamped = energy.coerceIn(0L, config.capacity)

    if (clamped >= config.capacity) {
<<<<<<< HEAD
        result.orCreateNbt.putBoolean(EnergyStorageBlock.NBT_FULL, true)
        result.orCreateNbt.remove(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        return
    }

    val nbt = result.orCreateNbt
    nbt.putBoolean(EnergyStorageBlock.NBT_FULL, false)

    if (clamped <= 0L) {
        nbt.remove(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        return
    }

    val blockEntityTag = nbt.getCompound(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
    blockEntityTag.putLong(EnergyStorageSync.NBT_ENERGY_STORED, clamped)
    nbt.put(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG, blockEntityTag)
=======
        result.editCustomData {
            it.putBoolean(EnergyStorageBlock.NBT_FULL, true)
            it.remove(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        }
        return
    }

    result.editCustomData { nbt ->
        nbt.putBoolean(EnergyStorageBlock.NBT_FULL, false)
        if (clamped <= 0L) {
            nbt.remove(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
            return@editCustomData
        }
        val blockEntityTag = nbt.getCompound(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        blockEntityTag.putLong(EnergyStorageSync.NBT_ENERGY_STORED, clamped)
        nbt.put(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG, blockEntityTag)
    }
>>>>>>> backup/1.21.1
}

private fun Long.saturatingAdd(other: Long): Long {
    if (other <= 0L) return this
    return if (Long.MAX_VALUE - this < other) Long.MAX_VALUE else this + other
}

<<<<<<< HEAD
@ModMachineRecipe(id = "consume_wrench_shaped", recipeClass = ConsumeWrenchShapedRecipe::class)
object ConsumeWrenchShapedRecipeSerializer : RecipeSerializer<ConsumeWrenchShapedRecipe> {
    override fun read(id: Identifier, json: JsonObject): ConsumeWrenchShapedRecipe {
        val shaped = RecipeSerializer.SHAPED.read(id, json)
        return ConsumeWrenchShapedRecipe(shaped)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): ConsumeWrenchShapedRecipe {
        val shaped = RecipeSerializer.SHAPED.read(id, buf)
        return ConsumeWrenchShapedRecipe(shaped)
    }

    override fun write(buf: PacketByteBuf, recipe: ConsumeWrenchShapedRecipe) {
        RecipeSerializer.SHAPED.write(buf, recipe)
    }
=======
object ConsumeWrenchShapedRecipeSerializer : RecipeSerializer<ConsumeWrenchShapedRecipe> {
    override fun codec(): MapCodec<ConsumeWrenchShapedRecipe> =
        ShapedRecipe.Serializer.CODEC.xmap(
            { ConsumeWrenchShapedRecipe(it) },
            { it }
        )

    override fun packetCodec(): PacketCodec<RegistryByteBuf, ConsumeWrenchShapedRecipe> =
        ShapedRecipe.Serializer.PACKET_CODEC.xmap(
            { ConsumeWrenchShapedRecipe(it) },
            { it }
        )
>>>>>>> backup/1.21.1
}

object ConsumeWrenchShapedRecipeDatagen {
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
            json.addProperty("type", "ic2_120:consume_wrench_shaped")
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

        override fun getSerializer(): RecipeSerializer<*> = ConsumeWrenchShapedRecipeSerializer

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
=======
        val ingredientMap = keys.mapValues { (_, item) -> Ingredient.ofItems(item) }
        val raw = RawShapedRecipe.create(ingredientMap, pattern)
        val resultStack = ItemStack(result, count)
        val shaped = ShapedRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), raw, resultStack)
        val recipe = ConsumeWrenchShapedRecipe(shaped, raw)
        exporter.accept(recipeId, recipe, null)
>>>>>>> backup/1.21.1
    }
}
