import com.google.gson.Gson;
import com.voidclient.mobile.*;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.*;

/** Client-thread adapter. This is the only mobile layer permitted to access native game state. */
final class MobileRuntime implements GestureRecognizer.Sink {
    private static final MobileRuntime INSTANCE = new MobileRuntime();
    private final GestureRecognizer gestures = new GestureRecognizer(this, MobileConfig.slopPixels(), MobileConfig.holdMillis());
    private final Gson gson = new Gson();
    private List<Widget> visible = new ArrayList<>(), building = new ArrayList<>();
    private final Map<Long, Hit> owners = new HashMap<>();
    private long token, revision = 1, textSession = 1, menuSerial, menuDeadline;
    private int root = -2, gameState = -1, ticks;
    private ViewportState viewport;
    private Class373_Sub1 input;
    private Hit lastTarget;
    private double pointerX, pointerY, zoomRemainder;
    private String status = "Mobile mode enabled", textPending = "";
    private final ArrayDeque<Integer> keysPending = new ArrayDeque<>();
    private volatile List<Entry> menu = Collections.emptyList();
    private int menuX, menuY, selected = -1;
    private volatile boolean inspecting;
    private int textAck;
    private boolean textAccepted, contextNextTap;

    private static final class Widget {
        final Class46 nativeWidget;
        final int left, top, right, bottom, item;
        final boolean scroll, interactive, scene, blocking;
        Widget(Class46 w, int l, int t, int r, int b) {
            nativeWidget = w; left = l; top = t; right = r; bottom = b; item = w.anInt812;
            scroll = w.anInt774 == 0 && (w.anInt791 > w.anInt789 || w.anInt698 > w.anInt709);
            scene = w.anInt765 == Class239_Sub10.anInt5943 || w.anInt765 == Class312.anInt3932;
            blocking = w.aBoolean776;
            interactive = scene || blocking || scroll || w.aBoolean682 || w.anInt774 == 2
                || (w.aStringArray833 != null) || client.method105(w).anInt7098 != 0;
        }
        boolean contains(int x, int y) { return x >= left && y >= top && x < right && y < bottom; }
    }
    private static final class Hit {
        final long token;
        final Widget target, scroller;
        final GestureRecognizer.Kind kind;
        Hit(long token, Widget target, Widget scroller, GestureRecognizer.Kind kind) {
            this.token = token; this.target = target; this.scroller = scroller; this.kind = kind;
        }
    }
    private static final class Entry {
        final int index, arg1, arg2, opcode, extra1, extra2;
        final long identifier;
        final String label;
        final java.util.List<Object> signature;
        Entry(int index, Class348_Sub42_Sub12 nativeEntry) {
            this.index = index; arg1 = nativeEntry.anInt9602; arg2 = nativeEntry.anInt9607;
            opcode = nativeEntry.anInt9608; identifier = nativeEntry.aLong9605;
            extra1 = nativeEntry.anInt9599; extra2 = nativeEntry.anInt9609;
            label = plain(Class316.method2367((byte) -126, nativeEntry));
            signature = signature(nativeEntry);
        }
        static java.util.List<Object> signature(Class348_Sub42_Sub12 e) {
            return Arrays.asList(e.anInt9608, e.anInt9602, e.anInt9607, e.aLong9605, e.aLong9600,
                e.anInt9599, e.anInt9609, e.aString9595, e.aString9593, e.aString9601,
                e.aBoolean9597, e.aBoolean9610, e.aBoolean9611);
        }
    }
    private MobileRuntime() { }
    static void record(Class46 w, int l, int t, int r, int b) {
        if (MobileConfig.enabled() && l < r && t < b && INSTANCE.building.size() < 10000)
            INSTANCE.building.add(new Widget(w, l, t, r, b));
    }
    static boolean blocksMouse() {
        return MobileConfig.enabled() && (MobileConfig.browser() || MobileBridge.suspended()
            || !INSTANCE.menu.isEmpty() || INSTANCE.inspecting);
    }
    static void tick(Class373_Sub1 mouse, Component canvas) {
        if (!MobileConfig.enabled()) return;
        INSTANCE.input = mouse;
        try { INSTANCE.update(canvas); }
        catch (RuntimeException ex) {
            INSTANCE.cancelAll(); INSTANCE.status = "Mobile input stopped: " + ex.getClass().getSimpleName();
            INSTANCE.publish();
            throw ex; // Do not silently continue with half-applied input state.
        }
    }
    private void update(Component canvas) {
        if (canvas == null || canvas.getWidth() < 1 || canvas.getHeight() < 1
                || Class321.anInt4017 < 1 || Class348_Sub42_Sub8_Sub2.anInt10432 < 1) return;
        Point p = MobileLauncher.canvasOrigin(canvas);
        double scale = MobileLauncher.displayScale();
        p.x = (int) Math.round(p.x * scale); p.y = (int) Math.round(p.y * scale);
        int displayWidth = Math.max(1, (int) Math.round(canvas.getWidth() * scale));
        int displayHeight = Math.max(1, (int) Math.round(canvas.getHeight() * scale));
        ViewportState next = new ViewportState(revision, p.x, p.y, displayWidth, displayHeight,
            Class321.anInt4017, Class348_Sub42_Sub8_Sub2.anInt10432,
            Math.max(1, Applet_Sub1.getConfiguredInterfaceLayoutWidth()),
            Math.max(1, Applet_Sub1.getConfiguredInterfaceLayoutHeight()));
        boolean changed = !next.sameGeometry(viewport) || root != r.anInt9721 || gameState != Class240.anInt4674;
        if (changed) {
            if (root != r.anInt9721 || gameState != Class240.anInt4674) textSession++;
            cancelAll(); revision++; root = r.anInt9721; gameState = Class240.anInt4674;
            next = new ViewportState(revision, p.x, p.y, displayWidth, displayHeight,
                next.nativeWidth, next.nativeHeight, next.logicalWidth, next.logicalHeight);
            building.clear(); visible.clear(); lastTarget = null;
        }
        viewport = next;
        List<Widget> old = visible; visible = building; building = old; building.clear();
        long now = System.nanoTime() / 1000000;
        if (MobileBridge.suspended()) cancelAll();
        for (MobileBridge.Command c : MobileBridge.drain()) {
            if ("cancel".equals(c.type)) { cancelAll(); continue; }
            if (MobileBridge.suspended()) continue;
            if ("select".equals(c.type)) {
                if (c.revision == menuSerial && c.id >= 0 && c.id < menu.size()) selected = c.id;
                continue;
            }
            if ("text".equals(c.type) || "key".equals(c.type)) {
                if ("text".equals(c.type)) { textAck = c.id; textAccepted = false; }
                if (c.revision != textSession || !menu.isEmpty() || inspecting) { status = "Text focus changed; reopen the editor"; continue; }
                if ("text".equals(c.type)) {
                    if (validText(c.text) && textPending.length() + c.text.length() <= 2048) {
                        textPending += c.text; textAccepted = true; status = "Text queued; check the focused client field";
                    } else status = "Text rejected: unsupported character or insertion queue is full";
                } else if (c.id == KeyEvent.VK_ENTER || c.id == KeyEvent.VK_BACK_SPACE || c.id == KeyEvent.VK_TAB || c.id == KeyEvent.VK_ESCAPE) {
                    if (keysPending.size() < 16) keysPending.add(c.id);
                }
                continue;
            }
            if (c.revision != revision) { cancelAll(); status = "View changed; try again"; continue; }
            if ("dismiss".equals(c.type)) { cancelAll(); continue; }
            if (!menu.isEmpty()) continue;
            switch (c.type) {
                case "down":
                    if (!viewport.contains(c.x, c.y) || inspecting) break;
                    textPending = ""; keysPending.clear();
                    gestures.down(c.id, c.x, c.y, c.time, revision); break;
                case "move":
                    if (!viewport.contains(c.x, c.y)) gestures.cancel();
                    else gestures.move(c.id, c.x, c.y, c.time, revision); break;
                case "up":
                    if (!viewport.contains(c.x, c.y)) gestures.cancel();
                    else gestures.up(c.id, c.x, c.y, c.time, revision); break;
                case "wheel":
                    GestureRecognizer.Target wheelTarget = hit(c.x, c.y);
                    if (wheelTarget != null && wheelTarget.kind == GestureRecognizer.Kind.SCROLL) scroll(wheelTarget, 0, -c.id);
                    else if (wheelTarget != null && wheelTarget.kind == GestureRecognizer.Kind.WORLD) zoom(-c.id);
                    break;
                case "contextAt":
                    GestureRecognizer.Target pointed = hit(c.x, c.y);
                    if (pointed != null) context(pointed, c.x, c.y);
                    break;
                case "context": armContext(); break;
                case "inspect": gestures.cancel(); inspecting = !inspecting; break;
                case "cameraLeft": camera(-32, 0); break;
                case "cameraRight": camera(32, 0); break;
                case "cameraUp": camera(0, -32); break;
                case "cameraDown": camera(0, 32); break;
                case "zoomIn": zoom(32); break;
                case "zoomOut": zoom(-32); break;
                default: break;
            }
        }
        gestures.tick(now, revision);
        if (!menu.isEmpty() && now > menuDeadline) { closeMenu(); status = "Actions expired; open them again"; }
        if (!gestures.active()) owners.entrySet().removeIf(e -> lastTarget == null || e.getKey() != lastTarget.token);
        if (Class182.aClass346_2449 instanceof Class346_Sub1 && menu.isEmpty() && !inspecting) {
            Class346_Sub1 keyboard = (Class346_Sub1) Class182.aClass346_2449;
            if (!textPending.isEmpty()) {
                int count = Math.min(24, textPending.length());
                keyboard.mobileText(textPending.substring(0, count)); textPending = textPending.substring(count);
            } else if (!keysPending.isEmpty()) {
                int key = keysPending.remove(); keyboard.mobileKey(key);
                if (key == KeyEvent.VK_TAB || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) { textSession++; keysPending.clear(); }
            }
        }
        if (++ticks % 3 == 0 || changed) publish();
    }
    public GestureRecognizer.Target hit(double x, double y) {
        if (viewport == null || !viewport.contains(x, y) || !menu.isEmpty() || inspecting) return null;
        int lx = viewport.logicalX(x), ly = viewport.logicalY(y);
        Widget target = null, scroller = null;
        int barrier = -1;
        for (int i = 0; i < visible.size(); i++) {
            Widget w = visible.get(i);
            if (!w.contains(lx, ly) || w.nativeWidget.aBoolean813) continue;
            if (w.blocking) { barrier = i; scroller = null; }
            if (w.scroll && i >= barrier) scroller = w;
            if (w.interactive) target = w;
        }
        GestureRecognizer.Kind kind = target != null && target.scene ? GestureRecognizer.Kind.WORLD
            : scroller != null ? GestureRecognizer.Kind.SCROLL : GestureRecognizer.Kind.CONTROL;
        if (target == null && gameState == 3) return null; // Fail closed until the game UI has been traversed.
        Hit h = new Hit(++token, target, scroller, kind); owners.put(h.token, h);
        return new GestureRecognizer.Target(h.token, kind);
    }
    public boolean valid(GestureRecognizer.Target target) {
        Hit h = owners.get(target.token);
        if (h == null) return false;
        Widget captured = h.kind == GestureRecognizer.Kind.SCROLL ? h.scroller : h.target;
        return stillVisible(captured);
    }
    private boolean stillVisible(Widget captured) {
        if (captured == null) return gameState != 3;
        for (Widget w : visible) {
            if (w.nativeWidget == captured.nativeWidget && w.item == captured.item
                    && w.nativeWidget.anInt812 == captured.item
                    && w.left == captured.left && w.top == captured.top && w.right == captured.right
                    && w.bottom == captured.bottom && !w.nativeWidget.aBoolean813) return true;
        }
        return false;
    }
    private void armContext() {
        gestures.cancel(); contextNextTap = !contextNextTap;
        status = contextNextTap ? "Tap a target to choose its actions; Actions again cancels" : "Normal tapping restored";
    }
    public void tap(GestureRecognizer.Target t, double x, double y) {
        if (!valid(t) || !viewport.contains(x, y)) return;
        Hit h = owners.get(t.token);
        if (h == null || !stillVisible(h.target)) return;
        lastTarget = h; pointerX = x; pointerY = y; textSession++;
        if (contextNextTap) { contextNextTap = false; context(t, x, y); return; }
        input.mobileClick(viewport.nativeX(x), viewport.nativeY(y), false);
    }
    public void context(GestureRecognizer.Target t, double x, double y) {
        if (!valid(t) || !viewport.contains(x, y)) return;
        lastTarget = owners.get(t.token); pointerX = x; pointerY = y;
        input.mobileClick(viewport.nativeX(x), viewport.nativeY(y), true);
    }
    public void scroll(GestureRecognizer.Target target, double dx, double dy) {
        Hit h = owners.get(target.token);
        if (h == null || h.scroller == null) return;
        Class46 w = h.scroller.nativeWidget;
        w.anInt747 = clamp(w.anInt747 - (int) Math.round(dx * viewport.logicalWidth / viewport.width), 0, Math.max(0, w.anInt698 - w.anInt709));
        w.anInt755 = clamp(w.anInt755 - (int) Math.round(dy * viewport.logicalHeight / viewport.height), 0, Math.max(0, w.anInt791 - w.anInt789));
        Class251.method1916(-9343, w);
    }
    public void camera(double dx, double dy) {
        if (gameState != 3 || Class348_Sub40_Sub21.anInt9282 != 1 || !menu.isEmpty()) return;
        Class314.aFloat3938 = (float) ((Class314.aFloat3938 - dx * 16 + 16384 * 100) % 16384);
        Class76.aFloat1287 = Math.max(1024, Math.min(3072, Class76.aFloat1287 + (float) dy * 10));
    }
    public void zoom(double delta) {
        if (gameState != 3 || Class348_Sub40_Sub21.anInt9282 != 1 || !menu.isEmpty()) return;
        zoomRemainder += delta;
        int steps = (int) (zoomRemainder / 12);
        if (steps != 0) { Class320.zoomStep = clamp(Class320.zoomStep + steps * Loader.ZOOM_OFFSET_STEP, -2000, 2000); zoomRemainder -= steps * 12; }
    }
    public void cancel() {
        // Release without invoking the legacy release-to-activate or drop path.
        Class289.aClass46_3701 = null; Class331.aClass46_4130 = null;
        Class318_Sub1_Sub3_Sub4.aClass46_10336 = null; Class300.aBoolean3819 = false;
        if (input != null) input.mobileCancel();
        if (Class182.aClass346_2449 instanceof Class346_Sub1) ((Class346_Sub1) Class182.aClass346_2449).mobileCancel();
        textPending = ""; keysPending.clear();
    }
    private void cancelAll() { contextNextTap = false; gestures.cancel(); closeMenu(); inspecting = false; owners.clear(); lastTarget = null; }
    private void closeMenu() { menu = Collections.emptyList(); selected = -1; }
    static boolean openMenu(int x, int y) {
        if (!MobileConfig.enabled()) return false;
        MobileRuntime rt = INSTANCE;
        List<Class348_Sub42_Sub12> nativeEntries = entries();
        List<Entry> result = new ArrayList<>();
        Collections.reverse(nativeEntries);
        for (Class348_Sub42_Sub12 entry : nativeEntries) {
            if (result.size() >= 500) break;
            result.add(new Entry(result.size(), entry));
        }
        rt.gestures.cancel(); rt.contextNextTap = false;
        rt.menu = result; rt.menuX = x; rt.menuY = y; rt.selected = -1; rt.menuSerial++;
        rt.menuDeadline = System.nanoTime() / 1000000 + 15000;
        rt.textPending = ""; rt.keysPending.clear();
        rt.publish(); return true;
    }
    /** Called after the native client rebuilds/sorts candidates, not on the browser/EDT thread. */
    static boolean afterMenuBuild() {
        if (!MobileConfig.enabled()) return false;
        MobileRuntime rt = INSTANCE;
        if (rt.menu.isEmpty()) return rt.inspecting || MobileBridge.suspended();
        if (rt.selected >= 0) {
            Entry chosen = rt.menu.get(rt.selected);
            Class348_Sub42_Sub12 matched = null;
            for (Class348_Sub42_Sub12 current : entries()) if (chosen.signature.equals(Entry.signature(current))) { matched = current; break; }
            rt.closeMenu(); rt.textSession++;
            if (matched != null && rt.root == r.anInt9721 && rt.gameState == Class240.anInt4674)
                Class325.method2599((byte) 109, matched, rt.menuY, rt.menuX);
            else rt.status = "That action changed; open the menu again";
            rt.publish();
        }
        return true; // Consume this cycle even after selection/dismissal. No world click-through.
    }
    private static List<Class348_Sub42_Sub12> entries() {
        List<Class348_Sub42_Sub12> result = new ArrayList<>();
        if (Class348_Sub40_Sub4.aClass262_9111 == null) return result;
        Class312 iterator = new Class312(Class348_Sub40_Sub4.aClass262_9111);
        for (Node n = iterator.method2327((byte) -53); n != null && result.size() < 500; n = iterator.method2329(10))
            if (n instanceof Class348_Sub42_Sub12) result.add((Class348_Sub42_Sub12) n);
        return result;
    }
    private void publish() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", viewport != null); result.put("status", status); result.put("revision", revision);
        result.put("viewport", viewport); result.put("menuId", menuSerial);
        result.put("hostSize", MobileLauncher.hostSize()); result.put("textSession", textSession);
        result.put("textAck", textAck); result.put("textAccepted", textAccepted);
        result.put("displayScale", MobileLauncher.displayScale());
        result.put("contextNextTap", contextNextTap);
        List<Map<String, Object>> choices = new ArrayList<>();
        for (Entry e : menu) { Map<String, Object> row = new LinkedHashMap<>(); row.put("id", e.index); row.put("label", e.label); choices.add(row); }
        result.put("menu", choices); result.put("inspecting", inspecting); result.put("root", root);
        if (inspecting) {
            List<Map<String, Object>> widgets = new ArrayList<>();
            for (Widget w : visible) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", w.nativeWidget.anInt830); row.put("child", w.nativeWidget.anInt704);
                row.put("type", w.nativeWidget.anInt774); row.put("contentType", w.nativeWidget.anInt765);
                row.put("bounds", new int[] {w.left, w.top, w.right - w.left, w.bottom - w.top}); row.put("scroll", w.scroll);
                widgets.add(row);
            }
            result.put("widgets", widgets); // No chat, credentials or editable field text is exported.
        }
        MobileBridge.publish(gson.toJson(result), revision);
    }
    private static boolean validText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c) || !Class122.method1089(-125, c)) return false;
        }
        return true;
    }
    private static String plain(String value) { return value == null ? "Action" : value.replaceAll("<[^>]*>", "").trim(); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
