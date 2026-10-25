package ic2_120.mixin.client;

import ic2_120.content.block.TransformerBlock;
import ic2_120.content.block.machines.TransformerBlockEntity;
import ic2_120.content.sync.TransformerSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 HUD 上显示指向的变压器方块的当前模式（升压/降压）。
 */
@Mixin(Gui.class)
public abstract class TransformerTooltipMixin {
    @Shadow
    @Final
    private Minecraft client;

    /**
     * 是否显示变压器模式 tooltip 的开关
     * 通过 TransformerTooltipConfig 来控制
     */
    private static boolean showTransformerTooltip = false;

    /**
     * 在 HUD 渲染后，检查玩家指向的方块是否为变压器，如果是则显示模式信息。
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void renderTransformerMode(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!showTransformerTooltip) {
            return;
        }

        if (client.getDebugOverlay().showDebugScreen() || client.options.hideGui) {
            return;
        }

        // 检查玩家指向的方块
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) client.hitResult;
        var world = client.level;
        if (world == null) return;

        var blockPos = blockHit.getBlockPos();
        var blockState = world.getBlockState(blockPos);

        // 检查是否为变压器方块
        if (!(blockState.getBlock() instanceof TransformerBlock)) {
            return;
        }

        // 获取方块实体
        var blockEntity = world.getBlockEntity(blockPos);
        if (!(blockEntity instanceof TransformerBlockEntity transformerBE)) {
            return;
        }

        // 获取当前模式
        var mode = transformerBE.getSyncedMode();
        Component modeText;
        if (mode == TransformerSync.Mode.STEP_UP) {
            modeText = Component.literal("升压模式 (低→高)").withStyle(ChatFormatting.GREEN);
        } else {
            modeText = Component.literal("降压模式 (高→低)").withStyle(ChatFormatting.RED);
        }

        // 在屏幕上方绘制模式文本
        var windowHeight = client.getWindow().getGuiScaledHeight();
        var textRenderer = client.font;

        // 在准星上方显示
        var y = windowHeight / 2 - 20;
        var x = client.getWindow().getGuiScaledWidth() / 2 - textRenderer.width(modeText) / 2;

        context.drawString(textRenderer, modeText, x, y, 0xFFFFFFFF, true);
    }
}
