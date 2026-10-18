package dev.loopclient.modules;

import dev.loopclient.module.Module;
import net.minecraft.client.MinecraftClient;

/** Модули игрока: яркость и зум. */
public final class PlayerModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private PlayerModules() {}

    /** Полная яркость: гамма на максимум, при выключении возвращаем как было. */
    public static class Fullbright extends Module {
        private double previous = 1.0;

        public Fullbright() {
            super("Fullbright", Category.PLAYER);
            num("Яркость", 12, 1, 20, 1);
        }

        @Override
        public void onEnable() {
            previous = mc.options.getGamma().getValue();
            mc.options.getGamma().setValue(setting("Яркость").value);
        }

        @Override
        public void onDisable() {
            mc.options.getGamma().setValue(previous);
        }
    }

    /** Зум: сужает FOV, пока модуль включён. */
    public static class Zoom extends Module {
        private int previous = 70;

        public Zoom() {
            super("Zoom", Category.PLAYER);
            num("FOV", 30, 10, 70, 1);
        }

        @Override
        public void onEnable() {
            previous = mc.options.getFov().getValue();
            mc.options.getFov().setValue((int) setting("FOV").value);
        }

        @Override
        public void onDisable() {
            mc.options.getFov().setValue(previous);
        }
    }
}
