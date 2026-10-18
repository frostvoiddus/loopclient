package dev.loopclient.modules;

import dev.loopclient.module.Module;
import dev.loopclient.module.Modules;
import dev.loopclient.util.Config;
import dev.loopclient.util.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Служебные функции (вкладка «Разное»). */
public final class MiscModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private MiscModules() {}

    /** Паника: выключает всё разом. */
    public static class Panic extends Module {
        public Panic() {
            super("Panic", Category.MISC);
        }

        @Override
        public void onEnable() {
            for (Module m : Modules.ALL) {
                if (m != this && m.isEnabled()) m.setEnabled(false);
            }
            Config.save();
            setEnabled(false);
        }
    }

    /** Время сессии в HUD. */
    public static class Session extends Module {
        private long startedAt;

        public Session() {
            super("Session", Category.MISC);
        }

        @Override
        public void onEnable() {
            startedAt = System.currentTimeMillis();
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (startedAt == 0) startedAt = System.currentTimeMillis();
            long sec = (System.currentTimeMillis() - startedAt) / 1000L;
            long h = sec / 3600;
            long m = (sec % 3600) / 60;
            String text = "session " + (h > 0 ? h + "ч " : "") + m + "м";
            int sw = mc.getWindow().getScaledWidth();
            Ui.text(ctx, text, sw - Ui.width(text) - 6, 66, Ui.GRAY);
        }
    }

    /** Автосохранение конфига раз в минуту. */
    public static class AutoSave extends Module {
        private int ticks;

        public AutoSave() {
            super("AutoSave", Category.MISC);
        }

        @Override
        public void onTick() {
            ticks++;
            if (ticks >= 1200) {
                ticks = 0;
                Config.save();
            }
        }
    }
}
