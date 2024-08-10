package com.nyasha.inject.mixin.net.minecraft.client.gui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.nyasha.util.IMinecraftMixin;
import com.nyasha.util.render.Render2DEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author yuxiangll
 * @since 2024/8/9 下午8:14
 * IntelliJ IDEA
 */

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay implements IMinecraftMixin {
    @Final
    @Shadow
    private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime = -1L;
    @Shadow private long reloadStartTime = -1L;
    @Final @Shadow private ResourceReload reload;
    @Final @Shadow private Consumer<Optional<Throwable>> exceptionHandler;




    @Inject(method = "render",at = @At("HEAD"),cancellable = true)
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();
        renderCustom(context,mouseX,mouseY,delta);
    }

    @Unique
    private void renderCustom(DrawContext context, int mouseX, int mouseY, float delta) {
        int i = mc.getWindow().getScaledWidth();
        int j = mc.getWindow().getScaledHeight();
        long l = Util.getMeasuringTimeMs();
        if (reloading && reloadStartTime == -1L) {
            reloadStartTime = l;
        }

        float f = reloadCompleteTime > -1L ? (float) (l - reloadCompleteTime) / 1000.0F : -1.0F;
        float g = reloadStartTime > -1L ? (float) (l - reloadStartTime) / 500.0F : -1.0F;
        float h;
        int k;
        if (f >= 1.0F) {
            if (mc.currentScreen != null)
                mc.currentScreen.render(context, 0, 0, delta);

            k = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            context.fill(0, 0, i, j, withAlpha(new Color(0x2D292C).getRGB(), k));
            h = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (reloading) {
            if (mc.currentScreen != null && g < 1.0F)
                mc.currentScreen.render(context, mouseX, mouseY, delta);

            k = MathHelper.ceil(MathHelper.clamp((double) g, 0.15, 1.0) * 255.0);
            context.fill(0, 0, i, j, withAlpha(new Color(0x99988594, true).getRGB(), k));
            h = MathHelper.clamp(g, 0.0F, 1.0F);
        } else {
            k = new Color(0x2D292C).getRGB();
            float m = (float) (k >> 16 & 255) / 255.0F;
            float n = (float) (k >> 8 & 255) / 255.0F;
            float o = (float) (k & 255) / 255.0F;
            GlStateManager._clearColor(m, n, o, 1.0F);
            GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);
            h = 1.0F;
        }

        k = (int) ((double) context.getScaledWindowWidth() * 0.5);
        int p = (int) ((double) context.getScaledWindowHeight() * 0.5);


        float width = mc.getWindow().getScaledWidth()*(1-progress);
        Render2DEngine.INSTANCE.drawRound(context.getMatrices(), (float) mc.getWindow().getScaledWidth() /2-width/2, (float) mc.getWindow().getScaledHeight() /2,width,5F,2F, new Color(255,255,255,(int)(255*(1-progress))));
        float t = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + t * 0.050000012F, 0.0F, 1.0F);


        if (f >= 2.0F) {
            mc.setOverlay(null);
        }

        if (reloadCompleteTime == -1L && reload.isComplete() && (!reloading || g >= 2.0F)) {
            try {
                reload.throwException();
                exceptionHandler.accept(Optional.empty());
            } catch (Throwable var23) {
                exceptionHandler.accept(Optional.of(var23));
            }

            reloadCompleteTime = Util.getMeasuringTimeMs();
            if (mc.currentScreen != null) {
                mc.currentScreen.init(
                        mc,
                        mc.getWindow().getScaledWidth(),
                        mc.getWindow().getScaledHeight());
            }
        }
    }




    @Unique
    private static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

}
