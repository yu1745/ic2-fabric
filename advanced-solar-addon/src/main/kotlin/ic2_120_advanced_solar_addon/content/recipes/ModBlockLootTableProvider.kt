package ic2_120_advanced_solar_addon.content.recipes

import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
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

class ModBlockLootTableProvider(output: FabricDataOutput) : FabricBlockLootTableProvider(output) {

    private val wrenchPredicateBuilder = ItemPredicate.Builder.create()
        .items(
            Registries.ITEM.get(Identifier("ic2_120", "wrench")),
            Registries.ITEM.get(Identifier("ic2_120", "electric_wrench"))
        )

    override fun generate() {
        for (block in Registries.BLOCK) {
            val id = Registries.BLOCK.getId(block)
            if (id.namespace != IC2AdvancedSolarAddon.MOD_ID) continue

            when {
                // 机器方块：需要扳手才能掉落本体，否则掉落 machine
                block is MachineBlock -> {
                    addDrop(block, createMachineLootTable(block))
                }

                // 太阳能板等：直接掉落本体
                else -> {
                    addDrop(block)
                }
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
