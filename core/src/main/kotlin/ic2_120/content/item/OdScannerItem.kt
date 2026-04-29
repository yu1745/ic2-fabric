package ic2_120.content.item

import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.screen.ScannerScreenHandler
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.recipe.book.RecipeCategory
import io.netty.buffer.Unpooled
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import ic2_120.registry.annotation.RecipeProvider

/**
 * 扫描仪类型：OD（矿石密度）与 OV（矿石价值）。
 */
enum class ScannerType(
    val energyCapacity: Long,
    val energyPerScan: Int,
    val scanRadius: Int,
    val maxUses: Int,
    val tier: Int
) {
    OD(10_000L, 48, 7, 208, 1),
    OV(1_000_000L, 250, 12, 512, 3);

    companion object {
        fun fromTier(tier: Int): ScannerType = entries.find { it.tier == tier } ?: OD
    }
}

/**
 * OD 扫描仪 - 矿石密度扫描器。
 *
 * 参数：
 * - 能量等级 1，容量 10,000 EU，每次扫描消耗 48 EU
 * - 扫描半径 7（13×13 区域），共 208 次使用
 *
 * OV 扫描仪 - 矿石价值扫描器（AdvancedScanner）。
 *
 * 参数：
 * - 能量等级 3，容量 1,000,000 EU，每次扫描消耗 250 EU
 * - 扫描半径 12（25×25 区域），共 512 次使用
 *
 * 使用方式：必须先在储能箱/高级储能箱中充电。右键打开 GUI，点击"扫描"。
 */
@ModItem(name = "scanner", tab = CreativeTab.IC2_TOOLS, group = "tools")
class OdScannerItem : Item(FabricItemSettings().maxCount(1)), IElectricTool {

    override val tier: Int get() = 1
    override val maxCapacity: Long get() = ScannerType.OD.energyCapacity

    override fun getEnergy(stack: ItemStack): Long = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun getMaxEnergy(): Long = maxCapacity

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
        val uses = getUsesRemaining(stack)
        tooltip.add(Text.literal("剩余使用次数: $uses").formatted(net.minecraft.util.Formatting.GRAY))
        tooltip.add(Text.literal("扫描半径: ${ScannerType.OD.scanRadius * 2 + 1}×${ScannerType.OD.scanRadius * 2 + 1}").formatted(net.minecraft.util.Formatting.GRAY))
    }

    override fun isItemBarVisible(stack: ItemStack): Boolean = true
    override fun getItemBarStep(stack: ItemStack): Int = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack): Int = getEnergyBarColor(stack)

    /**
     * 右键：打开扫描仪 GUI。
     */
    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        return openScannerScreen(world, player, hand)
    }

    companion object {
        const val NBT_USES = "ScannerUses"

        /**
         * 打开扫描仪 GUI（OD 与 OV 共用）。
         */
        fun openScannerScreen(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
            val stack = player.getStackInHand(hand)
            if (world.isClient) return TypedActionResult.pass(stack)

            player.openHandledScreen(object : ExtendedScreenHandlerFactory {
                override fun getDisplayName(): Text = Text.translatable("item.ic2_120.scanner")
                override fun writeScreenOpeningData(serverPlayer: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
                    val s = serverPlayer.getStackInHand(hand)
                    val t = getScannerType(s)
                    buf.writeInt(IElectricTool.getEnergy(s).toInt().coerceAtMost(Int.MAX_VALUE))
                    buf.writeInt(t.energyCapacity.toInt().coerceAtMost(Int.MAX_VALUE))
                    buf.writeVarInt(getUsesRemaining(s))
                    buf.writeVarInt(t.maxUses)
                }
                override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): net.minecraft.screen.ScreenHandler {
                    val s = player.getStackInHand(hand)
                    val t = getScannerType(s)
                    val buf = PacketByteBuf(Unpooled.buffer())
                    buf.writeInt(IElectricTool.getEnergy(s).toInt().coerceAtMost(Int.MAX_VALUE))
                    buf.writeInt(t.energyCapacity.toInt().coerceAtMost(Int.MAX_VALUE))
                    buf.writeVarInt(getUsesRemaining(s))
                    buf.writeVarInt(t.maxUses)
                    return ScannerScreenHandler.fromBuffer(syncId, playerInventory, buf)
                }
            })

            return TypedActionResult.success(stack, true)
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
        fun generateRecipes(exporter: java.util.function.Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            val circuit = ic2_120.content.item.AdvancedCircuit::class.instance()
            val battery = ic2_120.content.item.energy.ReBatteryItem::class.instance()
            val insulatedCopper = ic2_120.content.block.cables.InsulatedCopperCableBlock::class.item()
            val glowstone = net.minecraft.item.Items.GLOWSTONE_DUST

            net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder.create(
                RecipeCategory.TOOLS,
                OdScannerItem::class.instance(),
                1
            )
                .pattern("gCg").pattern("ibi").pattern("ccc")
                .input('g', glowstone)
                .input('C', circuit)
                .input('i', insulatedCopper)
                .input('b', battery)
                .input('c', insulatedCopper)
                .criterion(
                    net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem(battery),
                    net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem(battery)
                )
                .offerTo(exporter, OdScannerItem::class.id())
        }
    }
}

/**
 * OV 扫描仪 - 矿石价值扫描器，能量等级 3，扫描范围更大。
 */
@ModItem(name = "advanced_scanner", tab = CreativeTab.IC2_TOOLS, group = "tools")
class AdvancedScannerItem : Item(FabricItemSettings().maxCount(1)), IElectricTool {

    override val tier: Int get() = 3
    override val maxCapacity: Long get() = ScannerType.OV.energyCapacity

    override fun getEnergy(stack: ItemStack): Long = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun getMaxEnergy(): Long = maxCapacity

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
        val uses = OdScannerItem.getUsesRemaining(stack)
        tooltip.add(Text.literal("剩余使用次数: $uses").formatted(net.minecraft.util.Formatting.GRAY))
        tooltip.add(Text.literal("扫描半径: ${ScannerType.OV.scanRadius * 2 + 1}×${ScannerType.OV.scanRadius * 2 + 1}").formatted(net.minecraft.util.Formatting.GRAY))
        tooltip.add(Text.literal("⚠ OV 扫描仪 - 扫描范围更大").formatted(net.minecraft.util.Formatting.YELLOW))
    }

    override fun isItemBarVisible(stack: ItemStack): Boolean = true
    override fun getItemBarStep(stack: ItemStack): Int = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack): Int = getEnergyBarColor(stack)

    /**
     * 右键：打开扫描仪 GUI。
     * 与 OD 扫描仪共用同一套逻辑（通过 getScannerType 区分类型）。
     */
    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
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
        fun generateRecipes(exporter: java.util.function.Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            val goldCasing = ic2_120.content.item.GoldCasing::class.instance()
            val energyCrystal = ic2_120.content.item.energy.EnergyCrystalItem::class.instance()
            val glowstone = net.minecraft.item.Items.GLOWSTONE_DUST
            val circuitAdvanced = ic2_120.content.item.AdvancedCircuit::class.instance()
            val insulatedGold = ic2_120.content.block.cables.InsulatedGoldCableBlock::class.item()
            val odScanner = OdScannerItem::class.instance()

            net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder.create(
                RecipeCategory.TOOLS,
                AdvancedScannerItem::class.instance(),
                1
            )
                .pattern("gCg").pattern("gAg").pattern("iIi")
                .input('g', glowstone)
                .input('C', circuitAdvanced)
                .input('A', energyCrystal)
                .input('i', insulatedGold)
                .input('I', odScanner)
                .criterion(
                    net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem(odScanner),
                    net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem(odScanner)
                )
                .offerTo(exporter, AdvancedScannerItem::class.id())
        }
    }
}
