package ic2_120.content.player

import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.item.energy.IElectricTool
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import java.util.UUID

/**
 * 喷气背包/电力喷气背包/量子胸甲的飞行管理（服务端 tick）。
 *
 * 设计上完全复用 Minecraft 的创造模式飞行代码：
 *
 * - 服务端只负责根据玩家装备的胸甲是否可飞行，授予/剥夺 [net.minecraft.entity.player.PlayerAbilities.allowFlying]。
 * - 玩家按下/松开空格、Shift 时的起降/悬停、双击空格切换飞行等行为，全部由
 *   [net.minecraft.client.network.ClientPlayerEntity.tickMovement] 内部的 `abilityResyncCountdown`
 *   双击检测与 [net.minecraft.entity.LivingEntity.travel] 内的飞行分支处理。
 * - 玩家通过双击空格或落地在客户端切换 `flying` 后，vanilla 客户端会发出
 *   [net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket]，服务器照单接受即可。
 *   本管理器不主动把 `flying` 拉到 `true`，从而保留原版创造飞行的起停行为。
 *
 * 因此本类没有任何手写的「按键按下/抬起」检测或额外状态机：玩家穿上一件可飞行的胸甲，就相当于
 * 进入了「带飞行权限的生存模式」，与创造模式完全一致——燃料还够就等效创造，燃料耗尽立刻变回生存。
 */
object FlightManager {
    private val abilitySnapshots = mutableMapOf<UUID, FlightAbilitySnapshot>()

    /**
     * 维度切换后客户端的 ClientPlayerEntity 会被销毁重建，新实例的 allowFlying 会被 vanilla 的
     * GameMode.setAbilities(SURVIVAL) 抹成 false（见 onPlayerRespawn 里的 copyAbilities + setGameModes）。
     * 此处登记需要在下次 tick 强制重发一次 abilities 包的玩家，使客户端 allowFlying 重新与服务端对齐。
     * 放到 tick 里发（而非维度切换事件回调里直接发）是为了保证它是维度切换流程中「最后一个」abilities 包，
     * 不被后续的 setAbilities 覆盖。
     */
    private val pendingResync = java.util.concurrent.ConcurrentHashMap.newKeySet<UUID>()

    fun tick(server: MinecraftServer) {
        for (world in server.worlds) {
            for (player in world.players) {
                tickPlayer(player)
            }
        }
    }

    private fun tickPlayer(player: PlayerEntity) {
        // 创造/旁观模式的飞行由游戏模式自己处理，不要插手
        if (player.isCreative || player.isSpectator) {
            abilitySnapshots.remove(player.uuid)
            pendingResync.remove(player.uuid)
            return
        }

        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        val source = flightSource(chest)
        // 燃料/能量是否充足？边界：刚从「有」变「无」时这一 tick 不能「先开再关」，
        // 否则会发两次 abilities 包让客户端闪一下，所以先做预检再决定授权。
        if (source == null || !source.hasEnergy(chest)) {
            restorePreviousFlightState(player)
            pendingResync.remove(player.uuid)
            return
        }

        grantFlightPermission(player)

        // 真正在飞的时候才消耗燃料/能量。耗尽时由上面的 !hasEnergy 分支在下一 tick 收回飞行权限。
        if (player.abilities.flying) {
            source.consume(chest)
        }

        // 维度切换兜底：grantFlightPermission 在服务端 allowFlying 已为 true 时不会重发，
        // 这里强制补发一次，把客户端被 setAbilities(SURVIVAL) 抹掉的 allowFlying 同步回来。
        if (pendingResync.remove(player.uuid)) {
            player.sendAbilitiesUpdate()
        }
    }

    /**
     * 玩家维度切换后调用（由 ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD 触发）。
     * 这里只做登记，真正的 abilities 重发放到 [tickPlayer] 里执行，保证是维度切换流程中最后的包。
     */
    fun onWorldChanged(player: net.minecraft.server.network.ServerPlayerEntity) {
        pendingResync.add(player.uuid)
    }

    private fun grantFlightPermission(player: PlayerEntity) {
        abilitySnapshots.getOrPut(player.uuid) {
            FlightAbilitySnapshot(
                allowFlying = player.abilities.allowFlying,
                flying = player.abilities.flying
            )
        }

        if (player.abilities.allowFlying) return

        player.abilities.allowFlying = true
        player.sendAbilitiesUpdate()
    }

    private fun restorePreviousFlightState(player: PlayerEntity) {
        val snapshot = abilitySnapshots.remove(player.uuid) ?: return

        var changed = false
        if (player.abilities.flying != snapshot.flying) {
            player.abilities.flying = snapshot.flying
            changed = true
        }
        if (player.abilities.allowFlying != snapshot.allowFlying) {
            player.abilities.allowFlying = snapshot.allowFlying
            changed = true
        }
        if (changed) {
            player.sendAbilitiesUpdate()
        }
    }

    private data class FlightAbilitySnapshot(
        val allowFlying: Boolean,
        val flying: Boolean
    )

    private fun flightSource(stack: ItemStack): FlightSource? = when (val item = stack.item) {
        is JetpackItem -> JetpackSource
        is ElectricJetpack -> ElectricJetpackSource
        is QuantumChestplate -> QuantumSource
        else -> null
    }

    private interface FlightSource {
        /** 当前是否还有燃料/能量可供飞行。 */
        fun hasEnergy(stack: ItemStack): Boolean
        /** 消耗一次 tick 的飞行燃料/能量；调用前应确保 [hasEnergy] 为 true。 */
        fun consume(stack: ItemStack)
    }

    private object JetpackSource : FlightSource {
        override fun hasEnergy(stack: ItemStack): Boolean = JetpackItem.getFuel(stack) > 0
        override fun consume(stack: ItemStack) { JetpackItem.consumeFuelPerTick(stack) }
    }

    private object ElectricJetpackSource : FlightSource {
        override fun hasEnergy(stack: ItemStack): Boolean =
            (stack.item as ElectricJetpack).getEnergy(stack) > 0
        override fun consume(stack: ItemStack) {
            (stack.item as ElectricJetpack).consumeFlightEnergyPerTick(stack)
        }
    }

    private object QuantumSource : FlightSource {
        override fun hasEnergy(stack: ItemStack): Boolean =
            stack.orCreateNbt.getLong(IElectricTool.ENERGY_KEY) > 0
        override fun consume(stack: ItemStack) { QuantumChestplate.consumeFlightEnergyPerTick(stack) }
    }
}
