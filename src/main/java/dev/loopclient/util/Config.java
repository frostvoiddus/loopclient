package dev.loopclient.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loopclient.module.Module;
import dev.loopclient.module.Modules;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Конфиг: какие модули включены и значения их настроек. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String key;

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("loopclient.json");
    }

    public static String getKey() {
        return key;
    }

    public static void setKey(String value) {
        key = value;
        save();
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonObject mods = new JsonObject();
            for (Module m : Modules.ALL) {
                JsonObject one = new JsonObject();
                one.addProperty("enabled", m.isEnabled());
                one.addProperty("ox", m.offsetX);
                one.addProperty("oy", m.offsetY);
                JsonObject settings = new JsonObject();
                for (Module.Setting s : m.settings) {
                    if (s.kind == Module.Kind.BOOL) settings.addProperty(s.name, s.bool);
                    else if (s.kind == Module.Kind.MODE) settings.addProperty(s.name, s.mode);
                    else settings.addProperty(s.name, s.value);
                }
                one.add("settings", settings);
                mods.add(m.name, one);
            }
            root.add("modules", mods);
            if (key != null && !key.isEmpty()) root.addProperty("key", key);
            root.addProperty("version", 1);

            Path path = file();
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(root, w);
            }
        } catch (Throwable ignored) {
            // конфиг не должен ронять игру
        }
    }

    public static void load() {
        try {
            Path path = file();
            if (!Files.exists(path)) return;
            JsonObject root;
            try (Reader r = Files.newBufferedReader(path)) {
                JsonElement parsed = JsonParser.parseReader(r);
                root = parsed.getAsJsonObject();
            }
            if (root.has("key")) key = root.get("key").getAsString();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : Modules.ALL) {
                if (!mods.has(m.name)) continue;
                JsonObject one = mods.getAsJsonObject(m.name);
                if (one.has("settings")) {
                    JsonObject settings = one.getAsJsonObject("settings");
                    for (Module.Setting s : m.settings) {
                        if (!settings.has(s.name)) continue;
                        try {
                            if (s.kind == Module.Kind.BOOL) s.bool = settings.get(s.name).getAsBoolean();
                            else if (s.kind == Module.Kind.MODE) s.mode = settings.get(s.name).getAsString();
                            else s.value = settings.get(s.name).getAsDouble();
                        } catch (Throwable ignored) {
                            // старое/битое значение — оставляем дефолт
                        }
                    }
                }
                if (one.has("ox")) m.offsetX = one.get("ox").getAsInt();
                if (one.has("oy")) m.offsetY = one.get("oy").getAsInt();
                if (one.has("enabled") && one.get("enabled").getAsBoolean()) {
                    m.setEnabled(true);
                }
            }
        } catch (Throwable ignored) {
            // нет файла или он битый — играем с дефолтами
        }
    }
}
