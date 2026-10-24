package ic2_120.mixin;

import ic2_120.content.block.AnimalmatronBlock;
import net.minecraft.entity.passive.ChickenEntity;
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
@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin {

	/**
	 * 在 tickMovement 开头注入检查。
	 * 若鸡处于牲畜监管机范围内且即将下蛋（eggLayTime <= 1），
	 * 重置 eggLayTime 阻止蛋自然掉落，蛋由监管机统一收集。
	 */
	@Inject(
		method = "tickMovement",
		at = @At("HEAD")
	)
	private void ic2_120$preventEggLay(CallbackInfo ci) {
		ChickenEntity chicken = (ChickenEntity) (Object) this;
		// 统一走 AnimalmatronBlock.isManaged：与 BE 扫描同几何 + 要求 ACTIVE（P1/P4）。
		if (chicken.eggLayTime <= 1 && AnimalmatronBlock.isManaged(chicken)) {
			chicken.eggLayTime = ThreadLocalRandom.current().nextInt(6000) + 6000;
		}
	}
}
