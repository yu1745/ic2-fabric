package ic2_120.mixin;

import ic2_120.content.block.AnimalmatronBlock;
import ic2_120.content.entity.AnimalFoodMapping;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 被动实体 Mixin
 *
 * 实现：
 * 1. 阻止牲畜监管机范围内动物的自然生长
 */
@Mixin(AgeableMob.class)
public class PassiveEntityMixin {

    @Unique
    private boolean ic2_120$blockingNaturalGrowth = false;

    /**
     * 在自然年龄推进的 tick 期间打开标记，只拦截该路径下的 setBreedingAge。
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void ic2AnimalmatronStartNaturalGrowthGuard(CallbackInfo ci) {
        AgeableMob entity = (AgeableMob) (Object) this;
        this.ic2_120$blockingNaturalGrowth =
                entity.level() instanceof net.minecraft.server.level.ServerLevel &&
                        entity.isBaby() &&
                        AnimalFoodMapping.isManagedAnimal(entity);
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void ic2AnimalmatronStopNaturalGrowthGuard(CallbackInfo ci) {
        this.ic2_120$blockingNaturalGrowth = false;
    }

    /**
     * 仅阻止 tickMovement 内部把幼崽年龄自然推进到 0；手动 growUp/setBaby(false) 不受影响。
     */
    @Inject(method = "setAge", at = @At("HEAD"), cancellable = true)
    private void ic2AnimalmatronPreventNaturalGrowth(int age, CallbackInfo ci) {
        if (!this.ic2_120$blockingNaturalGrowth) {
            return;
        }

        AgeableMob entity = (AgeableMob) (Object) this;

        if (age <= entity.getAge()) {
            return;
        }

        // 统一走 AnimalmatronBlock.isManaged：与 BE 扫描同几何 + 要求 ACTIVE（P1/P4）。
        // 断电/停机时不拦截，幼崽可自然生长。
        if (AnimalmatronBlock.isManaged(entity)) {
            ci.cancel();
        }
    }
}
