package ic2_120.content.block.machines

import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.CreativeGeneratorBlock
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.sync.CreativeGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
<<<<<<< HEAD
=======
import net.minecraft.registry.RegistryWrapper
>>>>>>> backup/1.21.1
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 创造模式发电机方块实体。无缓存，直接输出 512 EU/t。
 */
@ModBlockEntity(block = CreativeGeneratorBlock::class)
class CreativeGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IGenerator, ITieredMachine {

    override val activeProperty = CreativeGeneratorBlock.ACTIVE
    override val tier = GENERATOR_TIER

    companion object {
        const val GENERATOR_TIER = 1
    }

<<<<<<< HEAD
    val syncedData = SyncedData()
=======
    val syncedData = SyncedData(this)
>>>>>>> backup/1.21.1

    @RegisterEnergy
    val sync = CreativeGeneratorSync(syncedData, { world?.time })

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    override fun getInventory(): net.minecraft.inventory.Inventory? = null

    constructor(pos: BlockPos, state: BlockState) : this(
        CreativeGeneratorBlockEntity::class.type(),
        pos,
        state
    )

<<<<<<< HEAD
    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
=======
    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
>>>>>>> backup/1.21.1
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CreativeGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, CreativeGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
    }

<<<<<<< HEAD
    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
=======
    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
>>>>>>> backup/1.21.1
        syncedData.writeNbt(nbt)
        nbt.putLong(CreativeGeneratorSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 生成 512 EU/t，无缓存直出
        val space = (CreativeGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        val toGenerate = if (space > 0L) minOf(CreativeGeneratorSync.GENERATION_RATE, space) else 0L
        sync.generateEnergy(toGenerate)
        adjacentEnergyTransfer.tick()

        setActiveState(world, pos, state, true)
        sync.syncCurrentTickFlow()
    }
}
