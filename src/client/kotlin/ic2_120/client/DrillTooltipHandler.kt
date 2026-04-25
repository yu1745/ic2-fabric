package ic2_120.client

import ic2_120.content.item.DiamondDrill
import ic2_120.content.item.Drill
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.Chainsaw
import ic2_120.content.item.NanoSaber
import ic2_120.content.item.energy.IElectricTool
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * 钻头 Tooltip 客户端处理器
 *
 * 参考量子套装，动态添加：
 * - 还能挖 X 次（根据当前电量与单次耗能计算）
 * - 铱钻头：精准采集快捷键（动态显示玩家绑定的按键）
 */
@Environment(EnvType.CLIENT)
object DrillTooltipHandler {

    private const val DRILL_EU_PER_BLOCK = 50L
    private const val DIAMOND_DRILL_EU_PER_BLOCK = 80L
    private const val IRIDIUM_DRILL_EU_PER_BLOCK = 800L
    private const val IRIDIUM_DRILL_SILK_EU_PER_BLOCK = 8_000L
    private const val CHAINSAW_EU_PER_BLOCK = 250L
    private const val NANO_SABER_EU_PER_HIT = 1_000L

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type, lines ->
            val item = stack.item
            if (item !is IElectricTool) return@register

            val euPerBlock = when (item) {
                is Chainsaw -> CHAINSAW_EU_PER_BLOCK
                is Drill -> DRILL_EU_PER_BLOCK
                is DiamondDrill -> DIAMOND_DRILL_EU_PER_BLOCK
                is IridiumDrill -> {
                    if (IridiumDrill.isSilkTouchEnabled(stack)) IRIDIUM_DRILL_SILK_EU_PER_BLOCK else IRIDIUM_DRILL_EU_PER_BLOCK
                }
                is NanoSaber -> NANO_SABER_EU_PER_HIT
                else -> null
            }

            if (euPerBlock != null) {
                val energy = item.getEnergy(stack)
                val remainingBlocks = if (euPerBlock > 0) energy / euPerBlock else 0L
                lines.add(
                    Text.literal(
                        when (item) {
                            is Chainsaw -> "还能砍 "
                            is NanoSaber -> "还能攻击 "
                            else -> "还能挖 "
                        }
                    )
                        .formatted(Formatting.GRAY)
                        .append(Text.literal("$remainingBlocks").formatted(Formatting.YELLOW))
                        .append(Text.literal(" 次").formatted(Formatting.GRAY))
                )
            }

            // 铱钻头：模式键 + 右键切换精准采集（不需要 Alt）
            if (item is IridiumDrill) {
                val modeKey = ModeKeybinds.getModeKey()
                val boundKeyName = modeKey.boundKeyLocalizedText.string
                val silkEnabled = IridiumDrill.isSilkTouchEnabled(stack)
                lines.add(
                    Text.literal(
                        if (silkEnabled) "当前模式: 精准采集" else "当前模式: 时运III + 效率III"
                    ).formatted(Formatting.GRAY)
                )
                lines.add(
                    Text.literal("精准采集按键: ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(boundKeyName).formatted(Formatting.YELLOW))
                        .append(Text.literal(" + 右键").formatted(Formatting.YELLOW))
                )
            }
        }
    }
}
