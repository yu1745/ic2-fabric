package ic2_120.content.recipes

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import ic2_120.registry.ClassScanner
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import java.util.concurrent.CompletableFuture

/**
 * 工作台配方数据生成器
 * 从 ClassScanner 收集的配方生成器生成配方 JSON 文件
 *
 * 注意：ClassScanner 在 Ic2_120.onInitialize() 时扫描并收集配方生成器，
 * datagen 任务运行时会调用 generate() 方法执行实际的配方生成。
 */
class ModRecipeProvider(output: FabricPackOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) : FabricRecipeProvider(output, registriesFuture) {

    override fun createRecipeProvider(registries: HolderLookup.Provider, recipeExporter: RecipeOutput): RecipeProvider =
        object : RecipeProvider(registries, recipeExporter) {
            override fun buildRecipes() {
                // Fabric datagen runs before the first server resource reload in 26.2.
                // ItemStack constructors require the item holder's default components.
                net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                    .build(registries).forEach { it.apply() }
                ModMachineRecipes.register()
                ClassScanner.generateAllRecipes(recipeExporter)
            }
        }

    override fun getName(): String = "IC2 recipes"
}
