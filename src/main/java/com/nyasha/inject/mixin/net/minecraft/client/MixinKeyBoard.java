package com.nyasha.inject.mixin.net.minecraft.client;

import com.nyasha.Nyasha;
import com.nyasha.events.EventKeyPress;
import com.nyasha.events.EventKeyRelease;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author yuxiangll
 * @since 2024/8/9 下午8:09
 * IntelliJ IDEA
 */

@Mixin(Keyboard.class)
public abstract class MixinKeyBoard {

    @Inject(method = "onKey", at = @At("HEAD"))
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo callback) {
        switch (action) {
            case 0 -> Nyasha.INSTANCE.getEventBus().post(new EventKeyRelease(key,scancode));
            case 1 -> Nyasha.INSTANCE.getEventBus().post(new EventKeyPress(key,scancode));
        }
    }

}