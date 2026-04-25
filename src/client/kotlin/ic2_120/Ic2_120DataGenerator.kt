package ic2_120

import ic2_120.content.entity.ModEntities
import ic2_120.content.fluid.ModFluids
import ic2_120.content.recipes.MaterialTagRegistry
import ic2_120.content.recipes.ModBlockLootTableProvider
import ic2_120.content.recipes.ModBlockTagProvider
import ic2_120.content.recipes.ModItemTagProvider
import ic2_120.content.recipes.ModRecipeProvider
import ic2_120.registry.ClassScanner
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object Ic2_120DataGenerator : DataGeneratorEntrypoint {
	override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
		val skipGeneration = System.getProperty("skip.ic2_120.datagen")?.toBoolean() == true

		// datagen 可能在 ModInitializer 之前执行；若注解矿辞表为空且物品未注册，则先完成扫描注册。
		// 即使附属 mod 设置了 skip，注册扫描仍会执行（附属 datagen 可能依赖本体的注册信息）。
		if (MaterialTagRegistry.itemEntries.isEmpty()) {
			val tinId = Identifier.of(Ic2_120.MOD_ID, "tin_ingot")
			if (!Registries.ITEM.containsId(tinId)) {
				ModFluids.register()
				ModEntities.register()
				ClassScanner.scanAndRegister(
					Ic2_120.MOD_ID,
					listOf(
						"ic2_120.content.tab",
						"ic2_120.content.block",
						"ic2_120.content.screen",
						"ic2_120.content.item",
					),
				)
			}
		}

		// 附属 mod 可通过 JVM 参数 -Dskip.ic2_120.datagen=true 跳过本体的 JSON 生成
		if (skipGeneration) {
			return
		}

		val pack = fabricDataGenerator.createPack()

		// 注册配方数据生成器
		pack.addProvider { output: net.fabricmc.fabric.api.datagen.v1.FabricDataOutput ->
			ModRecipeProvider(output)
		}
		// 注册方块掉落表生成器（机器方块需扳手拆才掉完整机器）
		pack.addProvider { output: net.fabricmc.fabric.api.datagen.v1.FabricDataOutput ->
			ModBlockLootTableProvider(output)
		}
		// 注册方块标签生成器（机器方块加入 mineable/pickaxe、needs_iron_tool，使铁镐能正常挖掘）
		pack.addProvider { output, registriesFuture ->
			ModBlockTagProvider(output, registriesFuture)
		}
		// 物品标签（注解 materialTags → c:/forge:/compat:）
		pack.addProvider { output, registriesFuture ->
			ModItemTagProvider(output, registriesFuture)
		}
	}
}
