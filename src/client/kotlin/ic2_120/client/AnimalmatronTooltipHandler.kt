package ic2_120.client

import ic2_120.content.entity.AnimalFoodMapping
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

@Environment(EnvType.CLIENT)
object AnimalmatronTooltipHandler {

    private val ANIMALMATRON_ID = Identifier.of("ic2_120", "animalmatron")

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type, tooltip ->
            val itemId = net.minecraft.registry.Registries.ITEM.getId(stack.item)
            if (itemId != ANIMALMATRON_ID) return@register

            if (Screen.hasShiftDown()) {
                tooltip.add(Text.translatable("item.ic2_120.animalmatron.tooltip.food_table").formatted(Formatting.GRAY))
                AnimalFoodMapping.getFoodMap().forEach { (id, food) ->
                    val animalKey = "entity.${id.namespace}.${id.path}"
                    val animalName = Text.translatable(animalKey).string
                    val foodName = Text.translatable(food.first().translationKey).string
                    tooltip.add(Text.literal("  ${animalName}: ${foodName}").formatted(Formatting.DARK_GRAY))
                }
                tooltip.add(Text.translatable("item.ic2_120.animalmatron.tooltip.breeding_limit").formatted(Formatting.DARK_GRAY))
            } else {
                tooltip.add(Text.translatable("item.ic2_120.animalmatron.tooltip").formatted(Formatting.GRAY))
            }
        }
    }
}
