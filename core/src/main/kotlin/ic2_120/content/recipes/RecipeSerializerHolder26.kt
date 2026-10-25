package ic2_120.content.recipes

import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer

/** 26.2 recipe serializers are final records; keep annotated object identity here. */
abstract class RecipeSerializerHolder26<T : Recipe<*>> {
    abstract fun codec(): MapCodec<T>
    abstract fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, T>
    val serializer: RecipeSerializer<T> by lazy { RecipeSerializer(codec(), streamCodec()) }
}
