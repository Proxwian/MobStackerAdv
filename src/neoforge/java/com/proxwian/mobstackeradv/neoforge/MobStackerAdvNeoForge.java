package com.proxwian.mobstackeradv.neoforge;

import com.proxwian.mobstackeradv.MobBreedingEvents;
import com.proxwian.mobstackeradv.MobDeathEvents;
import com.proxwian.mobstackeradv.MobInteractionEvents;
import com.proxwian.mobstackeradv.MobStackerAdv;
import com.proxwian.mobstackeradv.MobStackingEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
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
