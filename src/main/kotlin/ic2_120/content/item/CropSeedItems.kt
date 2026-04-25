package ic2_120.content.item

import ic2_120.content.crop.CropStats
import ic2_120.content.crop.CropSystem
import ic2_120.content.crop.CropType
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.screen.CropnalyzerScreenHandler
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import ic2_120.getCustomData
import ic2_120.getOrCreateCustomData

object CropSeedData {
    private const val NBT_OWNER = "owner"
    private const val NBT_ID = "id"
    private const val NBT_GROWTH = "growth"
    private const val NBT_GAIN = "gain"
    private const val NBT_RESISTANCE = "resistance"
    private const val NBT_SCAN_LEVEL = "scan"

    fun readType(stack: ItemStack): CropType? {
        val nbt = stack.getCustomData() ?: return null
        val id = if (nbt.contains(NBT_ID)) nbt.getString(NBT_ID) else ""
        if (id.isEmpty()) return null
        return CropType.entries.firstOrNull { it.asString() == id }
    }

    fun readStats(stack: ItemStack): CropStats {
        val nbt = stack.getCustomData()
        if (nbt == null) return CropStats()
        return CropStats(
            growth = nbt.getInt(NBT_GROWTH).coerceIn(0, 31),
            gain = nbt.getInt(NBT_GAIN).coerceIn(0, 31),
            resistance = nbt.getInt(NBT_RESISTANCE).coerceIn(0, 31)
        )
    }

    fun readScanLevel(stack: ItemStack): Int =
        stack.getCustomData()?.getInt(NBT_SCAN_LEVEL)?.coerceIn(0, 4) ?: 0

    fun write(stack: ItemStack, type: CropType, stats: CropStats, scanLevel: Int) {
        val nbt = stack.getOrCreateCustomData()
        nbt.putString(NBT_OWNER, "ic2_120")
        nbt.putString(NBT_ID, type.asString())
        nbt.putInt(NBT_GROWTH, stats.growth.coerceIn(0, 31))
        nbt.putInt(NBT_GAIN, stats.gain.coerceIn(0, 31))
        nbt.putInt(NBT_RESISTANCE, stats.resistance.coerceIn(0, 31))
        nbt.putInt(NBT_SCAN_LEVEL, scanLevel.coerceIn(0, 4))
    }

    fun displayName(type: CropType): Text = Text.translatable("crop.ic2_120.${type.asString()}")
}

@ModItem(name = "crop_seed_bag", tab = CreativeTab.IC2_CROP_SEEDS, group = "crops")
class CropSeedBagItem : Item(Item.Settings().maxCount(1)) {
    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        val scanLevel = CropSeedData.readScanLevel(stack)
        val type = CropSeedData.readType(stack)
        tooltip.add(Text.literal("扫描等级: $scanLevel/4").formatted(net.minecraft.util.Formatting.GRAY))
        if (scanLevel <= 0 || type == null) {
            tooltip.add(Text.literal("作物: 未知").formatted(net.minecraft.util.Formatting.DARK_GRAY))
            return
        }

        val def = CropSystem.definition(type)
        tooltip.add(
            Text.literal("作物: ")
                .append(CropSeedData.displayName(type))
                .formatted(net.minecraft.util.Formatting.GRAY)
        )
        if (scanLevel >= 2) {
            tooltip.add(
                Text.literal("属性: ${def.attributes.take(3).joinToString("/")}")
                    .formatted(net.minecraft.util.Formatting.DARK_AQUA)
            )
        }
        if (scanLevel >= 3) {
            tooltip.add(
                Text.literal("等级: ${def.tier}  成熟龄: ${def.maxVisualAge}")
                    .formatted(net.minecraft.util.Formatting.BLUE)
            )
        }
        if (scanLevel >= 4) {
            val stats = CropSeedData.readStats(stack)
            tooltip.add(
                Text.literal("G:${stats.growth} Ga:${stats.gain} R:${stats.resistance}")
                    .formatted(net.minecraft.util.Formatting.DARK_GREEN)
            )
        }
    }

    companion object {
        fun createStack(type: CropType, stats: CropStats, scanLevel: Int = 1): ItemStack =
            ItemStack(CropSeedBagItem::class.instance()).apply {
                CropSeedData.write(this, type, stats, scanLevel)
            }

        fun createInitialSeedStacks(): List<ItemStack> {
            val baseStats = CropStats(1, 1, 1)
            return CropType.entries
                .filter { it != CropType.WEED }
                .map { type -> createStack(type, baseStats, scanLevel = 4) }
        }
    }
}

@ModItem(name = "cropnalyzer", tab = CreativeTab.IC2_TOOLS, group = "tools")
class CropnalyzerItem : Item(Item.Settings().maxCount(1)), IElectricTool {
    override val tier: Int = 1
    override val maxCapacity: Long = 10_000L

    override fun getEnergy(stack: ItemStack): Long = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
        tooltip.add(Text.literal("右键打开 GUI 扫描种子袋").formatted(net.minecraft.util.Formatting.GRAY))
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val scanner = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(scanner)

        user.openHandledScreen(object : ExtendedScreenHandlerFactory {
            override fun getDisplayName(): Text = Text.translatable("item.ic2_120.cropnalyzer")

            override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
                buf.writeEnumConstant(hand)
            }

            override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): net.minecraft.screen.ScreenHandler {
                return CropnalyzerScreenHandler(syncId, playerInventory, hand)
            }
        })

        return TypedActionResult.success(scanner, true)
    }

    override fun useOnBlock(context: net.minecraft.item.ItemUsageContext): ActionResult {
        val world = context.world
        if (world.isClient) return ActionResult.SUCCESS
        val player = context.player ?: return ActionResult.PASS
        val stack = context.stack
        val be = world.getBlockEntity(context.blockPos)
        val cropBe = be as? ic2_120.content.block.CropBlockEntity ?: return ActionResult.PASS
        val cropState = world.getBlockState(context.blockPos)
        val type = cropState.get(ic2_120.content.block.CropBlock.CROP_TYPE)

        val energy = getEnergy(stack)
        if (energy < ENERGY_PER_SCAN) {
            player.sendMessage(Text.literal("Cropnalyzer 电量不足").formatted(net.minecraft.util.Formatting.RED), true)
            return ActionResult.SUCCESS
        }
        setEnergy(stack, energy - ENERGY_PER_SCAN)

        val msg = Text.literal("作物 ")
            .append(CropSeedData.displayName(type))
            .append(
                Text.literal(" | G:${cropBe.stats.growth} Ga:${cropBe.stats.gain} R:${cropBe.stats.resistance}")
            )
        player.sendMessage(msg, false)
        return ActionResult.SUCCESS
    }

    companion object {
        const val ENERGY_PER_SCAN = 50L

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val cable = InsulatedCopperCableBlock::class.item()
            val circuit = Circuit::class.instance()
            if (cable == Items.AIR || circuit == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CropnalyzerItem::class.instance(), 1)
                .pattern("CC ")
                .pattern("RGR")
                .pattern("RXR")
                .input('C', cable)
                .input('R', Items.REDSTONE)
                .input('G', Items.GLASS)
                .input('X', circuit)
                .criterion(hasItem(circuit), conditionsFromItem(circuit))
                .offerTo(exporter, CropnalyzerItem::class.id())
        }

        fun buildResultMessage(type: CropType, stats: CropStats, level: Int): Text {
            return when {
                level <= 0 -> Text.literal("扫描失败")
                level == 1 -> Text.literal("扫描 Lv1: ").append(CropSeedData.displayName(type))
                level == 2 -> Text.literal("扫描 Lv2: ").append(CropSeedData.displayName(type)).append(Text.literal(" | 属性已解锁"))
                level == 3 -> Text.literal("扫描 Lv3: ").append(CropSeedData.displayName(type)).append(Text.literal(" | 生长信息已解锁"))
                else -> Text.literal("扫描 Lv4: ").append(CropSeedData.displayName(type))
                    .append(Text.literal(" | G:${stats.growth} Ga:${stats.gain} R:${stats.resistance}"))
            }
        }
    }
}
