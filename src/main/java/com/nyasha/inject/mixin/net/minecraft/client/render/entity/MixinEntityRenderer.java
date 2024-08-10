package com.nyasha.inject.mixin.net.minecraft.client.render.entity;

import com.nyasha.Nyasha;
import com.nyasha.events.EventRenderEntityName;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author yuxiangll
 * @since 2024/8/9 下午8:05
 * IntelliJ IDEA
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    protected void renderLabelIfPresent(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        EventRenderEntityName eventRenderEntityName = new EventRenderEntityName();
        Nyasha.INSTANCE.getEventBus().post(eventRenderEntityName);
        if (eventRenderEntityName.isCancelled()) ci.cancel();
    }

}
