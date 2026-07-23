package ic2_120.integration.jei

import ic2_120.client.screen.FluidUpgradeScreen
import ic2_120.client.screen.ItemUpgradeScreen
import ic2_120.client.screen.PumpAttachmentScreen
import ic2_120.content.network.NetworkManager
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import io.netty.buffer.Unpooled
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.handlers.IGhostIngredientHandler
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes
import mezz.jei.api.ingredients.ITypedIngredient
import mezz.jei.api.registration.IGuiHandlerRegistration
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.util.math.Rect2i
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.network.PacketByteBuf
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

@JeiPlugin
class Ic2JeiGhostPlugin : IModPlugin {
    override fun getPluginUid(): Identifier = Identifier("ic2_120", "ghost_filters")

    override fun registerGuiHandlers(registration: IGuiHandlerRegistration) {
        val fluidIngredientType = registration.jeiHelpers.platformFluidHelper.fluidIngredientType
        registration.addGhostIngredientHandler(
            PumpAttachmentScreen::class.java,
            FluidFilterGhostHandler(fluidIngredientType) { it.ghostFilterArea() }
        )
        registration.addGhostIngredientHandler(
            FluidUpgradeScreen::class.java,
            FluidFilterGhostHandler(fluidIngredientType) { it.ghostFilterArea() }
        )

        registration.addGhostIngredientHandler(
            HandledScreen::class.java,
            ItemFilterGhostHandler()
        )
    }

    private class FluidFilterGhostHandler<T : Screen>(
        private val fluidIngredientType: IIngredientTypeWithSubtypes<Fluid, *>,
        private val areaProvider: (T) -> Rect2i
    ) : IGhostIngredientHandler<T> {
        override fun <I : Any> getTargetsTyped(
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

        private fun <I : Any> resolveFluid(ingredient: ITypedIngredient<I>): Fluid? {
            val fluid = resolveFluidIngredient(ingredient)
            if (fluid != null) return fluid

            val stack = VanillaTypes.ITEM_STACK
                .castIngredient(ingredient.ingredient)
                .orElse(null)
                ?: return null
            return FluidPipeUpgradeComponent.readFluidFromItemStack(stack)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <I : Any> resolveFluidIngredient(ingredient: ITypedIngredient<I>): Fluid? {
            if (ingredient.type !== fluidIngredientType) return null
            val typedFluidIngredientType = fluidIngredientType as IIngredientTypeWithSubtypes<Fluid, I>
            return typedFluidIngredientType.getBase(ingredient.ingredient)
        }

        private fun sendFluidFilter(fluid: Fluid) {
            if (fluid == Fluids.EMPTY) return
            val buf = PacketByteBuf(Unpooled.buffer())
            buf.writeIdentifier(Registries.FLUID.getId(fluid))
            ClientPlayNetworking.send(NetworkManager.SET_FLUID_FILTER_PACKET, buf)
        }
    }

    private class ItemFilterGhostHandler : IGhostIngredientHandler<HandledScreen<*>> {
        override fun <I : Any> getTargetsTyped(
            gui: HandledScreen<*>,
            ingredient: ITypedIngredient<I>,
            doStart: Boolean
        ): List<IGhostIngredientHandler.Target<I>> {
            val itemGui = gui as? ItemUpgradeScreen ?: return emptyList()
            val stack = resolveItem(ingredient) ?: return emptyList()
            return listOf(object : IGhostIngredientHandler.Target<I> {
                override fun getArea(): Rect2i = itemGui.ghostFilterArea()

                override fun accept(ingredient: I) {
                    sendItemFilter(itemGui.ghostFilterSlotIndex, stack)
                }
            })
        }

        override fun onComplete() = Unit

        private fun <I : Any> resolveItem(ingredient: ITypedIngredient<I>): ItemStack? {
            val stack = VanillaTypes.ITEM_STACK.castIngredient(ingredient.ingredient)
                .orElse(null)
                ?: return null
            return stack.takeUnless { it.isEmpty || it.item == net.minecraft.item.Items.AIR }
        }

        private fun sendItemFilter(slotIndex: Int, stack: ItemStack) {
            val buf = PacketByteBuf(Unpooled.buffer())
            buf.writeVarInt(slotIndex)
            buf.writeItemStack(stack)
            ClientPlayNetworking.send(NetworkManager.SET_ITEM_FILTER_PACKET, buf)
        }
    }
}
