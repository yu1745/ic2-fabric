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

    inline fun <reified T : Recipe<*>> jsonMapCodec(
        jsonType: String,
        crossinline decode: (ResourceLocation, JsonObject) -> T,
        crossinline encode: (JsonObject, T) -> Unit
    ): MapCodec<T> = object : MapCodec<T>() {
        override fun <O : Any?> decode(ops: DynamicOps<O>, input: MapLike<O>): DataResult<T> =
            runCatching {
                val json = JsonObject()
                input.entries().forEach { pair ->
                    val key: JsonElement = ops.convertTo(JsonOps.INSTANCE, pair.first).result().orElseThrow()
                    val value: JsonElement = ops.convertTo(JsonOps.INSTANCE, pair.second).result().orElseThrow()
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
                val converted: O = JsonOps.INSTANCE.convertTo(ops, el).result().orElseThrow()
                ops.getMapValues(converted).ifSuccess { pairs ->
                    pairs.forEach { pair -> prefix.add(pair.first, pair.second) }
                }
            } catch (t: Throwable) {
                prefix
            }

        override fun <O : Any?> keys(ops: DynamicOps<O>): DataResult<Stream<com.mojang.datafixers.util.Pair<O, O>>> =
            DataResult.success(Stream.empty())
    }
}

/** Ingredient JSON 解码桥（mojmap 1.21.1 无 Ingredient.fromJson(JsonObject)） */
fun INGREDIENT_CODEC(json: com.google.gson.JsonObject): net.minecraft.world.item.crafting.Ingredient =
    net.minecraft.world.item.crafting.Ingredient.CODEC
        .parse(JsonOps.INSTANCE, json)
        .result().orElseThrow()
