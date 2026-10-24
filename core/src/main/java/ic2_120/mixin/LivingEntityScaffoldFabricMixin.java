package ic2_120.mixin;

import ic2_120.content.block.Ic2ScaffoldBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Fabric uses the vanilla scaffolding block check in applyClimbingSpeed. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityScaffoldFabricMixin {

    @Redirect(
        method = "handleOnClimbable",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
        ),
        require = 0
    )
    private boolean ic2_120$treatIc2ScaffoldAsScaffolding(BlockState state, Block vanillaBlock) {
        return state.is(vanillaBlock) || state.getBlock() instanceof Ic2ScaffoldBlock;
    }
}
