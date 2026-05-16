package com.proxwian.mobstackeradv;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MobConversionEvents {
    private static final int PIG_LIGHTNING_CONVERSION_LOCK_TICKS = 20;

    private MobConversionEvents() {
    }

    public static boolean handlePigLightningConversion(Pig pig, ServerLevel level) {
        int count = MobStackerData.getStackCount(pig);
        if (level.getDifficulty() == Difficulty.PEACEFUL || count <= 1 || !MobStackerData.canStack(pig)) {
            return false;
        }

        long gameTime = level.getGameTime();
        long lockedUntil = pig.getPersistentData().getLong(MobStackerTags.PIG_LIGHTNING_CONVERSION_UNTIL);
        if (lockedUntil > gameTime) {
            return true;
        }

        ZombifiedPiglin piglin = EntityType.ZOMBIFIED_PIGLIN.create(level);
        if (piglin == null) {
            return false;
        }

        piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        piglin.moveTo(pig.getX(), pig.getY(), pig.getZ(), pig.getYRot(), pig.getXRot());
        piglin.setNoAi(pig.isNoAi());
        piglin.setBaby(pig.isBaby());
        piglin.setPersistenceRequired();
        level.addFreshEntity(piglin);

        int remaining = count - 1;
        MobStackerData.setStackCount(pig, remaining);
        MobStackerData.clampStackState(pig);
        pig.getPersistentData().putLong(MobStackerTags.PIG_LIGHTNING_CONVERSION_UNTIL,
                gameTime + PIG_LIGHTNING_CONVERSION_LOCK_TICKS);
        MobStackerData.refreshName(pig);
        return true;
    }

    public static void afterPigLightningConversion(Pig pig, LivingEntity outcome, ServerLevel level) {
        int count = MobStackerData.getStackCount(pig);
        if (count <= 1 || !MobStackerData.canStack(pig)) {
            return;
        }

        if (pig.getPersistentData().getBoolean(MobStackerTags.MANAGED_NAME)) {
            outcome.setCustomName(null);
            outcome.setCustomNameVisible(false);
        }

        spawnRemainingPigs(pig, level, count - 1);
    }

    private static void spawnRemainingPigs(Pig source, ServerLevel level, int count) {
        if (count < MobStackerConfig.MINIMUM_GROUP_SIZE.get()) {
            MobCopies.spawnVanillaCopiesFromStack(source, level, count);
            return;
        }

        Mob replacement = MobCopies.copyMob(source, level);
        if (replacement == null) {
            return;
        }

        MobStackerData.setStackCount(replacement, count);
        MobStackerData.clampStackState(replacement);
        MobStackerData.refreshName(replacement);
        level.addFreshEntityWithPassengers(replacement);
    }
}
