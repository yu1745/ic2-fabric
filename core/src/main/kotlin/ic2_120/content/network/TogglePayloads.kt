package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

object ToggleNightVisionGogglesPayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleNightVisionGogglesPayload>(Ic2_120.id("toggle_night_vision_goggles"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleNightVisionGogglesPayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleNightVisionGogglesPayload }
    )
    override fun getId() = ID
}

object ToggleNanoVisionPayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleNanoVisionPayload>(Ic2_120.id("toggle_nano_vision"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleNanoVisionPayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleNanoVisionPayload }
    )
    override fun getId() = ID
}

object ToggleQuantumFlightPayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleQuantumFlightPayload>(Ic2_120.id("toggle_quantum_flight"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleQuantumFlightPayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleQuantumFlightPayload }
    )
    override fun getId() = ID
}

object ToggleIridiumSilkTouchPayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleIridiumSilkTouchPayload>(Ic2_120.id("toggle_iridium_silk_touch"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleIridiumSilkTouchPayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleIridiumSilkTouchPayload }
    )
    override fun getId() = ID
}

object ToggleJetpackFlightPayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleJetpackFlightPayload>(Ic2_120.id("toggle_jetpack_flight"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleJetpackFlightPayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleJetpackFlightPayload }
    )
    override fun getId() = ID
}

object ToggleFoamSprayerModePayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleFoamSprayerModePayload>(Ic2_120.id("toggle_foam_sprayer_mode"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleFoamSprayerModePayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleFoamSprayerModePayload }
    )
    override fun getId() = ID
}

object ToggleMiningLaserModePayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleMiningLaserModePayload>(Ic2_120.id("toggle_mining_laser_mode"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleMiningLaserModePayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleMiningLaserModePayload }
    )
    override fun getId() = ID
}

object ToggleQuantumLeggingsSpeedPayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleQuantumLeggingsSpeedPayload>(Ic2_120.id("toggle_quantum_leggings_speed"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleQuantumLeggingsSpeedPayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleQuantumLeggingsSpeedPayload }
    )
    override fun getId() = ID
}

object ToggleQuantumBootsJumpPayload : CustomPayload {
    val ID = CustomPayload.Id<ToggleQuantumBootsJumpPayload>(Ic2_120.id("toggle_quantum_boots_jump"))
    val CODEC: PacketCodec<PacketByteBuf, ToggleQuantumBootsJumpPayload> = PacketCodec.of(
        { _, _ -> },
        { ToggleQuantumBootsJumpPayload }
    )
    override fun getId() = ID
}
