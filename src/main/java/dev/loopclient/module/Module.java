package dev.loopclient.module;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

/** Базовый модуль: имя, категория, вкл/выкл, настройки, хуки. */
public abstract class Module {

    public enum Category {
        HUD("HUD"),
        VISUAL("Визуал"),
        PLAYER("Игрок"),
        MISC("Разное");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public enum Kind { BOOL, NUM, MODE }

    /** Настройка модуля: галочка, число или список. */
    public static final class Setting {
        public final String name;
        public final Kind kind;
        public boolean bool;
        public double value;
        public double min;
        public double max;
        public double step;
        public String mode;
        public String[] modes = new String[0];

        private Setting(String name, Kind kind) {
            this.name = name;
            this.kind = kind;
        }

        public static Setting bool(String name, boolean def) {
            Setting s = new Setting(name, Kind.BOOL);
            s.bool = def;
            return s;
        }

        public static Setting num(String name, double def, double min, double max, double step) {
            Setting s = new Setting(name, Kind.NUM);
            s.value = def;
            s.min = min;
            s.max = max;
            s.step = step <= 0 ? 1 : step;
            return s;
        }

        public static Setting mode(String name, String def, String... modes) {
            Setting s = new Setting(name, Kind.MODE);
            s.modes = modes;
            s.mode = def;
            return s;
        }

        public void setFromRatio(double ratio) {
            double raw = min + (max - min) * Math.max(0, Math.min(1, ratio));
            value = Math.round(raw / step) * step;
        }

        public double ratio() {
            if (max <= min) return 0;
            return (value - min) / (max - min);
        }

        public String display() {
            if (kind == Kind.BOOL) return bool ? "вкл" : "выкл";
            if (kind == Kind.MODE) return mode;
            if (step >= 1) return String.valueOf((int) Math.round(value));
            return String.format("%.1f", value);
        }
    }

    public final String name;
    public final Category category;
    private boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected Setting bool(String name, boolean def) {
        Setting s = Setting.bool(name, def);
        settings.add(s);
        return s;
    }

    protected Setting num(String name, double def, double min, double max, double step) {
        Setting s = Setting.num(name, def, min, max, step);
        settings.add(s);
        return s;
    }

    protected Setting mode(String name, String def, String... modes) {
        Setting s = Setting.mode(name, def, modes);
        settings.add(s);
        return s;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean on) {
        if (this.enabled == on) return;
        this.enabled = on;
        if (on) onEnable();
        else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick() {}

    public void onHud(DrawContext ctx) {}

    public void onWorld(WorldRenderContext ctx) {}

    public Setting setting(String name) {
        for (Setting s : settings) {
            if (s.name.equalsIgnoreCase(name)) return s;
        }
        return null;
    }
}
