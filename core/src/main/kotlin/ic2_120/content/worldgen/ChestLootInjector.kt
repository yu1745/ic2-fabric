package ic2_120.content.worldgen

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.loot.LootPool
import net.minecraft.loot.entry.EmptyEntry
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.SetCountLootFunction
import net.minecraft.loot.provider.number.UniformLootNumberProvider
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 以代码方式向原版奖励箱追加 IC2 战利品，避免整表覆盖并降低与其他模组的冲突概率。
 */
object ChestLootInjector {
    /** 工业风奖励箱 — 废弃矿井、下界要塞 */
    private val industrialChests = setOf(
        chest("abandoned_mineshaft"),
        chest("nether_bridge")
    )

    /** 冒险风奖励箱 — 沙漠神殿、末地城、雪屋、丛林神庙 */
    private val adventureChests = setOf(
        chest("desert_pyramid"),
        chest("end_city_treasure"),
        chest("igloo_chest"),
        chest("jungle_temple")
    )

    /** 远古城市 — 深暗之域，高级内容 */
    private val ancientCityChests = setOf(
        chest("ancient_city"),
        chest("ancient_city_ice_box")
    )

    /** 堡垒遗迹 — 下界高级建筑 */
    private val bastionChests = setOf(
        chest("bastion_bridge"),
        chest("bastion_hoglin_stable"),
        chest("bastion_other"),
        chest("bastion_treasure")
    )

    /** 船只残骸 */
    private val shipwreckChests = setOf(
        chest("shipwreck_map"),
        chest("shipwreck_supply"),
        chest("shipwreck_treasure")
    )

    /** 水下废墟 */
    private val underwaterRuinChests = setOf(
        chest("underwater_ruin_big"),
        chest("underwater_ruin_small")
    )

    /** 要塞标准 — 十字路口、图书馆 */
    private val strongholdStandardChests = setOf(
        chest("stronghold_crossing"),
        chest("stronghold_library")
    )

    /** 村庄铁匠类 — 盔甲匠、工具匠、武器匠 */
    private val villageBlacksmithEquivalents = setOf(
        chest("village/village_armorer"),
        chest("village/village_toolsmith"),
        chest("village/village_weaponsmith")
    )

    /** 村庄房屋类 — 各生物群系房屋 */
    private val villageHouseChests = setOf(
        chest("village/village_desert_house"),
        chest("village/village_plains_house"),
        chest("village/village_savanna_house"),
        chest("village/village_snowy_house"),
        chest("village/village_taiga_house")
    )

    /** 村庄职业类 — 屠夫、制图师、渔夫、制箭师、石匠、牧羊人、皮匠、神职人员 */
    private val villageWorkerChests = setOf(
        chest("village/village_butcher"),
        chest("village/village_cartographer"),
        chest("village/village_fisher"),
        chest("village/village_fletcher"),
        chest("village/village_mason"),
        chest("village/village_shepherd"),
        chest("village/village_tannery"),
        chest("village/village_temple")
    )

    /** 其他独立奖励箱 */
    private val miscellaneousChests = setOf(
        chest("buried_treasure"),
        chest("pillager_outpost"),
        chest("ruined_portal"),
        chest("woodland_mansion")
    )

    /** 试炼密室基础 — 入口、走廊、交叉口 barrels */
    private val trialBasicChests = setOf(
        chest("trial_chambers/entrance"),
        chest("trial_chambers/corridor"),
        chest("trial_chambers/intersection_barrel")
    )

    /** 试炼密室补给箱 */
    private val trialSupplyChests = setOf(
        chest("trial_chambers/supply")
    )

    /** 试炼密室交叉口（高级） */
    private val trialAdvancedChests = setOf(
        chest("trial_chambers/intersection")
    )

    /** 试炼密室宝库（普通） */
    private val trialRewardChests = setOf(
        chest("trial_chambers/reward"),
        chest("trial_chambers/reward_common"),
        chest("trial_chambers/reward_rare"),
        chest("trial_chambers/reward_unique")
    )

    /** 试炼密室宝库（不祥） */
    private val trialOminousRewardChests = setOf(
        chest("trial_chambers/reward_ominous"),
        chest("trial_chambers/reward_ominous_common"),
        chest("trial_chambers/reward_ominous_rare"),
        chest("trial_chambers/reward_ominous_unique")
    )

    /** 所有需要注入的奖励箱 ID 的并集，用于快速判断 */
    private val allChests = industrialChests + adventureChests + ancientCityChests +
        bastionChests + shipwreckChests + underwaterRuinChests + strongholdStandardChests +
        villageBlacksmithEquivalents + villageHouseChests + villageWorkerChests +
        miscellaneousChests + trialBasicChests + trialSupplyChests + trialAdvancedChests +
        trialRewardChests + trialOminousRewardChests + setOf(
            chest("simple_dungeon"),
            chest("spawn_bonus_chest"),
            chest("stronghold_corridor"),
        )

    fun register() {
        LootTableEvents.MODIFY.register { key, tableBuilder, source, lookup ->
            val lootTableId = key.value
            if (lootTableId !in allChests) return@register

            when (lootTableId) {
                in industrialChests -> tableBuilder.pool(createIndustrialPool())
                in adventureChests -> tableBuilder.pool(createAdventurePool())
                in ancientCityChests -> tableBuilder.pool(createAncientCityPool())
                in bastionChests -> tableBuilder.pool(createBastionPool())
                in shipwreckChests -> tableBuilder.pool(createShipwreckPool())
                in underwaterRuinChests -> tableBuilder.pool(createUnderwaterRuinPool())
                chest("simple_dungeon") -> tableBuilder.pool(createDungeonPool())
                chest("spawn_bonus_chest") -> tableBuilder.pool(createSpawnBonusPool())
                chest("stronghold_corridor") -> tableBuilder.pool(createStrongholdCorridorPool())
                in strongholdStandardChests -> tableBuilder.pool(createStrongholdStandardPool())
                in villageBlacksmithEquivalents -> tableBuilder.pool(createVillageBlacksmithPool())
                in villageHouseChests -> tableBuilder.pool(createVillageHousePool())
                in villageWorkerChests -> tableBuilder.pool(createVillageWorkerPool())
                in miscellaneousChests -> tableBuilder.pool(createAdventurePool())
                in trialBasicChests -> tableBuilder.pool(createTrialBasicPool())
                in trialSupplyChests -> tableBuilder.pool(createTrialSupplyPool())
                in trialAdvancedChests -> tableBuilder.pool(createTrialAdvancedPool())
                in trialRewardChests -> tableBuilder.pool(createTrialRewardPool())
                in trialOminousRewardChests -> tableBuilder.pool(createTrialOminousPool())
            }
        }
    }

    private fun createIndustrialPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_shard"), 8, 2f, 5f)
            .withWeightedItem(modItem("bronze_pickaxe"), 1)
            .withWeightedItem(modItem("filled_tin_can"), 8, 4f, 16f)
            .withEmpty(34)

    private fun createAdventurePool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_shard"), 8, 1f, 2f)
            .withWeightedItem(modItem("bronze_pickaxe"), 3)
            .withWeightedItem(modItem("bronze_sword"), 3)
            .withWeightedItem(modItem("bronze_helmet"), 3)
            .withWeightedItem(modItem("bronze_chestplate"), 3)
            .withWeightedItem(modItem("bronze_leggings"), 3)
            .withWeightedItem(modItem("bronze_boots"), 3)
            .withEmpty(43)

    private fun createDungeonPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 10, 2f, 5f)
            .withWeightedItem(modItem("tin_ingot"), 10, 2f, 5f)
            .withWeightedItem(modItem("iridium_ore_item"), 2, 1f, 2f)
            .withWeightedItem(modItem("iridium_shard"), 10, 6f, 14f)
            .withEmpty(32)

    private fun createSpawnBonusPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 1f))
            .withWeightedItem(modItem("treetap"), 8)
            .withEmpty(2)

    private fun createStrongholdCorridorPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_ore_item"), 1, 1f, 4f)
            .withWeightedItem(modItem("iridium_shard"), 8, 4f, 14f)
            .withWeightedItem(modItem("bronze_pickaxe"), 3)
            .withWeightedItem(modItem("bronze_sword"), 3)
            .withWeightedItem(modItem("bronze_helmet"), 3)
            .withWeightedItem(modItem("bronze_chestplate"), 3)
            .withWeightedItem(modItem("bronze_leggings"), 3)
            .withWeightedItem(modItem("bronze_boots"), 3)
            .withEmpty(44)

    private fun createStrongholdStandardPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_ore_item"), 1)
            .withWeightedItem(modItem("iridium_shard"), 8, 2f, 5f)
            .withWeightedItem(modItem("bronze_pickaxe"), 3)
            .withWeightedItem(modItem("bronze_sword"), 3)
            .withWeightedItem(modItem("bronze_helmet"), 3)
            .withWeightedItem(modItem("bronze_chestplate"), 3)
            .withWeightedItem(modItem("bronze_leggings"), 3)
            .withWeightedItem(modItem("bronze_boots"), 3)
            .withEmpty(44)

    private fun createVillageBlacksmithPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 5f)
            .withWeightedItem(modItem("iridium_shard"), 8, 3f, 7f)
            .withWeightedItem(modItem("bronze_pickaxe"), 3)
            .withWeightedItem(modItem("bronze_sword"), 3)
            .withWeightedItem(modItem("bronze_helmet"), 3)
            .withWeightedItem(modItem("bronze_chestplate"), 3)
            .withWeightedItem(modItem("bronze_leggings"), 3)
            .withWeightedItem(modItem("bronze_boots"), 3)
            .withWeightedItem(modItem("bronze_ingot"), 5, 2f, 4f)
            .withWeightedItem(modItem("rubber_sapling"), 4, 1f, 4f)
            .withEmpty(52)

    private fun createAncientCityPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 8, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 7, 1f, 5f)
            .withWeightedItem(modItem("iridium_ore_item"), 3, 1f, 3f)
            .withWeightedItem(modItem("iridium_shard"), 10, 4f, 12f)
            .withWeightedItem(modItem("energy_crystal"), 2)
            .withEmpty(30)

    private fun createBastionPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(Items.COPPER_INGOT, 8, 2f, 6f)
            .withWeightedItem(modItem("tin_ingot"), 7, 1f, 5f)
            .withWeightedItem(modItem("iridium_shard"), 8, 2f, 8f)
            .withWeightedItem(modItem("bronze_pickaxe"), 2)
            .withWeightedItem(modItem("bronze_sword"), 2)
            .withWeightedItem(modItem("filled_tin_can"), 6, 4f, 12f)
            .withEmpty(35)

    private fun createShipwreckPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 2f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 1f, 4f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 3f)
            .withWeightedItem(modItem("iridium_shard"), 6, 1f, 4f)
            .withWeightedItem(modItem("bronze_ingot"), 5, 1f, 3f)
            .withEmpty(45)

    private fun createUnderwaterRuinPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 1f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 1f, 3f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 2f)
            .withWeightedItem(modItem("iridium_shard"), 5, 1f, 3f)
            .withEmpty(50)

    private fun createVillageHousePool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 1f))
            .withWeightedItem(Items.COPPER_INGOT, 8, 1f, 3f)
            .withWeightedItem(modItem("tin_ingot"), 7, 1f, 2f)
            .withWeightedItem(modItem("iridium_shard"), 4, 1f, 3f)
            .withWeightedItem(modItem("rubber_sapling"), 3, 1f, 2f)
            .withEmpty(55)

    private fun createVillageWorkerPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 2f))
            .withWeightedItem(Items.COPPER_INGOT, 9, 1f, 4f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 3f)
            .withWeightedItem(modItem("iridium_shard"), 6, 1f, 4f)
            .withWeightedItem(modItem("bronze_ingot"), 4, 1f, 3f)
            .withWeightedItem(modItem("rubber_sapling"), 3, 1f, 2f)
            .withEmpty(45)

    private fun createTrialBasicPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 2f))
            .withWeightedItem(modItem("treetap"), 6)
            .withWeightedItem(modItem("rubber_sapling"), 6, 1f, 2f)
            .withWeightedItem(Items.COPPER_INGOT, 8, 1f, 3f)
            .withWeightedItem(modItem("tin_ingot"), 8, 1f, 2f)
            .withEmpty(40)

    private fun createTrialSupplyPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(modItem("filled_tin_can"), 12, 1f, 3f)
            .withWeightedItem(Items.COPPER_INGOT, 8, 2f, 4f)
            .withWeightedItem(modItem("tin_ingot"), 6, 1f, 3f)
            .withWeightedItem(modItem("rubber_sapling"), 8, 1f, 2f)
            .withEmpty(35)

    private fun createTrialAdvancedPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 3f))
            .withWeightedItem(modItem("bronze_ingot"), 10, 2f, 4f)
            .withWeightedItem(modItem("iridium_shard"), 8, 1f, 3f)
            .withWeightedItem(modItem("bronze_pickaxe"), 5)
            .withWeightedItem(modItem("bronze_sword"), 4)
            .withWeightedItem(modItem("circuit"), 4)
            .withWeightedItem(modItem("energy_crystal"), 2)
            .withEmpty(30)

    private fun createTrialRewardPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 2f))
            .withWeightedItem(modItem("advanced_circuit"), 8)
            .withWeightedItem(modItem("energy_crystal"), 6)
            .withWeightedItem(modItem("carbon_plate"), 5)
            .withWeightedItem(modItem("iridium_shard"), 8, 2f, 5f)
            .withWeightedItem(modItem("diamond_drill"), 3)
            .withWeightedItem(modItem("chainsaw"), 2)
            .withWeightedItem(modItem("carbon_rotor"), 2)
            .withEmpty(25)

    private fun createTrialOminousPool(): LootPool.Builder =
        LootPool.builder()
            .rolls(UniformLootNumberProvider.create(1f, 2f))
            .withWeightedItem(modItem("lapotron_crystal"), 6)
            .withWeightedItem(modItem("iridium"), 5, 1f, 3f)
            .withWeightedItem(modItem("iridium_drill"), 2)
            .withWeightedItem(modItem("nano_saber"), 2)
            .withWeightedItem(modItem("mining_laser"), 2)
            .withWeightedItem(modItem("iridium_shard"), 8, 4f, 8f)
            .withEmpty(20)

    private fun LootPool.Builder.withWeightedItem(item: Item, weight: Int, min: Float? = null, max: Float? = null): LootPool.Builder {
        val entry = ItemEntry.builder(item).weight(weight)
        if (min != null && max != null) {
            entry.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)))
        }
        return with(entry)
    }

    private fun LootPool.Builder.withEmpty(weight: Int): LootPool.Builder =
        with(EmptyEntry.builder().weight(weight))

    private fun modItem(path: String): Item = Registries.ITEM.get(Ic2_120.id(path))

    private fun chest(path: String): Identifier = Identifier.of("minecraft", "chests/$path")
}
