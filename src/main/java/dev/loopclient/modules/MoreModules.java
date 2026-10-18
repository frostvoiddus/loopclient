package dev.loopclient.modules;

import dev.loopclient.module.Module;
import dev.loopclient.util.Ui;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.player.PlayerEntity;

/** Вторая пачка модулей: HUD-информация и мелочи без миксинов. */
public final class MoreModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private MoreModules() {}

    /** WASD + ЛКМ/ПКМ на экране. */
    public static class Keystrokes extends Module {
        public Keystrokes() {
            super("Keystrokes", Category.HUD);
            bool("Показывать мышь", true);
        }

        private void key(DrawContext ctx, int x, int y, String label, boolean down) {
            Ui.rect(ctx, x, y, 18, 18, down ? 0xCCFFFFFF : 0x88000000);
            Ui.text(ctx, label, x + (18 - Ui.width(label)) / 2, y + 5, down ? 0xFF101014 : Ui.WHITE);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.options == null) return;
            int x = 6;
            int y = mc.getWindow().getScaledHeight() - 74;
            key(ctx, x + 20, y, "W", mc.options.forwardKey.isPressed());
            key(ctx, x, y + 20, "A", mc.options.leftKey.isPressed());
            key(ctx, x + 20, y + 20, "S", mc.options.backKey.isPressed());
            key(ctx, x + 40, y + 20, "D", mc.options.rightKey.isPressed());
            if (setting("Показывать мышь").bool) {
                key(ctx, x, y + 42, "L", mc.options.attackKey.isPressed());
                key(ctx, x + 20, y + 42, "R", mc.options.useKey.isPressed());
            }
        }
    }

    /** Скорость в блоках/с. */
    public static class Speed extends Module {
        public Speed() {
            super("Speed", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null) return;
            double v = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z) * 20.0;
            String text = String.format(Locale.ROOT, "%.2f b/s", v);
            Ui.text(ctx, text, 6, 84, Ui.WHITE);
        }
    }

    /** Часы. */
    public static class Clock extends Module {
        public Clock() {
            super("Clock", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            String text = LocalTime.now().format(TIME);
            Ui.text(ctx, text, 6, 96, Ui.GRAY);
        }
    }

    /** Направление и yaw. */
    public static class Direction extends Module {
        public Direction() {
            super("Direction", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null) return;
            String dir = mc.player.getHorizontalFacing().asString();
            int yaw = (int) ((mc.player.getYaw() % 360 + 360) % 360);
            Ui.text(ctx, dir + " " + yaw + "°", 6, 108, Ui.GRAY);
        }
    }

    /** Адрес сервера. */
    public static class ServerInfoHud extends Module {
        public ServerInfoHud() {
            super("ServerInfo", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            String text;
            if (mc.isInSingleplayer()) {
                text = "singleplayer";
            } else {
                ServerInfo info = mc.getCurrentServerEntry();
                text = info != null && info.address != null ? info.address : "—";
            }
            Ui.text(ctx, Ui.trim(text, 190), 6, 120, Ui.DIM);
        }
    }

    /** Ник и пинг. */
    public static class PlayerInfoHud extends Module {
        public PlayerInfoHud() {
            super("PlayerInfo", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            String nick = mc.getSession() != null ? mc.getSession().getUsername() : "player";
            Ui.text(ctx, nick, 6, 132, Ui.WHITE);
        }
    }

    /** Память JVM. */
    public static class Memory extends Module {
        public Memory() {
            super("Memory", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            long max = rt.maxMemory() / 1048576L;
            Ui.text(ctx, "mem " + used + "/" + max + " МБ", 6, 144, Ui.GRAY);
        }
    }

    /** Клики в секунду. */
    public static class Cps extends Module {
        private final Deque<Long> clicks = new ArrayDeque<>();
        private boolean wasDown;

        public Cps() {
            super("CPS", Category.HUD);
        }

        @Override
        public void onTick() {
            if (mc.options == null) return;
            boolean down = mc.options.attackKey.isPressed();
            if (down && !wasDown) clicks.addLast(System.currentTimeMillis());
            wasDown = down;
            long now = System.currentTimeMillis();
            while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) clicks.pollFirst();
        }

        @Override
        public void onHud(DrawContext ctx) {
            Ui.text(ctx, "cps " + clicks.size(), 6, 156, Ui.WHITE);
        }
    }

    /** Здоровье, броня-поглощение и еда. */
    public static class HealthBar extends Module {
        public HealthBar() {
            super("HealthBar", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null) return;
            float hp = mc.player.getHealth();
            float max = mc.player.getMaxHealth();
            float abs = mc.player.getAbsorptionAmount();
            String text = String.format(Locale.ROOT, "hp %.0f/%.0f", hp, max);
            if (abs > 0) text += String.format(Locale.ROOT, " +%.0f", abs);
            text += "  food " + mc.player.getHungerManager().getFoodLevel();
            Ui.text(ctx, text, 6, 168, hp <= max * 0.35f ? Ui.BAD : Ui.WHITE);
        }
    }

    /** Ближайший игрок: ник и HP. */
    public static class TargetHud extends Module {
        private final Setting range = num("Дистанция", 24, 4, 64, 1);

        public TargetHud() {
            super("TargetHUD", Category.HUD);
        }

        @Override
        public void onHud(DrawContext ctx) {
            if (mc.player == null || mc.world == null) return;
            double max = range.value;
            PlayerEntity target = null;
            double best = max * max;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                double d = mc.player.squaredDistanceTo(p);
                if (d < best) {
                    best = d;
                    target = p;
                }
            }
            if (target == null) return;

            int w = 150;
            int x = mc.getWindow().getScaledWidth() / 2 - w / 2;
            int y = 24;
            Ui.rect(ctx, x, y, w, 26, 0xCC000000);
            Ui.text(ctx, Ui.trim(target.getName().getString(), 120), x + 6, y + 5, Ui.WHITE);
            Ui.text(ctx, String.format(Locale.ROOT, "%.0f hp", target.getHealth()), x + 6, y + 15, Ui.GRAY);
            int barW = w - 12;
            float ratio = Math.max(0, Math.min(1, target.getHealth() / Math.max(1, target.getMaxHealth())));
            Ui.rect(ctx, x + 6, y + 24, barW, 2, 0xFF2A2A32);
            Ui.rect(ctx, x + 6, y + 24, (int) (barW * ratio), 2, Ui.OK);
        }
    }

    /** Постоянный FOV. */
    public static class CustomFov extends Module {
        private int previous = 70;

        public CustomFov() {
            super("CustomFov", Category.PLAYER);
            num("FOV", 90, 30, 120, 1);
        }

        @Override
        public void onEnable() {
            previous = mc.options.getFov().getValue();
            mc.options.getFov().setValue((int) setting("FOV").value);
        }

        @Override
        public void onDisable() {
            mc.options.getFov().setValue(previous);
        }
    }
}
