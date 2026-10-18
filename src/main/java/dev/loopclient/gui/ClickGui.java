package dev.loopclient.gui;

import dev.loopclient.module.Module;
import dev.loopclient.module.Modules;
import dev.loopclient.util.Config;
import dev.loopclient.util.Ui;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI: вкладки категорий, список модулей, настройки выбранного модуля.
 * Открывается RightShift, закрывается Esc или RightShift.
 */
public class ClickGui extends Screen {
    private static final int W = 520;
    private static final int H = 300;
    private static final int ROW = 20;

    private Module.Category category = Module.Category.HUD;
    private Module selected;
    private int scroll;

    public ClickGui() {
        super(Text.literal("loopclient"));
    }

    public boolean isPauseScreen() {
        return false;
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

    private int listTop(int y) {
        return y + 36;
    }

    private int listBottom(int y) {
        return y + H - 10;
    }

    private int maxScroll(int y) {
        int visible = (listBottom(y) - listTop(y)) / ROW;
        return Math.max(0, list().size() - visible);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        int x = px();
        int y = py();

        Ui.rect(ctx, x, y, W, H, 0xF00E0E12);
        Ui.rect(ctx, x, y, W, 28, Ui.PANEL_2);
        Ui.rect(ctx, x, y, W, 1, 0xFF2A2A32);

        Ui.serif(ctx, "loopclient", x + 12, y + 5, Ui.WHITE);
        Ui.text(ctx, "RightShift", x + W - Ui.width("RightShift") - 12, y + 10, Ui.DIM);

        // вкладки
        int tabX = x + 96;
        for (Module.Category c : Module.Category.values()) {
            int tw = Ui.width(c.label) + 16;
            boolean active = c == category;
            Ui.rect(ctx, tabX, y + 6, tw, 16, active ? 0xFF2A2A32 : 0x00000000);
            Ui.text(ctx, c.label, tabX + 8, y + 10, active ? Ui.WHITE : Ui.GRAY);
            tabX += tw + 4;
        }

        // список модулей
        List<Module> modules = list();
        int top = listTop(y);
        int bottom = listBottom(y);
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int ry = top + (i - scroll) * ROW;
            if (ry < top || ry + ROW - 2 > bottom) continue;

            boolean hover = mouseX >= x + 8 && mouseX <= x + 300 && mouseY >= ry && mouseY <= ry + ROW - 3;
            if (m == selected) {
                Ui.rect(ctx, x + 8, ry, 292, ROW - 2, 0xFF22222A);
            } else if (hover) {
                Ui.rect(ctx, x + 8, ry, 292, ROW - 2, 0xFF191920);
            }
            Ui.text(ctx, m.name, x + 16, ry + 6, m.isEnabled() ? Ui.WHITE : Ui.GRAY);
            String state = m.isEnabled() ? "вкл" : "выкл";
            Ui.text(ctx, state, x + 292 - Ui.width(state), ry + 6, m.isEnabled() ? Ui.OK : Ui.DIM);
        }

        // панель настроек
        Ui.rect(ctx, x + 304, top, 1, bottom - top, 0xFF2A2A32);
        if (selected == null) {
            Ui.text(ctx, "выберите модуль", x + 316, top + 8, Ui.DIM);
        } else {
            Ui.text(ctx, selected.name, x + 316, top + 4, Ui.WHITE);
            int sy = top + 22;
            for (Module.Setting s : selected.settings) {
                int rowH = s.kind == Module.Kind.BOOL ? 16 : 28;
                Ui.text(ctx, s.name, x + 316, sy + 2, Ui.GRAY);
                Ui.text(ctx, s.display(), x + W - 16 - Ui.width(s.display()), sy + 2, Ui.WHITE);
                if (s.kind == Module.Kind.NUM) {
                    int barX = x + 316;
                    int barY = sy + 15;
                    int barW = W - 332;
                    Ui.rect(ctx, barX, barY, barW, 3, 0xFF2A2A32);
                    Ui.rect(ctx, barX, barY, (int) (barW * s.ratio()), 3, Ui.WHITE);
                } else if (s.kind == Module.Kind.BOOL) {
                    int bx = x + W - 34;
                    Ui.rect(ctx, bx, sy + 3, 18, 9, s.bool ? Ui.OK : 0xFF2A2A32);
                    Ui.rect(ctx, s.bool ? bx + 11 : bx + 1, sy + 4, 7, 7, 0xFFFFFFFF);
                }
                sy += rowH + 6;
            }
        }

        Ui.text(ctx, "Esc — закрыть · клик по модулю — вкл/выкл", x + 12, y + H - 16, Ui.DIM);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = px();
        int y = py();

        int tabX = x + 96;
        for (Module.Category c : Module.Category.values()) {
            int tw = Ui.width(c.label) + 16;
            if (mx >= tabX && mx <= tabX + tw && my >= y + 6 && my <= y + 22) {
                category = c;
                selected = null;
                scroll = 0;
                return true;
            }
            tabX += tw + 4;
        }

        List<Module> modules = list();
        int top = listTop(y);
        int bottom = listBottom(y);
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int ry = top + (i - scroll) * ROW;
            if (ry < top || ry + ROW - 2 > bottom) continue;
            if (mx >= x + 8 && mx <= x + 300 && my >= ry && my <= ry + ROW - 3) {
                selected = m;
                if (button == 0) {
                    m.toggle();
                    Config.save();
                }
                return true;
            }
        }

        if (selected != null) {
            int sy = top + 22;
            for (Module.Setting s : selected.settings) {
                int rowH = s.kind == Module.Kind.BOOL ? 16 : 28;
                if (my >= sy && my <= sy + rowH && mx >= x + 312 && mx <= x + W - 8) {
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
                        int barX = x + 316;
                        int barW = W - 332;
                        s.setFromRatio((mx - barX) / (double) barW);
                    }
                    Config.save();
                    return true;
                }
                sy += rowH + 6;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int y = py();
        scroll = Math.max(0, Math.min(maxScroll(y), scroll - (int) Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
