package com.oozing.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Saved to config/oozings-settings.json */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("oozings-settings.json");
    private static Map<String, Item> itemLookup;

    public Set<String> bannedPotions = new LinkedHashSet<>();
    /** item id -> cooldown in seconds (0 = off) */
    public Map<String, Integer> cooldowns = new LinkedHashMap<>();

    public static Config INSTANCE = new Config();

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) INSTANCE = c;
            } else {
                INSTANCE.cooldowns.put("minecraft:ender_pearl", 0);
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (INSTANCE.bannedPotions == null) INSTANCE.bannedPotions = new LinkedHashSet<>();
        if (INSTANCE.cooldowns == null) INSTANCE.cooldowns = new LinkedHashMap<>();
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static Item item(String key) {
        if (itemLookup == null) {
            itemLookup = new HashMap<>();
            for (Item i : BuiltInRegistries.ITEM) itemLookup.put(key(i), i);
        }
        return itemLookup.get(key);
    }

    public static int cooldownSeconds(Item item) {
        return INSTANCE.cooldowns.getOrDefault(key(item), 0);
    }
}
