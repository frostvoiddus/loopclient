package dev.loopclient;

import dev.loopclient.gui.ClickGui;
import dev.loopclient.gui.KeyScreen;
import dev.loopclient.util.KeyCheck;
import dev.loopclient.module.Modules;
import dev.loopclient.util.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * loopclient — визуальный клиент.
 * Точка входа: меню по RightShift, HUD каждый кадр, ESP/хитбоксы в мире.
 */
public class LoopClient implements ClientModInitializer {
    public static final String MOD_ID = "loopclient";

    public static LoopClient INSTANCE;
    public static MinecraftClient mc;
    private static KeyBinding menuKey;
    private static boolean keyGatePending;
    private static int gateDelay;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        mc = MinecraftClient.getInstance();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.loopclient.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.loopclient"
        ));

        Modules.init();
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            mc = client;
            if (menuKey != null && menuKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new ClickGui());
                dev.loopclient.util.Sfx.open();
            }
            if (gateDelay > 0) gateDelay--;
            if (gateDelay == 0 && client.currentScreen == null && client.player != null
                && (keyGatePending || !KeyCheck.verify(Config.getKey()).ok)
                && !(client.currentScreen instanceof KeyScreen)) {
                keyGatePending = false;
                if (KeyCheck.verify(Config.getKey()).ok == false) {
                    client.setScreen(new KeyScreen(false));
                }
            }
            Modules.tick();
        });

        // при заходе на сервер — спросить ключ, если его нет
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.currentScreen == null) keyGatePending = true;
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Modules.hud(ctx));

        WorldRenderEvents.LAST.register(ctx -> Modules.world(ctx));

        Runtime.getRuntime().addShutdownHook(new Thread(Config::save, "loopclient-config"));
    }
}
