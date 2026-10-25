package ic2_120.compat

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.equipment.Equippable
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorType

/** Keeps the mod's armor subclasses while 26.2 stores armor behavior in item components. */
open class ArmorItem(
    val material: Holder<ArmorMaterial>,
    val type: Type,
    properties: Item.Properties
) : Item(
    properties
        .attributes(material.value().createAttributes(type.native))
        .enchantable(material.value().enchantmentValue())
        .component(
            DataComponents.EQUIPPABLE,
            Equippable.builder(type.slot)
                .setEquipSound(material.value().equipSound())
                .setAsset(material.value().assetId())
                .build()
        )
) {
    enum class Type(val native: ArmorType) {
        HELMET(ArmorType.HELMET),
        CHESTPLATE(ArmorType.CHESTPLATE),
        LEGGINGS(ArmorType.LEGGINGS),
        BOOTS(ArmorType.BOOTS);

        val slot get() = native.slot
    }
}
