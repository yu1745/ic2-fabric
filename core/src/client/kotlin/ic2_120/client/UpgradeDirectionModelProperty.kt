package ic2_120.client

import com.mojang.serialization.MapCodec
import ic2_120.Ic2_120
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.item.ItemStack

/** Chooses the last configured side in the old override priority order. */
object UpgradeDirectionModelProperty : RangeSelectItemModelProperty {
    private val ID = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "upgrade_direction")
    private val CODEC: MapCodec<UpgradeDirectionModelProperty> = MapCodec.unit(this)
    private val SIDES = listOf(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

    fun register() {
        RangeSelectItemModelProperties.ID_MAPPER.put(ID, CODEC)
    }

    override fun get(stack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float {
        val id = BuiltInRegistries.ITEM.getKey(stack.item).path
        val configured = if (id.startsWith("fluid_"))
            FluidPipeUpgradeComponent.readDirections(stack)
        else
            EjectorUpgradeComponent.readDirections(stack)
        return (SIDES.indexOfLast { it in configured } + 1).toFloat()
    }

    override fun type(): MapCodec<UpgradeDirectionModelProperty> = CODEC
}
