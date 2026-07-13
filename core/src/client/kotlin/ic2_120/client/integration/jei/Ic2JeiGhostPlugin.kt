package ic2_120.client.integration.jei

import ic2_120.client.screen.FluidUpgradeScreen
import ic2_120.client.screen.PumpAttachmentScreen
import ic2_120.content.network.NetworkManager
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.registry.ClassScanner
import io.netty.buffer.Unpooled
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.fabric.constants.FabricTypes
import mezz.jei.api.gui.handlers.IGhostIngredientHandler
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes
import mezz.jei.api.ingredients.ITypedIngredient
import mezz.jei.api.registration.IGuiHandlerRegistration
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.Rect2i
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

@JeiPlugin
class Ic2JeiGhostPlugin : IModPlugin {
    override fun getPluginUid(): Identifier = Identifier("ic2_120", "ghost_filters")

    override fun registerGuiHandlers(registration: IGuiHandlerRegistration) {
        registration.addGhostIngredientHandler(
            PumpAttachmentScreen::class.java,
            FluidFilterGhostHandler { it.ghostFilterArea() }
        )
        registration.addGhostIngredientHandler(
            FluidUpgradeScreen::class.java,
            FluidFilterGhostHandler { it.ghostFilterArea() }
        )
    }

    private class FluidFilterGhostHandler<T : Screen>(
        private val areaProvider: (T) -> Rect2i
    ) : IGhostIngredientHandler<T> {
        override fun <I> getTargetsTyped(
            gui: T,
            ingredient: ITypedIngredient<I>,
            doStart: Boolean
        ): List<IGhostIngredientHandler.Target<I>> {
            val fluid = resolveFluid(ingredient) ?: return emptyList()
            return listOf(object : IGhostIngredientHandler.Target<I> {
                override fun getArea(): Rect2i = areaProvider(gui)

                override fun accept(ingredient: I) {
                    sendFluidFilter(fluid)
                }
            })
        }

        override fun onComplete() = Unit

        private fun <I> resolveFluid(ingredient: ITypedIngredient<I>): Fluid? {
            val fluid = JeiFluidIngredientResolver.resolve(ingredient)
            if (fluid != null) return fluid

            val stack = VanillaTypes.ITEM_STACK
                .castIngredient(ingredient.ingredient)
                .orElse(null)
                ?: return null
            return FluidPipeUpgradeComponent.readFluidFromItemStack(stack)
        }

        private fun sendFluidFilter(fluid: Fluid) {
            if (fluid == Fluids.EMPTY) return
            val buf = PacketByteBuf(Unpooled.buffer())
            buf.writeIdentifier(Registries.FLUID.getId(fluid))
            ClientPlayNetworking.send(NetworkManager.SET_FLUID_FILTER_PACKET, buf)
        }
    }

    private fun interface JeiFluidIngredientResolver {
        fun resolve(ingredient: ITypedIngredient<*>): Fluid?

        companion object {
            private val active: JeiFluidIngredientResolver by lazy {
                if (ClassScanner.isSinytraConnectorRuntime()) ForgeJeiFluidIngredientResolver
                else FabricJeiFluidIngredientResolver
            }

            fun resolve(ingredient: ITypedIngredient<*>): Fluid? = active.resolve(ingredient)
        }
    }

    /** Native Fabric JEI path. Kept isolated so Forge never resolves FabricTypes. */
    private object FabricJeiFluidIngredientResolver : JeiFluidIngredientResolver {
        override fun resolve(ingredient: ITypedIngredient<*>): Fluid? {
            val fluidIngredient = FabricTypes.FLUID_STACK
                .castIngredient(ingredient.ingredient)
                .orElse(null)
                ?: return null
            return FabricTypes.FLUID_STACK.getBase(fluidIngredient)
        }
    }

    /**
     * Forge JEI path used when this Fabric mod is transformed by Sinytra Connector.
     * ForgeTypes cannot be a compile-time dependency of this Fabric source set, so only
     * the field lookup is reflective; fluid extraction uses JEI's loader-neutral API.
     */
    private object ForgeJeiFluidIngredientResolver : JeiFluidIngredientResolver {
        @Suppress("UNCHECKED_CAST")
        private val fluidStackType: IIngredientTypeWithSubtypes<Fluid, Any>? by lazy {
            runCatching {
                Class.forName(
                    "mezz.jei.api.forge.ForgeTypes",
                    true,
                    Ic2JeiGhostPlugin::class.java.classLoader
                ).getField("FLUID_STACK").get(null) as IIngredientTypeWithSubtypes<Fluid, Any>
            }.getOrNull()
        }

        override fun resolve(ingredient: ITypedIngredient<*>): Fluid? {
            val type = fluidStackType ?: return null
            val fluidIngredient = type.castIngredient(ingredient.ingredient)
                .orElse(null)
                ?: return null
            return type.getBase(fluidIngredient)
        }
    }
}
