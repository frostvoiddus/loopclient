package dev.loopclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Мелкие помощники для рисования. */
public final class Ui {
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final int WHITE = 0xFFFFFFFF;
    public static final int GRAY = 0xFF9A9AA4;
    public static final int DIM = 0xFF65656E;
    public static final int PANEL = 0xF0121216;
    public static final int PANEL_2 = 0xFF17171C;
    public static final int ACCENT = 0xFFFFFFFF;
    public static final int OK = 0xFF7EE2A8;
    public static final int BAD = 0xFFF87171;

    /** Шрифт как на сайте: Instrument Serif Italic. */
    public static final Style SERIF = Style.EMPTY.withFont(Identifier.of("loopclient", "serif"));

    private Ui() {}

    public static void rect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    /** Прямоугольник со скруглёнными углами (ступеньками). */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (r < 1 || w < 4 || h < 4) {
            rect(ctx, x, y, w, h, color);
            return;
        }
        int rr = Math.min(r, Math.min(w / 2, h / 2));
        rect(ctx, x + rr, y, w - rr * 2, h, color);
        rect(ctx, x, y + rr, rr, h - rr * 2, color);
        rect(ctx, x + w - rr, y + rr, rr, h - rr * 2, color);
        for (int i = 0; i < rr; i++) {
            int len = (int) Math.ceil(Math.sqrt((double) rr * rr - (double) (rr - i - 1) * (rr - i - 1)));
            if (len < 1) len = 1;
            rect(ctx, x + rr - len, y + i, len, 1, color);
            rect(ctx, x + w - rr, y + i, len, 1, color);
            rect(ctx, x + rr - len, y + h - i - 1, len, 1, color);
            rect(ctx, x + w - rr, y + h - i - 1, len, 1, color);
        }
    }

    public static void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(mc.textRenderer, s, x, y, color, true);
    }

    public static void serif(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(mc.textRenderer, Text.literal(s).setStyle(Style.EMPTY.withItalic(true)), x, y, color, true);
    }

    public static int serifWidth(String s) {
        return mc.textRenderer.getWidth(Text.literal(s).setStyle(Style.EMPTY.withItalic(true)));
    }

    public static int width(String s) {
        return mc.textRenderer.getWidth(s);
    }

    public static String trim(String s, int maxWidth) {
        if (width(s) <= maxWidth) return s;
        String out = s;
        while (out.length() > 1 && width(out + "…") > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "…";
    }

    public static String duration(int ticks) {
        int seconds = ticks / 20;
        int m = seconds / 60;
        int s = seconds % 60;
        return m > 0 ? m + "м " + s + "с" : s + "с";
    }
}
