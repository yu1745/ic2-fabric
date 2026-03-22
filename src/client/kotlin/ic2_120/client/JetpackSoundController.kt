package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.sound.MovingSoundInstance
import net.minecraft.entity.EquipmentSlot
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.random.Random
import org.slf4j.LoggerFactory

object JetpackSoundController {
    private val logger = LoggerFactory.getLogger("ic2_120/JetpackSoundController")
    private val JETPACK_SOUND_ID = Identifier(Ic2_120.MOD_ID, "item.jetpack.loop")
    private val JETPACK_SOUND = SoundEvent.of(JETPACK_SOUND_ID)

    private var loopSound: JetpackLoopSoundInstance? = null
    private var lastActive = false

    fun register() {
        logger.info("Jetpack sound controller registered. soundId={}", JETPACK_SOUND_ID)
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player
            val active = player != null && isJetpackFlightActive(player)

            if (active && !lastActive) {
                logger.info("Jetpack sound start.")
            } else if (!active && lastActive) {
                logger.info("Jetpack sound fade out.")
            }
            lastActive = active

            val current = loopSound
            if (active) {
                if (current == null || current.isDone) {
                    val created = JetpackLoopSoundInstance(player)
                    loopSound = created
                    client.soundManager.play(created)
                } else {
                    current.setFadingOut(false)
                }
            } else if (current != null) {
                current.setFadingOut(true)
                if (current.isDone) {
                    loopSound = null
                }
            }
        }
    }

    private fun isJetpackFlightActive(player: ClientPlayerEntity): Boolean {
        if (player.isCreative || player.isSpectator) return false
        if (!player.abilities.flying) return false
        if (player.isOnGround || player.isTouchingWater || player.isClimbing) return false

        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        return when (val item = chest.item) {
            is JetpackItem -> JetpackItem.isFlightEnabled(chest)
            is ElectricJetpack -> item.isFlightEnabled(chest)
            else -> false
        }
    }

    private class JetpackLoopSoundInstance(
        private val player: ClientPlayerEntity
    ) : MovingSoundInstance(JETPACK_SOUND, SoundCategory.PLAYERS, Random.create()) {
        private var fadingOut = false

        init {
            repeat = true
            repeatDelay = 0
            volume = 0.42f
            pitch = 1.0f
            x = player.x
            y = player.y
            z = player.z
        }

        override fun tick() {
            if (player.isRemoved) {
                setDone()
                return
            }

            x = player.x
            y = player.y
            z = player.z

            if (fadingOut) {
                volume = (volume - 0.06f).coerceAtLeast(0f)
                if (volume <= 0.001f) {
                    setDone()
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
