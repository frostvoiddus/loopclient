package dev.loopclient.modules;

import dev.loopclient.module.Module;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Визуальные модули без миксинов: китайская шляпа, подсветка блока, трассеры, круги прыжка. */
public final class VisualModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private VisualModules() {}

    /** Китайская шляпа: стопка сужающихся коробок над головой. */
    public static class ChinaHat extends Module {
        private final Setting height = num("Высота", 0.28, 0.1, 0.8, 0.01);

        public ChinaHat() {
            super("ChinaHat", Category.VISUAL);
        }

        @Override
        public void onWorld(WorldRenderContext ctx) {
            if (mc.player == null || ctx.camera() == null) return;
            MatrixStack matrices = ctx.matrixStack();
            VertexConsumerProvider consumers = ctx.consumers();
            if (matrices == null || consumers == null) return;

            Vec3d cam = ctx.camera().getPos();
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

            for (PlayerEntity player : worldPlayers(ctx)) {
                double top = player.getY() + player.getHeight() + height.value;
                double cx = player.getX();
                double cz = player.getZ();

                // 3 яруса: широкий -> узкий
                for (int i = 0; i < 3; i++) {
                    double r = 0.42 - i * 0.12;
                    double y1 = top + i * 0.12;
                    double y2 = y1 + 0.12;
                    Box box = new Box(cx - r, y1, cz - r, cx + r, y2, cz + r)
                        .offset(-cam.x, -cam.y, -cam.z);
                    VertexRendering.drawBox(matrices, lines, box, 0.55f, 0.35f, 0.95f, 0.85f);
                }
            }
            ((VertexConsumerProvider.Immediate) consumers).draw(RenderLayer.getLines());
        }
    }

    /** Подсветка блока, на который смотришь. */
    public static class BlockHighlight extends Module {
        public BlockHighlight() {
            super("BlockHighlight", Category.VISUAL);
        }

        @Override
        public void onWorld(WorldRenderContext ctx) {
            if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
            if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
            if (ctx.camera() == null) return;

            MatrixStack matrices = ctx.matrixStack();
            VertexConsumerProvider consumers = ctx.consumers();
            if (matrices == null || consumers == null) return;

            BlockPos pos = hit.getBlockPos();
            Vec3d cam = ctx.camera().getPos();
            Box box = new Box(pos).expand(0.002).offset(-cam.x, -cam.y, -cam.z);
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            VertexRendering.drawBox(matrices, lines, box, 1f, 1f, 1f, 0.9f);
            ((VertexConsumerProvider.Immediate) consumers).draw(RenderLayer.getLines());
        }
    }

    /** Трассеры: тонкие лучи от камеры к игрокам. */
    public static class Tracers extends Module {
        private final Setting distance = num("Дистанция", 64, 8, 160, 1);

        public Tracers() {
            super("Tracers", Category.VISUAL);
        }

        @Override
        public void onWorld(WorldRenderContext ctx) {
            if (mc.player == null || ctx.camera() == null) return;
            MatrixStack matrices = ctx.matrixStack();
            VertexConsumerProvider consumers = ctx.consumers();
            if (matrices == null || consumers == null) return;

            Vec3d cam = ctx.camera().getPos();
            double max = distance.value;
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

            for (PlayerEntity player : worldPlayers(ctx)) {
                if (player == mc.player) continue;
                if (mc.player.squaredDistanceTo(player) > max * max) continue;

                double tx = player.getX() - cam.x;
                double ty = player.getY() + player.getHeight() / 2 - cam.y;
                double tz = player.getZ() - cam.z;
                // луч как очень тонкая коробка от камеры (0,0,0) к цели
                Box beam = beamBox(tx, ty, tz, 0.02);
                VertexRendering.drawBox(matrices, lines, beam, 0.55f, 0.35f, 0.95f, 0.75f);
            }
            ((VertexConsumerProvider.Immediate) consumers).draw(RenderLayer.getLines());
        }
    }

    /** Круги при прыжке: расширяющийся след на земле. */
    public static class JumpCircles extends Module {
        private static final List<Ring> RINGS = new ArrayList<>();
        private boolean wasOnGround = true;

        public JumpCircles() {
            super("JumpCircles", Category.VISUAL);
            num("Живут, мс", 900, 300, 2000, 50);
        }

        private static final class Ring {
            final double x;
            final double y;
            final double z;
            final long born;

            Ring(double x, double y, double z, long born) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.born = born;
            }
        }

        @Override
        public void onTick() {
            if (mc.player == null) return;
            boolean onGround = mc.player.isOnGround();
            if (wasOnGround && !onGround && mc.player.getVelocity().y > 0.12) {
                RINGS.add(new Ring(mc.player.getX(), mc.player.getY() + 0.02, mc.player.getZ(), System.currentTimeMillis()));
            }
            wasOnGround = onGround;
        }

        @Override
        public void onWorld(WorldRenderContext ctx) {
            if (ctx.camera() == null) return;
            MatrixStack matrices = ctx.matrixStack();
            VertexConsumerProvider consumers = ctx.consumers();
            if (matrices == null || consumers == null) return;

            long life = (long) setting("Живут, мс").value;
            long now = System.currentTimeMillis();
            RINGS.removeIf(r -> now - r.born > life);
            if (RINGS.isEmpty()) return;

            Vec3d cam = ctx.camera().getPos();
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

            for (Ring ring : RINGS) {
                float t = (now - ring.born) / (float) life;
                double r = 0.25 + t * 0.9;
                float alpha = (1f - t) * 0.8f;
                Box box = new Box(ring.x - r, ring.y, ring.z - r, ring.x + r, ring.y + 0.03, ring.z + r)
                    .offset(-cam.x, -cam.y, -cam.z);
                VertexRendering.drawBox(matrices, lines, box, 0.7f, 0.6f, 1f, alpha);
            }
            ((VertexConsumerProvider.Immediate) consumers).draw(RenderLayer.getLines());
        }
    }

    /* helpers */

    private static List<PlayerEntity> worldPlayers(WorldRenderContext ctx) {
        List<PlayerEntity> list = new ArrayList<>();
        if (ctx.world() == null) return list;
        for (Entity e : ctx.world().getEntities()) {
            if (e instanceof PlayerEntity p) list.add(p);
        }
        return list;
    }

    /** Очень тонкая коробка от камеры к точке (tx,ty,tz). */
    private static Box beamBox(double tx, double ty, double tz, double w) {
        double minX = Math.min(0, tx) - w;
        double minY = Math.min(0, ty) - w;
        double minZ = Math.min(0, tz) - w;
        double maxX = Math.max(0, tx) + w;
        double maxY = Math.max(0, ty) + w;
        double maxZ = Math.max(0, tz) + w;
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
