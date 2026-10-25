package ic2_120.content.recipes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.JsonOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.MapLike
import com.mojang.serialization.RecordBuilder
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.Recipe
import java.util.stream.Stream

/**
 * yarn 时代 RecipeSerializer（fromJson/read/write 三件套）→ mojmap 1.21.1
 * （codec(): MapCodec + streamCodec(): StreamCodec）的桥接。
 *
 * 各自定义序列化器保留 fromJson/read/write 私有实现，经此桥暴露给原版 RecipeManager。
 * JSON 直接整对象透传（含 type 字段），与 datagen 产出的格式一致。
 */
object RecipeCodecBridge {
    /** codec 解码时原版不再传入配方 id，用占位 id；真实 id 由 RecipeHolder 携带 */
    val DUMMY_ID: Identifier = Identifier.withDefaultNamespace("bridge")
    @PublishedApi internal val decodingOps = ThreadLocal<DynamicOps<*>?>()
    @PublishedApi internal val encodingOps = ThreadLocal<DynamicOps<*>?>()

    @Suppress("UNCHECKED_CAST")
    fun customIngredientFromJson(json: JsonObject): net.minecraft.world.item.crafting.Ingredient {
        val ops = decodingOps.get() as? DynamicOps<Any?>
            ?: throw IllegalStateException("Custom ingredient requires registry decode context")
        val encoded = JsonOps.INSTANCE.convertTo(ops, json)
        return net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops, encoded).getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    fun customIngredientJson(ingredient: net.minecraft.world.item.crafting.Ingredient): JsonObject {
        val ops = encodingOps.get() as? DynamicOps<Any?>
            ?: throw IllegalStateException("Custom ingredient requires registry encode context")
        val encoded = net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(ops, ingredient).getOrThrow()
        return ops.convertTo(JsonOps.INSTANCE, encoded).asJsonObject
    }

    @Suppress("UNCHECKED_CAST")
    fun ingredientFromId(id: String): net.minecraft.world.item.crafting.Ingredient {
        val ops = decodingOps.get() as? DynamicOps<Any?>
        if (ops != null) {
            return net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops, ops.createString(id)).getOrThrow()
        }
        return if (id.startsWith("#")) {
            ic2_120.compat.tagIngredient(
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.parse(id.substring(1)))
            )
        } else {
            net.minecraft.world.item.crafting.Ingredient.of(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(id))
            )
        }
    }

    inline fun <reified T : Recipe<*>> jsonMapCodec(
        jsonType: String,
        crossinline decode: (Identifier, JsonObject) -> T,
        crossinline encode: (JsonObject, T) -> Unit
    ): MapCodec<T> = object : MapCodec<T>() {
        override fun <O : Any?> decode(ops: DynamicOps<O>, input: MapLike<O>): DataResult<T> =
            runCatching {
                val json = JsonObject()
                input.entries().forEach { pair ->
                    // DFU 8：convertTo 直接返回目标类型（非 DataResult）
                    val key: JsonElement = ops.convertTo(JsonOps.INSTANCE, pair.first)
                    val value: JsonElement = ops.convertTo(JsonOps.INSTANCE, pair.second)
                    json.add(key.asString, value)
                }
                val previousOps = decodingOps.get()
                decodingOps.set(ops)
                try {
                    decode(DUMMY_ID, json)
                } finally {
                    decodingOps.set(previousOps)
                }
            }.fold(
                { DataResult.success(it) },
                { DataResult.error { it.message ?: "decode failed" } }
            )

        override fun <O : Any?> encode(input: T, ops: DynamicOps<O>, prefix: RecordBuilder<O>): RecordBuilder<O> {
                val json = JsonObject()
                val previousOps = encodingOps.get()
                encodingOps.set(ops)
                try {
                    encode(json, input)
                } finally {
                    encodingOps.set(previousOps)
                }
                if (!json.has("type")) json.addProperty("type", jsonType)
                val el: JsonElement = JsonParser.parseString(json.toString())
                val converted: O = JsonOps.INSTANCE.convertTo(ops, el)
                ops.getMapValues(converted).ifSuccess { pairs ->
                    pairs.forEach { pair -> prefix.add(pair.first, pair.second) }
                }
                return prefix
        }

        // DFU 8.0.16：MapCodec.keys 抽象签名为 <T> Stream<T> keys(DynamicOps<T>)（javap 实锤）
        override fun <O : Any?> keys(ops: DynamicOps<O>): Stream<O> = Stream.empty()
    }
}

/** Ingredient JSON 解码桥（mojmap 1.21.1 无 Ingredient.fromJson(JsonObject)） */
fun INGREDIENT_CODEC(json: com.google.gson.JsonObject): net.minecraft.world.item.crafting.Ingredient {
    return when {
        json.has("fabric:type") -> RecipeCodecBridge.customIngredientFromJson(json)
        json.has("tag") -> RecipeCodecBridge.ingredientFromId("#${json.get("tag").asString}")
        json.has("item") -> RecipeCodecBridge.ingredientFromId(json.get("item").asString)
        else -> throw IllegalArgumentException("Ingredient must contain item or tag: $json")
    }
}

/** Keep the legacy custom recipe JSON shape while retaining unresolved tags during datagen. */
fun ingredientJson26(ingredient: net.minecraft.world.item.crafting.Ingredient): JsonObject {
    if (ingredient.getCustomIngredient() != null) return RecipeCodecBridge.customIngredientJson(ingredient)
    val json = JsonObject()
    when (val display = ingredient.display()) {
        is net.minecraft.world.item.crafting.display.SlotDisplay.TagSlotDisplay ->
            json.addProperty("tag", display.tag().location().toString())
        is net.minecraft.world.item.crafting.display.SlotDisplay.Composite -> {
            val items = display.contents()
            require(items.size == 1) {
                "Expected one item in ingredient: $display"
            }
            val source = when (val entry = items[0]) {
                is net.minecraft.world.item.crafting.display.SlotDisplay.WithRemainder -> entry.input()
                else -> entry
            }
            require(source is net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay) {
                "Expected one item in ingredient: $display"
            }
            val item = source
            json.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.item().value()).toString())
        }
        else -> throw IllegalArgumentException("Unsupported ingredient: $display")
    }
    return json
}

fun stackJson26(stack: net.minecraft.world.item.ItemStack): JsonObject {
    require(!stack.isEmpty) { "Cannot encode an empty recipe result" }
    val json = JsonObject()
    json.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.item).toString())
    if (stack.count > 1) json.addProperty("count", stack.count)
    return json
}
