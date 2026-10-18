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
 * Редактор HUD: список элементов справа, выбранный таскается мышью.
 * Стрелки — точная подстройка, R — сброс позиции, Esc — выход.
 */
public class HudEditor extends Screen {
    private static final int PANEL_W = 210;
    private static final int ROW = 20;

    private Module selected;
    private boolean dragging;
    private double lastX;
    private double lastY;
    private int scroll;

    public HudEditor() {
        super(Text.literal("loopclient hud"));
    }

    private List<Module> hudModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : Modules.ALL) {
            if (m.category == Module.Category.HUD && m.isEnabled()) list.add(m);
        }
        return list;
    }

    private int panelX() {
        return width - PANEL_W - 10;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // сам HUD рисуем как в игре — чтобы видеть результат
        Modules.hud(ctx);

        // затемнение краёв и подсказка
        ctx.fill(0, height - 22, width, height, 0xB0000000);
        String hint = selected == null
            ? "выбери элемент справа · F7 / Esc — закрыть"
            : selected.name + "  x=" + selected.offsetX + " y=" + selected.offsetY + "  · тяни мышью, R — сброс";
        Ui.text(ctx, hint, 8, height - 15, Ui.WHITE);

        int x = panelX();
        Ui.roundRect(ctx, x, 8, PANEL_W, height - 36, 10, 0xE00A0A10);
        Ui.rect(ctx, x, 8, PANEL_W, 24, 0xFF12121A);
        Ui.text(ctx, "HUD элементы", x + 10, 15, Ui.WHITE);

        List<Module> list = hudModules();
        int top = 38;
        for (int i = 0; i < list.size(); i++) {
            int ry = top + (i - scroll) * ROW;
            if (ry < top || ry + ROW > height - 40) continue;
            Module m = list.get(i);
            boolean hover = mouseX >= x + 6 && mouseX <= x + PANEL_W - 6 && mouseY >= ry && mouseY <= ry + ROW - 2;
            if (m == selected) {
                Ui.roundRect(ctx, x + 6, ry, PANEL_W - 12, ROW - 2, 6, 0xFF1B1B26);
                Ui.rect(ctx, x + 6, ry + 5, 2, 10, 0xFF8B5CF6);
            } else if (hover) {
                Ui.roundRect(ctx, x + 6, ry, PANEL_W - 12, ROW - 2, 6, 0xFF14141C);
            }
            Ui.text(ctx, m.name, x + 16, ry + 6, m == selected ? Ui.WHITE : Ui.GRAY);
        }
        if (list.isEmpty()) {
            Ui.text(ctx, "включи HUD-модули", x + 10, top + 4, Ui.DIM);
        }

        // рамка вокруг выбранного элемента
        if (selected != null) {
            int bx = 8 + selected.offsetX;
            int by = 6 + selected.offsetY;
            Ui.rect(ctx, bx - 4, by - 4, 120, 28, 0x338B5CF6);
            Ui.rect(ctx, bx - 4, by - 4, 120, 1, 0xFF8B5CF6);
            Ui.rect(ctx, bx - 4, by + 23, 120, 1, 0xFF8B5CF6);
            Ui.rect(ctx, bx - 4, by - 4, 1, 28, 0xFF8B5CF6);
            Ui.rect(ctx, bx + 115, by - 4, 1, 28, 0xFF8B5CF6);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = panelX();
        List<Module> list = hudModules();
        int top = 38;
        for (int i = 0; i < list.size(); i++) {
            int ry = top + (i - scroll) * ROW;
            if (ry < top || ry + ROW > height - 40) continue;
            if (mx >= x + 6 && mx <= x + PANEL_W - 6 && my >= ry && my <= ry + ROW - 2) {
                selected = list.get(i);
                Sfx.click();
                return true;
            }
        }
        if (selected != null) {
            dragging = true;
            lastX = mx;
            lastY = my;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && selected != null) {
            selected.offsetX += (int) Math.round(mx - lastX);
            selected.offsetY += (int) Math.round(my - lastY);
            lastX = mx;
            lastY = my;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            Config.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (selected != null) {
            selected.offsetY -= (int) Math.signum(vertical) * 2;
            Config.save();
            return true;
        }
        scroll = Math.max(0, scroll - (int) Math.signum(vertical));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected != null) {
            int step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 2;
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                selected.offsetX -= step;
                Config.save();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                selected.offsetX += step;
                Config.save();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                selected.offsetY -= step;
                Config.save();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                selected.offsetY += step;
                Config.save();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_R) {
                selected.offsetX = 0;
                selected.offsetY = 0;
                Config.save();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_F7) {
            if (client != null) client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
