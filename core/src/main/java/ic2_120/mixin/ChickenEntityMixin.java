package ic2_120.mixin;

import ic2_120.content.block.AnimalmatronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
		if (chicken.eggLayTime <= 1 && ic2_120$isInsideAnimalmatronRange(chicken)) {
			chicken.eggLayTime = ThreadLocalRandom.current().nextInt(6000) + 6000;
		}
	}

	@Unique
	private boolean ic2_120$isInsideAnimalmatronRange(ChickenEntity entity) {
		if (!(entity.getWorld() instanceof ServerWorld world)) {
			return false;
		}
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
