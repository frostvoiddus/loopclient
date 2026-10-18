package dev.loopclient.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Проверка ключа доступа: формат + HMAC-SHA256, как в лаунчере.
 * Секрет должен совпадать с тем, которым генерируются ключи.
 * В продакшене проверку надо делать на сервере — клиентский секрет извлекается из jar.
 */
public final class KeyCheck {

    /** Демо-секрет из лаунчера (secret.js). Поменяйте на свой вместе с генератором. */
    public static final String SECRET = "loopclient-demo-secret-change-me";

    /** code -> срок в днях; 18 = 180 дней, FF = навсегда. */
    private static final String[][] PLANS = {
        {"07", "7 дней"}, {"14", "14 дней"}, {"30", "30 дней"},
        {"90", "90 дней"}, {"18", "180 дней"}, {"FF", "навсегда"},
    };

    private KeyCheck() {}

    public static final class Result {
        public final boolean ok;
        public final String error;
        public final String plan;

        private Result(boolean ok, String error, String plan) {
            this.ok = ok;
            this.error = error;
            this.plan = plan;
        }
    }

    private static Result fail(String error) {
        return new Result(false, error, null);
    }

    public static Result verify(String input) {
        String raw = String.valueOf(input == null ? "" : input).trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (raw.isEmpty()) return fail("Введите ключ");
        String[] parts = raw.split("-");
        if (parts.length != 5 || !"LOOP".equals(parts[0])) {
            return fail("Формат: LOOP-30-XXXX-XXXX-XXXXXX");
        }
        String code = parts[1];
        String plan = null;
        for (String[] p : PLANS) {
            if (p[0].equals(code)) plan = p[1];
        }
        if (plan == null) return fail("Неизвестный тариф: " + code);
        if (!parts[2].matches("[A-Z0-9]{4}") || !parts[3].matches("[A-Z0-9]{4}")) {
            return fail("Формат: LOOP-30-XXXX-XXXX-XXXXXX");
        }
        String expected = sign(parts[0], parts[1], parts[2], parts[3]);
        if (!expected.equalsIgnoreCase(parts[4])) return fail("Ключ неверный или подделан");
        return new Result(true, null, plan);
    }

    /** HMAC-SHA256(secret, "LOOP-code-p1-p2"), первые 6 hex, как в keys.js. */
    public static String sign(String... parts) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(String.join("-", parts).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.substring(0, 6).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }
}
