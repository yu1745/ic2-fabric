package ic2_120_industrial_upgrade.content.block

import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.cables.GlassFibreCableBlock
import ic2_120.content.item.Alloy
import ic2_120.content.item.BronzeIngot
import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.DenseBronzePlate
import ic2_120.content.item.DenseCopperPlate
import ic2_120.content.item.DenseGoldPlate
import ic2_120.content.item.DenseIronPlate
import ic2_120.content.item.DenseLapisPlate
import ic2_120.content.item.DenseLeadPlate
import ic2_120.content.item.DenseObsidianPlate
import ic2_120.content.item.DenseSteelPlate
import ic2_120.content.item.DenseTinPlate
import ic2_120.content.item.MixedMetalIngot
import ic2_120.content.item.RubberItem
import ic2_120.content.item.SilverIngot
import ic2_120.content.item.SteelIngot
import ic2_120.content.item.energy.ITiered
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.minecraft.data.recipes.RecipeProvider.getHasName
import net.minecraft.data.recipes.RecipeProvider.has
import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.BaseEntityBlock

// ── 工业升级高压玻璃 EU 导线（11 档）────────────────────────────────────────────
//
// 数值规则（与 core 衔接）：
//   - 基准：core 玻璃纤维导线 = tier 5 = 8192 EU/t
//   - 首档（光谱）= 玻璃纤维 × 4 = tier 6 = 32768 EU/t，之后每档 ×4
//   - tier = 6..16，capacity 由 EnergyTier.euPerTickFromTier(tier) 自动给出（32×4^(tier-1)）
//   - 损耗 loss 固定 0（无损传输），getEnergyLoss 返回 0L（单位毫 EU）
//
// 注册方式：本类不走 @ModBlock（附属 main entrypoint 晚于 core，无法被 core 的 CableBlockEntity
// 统一注册捕获）。改由 IUCableProvider 通过 core 的 "ic2_120:cables" entrypoint 调用
// ClassScanner.registerCableBlock 在 core.onInitialize 期间注册，使 core 能把它们合并进
// CableBlockEntity 的统一 BlockEntityType。
//
// 合成配方（级联，全部用 core 已有材料）保留 @RecipeProvider，由 datagen 在构建期生成。
//
// 命名等级链（已确认，与上游 CableType glass~glass10 对应）：玻璃纤维(core) < 光谱 < 质子 < 奇异 < 神话 < 量子 < 光子 < 中子 < 完美 < 寰宇 < 街射 < 无限
// 贴图对应（上游 item 贴图按 BlockCableEntity.name 的 itemMeta 顺序）：
// 光谱=glass/itemcable、质子=glass1/itemcableo、奇异=glass2/itemgoldcable、神话=glass3/itemgoldcablei、
// 量子=glass4/itemgoldcableii、光子=glass5/itemironcable、中子=glass6/itemironcablei、
// 完美=glass7/itemironcableii、寰宇=glass8/itemironcableiiii、街射=glass9/itemglasscable、无限=glass10/itemglasscablei

/** 光谱导线（tier 6，32768 EU/t，无损）。 */
class SpectralCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> SpectralCableBlock() }
    override val tier: Int = 6
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 质子导线（tier 7，131072 EU/t，无损）。 */
class ProtonCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> ProtonCableBlock() }
    override val tier: Int = 7
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 奇异导线（tier 8，524288 EU/t，无损）。 */
class SingularCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> SingularCableBlock() }
    override val tier: Int = 8
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 神话导线（tier 9，2097152 EU/t，无损）。 */
class MythicalCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> MythicalCableBlock() }
    override val tier: Int = 9
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 量子导线（tier 10，8388608 EU/t，无损）。 */
class QuantumCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> QuantumCableBlock() }
    override val tier: Int = 10
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 光子导线（tier 11，33554432 EU/t，无损）。 */
class PhotonicCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> PhotonicCableBlock() }
    override val tier: Int = 11
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 中子导线（tier 12，134217728 EU/t，无损）。 */
class NeutronCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> NeutronCableBlock() }
    override val tier: Int = 12
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 完美导线（tier 13，536870912 EU/t，无损）。 */
class PerfectCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> PerfectCableBlock() }
    override val tier: Int = 13
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 寰宇导线（tier 14，2147483648 EU/t，无损）。 */
class UniversalCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> UniversalCableBlock() }
    override val tier: Int = 14
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 街射导线（tier 15，8589934592 EU/t，无损）。 */
class DiffractiveCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> DiffractiveCableBlock() }
    override val tier: Int = 15
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 无限导线（tier 16，34359738368 EU/t，无损）。 */
class InfinityCableBlock : BaseCableBlock(), ITiered {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec { _ -> InfinityCableBlock() }
    override val tier: Int = 16
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/**
 * 导线合成配方（级联，全部用 core 已有材料，不引入新材料）。
 *
 * 集中在一个 object 的 @RecipeProvider 方法里生成全部 11 档合成表，
 * 由 core 的 ClassScanner 在附属 main entrypoint（datagen 阶段）扫描执行。
 */
object CableRecipes {
    /**
     * 生成 11 档导线的级联合成表。由 [ic2_120_industrial_upgrade.content.recipes.ModRecipeProvider]
     * 显式调用（导线不走 @ModBlock，其 companion 不参与 ClassScanner 的 @RecipeProvider 扫描）。
     */
    fun generateRecipes(exporter: RecipeOutput) {
        // 1 光谱：core 玻璃纤维 + 混合金属锭 + 橡胶
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SpectralCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BBB")
            .pattern(" C ")
            .define('A', GlassFibreCableBlock::class.item())
            .define('B', MixedMetalIngot::class.instance())
            .define('C', RubberItem::class.instance())
            .unlockedBy(getHasName(GlassFibreCableBlock::class.item()), has(GlassFibreCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("spectral_cable"))

        // 2 质子：光谱导线 + 致密锡板 + 银锭
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProtonCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .define('A', SpectralCableBlock::class.item())
            .define('B', DenseTinPlate::class.instance())
            .define('C', SilverIngot::class.instance())
            .unlockedBy(getHasName(SpectralCableBlock::class.item()), has(SpectralCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("proton_cable"))

        // 3 奇异：质子导线 + 致密锡板 + 高级合金
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SingularCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .define('A', ProtonCableBlock::class.item())
            .define('B', DenseTinPlate::class.instance())
            .define('C', Alloy::class.instance())
            .unlockedBy(getHasName(ProtonCableBlock::class.item()), has(ProtonCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("singular_cable"))

        // 4 神话：奇异导线 + 致密铅板 + 高级合金 + 致密金板
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MythicalCableBlock::class.item(), 1)
            .pattern("DAD")
            .pattern("BCB")
            .pattern("DAD")
            .define('A', SingularCableBlock::class.item())
            .define('B', DenseLeadPlate::class.instance())
            .define('C', Alloy::class.instance())
            .define('D', DenseGoldPlate::class.instance())
            .unlockedBy(getHasName(SingularCableBlock::class.item()), has(SingularCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("mythical_cable"))

        // 5 量子：神话导线 + 钢锭 + 碳板 + 青铜锭
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, QuantumCableBlock::class.item(), 1)
            .pattern("DAD")
            .pattern("BCB")
            .pattern("DAD")
            .define('A', MythicalCableBlock::class.item())
            .define('B', SteelIngot::class.instance())
            .define('C', CarbonPlate::class.instance())
            .define('D', BronzeIngot::class.instance())
            .unlockedBy(getHasName(MythicalCableBlock::class.item()), has(MythicalCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("quantum_cable"))

        // 6 光子：量子导线 + 致密铜板 + 致密钢板
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PhotonicCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .define('A', QuantumCableBlock::class.item())
            .define('B', DenseCopperPlate::class.instance())
            .define('C', DenseSteelPlate::class.instance())
            .unlockedBy(getHasName(QuantumCableBlock::class.item()), has(QuantumCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("photonic_cable"))

        // 7 中子：光子导线 + 致密铁板 + 致密度石岩板
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, NeutronCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .define('A', PhotonicCableBlock::class.item())
            .define('B', DenseIronPlate::class.instance())
            .define('C', DenseObsidianPlate::class.instance())
            .unlockedBy(getHasName(PhotonicCableBlock::class.item()), has(PhotonicCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("neutron_cable"))

        // 8 完美：中子导线 + 致密青金石板 + 致密青铜板
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PerfectCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .define('A', NeutronCableBlock::class.item())
            .define('B', DenseLapisPlate::class.instance())
            .define('C', DenseBronzePlate::class.instance())
            .unlockedBy(getHasName(NeutronCableBlock::class.item()), has(NeutronCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("perfect_cable"))

        // 9 寰宇：完美导线 + 混合金属锭 + 致密铁板 + 致密青金石板
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, UniversalCableBlock::class.item(), 1)
            .pattern("DAD")
            .pattern("BCB")
            .pattern("DAD")
            .define('A', PerfectCableBlock::class.item())
            .define('B', DenseIronPlate::class.instance())
            .define('C', MixedMetalIngot::class.instance())
            .define('D', DenseLapisPlate::class.instance())
            .unlockedBy(getHasName(PerfectCableBlock::class.item()), has(PerfectCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("universal_cable"))

        // 10 街射：寰宇导线 + 碳板 + 混合金属锭 + 致密度石岩板
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, DiffractiveCableBlock::class.item(), 1)
            .pattern("DAD")
            .pattern("BCB")
            .pattern("DAD")
            .define('A', UniversalCableBlock::class.item())
            .define('B', CarbonPlate::class.instance())
            .define('C', MixedMetalIngot::class.instance())
            .define('D', DenseObsidianPlate::class.instance())
            .unlockedBy(getHasName(UniversalCableBlock::class.item()), has(UniversalCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("diffractive_cable"))

        // 11 无限：街射导线 + 致密青金石板 + 致密青铜板 + 致密度石岩板
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, InfinityCableBlock::class.item(), 1)
            .pattern("DAD")
            .pattern("BCB")
            .pattern("DAD")
            .define('A', DiffractiveCableBlock::class.item())
            .define('B', DenseLapisPlate::class.instance())
            .define('C', DenseBronzePlate::class.instance())
            .define('D', DenseObsidianPlate::class.instance())
            .unlockedBy(getHasName(DiffractiveCableBlock::class.item()), has(DiffractiveCableBlock::class.item()))
            .save(exporter, IC2IndustrialUpgrade.id("infinity_cable"))
    }
}
