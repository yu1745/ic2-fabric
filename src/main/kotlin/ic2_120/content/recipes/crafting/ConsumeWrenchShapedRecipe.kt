package ic2_120.content.recipes.crafting

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.sync.EnergyStorageSync
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.inventory.RecipeInputInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import java.util.function.Consumer
import ic2_120.getCustomData
import ic2_120.getOrCreateCustomData

/**
 * 工作台有序配方：消耗参与合成的扳手/电动工具，完全不返回余物，并继承电量累加功能。
 */
class ConsumeWrenchShapedRecipe(delegate: ShapedRecipe) : ShapedRecipe(
    delegate.id,
    delegate.group,
    delegate.category,
    delegate.width,
    delegate.height,
    delegate.ingredients,
    delegate.getResult(RegistryWrapper.WrapperLookup.EMPTY),
    delegate.showNotification()
) {
    override fun craft(inventory: RecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        val result = super.craft(inventory, registryManager)

        val totalEnergy = sumEnergyFromIngredients(inventory)
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

    override fun getRemainder(inventory: RecipeInputInventory): DefaultedList<ItemStack> {
        // 完全消耗所有材料，不返回任何东西
        return DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY)
    }

    override fun getSerializer(): RecipeSerializer<*> = ConsumeWrenchShapedRecipeSerializer
}

private fun sumEnergyFromIngredients(inventory: RecipeInputInventory): Long {
    var total = 0L
    for (slot in 0 until inventory.size()) {
        val stack = inventory.getStack(slot)
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

        if (stack.getCustomData()?.getBoolean(EnergyStorageBlock.NBT_FULL) == true) {
            return config.capacity
        }

        val blockEntityTag = stack.getCustomData()?.getCompound(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG) ?: return 0L
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
        result.getOrCreateCustomData().putBoolean(EnergyStorageBlock.NBT_FULL, true)
        result.getOrCreateCustomData().remove(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        return
    }

    val nbt = result.getOrCreateCustomData()
    nbt.putBoolean(EnergyStorageBlock.NBT_FULL, false)

    if (clamped <= 0L) {
        nbt.remove(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        return
    }

    val blockEntityTag = nbt.getCompound(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
    blockEntityTag.putLong(EnergyStorageSync.NBT_ENERGY_STORED, clamped)
    nbt.put(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG, blockEntityTag)
}

private fun Long.saturatingAdd(other: Long): Long {
    if (other <= 0L) return this
    return if (Long.MAX_VALUE - this < other) Long.MAX_VALUE else this + other
}

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
}

object ConsumeWrenchShapedRecipeDatagen {
    fun offer(
        exporter: Consumer<RecipeExporter>,
        recipeId: Identifier,
        result: Item,
        pattern: List<String>,
        keys: Map<Char, Item>,
        count: Int = 1,
        category: String = "misc"
    ) {
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
    ) : RecipeExporter {
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
    }
}
