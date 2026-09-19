package com.voidclient.mobile;

import java.awt.Insets;
import java.awt.Rectangle;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Standalone mobile preferences only. Never stores game credentials or desktop window state. */
public final class StandaloneSettings {
    public boolean fit = true;
    public boolean touch = true;
    public int width = 960, height = 540, gameScale = 125, controlScale = 0;
    public String address;
    public int port;
    public StandaloneSettings(String address, int port) { this.address = address; this.port = port; }

    public StandaloneSettings copy() {
        StandaloneSettings s = new StandaloneSettings(address, port);
        s.fit = fit; s.touch = touch; s.width = width; s.height = height;
        s.gameScale = gameScale; s.controlScale = controlScale; return s;
    }
    public void validate() {
        address = address == null ? "" : address.trim();
        if (address.isEmpty() || address.length() > 253 || address.matches(".*[\\s/\\\\?#@].*")
                || address.contains("://") || (address.indexOf(':') >= 0 && address.indexOf(':') == address.lastIndexOf(':'))) throw new IllegalArgumentException("Enter a hostname or IP address, without a URL or path.");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("TCP port must be 1–65535.");
        if (width < 240 || height < 240 || width > 4096 || height > 4096
                || (long) width * height > 8388608L) throw new IllegalArgumentException("Window dimensions must be 240–4096, up to 8 megapixels.");
        if (gameScale < 100 || gameScale > 200) throw new IllegalArgumentException("Game size must be 100–200%.");
        if (controlScale != 0 && (controlScale < 100 || controlScale > 250))
            throw new IllegalArgumentException("Tool size must be Auto or 100–250%.");
    }
    public static Rectangle usable(Rectangle screen, Insets insets) {
        if (screen == null || screen.width < 1 || screen.height < 1) return new Rectangle(0, 0, 960, 540);
        Rectangle r = new Rectangle(screen);
        if (insets != null) {
            int l = Math.max(0, insets.left), t = Math.max(0, insets.top);
            long w = (long) r.width - l - Math.max(0, insets.right);
            long h = (long) r.height - t - Math.max(0, insets.bottom);
            if (w > 0 && h > 0) r = new Rectangle(r.x + l, r.y + t, (int) w, (int) h);
        }
        return r;
    }
    public Rectangle windowBounds(Rectangle usable) {
        Rectangle area = usable(usable, null);
        int w = fit ? area.width : Math.min(width, area.width);
        int h = fit ? area.height : Math.min(height, area.height);
        return new Rectangle(area.x + (area.width - w) / 2, area.y + (area.height - h) / 2, w, h);
    }
    public int effectiveControlScale(Rectangle screen) {
        if (controlScale != 0) return controlScale;
        return Math.max(100, Math.min(200, (int) Math.round(Math.min(screen.width, screen.height) / 540.0 * 4) * 25));
    }
    public static int dockColumns(int availableWidth, int scale) {
        return Math.max(1, Math.min(5, availableWidth / Math.max(1, (int) Math.ceil(100.0 * scale / 100))));
    }
    /** Keep Display reachable when a large tool setting is used in a smaller manual window. */
    public static int fittingDockScale(int requested, int width, int height) {
        int scale = requested;
        int budget = Math.max(1, height - Math.min(160, height / 2));
        while (scale > 100) {
            int columns = dockColumns(Math.max(1, width - 8), scale);
            int rows = (5 + columns - 1) / columns;
            if (rows * (48 * scale / 100) + (rows - 1) * 4 + 8 <= budget) break;
            scale -= 25;
        }
        return Math.max(100, scale);
    }
    public static Path defaultPath() { return Paths.get(System.getProperty("user.home", "."), ".void-client", "mobile-host.properties"); }
    public static StandaloneSettings load(Path path, String address, int port) throws IOException {
        StandaloneSettings s = new StandaloneSettings(address, port);
        if (!Files.exists(path)) return s;
        Properties p = new Properties(); try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        s.address = p.getProperty("address", address); s.port = integer(p, "port", port);
        s.fit = Boolean.parseBoolean(p.getProperty("fit", "true")); s.touch = Boolean.parseBoolean(p.getProperty("touch", "true"));
        s.width = integer(p, "width", 960); s.height = integer(p, "height", 540);
        s.gameScale = integer(p, "gameScale", 125); s.controlScale = integer(p, "controlScale", 0);
        s.validate(); return s;
    }
    private static int integer(Properties p, String key, int fallback) {
        return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)));
    }
    public void save(Path path) throws IOException {
        validate(); Path absolute = path.toAbsolutePath(); Files.createDirectories(absolute.getParent());
        Properties p = new Properties();
        p.setProperty("address", address); p.setProperty("port", Integer.toString(port));
        p.setProperty("fit", Boolean.toString(fit)); p.setProperty("touch", Boolean.toString(touch));
        p.setProperty("width", Integer.toString(width)); p.setProperty("height", Integer.toString(height));
        p.setProperty("gameScale", Integer.toString(gameScale)); p.setProperty("controlScale", Integer.toString(controlScale));
        Path temp = Files.createTempFile(absolute.getParent(), "mobile-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Void standalone mobile host; no login credentials"); }
            try { Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
