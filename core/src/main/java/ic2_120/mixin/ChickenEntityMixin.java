package ic2_120.mixin;

import ic2_120.content.block.AnimalmatronBlock;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 鸡实体 Mixin
 *
 * 实现：
 * 1. 阻止牲畜监管机范围内的鸡自然下蛋（蛋由监管机统一收集）
 */
@Mixin(Chicken.class)
public abstract class ChickenEntityMixin {

	/**
	 * 在 tickMovement 开头注入检查。
	 * 若鸡处于牲畜监管机范围内且即将下蛋（eggTime <= 1），
	 * 重置 eggTime 阻止蛋自然掉落，蛋由监管机统一收集。
	 */
	@Inject(
		method = "aiStep",
		at = @At("HEAD")
	)
	private void ic2_120$preventEggLay(CallbackInfo ci) {
		Chicken chicken = (Chicken) (Object) this;
		// 统一走 AnimalmatronBlock.isManaged：与 BE 扫描同几何 + 要求 ACTIVE（P1/P4）。
		if (chicken.eggTime <= 1 && AnimalmatronBlock.isManaged(chicken)) {
			chicken.eggTime = ThreadLocalRandom.current().nextInt(6000) + 6000;
		}
	}
}
