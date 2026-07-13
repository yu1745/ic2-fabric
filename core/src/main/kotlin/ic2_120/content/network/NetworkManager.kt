package ic2_120.content.network

import ic2_120.Ic2_120
import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.block.machines.ReplicatorBlockEntity
import ic2_120.content.screen.FluidUpgradeScreenHandler
import ic2_120.content.screen.PumpAttachmentScreenHandler
import net.minecraft.registry.Registries
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.Chainsaw
import ic2_120.content.item.MiningLaserItem
import ic2_120.content.item.MiningLaserServerSuppress
import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumHelmet
import ic2_120.content.item.armor.QuantumLeggings
import ic2_120.content.item.armor.QuantumBoots
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.entity.EquipmentSlot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.server.world.ServerWorld

object NetworkManager {
    private val REACTOR_HEAT_INFO_PACKET = Identifier(Ic2_120.MOD_ID, "reactor_heat_info")
    private val WIND_ROTOR_STATE_PACKET = Identifier(Ic2_120.MOD_ID, "wind_rotor_state")
    private val WATER_ROTOR_STATE_PACKET = Identifier(Ic2_120.MOD_ID, "water_rotor_state")
    private val REACTOR_LAYOUT_LOCK_PACKET = Identifier(Ic2_120.MOD_ID, "reactor_layout_lock")

    private val TELEPORTER_VISUAL_STATE_PACKET = TeleporterVisualStatePacket.ID
    val TOGGLE_NIGHT_VISION_GOGGLES_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_night_vision_goggles")
    val TOGGLE_NANO_VISION_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_nano_vision")
    val TOGGLE_IRIDIUM_SILK_TOUCH_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_iridium_silk_touch")
    val TOGGLE_CHAINSAW_SHEAR_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_chainsaw_shear")
    val TOGGLE_FOAM_SPRAYER_MODE_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_foam_sprayer_mode")
    val TOGGLE_MINING_LASER_MODE_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_mining_laser_mode")
    val TOGGLE_QUANTUM_LEGGINGS_SPEED_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_quantum_leggings_speed")
    val QUANTUM_LEGGINGS_SPEED_TICK_PACKET = Identifier(Ic2_120.MOD_ID, "quantum_leggings_speed_tick")
    val TOGGLE_QUANTUM_BOOTS_JUMP_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_quantum_boots_jump")
    val SELECT_TEMPLATE_PACKET = Identifier(Ic2_120.MOD_ID, "select_template")
    val SET_FLUID_FILTER_PACKET = Identifier(Ic2_120.MOD_ID, "set_fluid_filter")

    fun register() {
        // 注册服务端接收处理器（如果需要）
        ServerPlayNetworking.registerGlobalReceiver(REACTOR_HEAT_INFO_PACKET) { server, player, handler, buf, responseSender ->
            val packet = ReactorHeatInfoPacket.read(buf)
            server.execute {
                // 处理服务端接收到的数据包（如果需要）
            }
        }

        // 风力发电机转子状态包（仅 S2C，服务端不需要接收处理器）
        ServerPlayNetworking.registerGlobalReceiver(WIND_ROTOR_STATE_PACKET) { server, player, handler, buf, responseSender ->
            // 空实现，这个包只用于服务端发送到客户端
        }

        // 水力发电机转子状态包（仅 S2C，服务端不需要接收处理器）
        ServerPlayNetworking.registerGlobalReceiver(WATER_ROTOR_STATE_PACKET) { server, player, handler, buf, responseSender ->
            // 空实现
        }

        

        // 传送机渲染状态包（仅 S2C，服务端不需要接收处理器）
        ServerPlayNetworking.registerGlobalReceiver(TELEPORTER_VISUAL_STATE_PACKET) { _, _, _, _, _ -> }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_NIGHT_VISION_GOGGLES_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.getEquippedStack(EquipmentSlot.HEAD)
                val goggles = stack.item as? NightVisionGoggles ?: return@execute
                val enabled = NightVisionGoggles.toggleEnabled(stack)
                val key = if (enabled) "message.ic2_120.night_vision_goggles.enabled" else "message.ic2_120.night_vision_goggles.disabled"
                player.sendMessage(Text.translatable(key), true)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_NANO_VISION_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.getEquippedStack(EquipmentSlot.HEAD)
                when (val item = stack.item) {
                    is NanoHelmet -> {
                        val enabled = NanoHelmet.toggleNightVision(stack)
                        player.sendMessage(Text.translatable(
                            if (enabled) "message.ic2_120.nano_helmet.nv_on" else "message.ic2_120.nano_helmet.nv_off"
                        ), true)
                    }
                    is QuantumHelmet -> {
                        val enabled = QuantumHelmet.toggleNightVision(stack)
                        player.sendMessage(Text.translatable(
                            if (enabled) "message.ic2_120.quantum_helmet.nv_on" else "message.ic2_120.quantum_helmet.nv_off"
                        ), true)
                    }
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_QUANTUM_LEGGINGS_SPEED_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.getEquippedStack(EquipmentSlot.LEGS)
                if (stack.item is QuantumLeggings) {
                    val tier = QuantumLeggings.cycleSpeedTier(stack)
                    player.sendMessage(Text.translatable(
                        when (tier) {
                            1 -> "message.ic2_120.quantum_leggings.speed_tier1"
                            2 -> "message.ic2_120.quantum_leggings.speed_tier2"
                            else -> "message.ic2_120.quantum_leggings.speed_off"
                        }
                    ), true)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(QUANTUM_LEGGINGS_SPEED_TICK_PACKET) { server, player, _, _, _ ->
            server.execute {
                if (player.hasVehicle() || player.abilities.flying || player.isSpectator) return@execute
                if (!player.isOnGround && !player.isTouchingWater) return@execute

                val stack = player.getEquippedStack(EquipmentSlot.LEGS)
                if (stack.item is QuantumLeggings) {
                    QuantumLeggings.consumeSpeedEnergyTick(stack)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_QUANTUM_BOOTS_JUMP_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.getEquippedStack(EquipmentSlot.FEET)
                if (stack.item is QuantumBoots) {
                    val enabled = QuantumBoots.toggleSuperJump(stack)
                    player.sendMessage(Text.translatable(
                        if (enabled) "message.ic2_120.quantum_boots.jump_on" else "message.ic2_120.quantum_boots.jump_off"
                    ), true)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_IRIDIUM_SILK_TOUCH_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.mainHandStack
                if (stack.item is IridiumDrill) {
                    val enabled = IridiumDrill.toggleSilkTouch(stack)
                    player.sendMessage(Text.translatable(
                        if (enabled) "message.ic2_120.iridium_drill.silk_touch_on" else "message.ic2_120.iridium_drill.silk_touch_off"
                    ), true)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_CHAINSAW_SHEAR_PACKET) { server, player, _, _, _ ->
            server.execute {
                for (hand in arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND)) {
                    val stack = player.getStackInHand(hand)
                    if (stack.item is Chainsaw) {
                        val enabled = Chainsaw.toggleShear(stack)
                        player.setStackInHand(hand, stack)
                        player.sendMessage(
                            Text.literal(if (enabled) "链锯剪刀模式：开启" else "链锯剪刀模式：关闭"),
                            true
                        )
                        return@execute
                    }
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_FOAM_SPRAYER_MODE_PACKET) { server, player, _, _, _ ->
            server.execute {
                for (hand in arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND)) {
                    val stack = player.getStackInHand(hand)
                    if (stack.item is FoamSprayerItem) {
                        val multi = FoamSprayerItem.toggleMultiMode(stack)
                        player.setStackInHand(hand, stack)
                        player.sendMessage(
                            Text.translatable(
                                if (multi) "message.ic2_120.foam_sprayer.mode_multi" else "message.ic2_120.foam_sprayer.mode_single"
                            ),
                            true
                        )
                        return@execute
                    }
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_MINING_LASER_MODE_PACKET) { server, player, _, _, _ ->
            server.execute {
                for (hand in arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND)) {
                    val stack = player.getStackInHand(hand)
                    if (stack.item is MiningLaserItem) {
                        MiningLaserServerSuppress.onMiningLaserModeToggled(player)
                        val newMode = MiningLaserItem.cycleMode(stack)
                        player.setStackInHand(hand, stack)
                        player.sendMessage(
                            Text.translatable(newMode.translationKey),
                            true
                        )
                        return@execute
                    }
                }
            }
        }

        // 模板选择 C2S 包：绕过 ButtonClickC2SPacket 的 buttonId byte 限制
        ServerPlayNetworking.registerGlobalReceiver(SELECT_TEMPLATE_PACKET) { server, player, _, buf, _ ->
            val pos = buf.readBlockPos()
            val index = buf.readVarInt()
            server.execute {
                val be = player.world.getBlockEntity(pos)
                when (be) {
                    is ReplicatorBlockEntity -> be.selectTemplate(index)
                    is PatternStorageBlockEntity -> be.selectTemplate(index)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(SET_FLUID_FILTER_PACKET) { server, player, _, buf, _ ->
            val fluidId = buf.readIdentifier()
            server.execute {
                if (!Registries.FLUID.containsId(fluidId)) return@execute
                val fluid = Registries.FLUID.get(fluidId)
                when (val handler = player.currentScreenHandler) {
                    is FluidUpgradeScreenHandler -> handler.setFluidFilter(fluid)
                    is PumpAttachmentScreenHandler -> handler.setFluidFilter(fluid)
                }
            }
        }
    }

    // 发送数据包到客户端
    fun sendToClient(player: ServerPlayerEntity, packet: ReactorHeatInfoPacket) {
        val buf = PacketByteBuf(Unpooled.buffer())
        ReactorHeatInfoPacket.write(packet, buf)
        ServerPlayNetworking.send(player, REACTOR_HEAT_INFO_PACKET, buf)
    }

    fun sendToClient(player: ServerPlayerEntity, packet: ReactorLayoutLockPacket) {
        val buf = PacketByteBuf(Unpooled.buffer())
        ReactorLayoutLockPacket.write(packet, buf)
        ServerPlayNetworking.send(player, REACTOR_LAYOUT_LOCK_PACKET, buf)
    }

    // 发送风力发电机转子状态到附近玩家
    fun sendWindRotorStateToNearby(world: net.minecraft.world.World, pos: net.minecraft.util.math.BlockPos, isStuck: Boolean, stuckAngle: Float) {
        if (world.isClient) return

        val serverWorld = world as net.minecraft.server.world.ServerWorld
        for (player in serverWorld.players) {
            // 检查玩家是否在能看到这个方块的范围内（64格内）
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                val buf = PacketByteBuf(Unpooled.buffer())
                WindRotorStatePacket.write(WindRotorStatePacket(pos, isStuck, stuckAngle), buf)
                ServerPlayNetworking.send(player, WindRotorStatePacket.ID, buf)
            }
        }
    }

    fun sendTeleporterVisualStateToNearby(
        world: net.minecraft.world.World,
        pos: net.minecraft.util.math.BlockPos,
        be: ic2_120.content.block.machines.TeleporterBlockEntity
    ) {
        if (world.isClient) return
        val serverWorld = world as net.minecraft.server.world.ServerWorld
        val packet = TeleporterVisualStatePacket(
            pos = pos,
            charging = be.sync.charging != 0,
            chargeProgress = be.sync.chargeProgress.coerceAtLeast(0),
            chargeMax = be.sync.chargeMax.coerceAtLeast(0),
            teleportRange = be.getTeleportRange(),
            chargingEntityId = be.getChargingEntityId()
        )
        for (player in serverWorld.players) {
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                val buf = PacketByteBuf(Unpooled.buffer())
                TeleporterVisualStatePacket.write(packet, buf)
                ServerPlayNetworking.send(player, TELEPORTER_VISUAL_STATE_PACKET, buf)
            }
        }
    }

    // 发送水力发电机转子状态到附近玩家
    fun sendWaterRotorStateToNearby(world: net.minecraft.world.World, pos: net.minecraft.util.math.BlockPos, isStuck: Boolean, stuckAngle: Float) {
        if (world.isClient) return

        val serverWorld = world as net.minecraft.server.world.ServerWorld
        for (player in serverWorld.players) {
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                val buf = PacketByteBuf(Unpooled.buffer())
                WaterRotorStatePacket.write(WaterRotorStatePacket(pos, isStuck, stuckAngle), buf)
                ServerPlayNetworking.send(player, WaterRotorStatePacket.ID, buf)
            }
        }
    }

    fun sendSemifluidGeneratorFuelState(world: ServerWorld, pos: BlockPos, fuelColorArgb: Int) {
        val packet = SemifluidGeneratorFuelStatePacket(pos, fuelColorArgb)
        for (player in world.players) {
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                val buf = PacketByteBuf(Unpooled.buffer())
                SemifluidGeneratorFuelStatePacket.write(packet, buf)
                ServerPlayNetworking.send(player, SemifluidGeneratorFuelStatePacket.ID, buf)
            }
        }
    }

}
