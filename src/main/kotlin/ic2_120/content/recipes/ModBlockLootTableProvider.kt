package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.block.CreativeGeneratorBlock
import ic2_120.content.block.MachineBlock
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider
import net.minecraft.loot.LootPool
import net.minecraft.loot.LootTable
import net.minecraft.loot.condition.InvertedLootCondition
import net.minecraft.loot.condition.MatchToolLootCondition
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.ExplosionDecayLootFunction
import net.minecraft.loot.provider.number.ConstantLootNumberProvider
import net.minecraft.predicate.item.ItemPredicate
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 生成方块掉落表。
 * - [MachineBlock]：扳手/电扳手拆掉完整机器，否则掉外壳（[CreativeGeneratorBlock] 除外：始终掉本体）。
 * - 其余 mod 方块：默认掉落方块物品（与注册物品一致）。
 * - [rubber_leaves]：跳过，使用 `resources` 中自定义掉落（时运/树苗等）。
 * - 储物箱 / 储罐：跳过；其在 [ic2_120.content.block.storage.StorageBoxBlock]、[TankBlock] 的
 *   `onStateReplaced` 中掉落带 `BlockEntityTag` 的物品。若此处再生成标准 `addDrop` loot 表，
 *   `Block.dropStacks` 会先按 loot 掉一个空方块，再与上述逻辑叠加，造成**重复掉落**。
 */
class ModBlockLootTableProvider(output: FabricDataOutput) : FabricBlockLootTableProvider(output) {

    private val wrenchPredicateBuilder = ItemPredicate.Builder.create()
        .items(
            Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "wrench")),
            Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "electric_wrench"))
        )

    /** 不生成 loot JSON：资源里已有，或由方块代码在破坏时单独生成带 BE 数据的掉落物 */
    private val skipGeneratedLootTable = setOf(
        "rubber_leaves",
        "wooden_storage_box",
        "bronze_storage_box",
        "iron_storage_box",
        "steel_storage_box",
        "iridium_storage_box",
        "bronze_tank",
        "iron_tank",
        "steel_tank",
        "iridium_tank",
    )

    override fun generate() {
        val reinforcedDoorId = Identifier(Ic2_120.MOD_ID, "reinforced_door")

        for (block in Registries.BLOCK) {
            val id = Registries.BLOCK.getId(block)
            if (id.namespace != Ic2_120.MOD_ID) continue
            if (id.path in skipGeneratedLootTable) continue

            when {
                block is CreativeGeneratorBlock -> addDrop(block)
                block is MachineBlock -> addDrop(block, createMachineLootTable(block))
                id == reinforcedDoorId -> addDrop(block, doorDrops(block))
                else -> addDrop(block)
            }
        }
    }

    private fun createMachineLootTable(block: MachineBlock): LootTable.Builder {
        val blockItem = block.asItem()
        val casingItem = block.getCasingDrop()

        val wrenchConditionBuilder = MatchToolLootCondition.builder(wrenchPredicateBuilder)
        val notWrenchConditionBuilder = InvertedLootCondition.builder(wrenchConditionBuilder)

        return LootTable.builder()
            .pool(
                LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1f))
                    .conditionally(wrenchConditionBuilder)
                    .with(ItemEntry.builder(blockItem).apply(ExplosionDecayLootFunction.builder()))
            )
            .pool(
                LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1f))
                    .conditionally(notWrenchConditionBuilder)
                    .with(ItemEntry.builder(casingItem).apply(ExplosionDecayLootFunction.builder()))
            )
    }
}
