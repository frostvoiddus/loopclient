package dev.loopclient.gui;

import dev.loopclient.module.Module;
import dev.loopclient.module.Modules;
import dev.loopclient.util.Config;
import dev.loopclient.util.Sfx;
import dev.loopclient.util.Ui;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI: вкладки категорий, список модулей, настройки.
 * RightShift — открыть, Esc — закрыть. Анимация появления + звуки.
 */
public class ClickGui extends Screen {
    private static final int W = 560;
    private static final int H = 340;
    private static final int ROW = 26;
    private static final int LIST_W = 300;

    private Module.Category category = Module.Category.HUD;
    private Module selected;
    private int scroll;
    private final long openedAt = System.currentTimeMillis();
    private final Map<Module, Float> anim = new HashMap<>();

    public ClickGui() {
        super(Text.literal("loopclient"));
    }

    private int px() {
        return width / 2 - W / 2;
    }

    private int py() {
        return height / 2 - H / 2;
    }

    private List<Module> list() {
        return Modules.of(category);
    }

    private int listTop() {
        return py() + 62;
    }

    private int listBottom() {
        return py() + H - 26;
    }

    private int maxScroll() {
        int visible = (listBottom() - listTop()) / ROW;
        return Math.max(0, list().size() - visible);
    }

    private float fade() {
        float t = (System.currentTimeMillis() - openedAt) / 170.0f;
        return Math.max(0f, Math.min(1f, t));
    }

    private static int alpha(int color, float a) {
        int al = (int) (((color >>> 24) & 0xFF) * a);
        return (al << 24) | (color & 0x00FFFFFF);
    }

    private float animOf(Module m) {
        float target = m.isEnabled() ? 1f : 0f;
        float current = anim.getOrDefault(m, target);
        float next = current + (target - current) * 0.35f;
        if (Math.abs(next - target) < 0.01f) next = target;
        anim.put(m, next);
        return next;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = fade();
        ctx.fill(0, 0, width, height, alpha(0x99000000, a));

        int x = px();
        int y = py();
        int w = W;
        int h = H;

        Ui.rect(ctx, x + 3, y + 4, w, h, alpha(0x66000000, a));
        Ui.rect(ctx, x, y, w, h, alpha(0xF0090910, a));
        Ui.rect(ctx, x, y, w, 1, alpha(0xFF2C2C36, a));
        Ui.rect(ctx, x, y + h - 1, w, 1, alpha(0xFF1A1A22, a));

        Ui.rect(ctx, x, y, w, 46, alpha(0xFF0E0E14, a));
        Ui.rect(ctx, x, y + 45, w, 1, alpha(0xFF24242E, a));

        drawSerif(ctx, "loopclient", x + 16, y + 12, alpha(0xFFFFFFFF, a));
        Ui.text(ctx, "what makes you better", x + 16, y + 29, alpha(0xFF6A6A78, a));
        Ui.text(ctx, "1.0", x + w - Ui.width("1.0") - 16, y + 14, alpha(0xFF4A4A56, a));

        int tabX = x + 150;
        for (Module.Category c : Module.Category.values()) {
            int tw = Ui.width(c.label) + 20;
            boolean active = c == category;
            boolean hover = mouseX >= tabX && mouseX <= tabX + tw && mouseY >= y + 12 && mouseY <= y + 34;
            if (hover && !active) Ui.rect(ctx, tabX, y + 12, tw, 22, alpha(0xFF16161D, a));
            Ui.text(ctx, c.label, tabX + 10, y + 18, alpha(active ? 0xFFFFFFFF : hover ? 0xFFB9B9C4 : 0xFF6A6A78, a));
            if (active) Ui.rect(ctx, tabX + 10, y + 33, tw - 20, 2, alpha(0xFFFFFFFF, a));
            tabX += tw + 4;
        }

        Ui.rect(ctx, x + LIST_W + 6, listTop() - 8, 1, listBottom() - listTop() + 8, alpha(0xFF1E1E26, a));

        List<Module> modules = list();
        int top = listTop();
        int bottom = listBottom();
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int ry = top + (i - scroll) * ROW;
            if (ry < top || ry + ROW - 4 > bottom) continue;

            float on = animOf(m);
            boolean hover = mouseX >= x + 8 && mouseX <= x + LIST_W + 2 && mouseY >= ry && mouseY <= ry + ROW - 4;

            if (m == selected) {
                Ui.rect(ctx, x + 8, ry, LIST_W - 6, ROW - 4, alpha(0xFF1C1C24, a));
            } else if (hover) {
                Ui.rect(ctx, x + 8, ry, LIST_W - 6, ROW - 4, alpha(0xFF141419, a));
            }
            if (on > 0.02f) Ui.rect(ctx, x + 8, ry, 2, ROW - 4, alpha(0xFFFFFFFF, a * on));

            int textColor = m.isEnabled() ? 0xFFFFFFFF : 0xFF7C7C88;
            Ui.text(ctx, m.name, x + 18, ry + 7, alpha(textColor, a));

            int tw = 26;
            int th = 12;
            int tx = x + LIST_W - 14 - tw;
            int ty = ry + 6;
            Ui.rect(ctx, tx, ty, tw, th, alpha(0xFF23232B, a));
            int knobX = tx + 1 + (int) ((tw - th) * on);
            Ui.rect(ctx, knobX, ty + 1, th - 2, th - 2, alpha(m.isEnabled() ? 0xFFFFFFFF : 0xFF6A6A78, a));
        }

        if (maxScroll() > 0) {
            int trackX = x + LIST_W - 3;
            int trackH = bottom - top;
            int barH = Math.max(18, trackH * trackH / (list().size() * ROW));
            int barY = top + (scroll * (trackH - barH)) / Math.max(1, maxScroll());
            Ui.rect(ctx, trackX, top, 2, trackH, alpha(0xFF1A1A22, a));
            Ui.rect(ctx, trackX, barY, 2, barH, alpha(0xFF4A4A58, a));
        }

        int sx = x + LIST_W + 18;
        int sw = w - (LIST_W + 18) - 16;
        if (selected == null) {
            Ui.text(ctx, "выбери модуль слева", sx, top + 6, alpha(0xFF55555F, a));
        } else {
            Ui.text(ctx, selected.name, sx, top, alpha(0xFFFFFFFF, a));
            Ui.rect(ctx, sx, top + 15, sw, 1, alpha(0xFF1E1E26, a));
            int sy = top + 24;
            for (Module.Setting s : selected.settings) {
                int rowH = s.kind == Module.Kind.BOOL ? 20 : 32;
                Ui.text(ctx, s.name, sx, sy, alpha(0xFF8A8A96, a));
                Ui.text(ctx, s.display(), sx + sw - Ui.width(s.display()), sy, alpha(0xFFFFFFFF, a));

                if (s.kind == Module.Kind.NUM) {
                    int barY = sy + 18;
                    Ui.rect(ctx, sx, barY, sw, 3, alpha(0xFF23232B, a));
                    Ui.rect(ctx, sx, barY, (int) (sw * s.ratio()), 3, alpha(0xFFFFFFFF, a));
                } else if (s.kind == Module.Kind.BOOL) {
                    int tw = 26;
                    int tx = sx + sw - tw;
                    int ty = sy - 2;
                    Ui.rect(ctx, tx, ty, tw, 12, alpha(0xFF23232B, a));
                    Ui.rect(ctx, s.bool ? tx + tw - 12 : tx + 1, ty + 1, 10, 10, alpha(s.bool ? 0xFFFFFFFF : 0xFF6A6A78, a));
                }
                sy += rowH + 8;
            }
            if (selected.settings.isEmpty()) {
                Ui.text(ctx, "у модуля нет настроек", sx, top + 28, alpha(0xFF55555F, a));
            }
        }

        Ui.rect(ctx, x, y + h - 24, w, 1, alpha(0xFF1A1A22, a));
        String hint = "RightShift / Esc — закрыть · клик — вкл/выкл · колесо — прокрутка";
        Ui.text(ctx, hint, x + (w - Ui.width(hint)) / 2, y + h - 15, alpha(0xFF4A4A56, a));
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

        int tabX = x + 150;
        for (Module.Category c : Module.Category.values()) {
            int tw = Ui.width(c.label) + 20;
            if (mx >= tabX && mx <= tabX + tw && my >= y + 12 && my <= y + 34) {
                category = c;
                selected = null;
                scroll = 0;
                Sfx.click();
                return true;
            }
            tabX += tw + 4;
        }

        List<Module> modules = list();
        int top = listTop();
        int bottom = listBottom();
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int ry = top + (i - scroll) * ROW;
            if (ry < top || ry + ROW - 4 > bottom) continue;
            if (mx >= x + 8 && mx <= x + LIST_W + 2 && my >= ry && my <= ry + ROW - 4) {
                selected = m;
                if (button == 0) {
                    m.toggle();
                    Sfx.toggle(m.isEnabled());
                    Config.save();
                }
                return true;
            }
        }

        if (selected != null) {
            int sx = x + LIST_W + 18;
            int sw = W - (LIST_W + 18) - 16;
            int sy = top + 24;
            for (Module.Setting s : selected.settings) {
                int rowH = s.kind == Module.Kind.BOOL ? 20 : 32;
                if (my >= sy - 2 && my <= sy + rowH && mx >= sx && mx <= sx + sw) {
                    if (s.kind == Module.Kind.BOOL) {
                        s.bool = !s.bool;
                    } else if (s.kind == Module.Kind.MODE) {
                        if (s.modes.length > 0) {
                            int idx = 0;
                            for (int k = 0; k < s.modes.length; k++) {
                                if (s.modes[k].equals(s.mode)) idx = k;
                            }
                            s.mode = s.modes[(idx + 1) % s.modes.length];
                        }
                    } else {
                        s.setFromRatio((mx - sx) / (double) sw);
                    }
                    Sfx.click();
                    Config.save();
                    return true;
                }
                sy += rowH + 8;
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (client != null) client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
