package ic2_120.mixin;

import ic2_120.registry.BlockConstructionId26;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.Properties.class)
public abstract class BlockPropertiesIdMixin {
    @Shadow private ResourceKey<Block> id;

    @Inject(method = "effectiveDrops", at = @At("HEAD"))
    private void ic2$setConstructionId(CallbackInfoReturnable<?> cir) {
        if (id == null) {
            ResourceKey<Block> key = BlockConstructionId26.CURRENT.get();
            if (key != null) ((BlockBehaviour.Properties) (Object) this).setId(key);
        }
    }
}
