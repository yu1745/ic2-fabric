package ic2_120.mixin;

import ic2_120.content.block.AnimalmatronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 鸡实体 Mixin
 *
 * 实现：
 * 1. 阻止牲畜监管机范围内的鸡自然下蛋（蛋由监管机统一收集）
 */
@Mixin(ChickenEntity.class)
public class ChickenEntityMixin {

	/**
	 * 拦截鸡下蛋的 dropItem 调用。
	 * 若鸡处于牲畜监管机范围内，阻止蛋自然掉落（由监管机收集）。
	 * eggLayTime 计时器仍正常重置，不影响鸡的产蛋周期。
	 */
	@Redirect(
		method = "tickMovement",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;dropItem(Lnet/minecraft/item/ItemConvertible;)Lnet/minecraft/entity/ItemEntity;")
	)
	private ItemEntity ic2_120$preventEggLay(net.minecraft.entity.Entity instance, ItemConvertible item) {
		ChickenEntity chicken = (ChickenEntity) (Object) this;
		if (ic2_120$isInsideAnimalmatronRange(chicken)) {
			return null;
		}
		return instance.dropItem(item);
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
