package ic2_120.client

import ic2_120.config.Ic2Config
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceLocation

@Environment(EnvType.CLIENT)
object PeatOreTooltipHandler {

    private val PEAT_ORE_ID = ResourceLocation.of("ic2_120", "peat_ore")

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, lines ->
            val itemId = BuiltInRegistries.ITEM.getId(stack.item)
            if (itemId != PEAT_ORE_ID) return@register

            if (Screen.hasShiftDown()) {
                val config = Ic2Config.current.worldgen.peatOre
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.recipes").formatted(ChatFormatting.GOLD))
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.coke_oven").formatted(ChatFormatting.DARK_GRAY))
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.centrifuge").formatted(ChatFormatting.DARK_GRAY))
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.canner").formatted(ChatFormatting.DARK_GRAY))

                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.spawn_header").formatted(ChatFormatting.GOLD))
                if (config.enabled) {
                    config.biomes.forEach { biomeId ->
                        val biomeName = Component.translatable("biome.${biomeId.replace(':', '.')}")
                        lines.add(Component.literal("  ").append(biomeName).formatted(ChatFormatting.DARK_GRAY))
                    }
                    lines.add(
                        Component.translatable("tooltip.ic2_120.peat_ore.spawn_range", config.minY, config.maxY)
                            .formatted(ChatFormatting.DARK_GRAY)
                    )
                } else {
                    lines.add(Component.translatable("tooltip.ic2_120.peat_ore.spawn_disabled").formatted(ChatFormatting.RED))
                }
            } else {
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.hint").formatted(ChatFormatting.DARK_GRAY))
            }
        }
    }
}
