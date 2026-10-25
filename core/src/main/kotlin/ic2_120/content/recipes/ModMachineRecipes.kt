package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.registry.ClassScanner
import ic2_120.content.recipes.metalformer.CuttingRecipe
import ic2_120.content.recipes.metalformer.ExtrudingRecipe
import ic2_120.content.recipes.metalformer.MetalFormerRecipe
import ic2_120.content.recipes.metalformer.RollingRecipe
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import kotlin.reflect.KClass

object ModMachineRecipes {

    private var initialized = false

    private val recipeTypeByRecipeClass = mutableMapOf<KClass<out Recipe<*>>, RecipeType<*>>()
    private val recipeSerializerByRecipeClass = mutableMapOf<KClass<out Recipe<*>>, RecipeSerializer<*>>()
    private val recipeTypeBySerializerClass = mutableMapOf<KClass<*>, RecipeType<*>>()
    private val recipeTypeByBlockEntityClass = mutableMapOf<KClass<*>, RecipeType<*>>()

    /**
     * 从 [ic2_120.registry.annotation.ModMachineRecipe] 扫描并注册，
     * 再应用 [ic2_120.registry.annotation.ModMachineRecipeBinding] 建立 BlockEntity → RecipeType。
     */
    /** 可安全多次调用（已初始化则直接返回），便于 datagen 与主类入口共用。 */
    fun register() {
        if (initialized) return
        val entries = ClassScanner.collectMachineRecipeRegistrations()
        val modId = Ic2_120.MOD_ID
        for (e in entries) {
            val serializerHolder = e.serializerClass.objectInstance
                ?: error("机器配方序列化器 ${e.serializerClass} 必须是 Kotlin object")
            val serializer = serializerHolder.serializer
            val type = object : RecipeType<Recipe<*>> {
                override fun toString() = "$modId:${e.id}"
            }
            Registry.register(BuiltInRegistries.RECIPE_TYPE, Ic2_120.id(e.id), type)
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Ic2_120.id(e.id), serializer)
            recipeTypeByRecipeClass[e.recipeClass] = type
            recipeSerializerByRecipeClass[e.recipeClass] = serializer
            recipeTypeBySerializerClass[e.serializerClass] = type
        }
        aliasMetalFormerRecipeVariants()
        for ((beClass, serializerClass) in ClassScanner.collectMachineRecipeBindings()) {
            val rt = recipeTypeBySerializerClass[serializerClass]
                ?: error(
                    "BlockEntity ${beClass.simpleName} 的 @ModMachineRecipeBinding 指向未注册的序列化器 $serializerClass"
                )
            recipeTypeByBlockEntityClass[beClass] = rt
        }
        initialized = true
    }

    /** 辊压/切割/挤压子类与 [MetalFormerRecipe] 共用同一 RecipeType / Serializer */
    private fun aliasMetalFormerRecipeVariants() {
        val t = recipeTypeByRecipeClass[MetalFormerRecipe::class] ?: return
        val s = recipeSerializerByRecipeClass[MetalFormerRecipe::class] ?: return
        for (k in listOf(RollingRecipe::class, CuttingRecipe::class, ExtrudingRecipe::class)) {
            recipeTypeByRecipeClass[k] = t
            recipeSerializerByRecipeClass[k] = s
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Recipe<*>> recipeType(recipeClass: KClass<T>): RecipeType<T> =
        recipeTypeByRecipeClass[recipeClass] as? RecipeType<T>
            ?: error("未注册的配方类: $recipeClass（序列化器需带 @ModMachineRecipe 且已先 register）")

    @Suppress("UNCHECKED_CAST")
    fun <T : Recipe<*>> recipeSerializer(recipeClass: KClass<T>): RecipeSerializer<T> =
        recipeSerializerByRecipeClass[recipeClass] as? RecipeSerializer<T>
            ?: error("未注册的配方类: $recipeClass")

    /**
     * 由 [ic2_120.registry.annotation.ModMachineRecipeBinding] 解析，例如：
     * `recipeManager.getFirstMatch(ModMachineRecipes.getRecipeType(this::class), …)`。
     */
    fun getRecipeType(blockEntityClass: KClass<*>): RecipeType<*> =
        recipeTypeByBlockEntityClass[blockEntityClass]
            ?: error(
                "未找到 BlockEntity $blockEntityClass 的配方类型，请添加 @ModMachineRecipeBinding(serializerClass = …)"
            )
}

/**
 * 当前机器 BlockEntity 在 [ic2_120.registry.annotation.ModMachineRecipeBinding] 中绑定的 [RecipeType]。
 * 例：`getRecipeType<MaceratorRecipe>()`、`ModMachineRecipes.getRecipeType<MaceratorRecipe>(MaceratorBlockEntity::class)`。
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified R : Recipe<*>> BlockEntity.getRecipeType(): RecipeType<R> =
    ModMachineRecipes.getRecipeType(this::class) as RecipeType<R>
