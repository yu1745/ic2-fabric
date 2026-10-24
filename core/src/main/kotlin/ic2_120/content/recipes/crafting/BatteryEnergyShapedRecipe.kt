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
import java.util.Optional
import java.util.stream.Stream
import ic2_120.editCustomData
import ic2_120.getCustomData

private val EMPTY_LOOKUP = RegistryWrapper.WrapperLookup.of(Stream.empty())
>>>>>>> backup/1.21.1

/**
 * 工作台有序配方：继承原料中的能量到成品。
 *
 * 原料电量来源：累加各格 [IBatteryItem]、[IElectricTool]、储电盒物品的已存 EU。
 *
 * - 成品为 [IBatteryItem] 时：写入成品（按 [IBatteryItem.maxCapacity] 截断）。
 * - 成品为 [IElectricTool] 时：写入成品（按 [IElectricTool.maxCapacity] 截断），例如用驱动把手合成钻头。
 * - 成品为 [EnergyStorageBlock.EnergyStorageBlockItem] 时：写回储电盒 NBT。
 */
<<<<<<< HEAD
class BatteryEnergyShapedRecipe(delegate: ShapedRecipe) : ShapedRecipe(
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
class BatteryEnergyShapedRecipe(delegate: ShapedRecipe, raw: RawShapedRecipe = RawShapedRecipe(delegate.width, delegate.height, delegate.ingredients, Optional.empty())) : ShapedRecipe(
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
<<<<<<< HEAD
                // setCurrentCharge 内部会按成品 maxCapacity 自动截断
=======
>>>>>>> backup/1.21.1
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

    override fun getSerializer(): RecipeSerializer<*> = BatteryEnergyShapedRecipeSerializer
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
        // 满电时清理 BlockEntityTag，保持与现有 Full 变体一致
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
}

@ModMachineRecipe(id = "battery_energy_shaped", recipeClass = BatteryEnergyShapedRecipe::class)
object BatteryEnergyShapedRecipeSerializer : RecipeSerializer<BatteryEnergyShapedRecipe> {
    override fun read(id: Identifier, json: JsonObject): BatteryEnergyShapedRecipe {
        val shaped = RecipeSerializer.SHAPED.read(id, json)
        return BatteryEnergyShapedRecipe(shaped)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): BatteryEnergyShapedRecipe {
        val shaped = RecipeSerializer.SHAPED.read(id, buf)
        return BatteryEnergyShapedRecipe(shaped)
    }

    override fun write(buf: PacketByteBuf, recipe: BatteryEnergyShapedRecipe) {
        RecipeSerializer.SHAPED.write(buf, recipe)
    }
}

object BatteryEnergyShapedRecipeDatagen {
    /**
     * @param category 原版配方书分类，如 [net.minecraft.recipe.book.RecipeCategory] 导出的 JSON：
     * `"misc"`、`"equipment"`（工具）、`"building"` 等。
     */
    fun offer(
        exporter: Consumer<RecipeJsonProvider>,
=======
        result.editCustomData {
            it.putBoolean(EnergyStorageBlock.NBT_FULL, true)
            // 满电时清理 BlockEntityTag，保持与现有 Full 变体一致
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
}

object BatteryEnergyShapedRecipeSerializer : RecipeSerializer<BatteryEnergyShapedRecipe> {
    override fun codec(): MapCodec<BatteryEnergyShapedRecipe> =
        RecipeSerializer.SHAPED.codec().xmap(
            { BatteryEnergyShapedRecipe(it) },
            { it }
        )

    override fun packetCodec(): PacketCodec<RegistryByteBuf, BatteryEnergyShapedRecipe> =
        RecipeSerializer.SHAPED.packetCodec().xmap(
            { BatteryEnergyShapedRecipe(it) },
            { it }
        )
}

object BatteryEnergyShapedRecipeDatagen {
    fun offer(
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
            json.addProperty("type", "ic2_120:battery_energy_shaped")
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

        override fun getSerializer(): RecipeSerializer<*> = BatteryEnergyShapedRecipeSerializer

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
=======
        val ingredientMap = keys.mapValues { (_, item) -> Ingredient.ofItems(item) }
        val raw = RawShapedRecipe.create(ingredientMap, pattern)
        val resultStack = ItemStack(result, count)
        val shaped = ShapedRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), raw, resultStack)
        val recipe = BatteryEnergyShapedRecipe(shaped, raw)
        exporter.accept(recipeId, recipe, null)
>>>>>>> backup/1.21.1
    }
}

private fun Long.saturatingAdd(other: Long): Long {
    if (other <= 0L) return this
    return if (Long.MAX_VALUE - this < other) Long.MAX_VALUE else this + other
}
