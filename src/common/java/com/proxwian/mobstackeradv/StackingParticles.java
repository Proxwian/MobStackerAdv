package com.proxwian.mobstackeradv;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

final class StackingParticles {
    private static final int PARTICLES_PER_MERGE = 16;

    private StackingParticles() {
    }

    static void spawnMergePoof(ServerLevel level, Mob mob) {
        SimpleParticleType particle = configuredParticle();
        if (particle == null) {
            return;
        }

        level.sendParticles(
                particle,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5D,
                mob.getZ(),
                PARTICLES_PER_MERGE,
                mob.getBbWidth() * 0.5D,
                mob.getBbHeight() * 0.35D,
                mob.getBbWidth() * 0.5D,
                0.02D);
    }

    private static SimpleParticleType configuredParticle() {
        String configuredId = MobStackerConfig.STACK_PARTICLE_ID.get().trim();
        if (configuredId.equalsIgnoreCase("none")) {
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(configuredId);
        if (id == null) {
            return ParticleTypes.POOF;
        }

        ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (particleType instanceof SimpleParticleType simpleParticleType) {
            return simpleParticleType;
        }

        return ParticleTypes.POOF;
    }
}
