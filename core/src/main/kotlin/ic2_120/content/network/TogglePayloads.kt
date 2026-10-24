package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

object ToggleNightVisionGogglesPayload : CustomPacketPayload {
    val ID = CustomPacketPayload.Id<ToggleNightVisionGogglesPayload>(Ic2_120.id("toggle_night_vision_goggles"))
    val CODEC: StreamCodec<FriendlyByteBuf, ToggleNightVisionGogglesPayload> = StreamCodec.of(
        { _, _ -> },
        { ToggleNightVisionGogglesPayload }
    )
    override fun getId() = ID
}

object ToggleNanoVisionPayload : CustomPacketPayload {
    val ID = CustomPacketPayload.Id<ToggleNanoVisionPayload>(Ic2_120.id("toggle_nano_vision"))
    val CODEC: StreamCodec<FriendlyByteBuf, ToggleNanoVisionPayload> = StreamCodec.of(
        { _, _ -> },
        { ToggleNanoVisionPayload }
    )
    override fun getId() = ID
}

object ToggleIridiumSilkTouchPayload : CustomPacketPayload {
    val ID = CustomPacketPayload.Id<ToggleIridiumSilkTouchPayload>(Ic2_120.id("toggle_iridium_silk_touch"))
    val CODEC: StreamCodec<FriendlyByteBuf, ToggleIridiumSilkTouchPayload> = StreamCodec.of(
        { _, _ -> },
        { ToggleIridiumSilkTouchPayload }
    )
    override fun getId() = ID
}

object ToggleFoamSprayerModePayload : CustomPacketPayload {
    val ID = CustomPacketPayload.Id<ToggleFoamSprayerModePayload>(Ic2_120.id("toggle_foam_sprayer_mode"))
    val CODEC: StreamCodec<FriendlyByteBuf, ToggleFoamSprayerModePayload> = StreamCodec.of(
        { _, _ -> },
        { ToggleFoamSprayerModePayload }
    )
    override fun getId() = ID
}

object ToggleMiningLaserModePayload : CustomPacketPayload {
    val ID = CustomPacketPayload.Id<ToggleMiningLaserModePayload>(Ic2_120.id("toggle_mining_laser_mode"))
    val CODEC: StreamCodec<FriendlyByteBuf, ToggleMiningLaserModePayload> = StreamCodec.of(
        { _, _ -> },
        { ToggleMiningLaserModePayload }
    )
    override fun getId() = ID
}

object ToggleQuantumLeggingsSpeedPayload : CustomPacketPayload {
    val ID = CustomPacketPayload.Id<ToggleQuantumLeggingsSpeedPayload>(Ic2_120.id("toggle_quantum_leggings_speed"))
    val CODEC: StreamCodec<FriendlyByteBuf, ToggleQuantumLeggingsSpeedPayload> = StreamCodec.of(
        { _, _ -> },
        { ToggleQuantumLeggingsSpeedPayload }
    )
    override fun getId() = ID
}

object ToggleQuantumBootsJumpPayload : CustomPacketPayload {
    val ID = CustomPacketPayload.Id<ToggleQuantumBootsJumpPayload>(Ic2_120.id("toggle_quantum_boots_jump"))
    val CODEC: StreamCodec<FriendlyByteBuf, ToggleQuantumBootsJumpPayload> = StreamCodec.of(
        { _, _ -> },
        { ToggleQuantumBootsJumpPayload }
    )
    override fun getId() = ID
}
