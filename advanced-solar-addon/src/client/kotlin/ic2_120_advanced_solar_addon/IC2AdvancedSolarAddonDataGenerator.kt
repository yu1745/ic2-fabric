package ic2_120_advanced_solar_addon

import ic2_120_advanced_solar_addon.content.recipes.ModBlockLootTableProvider
import ic2_120_advanced_solar_addon.content.recipes.ModBlockTagProvider
import ic2_120_advanced_solar_addon.content.recipes.ModRecipeProvider
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

object IC2AdvancedSolarAddonDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()

        // 注册合成表生成器（扫描 @RecipeProvider 注解方法）
        pack.addProvider { output: net.fabricmc.fabric.api.datagen.v1.FabricDataOutput ->
            ModRecipeProvider(output)
        }
        // 注册方块掉落表生成器（机器方块需扳手拆才掉完整机器）
        pack.addProvider { output: net.fabricmc.fabric.api.datagen.v1.FabricDataOutput ->
            ModBlockLootTableProvider(output)
        }
        // 注册方块标签生成器（为机器方块添加挖掘标签）
        pack.addProvider { output, registriesFuture ->
            ModBlockTagProvider(output, registriesFuture)
        }
    }
}
