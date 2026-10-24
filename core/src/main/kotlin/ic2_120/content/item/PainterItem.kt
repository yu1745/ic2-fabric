package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.stack

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.integration.ftbchunks.ClaimProtection

import ic2_120.content.block.BlackWallBlock
import ic2_120.content.block.BlueWallBlock
import ic2_120.content.block.BrownWallBlock
import ic2_120.content.block.CyanWallBlock
import ic2_120.content.block.GrayWallBlock
import ic2_120.content.block.GreenWallBlock
import ic2_120.content.block.LightBlueWallBlock
import ic2_120.content.block.LightGrayWallBlock
import ic2_120.content.block.LimeWallBlock
import ic2_120.content.block.MagentaWallBlock
import ic2_120.content.block.OrangeWallBlock
import ic2_120.content.block.PinkWallBlock
import ic2_120.content.block.PurpleWallBlock
import ic2_120.content.block.RedWallBlock
import ic2_120.content.block.WhiteWallBlock
import ic2_120.content.block.YellowWallBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.level.block.Block
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.DyeColor
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level
import java.util.function.Consumer

/**
 * IC2 涂刷器。
 *
 * 右手持刷子、副手持染料右键可将颜色装入刷子；之后右键固化后的建筑泡沫墙
 * 进行染色。未固化泡沫和强化泡沫不会被染色，符合原版 IC2 Painter 的目标范围。
 */
@ModItem(name = "painter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class PainterItem : Item(Item.Properties().maxStackSize(1).maxDamage(MAX_DAMAGE)) {

    override fun getName(stack: ItemStack): Component {
        val color = getColor(stack)
        return Component.translatable(
            if (color == null) "item.ic2_120.painter" else "item.ic2_120.painter.${color.getName()}"
        )
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        val stack = context.stack
        val dye = player.getItemInHand(InteractionHand.OFF_HAND).item as? DyeItem
        if (dye != null) {
            if (setColor(stack, dye.color, player, InteractionHand.OFF_HAND)) {
                return if (context.world.isClient) InteractionResult.SUCCESS else InteractionResult.CONSUME
            }
        }

        val color = getColor(stack) ?: return InteractionResult.PASS
        val oldBlock = context.world.getBlockState(context.blockPos).block
        val target = wallFor(color)
        if (target == null || oldBlock === target) return InteractionResult.PASS
        if (wallColor(oldBlock) != null) {
            if (!context.world.isClient && ClaimProtection.isProtected(context.world, context.blockPos, player, ClaimProtection.EDIT_BLOCK)) {
                return InteractionResult.FAIL
            }
            context.world.setBlock(context.blockPos, target.defaultBlockState)
            if (!context.world.isClient) damagePainter(stack, player, context.hand)
            return InteractionResult.CONSUME
        }
        return InteractionResult.PASS
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)
        val dyeHand = if (hand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        val dye = user.getItemInHand(dyeHand).item as? DyeItem
        if (dye != null && setColor(stack, dye.color, user, dyeHand)) {
            return InteractionResultHolder.success(stack, world.isClient)
        }
        return InteractionResultHolder.pass(stack)
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: Level?,
        tooltip: MutableList<Component>,
        context: net.minecraft.client.item.TooltipContext,
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val color = getColor(stack)
        tooltip.add(
            Component.translatable(
                if (color == null) "tooltip.ic2_120.painter.uncolored" else "tooltip.ic2_120.painter.color",
                color?.getName() ?: "",
            ).withStyle(ChatFormatting.GRAY)
        )
        tooltip.add(
            Component.translatable(
                "tooltip.ic2_120.painter.durability",
                (stack.maxDamage - stack.damage).coerceAtLeast(0),
                stack.maxDamage,
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun isBarVisible(stack: ItemStack): Boolean = true

    private fun setColor(stack: ItemStack, color: DyeColor, player: Player, dyeHand: InteractionHand): Boolean {
        if (getColor(stack) == color) return false
        stack.orCreateNbt.putInt(COLOR_KEY, color.id)
        if (!player.abilities.creativeMode) player.getItemInHand(dyeHand).shrink(1)
        return true
    }

    private fun damagePainter(stack: ItemStack, player: Player, hand: InteractionHand) {
        val nextDamage = stack.damage + 1
        if (nextDamage >= stack.maxDamage) {
            // 原版 IC2 涂刷器耗尽后不会消失，而是恢复成未着色的初始刷子。
            stack.damage = 0
            stack.removeSubNbt(COLOR_KEY)
            player.sendToolBreakStatus(hand)
        } else {
            stack.damage = nextDamage
        }
    }

    companion object {
        private const val COLOR_KEY = "PainterColor"
        private const val MAX_DAMAGE = 32

        fun getColor(stack: ItemStack): DyeColor? {
            val nbt = stack.nbt ?: return null
            if (!nbt.hasProperty(COLOR_KEY)) return null
            return DyeColor.byId(nbt.getInt(COLOR_KEY))
        }

        /** 创造栏和 JEI 使用的 16 个带色刷子变体。 */
        fun createColoredStacks(): List<ItemStack> = DyeColor.entries.map { color ->
            ItemStack(PainterItem::class.instance()).also { stack ->
                stack.orCreateNbt.putInt(COLOR_KEY, color.id)
            }
        }

        private fun wallFor(color: DyeColor): Block? = when (color) {
            DyeColor.WHITE -> WhiteWallBlock::class.instance()
            DyeColor.ORANGE -> OrangeWallBlock::class.instance()
            DyeColor.MAGENTA -> MagentaWallBlock::class.instance()
            DyeColor.LIGHT_BLUE -> LightBlueWallBlock::class.instance()
            DyeColor.YELLOW -> YellowWallBlock::class.instance()
            DyeColor.LIME -> LimeWallBlock::class.instance()
            DyeColor.PINK -> PinkWallBlock::class.instance()
            DyeColor.GRAY -> GrayWallBlock::class.instance()
            DyeColor.LIGHT_GRAY -> LightGrayWallBlock::class.instance()
            DyeColor.CYAN -> CyanWallBlock::class.instance()
            DyeColor.PURPLE -> PurpleWallBlock::class.instance()
            DyeColor.BLUE -> BlueWallBlock::class.instance()
            DyeColor.BROWN -> BrownWallBlock::class.instance()
            DyeColor.GREEN -> GreenWallBlock::class.instance()
            DyeColor.RED -> RedWallBlock::class.instance()
            DyeColor.BLACK -> BlackWallBlock::class.instance()
        }

        private fun wallColor(block: Block): DyeColor? = DyeColor.entries.firstOrNull { wallFor(it) === block }

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.EQUIPMENT, PainterItem::class.instance(), 1)
                .pattern("II ")
                .pattern(" IS")
                .pattern("  I")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(exporter, PainterItem::class.id())
        }
    }
}
