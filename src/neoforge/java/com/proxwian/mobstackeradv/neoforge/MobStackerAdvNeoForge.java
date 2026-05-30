package com.proxwian.mobstackeradv.neoforge;

import com.proxwian.mobstackeradv.MobBreedingEvents;
import com.proxwian.mobstackeradv.MobConversionEvents;
import com.proxwian.mobstackeradv.MobDeathEvents;
import com.proxwian.mobstackeradv.MobInteractionEvents;
import com.proxwian.mobstackeradv.MobStackerAdv;
import com.proxwian.mobstackeradv.MobStackingEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@Mod(MobStackerAdv.MOD_ID)
public final class MobStackerAdvNeoForge {
    private final MobDeathEvents deathEvents = new MobDeathEvents();

    public MobStackerAdvNeoForge() {
        MobStackerAdv.init(FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob) {
            MobStackingEvents.onMobTick(mob);
        }
    }

    @SubscribeEvent
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getChild() != null) {
            MobBreedingEvents.onBabySpawn(event.getChild());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingConversionPre(LivingConversionEvent.Pre event) {
        if (event.getEntity() instanceof Pig pig && event.getOutcome() == EntityType.ZOMBIFIED_PIGLIN
                && pig.level() instanceof ServerLevel level
                && MobConversionEvents.handlePigLightningConversion(pig, level)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingConversionPost(LivingConversionEvent.Post event) {
        if (event.getEntity() instanceof Pig pig && event.getOutcome() instanceof ZombifiedPiglin
                && pig.level() instanceof ServerLevel level) {
            MobConversionEvents.afterPigLightningConversion(pig, event.getOutcome(), level);
        }
    }

    @SubscribeEvent
    public void onMobInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Mob mob)) {
            return;
        }

        InteractionResult result = MobInteractionEvents.onMobInteract(event.getEntity(), event.getHand(), mob);
        if (result.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Mob mob && !deathEvents.allowDamage(mob, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Mob mob && event.getNewDamage() >= mob.getHealth()
                && !deathEvents.allowDeath(mob, event.getSource(), event.getNewDamage())) {
            event.setNewDamage(0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && !deathEvents.allowDeath(mob, event.getSource(), mob.getHealth())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Mob mob && deathEvents.shouldProtectDrops(mob)) {
            for (var drop : event.getDrops()) {
                deathEvents.protectDrop(drop);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        deathEvents.onServerTickEnd();
    }
}
