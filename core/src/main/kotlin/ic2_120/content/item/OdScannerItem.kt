package ic2_120.content.item

import ic2_120.compat.isClient

import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.screen.ScannerScreenHandler
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.data.recipes.RecipeCategory
import io.netty.buffer.Unpooled
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import ic2_120.registry.annotation.RecipeProvider

/**
 * 扫描仪类型：OD（矿石密度）与 OV（矿石价值）。
 */
enum class ScannerType(
    val energyCapacity: Long,
    val energyPerScan: Int,
    val scanRadius: Int,
    val maxUses: Int,
    val tier: Int,
    val deepY: Int = 0
) {
    OD(100_000L, 50, 6, 20, 1, 0),
    OV(1_000_000L, 250, 12, 200, 2, -64);

    companion object {
        fun fromTier(tier: Int): ScannerType = entries.find { it.tier == tier } ?: OD
    }
}

/**
 * OD 扫描仪 - 矿石密度扫描器。
 *
 * 参数：
 * - 能量等级 1，容量 100,000 EU，每次扫描消耗 50 EU
 * - 扫描半径 6（13×13 区域），深度至 y=0，共 20 次使用
 *
 * OV 扫描仪 - 矿石价值扫描器（AdvancedScanner）。
 *
 * 参数：
 * - 能量等级 2，容量 1,000,000 EU，每次扫描消耗 250 EU
 * - 扫描半径 12（25×25 区域），深度至 y=-64，共 200 次使用
 *
 * 使用方式：必须先在储能箱/高级储能箱中充电。右键打开 GUI，点击"扫描"。
 */
@ModItem(name = "scanner", tab = CreativeTab.IC2_TOOLS, group = "tools")
class OdScannerItem : Item(Item.Properties().maxStackSize(1)), IElectricTool {

    override val tier: Int get() = 1
    override val maxCapacity: Long get() = ScannerType.OD.energyCapacity

    override fun getEnergy(stack: ItemStack): Long = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun getMaxEnergy(): Long = maxCapacity

    override fun appendTooltip(
        stack: ItemStack,
        world: Level?,
        tooltip: MutableList<Component>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
        val uses = getUsesRemaining(stack)
        tooltip.add(Component.literal("剩余使用次数: $uses").withStyle(net.minecraft.ChatFormatting.GRAY))
        tooltip.add(Component.literal("扫描范围: ${ScannerType.OD.scanRadius * 2 + 1}×${ScannerType.OD.scanRadius * 2 + 1}").withStyle(net.minecraft.ChatFormatting.GRAY))
    }

    override fun isBarVisible(stack: ItemStack): Boolean = true
    override fun getBarWidth(stack: ItemStack): Int = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack): Int = getEnergyBarColor(stack)

    /**
     * 右键：打开扫描仪 GUI。
     */
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        return openScannerScreen(world, player, hand)
    }

    companion object {
        const val NBT_USES = "ScannerUses"

        /**
         * 打开扫描仪 GUI（OD 与 OV 共用）。
         */
        fun openScannerScreen(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
            val stack = player.getItemInHand(hand)
            if (world.isClient) return InteractionResultHolder.pass(stack)

            player.openMenu(object : ExtendedScreenHandlerFactory {
                override fun getDisplayName(): Component {
                    // OD/OV 共用 GUI：标题必须跟随实际扫描仪类型，不能写死 OD 的 key
                    val t = getScannerType(player.getItemInHand(hand))
                    val key = if (t == ScannerType.OV) "item.ic2_120.advanced_scanner" else "item.ic2_120.scanner"
                    return Component.translatable(key)
                }
                override fun writeScreenOpeningData(serverPlayer: net.minecraft.server.level.ServerPlayer, buf: FriendlyByteBuf) {
                    val s = serverPlayer.getItemInHand(hand)
                    val t = getScannerType(s)
                    buf.writeInt(IElectricTool.getEnergy(s).toInt().coerceAtMost(Int.MAX_VALUE))
                    buf.writeInt(t.energyCapacity.toInt().coerceAtMost(Int.MAX_VALUE))
                    buf.writeVarInt(getUsesRemaining(s))
                    buf.writeVarInt(t.maxUses)
                }
                override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): net.minecraft.world.inventory.AbstractContainerMenu {
                    val s = player.getItemInHand(hand)
                    val t = getScannerType(s)
                    val buf = FriendlyByteBuf(Unpooled.buffer())
                    buf.writeInt(IElectricTool.getEnergy(s).toInt().coerceAtMost(Int.MAX_VALUE))
                    buf.writeInt(t.energyCapacity.toInt().coerceAtMost(Int.MAX_VALUE))
                    buf.writeVarInt(getUsesRemaining(s))
                    buf.writeVarInt(t.maxUses)
                    return ScannerScreenHandler.fromBuffer(syncId, playerInventory, buf)
                }
            })

            return InteractionResultHolder.success(stack)
        }

        fun getScannerType(stack: ItemStack): ScannerType {
            val item = stack.item
            return when {
                item is AdvancedScannerItem -> ScannerType.OV
                item is OdScannerItem -> ScannerType.OD
                else -> {
                    val tool = item as? IElectricTool
                    ScannerType.fromTier(tool?.tier ?: 1)
                }
            }
        }

        fun getUsesRemaining(stack: ItemStack): Int {
            return stack.orCreateNbt.getInt(NBT_USES).takeIf { it > 0 }
                ?: getScannerType(stack).maxUses
        }

        fun setUsesRemaining(stack: ItemStack, uses: Int) {
            stack.orCreateNbt.putInt(NBT_USES, uses)
        }

        /**
         * 生成 OD 扫描仪合成表。
         * 合成表：
         * [空]       [荧光粉]      [空]
         * [电路板]   [充电电池]    [电路板]
         * [绝缘铜质导线] [绝缘铜质导线] [绝缘铜质导线]
         */
        @RecipeProvider
        fun generateRecipes(exporter: java.util.function.Consumer<net.minecraft.data.recipes.RecipeOutput>) {
            val circuit = ic2_120.content.item.AdvancedCircuit::class.instance()
            val battery = ic2_120.content.item.energy.ReBatteryItem::class.instance()
            val insulatedCopper = ic2_120.content.block.cables.InsulatedCopperCableBlock::class.item()
            val glowstone = net.minecraft.world.item.Items.GLOWSTONE_DUST

            net.minecraft.data.recipes.ShapedRecipeBuilder.shaped(
                RecipeCategory.EQUIPMENT,
                OdScannerItem::class.instance(),
                1
            )
                .pattern("gCg").pattern("ibi").pattern("ccc")
                .define('g', glowstone)
                .define('C', circuit)
                .define('i', insulatedCopper)
                .define('b', battery)
                .define('c', insulatedCopper)
                .unlockedBy(
                    net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.has(battery),
                    net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.has(battery)
                )
                .save(exporter, OdScannerItem::class.id())
        }
    }
}

/**
 * OV 扫描仪 - 矿石价值扫描器，能量等级 2，扫描范围更大。
 */
@ModItem(name = "advanced_scanner", tab = CreativeTab.IC2_TOOLS, group = "tools")
class AdvancedScannerItem : Item(Item.Properties().maxStackSize(1)), IElectricTool {

    override val tier: Int get() = 3
    override val maxCapacity: Long get() = ScannerType.OV.energyCapacity

    override fun getEnergy(stack: ItemStack): Long = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun getMaxEnergy(): Long = maxCapacity

    override fun appendTooltip(
        stack: ItemStack,
        world: Level?,
        tooltip: MutableList<Component>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
        val uses = OdScannerItem.getUsesRemaining(stack)
        tooltip.add(Component.literal("剩余使用次数: $uses").withStyle(net.minecraft.ChatFormatting.GRAY))
        tooltip.add(Component.literal("扫描范围: ${ScannerType.OV.scanRadius * 2 + 1}×${ScannerType.OV.scanRadius * 2 + 1}").withStyle(net.minecraft.ChatFormatting.GRAY))
        tooltip.add(Component.translatable("item.ic2_120.advanced_scanner.tooltip").withStyle(net.minecraft.ChatFormatting.YELLOW))
    }

    override fun isBarVisible(stack: ItemStack): Boolean = true
    override fun getBarWidth(stack: ItemStack): Int = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack): Int = getEnergyBarColor(stack)

    /**
     * 右键：打开扫描仪 GUI。
     * 与 OD 扫描仪共用同一套逻辑（通过 getScannerType 区分类型）。
     */
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        return OdScannerItem.openScannerScreen(world, player, hand)
    }

    companion object {
        /**
         * 生成 OV 扫描仪合成表。
         * 合成表：
         * [黄金外壳] [能量水晶] [黄金外壳]
         * [荧光粉]   [高级电路] [荧光粉]
         * [2x绝缘金质导线] [OD扫描器] [2x绝缘金质导线]
         */
        @RecipeProvider
        fun generateRecipes(exporter: java.util.function.Consumer<net.minecraft.data.recipes.RecipeOutput>) {
            val goldCasing = ic2_120.content.item.GoldCasing::class.instance()
            val energyCrystal = ic2_120.content.item.energy.EnergyCrystalItem::class.instance()
            val glowstone = net.minecraft.world.item.Items.GLOWSTONE_DUST
            val circuitAdvanced = ic2_120.content.item.AdvancedCircuit::class.instance()
            val insulatedGold = ic2_120.content.block.cables.InsulatedGoldCableBlock::class.item()
            val odScanner = OdScannerItem::class.instance()

            net.minecraft.data.recipes.ShapedRecipeBuilder.shaped(
                RecipeCategory.EQUIPMENT,
                AdvancedScannerItem::class.instance(),
                1
            )
                .pattern("gCg").pattern("gAg").pattern("iIi")
                .define('g', glowstone)
                .define('C', circuitAdvanced)
                .define('A', energyCrystal)
                .define('i', insulatedGold)
                .define('I', odScanner)
                .unlockedBy(
                    net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.has(odScanner),
                    net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.has(odScanner)
                )
                .save(exporter, AdvancedScannerItem::class.id())
        }
    }
}
