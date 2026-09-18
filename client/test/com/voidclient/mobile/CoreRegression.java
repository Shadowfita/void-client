package com.voidclient.mobile;

import java.util.*;

/** Dependency-free deterministic tests; also called by the Gradle mobileTest task. */
public final class CoreRegression {
    private static int checks;
    private static final class Sink implements GestureRecognizer.Sink {
        GestureRecognizer.Kind kind = GestureRecognizer.Kind.CONTROL;
        boolean valid = true;
        final List<String> events = new ArrayList<>();
        public GestureRecognizer.Target hit(double x, double y) { return new GestureRecognizer.Target(1, kind); }
        public boolean valid(GestureRecognizer.Target target) { return valid; }
        public void tap(GestureRecognizer.Target t, double x, double y) { events.add("tap"); }
        public void context(GestureRecognizer.Target t, double x, double y) { events.add("context"); }
        public void scroll(GestureRecognizer.Target t, double x, double y) { events.add("scroll"); }
        public void camera(double x, double y) { events.add("camera"); }
        public void zoom(double delta) { events.add("zoom"); }
        public void cancel() { events.add("cancel"); }
    }
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    public static int run() {
        checks = 0;
        ViewportState v = new ViewportState(4, 20, 30, 400, 800, 800, 1600, 320, 640);
        check(v.contains(20, 30), "top/left edge included");
        check(!v.contains(420, 830), "bottom/right edge excluded");
        check(!v.contains(Double.NaN, 35), "NaN rejected");
        check(!v.contains(30, Double.POSITIVE_INFINITY), "Infinity rejected");
        check(v.nativeX(220) == 400 && v.logicalY(430) == 320, "independent native/logical transforms");
        boolean rejected = false; try { v.nativeX(19); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "out-of-bounds input is not clamped into a hit");
        for (int x = 0; x < 320; x++) check(v.logicalX(v.hostX(x + .5)) == x, "logical/display round trip " + x);
        check(v.sameGeometry(new ViewportState(99, 20, 30, 400, 800, 800, 1600, 320, 640)), "geometry independent of revision");
        check(v.profile.equals("compact-portrait"), "portrait profile");
        check(new ViewportState(1, 0, 0, 844, 390, 844, 390, 844, 390).profile.equals("compact-landscape"), "landscape profile");
        check(new ViewportState(1, 0, 0, 1024, 768, 1024, 768, 1024, 768).profile.equals("tablet"), "tablet profile");
        Sink s = new Sink(); GestureRecognizer g = new GestureRecognizer(s, 10, 550);
        g.down(1, 10, 10, 0, 1); check(s.events.isEmpty(), "touchdown does not activate");
        g.up(1, 10, 10, 100, 1); check(s.events.equals(Arrays.asList("tap")), "one tap on release");
        s.events.clear(); g.down(1, 10, 10, 0, 1); g.tick(550, 1); g.up(1, 10, 10, 600, 1);
        check(s.events.equals(Arrays.asList("context")), "long press never also taps");
        s.events.clear(); g.down(1, 10, 10, 0, 1); g.up(1, 10, 10, 700, 1);
        check(s.events.equals(Arrays.asList("context")), "delayed tick still uses event duration");
        s.events.clear(); g.down(1, 10, 10, 0, 1); g.move(1, 30, 10, 100, 1); g.up(1, 30, 10, 150, 1);
        check(s.events.isEmpty(), "dragging a control cancels its action");
        s.events.clear(); s.kind = GestureRecognizer.Kind.SCROLL;
        g.down(1, 10, 10, 0, 1); g.move(1, 10, 30, 100, 1); g.move(1, 10, 50, 150, 1); g.up(1, 10, 50, 160, 1);
        check(s.events.contains("scroll") && !s.events.contains("tap") && !s.events.contains("context"), "scroll owns the gesture");
        s.events.clear(); s.kind = GestureRecognizer.Kind.WORLD;
        g.down(1, 10, 10, 0, 1); g.move(1, 25, 10, 20, 1); g.up(1, 25, 10, 30, 1);
        check(s.events.contains("camera") && !s.events.contains("tap"), "camera drag cannot walk");
        s.events.clear(); g.down(1, 10, 10, 0, 1); g.down(2, 50, 10, 5, 1); g.move(2, 80, 10, 10, 1);
        g.up(2, 80, 10, 15, 1); g.up(1, 10, 10, 20, 1);
        check(s.events.contains("zoom") && !s.events.contains("tap") && !s.events.contains("context"), "pinch has no residual tap");
        s.events.clear(); s.kind = GestureRecognizer.Kind.CONTROL;
        g.down(1, 10, 10, 0, 1); g.down(2, 50, 10, 5, 1); g.up(2, 50, 10, 10, 1); g.up(1, 10, 10, 15, 1);
        check(s.events.isEmpty(), "multitouch on controls never activates");
        s.events.clear(); g.down(1, 10, 10, 0, 1); g.tick(100, 2); g.up(1, 10, 10, 120, 2);
        check(s.events.equals(Arrays.asList("cancel")), "rotation/revision change cancels");
        s.events.clear(); g.down(1, 10, 10, 0, 1); s.valid = false; g.tick(100, 1); g.up(1, 10, 10, 120, 1);
        check(s.events.equals(Arrays.asList("cancel")), "removed target cancels"); s.valid = true;
        s.events.clear(); g.down(1, 10, 10, 0, 1); g.cancel(); g.up(1, 10, 10, 100, 1);
        check(s.events.equals(Arrays.asList("cancel")), "cancel followed by release cannot activate");
        s.events.clear(); g.down(1, 10, 10, 0, 1); g.move(1, Double.NaN, 10, 1, 1);
        check(s.events.equals(Arrays.asList("cancel")), "invalid pointer fails closed");
        s.events.clear(); g.down(1, 10, 10, 0, 1); g.down(1, 10, 10, 1, 1);
        check(s.events.equals(Arrays.asList("cancel")), "duplicate pointer id fails closed");
        s.events.clear(); s.kind = GestureRecognizer.Kind.BLOCKED; g.down(1, 10, 10, 0, 1); g.up(1, 10, 10, 1, 1);
        check(s.events.isEmpty() && !g.active(), "blocked targets do not own input");
        // Random cancelled streams must never resurrect an activation after cancellation.
        Random random = new Random(634);
        s.kind = GestureRecognizer.Kind.CONTROL;
        for (int n = 0; n < 1000; n++) {
            s.events.clear(); g.down(1, 20, 20, 0, 1);
            g.move(1, random.nextInt(50), random.nextInt(50), 10, 1);
            g.cancel(); g.up(1, 20, 20, 20, 1); g.tick(1000, 1);
            check(!s.events.contains("tap") && !s.events.contains("context"), "cancel trace " + n);
        }
        System.setProperty("void.mobile", "true"); MobileBridge.setSuspended(false); MobileBridge.drain();
        MobileBridge.pointer("down", 1, 1, 1, 2);
        for (int n = 0; n < 1000; n++) MobileBridge.pointer("move", 1, n, 1, 2);
        MobileBridge.pointer("up", 1, 999, 1, 2);
        List<MobileBridge.Command> commands = MobileBridge.drain();
        check(commands.size() == 3 && commands.get(1).x == 999, "movement coalesces, press/release do not");
        check(commands.get(0).type.equals("down") && commands.get(2).type.equals("up"), "boundary event order");
        for (int n = 0; n < 200; n++) MobileBridge.pointer("down", n, 1, 1, 2);
        commands = MobileBridge.drain();
        check(commands.size() == 1 && commands.get(0).type.equals("cancel"), "queue overflow cancels instead of dropping release");
        MobileBridge.pointer("down", 1, 1, 1, 2); MobileBridge.cancel(); MobileBridge.pointer("up", 1, 1, 1, 2);
        commands = MobileBridge.drain();
        check(commands.get(0).type.equals("cancel"), "explicit cancellation orders before a late up");
        MobileBridge.setSuspended(true); MobileBridge.drain(); MobileBridge.pointer("down", 1, 1, 1, 2);
        check(MobileBridge.drain().isEmpty(), "background input rejected");
        MobileBridge.setSuspended(false); MobileBridge.drain();
        rejected = false; try { MobileBridge.pointer("invented", 1, 1, 1, 2); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "unknown bridge phases rejected");
        rejected = false; try { MobileBridge.insertText(new String(new char[2049]), 2); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "text limit enforced at boundary");
        System.clearProperty("void.mobile"); MobileBridge.pointer("down", 1, 1, 1, 2);
        check(MobileBridge.drain().isEmpty(), "desktop mode ignores mobile API");
        System.out.println("Core regression: " + checks + " assertions passed");
        return checks;
    }
    public static void main(String[] args) { run(); }
}
