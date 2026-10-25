package ic2_120.content.recipes

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import net.minecraft.core.registries.Registries

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
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition
import net.minecraft.world.level.storage.loot.predicates.MatchTool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.advancements.predicates.MinMaxBounds
import net.minecraft.advancements.predicates.StatePropertiesPredicate
import net.minecraft.advancements.predicates.EnchantmentPredicate
import net.minecraft.advancements.predicates.ItemPredicate
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import java.util.concurrent.CompletableFuture

/**
 * 生成方块掉落表。
 * - [MachineBlock]：扳手/电扳手拆掉完整机器，否则掉外壳（[CreativeGeneratorBlock] 除外：始终掉本体）。
 * - 其余 mod 方块：默认掉落方块物品（与注册物品一致）。
 * - `@ModBlock(generateBlockLootTable = false)`：由 [ClassScanner] 登记路径后此处跳过。例如：
 *   橡胶叶（`resources` 自定义掉落）、储物箱/储罐（`onStateReplaced` 带 BE 标签）、作物（`onBreak`）等。
 */
class ModBlockLootTableProvider(
    output: FabricPackOutput,
    registryLookup: CompletableFuture<HolderLookup.Provider>
) : FabricBlockLootSubProvider(output, registryLookup) {

    private val wrenchPredicateBuilder by lazy { ItemPredicate.Builder.item()
        .of(
            registries.lookupOrThrow(Registries.ITEM),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "wrench")),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "electric_wrench"))
        ) }

    override fun generate() {
        val reinforcedDoorId = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "reinforced_door")

        for (block in BuiltInRegistries.BLOCK) {
            val id = BuiltInRegistries.BLOCK.getKey(block)
            if (id.namespace != Ic2_120.MOD_ID) continue
            if (block.lootTable.isEmpty) continue
            if (ClassScanner.shouldSkipGeneratedBlockLootTable(id.path)) continue

            when {
                block is CreativeGeneratorBlock -> dropRegisteredBlock(block)
                block is MachineBlock -> add(block, createMachineLootTable(block))
                block is LeadOreBlock -> addCustomOreDrop(block, RawLead::class.instance())
                block is TinOreBlock -> addCustomOreDrop(block, RawTin::class.instance())
                block is UraniumOreBlock -> addCustomOreDrop(block, RawUranium::class.instance())
                block is DeepslateLeadOreBlock -> addCustomOreDrop(block, RawLead::class.instance())
                block is DeepslateTinOreBlock -> addCustomOreDrop(block, RawTin::class.instance())
                block is DeepslateUraniumOreBlock -> addCustomOreDrop(block, RawUranium::class.instance())
                block is RubberLeavesBlock -> add(block, rubberLeavesLootTable(block))
                id == reinforcedDoorId -> add(block, createDoorTable(block))
                else -> dropRegisteredBlock(block)
            }
        }
    }

    private fun createMachineLootTable(block: MachineBlock): LootTable.Builder {
        val blockItem = registeredItem(block)
        val casingItem = block.getCasingDrop().takeUnless { it == Items.AIR } ?: registeredItem(block)

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

    private fun registeredItem(block: net.minecraft.world.level.block.Block): net.minecraft.world.item.Item {
        val id = BuiltInRegistries.BLOCK.getKey(block)
        val item = BuiltInRegistries.ITEM.getValue(id)
        check(item != Items.AIR) { "方块 $id 没有同名物品，无法生成掉落表" }
        return item
    }

    private fun dropRegisteredBlock(block: net.minecraft.world.level.block.Block) {
        add(block, createSingleItemTable(registeredItem(block)))
    }

    /**
     * IC2 矿石自定义掉落：时运1→1.2倍、时运2→1.4倍、时运3→1.6倍。
     * 使用 BinomialWithBonusCount(0.2, extra=0) 实现：
     * count + Binomial(fortune+0, 0.2) → 时运0=1, 时运1≈1.2, 时运2≈1.4, 时运3≈1.6
     */
    private fun addCustomOreDrop(block: net.minecraft.world.level.block.Block, dropItem: net.minecraft.world.item.Item) {
        val enchantmentLookup = registries.lookupOrThrow(Registries.ENCHANTMENT)
        val fortuneEntry = enchantmentLookup.getOrThrow(Enchantments.FORTUNE)
        add(block, createSilkTouchDispatchTable(block,
            applyExplosionDecay(dropItem, LootItem.lootTableItem(dropItem)
                .apply(ApplyBonusCount.addBonusBinomialDistributionCount(fortuneEntry, 0.2f, 0))
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
        val leafItem = registeredItem(block)
        val saplingItem = RubberSaplingBlock::class.instance()

        val silkTouchOrShears = AnyOfCondition.anyOf(
            MatchTool.toolMatches(ItemPredicate.Builder.item().of(registries.lookupOrThrow(Registries.ITEM), Items.SHEARS)),
            hasSilkTouch()
        )
        val boundLeaf = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(RubberLeavesBlock.RUBBER_SAPLING_DROP, true))
        val notSilkTouchOrShears = InvertedLootItemCondition.invert(silkTouchOrShears)

        return LootTable.lootTable()
            // 剪刀/精准采集 → 掉树叶本身
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1f))
                    .`when`(silkTouchOrShears)
                    .add(applyExplosionCondition(block, LootItem.lootTableItem(leafItem)))
            )
            // 保底绑定叶 → 必掉 1 个树苗（世界生成/树苗生长时由 bindLeaves 写入方块状态，运行时可判定）
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1f))
                    .`when`(notSilkTouchOrShears)
                    .`when`(boundLeaf)
                    .add(LootItem.lootTableItem(saplingItem))
            )
    }
}
