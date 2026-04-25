package ic2_120.content.block

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
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

// ============== Block Definitions ==============

@ModBlock(name = "batbox", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class BatBoxBlock : EnergyStorageBlock(EnergyStorageConfig.BATBOX) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.BatBoxBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.BatBoxBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val tinCable = InsulatedTinCableBlock::class.item()
            val battery = ReBatteryItem::class.instance()
            val planks = Items.OAK_PLANKS
            if (tinCable != Items.AIR && battery != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BatBoxBlock::class.item(), 1)
                    .pattern("WTW")
                    .pattern("BBB")
                    .pattern("WWW")
                    .input('W', planks)
                    .input('T', tinCable)
                    .input('B', battery)
                    .criterion(hasItem(battery), conditionsFromItem(battery))
                    .offerTo(exporter, BatBoxBlock::class.id())
            }
        }
    }

    class BatBoxBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.BATBOX) {
        override val translationKeyFull: String = "block.ic2_120.batbox_full"
    }
}

@ModBlock(name = "cesu", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class CesuBlock : EnergyStorageBlock(EnergyStorageConfig.CESU) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.CesuBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.CesuBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val copperCable = InsulatedCopperCableBlock::class.item()
            val battery = AdvancedReBatteryItem::class.instance()
            val plate = BronzePlate::class.instance()
            if (copperCable != Items.AIR && battery != Items.AIR && plate != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CesuBlock::class.item(), 1)
                    .pattern("WTW")
                    .pattern("BBB")
                    .pattern("WWW")
                    .input('W', plate)
                    .input('T', copperCable)
                    .input('B', battery)
                    .criterion(hasItem(battery), conditionsFromItem(battery))
                    .offerTo(exporter, CesuBlock::class.id())
            }
        }
    }

    class CesuBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.CESU) {
        override val translationKeyFull: String = "block.ic2_120.cesu_full"
    }
}

@ModBlock(name = "mfe", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfeBlock : EnergyStorageBlock(EnergyStorageConfig.MFE) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfeBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.MfeBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val goldCable = DoubleInsulatedGoldCableBlock::class.item()
            val crystal = EnergyCrystalItem::class.instance()
            val machine = MachineCasingBlock::class.item()
            if (goldCable != Items.AIR && crystal != Items.AIR && machine != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MfeBlock::class.item(), 1)
                    .pattern("GEG")
                    .pattern("EME")
                    .pattern("GEG")
                    .input('G', goldCable)
                    .input('E', crystal)
                    .input('M', machine)
                    .criterion(hasItem(crystal), conditionsFromItem(crystal))
                    .offerTo(exporter, MfeBlock::class.id())
            }
        }
    }

    class MfeBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFE) {
        override val translationKeyFull: String = "block.ic2_120.mfe_full"
    }
}

@ModBlock(name = "mfsu", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfsuBlock : EnergyStorageBlock(EnergyStorageConfig.MFSU) {
    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfsuBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.MfsuBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val lapotron = LapotronCrystalItem::class.instance()
            val advCircuit = AdvancedCircuit::class.instance()
            val mfe = MfeBlock::class.item()
            val advCasing = AdvancedMachineCasingBlock::class.item()
            if (lapotron != Items.AIR && advCircuit != Items.AIR && mfe != Items.AIR && advCasing != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MfsuBlock::class.item(), 1)
                    .pattern("LAL")
                    .pattern("LFL")
                    .pattern("LCL")
                    .input('L', lapotron)
                    .input('A', advCircuit)
                    .input('F', mfe)
                    .input('C', advCasing)
                    .criterion(hasItem(mfe), conditionsFromItem(mfe))
                    .offerTo(exporter, MfsuBlock::class.id())
            }
        }
    }

    class MfsuBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFSU) {
        override val translationKeyFull: String = "block.ic2_120.mfsu_full"
    }
}

abstract class ChargepadBlock(config: EnergyStorageConfig) : EnergyStorageBlock(config) {
    init {
        defaultState = stateManager.defaultState
            .with(Properties.HORIZONTAL_FACING, net.minecraft.util.math.Direction.NORTH)
            .with(EnergyStorageBlock.ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(EnergyStorageBlock.ACTIVE)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(
        state: BlockState,
        world: net.minecraft.world.BlockView,
        pos: net.minecraft.util.math.BlockPos,
        context: net.minecraft.block.ShapeContext
    ): net.minecraft.util.shape.VoxelShape = FULL_MINUS_TOP

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: net.minecraft.world.BlockView,
        pos: net.minecraft.util.math.BlockPos,
        context: net.minecraft.block.ShapeContext
    ): net.minecraft.util.shape.VoxelShape = FULL_MINUS_TOP

    companion object {
        /** 去掉顶部的 1/16，与模型视觉对齐 */
        private val FULL_MINUS_TOP = net.minecraft.util.shape.VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 15.0 / 16.0, 1.0)
    }
}

@ModBlock(name = "batbox_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class BatBoxChargepadBlock : ChargepadBlock(EnergyStorageConfig.BATBOX_CHARGEPAD) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.BatBoxChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.BatBoxChargepadBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val circuit = Circuit::class.instance()
            val rubber = RubberItem::class.instance()
            val base = BatBoxBlock::class.item()
            if (circuit != Items.AIR && rubber != Items.AIR && base != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BatBoxChargepadBlock::class.item(), 1)
                    .pattern("CPC")
                    .pattern("RBR")
                    .input('C', circuit)
                    .input('P', Items.STONE_PRESSURE_PLATE)
                    .input('R', rubber)
                    .input('B', base)
                    .criterion(hasItem(base), conditionsFromItem(base))
                    .offerTo(exporter, BatBoxChargepadBlock::class.id())
            }
        }
    }

    class BatBoxChargepadBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.BATBOX_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.batbox_chargepad_full"
    }
}

@ModBlock(name = "cesu_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class CesuChargepadBlock : ChargepadBlock(EnergyStorageConfig.CESU_CHARGEPAD) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.CesuChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.CesuChargepadBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val circuit = Circuit::class.instance()
            val rubber = RubberItem::class.instance()
            val base = CesuBlock::class.item()
            if (circuit != Items.AIR && rubber != Items.AIR && base != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CesuChargepadBlock::class.item(), 1)
                    .pattern("CPC")
                    .pattern("RBR")
                    .input('C', circuit)
                    .input('P', Items.STONE_PRESSURE_PLATE)
                    .input('R', rubber)
                    .input('B', base)
                    .criterion(hasItem(base), conditionsFromItem(base))
                    .offerTo(exporter, CesuChargepadBlock::class.id())
            }
        }
    }

    class CesuChargepadBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.CESU_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.cesu_chargepad_full"
    }
}

@ModBlock(name = "mfe_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfeChargepadBlock : ChargepadBlock(EnergyStorageConfig.MFE_CHARGEPAD) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfeChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.MfeChargepadBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val circuit = Circuit::class.instance()
            val rubber = RubberItem::class.instance()
            val base = MfeBlock::class.item()
            if (circuit != Items.AIR && rubber != Items.AIR && base != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MfeChargepadBlock::class.item(), 1)
                    .pattern("CPC")
                    .pattern("RBR")
                    .input('C', circuit)
                    .input('P', Items.STONE_PRESSURE_PLATE)
                    .input('R', rubber)
                    .input('B', base)
                    .criterion(hasItem(base), conditionsFromItem(base))
                    .offerTo(exporter, MfeChargepadBlock::class.id())
            }
        }
    }

    class MfeChargepadBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFE_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.mfe_chargepad_full"
    }
}

@ModBlock(name = "mfsu_chargepad", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "bat_box")
class MfsuChargepadBlock : ChargepadBlock(EnergyStorageConfig.MFSU_CHARGEPAD) {
    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfsuChargepadBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, EnergyStorageBlockEntity.MfsuChargepadBlockEntity::class.type()){ w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val circuit = Circuit::class.instance()
            val rubber = RubberItem::class.instance()
            val base = MfsuBlock::class.item()
            if (circuit != Items.AIR && rubber != Items.AIR && base != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MfsuChargepadBlock::class.item(), 1)
                    .pattern("CPC")
                    .pattern("RBR")
                    .input('C', circuit)
                    .input('P', Items.STONE_PRESSURE_PLATE)
                    .input('R', rubber)
                    .input('B', base)
                    .criterion(hasItem(base), conditionsFromItem(base))
                    .offerTo(exporter, MfsuChargepadBlock::class.id())
            }
        }
    }

    class MfsuChargepadBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFSU_CHARGEPAD) {
        override val translationKeyFull: String = "block.ic2_120.mfsu_chargepad_full"
    }
}
