package dev.loopclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Звуки интерфейса на ванильных SoundEvents — без внешних файлов. */
public final class Sfx {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Sfx() {}

    public static void click() {
        play(SoundEvents.UI_BUTTON_CLICK, 0.55f);
    }

    public static void open() {
        play(SoundEvents.UI_TOAST_IN, 0.45f);
    }

    public static void toggle(boolean on) {
        play(on ? SoundEvents.BLOCK_NOTE_BLOCK_PLING : SoundEvents.UI_BUTTON_CLICK, 0.4f);
    }

    public static void error() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.5f);
    }

    private static void play(net.minecraft.registry.entry.RegistryEntry<SoundEvent> sound, float volume) {
        try {
            if (mc.getSoundManager() == null || sound == null) return;
            mc.getSoundManager().play(PositionedSoundInstance.master(sound, volume));
        } catch (Throwable ignored) {
            // звук не должен ломать меню
        }
    }
}
