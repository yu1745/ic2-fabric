package ic2_120.compat

import net.minecraft.core.HolderSet
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient

/** Build an ingredient from a tag without resolving its contents during registration/datagen. */
fun tagIngredient(tag: TagKey<Item>): Ingredient =
    Ingredient.of(HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag))
