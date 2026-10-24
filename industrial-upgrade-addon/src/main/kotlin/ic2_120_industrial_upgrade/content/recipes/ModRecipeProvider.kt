package ic2_120_industrial_upgrade.content.recipes

import ic2_120.content.item.Plutonium
import ic2_120.content.item.setFluidCellVariant
import ic2_120.registry.ClassScanner
import ic2_120.registry.instance
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import ic2_120_industrial_upgrade.content.fluid.NeutronFluid
import ic2_120_industrial_upgrade.content.item.Neutron
import ic2_120_industrial_upgrade.content.item.NeutronShard
import ic2_120_industrial_upgrade.content.item.Photoniy
import ic2_120_industrial_upgrade.content.item.PhotoniyIngot
import ic2_120_industrial_upgrade.content.item.Proton
import ic2_120_industrial_upgrade.content.item.ProtonShard
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import java.util.function.Consumer
import java.util.concurrent.CompletableFuture
import net.minecraft.core.HolderLookup

class ModRecipeProvider(output: FabricDataOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) : FabricRecipeProvider(output, registriesFuture) {

    override fun buildRecipes(exporter: RecipeOutput) {
        // 1. 扫描 @RecipeProvider 注解方法（工作台有序合成）
        ClassScanner.generateRecipesForMod(IC2IndustrialUpgrade.MOD_ID, exporter)

        // 2. 导线合成（11 档级联，导线由 core 的 ic2_120:cables entrypoint 注册，不走 @ModBlock，
        //    故其合成在此显式生成）
        ic2_120_industrial_upgrade.content.block.CableRecipes.generateRecipes(exporter)

        // 3. 压缩机配方（ic2_120:compressing，复用 core 的 CompressorRecipeSerializer）
        registerCompressorRecipes(exporter)
    }

    /**
     * 注册压缩机配方。core 的 CompressorRecipeSerializer JSON 格式：
     * { "type":"ic2_120:compressing", "ingredient":..., "input_count":N, "result":{item,count}[, "container_return":{...}] }
     */
    private fun registerCompressorRecipes(exporter: RecipeOutput) {
        // 光子锭：9×光子 → 1×光子锭
        compressorRecipe(exporter, "photoniy_to_ingot",
            input = Ingredient.of(Photoniy::class.instance().defaultInstance),
            inputCount = 9,
            outputId = "photoniy_ingot")

        // 质子碎片：1×钚 → 1×质子碎片
        compressorRecipe(exporter, "plutonium_to_proton_shard",
            input = Ingredient.of(Plutonium::class.instance().defaultInstance),
            inputCount = 1,
            outputId = "proton_shard")

        // 质子：18×质子碎片 → 1×质子
        compressorRecipe(exporter, "proton_shard_to_proton",
            input = Ingredient.of(ProtonShard::class.instance().defaultInstance),
            inputCount = 18,
            outputId = "proton")

        // 中子碎片：1×中子流体桶 → 1×中子碎片（返还空桶）
        compressorRecipe(exporter, "neutron_bucket_to_shard",
            input = Ingredient.of(NeutronFluid.NEUTRON_BUCKET),
            inputCount = 1,
            outputId = "neutron_shard",
            containerReturn = Items.BUCKET)

        // 中子碎片：1×中子流体单元 → 1×中子碎片（返还空流体单元），与桶等效
        val fluidCellItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "fluid_cell"))
        val emptyCellItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "empty_cell"))
        val neutronFluidCell = ItemStack(fluidCellItem).apply {
            setFluidCellVariant(FluidVariant.of(NeutronFluid.NEUTRON_STILL))
        }
        // vanilla 1.20.1 Ingredient 不支持 NBT 匹配（ofStacks.toJson() 会丢弃 NBT，test() 只比 item），
        // 用 Fabric 的 NbtIngredient 匹配"装了中子流体的流体单元"（partial：FluidVariant.fluid 子集即可），
        // 避免其他流体的 fluid_cell 被误压成中子碎片。
        compressorRecipe(exporter, "neutron_cell_to_shard",
            input = DefaultCustomIngredients.components(neutronFluidCell),
            inputCount = 1,
            outputId = "neutron_shard",
            containerReturn = emptyCellItem)

        // 中子：9×中子碎片 → 1×中子
        compressorRecipe(exporter, "neutron_shard_to_neutron",
            input = Ingredient.of(NeutronShard::class.instance().defaultInstance),
            inputCount = 9,
            outputId = "neutron")
    }

    private fun compressorRecipe(
        exporter: RecipeOutput,
        name: String,
        input: Ingredient,
        inputCount: Int,
        outputId: String,
        containerReturn: net.minecraft.world.item.Item? = null
    ) {
        val recipeId = IC2IndustrialUpgrade.id("compressing/$name")
        val outputItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath(IC2IndustrialUpgrade.MOD_ID, outputId))
        val recipe = ic2_120.content.recipes.compressor.CompressorRecipe(
            recipeId,
            input,
            inputCount,
            net.minecraft.world.item.ItemStack(outputItem, 1),
            if (containerReturn != null) net.minecraft.world.item.ItemStack(containerReturn, 1) else net.minecraft.world.item.ItemStack.EMPTY
        )
        exporter.accept(recipeId, recipe, null)
    }
}
