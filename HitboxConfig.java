package com.wwoqkx.hitboxvisuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HitboxConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hitboxvisuals.json");
    public static Settings SETTINGS = new Settings();

    private HitboxConfig() {}

    public static final class Settings {
        public boolean enabled = true;
        public boolean fillEnabled = true;
        public boolean hoverEnabled = true;
        public int outlineColor = 0xFF35D06F;
        public int fillColor = 0x5535D06F;
        public int hoverOutlineColor = 0xFFFFC928;
        public int hoverFillColor = 0x66FFC928;
        public int fillAlpha = 85;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader reader = Files.newBufferedReader(FILE)) {
                    Settings loaded = GSON.fromJson(reader, Settings.class);
                    if (loaded != null) SETTINGS = loaded;
                }
            } else {
                save();
            }
        } catch (Exception e) {
            System.err.println("[HitboxVisuals] Could not load config: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(SETTINGS, writer);
            }
        } catch (Exception e) {
            System.err.println("[HitboxVisuals] Could not save config: " + e.getMessage());
        }
    }
}
