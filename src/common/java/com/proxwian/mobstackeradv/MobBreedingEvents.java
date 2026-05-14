package com.proxwian.mobstackeradv;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

public final class MobBreedingEvents {
    private MobBreedingEvents() {
    }

    public static void onBabySpawn(AgeableMob child) {
        if (child.level().isClientSide()) {
            return;
        }

        if (!MobStackerData.canStack(child)) {
            return;
        }

        MobStackerData.setStackCount(child, 1);
        MobStackerData.refreshName(child);
    }

    public static void processStackedBreeding(Mob mob) {
        if (!(mob instanceof Animal animal) || animal.isBaby() || MobStackerData.getLoveCount(animal) <= 0) {
            return;
        }

        ServerLevel level = (ServerLevel) animal.level();
        int selfBreedCount = MobStackerData.getLoveCount(animal) / 2;
        if (selfBreedCount > 0) {
            AgeableMob child = animal.getBreedOffspring(level, animal);
            if (child != null) {
                spawnBabyStack(level, animal, child, selfBreedCount);
                spawnBreedingExperience(level, animal, selfBreedCount);
                MobStackerData.setLoveCount(animal, MobStackerData.getLoveCount(animal) - selfBreedCount * 2);
                MobStackerData.addBreedingCooldowns(animal, selfBreedCount * 2);
                level.broadcastEntityEvent(animal, (byte) 18);
                return;
            }
        }

        double radius = MobStackerConfig.STACK_RADIUS.get();
        AABB area = animal.getBoundingBox().inflate(radius);
        StackKey targetKey = StackKey.of(animal);
        List<Animal> partners = level.getEntitiesOfClass(Animal.class, area, partner -> canBreedWith(animal, partner, targetKey));

        for (Animal partner : partners) {
            int breedCount = Math.min(MobStackerData.getLoveCount(animal), MobStackerData.getLoveCount(partner));
            if (breedCount <= 0) {
                continue;
            }

            AgeableMob child = animal.getBreedOffspring(level, partner);
            if (child == null) {
                continue;
            }

            spawnBabyStack(level, animal, child, breedCount);
            spawnBreedingExperience(level, animal, breedCount);
            MobStackerData.setLoveCount(animal, MobStackerData.getLoveCount(animal) - breedCount);
            MobStackerData.setLoveCount(partner, MobStackerData.getLoveCount(partner) - breedCount);
            MobStackerData.addBreedingCooldowns(animal, breedCount);
            MobStackerData.addBreedingCooldowns(partner, breedCount);
            animal.resetLove();
            partner.resetLove();
            level.broadcastEntityEvent(animal, (byte) 18);
            level.broadcastEntityEvent(partner, (byte) 18);
            break;
        }
    }

    private static void spawnBabyStack(ServerLevel level, Animal parent, AgeableMob child, int count) {
        child.setBaby(true);
        child.moveTo(parent.getX(), parent.getY(), parent.getZ(), 0.0F, 0.0F);
        if (count < MobStackerConfig.MINIMUM_GROUP_SIZE.get()) {
            MobStackerData.clearStackData(child);
            child.setCustomName(null);
            child.setCustomNameVisible(false);
            level.addFreshEntityWithPassengers(child);
            MobCopies.spawnVanillaCopiesFromStack(child, level, count - 1);
            return;
        }

        MobStackerData.setStackCount(child, count);
        MobStackerData.setLoveCount(child, 0);
        MobStackerData.refreshName(child);
        level.addFreshEntityWithPassengers(child);
    }

    private static void spawnBreedingExperience(ServerLevel level, Animal parent, int breedCount) {
        if (breedCount <= 0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            return;
        }

        for (int index = 0; index < breedCount; index++) {
            int experience = parent.getRandom().nextInt(7) + 1;
            level.addFreshEntity(new ExperienceOrb(level, parent.getX(), parent.getY(), parent.getZ(), experience));
        }
    }

    private static boolean canBreedWith(Animal animal, Animal partner, StackKey targetKey) {
        return partner != animal
                && !partner.isRemoved()
                && partner.isAlive()
                && !partner.isBaby()
                && MobStackerData.canStack(partner)
                && MobStackerData.getLoveCount(partner) > 0
                && StackKey.of(partner).equals(targetKey);
    }
}
