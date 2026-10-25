package ic2_120.client

import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

object Ic2KeyCategory {
    val category: KeyMapping.Category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ic2_120", "ic2"))
}
