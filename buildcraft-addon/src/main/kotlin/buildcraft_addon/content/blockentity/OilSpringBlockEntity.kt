package buildcraft_addon.content.blockentity

import buildcraft_addon.content.block.SpringBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.BlockPos

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

    override fun writeNbt(nbt: CompoundTag) {
        super.writeNbt(nbt)
        nbt.putInt("totalSources", totalSources)
        val progressList = CompoundTag()
        pumpProgress.forEach { (uuid, info) ->
            val entry = CompoundTag()
            entry.putString("name", info.playerName)
            entry.putInt("pumped", info.sourcesPumped)
            entry.putLong("lastTime", info.lastPumpTime)
            progressList.put(uuid, entry)
        }
        nbt.put("pumpProgress", progressList)
    }

    override fun readNbt(nbt: CompoundTag) {
        super.readNbt(nbt)
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
