package com.proxwian.mobstackeradv;

import java.util.Comparator;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public final class MobStackingEvents {
    private MobStackingEvents() {
    }

    public static void onMobTick(Mob mob) {
        if (mob.level().isClientSide()) {
            return;
        }

        if (!MobStackerData.canStack(mob)) {
            return;
        }

        if (MobCopies.unstackIfBelowMinimum(mob)) {
            return;
        }

        if (MobStackerData.getStackCount(mob) <= 1 && MobStackerData.hasBreedingState(mob)) {
            MobStackerData.clearBreedingState(mob);
        }

        StackedEggLaying.process(mob);

        if (mob.tickCount % MobStackerConfig.SCAN_INTERVAL_TICKS.get() != 0) {
            return;
        }

        mergeNearby(mob);
        MobBreedingEvents.processStackedBreeding(mob);
        MobStackerData.refreshName(mob);
    }

    private static void mergeNearby(Mob target) {
        int targetCount = MobStackerData.getStackCount(target);
        int maxStackSize = MobStackerConfig.MAX_STACK_SIZE.get();
        if (targetCount >= maxStackSize || target.isRemoved()) {
            return;
        }

        ServerLevel level = (ServerLevel) target.level();
        double radius = MobStackerConfig.STACK_RADIUS.get();
        AABB area = target.getBoundingBox().inflate(radius);
        StackKey targetKey = StackKey.of(target);
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, area, candidate -> canMergeInto(target, candidate, targetKey));
        nearby.sort(Comparator.comparing(Mob::getUUID));
        if (targetCount <= 1 && totalStackCount(targetCount, nearby) < MobStackerConfig.MINIMUM_GROUP_SIZE.get()) {
            return;
        }

        for (Mob candidate : nearby) {
            if (targetCount >= maxStackSize) {
                break;
            }

            int candidateCount = MobStackerData.getStackCount(candidate);
            int transfer = transferableCount(targetCount, candidateCount, maxStackSize);
            if (transfer <= 0) {
                continue;
            }

            targetCount += transfer;
            candidateCount -= transfer;

            MobStackerData.setStackCount(target, targetCount);
            MobStackerData.transferBreedingState(candidate, target, transfer);
            MobStackerData.clampStackState(target);
            StackingParticles.spawnMergePoof(level, candidate);
            if (candidateCount <= 0) {
                candidate.discard();
            } else {
                MobStackerData.setStackCount(candidate, candidateCount);
                MobStackerData.clampStackState(candidate);
                if (!MobCopies.unstackIfBelowMinimum(candidate)) {
                    MobStackerData.refreshName(candidate);
                }
            }
        }
    }

    private static int transferableCount(int targetCount, int candidateCount, int maxStackSize) {
        int capacity = maxStackSize - targetCount;
        if (capacity <= 0) {
            return 0;
        }

        if (candidateCount <= capacity) {
            return candidateCount;
        }

        if (candidateCount >= maxStackSize) {
            return 0;
        }

        int remaining = candidateCount - capacity;
        if (remaining < MobStackerConfig.MINIMUM_GROUP_SIZE.get()) {
            return 0;
        }

        return capacity;
    }

    private static boolean canMergeInto(Mob target, Mob candidate, StackKey targetKey) {
        return candidate != target
                && !candidate.isRemoved()
                && candidate.isAlive()
                && MobStackerData.canStack(candidate)
                && !MobStackerData.isNoStackLocked(target)
                && !MobStackerData.isNoStackLocked(candidate)
                && target.getUUID().compareTo(candidate.getUUID()) < 0
                && StackKey.of(candidate).equals(targetKey);
    }

    private static int totalStackCount(int targetCount, List<Mob> nearby) {
        int total = targetCount;
        for (Mob mob : nearby) {
            total += MobStackerData.getStackCount(mob);
        }
        return total;
    }
}
