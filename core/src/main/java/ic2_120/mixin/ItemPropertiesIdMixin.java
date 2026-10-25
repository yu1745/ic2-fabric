package ic2_120.mixin;

import ic2_120.registry.ItemConstructionId26;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.Properties.class)
public abstract class ItemPropertiesIdMixin {
    @Shadow private ResourceKey<Item> id;

    @Inject(method = "effectiveDescriptionId", at = @At("HEAD"))
    private void ic2$setConstructionId(CallbackInfoReturnable<String> cir) {
        if (id == null) {
            ResourceKey<Item> key = ItemConstructionId26.CURRENT.get();
            if (key != null) ((Item.Properties) (Object) this).setId(key);
        }
    }
}
