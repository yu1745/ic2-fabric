package ic2_120.mixin;

import ic2_120.content.block.Ic2ScaffoldBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 原版仅允许 vanilla 脚手架在按住潜行时保留向下速度；其他 climbable 方块会按梯子处理并悬停。
 * IC2 脚手架拥有同样的穿行碰撞形状，因此也必须走原版脚手架的下降例外。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityScaffoldMixin {

    @Redirect(
        method = "applyClimbingSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"
        )
    )
    private boolean ic2_120$treatIc2ScaffoldAsScaffolding(BlockState state, Block vanillaScaffolding) {
        return state.isOf(vanillaScaffolding) || state.getBlock() instanceof Ic2ScaffoldBlock;
    }
}
