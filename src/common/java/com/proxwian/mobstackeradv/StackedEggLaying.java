package com.proxwian.mobstackeradv;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class StackedEggLaying {
    private StackedEggLaying() {
    }

    static void process(Mob mob) {
        if (!(mob instanceof Chicken chicken) || chicken.isBaby()) {
            return;
        }

        CompoundTag data = chicken.getPersistentData();
        int previousEggTime = data.getInt(MobStackerTags.PREVIOUS_EGG_TIME);
        int currentEggTime = chicken.eggTime;
        int extraEggs = MobStackerData.getStackCount(chicken) - 1;

        if (previousEggTime > 0 && currentEggTime > previousEggTime && extraEggs > 0) {
            spawnItemStacks(chicken, Items.EGG, extraEggs);
        }

        data.putInt(MobStackerTags.PREVIOUS_EGG_TIME, currentEggTime);
    }

    private static void spawnItemStacks(LivingEntity entity, Item item, int count) {
        int remaining = count;
        int maxStackSize = new ItemStack(item).getMaxStackSize();
        while (remaining > 0) {
            int dropCount = Math.min(remaining, maxStackSize);
            entity.spawnAtLocation(new ItemStack(item, dropCount), 0.0F);
            remaining -= dropCount;
        }
    }
}
