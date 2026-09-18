package com.voidclient.mobile;

import java.util.LinkedHashMap;
import java.util.Map;

/** Deterministic, clock-injected recognizer. All calls occur on the client thread. */
public final class GestureRecognizer {
    public enum Kind { CONTROL, SCROLL, WORLD, BLOCKED }
    public static final class Target {
        public final long token;
        public final Kind kind;
        public Target(long token, Kind kind) { this.token = token; this.kind = kind; }
    }
    public interface Sink {
        Target hit(double x, double y);
        boolean valid(Target target);
        void tap(Target target, double x, double y);
        void context(Target target, double x, double y);
        void scroll(Target target, double dx, double dy);
        void camera(double dx, double dy);
        void zoom(double displayDelta);
        void cancel();
    }
    private enum State { IDLE, PENDING, SCROLL, CAMERA, MULTI, CONSUMED }
    private static final class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
    }
    private final Sink sink;
    private final double slop;
    private final long holdMillis;
    private final Map<Integer, Point> pointers = new LinkedHashMap<>();
    private State state = State.IDLE;
    private Target owner;
    private int primary;
    private long downTime, revision;
    private double startX, startY, lastX, lastY, lastDistance, lastCenterX, lastCenterY;

    public GestureRecognizer(Sink sink, double slop, long holdMillis) {
        if (sink == null || !Double.isFinite(slop) || slop < 1 || holdMillis < 1)
            throw new IllegalArgumentException("Invalid gesture configuration");
        this.sink = sink; this.slop = slop; this.holdMillis = holdMillis;
    }
    public boolean active() { return !pointers.isEmpty(); }
    public void down(int id, double x, double y, long time, long viewportRevision) {
        if (!finite(x, y) || pointers.containsKey(id)) { cancel(); return; }
        if (pointers.isEmpty()) {
            owner = sink.hit(x, y);
            if (owner == null) owner = new Target(-1, Kind.BLOCKED);
            primary = id; startX = lastX = x; startY = lastY = y;
            downTime = time; revision = viewportRevision;
            state = owner.kind == Kind.BLOCKED ? State.CONSUMED : State.PENDING;
        } else if (viewportRevision != revision) { cancel(); return; }
        pointers.put(id, new Point(x, y));
        if (pointers.size() == 2) {
            Target second = sink.hit(x, y);
            if (state != State.CONSUMED && owner.kind == Kind.WORLD && second != null && second.kind == Kind.WORLD) {
                state = State.MULTI; rememberPair();
            } else state = State.CONSUMED;
        } else if (pointers.size() > 2) state = State.CONSUMED;
    }
    public void move(int id, double x, double y, long time, long viewportRevision) {
        Point point = pointers.get(id);
        if (point == null) return;
        if (!check(viewportRevision) || !finite(x, y)) { cancel(); return; }
        point.x = x; point.y = y;
        if (state == State.MULTI && pointers.size() == 2) {
            double[] pair = pair();
            sink.camera(pair[0] - lastCenterX, pair[1] - lastCenterY);
            sink.zoom(pair[2] - lastDistance);
            rememberPair(); return;
        }
        if (id != primary || state == State.CONSUMED) return;
        double dx = x - lastX, dy = y - lastY;
        if (state == State.PENDING && Math.hypot(x - startX, y - startY) > slop) {
            state = owner.kind == Kind.SCROLL ? State.SCROLL : owner.kind == Kind.WORLD ? State.CAMERA : State.CONSUMED;
            // Movement over a control cancels its tap; it never silently becomes a world drag.
        }
        if (state == State.SCROLL) sink.scroll(owner, dx, dy);
        else if (state == State.CAMERA) sink.camera(dx, dy);
        lastX = x; lastY = y;
    }
    public void up(int id, double x, double y, long time, long viewportRevision) {
        if (!pointers.containsKey(id)) return;
        move(id, x, y, time, viewportRevision);
        if (!pointers.containsKey(id)) return;
        if (state == State.PENDING && id == primary && pointers.size() == 1 && check(viewportRevision)) {
            if (time - downTime >= holdMillis) sink.context(owner, x, y);
            else sink.tap(owner, x, y);
        }
        pointers.remove(id);
        if (pointers.isEmpty()) { state = State.IDLE; owner = null; }
        else state = State.CONSUMED; // A finger remaining after pinch can never become a tap.
    }
    public void tick(long time, long viewportRevision) {
        if (active() && !check(viewportRevision)) { cancel(); return; }
        if (state == State.PENDING && time - downTime >= holdMillis) {
            state = State.CONSUMED;
            sink.context(owner, startX, startY);
        }
    }
    public void cancel() {
        pointers.clear(); owner = null; state = State.IDLE;
        sink.cancel();
    }
    private boolean check(long r) { return r == revision && owner != null && (owner.kind == Kind.BLOCKED || sink.valid(owner)); }
    private static boolean finite(double x, double y) { return Double.isFinite(x) && Double.isFinite(y); }
    private double[] pair() {
        Point[] p = pointers.values().toArray(new Point[0]);
        return new double[] {(p[0].x + p[1].x) / 2, (p[0].y + p[1].y) / 2,
            Math.hypot(p[0].x - p[1].x, p[0].y - p[1].y)};
    }
    private void rememberPair() {
        double[] p = pair(); lastCenterX = p[0]; lastCenterY = p[1]; lastDistance = p[2];
    }
}
