import com.voidclient.mobile.*;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Tests the actual native hooks against the compiled client, without a cache or server. */
public final class MobileNativeRegression {
    private static int checks;
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static Class46 root(int width, int height) {
        Class46 w = new Class46(); w.anInt834 = -1; w.anInt774 = 0; w.anInt765 = 0;
        w.anInt842 = width; w.anInt728 = height; w.anInt830 = 65536; return w;
    }
    public static void main(String[] args) throws Exception {
        int core = CoreRegression.run();
        System.setProperty("void.mobile", "true");
        Class46 w = root(765, 503);
        Class239_Sub28.method1843(390, -326, w, false, 600);
        Class14_Sub1.method239((byte) 115, 390, 600, w);
        check(w.anInt709 == 390 && w.anInt789 == 503, "native layout constrained horizontally");
        check(w.anInt698 == 765 && w.anInt791 == 0, "authored content extent retained");
        check(w.anInt842 == 765 && w.anInt728 == 503, "authored dimensions not overwritten");
        Class239_Sub28.method1843(390, -326, w, false, 600);
        check(w.anInt709 == 390 && w.anInt698 == 765, "idempotent reflow");
        Class239_Sub28.method1843(1000, -326, w, false, 800);
        check(w.anInt709 == 765 && w.anInt698 == 0, "large viewport restores authored zero extent");
        w.anInt698 = 900; Class239_Sub28.method1843(390, -326, w, false, 600);
        check(w.anInt698 == 900, "script content extent takes precedence");
        Class239_Sub28.method1843(1000, -326, w, false, 800);
        check(w.anInt698 == 900, "script extent preserved after returning to large viewport");
        w = root(765, 503); w.anInt788 = -30; w.anInt739 = -20;
        Class239_Sub28.method1843(320, -326, w, false, 240); Class14_Sub1.method239((byte) 115, 320, 240, w);
        check(w.anInt800 == 0 && w.anInt750 == 0 && w.anInt709 == 320 && w.anInt789 == 240, "fallback panel kept inside viewport");
        check(w.anInt698 == 765 && w.anInt791 == 503, "two-axis scroll extent");
        w = root(765, 503); w.anInt765 = 1337;
        Class239_Sub28.method1843(390, -326, w, false, 600);
        check(w.anInt709 == 765, "scene widgets not arbitrarily clamped");
        System.clearProperty("void.mobile");
        w = root(765, 503); Class239_Sub28.method1843(390, -326, w, false, 600);
        check(w.anInt709 == 765 && w.anInt698 == 0, "desktop native layout unchanged");
        Canvas canvas = new Canvas(); canvas.setSize(400, 600);
        Class373_Sub1 mouse = new Class373_Sub1(canvas, true);
        mouse.mobileClick(70, 80, false); mouse.method3589(0);
        check(mouse.method3597(true) == 70 && mouse.method3594((byte) 89) == 80, "tap updates tracked position without hover");
        int[] types = {-1, 0, 3};
        for (int type : types) {
            Class348_Sub45 e = mouse.method3596(0);
            check(e != null && e.method3310(58) == type, "native tap event order " + type);
        }
        check(mouse.method3596(0) == null, "no duplicate native click");
        mouse.mobileClick(100, 110, true); mouse.method3589(0);
        for (int type : new int[] {-1, 2, 5}) check(mouse.method3596(0).method3310(58) == type, "context event order " + type);
        mouse.mousePressed(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 1, 0, 3, 4, 1, false, MouseEvent.BUTTON1));
        mouse.method3589(0); check(mouse.method3595(-83), "desktop press still holds button");
        mouse.mobileCancel(); mouse.method3589(0);
        check(!mouse.method3595(-83) && mouse.method3596(0) == null, "cancel does not deliver release-to-activate");
        mouse.method3592(0);
        // The production SignLink initializes this before constructing the keyboard.
        Class297.aString3782 = System.getProperty("java.vendor", "Unknown");
        Class346_Sub1 keyboard = new Class346_Sub1(canvas);
        keyboard.mobileText("Ab9"); keyboard.method2695(67);
        for (char c : "Ab9".toCharArray()) {
            Class348_Sub11 e = (Class348_Sub11) keyboard.method2697(0);
            check(e != null && e.aChar4761 == c && e.anInt4771 == 3, "native text event " + c);
        }
        keyboard.mobileText("discard"); keyboard.mobileCancel(); keyboard.method2695(67);
        check(keyboard.method2697(0) == null, "cancel discards undelivered text");
        // Actions is a one-shot next-tap context mode: never execute a default action first.
        Constructor<MobileRuntime> runtimeCtor = MobileRuntime.class.getDeclaredConstructor(); runtimeCtor.setAccessible(true);
        MobileRuntime runtime = runtimeCtor.newInstance();
        Class373_Sub1 actionMouse = new Class373_Sub1(canvas, true);
        for (String name : new String[] {"viewport", "input"}) {
            Field f = MobileRuntime.class.getDeclaredField(name); f.setAccessible(true);
            f.set(runtime, "viewport".equals(name) ? new ViewportState(1, 0, 0, 400, 600, 400, 600, 400, 600) : actionMouse);
        }
        Method arm = MobileRuntime.class.getDeclaredMethod("armContext"); arm.setAccessible(true); arm.invoke(runtime);
        GestureRecognizer.Target target = runtime.hit(10, 20); runtime.tap(target, 10, 20); actionMouse.method3589(0);
        for (int type : new int[] {-1, 2, 5}) check(actionMouse.method3596(0).method3310(58) == type, "armed Actions emits only context " + type);
        runtime.tap(target, 10, 20); actionMouse.method3589(0);
        for (int type : new int[] {-1, 0, 3}) check(actionMouse.method3596(0).method3310(58) == type, "Actions is one-shot " + type);
        arm.invoke(runtime);
        Method cancel = MobileRuntime.class.getDeclaredMethod("cancelAll"); cancel.setAccessible(true); cancel.invoke(runtime);
        Field armed = MobileRuntime.class.getDeclaredField("contextNextTap"); armed.setAccessible(true);
        check(!(Boolean) armed.get(runtime), "cancellation disarms Actions mode"); actionMouse.method3592(0);
        Method validation = MobileRuntime.class.getDeclaredMethod("validText", String.class); validation.setAccessible(true);
        check((Boolean) validation.invoke(null, "Caf\u00e9 123"), "legacy characters allowed");
        check(!(Boolean) validation.invoke(null, "hello\nworld"), "paste cannot smuggle Enter");
        check(!(Boolean) validation.invoke(null, "\ud83d\ude00"), "unsupported Unicode rejected as a whole");
        Class<?> entry = Class.forName("MobileRuntime$Entry");
        Method signature = entry.getDeclaredMethod("signature", Class348_Sub42_Sub12.class); signature.setAccessible(true);
        Class348_Sub42_Sub12 action = new Class348_Sub42_Sub12("Use", "Item", -1, 18, 42, 123L, 1, 65536, true, false, 1L, false);
        Object key = signature.invoke(null, action); action.aLong9600 = 2;
        check(!key.equals(signature.invoke(null, action)), "operation number included in stale-action validation");
        action.aLong9600 = 1; check(key.equals(signature.invoke(null, action)), "identical fresh action matches");
        Constructor<?> constructor = entry.getDeclaredConstructor(int.class, Class348_Sub42_Sub12.class); constructor.setAccessible(true);
        Object row = constructor.newInstance(0, action); Field label = entry.getDeclaredField("label"); label.setAccessible(true);
        check("Use Item".equals(label.get(row)), "native menu label keeps both action and target");
        Method parse = Loader.class.getDeclaredMethod("parseLoaderArguments", String[].class); parse.setAccessible(true);
        parse.invoke(null, (Object) new String[] {"--mobile", "--touch-mouse"});
        check(MobileConfig.enabled() && !Loader.runelite, "mobile launch opts out of desktop shell explicitly");
        check(Boolean.getBoolean("void.mobile.emulateTouch"), "mouse emulation flag parsed");
        System.clearProperty("void.mobile"); System.clearProperty("void.mobile.emulateTouch");
        String report = "Core assertions: " + core + "\nNative integration assertions: " + checks + "\nAll automated checks passed.\n"
            + "Not tested: live cache/server workflow, real phones, CheerpJ transport/native compatibility.\n";
        System.out.print(report);
        Path out = Paths.get("build/reports/mobile/regression.txt"); Files.createDirectories(out.getParent()); Files.write(out, report.getBytes(StandardCharsets.UTF_8));
    }
}
