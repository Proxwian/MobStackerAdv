package com.proxwian.mobstackeradv.fabric;

import com.proxwian.mobstackeradv.MobDeathEvents;
import com.proxwian.mobstackeradv.MobInteractionEvents;
import com.proxwian.mobstackeradv.MobStackerAdv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;

public final class MobStackerAdvFabric implements ModInitializer {
    public static final MobDeathEvents DEATH_EVENTS = new MobDeathEvents();

    @Override
    public void onInitialize() {
        MobStackerAdv.init(FabricLoader.getInstance().getConfigDir());

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(entity instanceof Mob mob)) {
                return InteractionResult.PASS;
            }
            return MobInteractionEvents.onMobInteract(player, hand, mob);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof Mob mob) || DEATH_EVENTS.allowDamage(mob, source, amount));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !(entity instanceof Mob mob) || DEATH_EVENTS.allowDeath(mob, source, amount));
        ServerTickEvents.END_SERVER_TICK.register(server -> DEATH_EVENTS.onServerTickEnd());
    }
}
