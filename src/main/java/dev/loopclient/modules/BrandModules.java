package dev.loopclient.modules;

import dev.loopclient.module.Module;
import dev.loopclient.util.Ui;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Ватермарка в стиле клиента и улучшенный TargetESP. */
public final class BrandModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private BrandModules() {}

    /** Плашка: ■ | loopclient 1.21.4 | ник | fps | ping — всё отключаемое. */
    public static class Watermark2 extends Module {
        private final Setting nick = bool("Ник из лаунчера", true);
        private final Setting fps = bool("FPS", true);
        private final Setting ping = bool("Ping", true);
        private final Setting version = bool("Версия", true);
        private final Setting rightSide = mode("Сторона", "слева", "слева", "справа");
        private String cachedNick;
        private long nickLoaded = -1;

        public Watermark2() {
            super("Watermark+", Category.HUD);
        }

        /** Ник активного аккаунта из данных лаунчера. */
        private String launcherNick() {
            long now = System.currentTimeMillis();
            if (cachedNick != null && now - nickLoaded < 30000) return cachedNick;
            nickLoaded = now;
            cachedNick = "";
            try {
                String appData = System.getenv("APPDATA");
                if (appData == null) return cachedNick;
                Path dir = Paths.get(appData, "loopclient", "data");
                Path accounts = dir.resolve("accounts.json");
                if (!Files.exists(accounts)) return cachedNick;
                String json = new String(Files.readAllBytes(accounts), StandardCharsets.UTF_8);
                int name = json.indexOf("\"name\"");
                if (name < 0) return cachedNick;
                int first = json.indexOf('"', json.indexOf(':', name));
                int second = json.indexOf('"', first + 1);
                if (first > 0 && second > first) cachedNick = json.substring(first + 1, second);
            } catch (Throwable ignored) {
                // нет файла — просто без ника
            }
            return cachedNick;
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.getWindow() == null) return;

            StringBuilder sb = new StringBuilder("loopclient");
            if (version.bool) sb.append(" 1.21.4");
            if (nick.bool) {
                String n = launcherNick();
                if (n != null && !n.isEmpty()) sb.append(" · ").append(n);
            }
            if (fps.bool) sb.append(" · fps ").append(mc.getCurrentFps());
            if (ping.bool && mc.player != null && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) sb.append(" · ").append(entry.getLatency()).append("ms");
            }

            String text = sb.toString();
            int pad = 9;
            int dot = 8;
            int w = pad + dot + 8 + Ui.width(text) + pad;
            int sw = mc.getWindow().getScaledWidth();
            int x = rightSide.mode.equals("справа") ? sw - w - 6 : 6;
            int y = 6;

            Ui.roundRect(ctx, x, y, w, 20, 6, 0xCC06060A);
            Ui.rect(ctx, x + 1, y + 1, w - 2, 1, 0x33FFFFFF);
            Ui.rect(ctx, x + pad, y + 6, dot, dot, 0xFF8B5CF6);
            Ui.text(ctx, text, x + pad + dot + 8, y + 6, Ui.WHITE);
        }
    }

    /** TargetESP: коробка цели + полоса HP + линия до цели. */
    public static class TargetEsp extends Module {
        private final Setting distance = num("Дистанция", 64, 8, 160, 1);
        private final Setting hpBar = bool("Полоса HP", true);

        public TargetEsp() {
            super("TargetESP", Category.VISUAL);
        }

        @Override
        public void onWorld(WorldRenderContext ctx) {
            if (mc.player == null || ctx.world() == null || ctx.camera() == null) return;
            MatrixStack matrices = ctx.matrixStack();
            VertexConsumerProvider consumers = ctx.consumers();
            if (matrices == null || consumers == null) return;

            PlayerEntity target = null;
            double best = distance.value * distance.value;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                double d = mc.player.squaredDistanceTo(p);
                if (d < best) {
                    best = d;
                    target = p;
                }
            }
            if (target == null) return;

            Vec3d cam = ctx.camera().getPos();
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

            Box box = target.getBoundingBox().offset(-cam.x, -cam.y, -cam.z);
            float hp = Math.max(0f, Math.min(1f, target.getHealth() / Math.max(1f, target.getMaxHealth())));
            float r = 1f - hp;
            float g = hp;
            VertexRendering.drawBox(matrices, lines, box, r, g, 0.35f, 0.85f);
            // внешняя рамка чуть больше — эффект «двойной обводки»
            VertexRendering.drawBox(matrices, lines, box.expand(0.06), 1f, 1f, 1f, 0.25f);

            if (hpBar.bool) {
                double top = box.maxY;
                double w = box.maxX - box.minX;
                double left = box.minX;
                VertexRendering.drawBox(matrices, lines,
                    new Box(left, top + 0.12, box.minZ, left + w * hp, top + 0.18, box.minZ + 0.01),
                    r, g, 0.35f, 0.9f);
            }

            // линия от камеры к цели
            double tx = box.getCenter().x;
            double ty = box.getCenter().y;
            double tz = box.getCenter().z;
            VertexRendering.drawBox(matrices, lines,
                new Box(Math.min(0, tx) - 0.015, Math.min(0, ty) - 0.015, Math.min(0, tz) - 0.015,
                    Math.max(0, tx) + 0.015, Math.max(0, ty) + 0.015, Math.max(0, tz) + 0.015),
                0.55f, 0.35f, 0.95f, 0.55f);

            ((VertexConsumerProvider.Immediate) consumers).draw(RenderLayer.getLines());
        }
    }
}
