package ic2_120.compat

import ic2_120.compat.recipeManager
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.Level

/** 26.2 exposes the server recipe manager through ServerLevel.recipeAccess(). */
val Level.recipeManager: RecipeManager
    get() = (this as ServerLevel).recipeAccess()
