package ic2_120_advanced_solar_addon.content.recipes

import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import ic2_120.content.block.MachineBlock
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition
import net.minecraft.world.level.storage.loot.predicates.MatchTool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

class ModBlockLootTableProvider(output: FabricDataOutput) : FabricBlockLootTableProvider(output) {

    private val wrenchPredicateBuilder = ItemPredicate.Builder.create()
        .items(
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "wrench")),
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "electric_wrench"))
        )

    override fun generate() {
        for (block in BuiltInRegistries.BLOCK) {
            val id = BuiltInRegistries.BLOCK.getId(block)
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

        val wrenchConditionBuilder = MatchTool.builder(wrenchPredicateBuilder)
        val notWrenchConditionBuilder = InvertedLootItemCondition.builder(wrenchConditionBuilder)

        return LootTable.builder()
            .pool(
                LootPool.builder()
                    .rolls(ConstantValue.create(1f))
                    .conditionally(wrenchConditionBuilder)
                    .setValue(LootItem.builder(blockItem).apply(ApplyExplosionDecay.builder()))
            )
            .pool(
                LootPool.builder()
                    .rolls(ConstantValue.create(1f))
                    .conditionally(notWrenchConditionBuilder)
                    .setValue(LootItem.builder(casingItem).apply(ApplyExplosionDecay.builder()))
            )
    }
}
