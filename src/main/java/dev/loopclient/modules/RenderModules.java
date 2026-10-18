package dev.loopclient.modules;

import dev.loopclient.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Рендер в мире: ESP и хитбоксы. */
public final class RenderModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private RenderModules() {}

    /** Подсветка сущностей коробкой. */
    public static class Esp extends Module {
        private final Setting distance = num("Дистанция", 48, 8, 128, 1);
        private final Setting playersOnly = bool("Только игроки", true);

        public Esp() {
            super("ESP", Category.VISUAL);
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
                if (entity == mc.player) continue;
                if (playersOnly.bool && !(entity instanceof PlayerEntity)) continue;
                if (mc.player.squaredDistanceTo(entity) > max * max) continue;
                Box box = entity.getBoundingBox().offset(-cam.x, -cam.y, -cam.z);
                VertexRendering.drawBox(matrices, lines, box, 1.0f, 1.0f, 1.0f, 0.8f);
            }
            ((VertexConsumerProvider.Immediate) consumers).draw(RenderLayer.getLines());
        }
    }

    /** Хитбоксы всех сущностей, включая себя. */
    public static class Hitboxes extends Module {
        private final Setting distance = num("Дистанция", 32, 4, 96, 1);

        public Hitboxes() {
            super("Hitboxes", Category.VISUAL);
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
                if (mc.player.squaredDistanceTo(entity) > max * max) continue;
                Box box = entity.getBoundingBox().offset(-cam.x, -cam.y, -cam.z);
                VertexRendering.drawBox(matrices, lines, box, 0.55f, 0.9f, 1.0f, 0.7f);
            }
            ((VertexConsumerProvider.Immediate) consumers).draw(RenderLayer.getLines());
        }
    }
}
