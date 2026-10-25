package ic2_120.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Supplies registry keys to legacy no-argument item constructors. */
public final class ItemConstructionId26 {
    public static final ThreadLocal<ResourceKey<Item>> CURRENT = new ThreadLocal<>();

    private ItemConstructionId26() {}
}
