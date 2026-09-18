package com.voidclient.mobile;

/** Explicit session-only opt-in. No desktop preferences are modified. */
public final class MobileConfig {
    private MobileConfig() { }
    public static boolean enabled() { return Boolean.getBoolean("void.mobile"); }
    public static boolean browser() { return Boolean.getBoolean("void.mobile.browser"); }
    public static int holdMillis() { return integer("void.mobile.holdMillis", 550, 250, 1500); }
    public static int slopPixels() { return integer("void.mobile.slop", 10, 4, 40); }
    public static int integer(String key, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(System.getProperty(key, "")))); }
        catch (NumberFormatException ex) { return fallback; }
    }
}
