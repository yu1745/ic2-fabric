package ic2_120.client

import ic2_120.config.Ic2Config
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier

@Environment(EnvType.CLIENT)
object PeatOreTooltipHandler {

    private val PEAT_ORE_ID = Identifier.fromNamespaceAndPath("ic2_120", "peat_ore")

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            val itemId = BuiltInRegistries.ITEM.getKey(stack.item)
            if (itemId != PEAT_ORE_ID) return@register

            if (Minecraft.getInstance().hasShiftDown()) {
                val config = Ic2Config.current.worldgen.peatOre
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.recipes").withStyle(ChatFormatting.GOLD))
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.coke_oven").withStyle(ChatFormatting.DARK_GRAY))
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.centrifuge").withStyle(ChatFormatting.DARK_GRAY))
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.canner").withStyle(ChatFormatting.DARK_GRAY))

                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.spawn_header").withStyle(ChatFormatting.GOLD))
                if (config.enabled) {
                    config.biomes.forEach { biomeId ->
                        val biomeName = Component.translatable("biome.${biomeId.replace(':', '.')}")
                        lines.add(Component.literal("  ").append(biomeName).withStyle(ChatFormatting.DARK_GRAY))
                    }
                    lines.add(
                        Component.translatable("tooltip.ic2_120.peat_ore.spawn_range", config.minY, config.maxY)
                            .withStyle(ChatFormatting.DARK_GRAY)
                    )
                } else {
                    lines.add(Component.translatable("tooltip.ic2_120.peat_ore.spawn_disabled").withStyle(ChatFormatting.RED))
                }
            } else {
                lines.add(Component.translatable("tooltip.ic2_120.peat_ore.hint").withStyle(ChatFormatting.DARK_GRAY))
            }
        }
    }
}
