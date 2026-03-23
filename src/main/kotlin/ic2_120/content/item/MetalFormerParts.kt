package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.recipeId
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.FoodComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.world.World
import java.util.function.Consumer

// ========== 金属成型机制品 ==========

@ModItem(name = "tin_can", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class EmptyTinCanItem : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val tinIngot = TinIngot::class.instance()

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EmptyTinCanItem::class.instance(), 16)
                .pattern("TTT").pattern("T T").pattern("TTT")
                .input('T', tinIngot)
                .criterion(hasItem(tinIngot), conditionsFromItem(tinIngot))
                .offerTo(exporter, EmptyTinCanItem::class.recipeId("from_ingots_8"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EmptyTinCanItem::class.instance(), 4)
                .pattern(" T ").pattern("T T").pattern("TTT")
                .input('T', tinIngot)
                .criterion(hasItem(tinIngot), conditionsFromItem(tinIngot))
                .offerTo(exporter, EmptyTinCanItem::class.recipeId("from_ingots_6"))
        }
    }
}

/**
 * 满锡罐：由空锡罐在固体装罐机内灌装食物获得，也可在宝箱中开出。
 * 右击食用：生命未满时恢复 1 心生命值，生命满时恢复 1 格饥饿值。
 * 食用速度极快，食用后返还空锡罐。
 */
@ModItem(name = "filled_tin_can", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class FilledTinCanItem : Item(
    FabricItemSettings()
        .food(
            FoodComponent.Builder()
                .hunger(2)
                .saturationModifier(0.2f)
                .snack()
                .alwaysEdible()
                .build()
        )
) {

    private val emptyTinCanItem: Item
        get() = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "tin_can"))

    override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
        if (!world.isClient && user is PlayerEntity) {
            val player = user
            if (player.health < player.maxHealth) {
                player.heal(2f)
            } else {
                player.hungerManager.add(2, 0.4f)
            }
        }

        val remainder = ItemStack(emptyTinCanItem)
        if (stack.count > 1) {
            stack.decrement(1)
            if (user is PlayerEntity && !user.inventory.insertStack(remainder)) {
                user.dropItem(remainder, false)
            }
            return stack
        }
        return remainder
    }

    override fun getRecipeRemainder(stack: ItemStack): ItemStack = ItemStack(emptyTinCanItem)
}

@ModItem(name = "small_power_unit", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class SmallPowerUnitItem : Item(FabricItemSettings())

@ModItem(name = "power_unit", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class PowerUnitItem : Item(FabricItemSettings())

@ModItem(name = "fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class EmptyFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "iron_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleIronItem : Item(FabricItemSettings())

@ModItem(name = "steel_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleSteelItem : Item(FabricItemSettings())

@ModItem(name = "coin", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class IndustrialCurrencyItem : Item(FabricItemSettings())
