package ic2_120.content.item

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
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.Block
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.DyeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.DyeColor
import net.minecraft.util.Formatting
import net.minecraft.util.TypedActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World
import java.util.function.Consumer

/**
 * IC2 涂刷器。
 *
 * 右手持刷子、副手持染料右键可将颜色装入刷子；之后右键固化后的建筑泡沫墙
 * 进行染色。未固化泡沫和强化泡沫不会被染色，符合原版 IC2 Painter 的目标范围。
 */
@ModItem(name = "painter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class PainterItem : Item(FabricItemSettings().maxCount(1).maxDamage(MAX_DAMAGE)) {

    override fun getName(stack: ItemStack): Text {
        val color = getColor(stack)
        return Text.translatable(
            if (color == null) "item.ic2_120.painter" else "item.ic2_120.painter.${color.getName()}"
        )
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val player = context.player ?: return ActionResult.PASS
        val stack = context.stack
        val dye = player.getStackInHand(Hand.OFF_HAND).item as? DyeItem
        if (dye != null) {
            if (setColor(stack, dye.color, player, Hand.OFF_HAND)) {
                return if (context.world.isClient) ActionResult.SUCCESS else ActionResult.CONSUME
            }
        }

        val color = getColor(stack) ?: return ActionResult.PASS
        val oldBlock = context.world.getBlockState(context.blockPos).block
        val target = wallFor(color)
        if (target == null || oldBlock === target) return ActionResult.PASS
        if (wallColor(oldBlock) != null) {
            context.world.setBlockState(context.blockPos, target.defaultState)
            if (!context.world.isClient) damagePainter(stack, player, context.hand)
            return ActionResult.CONSUME
        }
        return ActionResult.PASS
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        val dyeHand = if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND
        val dye = user.getStackInHand(dyeHand).item as? DyeItem
        if (dye != null && setColor(stack, dye.color, user, dyeHand)) {
            return TypedActionResult.success(stack, world.isClient)
        }
        return TypedActionResult.pass(stack)
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext,
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        val color = getColor(stack)
        tooltip.add(
            Text.translatable(
                if (color == null) "tooltip.ic2_120.painter.uncolored" else "tooltip.ic2_120.painter.color",
                color?.getName() ?: "",
            ).formatted(Formatting.GRAY)
        )
        tooltip.add(
            Text.translatable(
                "tooltip.ic2_120.painter.durability",
                (stack.maxDamage - stack.damage).coerceAtLeast(0),
                stack.maxDamage,
            ).formatted(Formatting.GRAY)
        )
    }

    override fun isItemBarVisible(stack: ItemStack): Boolean = true

    private fun setColor(stack: ItemStack, color: DyeColor, player: PlayerEntity, dyeHand: Hand): Boolean {
        if (getColor(stack) == color) return false
        stack.orCreateNbt.putInt(COLOR_KEY, color.id)
        if (!player.abilities.creativeMode) player.getStackInHand(dyeHand).decrement(1)
        return true
    }

    private fun damagePainter(stack: ItemStack, player: PlayerEntity, hand: Hand) {
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
            if (!nbt.contains(COLOR_KEY)) return null
            return DyeColor.byId(nbt.getInt(COLOR_KEY))
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
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, PainterItem::class.instance(), 1)
                .pattern("II ")
                .pattern(" IS")
                .pattern("  I")
                .input('I', Items.IRON_INGOT)
                .input('S', Items.STICK)
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter, PainterItem::class.id())
        }
    }
}
