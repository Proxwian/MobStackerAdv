package com.proxwian.mobstackeradv;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

final class MobCopies {
    private MobCopies() {
    }

    static Mob copyMob(Mob source, ServerLevel level) {
        if (!(source.getType().create(level) instanceof Mob copy)) {
            return null;
        }

        CompoundTag tag = new CompoundTag();
        source.saveWithoutId(tag);
        tag.remove("UUID");
        tag.remove("Health");
        tag.remove("DeathTime");
        tag.remove("HurtTime");
        copy.load(tag);
        copy.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        copy.setHealth(copy.getMaxHealth());
        copy.clearFire();
        return copy;
    }

    static boolean unstackIfBelowMinimum(Mob mob) {
        int count = MobStackerData.getStackCount(mob);
        if (count <= 1 || count >= MobStackerConfig.MINIMUM_GROUP_SIZE.get() || !(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        spawnVanillaCopiesFromStack(mob, level, count - 1);
        MobStackerData.clearStackData(mob);
        mob.setCustomName(null);
        mob.setCustomNameVisible(false);
        return true;
    }

    static void spawnVanillaCopiesFromStack(Mob source, ServerLevel level, int count) {
        spawnVanillaCopiesFromStack(source, level, count, 0);
    }

    static void spawnVanillaCopiesFromStack(Mob source, ServerLevel level, int count, int invulnerableTicks) {
        for (int index = 0; index < count; index++) {
            Mob copy = copyMob(source, level);
            if (copy == null) {
                continue;
            }

            MobStackerData.clearStackData(copy);
            copy.setCustomName(null);
            copy.setCustomNameVisible(false);
            copy.invulnerableTime = Math.max(copy.invulnerableTime, invulnerableTicks);
            if (invulnerableTicks > 0) {
                MobStackerEntityData.get(copy).putLong(MobStackerTags.ENVIRONMENT_PROTECTION_UNTIL, level.getGameTime() + invulnerableTicks);
            }
            level.addFreshEntityWithPassengers(copy);
        }
    }
}
