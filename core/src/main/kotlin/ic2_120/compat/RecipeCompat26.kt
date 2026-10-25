package ic2_120.compat

import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.predicates.ItemPredicate
import net.minecraft.advancements.triggers.Criterion
import net.minecraft.advancements.triggers.InventoryChangeTrigger
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.ItemLike

fun has(item: ItemLike): Criterion<InventoryChangeTrigger.TriggerInstance> =
    RecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, item))

fun has(tag: TagKey<Item>): Criterion<InventoryChangeTrigger.TriggerInstance> =
    RecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, tag))

fun RecipeBuilder.save(output: RecipeOutput, id: Identifier) =
    save(output, ResourceKey.create(Registries.RECIPE, id))

fun RecipeOutput.accept(id: Identifier, recipe: Recipe<*>, advancement: AdvancementHolder?) =
    accept(ResourceKey.create(Registries.RECIPE, id), recipe, advancement)
