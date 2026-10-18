package dev.loopclient.modules;

import dev.loopclient.module.Module;
import dev.loopclient.util.Ui;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Дополнительные модули: предметы и игроки рядом. */
public final class ExtraModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private ExtraModules() {}

    /** Подсветка выпавших предметов. */
    public static class ItemEsp extends Module {
        private final Setting distance = num("Дистанция", 24, 4, 64, 1);

        public ItemEsp() {
            super("ItemESP", Category.VISUAL);
        }

        @Override
        public void onWorld(WorldRenderContext ctx) {
            if (mc.player == null || ctx.world() == null || ctx.camera() == null) return;
            MatrixStack matrices = ctx.matrixStack();
            VertexConsumerProvider consumers = ctx.consumers();
            if (matrices == null || consumers == null) return;

            Vec3d cam = ctx.camera().getPos();
            double max = distance.value;
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

            for (Entity entity : ctx.world().getEntities()) {
                if (!(entity instanceof ItemEntity)) continue;
                if (mc.player.squaredDistanceTo(entity) > max * max) continue;
                Box box = entity.getBoundingBox().offset(-cam.x, -cam.y, -cam.z);
                VertexRendering.drawBox(matrices, lines, box, 0.55f, 0.95f, 0.65f, 0.75f);
            }
            ((VertexConsumerProvider.Immediate) consumers).draw(RenderLayer.getLines());
        }
    }

    /** Список игроков рядом с HP. */
    public static class NearbyPlayers extends Module {
        private final Setting distance = num("Дистанция", 32, 8, 128, 1);

        public NearbyPlayers() {
            super("NearbyPlayers", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null || mc.world == null) return;
            double max = distance.value;
            List<PlayerEntity> found = new ArrayList<>();
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                if (mc.player.squaredDistanceTo(p) <= max * max) found.add(p);
            }
            found.sort((a, b) -> Double.compare(mc.player.squaredDistanceTo(a), mc.player.squaredDistanceTo(b)));

            int x = 6;
            int y = 182;
            Ui.text(ctx, "рядом: " + found.size(), x, y, Ui.WHITE);
            y += 12;
            int shown = 0;
            for (PlayerEntity p : found) {
                if (shown >= 5) break;
                double dist = Math.sqrt(mc.player.squaredDistanceTo(p));
                String line = p.getName().getString() + " " + String.format(Locale.ROOT, "%.0f", p.getHealth())
                    + "hp " + String.format(Locale.ROOT, "%.0f", dist) + "м";
                Ui.text(ctx, Ui.trim(line, 150), x, y, Ui.GRAY);
                y += 11;
                shown++;
            }
        }
    }
}
