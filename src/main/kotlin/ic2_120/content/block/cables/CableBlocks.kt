package ic2_120.content.block.cables

import ic2_120.content.item.energy.ITiered
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock

/**
 * 锡质导线。低压传输，32 EU/t，损耗 0.2 EU/格。
 */
@ModBlock(name = "tin_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class TinCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    override val tier: Int = 1
    override fun getTransferRate(): Long = 32L
    override fun getEnergyLoss(): Long = 200L
}

/**
 * 铜质导线。中低压传输，128 EU/t，损耗 0.2 EU/格。
 */
@ModBlock(name = "copper_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class CopperCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    override val tier: Int = 2
    override fun getTransferRate(): Long = 128L
    override fun getEnergyLoss(): Long = 200L
}

/**
 * 金质导线。中压传输，512 EU/t，损耗 0.4 EU/格。比正常导线细。
 */
@ModBlock(name = "gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class GoldCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    override val tier: Int = 3
    override fun getTransferRate(): Long = 512L
    override fun getEnergyLoss(): Long = 400L

    override fun getCableMin(): Double = 6.5 / 16.0
    override fun getCableMax(): Double = 9.5 / 16.0
}

/**
 * 高压导线（铁质）。2048 EU/t，损耗 0.8 EU/格。碰撞箱较默认导线更粗。
 */
@ModBlock(name = "iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class IronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    override val tier: Int = 4
    override fun getTransferRate(): Long = 2048L
    override fun getEnergyLoss(): Long = 800L

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0
}

/**
 * 玻璃纤维导线。超高压传输，8192 EU/t，损耗仅 0.025 EU/格。最高绝缘，绝不漏电。
 */
@ModBlock(name = "glass_fibre_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables", transparent = true)
class GlassFibreCableBlock(settings: AbstractBlock.Settings = defaultSettings().nonOpaque()) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    override val tier: Int = 5
    override fun getTransferRate(): Long = 8192L
    override fun getEnergyLoss(): Long = 25L
    override val insulationLevel: Int = 5
}

// ── 绝缘导线（损耗与裸线相同，传输率与对应裸线相同，仅防止触电） ────────────────────────

/**
 * 绝缘铜质导线。128 EU/t，损耗 0.2 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_copper_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedCopperCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    override val tier: Int = 2
    override fun getTransferRate(): Long = 128L
    override fun getEnergyLoss(): Long = 200L
    override val insulationLevel: Int = 2
}

/**
 * 绝缘锡质导线。32 EU/t，损耗 0.2 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_tin_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedTinCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    override val tier: Int = 1
    override fun getTransferRate(): Long = 32L
    override fun getEnergyLoss(): Long = 200L
    override val insulationLevel: Int = 2
}

/**
 * 绝缘金质导线。512 EU/t，损耗 0.4 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedGoldCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    override val tier: Int = 3
    override fun getTransferRate(): Long = 512L
    override fun getEnergyLoss(): Long = 400L
    override val insulationLevel: Int = 2

    override fun getCableMin(): Double = 6.5 / 16.0
    override fun getCableMax(): Double = 9.5 / 16.0
}

/**
 * 2x绝缘金质导线。512 EU/t，损耗 0.4 EU/格。2 倍绝缘（≤512）。
 */
@ModBlock(name = "double_insulated_gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class DoubleInsulatedGoldCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    override val tier: Int = 3
    override fun getTransferRate(): Long = 512L
    override fun getEnergyLoss(): Long = 400L
    override val insulationLevel: Int = 3

    override fun getCableMin(): Double = 6.5 / 16.0
    override fun getCableMax(): Double = 9.5 / 16.0
}

/**
 * 绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedIronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    override val tier: Int = 4
    override fun getTransferRate(): Long = 2048L
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 2

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0
}

/**
 * 2x绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。2 倍绝缘（≤512）。
 */
@ModBlock(name = "double_insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class DoubleInsulatedIronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    override val tier: Int = 4
    override fun getTransferRate(): Long = 2048L
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 3

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0
}

/**
 * 3x绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。3 倍绝缘（≤2048）。
 */
@ModBlock(name = "triple_insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class TripleInsulatedIronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    override val tier: Int = 4
    override fun getTransferRate(): Long = 2048L
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 4

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0
}
