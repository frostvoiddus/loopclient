package dev.loopclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Звуки интерфейса на ванильных SoundEvents — без внешних файлов.
 * В 1.21.4 поля SoundEvents бывают и SoundEvent, и RegistryEntry.Reference,
 * поэтому принимаем Object и разбираем тип на месте.
 */
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
        if (on) {
            play(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.4f);
        } else {
            play(SoundEvents.UI_BUTTON_CLICK, 0.4f);
        }
    }

    public static void error() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.5f);
    }

    @SuppressWarnings("unchecked")
    private static void play(Object sound, float volume) {
        try {
            if (sound == null || mc.getSoundManager() == null) return;
            if (sound instanceof RegistryEntry<?> entry) {
                mc.getSoundManager().play(PositionedSoundInstance.master(
                    (RegistryEntry<SoundEvent>) entry, volume));
            } else if (sound instanceof SoundEvent event) {
                mc.getSoundManager().play(PositionedSoundInstance.master(event, volume));
            }
        } catch (Throwable ignored) {
            // звук не должен ломать меню
        }
    }
}
