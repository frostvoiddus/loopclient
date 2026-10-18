package dev.loopclient.util;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Свой рендер шрифта: глифы берутся из PNG-атласа (16 колонок, ячейка 12x14).
 * Такой способ не зависит от парсера шрифтов Minecraft и работает всегда,
 * с кириллицей и латиницей. Ширина каждого глифа считается по прозрачности.
 */
public final class LoopFont {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final int COLS = 16;
    public static final int CELL_W = 12;
    public static final int CELL_H = 14;

    public static final Identifier UI_TEX = Identifier.of("loopclient", "font/ui");
    public static final Identifier SERIF_TEX = Identifier.of("loopclient", "font/serif");

    private static final Map<Character, Integer> INDEX = new HashMap<>();
    private static final String CHARSET = buildCharset();

    private static boolean loaded;
    private static boolean failed;
    private static final Map<Identifier, int[]> ADVANCE = new HashMap<>();
    private static int atlasW = 192;
    private static int atlasH = 154;

    private LoopFont() {}

    /** Тот же порядок, что при генерации атласа. */
    private static String buildCharset() {
        StringBuilder sb = new StringBuilder();
        for (int c = 32; c < 127; c++) sb.append((char) c);
        for (int c = 0x0410; c <= 0x044F; c++) sb.append((char) c);
        sb.append("Ёё—«»…°№");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sb.length(); i++) {
            char ch = sb.charAt(i);
            if (INDEX.containsKey(ch)) continue;
            INDEX.put(ch, out.length());
            out.append(ch);
        }
        return out.toString();
    }

    public static boolean ready() {
        if (failed) return false;
        if (!loaded) load();
        return loaded;
    }

    private static void load() {
        try {
            ADVANCE.put(UI_TEX, loadTexture(UI_TEX));
            ADVANCE.put(SERIF_TEX, loadTexture(SERIF_TEX));
            loaded = ADVANCE.get(UI_TEX) != null && ADVANCE.get(SERIF_TEX) != null;
            if (!loaded) failed = true;
        } catch (Throwable t) {
            failed = true;
            loaded = false;
        }
    }

    private static int[] loadTexture(Identifier id) {
        try {
            Optional<Resource> res = mc.getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            NativeImage image;
            try (InputStream in = res.get().getInputStream()) {
                image = NativeImage.read(in);
            }
            mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));

            atlasW = image.getWidth();
            atlasH = image.getHeight();
            int rows = atlasH / CELL_H;
            int[] advance = new int[CHARSET.length()];
            for (int i = 0; i < CHARSET.length(); i++) {
                int cx = (i % COLS) * CELL_W;
                int cy = (i / COLS) * CELL_H;
                int right = 0;
                for (int x = 0; x < CELL_W; x++) {
                    for (int y = 0; y < CELL_H; y++) {
                        int px = cx + x;
                        int py = cy + y;
                        if (px >= atlasW || py >= atlasH) continue;
                        int color = image.getColor(px, py);
                        if (((color >>> 24) & 0xFF) > 8) {
                            right = Math.max(right, x + 1);
                            break;
                        }
                    }
                }
                advance[i] = Math.max(1, right + 1);
            }
            if (rows <= 0) return null;
            return advance;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Нарисовать строку. Возвращает false, если атлас недоступен. */
    public static boolean draw(DrawContext ctx, String text, int x, int y, int color, boolean serif) {
        if (!ready()) return false;
        Identifier tex = serif ? SERIF_TEX : UI_TEX;
        int[] advance = ADVANCE.get(tex);
        if (advance == null) return false;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >>> 24) & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);
        int drawX = x;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                drawX += 4;
                continue;
            }
            Integer idx = INDEX.get(ch);
            if (idx == null) {
                drawX += 5;
                continue;
            }
            int u = (idx % COLS) * CELL_W;
            int v = (idx / COLS) * CELL_H;
            // тонируем через цвет шрифта: drawTexture не умеет цвет,
            // поэтому рисуем слой и поверх — заливку не делаем (текст белый в атласе)
            ctx.drawTexture(tex, drawX, y, 0, u, v, CELL_W, CELL_H, atlasW, atlasH);
            drawX += advance[idx];
        }
        return true;
    }

    /** Ширина строки в пикселях (по метрикам атласа). */
    public static int width(String text, boolean serif) {
        if (!ready()) return mc.textRenderer.getWidth(text);
        int[] advance = ADVANCE.get(serif ? SERIF_TEX : UI_TEX);
        if (advance == null) return mc.textRenderer.getWidth(text);
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                w += 4;
                continue;
            }
            Integer idx = INDEX.get(ch);
            w += idx == null ? 5 : advance[idx];
        }
        return w;
    }
}
