package ic2_120.content.screen

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

import ic2_120.content.item.AdvancedScannerItem
import ic2_120.content.item.OdScannerItem
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
    private val scannerUseSound: SoundEvent = SoundEvent.of(Identifier.of("ic2", "item.scanner.use"))

    init {
        addProperties(syncedData)

        // 不添加背包槽位，仅显示扫描信息

        // 从手持物品初始化数据（从 NBT 读取）
        val stack = playerInventory.getStack(playerInventory.selectedSlot)
        val type = OdScannerItem.getScannerType(stack)
        val energy = IElectricTool.getEnergy(stack).toInt().coerceIn(0, Int.MAX_VALUE)
        val uses = OdScannerItem.getUsesRemaining(stack)
        sync.init(energy, type.energyCapacity.toInt().coerceIn(0, Int.MAX_VALUE), uses, type.maxUses)
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
        if (id != BUTTON_ID_SCAN) return false

        val stack = player.mainHandStack
        if (stack.item !is OdScannerItem && stack.item !is AdvancedScannerItem) return false
        val type = OdScannerItem.getScannerType(stack)

        // 扣能量
        if (!sync.consumeEnergy(type.energyPerScan)) {
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
        val results = performScan(player, type.scanRadius)
        player.world.playSound(
            null,
            player.blockPos,
            scannerUseSound,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f
        )

        // 发送结果 S2C 包
        val packet = ScannerResultPacket(
            energy = sync.energy,
            energyCapacity = sync.energyCapacity,
            usesRemaining = sync.usesRemaining,
            maxUses = sync.maxUses,
            results = results
        )
        (player as? net.minecraft.server.network.ServerPlayerEntity)?.let {
            ServerPlayNetworking.send(it, packet)
        }

        return true
    }

    private fun performScan(player: PlayerEntity, radius: Int): List<OreScanEntry> {
        val world = player.world
        if (world.isClient) return emptyList()

        val center = player.blockPos
        val oreCounts = mutableMapOf<Identifier, Int>()

        val minY = world.bottomY
        val topY = world.topY

        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                for (y in minY until topY) {
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
