package buildcraft_addon.content.blockentity

import buildcraft_addon.content.block.SpringBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.math.BlockPos

@ModBlockEntity(block = SpringBlock::class)
class OilSpringBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    constructor(pos: BlockPos, state: BlockState) : this(
        OilSpringBlockEntity::class.type(), pos, state
    )

    var totalSources: Int = 0
    private val pumpProgress = mutableMapOf<String, PlayerPumpInfo>()

    fun onPumpOil(playerUuid: String, playerName: String) {
        val info = pumpProgress.getOrPut(playerUuid) { PlayerPumpInfo(playerName) }
        info.sourcesPumped++
        info.lastPumpTime = System.currentTimeMillis()
        if (totalSources > 0 && info.sourcesPumped >= totalSources * 7 / 8) {
            // Achievement placeholder
        }
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putInt("totalSources", totalSources)
        val progressList = NbtCompound()
        pumpProgress.forEach { (uuid, info) ->
            val entry = NbtCompound()
            entry.putString("name", info.playerName)
            entry.putInt("pumped", info.sourcesPumped)
            entry.putLong("lastTime", info.lastPumpTime)
            progressList.put(uuid, entry)
        }
        nbt.put("pumpProgress", progressList)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        totalSources = nbt.getInt("totalSources")
        pumpProgress.clear()
        if (nbt.contains("pumpProgress")) {
            val progressList = nbt.getCompound("pumpProgress")
            for (key in progressList.keys) {
                val entry = progressList.getCompound(key)
                pumpProgress[key] = PlayerPumpInfo(
                    playerName = entry.getString("name"),
                    sourcesPumped = entry.getInt("pumped"),
                    lastPumpTime = entry.getLong("lastTime")
                )
            }
        }
    }

    data class PlayerPumpInfo(
        val playerName: String,
        var sourcesPumped: Int = 0,
        var lastPumpTime: Long = 0
    )
}
