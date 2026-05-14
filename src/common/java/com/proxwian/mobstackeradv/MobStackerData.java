package com.proxwian.mobstackeradv;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

final class MobStackerData {
    static final int BREEDING_COOLDOWN_TICKS = 6000;

    private MobStackerData() {
    }

    static boolean canStack(Mob mob) {
        if (mob.hasCustomName() && !MobStackerEntityData.get(mob).getBoolean(MobStackerTags.MANAGED_NAME)) {
            return false;
        }

        if (isNoStackLocked(mob)) {
            return false;
        }

        ResourceLocation id = EntityType.getKey(mob.getType());
        return configuredEntityIds().contains(id.toString());
    }

    static int getStackCount(LivingEntity entity) {
        CompoundTag data = MobStackerEntityData.get(entity);
        return Math.max(1, data.getInt(MobStackerTags.STACK_COUNT));
    }

    static void setStackCount(LivingEntity entity, int count) {
        CompoundTag data = MobStackerEntityData.get(entity);
        data.putInt(MobStackerTags.STACK_COUNT, Math.max(1, count));
    }

    static int getLoveCount(LivingEntity entity) {
        return Math.max(0, MobStackerEntityData.get(entity).getInt(MobStackerTags.LOVE_COUNT));
    }

    static void setLoveCount(LivingEntity entity, int count) {
        int clamped = Math.max(0, Math.min(count, getStackCount(entity)));
        CompoundTag data = MobStackerEntityData.get(entity);
        if (clamped == 0) {
            data.remove(MobStackerTags.LOVE_COUNT);
        } else {
            data.putInt(MobStackerTags.LOVE_COUNT, clamped);
        }
    }

    static int getFeedableCount(LivingEntity entity) {
        long gameTime = entity.level().getGameTime();
        int coolingDown = pruneAndCountCooldowns(entity, gameTime);
        return Math.max(0, getStackCount(entity) - getLoveCount(entity) - coolingDown);
    }

    static boolean hasBreedingState(LivingEntity entity) {
        return getLoveCount(entity) > 0 || pruneAndCountCooldowns(entity, entity.level().getGameTime()) > 0;
    }

    static boolean hasLoveState(LivingEntity entity) {
        return getLoveCount(entity) > 0;
    }

    static void addBreedingCooldowns(LivingEntity entity, int count) {
        int[] current = MobStackerEntityData.get(entity).getIntArray(MobStackerTags.BREEDING_COOLDOWNS);
        int[] updated = new int[current.length + count];
        System.arraycopy(current, 0, updated, 0, current.length);
        int cooldownUntil = (int) Math.min(Integer.MAX_VALUE, entity.level().getGameTime() + BREEDING_COOLDOWN_TICKS);
        for (int index = current.length; index < updated.length; index++) {
            updated[index] = cooldownUntil;
        }
        MobStackerEntityData.get(entity).put(MobStackerTags.BREEDING_COOLDOWNS, new IntArrayTag(updated));
        clampStackState(entity);
    }

    static void transferBreedingState(LivingEntity source, LivingEntity target, int count) {
        if (count <= 0) {
            return;
        }

        int transferLove = Math.min(count, getLoveCount(source));
        if (transferLove > 0) {
            setLoveCount(source, getLoveCount(source) - transferLove);
            setLoveCount(target, getLoveCount(target) + transferLove);
        }

        transferBreedingCooldowns(source, target, count - transferLove);
    }

    private static void transferBreedingCooldowns(LivingEntity source, LivingEntity target, int count) {
        pruneAndCountCooldowns(source, source.level().getGameTime());
        pruneAndCountCooldowns(target, target.level().getGameTime());

        int[] sourceCooldowns = MobStackerEntityData.get(source).getIntArray(MobStackerTags.BREEDING_COOLDOWNS);
        if (count <= 0 || sourceCooldowns.length == 0) {
            return;
        }

        int transfer = Math.min(count, sourceCooldowns.length);
        int[] targetCooldowns = MobStackerEntityData.get(target).getIntArray(MobStackerTags.BREEDING_COOLDOWNS);
        int[] updatedTarget = new int[targetCooldowns.length + transfer];
        System.arraycopy(targetCooldowns, 0, updatedTarget, 0, targetCooldowns.length);
        System.arraycopy(sourceCooldowns, 0, updatedTarget, targetCooldowns.length, transfer);
        MobStackerEntityData.get(target).put(MobStackerTags.BREEDING_COOLDOWNS, new IntArrayTag(updatedTarget));

        int remaining = sourceCooldowns.length - transfer;
        if (remaining <= 0) {
            MobStackerEntityData.get(source).remove(MobStackerTags.BREEDING_COOLDOWNS);
        } else {
            int[] updatedSource = new int[remaining];
            System.arraycopy(sourceCooldowns, transfer, updatedSource, 0, remaining);
            MobStackerEntityData.get(source).put(MobStackerTags.BREEDING_COOLDOWNS, new IntArrayTag(updatedSource));
        }
    }

    static void clampStackState(LivingEntity entity) {
        pruneAndCountCooldowns(entity, entity.level().getGameTime());
        setLoveCount(entity, getLoveCount(entity));
        int maxCount = getStackCount(entity);
        int maxCooldowns = Math.max(0, maxCount - getLoveCount(entity));
        int[] cooldowns = MobStackerEntityData.get(entity).getIntArray(MobStackerTags.BREEDING_COOLDOWNS);
        if (cooldowns.length <= maxCooldowns) {
            return;
        }

        int[] trimmed = new int[maxCooldowns];
        System.arraycopy(cooldowns, 0, trimmed, 0, maxCooldowns);
        MobStackerEntityData.get(entity).put(MobStackerTags.BREEDING_COOLDOWNS, new IntArrayTag(trimmed));
    }

    static void clearStackData(LivingEntity entity) {
        CompoundTag data = MobStackerEntityData.get(entity);
        data.remove(MobStackerTags.STACK_COUNT);
        data.remove(MobStackerTags.MANAGED_NAME);
        data.remove(MobStackerTags.LOVE_COUNT);
        data.remove(MobStackerTags.BREEDING_COOLDOWNS);
    }

    static void clearBreedingState(LivingEntity entity) {
        CompoundTag data = MobStackerEntityData.get(entity);
        data.remove(MobStackerTags.LOVE_COUNT);
        data.remove(MobStackerTags.BREEDING_COOLDOWNS);
    }

    static void refreshName(Mob mob) {
        int count = getStackCount(mob);
        CompoundTag data = MobStackerEntityData.get(mob);

        if (count <= 1 || !MobStackerConfig.SHOW_STACK_NAME.get()) {
            if (data.getBoolean(MobStackerTags.MANAGED_NAME)) {
                mob.setCustomName(null);
                mob.setCustomNameVisible(false);
                data.remove(MobStackerTags.MANAGED_NAME);
            }
            return;
        }

        Component name = Component.literal("x" + count + " ").append(mob.getType().getDescription());
        mob.setCustomName(name);
        mob.setCustomNameVisible(true);
        data.putBoolean(MobStackerTags.MANAGED_NAME, true);
    }

    static boolean isNoStackLocked(Mob mob) {
        CompoundTag data = MobStackerEntityData.get(mob);
        long noStackUntil = data.getLong(MobStackerTags.NO_STACK_UNTIL);
        if (noStackUntil <= 0) {
            return false;
        }

        if (mob.level().getGameTime() < noStackUntil) {
            return true;
        }

        data.remove(MobStackerTags.NO_STACK_UNTIL);
        return false;
    }

    private static int pruneAndCountCooldowns(LivingEntity entity, long gameTime) {
        int[] cooldowns = MobStackerEntityData.get(entity).getIntArray(MobStackerTags.BREEDING_COOLDOWNS);
        if (cooldowns.length == 0) {
            return 0;
        }

        int activeCount = 0;
        int[] active = new int[cooldowns.length];
        for (int cooldown : cooldowns) {
            if (cooldown > gameTime) {
                active[activeCount] = cooldown;
                activeCount++;
            }
        }

        int[] trimmed = new int[activeCount];
        System.arraycopy(active, 0, trimmed, 0, activeCount);
        MobStackerEntityData.get(entity).put(MobStackerTags.BREEDING_COOLDOWNS, new IntArrayTag(trimmed));
        return activeCount;
    }

    private static Set<String> configuredEntityIds() {
        return new HashSet<>(MobStackerConfig.ENTITY_IDS.get().stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList());
    }
}
