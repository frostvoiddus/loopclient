package dev.loopclient.gui;

import dev.loopclient.module.Module;
import dev.loopclient.module.Modules;
import dev.loopclient.util.Config;
import dev.loopclient.util.Sfx;
import dev.loopclient.util.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI в стиле PulseVisuals: тёмная панель, фиолетовый акцент,
 * карточки модулей с тумблерами, поиск и категории.
 */
public class ClickGui extends Screen {
    private static final int W = 640;
    private static final int H = 380;
    private static final int CARD_W = 286;
    private static final int CARD_H = 44;
    private static final int GAP = 12;

    private static final int BG = 0xF00A0A0F;
    private static final int BG_HEAD = 0xFF0D0D14;
    private static final int CARD = 0xFF101018;
    private static final int CARD_HOVER = 0xFF16161F;
    private static final int BORDER = 0xFF1D1D28;
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int ACCENT_SOFT = 0xFF6D4AFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF7C7C8C;
    private static final int DIM = 0xFF4A4A58;

    private Module.Category category = Module.Category.VISUAL;
    private String query = "";
    private int scroll;
    private Module selected;
    private final long openedAt = System.currentTimeMillis();

    public ClickGui() {
        super(Text.literal("loopclient"));
    }

    private int px() {
        return width / 2 - W / 2;
    }

    private int py() {
        return height / 2 - H / 2;
    }

    private float fade() {
        float t = (System.currentTimeMillis() - openedAt) / 180.0f;
        return Math.max(0f, Math.min(1f, t));
    }

    private static int fadeColor(int color, float a) {
        int al = (int) (((color >>> 24) & 0xFF) * a);
        return (al << 24) | (color & 0x00FFFFFF);
    }

    private List<Module> visible() {
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.of(category)) {
            if (query.isEmpty() || m.name.toLowerCase().contains(query.toLowerCase())) out.add(m);
        }
        return out;
    }

    private int columns() {
        return 2;
    }

    private int gridTop() {
        return py() + 104;
    }

    private int gridBottom() {
        return py() + H - 30;
    }

    private int maxScroll() {
        int rows = (visible().size() + columns() - 1) / columns();
        int visibleRows = (gridBottom() - gridTop()) / (CARD_H + GAP);
        return Math.max(0, rows - Math.max(1, visibleRows));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = fade();
        ctx.fill(0, 0, width, height, fadeColor(0x99000000, a));

        int x = px();
        int y = py();

        // панель
        Ui.roundRect(ctx, x + 4, y + 6, W, H, 12, fadeColor(0x66000000, a));
        Ui.roundRect(ctx, x, y, W, H, 12, fadeColor(BG, a));
        Ui.roundRect(ctx, x, y, W, 46, 12, fadeColor(BG_HEAD, a));
        Ui.rect(ctx, x, y + 40, W, 1, fadeColor(BORDER, a));

        // лого + название
        Ui.rect(ctx, x + 18, y + 17, 12, 12, fadeColor(ACCENT, a));
        drawSerif(ctx, "loopclient", x + 38, y + 14, fadeColor(TEXT, a));
        Ui.text(ctx, "1.0", x + 38 + Ui.serifWidth("loopclient") + 8, y + 18, fadeColor(DIM, a));

        // вкладки
        int tx = x + W - 18;
        Module.Category[] cats = Module.Category.values();
        for (int i = cats.length - 1; i >= 0; i--) {
            Module.Category c = cats[i];
            int tw = Ui.width(c.label) + 22;
            tx -= tw;
            boolean active = c == category;
            boolean hover = mouseX >= tx && mouseX <= tx + tw && mouseY >= y + 10 && mouseY <= y + 36;
            if (active) {
                Ui.roundRect(ctx, tx, y + 10, tw, 26, 8, fadeColor(0xFF1A1A26, a));
                Ui.roundRect(ctx, tx, y + 34, tw, 2, 1, fadeColor(ACCENT, a));
            } else if (hover) {
                Ui.roundRect(ctx, tx, y + 10, tw, 26, 8, fadeColor(0xFF15151E, a));
            }
            Ui.text(ctx, c.label, tx + 11, y + 18, fadeColor(active ? TEXT : MUTED, a));
            tx -= 6;
        }

        // поиск
        int sx = x + 18;
        int sy = y + 58;
        int sw = W - 36;
        Ui.roundRect(ctx, sx, sy, sw, 30, 8, fadeColor(0xFF12121A, a));
        Ui.rect(ctx, sx + 1, sy + 1, sw - 2, 1, fadeColor(0xFF171722, a));
        Ui.text(ctx, query.isEmpty() ? "поиск модуля…" : query, sx + 12, sy + 11, fadeColor(query.isEmpty() ? DIM : TEXT, a));
        String count = visible().size() + "";
        Ui.text(ctx, count, sx + sw - Ui.width(count) - 12, sy + 11, fadeColor(DIM, a));

        // карточки
        List<Module> list = visible();
        int cols = columns();
        int top = gridTop();
        int bottom = gridBottom();
        int startIndex = scroll * cols;

        for (int i = 0; i < list.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            if (row < scroll) continue;
            int cy = top + (row - scroll) * (CARD_H + GAP);
            if (cy + CARD_H > bottom) break;

            Module m = list.get(i);
            int cx = x + 18 + col * (CARD_W + GAP);
            boolean hover = mouseX >= cx && mouseX <= cx + CARD_W && mouseY >= cy && mouseY <= cy + CARD_H;
            boolean on = m.isEnabled();

            Ui.roundRect(ctx, cx, cy, CARD_W, CARD_H, 9,
                fadeColor(hover ? CARD_HOVER : CARD, a));
            Ui.rect(ctx, cx + 1, cy + 1, CARD_W - 2, 1, fadeColor(on ? 0xFF23233A : BORDER, a));

            if (m == selected) {
                Ui.roundRect(ctx, cx, cy, 3, CARD_H, 2, fadeColor(ACCENT, a));
            }

            Ui.text(ctx, m.name, cx + 14, cy + 11, fadeColor(on ? TEXT : 0xFF9A9AAA, a));
            if (!m.settings.isEmpty()) {
                Ui.text(ctx, "настройки", cx + 14, cy + 26, fadeColor(DIM, a));
            }

            // тумблер
            int tw = 30;
            int th = 16;
            int tx2 = cx + CARD_W - tw - 14;
            int ty2 = cy + 14;
            Ui.roundRect(ctx, tx2, ty2, tw, th, th / 2, fadeColor(on ? ACCENT_SOFT : 0xFF22222C, a));
            int kx = on ? tx2 + tw - th + 2 : tx2 + 2;
            Ui.roundRect(ctx, kx, ty2 + 2, th - 4, th - 4, (th - 4) / 2, fadeColor(TEXT, a));
        }

        if (list.isEmpty()) {
            Ui.text(ctx, "ничего не найдено", x + 18, top + 8, fadeColor(DIM, a));
        }

        // полоса прокрутки
        if (maxScroll() > 0) {
            int trackX = x + W - 7;
            int trackH = bottom - top;
            Ui.roundRect(ctx, trackX, top, 3, trackH, 2, fadeColor(0xFF15151E, a));
            int barH = Math.max(24, trackH * trackH / ((list.size() / cols + 1) * (CARD_H + GAP)));
            int barY = top + (scroll * (trackH - barH)) / Math.max(1, maxScroll());
            Ui.roundRect(ctx, trackX, barY, 3, barH, 2, fadeColor(ACCENT, a));
        }

        // настройки выбранного модуля — тонкой строкой в подвале
        Ui.rect(ctx, x, y + H - 26, W, 1, fadeColor(BORDER, a));
        String hint = selected == null
            ? "клик по модулю — вкл/выкл · ПКМ — настройки · RightShift / Esc — закрыть"
            : selected.name + " · " + (selected.settings.isEmpty() ? "нет настроек" : selected.settings.size() + " настроек");
        Ui.text(ctx, hint, x + 18, y + H - 17, fadeColor(DIM, a));
    }

    private void drawSerif(DrawContext ctx, String text, int x, int y, int color) {
        try {
            Ui.serif(ctx, text, x, y, color);
        } catch (Throwable ignored) {
            Ui.text(ctx, text, x, y, color);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = px();
        int y = py();

        // вкладки
        int tx = x + W - 18;
        Module.Category[] cats = Module.Category.values();
        for (int i = cats.length - 1; i >= 0; i--) {
            Module.Category c = cats[i];
            int tw = Ui.width(c.label) + 22;
            tx -= tw;
            if (mx >= tx && mx <= tx + tw && my >= y + 10 && my <= y + 36) {
                category = c;
                scroll = 0;
                selected = null;
                Sfx.click();
                return true;
            }
            tx -= 6;
        }

        // карточки
        List<Module> list = visible();
        int cols = columns();
        int top = gridTop();
        int bottom = gridBottom();
        for (int i = 0; i < list.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            if (row < scroll) continue;
            int cy = top + (row - scroll) * (CARD_H + GAP);
            if (cy + CARD_H > bottom) break;
            int cx = x + 18 + col * (CARD_W + GAP);
            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                Module m = list.get(i);
                if (button == 1) {
                    selected = m;
                    Sfx.click();
                    return true;
                }
                m.toggle();
                Sfx.toggle(m.isEnabled());
                Config.save();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (query.length() < 24 && chr >= 32) {
            query += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (!query.isEmpty()) {
                query = "";
                return true;
            }
            if (client != null) client.setScreen(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
