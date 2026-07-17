package ic2_120.mixin;

import ic2_120.content.block.Ic2ScaffoldBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版仅允许 vanilla 脚手架在按住潜行时保留向下速度；其他 climbable 方块会按梯子处理并悬停。
 * IC2 脚手架拥有同样的穿行碰撞形状，因此也必须保留下降速度。
 *
 * <p>不能 Redirect {@code BlockState.is(Blocks.SCAFFOLDING)}：Forge 会把该调用替换为
 * {@code BlockState.isScaffolding(entity)}，导致 Connector 环境中注入点不存在。返回处修正不依赖
 * Fabric/Forge 的该处实现差异。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityScaffoldMixin {

    @Inject(
        method = "applyClimbingSpeed",
        at = @At("RETURN"),
        cancellable = true
    )
    private void ic2_120$preserveScaffoldDescent(
        Vec3d velocity,
        CallbackInfoReturnable<Vec3d> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Vec3d result = cir.getReturnValue();
        if (velocity.y < 0.0
            && result.y == 0.0
            && entity.getBlockStateAtPos().getBlock() instanceof Ic2ScaffoldBlock) {
            cir.setReturnValue(new Vec3d(
                result.x,
                Math.max(velocity.y, -0.15000000596046448),
                result.z
            ));
        }
    }
}
