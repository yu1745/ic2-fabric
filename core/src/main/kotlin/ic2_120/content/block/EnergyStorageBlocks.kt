package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient


import ic2_120.content.block.cables.DoubleInsulatedGoldCableBlock
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.block.cables.InsulatedTinCableBlock
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.BronzePlate
import ic2_120.content.item.Circuit
import ic2_120.content.item.RubberItem
import ic2_120.content.item.energy.AdvancedReBatteryItem
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.item.energy.LapotronCrystalItem
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.storage.EnergyStorageBlock.EnergyStorageBlockItem
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.item.energy.ReBatteryItem
import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.tags.ItemTags
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.InteractionResult

// ============== Block Definitions ==============

@ModBlock(name = "batbox", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class BatBoxBlock : EnergyStorageBlock(EnergyStorageConfig.BATBOX) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.BatBoxBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.BatBoxBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val tinCable = InsulatedTinCableBlock::class.item()
            val battery = ReBatteryItem::class.instance()
            if (tinCable != Items.AIR && battery != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BatBoxBlock::class.item(), 1)
                    .pattern("WTW")
                    .pattern("BBB")
                    .pattern("WWW")
                    .define('W', ItemTags.PLANKS)
                    .define('T', tinCable)
                    .define('B', battery)
                    .unlockedBy(getHasName(battery), has(battery))
                    .save(exporter, BatBoxBlock::class.id())
            }
        }
    }

    class BatBoxBlockItem(block: Block, settings: Item.Properties) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.BATBOX) {
        override val translationKeyFull: String = "block.ic2_120.batbox_full"
    }
}

@ModBlock(name = "cesu", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class CesuBlock : EnergyStorageBlock(EnergyStorageConfig.CESU) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.CesuBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.CesuBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val copperCable = InsulatedCopperCableBlock::class.item()
            val battery = AdvancedReBatteryItem::class.instance()
            val plate = BronzePlate::class.instance()
            if (copperCable != Items.AIR && battery != Items.AIR && plate != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CesuBlock::class.item(), 1)
                    .pattern("WTW")
                    .pattern("BBB")
                    .pattern("WWW")
                    .define('W', Ingredient.of(ModTags.Compat.Items.PLATES_BRONZE))
                    .define('T', copperCable)
                    .define('B', battery)
                    .unlockedBy(getHasName(battery), has(battery))
                    .save(exporter, CesuBlock::class.id())
            }
        }
    }

    class CesuBlockItem(block: Block, settings: Item.Properties) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.CESU) {
        override val translationKeyFull: String = "block.ic2_120.cesu_full"
    }
}

@ModBlock(name = "mfe", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfeBlock : EnergyStorageBlock(EnergyStorageConfig.MFE) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfeBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.MfeBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val goldCable = DoubleInsulatedGoldCableBlock::class.item()
            val crystal = EnergyCrystalItem::class.instance()
            val machine = MachineCasingBlock::class.item()
            if (goldCable != Items.AIR && crystal != Items.AIR && machine != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MfeBlock::class.item(), 1)
                    .pattern("GEG")
                    .pattern("EME")
                    .pattern("GEG")
                    .define('G', goldCable)
                    .define('E', crystal)
                    .define('M', machine)
                    .unlockedBy(getHasName(crystal), has(crystal))
                    .save(exporter, MfeBlock::class.id())
            }
        }
    }

    class MfeBlockItem(block: Block, settings: Item.Properties) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFE) {
        override val translationKeyFull: String = "block.ic2_120.mfe_full"
    }
}

@ModBlock(name = "mfsu", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfsuBlock : EnergyStorageBlock(EnergyStorageConfig.MFSU) {
    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfsuBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.MfsuBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val lapotron = LapotronCrystalItem::class.instance()
            val advCircuit = AdvancedCircuit::class.instance()
            val mfe = MfeBlock::class.item()
            val advCasing = AdvancedMachineCasingBlock::class.item()
            if (lapotron != Items.AIR && advCircuit != Items.AIR && mfe != Items.AIR && advCasing != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MfsuBlock::class.item(), 1)
                    .pattern("LAL")
                    .pattern("LFL")
                    .pattern("LCL")
                    .define('L', lapotron)
                    .define('A', advCircuit)
                    .define('F', mfe)
                    .define('C', advCasing)
                    .unlockedBy(getHasName(mfe), has(mfe))
                    .save(exporter, MfsuBlock::class.id())
            }
        }
    }

    class MfsuBlockItem(block: Block, settings: Item.Properties) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFSU) {
        override val translationKeyFull: String = "block.ic2_120.mfsu_full"
    }
}

/**
 * 充电座方块。与储电箱共用 EnergyStorageBlockEntity（含红石模式系统）与 GUI。
 */
abstract class ChargepadBlock(val config: EnergyStorageConfig) : MachineBlock(defaultMachineSettings() .noOcclusion()) { // 3D 模型：关闭相邻面剔除
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)
                .setValue(EnergyStorageBlock.ACTIVE, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(EnergyStorageBlock.ACTIVE)
    }

    /** 红石模式输出（与储电箱同源：BE 红石模式 0-6，默认 0 = 不输出）。 */
    override fun isSignalSource(state: BlockState): Boolean = true

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getSignal(
        state: BlockState,
        world: net.minecraft.world.level.BlockGetter,
        pos: BlockPos,
        direction: net.minecraft.core.Direction
    ): Int {
        val be = world.getBlockEntity(pos) as? EnergyStorageBlockEntity ?: return 0
        return if (be.emitsRedstoneNow) 15 else 0
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { factory ->
                player.openMenu(factory)
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getShape(
        state: BlockState,
        world: net.minecraft.world.level.BlockGetter,
        pos: net.minecraft.core.BlockPos,
        context: net.minecraft.world.phys.shapes.CollisionContext
    ): net.minecraft.world.phys.shapes.VoxelShape = FULL_MINUS_TOP

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: net.minecraft.world.level.BlockGetter,
        pos: net.minecraft.core.BlockPos,
        context: net.minecraft.world.phys.shapes.CollisionContext
    ): net.minecraft.world.phys.shapes.VoxelShape = FULL_MINUS_TOP

    companion object {
        /** 去掉顶部的 1/16，与模型视觉对齐 */
        private val FULL_MINUS_TOP = net.minecraft.world.phys.shapes.Shapes.box(0.0, 0.0, 0.0, 1.0, 15.0 / 16.0, 1.0)
    }
}

@ModBlock(name = "batbox_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class BatBoxChargepadBlock : ChargepadBlock(EnergyStorageConfig.BATBOX_CHARGEPAD) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.BatBoxChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.BatBoxChargepadBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val circuit = Circuit::class.instance()
            val rubber = RubberItem::class.instance()
            val base = BatBoxBlock::class.item()
            if (circuit != Items.AIR && rubber != Items.AIR && base != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BatBoxChargepadBlock::class.item(), 1)
                    .pattern("CPC")
                    .pattern("RBR")
                    .define('C', circuit)
                    .define('P', Items.STONE_PRESSURE_PLATE)
                    .define('R', Ingredient.of(ModTags.Compat.Items.RUBBER))
                    .define('B', base)
                    .unlockedBy(getHasName(base), has(base))
                    .save(exporter, BatBoxChargepadBlock::class.id())
            }
        }
    }

    class BatBoxChargepadBlockItem(block: Block, settings: Item.Properties) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.BATBOX_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.batbox_chargepad_full"
    }
}

@ModBlock(name = "cesu_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class CesuChargepadBlock : ChargepadBlock(EnergyStorageConfig.CESU_CHARGEPAD) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.CesuChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.CesuChargepadBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val circuit = Circuit::class.instance()
            val rubber = RubberItem::class.instance()
            val base = CesuBlock::class.item()
            if (circuit != Items.AIR && rubber != Items.AIR && base != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CesuChargepadBlock::class.item(), 1)
                    .pattern("CPC")
                    .pattern("RBR")
                    .define('C', circuit)
                    .define('P', Items.STONE_PRESSURE_PLATE)
                    .define('R', Ingredient.of(ModTags.Compat.Items.RUBBER))
                    .define('B', base)
                    .unlockedBy(getHasName(base), has(base))
                    .save(exporter, CesuChargepadBlock::class.id())
            }
        }
    }

    class CesuChargepadBlockItem(block: Block, settings: Item.Properties) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.CESU_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.cesu_chargepad_full"
    }
}

@ModBlock(name = "mfe_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfeChargepadBlock : ChargepadBlock(EnergyStorageConfig.MFE_CHARGEPAD) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfeChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.MfeChargepadBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val circuit = Circuit::class.instance()
            val rubber = RubberItem::class.instance()
            val base = MfeBlock::class.item()
            if (circuit != Items.AIR && rubber != Items.AIR && base != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MfeChargepadBlock::class.item(), 1)
                    .pattern("CPC")
                    .pattern("RBR")
                    .define('C', circuit)
                    .define('P', Items.STONE_PRESSURE_PLATE)
                    .define('R', Ingredient.of(ModTags.Compat.Items.RUBBER))
                    .define('B', base)
                    .unlockedBy(getHasName(base), has(base))
                    .save(exporter, MfeChargepadBlock::class.id())
            }
        }
    }

    class MfeChargepadBlockItem(block: Block, settings: Item.Properties) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFE_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.mfe_chargepad_full"
    }
}

@ModBlock(name = "mfsu_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfsuChargepadBlock : ChargepadBlock(EnergyStorageConfig.MFSU_CHARGEPAD) {
    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfsuChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.MfsuChargepadBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val circuit = Circuit::class.instance()
            val rubber = RubberItem::class.instance()
            val base = MfsuBlock::class.item()
            if (circuit != Items.AIR && rubber != Items.AIR && base != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MfsuChargepadBlock::class.item(), 1)
                    .pattern("CPC")
                    .pattern("RBR")
                    .define('C', circuit)
                    .define('P', Items.STONE_PRESSURE_PLATE)
                    .define('R', Ingredient.of(ModTags.Compat.Items.RUBBER))
                    .define('B', base)
                    .unlockedBy(getHasName(base), has(base))
                    .save(exporter, MfsuChargepadBlock::class.id())
            }
        }
    }

    class MfsuChargepadBlockItem(block: Block, settings: Item.Properties) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFSU_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.mfsu_chargepad_full"
    }
}
