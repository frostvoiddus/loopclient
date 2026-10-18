package dev.loopclient.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Проверка ключа доступа.
 *
 * Формат: LOOP-<код тарифа>-<4>-<4>-<подпись>
 * Подпись: HMAC-SHA256(secret, "<код>-<блок>-<блок>"), первые 6 hex в верхнем регистре
 * (ровно как в loopclient-keygen / keys.js — БЕЗ префикса LOOP).
 *
 * Секрет берётся так: файл %APPDATA%\loopclient\data\secret (его пишет лаунчер),
 * иначе демо-значение. В продакшене проверку надо делать на сервере.
 */
public final class KeyCheck {

    /** Демо-секрет (secret.js: DEMO). */
    public static final String DEMO_SECRET = "loopclient-demo-secret-change-me";

    private static final String[][] PLANS = {
        {"07", "7 дней"}, {"14", "14 дней"}, {"30", "30 дней"},
        {"90", "90 дней"}, {"18", "180 дней"}, {"FF", "навсегда"},
    };

    private static String secret;

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

    /** Секрет из файла лаунчера, иначе демо. */
    public static String secret() {
        if (secret != null) return secret;
        try {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                Path file = Paths.get(appData, "loopclient", "data", "secret");
                if (Files.exists(file)) {
                    String value = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
                    if (!value.isEmpty()) {
                        secret = value;
                        return secret;
                    }
                }
            }
        } catch (Throwable ignored) {
            // нет доступа к файлу — работаем с демо
        }
        secret = DEMO_SECRET;
        return secret;
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
        String expected = sign(code, parts[2], parts[3]);
        if (!expected.equalsIgnoreCase(parts[4])) return fail("Ключ неверный или подделан");
        return new Result(true, null, plan);
    }

    /** HMAC-SHA256(secret, "code-a-b"), первые 6 hex. */
    public static String sign(String code, String a, String b) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = code + "-" + a + "-" + b;
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) hex.append(String.format("%02x", value));
            return hex.substring(0, 6).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean hasValidKey() {
        return verify(Config.getKey()).ok;
    }
}
