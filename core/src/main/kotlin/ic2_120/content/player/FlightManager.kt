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
            return
        }

        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        val source = flightSource(chest)
        // 燃料/能量是否充足？边界：刚从「有」变「无」时这一 tick 不能「先开再关」，
        // 否则会发两次 abilities 包让客户端闪一下，所以先做预检再决定授权。
        if (source == null || !source.hasEnergy(chest)) {
            restorePreviousFlightState(player)
            return
        }

        grantFlightPermission(player)

        // 真正在飞的时候才消耗燃料/能量。耗尽时由上面的 !hasEnergy 分支在下一 tick 收回飞行权限。
        if (player.abilities.flying) {
            source.consume(chest)
        }
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
