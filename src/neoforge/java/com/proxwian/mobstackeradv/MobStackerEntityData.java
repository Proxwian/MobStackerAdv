package com.proxwian.mobstackeradv;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

final class MobStackerEntityData {
    private MobStackerEntityData() {
    }

    static CompoundTag get(LivingEntity entity) {
        return entity.getPersistentData();
    }
}
