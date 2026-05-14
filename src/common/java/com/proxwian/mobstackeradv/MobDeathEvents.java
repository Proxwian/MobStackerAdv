package com.proxwian.mobstackeradv;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;

public final class MobDeathEvents {
    private static final int POST_DEATH_DAMAGE_PROTECTION_TICKS = 10;
    private static final int POST_DEATH_ENVIRONMENT_PROTECTION_TICKS = 60;
    private final List<PendingSpawn> pendingSpawns = new ArrayList<>();

    public boolean allowDeath(Mob mob, DamageSource source, float lethalAmount) {
        if (mob.level().isClientSide()) {
            return true;
        }

        if (mob.getPersistentData().getBoolean(MobStackerTags.SACRIFICIAL_DEATH)) {
            mob.getPersistentData().remove(MobStackerTags.SACRIFICIAL_DEATH);
            return true;
        }

        if (hasPostDeathProtection(mob)) {
            mob.setHealth(mob.getMaxHealth());
            mob.clearFire();
            return false;
        }

        if (handleLethalStackDamage(mob, source, lethalAmount)) {
            return false;
        }

        int count = MobStackerData.getStackCount(mob);
        if (!MobStackerData.canStack(mob) || count <= 1) {
            return true;
        }

        spawnRemainingStackAfterDeath(mob, count - 1);
        if (isEnvironmentalFireDamage(source)) {
            mob.getPersistentData().putBoolean(MobStackerTags.PROTECT_FIRE_DROPS, true);
        }
        MobStackerData.clearStackData(mob);
        mob.setCustomName(null);
        mob.setCustomNameVisible(false);
        return true;
    }

    public boolean allowDamage(Mob mob, DamageSource source, float amount) {
        return mob.level().isClientSide() || !cancelProtectedDamage(mob, source);
    }

    public void protectDrop(ItemEntity drop) {
        drop.clearFire();
        drop.setInvulnerable(true);
    }

    public boolean shouldProtectDrops(Mob mob) {
        return !mob.level().isClientSide() && mob.getPersistentData().getBoolean(MobStackerTags.PROTECT_FIRE_DROPS);
    }

    public void onServerTickEnd() {
        if (pendingSpawns.isEmpty()) {
            return;
        }

        List<PendingSpawn> spawns = new ArrayList<>(pendingSpawns);
        pendingSpawns.clear();
        for (PendingSpawn spawn : spawns) {
            if (!spawn.level().isLoaded(spawn.mob().blockPosition())) {
                continue;
            }

            spawn.level().addFreshEntityWithPassengers(spawn.mob());
        }
    }

    private void spawnRemainingStackAfterDeath(Mob mob, int count) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        int protectionTicks = isEnvironmentalFireDamage(mob.getLastDamageSource())
                ? POST_DEATH_ENVIRONMENT_PROTECTION_TICKS
                : POST_DEATH_DAMAGE_PROTECTION_TICKS;

        if (count < MobStackerConfig.MINIMUM_GROUP_SIZE.get()) {
            queueVanillaCopiesFromStack(mob, level, count, protectionTicks);
            return;
        }

        Mob replacement = MobCopies.copyMob(mob, level);
        if (replacement == null) {
            return;
        }
        MobStackerData.setStackCount(replacement, count);
        MobStackerData.clampStackState(replacement);
        protectFromFollowUpDamage(replacement, protectionTicks);
        MobStackerData.refreshName(replacement);
        queueSpawn(level, replacement);
    }

    private void queueVanillaCopiesFromStack(Mob source, ServerLevel level, int count, int protectionTicks) {
        for (int index = 0; index < count; index++) {
            Mob copy = MobCopies.copyMob(source, level);
            if (copy == null) {
                continue;
            }

            MobStackerData.clearStackData(copy);
            copy.setCustomName(null);
            copy.setCustomNameVisible(false);
            protectFromFollowUpDamage(copy, protectionTicks);
            queueSpawn(level, copy);
        }
    }

    private void queueSpawn(ServerLevel level, Mob mob) {
        pendingSpawns.add(new PendingSpawn(level, mob));
    }

    private boolean handleLethalStackDamage(Mob mob, DamageSource source, float amount) {
        int count = MobStackerData.getStackCount(mob);
        if (amount < mob.getHealth() || count <= 1 || !MobStackerData.canStack(mob) || !(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        Mob sacrifice = MobCopies.copyMob(mob, level);
        if (sacrifice != null) {
            MobStackerData.clearStackData(sacrifice);
            sacrifice.setCustomName(null);
            sacrifice.setCustomNameVisible(false);
            sacrifice.getPersistentData().putBoolean(MobStackerTags.SACRIFICIAL_DEATH, true);
            level.addFreshEntityWithPassengers(sacrifice);
            sacrifice.hurt(source, Math.max(amount, sacrifice.getMaxHealth()));
        }

        int remaining = count - 1;
        if (remaining < MobStackerConfig.MINIMUM_GROUP_SIZE.get()) {
            queueVanillaCopiesFromStack(mob, level, remaining, POST_DEATH_DAMAGE_PROTECTION_TICKS);
            mob.discard();
            return true;
        }

        MobStackerData.setStackCount(mob, remaining);
        MobStackerData.clampStackState(mob);
        protectFromFollowUpDamage(mob, POST_DEATH_DAMAGE_PROTECTION_TICKS);
        mob.setHealth(mob.getMaxHealth());
        MobStackerData.refreshName(mob);
        return true;
    }

    private static void protectFromFollowUpDamage(Mob mob, int ticks) {
        if (ticks <= 0) {
            return;
        }

        mob.getPersistentData().putLong(MobStackerTags.ENVIRONMENT_PROTECTION_UNTIL, mob.level().getGameTime() + ticks);
        mob.clearFire();
        mob.invulnerableTime = Math.max(mob.invulnerableTime, ticks);
    }

    private static boolean hasPostDeathProtection(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        long protectedUntil = data.getLong(MobStackerTags.ENVIRONMENT_PROTECTION_UNTIL);
        if (protectedUntil <= 0) {
            return false;
        }

        if (mob.level().getGameTime() < protectedUntil) {
            return true;
        }

        data.remove(MobStackerTags.ENVIRONMENT_PROTECTION_UNTIL);
        return false;
    }

    private static boolean cancelProtectedDamage(Mob mob, DamageSource source) {
        if (!hasPostDeathProtection(mob)) {
            return false;
        }

        if (isEnvironmentalFireDamage(source)) {
            mob.clearFire();
        }
        return true;
    }

    private static boolean isEnvironmentalFireDamage(DamageSource source) {
        return source != null
                && (source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR));
    }

    private record PendingSpawn(ServerLevel level, Mob mob) {
    }
}
