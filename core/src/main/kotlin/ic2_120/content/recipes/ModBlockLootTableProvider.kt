package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.block.CreativeGeneratorBlock
import ic2_120.content.block.DeepslateLeadOreBlock
import ic2_120.content.block.DeepslateTinOreBlock
import ic2_120.content.block.DeepslateUraniumOreBlock
import ic2_120.content.block.LeadOreBlock
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.RubberLeavesBlock
import ic2_120.content.block.RubberSaplingBlock
import ic2_120.content.block.TinOreBlock
import ic2_120.content.block.UraniumOreBlock
import ic2_120.content.item.RawLead
import ic2_120.content.item.RawTin
import ic2_120.content.item.RawUranium
import ic2_120.registry.ClassScanner
import ic2_120.registry.instance
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.loot.LootPool
import net.minecraft.loot.LootTable
import net.minecraft.loot.condition.AnyOfLootCondition
import net.minecraft.loot.condition.BlockStatePropertyLootCondition
import net.minecraft.loot.condition.InvertedLootCondition
import net.minecraft.loot.condition.MatchToolLootCondition
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.ApplyBonusLootFunction
import net.minecraft.loot.function.ExplosionDecayLootFunction
import net.minecraft.loot.provider.number.ConstantLootNumberProvider
import net.minecraft.predicate.NumberRange
import net.minecraft.predicate.StatePredicate
import net.minecraft.predicate.item.EnchantmentPredicate
import net.minecraft.predicate.item.ItemPredicate
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

/**
 * 生成方块掉落表。
 * - [MachineBlock]：扳手/电扳手拆掉完整机器，否则掉外壳（[CreativeGeneratorBlock] 除外：始终掉本体）。
 * - 其余 mod 方块：默认掉落方块物品（与注册物品一致）。
 * - `@ModBlock(generateBlockLootTable = false)`：由 [ClassScanner] 登记路径后此处跳过。例如：
 *   橡胶叶（`resources` 自定义掉落）、储物箱/储罐（`onStateReplaced` 带 BE 标签）、作物（`onBreak`）等。
 */
class ModBlockLootTableProvider(
    output: FabricDataOutput,
    registryLookup: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricBlockLootTableProvider(output, registryLookup) {

    private val wrenchPredicateBuilder = ItemPredicate.Builder.create()
        .items(
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "wrench")),
            Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "electric_wrench"))
        )

    override fun generate() {
        val reinforcedDoorId = Identifier.of(Ic2_120.MOD_ID, "reinforced_door")

        for (block in Registries.BLOCK) {
            val id = Registries.BLOCK.getId(block)
            if (id.namespace != Ic2_120.MOD_ID) continue
            if (ClassScanner.shouldSkipGeneratedBlockLootTable(id.path)) continue

            when {
                block is CreativeGeneratorBlock -> addDrop(block)
                block is MachineBlock -> addDrop(block, createMachineLootTable(block))
                block is LeadOreBlock -> addCustomOreDrop(block, RawLead::class.instance())
                block is TinOreBlock -> addCustomOreDrop(block, RawTin::class.instance())
                block is UraniumOreBlock -> addCustomOreDrop(block, RawUranium::class.instance())
                block is DeepslateLeadOreBlock -> addCustomOreDrop(block, RawLead::class.instance())
                block is DeepslateTinOreBlock -> addCustomOreDrop(block, RawTin::class.instance())
                block is DeepslateUraniumOreBlock -> addCustomOreDrop(block, RawUranium::class.instance())
                block is RubberLeavesBlock -> addDrop(block, rubberLeavesLootTable(block))
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

    /**
     * IC2 矿石自定义掉落：时运1→1.2倍、时运2→1.4倍、时运3→1.6倍。
     * 使用 BinomialWithBonusCount(0.2, extra=0) 实现：
     * count + Binomial(fortune+0, 0.2) → 时运0=1, 时运1≈1.2, 时运2≈1.4, 时运3≈1.6
     */
    private fun addCustomOreDrop(block: net.minecraft.block.Block, dropItem: net.minecraft.item.Item) {
        val lookup: RegistryWrapper.WrapperLookup = registryLookup
        val enchantmentLookup = lookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
        val fortuneEntry = enchantmentLookup.getOrThrow(Enchantments.FORTUNE)
        addDrop(block, dropsWithSilkTouch(block,
            applyExplosionDecay(dropItem, ItemEntry.builder(dropItem)
                .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortuneEntry, 0.2f, 0))
            )
        ))
    }

    /**
     * 橡胶叶战利品表。树苗掉落必须走战利品表：动力锯砍树冠时只调用
     * `Block.getDrops`（滚战利品表），1.20.1 的 `spawnAfterBreak`/`onStacksDropped`
     * 只给经验值。原版树叶靠战利品表掉树苗所以正常，橡胶叶此前依赖
     * `onStacksDropped` 代码钩子，被锯子完全绕过 → 整树砍完一颗树苗都不掉。
     *
     * 结构对齐 vanilla `BlockLootTableGenerator` 的树叶表：
     * - 剪刀/精准采集 → 掉树叶块本身；
     * - 保底绑定叶（[RubberLeavesBlock.RUBBER_SAPLING_DROP]）→ 必掉 1 个树苗（世界生成/树苗生长时由
     *   [ic2_120.content.block.RubberTreeSaplingDrop.bindLeaves] 写到方块状态上，运行时可判定）。
     * 不保留任何随机 fallback——每棵保底树掉落树苗数精确等于配置期望 n（默认 1.25）。
     */
    private fun rubberLeavesLootTable(block: RubberLeavesBlock): LootTable.Builder {
        val leafItem = block.asItem()
        val saplingItem = RubberSaplingBlock::class.instance()

        val silkTouchOrShears = AnyOfLootCondition.builder(
            MatchToolLootCondition.builder(ItemPredicate.Builder.create().items(Items.SHEARS)),
            MatchToolLootCondition.builder(
                ItemPredicate.Builder.create()
                    .enchantment(EnchantmentPredicate(Enchantments.SILK_TOUCH, NumberRange.IntRange.atLeast(1)))
            )
        )
        val boundLeaf = BlockStatePropertyLootCondition.builder(block)
            .properties(StatePredicate.Builder.create().exactMatch(RubberLeavesBlock.RUBBER_SAPLING_DROP, true))
        val notSilkTouchOrShears = InvertedLootCondition.builder(silkTouchOrShears)

        return LootTable.builder()
            // 剪刀/精准采集 → 掉树叶本身
            .pool(
                LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1f))
                    .conditionally(silkTouchOrShears)
                    .with(addSurvivesExplosionCondition(block, ItemEntry.builder(leafItem)))
            )
            // 保底绑定叶 → 必掉 1 个树苗（世界生成/树苗生长时由 bindLeaves 写入方块状态，运行时可判定）
            .pool(
                LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1f))
                    .conditionally(notSilkTouchOrShears)
                    .conditionally(boundLeaf)
                    .with(ItemEntry.builder(saplingItem))
            )
    }
}
