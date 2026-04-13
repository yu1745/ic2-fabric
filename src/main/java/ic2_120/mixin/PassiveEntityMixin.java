package ic2_120.mixin;

import ic2_120.content.block.AnimalmatronBlock;
import ic2_120.content.entity.AnimalFoodMapping;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
@Mixin(PassiveEntity.class)
public class PassiveEntityMixin {

    @Unique
    private boolean ic2_120$blockingNaturalGrowth = false;

    /**
     * 在自然年龄推进的 tick 期间打开标记，只拦截该路径下的 setBreedingAge。
     */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void ic2AnimalmatronStartNaturalGrowthGuard(CallbackInfo ci) {
        PassiveEntity entity = (PassiveEntity) (Object) this;
        this.ic2_120$blockingNaturalGrowth =
                entity.getWorld() instanceof ServerWorld &&
                        entity.isBaby() &&
                        AnimalFoodMapping.isManagedAnimal(entity);
    }

    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void ic2AnimalmatronStopNaturalGrowthGuard(CallbackInfo ci) {
        this.ic2_120$blockingNaturalGrowth = false;
    }

    /**
     * 仅阻止 tickMovement 内部把幼崽年龄自然推进到 0；手动 growUp/setBaby(false) 不受影响。
     */
    @Inject(method = "setBreedingAge", at = @At("HEAD"), cancellable = true)
    private void ic2AnimalmatronPreventNaturalGrowth(int age, CallbackInfo ci) {
        if (!this.ic2_120$blockingNaturalGrowth) {
            return;
        }

        PassiveEntity entity = (PassiveEntity) (Object) this;
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }

        if (age <= entity.getBreedingAge()) {
            return;
        }

        if (ic2_120$isInsideAnimalmatronRange(entity, world)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean ic2_120$isInsideAnimalmatronRange(PassiveEntity entity, ServerWorld world) {
        BlockPos entityPos = entity.getBlockPos();
        Box searchBox = new Box(entityPos).expand(4.0);

        int minX = (int) Math.floor(searchBox.minX);
        int minY = (int) Math.floor(searchBox.minY);
        int minZ = (int) Math.floor(searchBox.minZ);
        int maxX = (int) Math.ceil(searchBox.maxX);
        int maxY = (int) Math.ceil(searchBox.maxY);
        int maxZ = (int) Math.ceil(searchBox.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.getBlock() instanceof AnimalmatronBlock) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
