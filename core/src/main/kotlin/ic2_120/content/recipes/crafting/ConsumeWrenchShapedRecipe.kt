package ic2_120.content.recipes.crafting

import net.minecraft.core.registries.BuiltInRegistries

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.serialization.MapCodec
import ic2_120.compat.orCreateNbt
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.recipes.INGREDIENT_CODEC
import ic2_120.content.sync.EnergyStorageSync
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingBookCategory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapedRecipePattern

/**
 * 工作台有序配方：消耗参与合成的扳手/电动工具，完全不返回余物，并继承电量累加功能。
 */
class ConsumeWrenchShapedRecipe(
    private val patternRows: List<String>,
    private val patternKeys: Map<Char, Ingredient>,
    result: ItemStack,
    group: String = "",
) : ShapedRecipe(group, CraftingBookCategory.MISC, ShapedRecipePattern.of(patternKeys, patternRows), result, true) {
    override fun assemble(input: CraftingInput, registryAccess: HolderLookup.Provider): ItemStack {
        val result = super.assemble(input, registryAccess)

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

    override fun getRemainingItems(input: CraftingInput): NonNullList<ItemStack> {
        // 完全消耗所有材料，不返回任何东西
        return NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY)
    }

    override fun getSerializer(): RecipeSerializer<*> = ConsumeWrenchShapedRecipeSerializer
}

private fun sumEnergyFromIngredients(inventory: CraftingInput): Long {
    var total = 0L
    for (slot in 0 until inventory.getContainerSize()) {
        val stack = inventory.getItem(slot)
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
        val id = BuiltInRegistries.ITEM.getKey(item) ?: return 0L
        val config = EnergyStorageConfig.fromBlockPath(id.path) ?: return 0L

        // TODO(1.21.1): 原 yarn 语义为 getSubNbt("BlockEntityTag")，组件时代暂以 CUSTOM_DATA NBT 桥接
        if (stack.orCreateNbt.getBoolean(EnergyStorageBlock.NBT_FULL)) {
            return config.capacity
        }

        val blockEntityTag = stack.orCreateNbt.getCompound(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        if (blockEntityTag.isEmpty) return 0L
        return blockEntityTag
            .getLong(EnergyStorageSync.NBT_ENERGY_STORED)
            .coerceIn(0L, config.capacity)
    }

    return 0L
}

private fun setStorageEnergy(result: ItemStack, energy: Long) {
    val id = BuiltInRegistries.ITEM.getKey(result.item) ?: return
    val config = EnergyStorageConfig.fromBlockPath(id.path) ?: return
    val clamped = energy.coerceIn(0L, config.capacity)

    if (clamped >= config.capacity) {
        result.orCreateNbt = result.orCreateNbt.apply { putBoolean(EnergyStorageBlock.NBT_FULL, true) }
        result.orCreateNbt = result.orCreateNbt.apply { remove(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG) }
        return
    }

    val nbt = result.orCreateNbt
    nbt.putBoolean(EnergyStorageBlock.NBT_FULL, false)

    if (clamped <= 0L) {
        nbt.remove(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        result.orCreateNbt = nbt
        return
    }

    val blockEntityTag = nbt.getCompound(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
    blockEntityTag.putLong(EnergyStorageSync.NBT_ENERGY_STORED, clamped)
    nbt.put(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG, blockEntityTag)
    result.orCreateNbt = nbt
}

private fun Long.saturatingAdd(other: Long): Long {
    if (other <= 0L) return this
    return if (Long.MAX_VALUE - this < other) Long.MAX_VALUE else this + other
}

fun decodeConsumeWrenchShapedJson(id: ResourceLocation, json: JsonObject): ConsumeWrenchShapedRecipe {
    val keyMap = linkedMapOf<Char, Ingredient>()
    val keyObj = json.getAsJsonObject("key")
    for ((charKey, value) in keyObj.entrySet()) {
        keyMap[charKey[0]] = INGREDIENT_CODEC(value.asJsonObject)
    }
    val rows = json.getAsJsonArray("pattern").map { it.asString }
    val resultObj = json.getAsJsonObject("result")
    val resultItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resultObj.get("item").asString))
    val count = if (resultObj.has("count")) resultObj.get("count").asInt else 1
    return ConsumeWrenchShapedRecipe(rows, keyMap, ItemStack(resultItem, count))
}

fun encodeConsumeWrenchShapedJson(json: JsonObject, recipe: ConsumeWrenchShapedRecipe) {
    json.addProperty("type", "ic2_120:consume_wrench_shaped")

    val keyObj = JsonObject()
    for ((charKey, ingredient) in recipe.patternKeys) {
        val obj = JsonObject()
        val first = ingredient.items.firstOrNull()
        obj.addProperty("item", first?.let { BuiltInRegistries.ITEM.getKey(it.item)?.toString() } ?: "minecraft:air")
        keyObj.add(charKey.toString(), obj)
    }
    json.add("key", keyObj)

    val patternArr = JsonArray()
    for (row in recipe.patternRows) patternArr.add(row)
    json.add("pattern", patternArr)

    val result = recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY)
    val resultObj = JsonObject()
    BuiltInRegistries.ITEM.getKey(result.item)?.let { resultObj.addProperty("item", it.toString()) }
    if (result.count > 1) resultObj.addProperty("count", result.count)
    json.add("result", resultObj)
    json.addProperty("show_notification", true)
}

@ModMachineRecipe(id = "consume_wrench_shaped", recipeClass = ConsumeWrenchShapedRecipe::class)
object ConsumeWrenchShapedRecipeSerializer : RecipeSerializer<ConsumeWrenchShapedRecipe> {
    override fun codec(): MapCodec<ConsumeWrenchShapedRecipe> = ic2_120.content.recipes.RecipeCodecBridge.jsonMapCodec(
        "ic2_120:consume_wrench_shaped",
        ::decodeConsumeWrenchShapedJson,
        ::encodeConsumeWrenchShapedJson
    )

    // TODO(1.21.1): 配方不走网络同步（双端各自读 datapack），流编解码仅做占位实现
    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ConsumeWrenchShapedRecipe> {
        val delegate = RecipeSerializer.SHAPED_RECIPE.streamCodec()
        return StreamCodec.ofMember(
            { buf: RegistryFriendlyByteBuf, recipe: ConsumeWrenchShapedRecipe -> delegate.encode(buf, recipe) },
            { _ ->
                ConsumeWrenchShapedRecipe(listOf("X"), linkedMapOf('X' to Ingredient.EMPTY), ItemStack.EMPTY)
            }
        )
    }
}

object ConsumeWrenchShapedRecipeDatagen {
    fun offer(
        exporter: net.minecraft.data.recipes.RecipeOutput,
        recipeId: ResourceLocation,
        result: Item,
        pattern: List<String>,
        keys: Map<Char, Item>,
        count: Int = 1,
        category: String = "misc"
    ) {
        val keyMap = linkedMapOf<Char, Ingredient>()
        for ((charKey, item) in keys) keyMap[charKey] = Ingredient.of(item)
        val recipe = ConsumeWrenchShapedRecipe(pattern, keyMap, ItemStack(result, count))
        exporter.accept(recipeId, recipe, null)
    }
}
