package ic2_120.content.item

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.vehicle.boat.Boat
import net.minecraft.world.item.BoatItem
import net.minecraft.world.item.Item

/** Uses vanilla boat placement, collision, spawn configuration, and item consumption. */
open class Ic2BoatItem(
    entityType: EntityType<out Boat>,
    settings: Item.Properties
) : BoatItem(entityType, settings)
