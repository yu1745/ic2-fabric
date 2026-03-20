package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.storage.EnergyStorageBlock.EnergyStorageBlockItem
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

// ============== Block Definitions ==============

@ModBlock(name = "batbox", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class BatBoxBlock : EnergyStorageBlock(EnergyStorageConfig.BATBOX) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.BatBoxBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.BatBoxBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class BatBoxBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.BATBOX) {
        override val translationKeyFull: String = "block.ic2_120.batbox_full"
    }
}

@ModBlock(name = "cesu", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class CesuBlock : EnergyStorageBlock(EnergyStorageConfig.CESU) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.CesuBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.CesuBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class CesuBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.CESU) {
        override val translationKeyFull: String = "block.ic2_120.cesu_full"
    }
}

@ModBlock(name = "mfe", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class MfeBlock : EnergyStorageBlock(EnergyStorageConfig.MFE) {
    override fun getCasingDrop(): Item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "advanced_machine"))
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfeBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.MfeBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class MfeBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFE) {
        override val translationKeyFull: String = "block.ic2_120.mfe_full"
    }
}

@ModBlock(name = "mfsu", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class MfsuBlock : EnergyStorageBlock(EnergyStorageConfig.MFSU) {
    override fun getCasingDrop(): Item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "advanced_machine"))
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        EnergyStorageBlockEntity.MfsuBlockEntity(pos, state)
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, EnergyStorageBlockEntity.MfsuBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    class MfsuBlockItem(block: Block, settings: Item.Settings) :
        EnergyStorageBlockItem(block, settings, EnergyStorageConfig.MFSU) {
        override val translationKeyFull: String = "block.ic2_120.mfsu_full"
    }
}
