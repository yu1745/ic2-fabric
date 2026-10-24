package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import org.slf4j.LoggerFactory


object JetpackSoundController {
    private val minecraft = Minecraft.getInstance()
    private val logger = LoggerFactory.getLogger("ic2_120/JetpackSoundController")
    private val JETPACK_SOUND_ID = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "item.jetpack.loop")
    private val JETPACK_SOUND = SoundEvent.createVariableRangeEvent(JETPACK_SOUND_ID)

    private var loopSound: JetpackLoopSoundInstance? = null
    private var lastActive = false

    fun register() {
        logger.info("Jetpack sound controller registered. soundId={}", JETPACK_SOUND_ID)
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = minecraft.player
            val active = player != null && isJetpackFlightActive(player)

            if (active && !lastActive) {
                logger.info("Jetpack sound start.")
            } else if (!active && lastActive) {
                logger.info("Jetpack sound fade out.")
            }
            lastActive = active

            val current = loopSound
            if (active) {
                if (current == null || current.isStopped) {
                    val created = JetpackLoopSoundInstance(player)
                    loopSound = created
                    minecraft.getSoundManager().play(created)
                } else {
                    current.setFadingOut(false)
                }
            } else if (current != null) {
                current.setFadingOut(true)
                if (current.isStopped) {
                    loopSound = null
                }
            }
        }
    }

    private fun isJetpackFlightActive(player: LocalPlayer): Boolean {
        if (player.isSpectator || player.abilities.instabuild) return false
        if (!player.abilities.flying) return false
        if (player.isOnGround || player.isInWater || player.onClimbable) return false

        val chest = player.getItemBySlot(EquipmentSlot.CHEST)
        return chest.item is JetpackItem || chest.item is ElectricJetpack
    }

    private class JetpackLoopSoundInstance(
        private val player: LocalPlayer
    ) : AbstractTickableSoundInstance(JETPACK_SOUND, SoundSource.PLAYERS, RandomSource.create()) {
        private var fadingOut = false

        init {
            looping = true
            delay = 0
            volume = 0.42f
            pitch = 1.0f
            x = player.x
            y = player.y
            z = player.z
        }

        override fun tick() {
            if (player.isRemoved) {
                stop()
                return
            }

            x = player.x
            y = player.y
            z = player.z

            if (fadingOut) {
                volume = (volume - 0.06f).coerceAtLeast(0f)
                if (volume <= 0.001f) {
                    stop()
                }
            } else {
                volume = 0.42f
            }
        }

        fun setFadingOut(value: Boolean) {
            fadingOut = value
        }
    }
}
