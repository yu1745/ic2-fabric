package ic2_120.content.block.pipes

import ic2_120.registry.BlockEntityTypeStore
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class PipeBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(TYPE, pos, state) {
    var network: PipeNetwork? = null
    var pipeLoad: Long = 0L
    private var disabledMask: Int = 0

    fun isDisabled(direction: Direction): Boolean {
        val bit = 1 shl direction.id
        return (disabledMask and bit) != 0
    }

    fun toggleDisabled(direction: Direction): Boolean {
        val bit = 1 shl direction.id
        disabledMask = if ((disabledMask and bit) != 0) disabledMask and bit.inv() else disabledMask or bit
        markDirty()
        return (disabledMask and bit) != 0
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        val net = network ?: PipeNetworkManager.getOrCreateNetwork(world, pos).also { network = it }
        net.tickIfNeeded(world)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        disabledMask = nbt.getInt("DisabledMask")
        pipeLoad = nbt.getLong("PipeLoad")
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putInt("DisabledMask", disabledMask)
        nbt.putLong("PipeLoad", pipeLoad)
    }

    companion object {
        lateinit var TYPE: BlockEntityType<PipeBlockEntity>
            private set

        fun register(modId: String) {
            val ids = listOf(
                "bronze_pipe_tiny", "bronze_pipe_small", "bronze_pipe_medium", "bronze_pipe_large",
                "carbon_pipe_tiny", "carbon_pipe_small", "carbon_pipe_medium", "carbon_pipe_large"
            ).map { Identifier(modId, it) }
            val blocks = ids.mapNotNull { id -> Registries.BLOCK.getOrEmpty(id).orElse(null) }
            require(blocks.size == ids.size) { "注册 PipeBlockEntity 失败：找不到部分管道方块" }

            val factory = FabricBlockEntityTypeBuilder.Factory<PipeBlockEntity> { p, s -> PipeBlockEntity(p, s) }
            @Suppress("UNCHECKED_CAST")
            TYPE = FabricBlockEntityTypeBuilder.create(factory, *blocks.toTypedArray()).build() as BlockEntityType<PipeBlockEntity>
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier(modId, "pipe"), TYPE)
            BlockEntityTypeStore.registerType(PipeBlockEntity::class, TYPE)
        }
    }
}
