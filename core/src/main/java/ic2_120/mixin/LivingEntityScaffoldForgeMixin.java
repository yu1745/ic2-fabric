package ic2_120.mixin;

import ic2_120.content.block.Ic2ScaffoldBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Forge replaces the vanilla scaffolding block check with BlockState.isScaffolding(Entity). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityScaffoldForgeMixin {

    @Redirect(
        method = "handleOnClimbable",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;isScaffolding(Lnet/minecraft/world/entity/LivingEntity;)Z",
            remap = false
        ),
        require = 0
    )
    private boolean ic2_120$treatIc2ScaffoldAsScaffolding(BlockState state, LivingEntity entity) {
        return state.is(Blocks.SCAFFOLDING) || state.getBlock() instanceof Ic2ScaffoldBlock;
    }
}
