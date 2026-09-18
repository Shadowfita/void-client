package com.voidclient.mobile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Public browser/Swing boundary. Producers enqueue commands; only the client thread mutates game state. */
public final class MobileBridge {
    public static final class Command {
        public final String type, text;
        public final int id;
        public final double x, y;
        public final long revision;
        public final long time = System.nanoTime() / 1000000;
        public Command(String type, int id, double x, double y, long revision, String text) {
            this.type = type; this.id = id; this.x = x; this.y = y; this.revision = revision; this.text = text;
        }
    }
    private static final int CAPACITY = 128;
    private static final ArrayDeque<Command> queue = new ArrayDeque<>();
    private static volatile String state = "{\"ready\":false,\"status\":\"Starting client\"}";
    private static volatile boolean suspended;
    private static boolean overflow;
    private static final java.util.concurrent.atomic.AtomicInteger textIds = new java.util.concurrent.atomic.AtomicInteger();
    private static volatile long acknowledgedRevision;
    private static volatile boolean textFocused;
    private MobileBridge() { }

    public static String snapshot() { return state; }
    public static void publish(String json, long revision) { state = json; acknowledgedRevision = revision; }
    public static boolean suspended() { return suspended; }
    public static boolean textFocused() { return textFocused; }
    public static void setTextFocus(boolean value) { textFocused = value; cancel(); }
    public static void setSuspended(boolean value) { suspended = value; cancel(); }
    public static void cancel() { offer(new Command("cancel", 0, 0, 0, 0, "")); }
    public static void pointer(String phase, int id, double x, double y, long revision) {
        if (!("down".equals(phase) || "move".equals(phase) || "up".equals(phase) || "context".equals(phase)))
            throw new IllegalArgumentException("Unknown pointer phase");
        if (!Double.isFinite(x) || !Double.isFinite(y)) { cancel(); return; }
        offer(new Command("context".equals(phase) ? "contextAt" : phase, id, x, y, revision, ""));
    }
    public static void wheel(double x, double y, int delta, long revision) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        offer(new Command("wheel", Math.max(-120, Math.min(120, delta)), x, y, revision, ""));
    }
    public static void action(String action, int id, long revision) {
        if (!("select".equals(action) || "dismiss".equals(action) || "context".equals(action)
                || "cameraLeft".equals(action) || "cameraRight".equals(action)
                || "cameraUp".equals(action) || "cameraDown".equals(action)
                || "zoomIn".equals(action) || "zoomOut".equals(action)
                || "key".equals(action) || "inspect".equals(action)))
            throw new IllegalArgumentException("Unknown mobile action");
        offer(new Command(action, id, 0, 0, revision, ""));
    }
    public static int insertText(String value, long session) {
        if (value == null || value.length() > 2048) throw new IllegalArgumentException("Text limit: 2048 characters");
        int id = textIds.incrementAndGet();
        offer(new Command("text", id, 0, 0, session, value));
        return id;
    }
    private static synchronized void offer(Command c) {
        if (!MobileConfig.enabled()) return;
        if ("cancel".equals(c.type)) { queue.clear(); overflow = false; queue.add(c); return; }
        if (suspended || overflow) return;
        Command last = queue.peekLast();
        if ("move".equals(c.type) && last != null && "move".equals(last.type)
                && c.id == last.id && c.revision == last.revision) queue.removeLast();
        if (queue.size() >= CAPACITY) {
            queue.clear(); queue.add(new Command("cancel", 0, 0, 0, 0, "")); overflow = true; return;
        }
        queue.add(c);
    }
    public static synchronized List<Command> drain() {
        List<Command> result = new ArrayList<>(queue); queue.clear(); overflow = false; return result;
    }
    public static long revision() { return acknowledgedRevision; }
}
