package ic2_120.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/** Supplies the registry key while legacy no-argument block constructors build their Properties. */
public final class BlockConstructionId26 {
    public static final ThreadLocal<ResourceKey<Block>> CURRENT = new ThreadLocal<>();

    private BlockConstructionId26() {}
}
