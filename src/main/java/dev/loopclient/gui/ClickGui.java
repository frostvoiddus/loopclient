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
 * Меню клиента: слева рейл категорий, в центре карточки модулей,
 * справа настройки выбранного. RightShift / Esc — закрыть.
 */
public class ClickGui extends Screen {
    private static final int W = 660;
    private static final int H = 400;
    private static final int RAIL = 132;
    private static final int LIST = 300;
    private static final int ROW = 34;
    private static final int PAD = 14;

    private static final int BG = 0xF00A0A10;
    private static final int RAIL_BG = 0xFF0C0C13;
    private static final int CARD = 0xFF101018;
    private static final int CARD_HOVER = 0xFF15151F;
    private static final int CARD_ON = 0xFF14121F;
    private static final int BORDER = 0xFF1C1C27;
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8C8C9C;
    private static final int DIM = 0xFF4C4C5A;

    private Module.Category category = Module.Category.VISUAL;
    private String query = "";
    private Module selected;
    private int scroll;
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
        String q = query.toLowerCase();
        for (Module m : Modules.of(category)) {
            if (q.isEmpty() || m.name.toLowerCase().contains(q)) out.add(m);
        }
        return out;
    }

    private int listTop() {
        return py() + 66;
    }

    private int listBottom() {
        return py() + H - 30;
    }

    private int maxScroll() {
        int visibleRows = (listBottom() - listTop()) / ROW;
        return Math.max(0, visible().size() - Math.max(1, visibleRows));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = fade();
        ctx.fill(0, 0, width, height, fadeColor(0x99000000, a));

        int x = px();
        int y = py();

        // тень и окно
        Ui.roundRect(ctx, x + 5, y + 7, W, H, 14, fadeColor(0x66000000, a));
        Ui.roundRect(ctx, x, y, W, H, 14, fadeColor(BG, a));

        // левый рейл
        Ui.roundRect(ctx, x, y, RAIL, H, 14, fadeColor(RAIL_BG, a));
        Ui.rect(ctx, x + RAIL - 1, y + 14, 1, H - 28, fadeColor(BORDER, a));

        // бренд
        Ui.rect(ctx, x + 16, y + 18, 10, 10, fadeColor(ACCENT, a));
        Ui.text(ctx, "loopclient", x + 32, y + 16, fadeColor(TEXT, a));
        Ui.text(ctx, "v1.0", x + 32, y + 28, fadeColor(DIM, a));

        // категории
        int cy = y + 56;
        for (Module.Category c : Module.Category.values()) {
            boolean active = c == category;
            boolean hover = mouseX >= x + 8 && mouseX <= x + RAIL - 8 && mouseY >= cy && mouseY <= cy + 26;

            if (active) {
                Ui.roundRect(ctx, x + 8, cy, RAIL - 16, 26, 8, fadeColor(0xFF171722, a));
                Ui.rect(ctx, x + 8, cy + 7, 2, 12, fadeColor(ACCENT, a));
            } else if (hover) {
                Ui.roundRect(ctx, x + 8, cy, RAIL - 16, 26, 8, fadeColor(0xFF12121A, a));
            }

            int count = Modules.of(c).size();
            Ui.text(ctx, c.label, x + 20, cy + 9, fadeColor(active ? TEXT : MUTED, a));
            String cnt = String.valueOf(count);
            Ui.text(ctx, cnt, x + RAIL - 16 - Ui.width(cnt), cy + 9, fadeColor(DIM, a));
            cy += 30;
        }

        // поиск
        int sx = x + RAIL + PAD;
        int sw = LIST;
        Ui.roundRect(ctx, sx, y + 18, sw, 32, 8, fadeColor(0xFF12121A, a));
        Ui.text(ctx, query.isEmpty() ? "поиск…" : query, sx + 12, y + 28, fadeColor(query.isEmpty() ? DIM : TEXT, a));
        String found = String.valueOf(visible().size());
        Ui.text(ctx, found, sx + sw - Ui.width(found) - 12, y + 28, fadeColor(DIM, a));

        // список карточек
        List<Module> list = visible();
        int top = listTop();
        int bottom = listBottom();
        for (int i = 0; i < list.size(); i++) {
            int row = i - scroll;
            int ry = top + row * ROW;
            if (ry < top - ROW || ry + ROW - 6 > bottom + ROW) continue;
            if (ry < top || ry + ROW - 6 > bottom) continue;

            Module m = list.get(i);
            boolean on = m.isEnabled();
            boolean hover = mouseX >= sx && mouseX <= sx + sw && mouseY >= ry && mouseY <= ry + ROW - 6;

            Ui.roundRect(ctx, sx, ry, sw, ROW - 6, 8, fadeColor(hover ? CARD_HOVER : on ? CARD_ON : CARD, a));
            Ui.rect(ctx, sx + 1, ry + 1, sw - 2, 1, fadeColor(on ? 0xFF2A2340 : BORDER, a));

            if (on) Ui.roundRect(ctx, sx, ry, 3, ROW - 6, 2, fadeColor(ACCENT, a));

            Ui.text(ctx, m.name, sx + 14, ry + 7, fadeColor(on ? TEXT : MUTED, a));
            String sub = m.settings.isEmpty() ? "без настроек" : m.settings.size() + " настроек";
            Ui.text(ctx, sub, sx + 14, ry + 18, fadeColor(DIM, a));

            // тумблер
            int tw = 30;
            int th = 15;
            int tx = sx + sw - tw - 12;
            int ty = ry + (ROW - 6 - th) / 2;
            Ui.roundRect(ctx, tx, ty, tw, th, th / 2, fadeColor(on ? ACCENT : 0xFF24242E, a));
            int kx = on ? tx + tw - th + 2 : tx + 2;
            Ui.roundRect(ctx, kx, ty + 2, th - 4, th - 4, (th - 4) / 2, fadeColor(TEXT, a));
        }

        if (list.isEmpty()) {
            Ui.text(ctx, "ничего не найдено", sx + 12, top + 6, fadeColor(DIM, a));
        }

        // скроллбар
        if (maxScroll() > 0) {
            int trackX = sx + sw + 4;
            int trackH = bottom - top;
            Ui.roundRect(ctx, trackX, top, 3, trackH, 2, fadeColor(0xFF15151E, a));
            int barH = Math.max(22, trackH * trackH / (list.size() * ROW));
            int barY = top + (scroll * (trackH - barH)) / Math.max(1, maxScroll());
            Ui.roundRect(ctx, trackX, barY, 3, barH, 2, fadeColor(ACCENT, a));
        }

        // правая панель — настройки
        int gx = sx + sw + 16;
        int gw = x + W - 16 - gx;
        Ui.roundRect(ctx, gx, y + 18, gw, H - 48, 10, fadeColor(0xFF0D0D14, a));
        if (selected == null) {
            Ui.text(ctx, "клик по модулю", gx + 12, y + 30, fadeColor(DIM, a));
            Ui.text(ctx, "ПКМ — выбрать", gx + 12, y + 42, fadeColor(DIM, a));
        } else {
            Ui.text(ctx, Ui.trim(selected.name, gw - 24), gx + 12, y + 30, fadeColor(TEXT, a));
            Ui.rect(ctx, gx + 12, y + 44, gw - 24, 1, fadeColor(BORDER, a));
            int sy = y + 54;
            for (Module.Setting s : selected.settings) {
                if (sy > y + H - 60) break;
                Ui.text(ctx, Ui.trim(s.name, gw - 40), gx + 12, sy, fadeColor(MUTED, a));
                Ui.text(ctx, s.display(), gx + gw - 12 - Ui.width(s.display()), sy, fadeColor(TEXT, a));
                if (s.kind == Module.Kind.NUM) {
                    Ui.rect(ctx, gx + 12, sy + 16, gw - 24, 3, fadeColor(0xFF23232B, a));
                    Ui.rect(ctx, gx + 12, sy + 16, (int) ((gw - 24) * s.ratio()), 3, fadeColor(ACCENT, a));
                    sy += 30;
                } else if (s.kind == Module.Kind.BOOL) {
                    int tw = 24;
                    int tx = gx + gw - 12 - tw;
                    Ui.roundRect(ctx, tx, sy - 2, tw, 11, 5, fadeColor(s.bool ? ACCENT : 0xFF23232B, a));
                    Ui.roundRect(ctx, s.bool ? tx + tw - 10 : tx + 1, sy - 1, 9, 9, 4, fadeColor(TEXT, a));
                    sy += 24;
                } else {
                    sy += 24;
                }
            }
        }

        // подвал
        Ui.rect(ctx, x + RAIL, y + H - 24, W - RAIL, 1, fadeColor(BORDER, a));
        String hint = "RightShift / Esc — закрыть · ЛКМ — вкл/выкл · ПКМ — настройки · колесо — скролл";
        Ui.text(ctx, hint, x + RAIL + PAD, y + H - 16, fadeColor(DIM, a));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = px();
        int y = py();

        // категории
        int cy = y + 56;
        for (Module.Category c : Module.Category.values()) {
            if (mx >= x + 8 && mx <= x + RAIL - 8 && my >= cy && my <= cy + 26) {
                category = c;
                selected = null;
                scroll = 0;
                Sfx.click();
                return true;
            }
            cy += 30;
        }

        // карточки
        int sx = x + RAIL + PAD;
        int sw = LIST;
        List<Module> list = visible();
        int top = listTop();
        int bottom = listBottom();
        for (int i = 0; i < list.size(); i++) {
            int ry = top + (i - scroll) * ROW;
            if (ry < top || ry + ROW - 6 > bottom) continue;
            if (mx >= sx && mx <= sx + sw && my >= ry && my <= ry + ROW - 6) {
                Module m = list.get(i);
                selected = m;
                if (button == 0) {
                    m.toggle();
                    Sfx.toggle(m.isEnabled());
                    Config.save();
                }
                return true;
            }
        }

        // настройки
        if (selected != null) {
            int gx = sx + sw + 16;
            int gw = x + W - 16 - gx;
            int sy = y + 54;
            for (Module.Setting s : selected.settings) {
                if (sy > y + H - 60) break;
                boolean inRow = mx >= gx + 8 && mx <= gx + gw - 8 && my >= sy - 4;
                if (s.kind == Module.Kind.NUM) {
                    if (inRow && my <= sy + 20) {
                        s.setFromRatio((mx - (gx + 12)) / (double) (gw - 24));
                        Sfx.click();
                        Config.save();
                        return true;
                    }
                    sy += 30;
                } else if (s.kind == Module.Kind.BOOL) {
                    if (inRow && my <= sy + 12) {
                        s.bool = !s.bool;
                        Sfx.click();
                        Config.save();
                        return true;
                    }
                    sy += 24;
                } else {
                    if (inRow && my <= sy + 12) {
                        if (s.modes.length > 0) {
                            int idx = 0;
                            for (int k = 0; k < s.modes.length; k++) {
                                if (s.modes[k].equals(s.mode)) idx = k;
                            }
                            s.mode = s.modes[(idx + 1) % s.modes.length];
                        }
                        Sfx.click();
                        Config.save();
                        return true;
                    }
                    sy += 24;
                }
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
        if (query.length() < 20 && chr >= 32) {
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
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
