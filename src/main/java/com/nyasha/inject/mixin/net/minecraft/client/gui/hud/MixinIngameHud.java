package com.nyasha.inject.mixin.net.minecraft.client.gui.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nyasha.Nyasha;
import com.nyasha.events.EventRenderGameOverlay;
import com.nyasha.module.imp.render.SwardBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author yuxiangll
 * @since 2024/8/9 下午8:08
 * IntelliJ IDEA
 */


@Mixin(InGameHud.class)
public abstract class MixinIngameHud {

    @Inject(at = @At(value = "RETURN"), method = "render", cancellable = true)
    public void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EventRenderGameOverlay eventRenderGameOverlay = new EventRenderGameOverlay(context, tickCounter);
        Nyasha.INSTANCE.getEventBus().post(eventRenderGameOverlay);
        if (eventRenderGameOverlay.isCancelled()) ci.cancel();
    }



    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;"), method = "renderHotbar")
    public ItemStack swordblocking$hideOffHandSlot(ItemStack original) {
        return (SwardBlock.INSTANCE.getEnable() && SwardBlock.INSTANCE.getHideOffhandSlot() && original.getItem() instanceof ShieldItem) ? ItemStack.EMPTY : original;
    }

}