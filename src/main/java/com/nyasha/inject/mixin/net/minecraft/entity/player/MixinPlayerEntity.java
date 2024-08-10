package com.nyasha.inject.mixin.net.minecraft.entity.player;


import com.nyasha.Nyasha;
import com.nyasha.events.EventPlayerJump;
import com.nyasha.events.EventPlayerTravel;
import com.nyasha.events.EventPostAttack;
import com.nyasha.util.IMinecraftMixin;
import com.nyasha.module.imp.combat.Reach;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author yuxiangll
 * @since 2024/8/9 下午8:11
 * IntelliJ IDEA
 */
@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements IMinecraftMixin {

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }


    @Inject(method = {"travel"}, at = {@At("HEAD")}, cancellable = true)
    private void travel(Vec3d movement, CallbackInfo info) {
        EventPlayerTravel eventPlayerTravel = new EventPlayerTravel(movement.getX(), movement.getY(), movement.getZ());
        Nyasha.INSTANCE.getEventBus().post(eventPlayerTravel);
        if (eventPlayerTravel.isCancelled()) info.cancel();
    }

    @Inject(method = {"jump"}, at = {@At("HEAD")})
    private void jump(CallbackInfo callback) {
        if (mc.player == null) return;
        Nyasha.INSTANCE.getEventBus().post(new EventPlayerJump());
    }



    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void attackAHook2(Entity target, CallbackInfo ci) {
        final EventPostAttack event = new EventPostAttack(target);
        Nyasha.INSTANCE.getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    public void getBlockInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
        if (Reach.INSTANCE.getEnable()){
            cir.setReturnValue((Double) Reach.INSTANCE.getBlockRange().getValue());
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    public void getEntityInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
        if (Reach.INSTANCE.getEnable()) {
            cir.setReturnValue((double) Reach.INSTANCE.getAttackRange().getValue());
        }
    }
}

