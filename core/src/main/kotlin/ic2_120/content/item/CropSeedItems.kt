package ic2_120.content.item

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.orCreateNbt

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.stack

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient
import ic2_120.compat.world

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
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level

object CropSeedData {
    private const val NBT_OWNER = "owner"
    private const val NBT_ID = "id"
    private const val NBT_GROWTH = "growth"
    private const val NBT_GAIN = "gain"
    private const val NBT_RESISTANCE = "resistance"
    private const val NBT_SCAN_LEVEL = "scan"

    fun readType(stack: ItemStack): CropType? {
        val nbt = stack.orCreateNbt
        if (nbt.isEmpty) return null
        val id = if (nbt.contains(NBT_ID)) nbt.getString(NBT_ID) else ""
        if (id.isEmpty()) return null
        return CropType.entries.firstOrNull { it.serializedName == id }
    }

    fun readStats(stack: ItemStack): CropStats {
        val nbt = stack.orCreateNbt
        if (nbt == null) return CropStats()
        return CropStats(
            growth = nbt.getInt(NBT_GROWTH).coerceIn(0, 31),
            gain = nbt.getInt(NBT_GAIN).coerceIn(0, 31),
            resistance = nbt.getInt(NBT_RESISTANCE).coerceIn(0, 31)
        )
    }

    fun readScanLevel(stack: ItemStack): Int =
        stack.orCreateNbt.getInt(NBT_SCAN_LEVEL).coerceIn(0, 4)

    fun write(stack: ItemStack, type: CropType, stats: CropStats, scanLevel: Int) {
        val nbt = stack.orCreateNbt
        nbt.putString(NBT_OWNER, "ic2_120")
        nbt.putString(NBT_ID, type.serializedName)
        nbt.putInt(NBT_GROWTH, stats.growth.coerceIn(0, 31))
        nbt.putInt(NBT_GAIN, stats.gain.coerceIn(0, 31))
        nbt.putInt(NBT_RESISTANCE, stats.resistance.coerceIn(0, 31))
        nbt.putInt(NBT_SCAN_LEVEL, scanLevel.coerceIn(0, 4))
    }

    fun displayName(type: CropType): Component = Component.translatable("crop.ic2_120.${type.serializedName}")
}

@ModItem(name = "crop_seed_bag", tab = CreativeTab.IC2_CROP_SEEDS, group = "crops")
class CropSeedBagItem : Item(Item.Properties().stacksTo(1)) {
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        val scanLevel = CropSeedData.readScanLevel(stack)
        val type = CropSeedData.readType(stack)
        tooltip.add(Component.literal("扫描等级: $scanLevel/4").withStyle(net.minecraft.ChatFormatting.GRAY))
        if (scanLevel <= 0 || type == null) {
            tooltip.add(Component.literal("作物: 未知").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
            return
        }

        val def = CropSystem.definition(type)
        tooltip.add(
            Component.literal("作物: ")
                .append(CropSeedData.displayName(type))
                .withStyle(net.minecraft.ChatFormatting.GRAY)
        )
        buildHarvestTooltip(type)?.let { tooltip.add(it) }
        if (scanLevel >= 2) {
            tooltip.add(
                Component.literal("属性: ${def.attributes.take(3).joinToString("/")}")
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA)
            )
        }
        if (scanLevel >= 3) {
            tooltip.add(
                Component.literal("等级: ${def.tier}  成熟龄: ${def.maxVisualAge}")
                    .withStyle(net.minecraft.ChatFormatting.BLUE)
            )
        }
        if (scanLevel >= 4) {
            val stats = CropSeedData.readStats(stack)
            tooltip.add(
                Component.literal("G:${stats.growth} Ga:${stats.gain} R:${stats.resistance}")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN)
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
class CropnalyzerItem : Item(Item.Properties().stacksTo(1)), IElectricTool {
    override val tier: Int = 2
    override val maxCapacity: Long = 100_000L
    override fun nominalEuPerTick() = 128L

    override fun getEnergy(stack: ItemStack): Long = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
        tooltip.add(Component.literal("右键打开 GUI 扫描种子袋").withStyle(net.minecraft.ChatFormatting.GRAY))
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val scanner = user.getItemInHand(hand)
        if (world.isClient) return InteractionResultHolder.success(scanner)

        user.openMenu(object : ExtendedScreenHandlerFactory<FriendlyByteBuf> {
            override fun getDisplayName(): Component = Component.translatable("item.ic2_120.cropnalyzer")

            override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
                val buf = FriendlyByteBuf(Unpooled.buffer())
                    buf.writeEnum(hand)
                return buf
            }

            override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): net.minecraft.world.inventory.AbstractContainerMenu {
                return CropnalyzerScreenHandler(syncId, playerInventory, hand)
            }
        })

        return InteractionResultHolder.success(scanner)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val world = context.level
        if (world.isClient) return InteractionResult.SUCCESS
        val player = context.player ?: return InteractionResult.PASS
        val stack = context.itemInHand
        val be = world.getBlockEntity(context.clickedPos)
        val cropBe = be as? ic2_120.content.block.CropBlockEntity ?: return InteractionResult.PASS
        val cropState = world.getBlockState(context.clickedPos)
        val type = cropState.getValue(ic2_120.content.block.CropBlock.CROP_TYPE)

        val energy = getEnergy(stack)
        if (energy < CROP_INSPECTION_ENERGY) {
            player.displayClientMessage(Component.literal("Cropnalyzer 电量不足").withStyle(net.minecraft.ChatFormatting.RED), true)
            return InteractionResult.SUCCESS
        }
        setEnergy(stack, energy - CROP_INSPECTION_ENERGY)

        val msg = Component.literal("作物 ")
            .append(CropSeedData.displayName(type))
            .append(
                Component.literal(" | G:${cropBe.stats.growth} Ga:${cropBe.stats.gain} R:${cropBe.stats.resistance}")
            )
        player.displayClientMessage(msg, false)
        return InteractionResult.SUCCESS
    }

    companion object {
        const val ENERGY_PER_SCAN = 10L
        const val CROP_INSPECTION_ENERGY = 900L

        fun energyForScanLevel(level: Int): Long = when (level.coerceIn(0, 4)) {
            0 -> 10L
            1 -> 90L
            2 -> 900L
            3 -> 9_000L
            else -> 0L
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val cable = InsulatedCopperCableBlock::class.item()
            val circuit = Circuit::class.instance()
            if (cable == Items.AIR || circuit == Items.AIR) return
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CropnalyzerItem::class.instance(), 1)
                .pattern("CC ")
                .pattern("RGR")
                .pattern("RXR")
                .define('C', cable)
                .define('R', Items.REDSTONE)
                .define('G', Items.GLASS)
                .define('X', circuit)
                .unlockedBy(getHasName(circuit), has(circuit))
                .save(exporter, CropnalyzerItem::class.id())
        }

        fun buildResultMessage(type: CropType, stats: CropStats, level: Int): Component {
            return when {
                level <= 0 -> Component.literal("扫描失败")
                level == 1 -> Component.literal("扫描 Lv1: ").append(CropSeedData.displayName(type))
                level == 2 -> Component.literal("扫描 Lv2: ").append(CropSeedData.displayName(type)).append(Component.literal(" | 属性已解锁"))
                level == 3 -> Component.literal("扫描 Lv3: ").append(CropSeedData.displayName(type)).append(Component.literal(" | 生长信息已解锁"))
                else -> Component.literal("扫描 Lv4: ").append(CropSeedData.displayName(type))
                    .append(Component.literal(" | G:${stats.growth} Ga:${stats.gain} R:${stats.resistance}"))
            }
        }
    }
}

private fun buildHarvestTooltip(type: CropType): Component? {
    val items = CropSystem.harvestItems(type)
    if (items.isEmpty()) return null

    val text = Component.translatable("tooltip.ic2_120.harvest")
    items.forEachIndexed { i, h ->
        if (i > 0) text.append(Component.translatable("tooltip.ic2_120.harvest.sep"))
        text.append(Component.translatable(h.item.descriptionId))
        when (h.qty) {
            is CropSystem.HQt.N -> text.append(Component.translatable("tooltip.ic2_120.harvest.qty_n", h.qty.n))
            is CropSystem.HQt.Age -> text.append(Component.translatable("tooltip.ic2_120.harvest.qty_age"))
            is CropSystem.HQt.Rng -> text.append(Component.translatable("tooltip.ic2_120.harvest.qty_rng", h.qty.min, h.qty.max))
            is CropSystem.HQt.One -> {}
        }
        if (h.chance in 1..99) {
            text.append(Component.translatable("tooltip.ic2_120.harvest.chance", h.chance))
        }
    }
    for (key in CropSystem.harvestNoteKeys(type)) {
        text.append(Component.literal(" "))
        text.append(Component.translatable("tooltip.ic2_120.harvest.note.$key"))
    }
    return text.withStyle(ChatFormatting.GRAY)
}
