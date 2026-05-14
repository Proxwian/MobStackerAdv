package com.proxwian.mobstackeradv;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class MobStackerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Value<List<String>> ENTITY_IDS = new Value<>(List.of("minecraft:cow", "minecraft:pig", "minecraft:sheep", "minecraft:chicken"));
    public static final Value<Double> STACK_RADIUS = new Value<>(8.0D);
    public static final Value<Integer> MINIMUM_GROUP_SIZE = new Value<>(2);
    public static final Value<Integer> MAX_STACK_SIZE = new Value<>(64);
    public static final Value<Integer> SCAN_INTERVAL_TICKS = new Value<>(40);
    public static final Value<Boolean> SHOW_STACK_NAME = new Value<>(true);
    public static final Value<String> STACK_PARTICLE_ID = new Value<>("minecraft:poof");
    public static final Value<String> SEPARATOR_ITEM_ID = new Value<>("minecraft:stick");
    public static final Value<Boolean> CONSUME_SEPARATOR_ITEM = new Value<>(false);
    public static final Value<Integer> SEPARATED_NO_STACK_TICKS = new Value<>(200);

    private MobStackerConfig() {
    }

    public static void load(Path configDirectory) {
        Path configFile = configDirectory.resolve(MobStackerAdv.MOD_ID + ".json");
        ConfigData data = new ConfigData();
        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null) {
                    data = loaded;
                }
            } catch (IOException ignored) {
                data = new ConfigData();
            }
        }

        ENTITY_IDS.set(cleanEntityIds(data.entityIds));
        STACK_RADIUS.set(clamp(data.stackRadius, 1.0D, 64.0D));
        MINIMUM_GROUP_SIZE.set(clamp(data.minimumGroupSize, 2, 1024));
        MAX_STACK_SIZE.set(clamp(data.maxStackSize, 2, 1024));
        SCAN_INTERVAL_TICKS.set(clamp(data.scanIntervalTicks, 5, 20 * 60));
        SHOW_STACK_NAME.set(data.showStackName);
        STACK_PARTICLE_ID.set(blankToDefault(data.stackParticleId, "minecraft:poof"));
        SEPARATOR_ITEM_ID.set(blankToDefault(data.separatorItemId, "minecraft:stick"));
        CONSUME_SEPARATOR_ITEM.set(data.consumeSeparatorItem);
        SEPARATED_NO_STACK_TICKS.set(clamp(data.separatedNoStackTicks, 0, 20 * 60 * 10));

        save(configDirectory);
    }

    private static void save(Path configDirectory) {
        try {
            Files.createDirectories(configDirectory);
            try (Writer writer = Files.newBufferedWriter(configDirectory.resolve(MobStackerAdv.MOD_ID + ".json"))) {
                GSON.toJson(ConfigData.fromCurrent(), writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static List<String> cleanEntityIds(List<String> ids) {
        List<String> cleaned = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    cleaned.add(id.trim());
                }
            }
        }
        return cleaned.isEmpty() ? List.of("minecraft:cow", "minecraft:pig", "minecraft:sheep", "minecraft:chicken") : cleaned;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Value<T> {
        private T value;

        private Value(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        private void set(T value) {
            this.value = value;
        }
    }

    private static final class ConfigData {
        List<String> entityIds = List.of("minecraft:cow", "minecraft:pig", "minecraft:sheep", "minecraft:chicken");
        double stackRadius = 8.0D;
        int minimumGroupSize = 2;
        int maxStackSize = 64;
        int scanIntervalTicks = 40;
        boolean showStackName = true;
        String stackParticleId = "minecraft:poof";
        String separatorItemId = "minecraft:stick";
        boolean consumeSeparatorItem = false;
        int separatedNoStackTicks = 200;

        static ConfigData fromCurrent() {
            ConfigData data = new ConfigData();
            data.entityIds = ENTITY_IDS.get();
            data.stackRadius = STACK_RADIUS.get();
            data.minimumGroupSize = MINIMUM_GROUP_SIZE.get();
            data.maxStackSize = MAX_STACK_SIZE.get();
            data.scanIntervalTicks = SCAN_INTERVAL_TICKS.get();
            data.showStackName = SHOW_STACK_NAME.get();
            data.stackParticleId = STACK_PARTICLE_ID.get();
            data.separatorItemId = SEPARATOR_ITEM_ID.get();
            data.consumeSeparatorItem = CONSUME_SEPARATOR_ITEM.get();
            data.separatedNoStackTicks = SEPARATED_NO_STACK_TICKS.get();
            return data;
        }
    }
}
