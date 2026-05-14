package com.proxwian.mobstackeradv;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;

record StackKey(ResourceLocation entityId, boolean baby, DyeColor sheepColor, boolean sheepSheared) {
    static StackKey of(Mob mob) {
        boolean baby = mob instanceof AgeableMob ageableMob && ageableMob.isBaby();
        DyeColor color = mob instanceof Sheep sheep ? sheep.getColor() : null;
        boolean sheared = mob instanceof Sheep sheep && sheep.isSheared();
        return new StackKey(EntityType.getKey(mob.getType()), baby, color, sheared);
    }
}
