package ic2_120_industrial_upgrade

import ic2_120_industrial_upgrade.content.recipes.ModBlockLootTableProvider
import ic2_120_industrial_upgrade.content.recipes.ModBlockTagProvider
import ic2_120_industrial_upgrade.content.recipes.ModRecipeProvider
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput

object IC2IndustrialUpgradeDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()

        // 注册合成表生成器（扫描 @RecipeProvider 注解方法 + 压缩机配方）
        pack.addProvider { output: FabricPackOutput, registriesFuture ->
            ModRecipeProvider(output, registriesFuture)
        }
        // 注册方块掉落表生成器（机器方块需扳手拆才掉完整机器）
        pack.addProvider { output: FabricPackOutput, registriesFuture ->
            ModBlockLootTableProvider(output, registriesFuture)
        }
        // 注册方块标签生成器（为机器方块添加挖掘标签）
        pack.addProvider { output, registriesFuture ->
            ModBlockTagProvider(output, registriesFuture)
        }
    }
}
