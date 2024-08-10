package com.nyasha.inject.mixin.net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import com.nyasha.Nyasha;
import com.nyasha.events.EventPlayerMove;
import com.nyasha.events.EventPostPlayerUpdate;
import com.nyasha.events.EventPrePlayerUpdate;
import com.nyasha.util.IMinecraftMixin;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author yuxiangll
 * @since 2024/8/9 下午8:03
 * IntelliJ IDEA
 */

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity implements IMinecraftMixin {


    @Unique
    private boolean updateLock = false;

    @Shadow
    protected abstract void sendMovementPackets();



    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onPlayerTick(CallbackInfo ci){
        if (mc.player == null) return;
        Nyasha.INSTANCE.getEventBus().post(new EventPrePlayerUpdate());
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void move$Inject$HEAD(MovementType type, Vec3d movement, CallbackInfo p_Info) {
        EventPlayerMove eventPlayerMove = new EventPlayerMove(type, movement.x, movement.y, movement.z);
        Nyasha.INSTANCE.getEventBus().post(eventPlayerMove);
        if (eventPlayerMove.isCancelled())
        {
            super.move(type, new Vec3d(eventPlayerMove.getX(), eventPlayerMove.getY(), eventPlayerMove.getZ()));
            p_Info.cancel();
        }
    }


    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void PostUpdateHook(CallbackInfo info) {
        if(mc.player == null || mc.world == null) return;
        if (updateLock) {
            return;
        }
        EventPostPlayerUpdate playerUpdateEvent = new EventPostPlayerUpdate();
        Nyasha.INSTANCE.getEventBus().post(playerUpdateEvent);
        if (playerUpdateEvent.isCancelled()) {
            info.cancel();
            if (playerUpdateEvent.getIterations() > 0) {
                for (int i = 0; i < playerUpdateEvent.getIterations(); i++) {
                    updateLock = true;
                    tick();
                    updateLock = false;
                    sendMovementPackets();
                }
            }
        }
    }

}
