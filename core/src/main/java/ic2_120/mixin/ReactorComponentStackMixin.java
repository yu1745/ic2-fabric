package ic2_120.mixin;

import ic2_120.content.reactor.AbstractDamageableReactorComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 反应堆组件（带寿命/热容的器件）的可堆叠性修复。
 *
 * <p>vanilla 1.20.1 中 {@link Item.Settings#maxDamage} 会把 maxCount 强制为 1，
 * 而 {@link Item#getMaxCount()} 是 final 方法，无法在 Item 子类里覆盖；
 * 同时 {@link ItemStack#isStackable()} 对「可损耗且已损耗」的物品恒为 false。
 * 这导致所有 AbstractDamageableReactorComponent 器件（燃料棒、散热片、冷却单元、
 * 热交换器、冷凝器等）即使寿命/热量完全相同也永远不能堆叠。
 *
 * <p>这里只放开两条「上限」限制：
 * <ul>
 *   <li>getMaxCount() → {@link AbstractDamageableReactorComponent#MAX_STACK_SIZE}（64）</li>
 *   <li>isStackable() → true（不再因已损耗而禁止合并）</li>
 * </ul>
 *
 * <p>合并本身仍由 vanilla {@link ItemStack#canCombine} 把关：要求 item 相同且
 * NBT 完全一致（含 "use"/"Damage" 标签）。因此**寿命（热量）相同的组件可以堆叠，
 * 寿命不同的组件依然不会堆叠**。AE2 模糊卡等按 vanilla 耐久工作的外部 mod 不受影响。
 */
@Mixin(ItemStack.class)
public abstract class ReactorComponentStackMixin {

	@Shadow
	public abstract Item getItem();

	@Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
	private void ic2$reactorComponentGetMaxCount(CallbackInfoReturnable<Integer> cir) {
		if (this.getItem() instanceof AbstractDamageableReactorComponent) {
			cir.setReturnValue(AbstractDamageableReactorComponent.MAX_STACK_SIZE);
		}
	}

	@Inject(method = "isStackable", at = @At("HEAD"), cancellable = true)
	private void ic2$reactorComponentIsStackable(CallbackInfoReturnable<Boolean> cir) {
		if (this.getItem() instanceof AbstractDamageableReactorComponent) {
			cir.setReturnValue(true);
		}
	}
}
