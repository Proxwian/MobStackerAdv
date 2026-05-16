package com.proxwian.mobstackeradv.forge;

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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(MobStackerAdv.MOD_ID)
public final class MobStackerAdvForge {
    private final MobDeathEvents deathEvents = new MobDeathEvents();

    public MobStackerAdvForge() {
        MobStackerAdv.init(FMLPaths.CONFIGDIR.get());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
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
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Mob mob && !deathEvents.allowDamage(mob, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getAmount() >= mob.getHealth()
                && !deathEvents.allowDeath(mob, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            deathEvents.onServerTickEnd();
        }
    }
}
