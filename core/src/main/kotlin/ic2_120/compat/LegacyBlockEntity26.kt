package ic2_120.compat

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/** Keeps machine-specific data in a single 26.2 value field while the machines migrate. */
open class LegacyBlockEntity26(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : BlockEntity(type, pos, state) {
    protected open fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {}
    protected open fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {}

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        val nbt = CompoundTag()
        saveAdditional(nbt, level?.registryAccess() ?: RegistryAccess.EMPTY)
        if (!nbt.isEmpty) output.store("ic2_data", CompoundTag.CODEC, nbt)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        loadAdditional(input.read("ic2_data", CompoundTag.CODEC).orElseGet(::CompoundTag), input.lookup())
    }
}
