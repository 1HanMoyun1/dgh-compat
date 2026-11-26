package com.hanmoyun.dghcompat;

import java.util.Locale;

/** Literal text for writable pages: independent of the dedicated server's language. */
public final class CaseBookText {
    private CaseBookText() {}

    public static String time(long dayTime) {
        long minutes = (Math.floorMod(dayTime, 24000L) * 60 / 1000 + 360) % 1440;
        return String.format(Locale.ROOT, "[%02d:%02d] ", minutes / 60, minutes % 60);
    }

    public static String part(String name) {
        return switch (name) {
            case "HEAD" -> "头部";
            case "TORSO" -> "躯干";
            case "LEFT_ARM" -> "左臂";
            case "RIGHT_ARM" -> "右臂";
            case "LEFT_LEG" -> "左腿";
            case "RIGHT_LEG" -> "右腿";
            case "BLOOD" -> "血液";
            default -> "全身";
        };
    }
}
