package ic2_120.client

import ic2_120.content.entity.AnimalFoodMapping
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.gui.screen.Screen
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

@Environment(EnvType.CLIENT)
object AnimalmatronTooltipHandler {

<<<<<<< HEAD
    private val ANIMALMATRON_ID = Identifier("ic2_120", "animalmatron")
=======
    private val ANIMALMATRON_ID = Identifier.of("ic2_120", "animalmatron")
>>>>>>> backup/1.21.1

    private data class ExtraProductInfo(val entityId: Identifier, val productId: Identifier, val needsShears: Boolean)

    private val EXTRA_PRODUCTS = listOf(
<<<<<<< HEAD
        ExtraProductInfo(Identifier("minecraft", "chicken"), Identifier("minecraft", "egg"), false),
        ExtraProductInfo(Identifier("minecraft", "sheep"), Identifier("minecraft", "white_wool"), true),
    )

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type ->
=======
        ExtraProductInfo(Identifier.of("minecraft", "chicken"), Identifier.of("minecraft", "egg"), false),
        ExtraProductInfo(Identifier.of("minecraft", "sheep"), Identifier.of("minecraft", "white_wool"), true),
    )

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type, tooltip ->
>>>>>>> backup/1.21.1
            val itemId = Registries.ITEM.getId(stack.item)
            if (itemId != ANIMALMATRON_ID) return@register

            if (Screen.hasShiftDown()) {
<<<<<<< HEAD
                type.add(Text.translatable("item.ic2_120.animalmatron.tooltip.food_table").formatted(Formatting.GRAY))
=======
                tooltip.add(Text.translatable("item.ic2_120.animalmatron.tooltip.food_table").formatted(Formatting.GRAY))
>>>>>>> backup/1.21.1
                AnimalFoodMapping.getFoodMap().forEach { (id, food) ->
                    val animalKey = "entity.${id.namespace}.${id.path}"
                    val animalName = Text.translatable(animalKey).string
                    val foodName = Text.translatable(food.first().translationKey).string
<<<<<<< HEAD
                    type.add(Text.literal("  ${animalName}: ${foodName}").formatted(Formatting.DARK_GRAY))
                }
                type.add(Text.translatable("item.ic2_120.animalmatron.tooltip.breeding_limit").formatted(Formatting.DARK_GRAY))

                type.add(Text.translatable("item.ic2_120.animalmatron.tooltip.extra_products").formatted(Formatting.GRAY))
=======
                    tooltip.add(Text.literal("  ${animalName}: ${foodName}").formatted(Formatting.DARK_GRAY))
                }
                tooltip.add(Text.translatable("item.ic2_120.animalmatron.tooltip.breeding_limit").formatted(Formatting.DARK_GRAY))

                tooltip.add(Text.translatable("item.ic2_120.animalmatron.tooltip.extra_products").formatted(Formatting.GRAY))
>>>>>>> backup/1.21.1
                EXTRA_PRODUCTS.forEach { (entityId, productId, needsShears) ->
                    val animalKey = "entity.${entityId.namespace}.${entityId.path}"
                    val animalName = Text.translatable(animalKey).string
                    val productItem = Registries.ITEM.get(productId)
                    val productName = Text.translatable(productItem.translationKey).string
                    val noteKey = if (needsShears)
                        "item.ic2_120.animalmatron.tooltip.extra_product_shears"
                    else
                        "item.ic2_120.animalmatron.tooltip.extra_product_auto"
                    val note = Text.translatable(noteKey).string
<<<<<<< HEAD
                    type.add(Text.literal("  ${animalName} → ${productName} ${note}").formatted(Formatting.DARK_GRAY))
                }
            } else {
                type.add(Text.translatable("item.ic2_120.animalmatron.tooltip").formatted(Formatting.GRAY))
=======
                    tooltip.add(Text.literal("  ${animalName} → ${productName} ${note}").formatted(Formatting.DARK_GRAY))
                }
            } else {
                tooltip.add(Text.translatable("item.ic2_120.animalmatron.tooltip").formatted(Formatting.GRAY))
>>>>>>> backup/1.21.1
            }
        }
    }
}
