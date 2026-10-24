package ic2_120.content.network

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.Ic2_120
import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.block.machines.ReplicatorBlockEntity
import ic2_120.content.screen.FluidUpgradeScreenHandler
import ic2_120.content.screen.ItemUpgradeScreenHandler
import ic2_120.content.screen.PumpAttachmentScreenHandler
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.MiningLaserItem
import ic2_120.content.item.MiningLaserServerSuppress
import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumHelmet
import ic2_120.content.item.armor.QuantumLeggings
import ic2_120.content.item.armor.QuantumBoots
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluid

/**
 * C2S：设置流体过滤（GUI 潜行点击过滤槽时由客户端发送）
 */
class SetFluidFilterPayload(val fluid: Fluid) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<SetFluidFilterPayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<SetFluidFilterPayload>(Ic2_120.id("set_fluid_filter"))
        val CODEC: StreamCodec<FriendlyByteBuf, SetFluidFilterPayload> = StreamCodec.of(
            { buf, value -> buf.writeResourceLocation(BuiltInRegistries.FLUID.getKey(value.fluid)) },
            { buf -> SetFluidFilterPayload(BuiltInRegistries.FLUID.get(buf.readResourceLocation())) }
        )
    }
}

/**
 * C2S：设置物品过滤（GUI 过滤槽）
 */
class SetItemFilterPayload(val slotIndex: Int, val stack: ItemStack) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<SetItemFilterPayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<SetItemFilterPayload>(Ic2_120.id("set_item_filter"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SetItemFilterPayload> = StreamCodec.of(
            { buf, value ->
                buf.writeVarInt(value.slotIndex)
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, value.stack)
            },
            { buf ->
                val slotIndex = buf.readVarInt()
                val stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
                SetItemFilterPayload(slotIndex, stack)
            }
        )
    }
}

/**
 * C2S：量子护腿速度档 tick 心跳
 */
object QuantumLeggingsSpeedTickPayload : CustomPacketPayload {
    val ID = CustomPacketPayload.Type<QuantumLeggingsSpeedTickPayload>(Ic2_120.id("quantum_leggings_speed_tick"))
    val CODEC: StreamCodec<FriendlyByteBuf, QuantumLeggingsSpeedTickPayload> = StreamCodec.of(
        { _, _ -> },
        { QuantumLeggingsSpeedTickPayload }
    )

    override fun type() = ID
}

/**
 * S2C 包装：把尚未迁移为 CustomPacketPayload 的旧包对象桥接为 payload 发送。
 */
class SemifluidGeneratorFuelStatePacketPayload(val packet: SemifluidGeneratorFuelStatePacket) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<SemifluidGeneratorFuelStatePacketPayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<SemifluidGeneratorFuelStatePacketPayload>(SemifluidGeneratorFuelStatePacket.ID)
        val CODEC: StreamCodec<FriendlyByteBuf, SemifluidGeneratorFuelStatePacketPayload> = StreamCodec.of(
            { buf, value -> SemifluidGeneratorFuelStatePacket.write(value.packet, buf) },
            { buf -> SemifluidGeneratorFuelStatePacketPayload(SemifluidGeneratorFuelStatePacket.read(buf)) }
        )
    }
}

class ReactorLayoutLockPacketPayload(val packet: ReactorLayoutLockPacket) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<ReactorLayoutLockPacketPayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<ReactorLayoutLockPacketPayload>(ReactorLayoutLockPacket.ID)
        val CODEC: StreamCodec<FriendlyByteBuf, ReactorLayoutLockPacketPayload> = StreamCodec.of(
            { buf, value -> ReactorLayoutLockPacket.write(value.packet, buf) },
            { buf -> ReactorLayoutLockPacketPayload(ReactorLayoutLockPacket.read(buf)) }
        )
    }
}

object NetworkManager {
    // 旧 ResourceLocation 常量保留给客户端兼容引用（实际收发走对应 payload type）
    val REACTOR_HEAT_INFO_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "reactor_heat_info")
    val WIND_ROTOR_STATE_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "wind_rotor_state")
    val WATER_ROTOR_STATE_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "water_rotor_state")
    val REACTOR_LAYOUT_LOCK_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "reactor_layout_lock")
    val TELEPORTER_VISUAL_STATE_PACKET = TeleporterVisualStatePacket.ID.id
    val TOGGLE_NIGHT_VISION_GOGGLES_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "toggle_night_vision_goggles")
    val TOGGLE_NANO_VISION_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "toggle_nano_vision")
    val TOGGLE_IRIDIUM_SILK_TOUCH_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "toggle_iridium_silk_touch")
    val TOGGLE_CHAINSAW_SHEAR_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "toggle_chainsaw_shear")
    val TOGGLE_FOAM_SPRAYER_MODE_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "toggle_foam_sprayer_mode")
    val TOGGLE_MINING_LASER_MODE_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "toggle_mining_laser_mode")
    val TOGGLE_QUANTUM_LEGGINGS_SPEED_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "toggle_quantum_leggings_speed")
    val QUANTUM_LEGGINGS_SPEED_TICK_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "quantum_leggings_speed_tick")
    val TOGGLE_QUANTUM_BOOTS_JUMP_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "toggle_quantum_boots_jump")
    val SELECT_TEMPLATE_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "select_template")
    val SET_FLUID_FILTER_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "set_fluid_filter")
    val SET_ITEM_FILTER_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "set_item_filter")

    fun register() {
        // ---- payload 类型注册（fabric 1.21.1：收发前必须注册 codec）----
        val c2s = PayloadTypeRegistry.playC2S()
        c2s.register(ToggleNightVisionGogglesPayload.ID, ToggleNightVisionGogglesPayload.CODEC)
        c2s.register(ToggleNanoVisionPayload.ID, ToggleNanoVisionPayload.CODEC)
        c2s.register(ToggleIridiumSilkTouchPayload.ID, ToggleIridiumSilkTouchPayload.CODEC)
        c2s.register(ToggleFoamSprayerModePayload.ID, ToggleFoamSprayerModePayload.CODEC)
        c2s.register(ToggleMiningLaserModePayload.ID, ToggleMiningLaserModePayload.CODEC)
        c2s.register(ToggleQuantumLeggingsSpeedPayload.ID, ToggleQuantumLeggingsSpeedPayload.CODEC)
        c2s.register(ToggleQuantumBootsJumpPayload.ID, ToggleQuantumBootsJumpPayload.CODEC)
        c2s.register(QuantumLeggingsSpeedTickPayload.ID, QuantumLeggingsSpeedTickPayload.CODEC)
        c2s.register(SelectTemplatePayload.ID, SelectTemplatePayload.CODEC)
        c2s.register(SetFluidFilterPayload.ID, SetFluidFilterPayload.CODEC)
        c2s.register(SetItemFilterPayload.ID, SetItemFilterPayload.CODEC)

        val s2c = PayloadTypeRegistry.playS2C()
        s2c.register(ReactorHeatInfoPacket.ID, ReactorHeatInfoPacket.CODEC)
        s2c.register(WindRotorStatePacket.ID, WindRotorStatePacket.CODEC)
        s2c.register(WaterRotorStatePacket.ID, WaterRotorStatePacket.CODEC)
        s2c.register(TeleporterVisualStatePacket.ID, TeleporterVisualStatePacket.CODEC)
        s2c.register(ReactorLayoutLockPacketPayload.ID, ReactorLayoutLockPacketPayload.CODEC)
        s2c.register(SemifluidGeneratorFuelStatePacketPayload.ID, SemifluidGeneratorFuelStatePacketPayload.CODEC)

        // TODO(1.21.1): chainsaw shear 切换暂无独立 payload，待客户端迁移后注册接收器。

        // ---- C2S 接收器 ----
        ServerPlayNetworking.registerGlobalReceiver(ToggleNightVisionGogglesPayload.ID) { _, context ->
            val player = context.player()
            context.server().execute {
                val stack = player.getItemBySlot(EquipmentSlot.HEAD)
                val goggles = stack.item as? NightVisionGoggles ?: return@execute
                val enabled = NightVisionGoggles.toggleEnabled(stack)
                val key = if (enabled) "message.ic2_120.night_vision_goggles.enabled" else "message.ic2_120.night_vision_goggles.disabled"
                player.displayClientMessage(Component.translatable(key), true)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleNanoVisionPayload.ID) { _, context ->
            val player = context.player()
            context.server().execute {
                val stack = player.getItemBySlot(EquipmentSlot.HEAD)
                when (val item = stack.item) {
                    is NanoHelmet -> {
                        val enabled = NanoHelmet.toggleNightVision(stack)
                        player.displayClientMessage(Component.translatable(
                            if (enabled) "message.ic2_120.nano_helmet.nv_on" else "message.ic2_120.nano_helmet.nv_off"
                        ), true)
                    }
                    is QuantumHelmet -> {
                        val enabled = QuantumHelmet.toggleNightVision(stack)
                        player.displayClientMessage(Component.translatable(
                            if (enabled) "message.ic2_120.quantum_helmet.nv_on" else "message.ic2_120.quantum_helmet.nv_off"
                        ), true)
                    }
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleQuantumLeggingsSpeedPayload.ID) { _, context ->
            val player = context.player()
            context.server().execute {
                val stack = player.getItemBySlot(EquipmentSlot.LEGS)
                if (stack.item is QuantumLeggings) {
                    val tier = QuantumLeggings.cycleSpeedTier(stack)
                    player.displayClientMessage(Component.translatable(
                        when (tier) {
                            1 -> "message.ic2_120.quantum_leggings.speed_tier1"
                            2 -> "message.ic2_120.quantum_leggings.speed_tier2"
                            else -> "message.ic2_120.quantum_leggings.speed_off"
                        }
                    ), true)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(QuantumLeggingsSpeedTickPayload.ID) { _, context ->
            val player = context.player()
            context.server().execute {
                if (player.isPassenger || player.abilities.flying || player.isSpectator) return@execute
                if (!player.onGround() && !player.isInWater) return@execute

                val stack = player.getItemBySlot(EquipmentSlot.LEGS)
                if (stack.item is QuantumLeggings) {
                    QuantumLeggings.consumeSpeedEnergyTick(stack)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleQuantumBootsJumpPayload.ID) { _, context ->
            val player = context.player()
            context.server().execute {
                val stack = player.getItemBySlot(EquipmentSlot.FEET)
                if (stack.item is QuantumBoots) {
                    val enabled = QuantumBoots.toggleSuperJump(stack)
                    player.displayClientMessage(Component.translatable(
                        if (enabled) "message.ic2_120.quantum_boots.jump_on" else "message.ic2_120.quantum_boots.jump_off"
                    ), true)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleIridiumSilkTouchPayload.ID) { _, context ->
            val player = context.player()
            context.server().execute {
                val stack = player.mainHandItem
                if (stack.item is IridiumDrill) {
                    val enabled = IridiumDrill.toggleSilkTouch(stack)
                    player.displayClientMessage(Component.translatable(
                        if (enabled) "message.ic2_120.iridium_drill.silk_touch_on" else "message.ic2_120.iridium_drill.silk_touch_off"
                    ), true)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleFoamSprayerModePayload.ID) { _, context ->
            val player = context.player()
            context.server().execute {
                for (hand in arrayOf(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND)) {
                    val stack = player.getItemInHand(player.usedItemHand)
                    if (stack.item is FoamSprayerItem) {
                        val multi = FoamSprayerItem.toggleMultiMode(stack)
                        player.setItemInHand(hand, stack)
                        player.displayClientMessage(
                            Component.translatable(
                                if (multi) "message.ic2_120.foam_sprayer.mode_multi" else "message.ic2_120.foam_sprayer.mode_single"
                            ),
                            true
                        )
                        return@execute
                    }
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleMiningLaserModePayload.ID) { _, context ->
            val player = context.player()
            context.server().execute {
                for (hand in arrayOf(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND)) {
                    val stack = player.getItemInHand(player.usedItemHand)
                    if (stack.item is MiningLaserItem) {
                        MiningLaserServerSuppress.onMiningLaserModeToggled(player)
                        val newMode = MiningLaserItem.cycleMode(stack)
                        player.setItemInHand(hand, stack)
                        player.displayClientMessage(
                            Component.translatable(newMode.translationKey),
                            true
                        )
                        return@execute
                    }
                }
            }
        }

        // 模板选择 C2S 包：绕过 ServerboundContainerButtonClickPacket 的 buttonId byte 限制
        ServerPlayNetworking.registerGlobalReceiver(SelectTemplatePayload.ID) { payload, context ->
            val player = context.player()
            context.server().execute {
                val be = player.level().getBlockEntity(payload.pos)
                when (be) {
                    is ReplicatorBlockEntity -> be.selectTemplate(payload.index)
                    is PatternStorageBlockEntity -> be.selectTemplate(payload.index)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(SetFluidFilterPayload.ID) { payload, context ->
            val player = context.player()
            context.server().execute {
                val fluid = payload.fluid
                when (val handler = player.containerMenu) {
                    is FluidUpgradeScreenHandler -> handler.setFluidFilter(fluid)
                    is PumpAttachmentScreenHandler -> handler.setFluidFilter(fluid)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(SetItemFilterPayload.ID) { payload, context ->
            val player = context.player()
            context.server().execute {
                val handler = player.containerMenu
                if (handler is ItemUpgradeScreenHandler) handler.setItemFilter(payload.slotIndex, payload.stack)
            }
        }
    }

    // 发送数据包到客户端
    fun sendToClient(player: ServerPlayer, packet: ReactorHeatInfoPacket) {
        ServerPlayNetworking.send(player, packet)
    }

    fun sendToClient(player: ServerPlayer, packet: ReactorLayoutLockPacket) {
        ServerPlayNetworking.send(player, ReactorLayoutLockPacketPayload(packet))
    }

    // 发送风力发电机转子状态到附近玩家
    fun sendWindRotorStateToNearby(world: Level, pos: BlockPos, isStuck: Boolean, stuckAngle: Float) {
        if (world.isClientSide) return

        val serverWorld = world as ServerLevel
        val packet = WindRotorStatePacket(pos, isStuck, stuckAngle)
        for (player in serverWorld.players()) {
            // 检查玩家是否在能看到这个方块的范围内（64格内）
            if (player.distanceToSqr(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                ServerPlayNetworking.send(player, packet)
            }
        }
    }

    fun sendTeleporterVisualStateToNearby(
        world: Level,
        pos: BlockPos,
        be: ic2_120.content.block.machines.TeleporterBlockEntity
    ) {
        if (world.isClientSide) return
        val serverWorld = world as ServerLevel
        val packet = TeleporterVisualStatePacket(
            pos = pos,
            charging = be.sync.charging != 0,
            chargeProgress = be.sync.chargeProgress.coerceAtLeast(0),
            chargeMax = be.sync.chargeMax.coerceAtLeast(0),
            teleportRange = be.getTeleportRange(),
            chargingEntityId = be.getChargingEntityId()
        )
        for (player in serverWorld.players()) {
            if (player.distanceToSqr(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                ServerPlayNetworking.send(player, packet)
            }
        }
    }

    // 发送水力发电机转子状态到附近玩家
    fun sendWaterRotorStateToNearby(world: Level, pos: BlockPos, isStuck: Boolean, stuckAngle: Float) {
        if (world.isClientSide) return

        val serverWorld = world as ServerLevel
        val packet = WaterRotorStatePacket(pos, isStuck, stuckAngle)
        for (player in serverWorld.players()) {
            if (player.distanceToSqr(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                ServerPlayNetworking.send(player, packet)
            }
        }
    }

    fun sendSemifluidGeneratorFuelState(world: ServerLevel, pos: BlockPos, fuelColorArgb: Int) {
        val packet = SemifluidGeneratorFuelStatePacketPayload(SemifluidGeneratorFuelStatePacket(pos, fuelColorArgb))
        for (player in world.players()) {
            if (player.distanceToSqr(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                ServerPlayNetworking.send(player, packet)
            }
        }
    }
}
