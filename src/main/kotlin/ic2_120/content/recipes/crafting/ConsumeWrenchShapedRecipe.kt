package ic2_120.content.recipes.crafting

import com.mojang.serialization.MapCodec
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
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
import ic2_120.getCustomData
import ic2_120.getOrCreateCustomData

private val EMPTY_LOOKUP = RegistryWrapper.WrapperLookup.of(Stream.empty())

/**
 * 工作台有序配方：消耗参与合成的扳手/电动工具，完全不返回余物，并继承电量累加功能。
 */
class ConsumeWrenchShapedRecipe(delegate: ShapedRecipe) : ShapedRecipe(
    delegate.group,
    delegate.category,
    RawShapedRecipe(delegate.width, delegate.height, delegate.ingredients, Optional.empty()),
    delegate.getResult(EMPTY_LOOKUP),
    delegate.showNotification()
) {
    override fun craft(input: CraftingRecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        val result = super.craft(input, lookup)

        val totalEnergy = sumEnergyFromIngredients(input)
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

    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
        return DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY)
    }

    override fun getSerializer(): RecipeSerializer<*> = ConsumeWrenchShapedRecipeSerializer
}

private fun sumEnergyFromIngredients(input: CraftingRecipeInput): Long {
    var total = 0L
    for (slot in 0 until input.getSize()) {
        val stack = input.getStackInSlot(slot)
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
}

object ConsumeWrenchShapedRecipeDatagen {
    fun offer(
        exporter: RecipeExporter,
        recipeId: Identifier,
        result: Item,
        pattern: List<String>,
        keys: Map<Char, Item>,
        count: Int = 1,
        category: String = "misc"
    ) {
        val ingredientMap = keys.mapValues { (_, item) -> Ingredient.ofItems(item) }
        val raw = RawShapedRecipe.create(ingredientMap, pattern)
        val resultStack = ItemStack(result, count)
        val shaped = ShapedRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), raw, resultStack)
        val recipe = ConsumeWrenchShapedRecipe(shaped)
        exporter.accept(recipeId, recipe, null)
    }
}
