package ic2_120.content.recipes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.JsonOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Recipe

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
        override fun <O : Any> decode(ops: DynamicOps<O>, input: O): DataResult<Pair<T, O>> =
            runCatching { ops.convertTo(JsonOps.INSTANCE, input) }.fold(
                { el ->
                    if (el != null && el.isJsonObject) {
                        runCatching { decode(DUMMY_ID, el.asJsonObject) }.fold(
                            { recipe -> DataResult.success(Pair(recipe, input)) },
                            { DataResult.error { it.message ?: "decode failed" } }
                        )
                    } else {
                        DataResult.error { "expected JsonObject, got: $el" }
                    }
                },
                { DataResult.error { it.message ?: "convert failed" } }
            )

        override fun <O : Any> encode(input: T, ops: DynamicOps<O>, prefix: O): DataResult<O> =
            runCatching {
                val json = JsonObject()
                encode(json, input)
                if (!json.has("type")) json.addProperty("type", jsonType)
                val el: JsonElement = JsonParser.parseString(json.toString())
                val converted = JsonOps.INSTANCE.convertTo(ops, el)
                DataResult.success(ops.mergeToPrimitive(prefix, converted).getOrThrow())
            }.fold(
                { it },
                { DataResult.error { it.message ?: "encode failed" } }
            )

        override fun <T1 : Any> keys(ops: DynamicOps<T1>): DataResult<java.util.stream.Stream<Pair<T1, T1>>> =
            DataResult.success(java.util.stream.Stream.empty())
    }
}

/** Ingredient JSON 解码桥（mojmap 1.21.1 无 Ingredient.fromJson(JsonObject)） */
fun INGREDIENT_CODEC(json: com.google.gson.JsonObject): net.minecraft.world.item.crafting.Ingredient =
    net.minecraft.world.item.crafting.Ingredient.CODEC
        .parse(JsonOps.INSTANCE, json)
        .getOrThrow()
