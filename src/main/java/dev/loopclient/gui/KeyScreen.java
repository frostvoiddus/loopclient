package dev.loopclient.gui;

import dev.loopclient.util.Config;
import dev.loopclient.util.KeyCheck;
import dev.loopclient.util.Ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Экран доступа: открывается при заходе на сервер, если ключ ещё не введён.
 * Пока ключ не принят, окно не закрывается (кроме Esc — тогда выход на сервер).
 */
public class KeyScreen extends Screen {
    private TextFieldWidget input;
    private String status = "";
    private int statusColor = Ui.GRAY;
    private final boolean canSkip;

    public KeyScreen(boolean canSkip) {
        super(Text.literal("loopclient — доступ"));
        this.canSkip = canSkip;
    }

    @Override
    protected void init() {
        int w = 240;
        int x = (width - w) / 2;
        int y = height / 2;

        input = new TextFieldWidget(textRenderer, x, y - 12, w, 20, Text.literal("ключ"));
        input.setMaxLength(40);
        input.setPlaceholder(Text.literal("LOOP-30-XXXX-XXXX-XXXXXX"));
        if (Config.getKey() != null) input.setText(Config.getKey());
        addDrawableChild(input);

        addDrawableChild(ButtonWidget.builder(Text.literal("Активировать"), b -> submit())
            .dimensions(x, y + 16, w, 20).build());

        setInitialFocus(input);
    }

    private void submit() {
        String text = input == null ? "" : input.getText();
        KeyCheck.Result result = KeyCheck.verify(text);
        if (!result.ok) {
            status = result.error;
            statusColor = Ui.BAD;
            return;
        }
        Config.setKey(text.trim().toUpperCase());
        status = "Ключ принят: " + result.plan;
        statusColor = Ui.OK;
        if (client != null) client.setScreen(null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        int w = 300;
        int h = 170;
        int x = (width - w) / 2;
        int y = height / 2 - 70;

        Ui.rect(ctx, x, y, w, h, 0xF00E0E12);
        Ui.rect(ctx, x, y, w, 1, 0xFF2A2A32);

        ctx.drawText(textRenderer, "loopclient", x + 14, y + 12, Ui.WHITE, true);
        ctx.drawText(textRenderer, "доступ по ключу", x + 14, y + 26, Ui.DIM, true);

        ctx.drawText(textRenderer, "Введите ключ, чтобы играть на сервере:", x + (w - 240) / 2, y + 44, Ui.GRAY, true);

        super.render(ctx, mouseX, mouseY, delta);

        if (!status.isEmpty()) {
            ctx.drawText(textRenderer, status, x + (w - textRenderer.getWidth(status)) / 2, y + h - 34, statusColor, true);
        }
        String hint = canSkip ? "Esc — выйти" : "Esc — выйти с сервера";
        ctx.drawText(textRenderer, hint, x + (w - textRenderer.getWidth(hint)) / 2, y + h - 18, Ui.DIM, true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (client != null) client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
