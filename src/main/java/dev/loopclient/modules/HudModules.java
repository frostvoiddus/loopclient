package dev.loopclient.modules;

import dev.loopclient.module.Module;
import dev.loopclient.util.Ui;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.effect.StatusEffectInstance;

/** HUD-модули: всё, что рисуется поверх игры. */
public final class HudModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private HudModules() {}

    /** Надпись клиента + FPS. */
    public static class Watermark extends Module {
        private final Setting fps = bool("Показывать FPS", true);
        private final Setting right = mode("Позиция", "слева", "слева", "справа");

        public Watermark() {
            super("Watermark", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.getWindow() == null) return;
            String title = "loopclient";
            String meta = fps.bool ? mc.getCurrentFps() + " fps" : "";
            int pad = 8;
            int titleW = Ui.serifWidth(title);
            int w = titleW + pad * 2 + (meta.isEmpty() ? 0 : Ui.width(meta) + 10);
            int sw = mc.getWindow().getScaledWidth();
            int x = right.mode.equals("справа") ? sw - w - 4 : 4;
            int y = 4;
            Ui.rect(ctx, x, y, w, 18, 0xAA000000);
            Ui.rect(ctx, x, y, 2, 18, Ui.WHITE);
            Ui.serif(ctx, title, x + pad + 2, y + 4, Ui.WHITE);
            if (!meta.isEmpty()) Ui.text(ctx, meta, x + w - Ui.width(meta) - pad, y + 5, Ui.GRAY);
        }
    }

    /** Координаты и направление. */
    public static class Coords extends Module {
        private final Setting precise = bool("Дробные", false);

        public Coords() {
            super("Coordinates", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null) return;
            String fmt = precise.bool ? "%.1f" : "%.0f";
            String line = String.format(Locale.ROOT, "xyz " + fmt + " " + fmt + " " + fmt,
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
            String dir = mc.player.getHorizontalFacing().asString();
            int w = Math.max(Ui.width(line), Ui.width(dir)) + 14;
            int y = 26;
            Ui.rect(ctx, 4, y, w, 30, 0x99000000);
            Ui.text(ctx, line, 11, y + 4, Ui.WHITE);
            Ui.text(ctx, dir, 11, y + 16, Ui.GRAY);
        }
    }

    /** FPS и пинг. */
    public static class FpsPing extends Module {
        public FpsPing() {
            super("FpsPing", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.getWindow() == null) return;
            String fps = mc.getCurrentFps() + " fps";
            String ping = "";
            if (mc.player != null && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency() + " ms";
            }
            int sw = mc.getWindow().getScaledWidth();
            int w = Ui.width(fps) + (ping.isEmpty() ? 0 : Ui.width(ping) + 10) + 16;
            int x = sw - w - 4;
            int y = 26;
            Ui.rect(ctx, x, y, w, 18, 0x99000000);
            Ui.text(ctx, fps, x + 8, y + 5, Ui.WHITE);
            if (!ping.isEmpty()) Ui.text(ctx, ping, x + w - Ui.width(ping) - 8, y + 5, Ui.GRAY);
        }
    }

    /** Броня и прочность. */
    public static class Armor extends Module {
        public Armor() {
            super("ArmorHUD", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null) return;
            PlayerInventory inv = mc.player.getInventory();
            int y = 60;
            for (int slot = 36; slot <= 39; slot++) {
                ItemStack stack = inv.getStack(slot);
                if (stack.isEmpty()) continue;
                String name = stack.getName().getString();
                String extra = "";
                if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                    int left = stack.getMaxDamage() - stack.getDamage();
                    int pct = (int) Math.round(100.0 * left / stack.getMaxDamage());
                    extra = pct + "%";
                }
                Ui.rect(ctx, 4, y, 132, 14, 0x99000000);
                Ui.text(ctx, Ui.trim(name, 96), 8, y + 3, Ui.WHITE);
                if (!extra.isEmpty()) Ui.text(ctx, extra, 128 - Ui.width(extra), y + 3, Ui.GRAY);
                y += 16;
            }
        }
    }

    /** Активные эффекты с таймером. */
    public static class Potions extends Module {
        public Potions() {
            super("PotionHUD", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null) return;
            Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
            int y = 110;
            for (StatusEffectInstance effect : effects) {
                String name = net.minecraft.text.Text.translatable(
                    effect.getEffectType().value().getTranslationKey()).getString();
                String amp = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
                String time = Ui.duration(effect.getDuration());
                String line = name + amp;
                Ui.rect(ctx, 4, y, 150, 14, 0x99000000);
                Ui.text(ctx, Ui.trim(line, 100), 8, y + 3, Ui.WHITE);
                Ui.text(ctx, time, 150 - Ui.width(time), y + 3, Ui.GRAY);
                y += 16;
            }
        }
    }

    /** Счётчик тотемов в инвентаре. */
    public static class Totems extends Module {
        public Totems() {
            super("TotemCounter", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null) return;
            PlayerInventory inv = mc.player.getInventory();
            int total = 0;
            for (int slot = 0; slot <= 40; slot++) {
                ItemStack stack = inv.getStack(slot);
                if (stack.getItem() == Items.TOTEM_OF_UNDYING) total += stack.getCount();
            }
            String text = "totems " + total;
            int w = Ui.width(text) + 16;
            int sw = mc.getWindow().getScaledWidth();
            int x = sw - w - 4;
            int y = 48;
            Ui.rect(ctx, x, y, w, 18, 0x99000000);
            Ui.text(ctx, text, x + 8, y + 5, total > 0 ? Ui.OK : Ui.BAD);
        }
    }
}
