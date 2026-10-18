package dev.loopclient.util;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Свой рендер шрифта из PNG-атласа (16 колонок, ячейка 12x14).
 * Текстуру грузит сам Minecraft по идентификатору — без NativeImage и регистрации.
 * Если что-то не так, draw() вернёт false, и Ui откатится на ванильный текст.
 */
public final class LoopFont {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final int COLS = 16;
    public static final int CELL_W = 12;
    public static final int CELL_H = 14;
    public static final int ATLAS_W = 192;
    public static final int ATLAS_H = 154;

    public static final Identifier UI_TEX = Identifier.of("loopclient", "font/ui");
    public static final Identifier SERIF_TEX = Identifier.of("loopclient", "font/serif");

    private static final Map<Character, Integer> INDEX = new HashMap<>();
    private static final int[] UI_ADVANCE;
    private static final int[] SERIF_ADVANCE;

    static {
        StringBuilder sb = new StringBuilder();
        for (int c = 32; c < 127; c++) sb.append((char) c);
        for (int c = 0x0410; c <= 0x044F; c++) sb.append((char) c);
        sb.append("Ёё—«»…°№");
        for (int i = 0; i < sb.length(); i++) {
            char ch = sb.charAt(i);
            if (!INDEX.containsKey(ch)) INDEX.put(ch, INDEX.size());
        }
        UI_ADVANCE = new int[INDEX.size()];
        SERIF_ADVANCE = new int[INDEX.size()];
        for (Map.Entry<Character, Integer> e : INDEX.entrySet()) {
            char ch = e.getKey();
            int idx = e.getValue();
            int w = (ch == ' ') ? 4 : measure(ch);
            UI_ADVANCE[idx] = Math.max(2, w);
            SERIF_ADVANCE[idx] = Math.max(2, w + 1);
        }
    }

    private LoopFont() {}

    /** Приблизительная ширина глифа в атласе (UI-шрифт, 11 px). */
    private static int measure(char ch) {
        if ("iIl.,:;'!|".indexOf(ch) >= 0) return 3;
        if ("fjrt()[]-«»".indexOf(ch) >= 0) return 5;
        if (Character.isUpperCase(ch)) return 9;
        return 7;
    }

    /** Нарисовать строку. false — рисовать атласом нельзя, нужен фоллбек. */
    public static boolean draw(DrawContext ctx, String text, int x, int y, int color, boolean serif) {
        if (text == null || text.isEmpty()) return true;
        try {
            Identifier tex = serif ? SERIF_TEX : UI_TEX;
            int[] advance = serif ? SERIF_ADVANCE : UI_ADVANCE;

            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = ((color >>> 24) & 0xFF) / 255f;
            RenderSystem.setShaderColor(r, g, b, a);

            int drawX = x;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                Integer idx = INDEX.get(ch);
                if (idx == null) {
                    drawX += 5;
                    continue;
                }
                int u = (idx % COLS) * CELL_W;
                int v = (idx / COLS) * CELL_H;
                ctx.drawTexture(tex, drawX, y, 0, (float) u, (float) v,
                    CELL_W, CELL_H, ATLAS_W, ATLAS_H);
                drawX += advance[idx];
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            return true;
        } catch (Throwable t) {
            try {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            } catch (Throwable ignored) {
                // ничего
            }
            return false;
        }
    }

    /** Ширина строки по метрикам атласа. */
    public static int width(String text, boolean serif) {
        if (text == null || text.isEmpty()) return 0;
        int[] advance = serif ? SERIF_ADVANCE : UI_ADVANCE;
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            Integer idx = INDEX.get(text.charAt(i));
            w += idx == null ? 5 : advance[idx];
        }
        return w;
    }

    public static boolean has(char ch) {
        return INDEX.containsKey(ch);
    }
}
