package dev.loopclient.module;

import dev.loopclient.modules.HudModules;
import dev.loopclient.modules.MiscModules;
import dev.loopclient.modules.MoreModules;
import dev.loopclient.modules.PlayerModules;
import dev.loopclient.modules.RenderModules;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;

/** Реестр модулей и рассылка хуков. */
public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    private Modules() {}

    public static void init() {
        // HUD — базовая пачка
        add(new HudModules.Watermark());
        add(new HudModules.Coords());
        add(new HudModules.FpsPing());
        add(new HudModules.Armor());
        add(new HudModules.Potions());
        add(new HudModules.Totems());

        // HUD — вторая пачка
        add(new MoreModules.Keystrokes());
        add(new MoreModules.Speed());
        add(new MoreModules.Clock());
        add(new MoreModules.Direction());
        add(new MoreModules.ServerInfoHud());
        add(new MoreModules.PlayerInfoHud());
        add(new MoreModules.Memory());
        add(new MoreModules.Cps());
        add(new MoreModules.HealthBar());
        add(new MoreModules.TargetHud());

        // визуал
        add(new RenderModules.Esp());
        add(new RenderModules.Hitboxes());

        // игрок
        add(new PlayerModules.Fullbright());
        add(new PlayerModules.Zoom());
        add(new MoreModules.CustomFov());

        // разное
        add(new MiscModules.Panic());
        add(new MiscModules.Session());
        add(new MiscModules.AutoSave());
    }

    public static void add(Module module) {
        ALL.add(module);
    }

    public static Module get(String name) {
        for (Module m : ALL) {
            if (m.name.equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public static List<Module> of(Module.Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : ALL) {
            if (m.category == category) list.add(m);
        }
        return list;
    }

    public static void tick() {
        for (Module m : ALL) {
            if (m.isEnabled()) m.onTick();
        }
    }

    public static void hud(DrawContext ctx) {
        for (Module m : ALL) {
            if (!m.isEnabled()) continue;
            try {
                m.onHud(ctx);
            } catch (Throwable ignored) {
                // один сломанный модуль не должен ломать весь HUD
            }
        }
    }

    public static void world(WorldRenderContext ctx) {
        for (Module m : ALL) {
            if (!m.isEnabled()) continue;
            try {
                m.onWorld(ctx);
            } catch (Throwable ignored) {
                // и рендер мира тоже
            }
        }
    }
}
