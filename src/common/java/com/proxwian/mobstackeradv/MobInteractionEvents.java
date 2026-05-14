package com.proxwian.mobstackeradv;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MobInteractionEvents {
    private MobInteractionEvents() {
    }

    public static InteractionResult onMobInteract(Player player, InteractionHand hand, Mob mob) {
        if (mob.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        if (!MobStackerData.canStack(mob)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (handleNameTagSplit(player, mob, held) || handleSeparatorSplit(player, mob, held)) {
            return InteractionResult.SUCCESS;
        }

        if (!(mob instanceof Animal animal)) {
            return InteractionResult.PASS;
        }

        if (handleStackedShearing(player, hand, animal, held) || handleStackedMilking(player, animal, held)) {
            return InteractionResult.SUCCESS;
        }

        if (animal.isBaby() || !animal.isFood(held) || MobStackerData.getStackCount(animal) <= 1) {
            return InteractionResult.PASS;
        }

        int feedable = MobStackerData.getFeedableCount(animal);
        if (feedable <= 0) {
            return InteractionResult.PASS;
        }

        int foodUsed = player.getAbilities().instabuild ? feedable : Math.min(feedable, held.getCount());
        if (foodUsed <= 0) {
            return InteractionResult.PASS;
        }

        MobStackerData.setLoveCount(animal, MobStackerData.getLoveCount(animal) + foodUsed);
        if (!player.getAbilities().instabuild) {
            held.shrink(foodUsed);
        }

        animal.level().broadcastEntityEvent(animal, (byte) 18);
        return InteractionResult.SUCCESS;
    }

    private static boolean handleNameTagSplit(Player player, Mob mob, ItemStack held) {
        if (!held.is(Items.NAME_TAG) || !held.has(DataComponents.CUSTOM_NAME) || MobStackerData.getStackCount(mob) <= 1) {
            return false;
        }

        Mob namedMob = splitOneFromStack(mob, 0);
        if (namedMob == null) {
            return false;
        }

        namedMob.setCustomName(held.getHoverName());
        namedMob.setCustomNameVisible(true);
        MobStackerEntityData.get(namedMob).remove(MobStackerTags.MANAGED_NAME);

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        return true;
    }

    private static boolean handleSeparatorSplit(Player player, Mob mob, ItemStack held) {
        if (MobStackerData.getStackCount(mob) <= 1 || !isSeparatorItem(held)) {
            return false;
        }

        Mob separated = splitOneFromStack(mob, MobStackerConfig.SEPARATED_NO_STACK_TICKS.get());
        if (separated == null) {
            return false;
        }

        if (MobStackerConfig.CONSUME_SEPARATOR_ITEM.get() && !player.getAbilities().instabuild) {
            held.shrink(1);
        }

        return true;
    }

    private static Mob splitOneFromStack(Mob source, int noStackTicks) {
        int sourceCount = MobStackerData.getStackCount(source);
        if (sourceCount <= 1 || !(source.level() instanceof ServerLevel level)) {
            return null;
        }

        Mob split = MobCopies.copyMob(source, level);
        if (split == null) {
            return null;
        }

        MobStackerData.setStackCount(source, sourceCount - 1);
        MobStackerData.clampStackState(source);
        if (!MobCopies.unstackIfBelowMinimum(source)) {
            MobStackerData.refreshName(source);
        }

        MobStackerData.setStackCount(split, 1);
        MobStackerData.clearStackData(split);
        if (noStackTicks > 0) {
            MobStackerEntityData.get(split).putLong(MobStackerTags.NO_STACK_UNTIL, level.getGameTime() + noStackTicks);
        } else {
            MobStackerEntityData.get(split).remove(MobStackerTags.NO_STACK_UNTIL);
        }
        split.setCustomName(null);
        split.setCustomNameVisible(false);
        level.addFreshEntityWithPassengers(split);
        return split;
    }

    private static boolean handleStackedShearing(Player player, InteractionHand hand, Animal animal, ItemStack held) {
        if (!(animal instanceof Sheep sheep) || !held.is(Items.SHEARS) || !sheep.readyForShearing()) {
            return false;
        }

        int stackCount = MobStackerData.getStackCount(sheep);
        int shearCount = player.getAbilities().instabuild ? stackCount : Math.min(stackCount, held.getMaxDamage() - held.getDamageValue());
        if (shearCount <= 0) {
            return false;
        }

        sheep.level().playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        sheep.setSheared(true);
        for (int index = 0; index < shearCount; index++) {
            int dropCount = 1 + sheep.getRandom().nextInt(3);
            sheep.spawnAtLocation(new ItemStack(woolItemFor(sheep.getColor()), dropCount), 1.0F);
        }

        if (!player.getAbilities().instabuild) {
            held.hurtAndBreak(shearCount, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        return true;
    }

    private static boolean handleStackedMilking(Player player, Animal animal, ItemStack held) {
        if (!(animal instanceof Cow) || animal.isBaby() || !held.is(Items.BUCKET)) {
            return false;
        }

        int milkCount = player.getAbilities().instabuild
                ? MobStackerData.getStackCount(animal)
                : Math.min(MobStackerData.getStackCount(animal), held.getCount());
        if (milkCount <= 0) {
            return false;
        }

        animal.level().playSound(null, animal, SoundEvents.COW_MILK, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player.getAbilities().instabuild) {
            giveItems(player, Items.MILK_BUCKET, milkCount);
        } else {
            held.shrink(milkCount);
            giveItems(player, Items.MILK_BUCKET, milkCount);
        }

        return true;
    }

    private static void giveItems(Player player, Item item, int count) {
        for (int index = 0; index < count; index++) {
            ItemStack stack = new ItemStack(item);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static Item woolItemFor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };
    }

    private static boolean isSeparatorItem(ItemStack stack) {
        ResourceLocation itemId = ResourceLocation.tryParse(MobStackerConfig.SEPARATOR_ITEM_ID.get().trim());
        if (itemId == null) {
            return false;
        }

        Item separatorItem = BuiltInRegistries.ITEM.get(itemId);
        return stack.is(separatorItem);
    }
}
