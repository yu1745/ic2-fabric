package ic2_120.content.screen

import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

import ic2_120.content.item.AdvancedScannerItem
import ic2_120.content.item.OdScannerItem
import ic2_120.content.item.ScannerType
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.network.OreScanEntry
import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.sync.ScannerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(name = "scanner")
class ScannerScreenHandler(
    syncId: Int,
    val playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext
) : ScreenHandler(ScannerScreenHandler::class.type(), syncId) {

    private val syncedData = SyncedData()
    val sync = ScannerSync(syncedData)
    private val scannerUseSound: SoundEvent = SoundEvent.of(Identifier("ic2", "item.scanner.use"))

    init {
        addProperties(syncedData)

        val stack = playerInventory.getStack(playerInventory.selectedSlot)
        val type = OdScannerItem.getScannerType(stack)
        val energy = IElectricTool.getEnergy(stack).toInt().coerceIn(0, Int.MAX_VALUE)
        val uses = OdScannerItem.getUsesRemaining(stack)
        sync.init(energy, type.energyCapacity.toInt().coerceIn(0, Int.MAX_VALUE), uses, type.maxUses)

        // 背包槽位 3×9
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, 9 + row * 9 + col, 0, 0))
            }
        }
        // 快捷栏槽位 1×9
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    fun getScannerStack(): ItemStack {
        return checkNotNull(getSliderStack()) { "Scanner not in hand" }
    }

    private fun getSliderStack(): ItemStack? {
        val stack = playerInventory.getStack(playerInventory.selectedSlot)
        return stack.takeIf { it.item is OdScannerItem || it.item is AdvancedScannerItem }
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        val stack = player.mainHandStack
        if (stack.item !is OdScannerItem && stack.item !is AdvancedScannerItem) return false

        if (id != BUTTON_ID_SCAN) return false

        val type = OdScannerItem.getScannerType(stack)
        val rangeY = maxOf(1, player.blockPos.y - type.deepY)
        val energyCost = computeEnergyCost(type, rangeY)

        if (!sync.consumeEnergy(energyCost)) {
            player.sendMessage(
                net.minecraft.text.Text.translatable("message.ic2_120.scanner.no_energy")
                    .formatted(net.minecraft.util.Formatting.RED),
                false
            )
            return true
        }

        if (!sync.consumeUse()) {
            player.sendMessage(
                net.minecraft.text.Text.translatable("message.ic2_120.scanner.no_uses")
                    .formatted(net.minecraft.util.Formatting.RED),
                false
            )
            return true
        }

        (stack.item as? IElectricTool)?.setEnergy(stack, sync.energy.toLong())
        OdScannerItem.setUsesRemaining(stack, sync.usesRemaining)

        val results = performScan(player)

        player.world.playSound(
            null,
            player.blockPos,
            scannerUseSound,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f
        )

        val buf = PacketByteBuf(Unpooled.buffer())
        ScannerResultPacket.write(
            ScannerResultPacket(
                energy = sync.energy,
                energyCapacity = sync.energyCapacity,
                usesRemaining = sync.usesRemaining,
                maxUses = sync.maxUses,
                results = results
            ),
            buf
        )
        (player as? net.minecraft.server.network.ServerPlayerEntity)?.let {
            ServerPlayNetworking.send(it, ScannerResultPacket.ID, buf)
        }

        return true
    }

    private fun performScan(player: PlayerEntity): List<OreScanEntry> {
        val world = player.world
        if (world.isClient) return emptyList()

        val center = player.blockPos
        val oreCounts = mutableMapOf<Identifier, Int>()

        val stack = player.mainHandStack
        val type = OdScannerItem.getScannerType(stack)
        val radius = type.scanRadius
        val minY = maxOf(world.bottomY, type.deepY)
        val maxY = center.y

        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                for (y in minY..maxY) {
                    val pos = center.add(dx, 0, dz).withY(y)
                    val state = world.getBlockState(pos)
                    if (state.isAir) continue
                    val blockId = Registries.BLOCK.getId(state.block)
                    oreCounts[blockId] = oreCounts.getOrDefault(blockId, 0) + 1
                }
            }
        }

        return oreCounts
            .filter { (id, _) ->
                id.path.contains("ore") || id.path in SCANNABLE_ORES
            }
            .map { (id, count) -> OreScanEntry(id.toString(), count) }
            .sortedByDescending { it.count }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        val slot = slots[index]
        if (!slot.hasStack()) return ItemStack.EMPTY
        val stack = slot.stack
        val original = stack.copy()
        val invEnd = PLAYER_INV_START + 36

        return if (index < PLAYER_INV_START + 27) {
            // 背包 → 快捷栏
            if (!insertItem(stack, PLAYER_INV_START + 27, invEnd, false)) ItemStack.EMPTY
            else {
                slot.onQuickTransfer(stack, original)
                if (stack.isEmpty) slot.stack = ItemStack.EMPTY
                else slot.markDirty()
                original
            }
        } else {
            // 快捷栏 → 背包
            if (!insertItem(stack, PLAYER_INV_START, PLAYER_INV_START + 27, false)) ItemStack.EMPTY
            else {
                slot.onQuickTransfer(stack, original)
                if (stack.isEmpty) slot.stack = ItemStack.EMPTY
                else slot.markDirty()
                original
            }
        }
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    companion object {
        const val PLAYER_INV_START = 0
        const val BUTTON_ID_SCAN = 0

        /**
         * 根据实际扫描体积计算能量消耗。
         *
         * X/Z 范围恒为 [ScannerType.scanRadius]，仅 Y 维度随玩家高度（[rangeY]）变化。
         * - 玩家位于地表时，实际体积 v ≥ 基准立方体 v0，倍数为 1.0（即 [ScannerType.energyPerScan]）；
         * - 玩家接近基岩层（y 小）时实际体积缩小，倍数上升（最高 10×），惩罚浅层扫描。
         */
        @JvmStatic
        fun computeEnergyCost(type: ScannerType, rangeY: Int): Int {
            val side = type.scanRadius * 2 + 1
            // X/Z 固定为 scanRadius，实际体积 = side² × (rangeY*2+1)
            val v = (side * side).toDouble() * (rangeY * 2 + 1)
            val v0 = (side * side * side).toDouble()
            val multiplier = if (v >= v0) 1.0 else 10.0 - 9.0 * (v / v0)
            return (type.energyPerScan * multiplier).toInt().coerceAtLeast(1)
        }

        private val SCANNABLE_ORES = setOf(
            "coal_ore", "iron_ore", "copper_ore", "gold_ore",
            "lapis_ore", "redstone_ore", "diamond_ore", "emerald_ore",
            "nether_gold_ore", "nether_quartz_ore",
            "deepslate_coal_ore", "deepslate_iron_ore", "deepslate_copper_ore",
            "deepslate_gold_ore", "deepslate_lapis_ore", "deepslate_redstone_ore",
            "deepslate_diamond_ore", "deepslate_emerald_ore",
            "ancient_debris", "netherite_block"
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ScannerScreenHandler {
            val energy = buf.readInt()
            val energyCapacity = buf.readInt()
            val usesRemaining = buf.readVarInt()
            val maxUses = buf.readVarInt()

            val context = ScreenHandlerContext.create(playerInventory.player.world, net.minecraft.util.math.BlockPos.ORIGIN)

            val handler = ScannerScreenHandler(syncId, playerInventory, context)
            handler.sync.init(energy, energyCapacity, usesRemaining, maxUses)
            return handler
        }
    }
}
