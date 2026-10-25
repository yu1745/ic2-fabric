package ic2_120.content.recipes.crafting

import ic2_120.compat.accept

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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapedRecipePattern

/**
 * 工作台有序配方：继承原料中的能量到成品。
 *
 * 原料电量来源：累加各格 [IBatteryItem]、[IElectricTool]、储电盒物品的已存 EU。
 *
 * - 成品为 [IBatteryItem] 时：写入成品（按 [IBatteryItem.maxCapacity] 截断）。
 * - 成品为 [IElectricTool] 时：写入成品（按 [IElectricTool.maxCapacity] 截断），例如用驱动把手合成钻头。
 * - 成品为 [EnergyStorageBlock.EnergyStorageBlockItem] 时：写回储电盒 NBT。
 */
class BatteryEnergyShapedRecipe(
    internal val patternRows: List<String>,
    internal val patternKeys: Map<Char, Ingredient>,
    result: ItemStack,
    group: String = "",
) : ShapedRecipe(net.minecraft.world.item.crafting.Recipe.CommonInfo(true), net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, group), ShapedRecipePattern.of(patternKeys, patternRows), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result)) {
    internal val recipeResult: ItemStack = result.copy()

    override fun assemble(input: CraftingInput): ItemStack {
        val result = super.assemble(input)

        val totalEnergy = sumEnergyFromIngredients(input)
        if (totalEnergy <= 0L) return result

        when (val resultItem = result.item) {
            is IBatteryItem -> {
                // setCurrentCharge 内部会按成品 maxCapacity 自动截断
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

    @Suppress("UNCHECKED_CAST")
    override fun getSerializer(): RecipeSerializer<ShapedRecipe> = BatteryEnergyShapedRecipeSerializer.serializer as RecipeSerializer<ShapedRecipe>
}

private fun sumEnergyFromIngredients(inventory: CraftingInput): Long {
    var total = 0L
    for (slot in 0 until inventory.size()) {
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
        if (stack.orCreateNbt.getBooleanOr(EnergyStorageBlock.NBT_FULL, false)) {
            return config.capacity
        }

        val blockEntityTag = stack.orCreateNbt.getCompoundOrEmpty(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
        if (blockEntityTag.isEmpty) return 0L
        return blockEntityTag
            .getLongOr(EnergyStorageSync.NBT_ENERGY_STORED, 0L)
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
        // 满电时清理 BlockEntityTag，保持与现有 Full 变体一致
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

    val blockEntityTag = nbt.getCompoundOrEmpty(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG)
    blockEntityTag.putLong(EnergyStorageSync.NBT_ENERGY_STORED, clamped)
    nbt.put(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG, blockEntityTag)
    result.orCreateNbt = nbt
}

fun decodeBatteryEnergyShapedJson(id: Identifier, json: JsonObject): BatteryEnergyShapedRecipe {
    val keyMap = linkedMapOf<Char, Ingredient>()
    val keyObj = json.getAsJsonObject("key")
    for ((charKey, value) in keyObj.entrySet()) {
        keyMap[charKey[0]] = INGREDIENT_CODEC(value.asJsonObject)
    }
    val rows = json.getAsJsonArray("pattern").map { it.asString }
    val resultObj = json.getAsJsonObject("result")
    val resultItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(resultObj.get("item").asString))
    val count = if (resultObj.has("count")) resultObj.get("count").asInt else 1
    return BatteryEnergyShapedRecipe(rows, keyMap, ItemStack(resultItem, count))
}

fun encodeBatteryEnergyShapedJson(json: JsonObject, recipe: BatteryEnergyShapedRecipe) {
    json.addProperty("type", "ic2_120:battery_energy_shaped")

    val keyObj = JsonObject()
    for ((charKey, ingredient) in recipe.patternKeys) {
        keyObj.add(charKey.toString(), ic2_120.content.recipes.ingredientJson26(ingredient))
    }
    json.add("key", keyObj)

    val patternArr = JsonArray()
    for (row in recipe.patternRows) patternArr.add(row)
    json.add("pattern", patternArr)

    val result = recipe.recipeResult.copy()
    val resultObj = JsonObject()
    BuiltInRegistries.ITEM.getKey(result.item)?.let {
        resultObj.addProperty("item", it.toString())
    }
    if (result.count > 1) resultObj.addProperty("count", result.count)
    json.add("result", resultObj)
    json.addProperty("show_notification", true)
}

@ModMachineRecipe(id = "battery_energy_shaped", recipeClass = BatteryEnergyShapedRecipe::class)
object BatteryEnergyShapedRecipeSerializer : ic2_120.content.recipes.RecipeSerializerHolder26<BatteryEnergyShapedRecipe>() {
    override fun codec(): MapCodec<BatteryEnergyShapedRecipe> = ic2_120.content.recipes.RecipeCodecBridge.jsonMapCodec(
        "ic2_120:battery_energy_shaped",
        ::decodeBatteryEnergyShapedJson,
        ::encodeBatteryEnergyShapedJson
    )

    // TODO(1.21.1): 配方不走网络同步（双端各自读 datapack），流编解码仅做占位实现
    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, BatteryEnergyShapedRecipe> =
        StreamCodec.ofMember(
            { recipe: BatteryEnergyShapedRecipe, buf: RegistryFriendlyByteBuf ->
                CraftingRecipeNetwork26.writeShaped(buf, recipe.patternRows, recipe.patternKeys, recipe.recipeResult)
            },
            { buf: RegistryFriendlyByteBuf ->
                val (rows, keys, result) = CraftingRecipeNetwork26.readShaped(buf)
                BatteryEnergyShapedRecipe(rows, keys, result)
            }
        )
}

object BatteryEnergyShapedRecipeDatagen {
    /**
     * @param category 原版配方书分类，如 [net.minecraft.data.recipes.RecipeCategory] 导出的 JSON：
     * `"misc"`、`"equipment"`（工具）、`"building"` 等。
     */
    fun offer(
        exporter: net.minecraft.data.recipes.RecipeOutput,
        recipeId: Identifier,
        result: Item,
        pattern: List<String>,
        keys: Map<Char, Item>,
        count: Int = 1,
        category: String = "misc"
    ) {
        val keyMap = linkedMapOf<Char, Ingredient>()
        for ((charKey, item) in keys) keyMap[charKey] = Ingredient.of(item)
        val recipe = BatteryEnergyShapedRecipe(pattern, keyMap, ItemStack(result, count))
        exporter.accept(recipeId, recipe, null)
    }
}

private fun Long.saturatingAdd(other: Long): Long {
    if (other <= 0L) return this
    return if (Long.MAX_VALUE - this < other) Long.MAX_VALUE else this + other
}
