package com.proxwian.mobstackeradv.fabric.mixin;

import com.proxwian.mobstackeradv.MobStackingEvents;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
abstract class MobTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void mobstackeradv$tick(CallbackInfo ci) {
        MobStackingEvents.onMobTick((Mob) (Object) this);
    }
}
