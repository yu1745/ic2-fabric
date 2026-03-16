package ic2_120.content.recipes

import ic2_120.registry.ClassScanner
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import java.util.function.Consumer

/**
 * 工作台配方数据生成器
 * 从 ClassScanner 收集的配方生成器生成配方 JSON 文件
 *
 * 注意：ClassScanner 在 Ic2_120.onInitialize() 时扫描并收集配方生成器，
 * datagen 任务运行时会调用 generate() 方法执行实际的配方生成。
 */
class ModRecipeProvider(output: FabricDataOutput) : FabricRecipeProvider(output) {

    override fun generate(recipeExporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
        // 调用 ClassScanner 中收集到的所有配方生成器
        ClassScanner.generateAllRecipes(recipeExporter)
    }
}
