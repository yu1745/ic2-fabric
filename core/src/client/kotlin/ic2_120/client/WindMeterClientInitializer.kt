package ic2_120.client

import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.content.item.WindMeter
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level

object WindMeterClientInitializer {
    fun init() {
        WindMeter.clientOutput = { world, user ->
            val we = WindKineticGeneratorBlockEntity
            val mean = we.meanWindFromY(user.blockY)
            val weather = we.weatherMultiplier(world)
            val gust = we.worldGustFactor(world, user.blockPosition())
            val effective = mean * weather * gust

            fun ku(multiplier: Int): Int =
                kotlin.math.floor(we.BASE_KU_AT_PEAK * multiplier * effective).toInt().coerceAtLeast(0)

            fun requiredStartY(multiplier: Int): Int {
                val threshold = we.startThresholdForMultiplier(multiplier)
                if (effective >= threshold) return -2
                val top = world.maxBuildHeight
                for (y in 0..top) {
                    val windAtY = we.meanWindFromY(y) * weather * gust
                    if (windAtY >= threshold) return y
                }
                return -1
            }

            fun permilleToPct(v: Double): String = String.format("%.1f%%", v * 100.0)
            fun permilleToMultiplier(v: Double): String = String.format("%.2fx", v)

            val hud = Minecraft.getInstance().gui.chat
            hud.addMessage(Component.translatable("gui.ic2_120.wind_meter.mean_wind", permilleToPct(mean)))
            hud.addMessage(Component.translatable("gui.ic2_120.wind_meter.weather_bonus", permilleToMultiplier(weather)))
            hud.addMessage(Component.translatable("gui.ic2_120.wind_meter.gust_bonus", permilleToMultiplier(gust)))
            hud.addMessage(Component.translatable("gui.ic2_120.wind_meter.effective_wind", permilleToPct(effective)))

            fun rotorLine(rotorKey: String, multiplier: Int) {
                val kuVal = ku(multiplier)
                val requiredY = requiredStartY(multiplier)
                val rotorName = Component.translatable(rotorKey)
                when {
                    requiredY >= 0 -> hud.addMessage(Component.translatable("gui.ic2_120.wind_meter.rotor_need_height", rotorName, kuVal, requiredY))
                    requiredY == -1 -> hud.addMessage(Component.translatable("gui.ic2_120.wind_meter.rotor_no_weather", rotorName, kuVal))
                    else -> hud.addMessage(Component.translatable("gui.ic2_120.wind_meter.rotor_output", rotorName, kuVal))
                }
            }

            rotorLine("gui.ic2_120.wind_meter.wood", 1)
            rotorLine("gui.ic2_120.wind_meter.bronze", 2)
            rotorLine("gui.ic2_120.wind_meter.iron", 2)
            rotorLine("gui.ic2_120.wind_meter.steel", 3)
            rotorLine("gui.ic2_120.wind_meter.carbon", 4)
        }
    }
}
