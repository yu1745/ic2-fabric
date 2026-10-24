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
import java.util.concurrent.CompletableFuture
import net.minecraft.core.HolderLookup

class ModBlockLootTableProvider(output: FabricDataOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) : FabricBlockLootTableProvider(output, registriesFuture) {

    private val wrenchPredicateBuilder = ItemPredicate.Builder.item()
        .of(
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "wrench")),
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "electric_wrench"))
        )

    override fun generate() {
        for (block in BuiltInRegistries.BLOCK) {
            val id = BuiltInRegistries.BLOCK.getKey(block)
            if (id.namespace != IC2AdvancedSolarAddon.MOD_ID) continue

            when {
                // 机器方块：需要扳手才能掉落本体，否则掉落 machine
                block is MachineBlock -> {
                    add(block, createMachineLootTable(block))
                }

                // 太阳能板等：直接掉落本体
                else -> {
                    dropSelf(block)
                }
            }
        }
    }

    private fun createMachineLootTable(block: MachineBlock): LootTable.Builder {
        val blockItem = block.asItem()
        val casingItem = block.getCasingDrop()

        val wrenchConditionBuilder = MatchTool.toolMatches(wrenchPredicateBuilder)
        val notWrenchConditionBuilder = InvertedLootItemCondition.invert(wrenchConditionBuilder)

        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1f))
                    .`when`(wrenchConditionBuilder)
                    .add(LootItem.lootTableItem(blockItem).apply(ApplyExplosionDecay.explosionDecay()))
            )
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1f))
                    .`when`(notWrenchConditionBuilder)
                    .add(LootItem.lootTableItem(casingItem).apply(ApplyExplosionDecay.explosionDecay()))
            )
    }
}
