package ic2_120.content.recipes

import ic2_120.Ic2_120
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
 * 机器方块：扳手/电扳手拆掉完整机器，否则掉外壳。
 */
class ModBlockLootTableProvider(output: FabricDataOutput) : FabricBlockLootTableProvider(output) {

    private val wrenchPredicateBuilder = ItemPredicate.Builder.create()
        .items(
            Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "wrench")),
            Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "electric_wrench"))
        )

    override fun generate() {
        // 机器外壳：直接掉落自身
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "machine")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "advanced_machine")))

        // 变压器（仅合成材料，无 BlockEntity）：直接掉落自身
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "lv_transformer")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "mv_transformer")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "hv_transformer")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "ev_transformer")))

        // 日光灯：直接掉落自身（无物品槽，无外壳）
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "luminator_flat")))
        // 流体管道：直接掉落自身
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "bronze_pipe_tiny")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "bronze_pipe_small")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "bronze_pipe_medium")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "bronze_pipe_large")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "carbon_pipe_tiny")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "carbon_pipe_small")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "carbon_pipe_medium")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "carbon_pipe_large")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "bronze_pump_attachment")))
        addDrop(Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "carbon_pump_attachment")))

        // 机器方块：条件掉落
        for (block in Registries.BLOCK) {
            val id = Registries.BLOCK.getId(block)
            if (id.namespace == Ic2_120.MOD_ID && block is MachineBlock) {
                addDrop(block, createMachineLootTable(block))
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

