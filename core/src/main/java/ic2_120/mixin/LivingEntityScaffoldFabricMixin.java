package ic2_120.mixin;

import ic2_120.content.block.Ic2ScaffoldBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Fabric uses the vanilla scaffolding block check in applyClimbingSpeed. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityScaffoldFabricMixin {

    @Redirect(
        method = "applyClimbingSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"
        ),
        require = 0
    )
    private boolean ic2_120$treatIc2ScaffoldAsScaffolding(BlockState state, Block vanillaScaffolding) {
        return state.isOf(vanillaScaffolding) || state.getBlock() instanceof Ic2ScaffoldBlock;
    }
}
