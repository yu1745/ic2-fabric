package ic2_120.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 让煤粉掉入水中后转化为湿煤粉。 */
@Mixin(ItemEntity.class)
public abstract class CoalDustWaterTransformMixin {
    @Unique
    private static final ResourceLocation IC2_COAL_DUST = new ResourceLocation.of("ic2_120", "coal_dust");

    @Unique
    private static final ResourceLocation IC2_COAL_FUEL_DUST = new ResourceLocation.of("ic2_120", "coal_fuel_dust");

    @Unique
    private int ic2$coalWaterTransformTicks;

    @Shadow
    public abstract ItemStack getStack();

    @Shadow
    public abstract void setStack(ItemStack stack);

    @Inject(method = "tick", at = @At("TAIL"))
    private void ic2$transformCoalDustInWater(CallbackInfo ci) {
        ItemEntity entity = (ItemEntity) (Object) this;
        Level world = entity.getWorld();

        if (world.isClient || entity.isRemoved()) {
            return;
        }

        ItemStack stack = this.getStack();
        if (stack.isEmpty() || stack.getItem() != BuiltInRegistries.ITEM.get(IC2_COAL_DUST)) {
            this.ic2$coalWaterTransformTicks = 0;
            return;
        }

        int x = Mth.floor(entity.getX());
        int y = Mth.floor((entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0D);
        int z = Mth.floor(entity.getZ());
        BlockPos fluidPos = new BlockPos(x, y, z);
        if (!world.getFluidState(fluidPos).isOf(Fluids.WATER)
                && !world.getFluidState(fluidPos).isOf(Fluids.FLOWING_WATER)) {
            this.ic2$coalWaterTransformTicks = 0;
            return;
        }

        if (++this.ic2$coalWaterTransformTicks < 60) {
            return;
        }
        this.ic2$coalWaterTransformTicks = 0;

        ItemStack output = new ItemStack(BuiltInRegistries.ITEM.get(IC2_COAL_FUEL_DUST));
        stack.shrink(1);
        if (stack.isEmpty()) {
            this.setStack(output);
        } else {
            this.setStack(stack);
            ItemEntity outputEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), output);
            outputEntity.setPickupDelay(10);
            world.spawnEntity(outputEntity);
        }
    }
}
