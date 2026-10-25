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
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
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
    val DUMMY_ID: ResourceLocation = ResourceLocation.withDefaultNamespace("bridge")

    fun ingredientJson(ingredient: Ingredient): JsonObject =
        Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow().asJsonObject

    fun stackJson(stack: ItemStack): JsonObject {
        require(!stack.isEmpty) { "Cannot encode an empty recipe output" }
        return JsonObject().apply {
            addProperty("item", BuiltInRegistries.ITEM.getKey(stack.item).toString())
            addProperty("count", stack.count)
        }
    }

    inline fun <reified T : Recipe<*>> jsonMapCodec(
        jsonType: String,
        crossinline decode: (ResourceLocation, JsonObject) -> T,
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
                decode(DUMMY_ID, json)
            }.fold(
                { DataResult.success(it) },
                { DataResult.error { it.message ?: "decode failed" } }
            )

        override fun <O : Any?> encode(input: T, ops: DynamicOps<O>, prefix: RecordBuilder<O>): RecordBuilder<O> =
            try {
                val json = JsonObject()
                encode(json, input)
                if (!json.has("type")) json.addProperty("type", jsonType)
                val el: JsonElement = JsonParser.parseString(json.toString())
                val converted: O = JsonOps.INSTANCE.convertTo(ops, el)
                ops.getMapValues(converted).ifSuccess { pairs ->
                    pairs.forEach { pair -> prefix.add(pair.first, pair.second) }
                }
                prefix
            } catch (t: Throwable) {
                throw IllegalStateException("Failed to encode $jsonType recipe", t)
            }

        // DFU 8.0.16：MapCodec.keys 抽象签名为 <T> Stream<T> keys(DynamicOps<T>)（javap 实锤）
        override fun <O : Any?> keys(ops: DynamicOps<O>): Stream<O> = Stream.empty()
    }
}

/** Ingredient JSON 解码桥（mojmap 1.21.1 无 Ingredient.fromJson(JsonObject)） */
fun INGREDIENT_CODEC(json: com.google.gson.JsonObject): net.minecraft.world.item.crafting.Ingredient =
    net.minecraft.world.item.crafting.Ingredient.CODEC
        .parse(JsonOps.INSTANCE, json)
        .result().orElseThrow()
