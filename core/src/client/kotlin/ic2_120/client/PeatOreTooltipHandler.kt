package ic2_120.client

import ic2_120.config.Ic2Config
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.gui.screen.Screen
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

@Environment(EnvType.CLIENT)
object PeatOreTooltipHandler {

    private val PEAT_ORE_ID = Identifier("ic2_120", "peat_ore")

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, lines ->
            val itemId = Registries.ITEM.getId(stack.item)
            if (itemId != PEAT_ORE_ID) return@register

            if (Screen.hasShiftDown()) {
                val config = Ic2Config.current.worldgen.peatOre
                lines.add(Text.translatable("tooltip.ic2_120.peat_ore.recipes").formatted(Formatting.GOLD))
                lines.add(Text.translatable("tooltip.ic2_120.peat_ore.coke_oven").formatted(Formatting.DARK_GRAY))
                lines.add(Text.translatable("tooltip.ic2_120.peat_ore.centrifuge").formatted(Formatting.DARK_GRAY))
                lines.add(Text.translatable("tooltip.ic2_120.peat_ore.canner").formatted(Formatting.DARK_GRAY))

                lines.add(Text.translatable("tooltip.ic2_120.peat_ore.spawn_header").formatted(Formatting.GOLD))
                if (config.enabled) {
                    config.biomes.forEach { biomeId ->
                        val biomeName = Text.translatable("biome.${biomeId.replace(':', '.')}")
                        lines.add(Text.literal("  ").append(biomeName).formatted(Formatting.DARK_GRAY))
                    }
                    lines.add(
                        Text.translatable("tooltip.ic2_120.peat_ore.spawn_range", config.minY, config.maxY)
                            .formatted(Formatting.DARK_GRAY)
                    )
                } else {
                    lines.add(Text.translatable("tooltip.ic2_120.peat_ore.spawn_disabled").formatted(Formatting.RED))
                }
            } else {
                lines.add(Text.translatable("tooltip.ic2_120.peat_ore.hint").formatted(Formatting.DARK_GRAY))
            }
        }
    }
}
