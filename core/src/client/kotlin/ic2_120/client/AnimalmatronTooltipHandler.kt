package ic2_120.client

import ic2_120.content.entity.AnimalFoodMapping
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier

@Environment(EnvType.CLIENT)
object AnimalmatronTooltipHandler {

    private val ANIMALMATRON_ID = Identifier.fromNamespaceAndPath("ic2_120", "animalmatron")

    private data class ExtraProductInfo(val entityId: Identifier, val productId: Identifier, val needsShears: Boolean)

    private val EXTRA_PRODUCTS = listOf(
        ExtraProductInfo(Identifier.fromNamespaceAndPath("minecraft", "chicken"), Identifier.fromNamespaceAndPath("minecraft", "egg"), false),
        ExtraProductInfo(Identifier.fromNamespaceAndPath("minecraft", "sheep"), Identifier.fromNamespaceAndPath("minecraft", "white_wool"), true),
    )

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            val itemId = BuiltInRegistries.ITEM.getKey(stack.item)
            if (itemId != ANIMALMATRON_ID) return@register

            if (Minecraft.getInstance().hasShiftDown()) {
                lines.add(Component.translatable("item.ic2_120.animalmatron.tooltip.food_table").withStyle(ChatFormatting.GRAY))
                AnimalFoodMapping.getFoodMap().forEach { (id, food) ->
                    val animalKey = "entity.${id.namespace}.${id.path}"
                    val animalName = Component.translatable(animalKey).string
                    val foodName = Component.translatable(food.first().descriptionId).string
                    lines.add(Component.literal("  ${animalName}: ${foodName}").withStyle(ChatFormatting.DARK_GRAY))
                }
                lines.add(Component.translatable("item.ic2_120.animalmatron.tooltip.breeding_limit").withStyle(ChatFormatting.DARK_GRAY))

                lines.add(Component.translatable("item.ic2_120.animalmatron.tooltip.extra_products").withStyle(ChatFormatting.GRAY))
                EXTRA_PRODUCTS.forEach { (entityId, productId, needsShears) ->
                    val animalKey = "entity.${entityId.namespace}.${entityId.path}"
                    val animalName = Component.translatable(animalKey).string
                    val productItem = BuiltInRegistries.ITEM.getValue(productId)
                    val productName = Component.translatable(productItem.descriptionId).string
                    val noteKey = if (needsShears)
                        "item.ic2_120.animalmatron.tooltip.extra_product_shears"
                    else
                        "item.ic2_120.animalmatron.tooltip.extra_product_auto"
                    val note = Component.translatable(noteKey).string
                    lines.add(Component.literal("  ${animalName} → ${productName} ${note}").withStyle(ChatFormatting.DARK_GRAY))
                }
            } else {
                lines.add(Component.translatable("item.ic2_120.animalmatron.tooltip").withStyle(ChatFormatting.GRAY))
            }
        }
    }
}
