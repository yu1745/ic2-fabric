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

/**
 * 扫描仪 GUI 的服务端 ScreenHandler。
 * 数据来源：直接从手持物品的 NBT 读取（playerInventory.selectedSlot）。
 */
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

        // 不添加背包槽位，仅显示扫描信息

        // 从手持物品初始化数据（从 NBT 读取）
        val stack = playerInventory.getStack(playerInventory.selectedSlot)
        val type = OdScannerItem.getScannerType(stack)
        val energy = IElectricTool.getEnergy(stack).toInt().coerceIn(0, Int.MAX_VALUE)
        val uses = OdScannerItem.getUsesRemaining(stack)
        sync.init(energy, type.energyCapacity.toInt().coerceIn(0, Int.MAX_VALUE), uses, type.maxUses)
        sync.initRanges(type.scanRadius)
    }

    /** 获取当前手持的扫描仪物品 */
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
        val type = OdScannerItem.getScannerType(stack)

        // 范围控制按钮（快速响应，不消耗能量）
        when (id) {
            BUTTON_RANGE_X_DEC -> { sync.rangeX = maxOf(1, sync.rangeX - 1); return true }
            BUTTON_RANGE_X_INC -> { sync.rangeX = minOf(type.scanRadius, sync.rangeX + 1); return true }
            BUTTON_RANGE_Y_DEC -> { sync.rangeY = maxOf(1, sync.rangeY - 1); return true }
            BUTTON_RANGE_Y_INC -> { sync.rangeY += 1; return true }
            BUTTON_RANGE_Y_DEC_10 -> { sync.rangeY = maxOf(1, sync.rangeY - 10); return true }
            BUTTON_RANGE_Y_INC_10 -> { sync.rangeY += 10; return true }
            BUTTON_RANGE_Z_DEC -> { sync.rangeZ = maxOf(1, sync.rangeZ - 1); return true }
            BUTTON_RANGE_Z_INC -> { sync.rangeZ = minOf(type.scanRadius, sync.rangeZ + 1); return true }
        }

        if (id != BUTTON_ID_SCAN) return false

        // 根据扫描盒子体积动态计算能量消耗
        val energyCost = computeEnergyCost(type, sync.rangeX, sync.rangeY, sync.rangeZ)

        // 扣能量
        if (!sync.consumeEnergy(energyCost)) {
            player.sendMessage(
                net.minecraft.text.Text.translatable("message.ic2_120.scanner.no_energy")
                    .formatted(net.minecraft.util.Formatting.RED),
                false
            )
            return true
        }

        // 扣使用次数
        if (!sync.consumeUse()) {
            player.sendMessage(
                net.minecraft.text.Text.translatable("message.ic2_120.scanner.no_uses")
                    .formatted(net.minecraft.util.Formatting.RED),
                false
            )
            return true
        }

        // 更新物品 NBT
        (stack.item as? IElectricTool)?.setEnergy(stack, sync.energy.toLong())
        OdScannerItem.setUsesRemaining(stack, sync.usesRemaining)

        // 执行扫描
        val results = performScan(player)
        player.world.playSound(
            null,
            player.blockPos,
            scannerUseSound,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f
        )

        // 发送结果 S2C 包
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

        val rangeX = sync.rangeX.coerceAtLeast(1)
        val rangeZ = sync.rangeZ.coerceAtLeast(1)
        val rangeY = sync.rangeY.coerceAtLeast(1)

        val minY = (center.y - rangeY).coerceAtLeast(world.bottomY)
        val maxY = (center.y + rangeY).coerceAtMost(world.topY - 1)

        for (dx in -rangeX..rangeX) {
            for (dz in -rangeZ..rangeZ) {
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

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY

    override fun canUse(player: PlayerEntity): Boolean = true

    companion object {
        const val BUTTON_ID_SCAN = 0

        /**
         * 根据扫描盒子体积计算实际能量消耗。
         * - V >= V0（默认体积）→ 1 倍消耗
         * - V < V0 → 线性插值，最大 10 倍（1×1×1 时 10 倍消耗）
         */
        @JvmStatic
        fun computeEnergyCost(type: ScannerType, rangeX: Int, rangeY: Int, rangeZ: Int): Int {
            val v = (rangeX * 2 + 1).toDouble() * (rangeY * 2 + 1) * (rangeZ * 2 + 1)
            val side = type.scanRadius * 2 + 1
            val v0 = (side * side * side).toDouble()
            val multiplier = if (v >= v0) 1.0 else 10.0 - 9.0 * (v / v0)
            return (type.energyPerScan * multiplier).toInt().coerceAtLeast(1)
        }
        const val BUTTON_RANGE_X_DEC = 1
        const val BUTTON_RANGE_X_INC = 2
        const val BUTTON_RANGE_Y_DEC = 3
        const val BUTTON_RANGE_Y_INC = 4
        const val BUTTON_RANGE_Y_DEC_10 = 5
        const val BUTTON_RANGE_Y_INC_10 = 6
        const val BUTTON_RANGE_Z_DEC = 7
        const val BUTTON_RANGE_Z_INC = 8

        const val PANEL_WIDTH = 256
        const val PANEL_HEIGHT = 256

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
            // 读取初始数据（服务端写入，服务端 also reads）
            val energy = buf.readInt()
            val energyCapacity = buf.readInt()
            val usesRemaining = buf.readVarInt()
            val maxUses = buf.readVarInt()

            val context = ScreenHandlerContext.create(playerInventory.player.world, net.minecraft.util.math.BlockPos.ORIGIN)

            val handler = ScannerScreenHandler(syncId, playerInventory, context)
            // 直接设置初始值（服务端已设置）
            handler.sync.init(energy, energyCapacity, usesRemaining, maxUses)
            return handler
        }
    }
}
