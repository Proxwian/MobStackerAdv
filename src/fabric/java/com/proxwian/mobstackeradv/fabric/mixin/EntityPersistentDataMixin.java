package com.proxwian.mobstackeradv.fabric.mixin;

import com.proxwian.mobstackeradv.MobStackerAdv;
import com.proxwian.mobstackeradv.fabric.MobStackerDataHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityPersistentDataMixin implements MobStackerDataHolder {
    @Unique
    private CompoundTag mobstackeradv$persistentData;

    @Override
    public CompoundTag mobstackeradv$getPersistentData() {
        if (mobstackeradv$persistentData == null) {
            mobstackeradv$persistentData = new CompoundTag();
        }
        return mobstackeradv$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void mobstackeradv$savePersistentData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (mobstackeradv$persistentData != null && !mobstackeradv$persistentData.isEmpty()) {
            tag.put(MobStackerAdv.MOD_ID, mobstackeradv$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void mobstackeradv$readPersistentData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(MobStackerAdv.MOD_ID, 10)) {
            mobstackeradv$persistentData = tag.getCompound(MobStackerAdv.MOD_ID).copy();
        }
    }
}
