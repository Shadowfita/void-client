import com.voidclient.mobile.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.ImageIO;

/** Real AWT/Swing host checks, not a phone, cache/server or game-rendering test. */
public final class StandaloneMobileRegression {
    private static int checks;
    private static volatile Throwable eventFailure;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static void edt(Runnable task) throws Exception { SwingUtilities.invokeAndWait(task); }
    private static void flush() throws Exception { edt(() -> {}); Thread.sleep(100); edt(() -> {}); if (eventFailure != null) throw new AssertionError("EDT exception", eventFailure); }
    private static void invalid(StandaloneSettings s, String message) { try { s.validate(); throw new AssertionError(message); } catch (IllegalArgumentException expected) { checks++; } }
    private static java.util.List<JButton> buttons(Container root) {
        java.util.List<JButton> result = new ArrayList<>();
        for (Component c : root.getComponents()) { if (c instanceof JButton) result.add((JButton) c); if (c instanceof Container) result.addAll(buttons((Container) c)); }
        return result;
    }
    private static JButton button(Container root, String name) { for (JButton b : buttons(root)) if (name.equals(b.getText()) || name.equals(b.getAccessibleContext().getAccessibleName())) return b; throw new AssertionError("Missing button " + name); }
    private static int[] size = {844, 390};
    public static void main(String[] args) throws Exception {
        try { run(); }
        catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
        finally { edt(() -> { for (Window window : Window.getWindows()) window.dispose(); }); }
    }
    private static void run() throws Exception {
        Files.createDirectories(Paths.get("build/reports/mobile"));
        Thread.setDefaultUncaughtExceptionHandler((t, error) -> { eventFailure = error; error.printStackTrace(); });
        StandaloneSettings s = new StandaloneSettings("example.invalid", 43594); s.validate();
        check(s.touch && s.fit && s.gameScale == 100, "dedicated defaults");
        check(StandaloneSettings.fittingDockScale(250, 320, 480) == 150, "oversized tools fit manual window");
        check(StandaloneSettings.usable(new Rectangle(-1920, 0, 1920, 1080), new Insets(24, 0, 48, 0)).equals(new Rectangle(-1920, 24, 1920, 1008)), "monitor origin and reserved area");
        check(StandaloneSettings.usable(new Rectangle(0, 0, 320, 240), new Insets(300, 0, 0, 0)).height == 240, "invalid insets fallback");
        check(s.windowBounds(new Rectangle(0, 0, 390, 844)).equals(new Rectangle(0, 0, 390, 844)), "portrait fit");
        s.fit = false; s.width = 1280; s.height = 720;
        check(s.windowBounds(new Rectangle(0, 0, 390, 844)).equals(new Rectangle(0, 62, 390, 720)), "manual stays in usable area");
        for (int width : new int[] {240, 320, 390, 844, 1024, 1920}) for (int scale : new int[] {100, 150, 200}) {
            int cols = StandaloneSettings.dockColumns(width, scale); check(cols >= 1 && cols <= 5, "bounded responsive columns");
        }
        s.width = 10000; invalid(s, "unbounded dimensions"); s.width = 960;
        s.address = "http://host:123"; invalid(s, "URL not endpoint"); s.address = "host/path"; invalid(s, "path rejected"); s.address = "host:123"; invalid(s, "embedded port rejected"); s.address = "::1"; s.validate(); check(true, "IPv6 literal accepted"); s.address = "example.invalid";
        s.port = 65536; invalid(s, "port overflow"); s.port = 43594; s.gameScale = 99; invalid(s, "bad scale"); s.gameScale = 125;
        Path temp = Files.createTempDirectory("void-mobile-test-"); System.setProperty("user.home", temp.toString());
        s.save(StandaloneSettings.defaultPath()); StandaloneSettings loaded = StandaloneSettings.load(StandaloneSettings.defaultPath(), "fallback", 1);
        check(loaded.address.equals(s.address) && loaded.port == s.port && loaded.gameScale == 125, "settings roundtrip");
        check(!new String(Files.readAllBytes(StandaloneSettings.defaultPath()), "UTF-8").contains("password="), "no credentials saved");
        s.fit = true; s.controlScale = 100;
        System.setProperty("void.mobile", "true"); System.setProperty("void.mobile.jarRunner", "true"); System.clearProperty("void.mobile.browser");
        StandaloneMobileHost.screens = owner -> new Rectangle(0, 0, size[0], size[1]);
        StandaloneMobileHost.applySettings(s, false);
        Loader loader = new Loader(); MobileLauncher.open(loader); JFrame host = loader.aJFrame2;
        flush();
        check(host.isUndecorated(), "dedicated host borderless");
        check(host.getBounds().equals(new Rectangle(0, 0, 844, 390)), "startup uses display not hard-coded 844x560");
        check(loader.getWidth() > 0 && loader.getHeight() > 0 && loader.getHeight() == 390, "actual applet receives full viewport height");
        check(buttons(host).isEmpty(), "no persistent Swing toolbar or mode strip");
        edt(MobileLauncher::textDialog); flush(); // harmless before game is ready
        check(eventFailure == null, "text during startup does not throw");
        for (int[] view : new int[][] {{320, 568}, {390, 844}, {844, 390}, {1024, 768}, {1920, 1080}}) {
            edt(() -> { size[0] = view[0]; size[1] = view[1]; }); Thread.sleep(900); flush();
            check(host.getWidth() == view[0] && host.getHeight() == view[1], "reported display change followed " + Arrays.toString(view));
            check(loader.getHeight() == host.getContentPane().getHeight(), "canvas retains full height " + view[1]);
            check(loader.getWidth() == host.getContentPane().getWidth(), "applet width follows " + view[0]);
            for (JButton b : buttons(host)) {
                Point p = SwingUtilities.convertPoint(b, 0, 0, host.getContentPane());
                check(p.x >= 0 && p.y >= 0 && p.x + b.getWidth() <= host.getContentPane().getWidth() && p.y + b.getHeight() <= host.getContentPane().getHeight(), "dock not clipped " + b.getText());
            }
        }
        edt(() -> { size[0] = 390; size[1] = 844; StandaloneMobileHost.fitNow(); MobileChrome.openMenu(); }); flush();
        JDialog tools=null;for(Window window:Window.getWindows())if(window instanceof JDialog&&window.isVisible()&&"Client settings".equals(((JDialog)window).getTitle()))tools=(JDialog)window;
        check(tools!=null,"More tools route reachable");final JDialog toolsDialog=tools;
        edt(()->button(toolsDialog,"Display and input").doClick());flush();
        JDialog display = null; for (Window w : Window.getWindows()) if (w instanceof JDialog && w.isVisible() && "Display and input".equals(((JDialog) w).getTitle())) display = (JDialog) w;
        check(display != null, "display dialog reachable"); final JDialog d = display;
        check(MobileBridge.hostOverlayActive() && MobileRuntime.blocksMouse(), "settings block game click-through");
        check(d.getWidth() <= host.getWidth() && d.getHeight() <= host.getHeight(), "dialog fits portrait");
        // The real form has a scrollable body and fixed actions, so bottom controls remain reachable.
        edt(() -> {
            try { ImageIO.write(new Robot().createScreenCapture(new Rectangle(host.getX(), host.getY(), host.getWidth(), host.getHeight())), "png", Paths.get("build/reports/mobile/jarrunner-display.png").toFile()); }
            catch (Exception ex) { throw new RuntimeException(ex); }
            button(d, "Reset").doClick();
        }); flush();
        check("100".equals(System.getProperty("void.mobile.gameScale")), "reset applies game size");
        edt(() -> button(d, "Close").doClick()); flush();check(MobileBridge.hostOverlayActive()&&toolsDialog.isEnabled(),"closing child surface restores parent without releasing game ownership");
        edt(()->button(toolsDialog,"Close").doClick());flush();check(!MobileBridge.hostOverlayActive(), "closing releases overlay ownership");
        s.fit = false; s.width = 320; s.height = 480; s.touch = false; s.controlScale = 0;
        edt(() -> StandaloneMobileHost.applySettings(s, false)); flush();
        check(host.getWidth() == 320 && host.getHeight() == 480, "manual window override applies");
        check(!Boolean.getBoolean("void.mobile.emulateTouch"), "ordinary mouse profile applies without restart");
        check(StandaloneMobileHost.controlScale() == 100, "auto controls use manual window rather than virtual desktop");
        s.controlScale = 250; edt(() -> StandaloneMobileHost.applySettings(s, false)); flush();
        check(StandaloneMobileHost.controlScale() < 250, "excessive tool scale capped to keep controls reachable");
        check(buttons(host).isEmpty(), "large control scale does not restore a toolbar");
        MobileBridge.drain();
        edt(() -> host.setSize(300, 460)); flush();
        boolean cancelled = false; for (MobileBridge.Command c : MobileBridge.drain()) cancelled |= "cancel".equals(c.type);
        check(cancelled, "actual resize cancels active input");
        check(StandaloneMobileHost.contentSize().width == 300, "actual content dimensions published");
        check(StandaloneMobileHost.diagnostics().contains("Java display"), "diagnostics show actual dimensions");
        edt(host::dispose); flush();
        check(StandaloneMobileHost.contentSize().width == 0 && !MobileBridge.hostOverlayActive(), "host disposal cleans up");
        // Browser host still follows explicitly supplied dimensions and has no native dock.
        System.setProperty("void.mobile.browser", "true");
        MobileLauncher.resizeHost(500, 300); Loader browser = new Loader(); MobileLauncher.open(browser); flush();
        check(browser.aJFrame2.getWidth() == 500 && browser.aJFrame2.getHeight() == 300, "browser resize contract preserved");
        check(buttons(browser.aJFrame2).isEmpty(), "browser has no native dock"); edt(browser.aJFrame2::dispose); flush();
        // Actual software-renderer scale policy and the existing input transform, without game assets.
        System.clearProperty("void.mobile.browser");
        Canvas canvas = new Canvas(); canvas.setSize(800, 600); client applet = new client();
        Class348_Sub40_Sub9.anApplet_Sub1_9169 = applet; Class305.aCanvas3869 = canvas;
        Class50_Sub1.aBoolean5219 = true; Class321.anInt4017 = 800; Class348_Sub42_Sub8_Sub2.anInt10432 = 600;
        Class348_Sub8.aHa6654 = new ha_Sub1(canvas, null, 800, 600);
        check(applet.applyStandaloneMobileScale(125), "software scaling policy applies");
        check(applet.isStretchedEnabled() && applet.getScalingFactor() == 125 && applet.getInterfaceScalingFactor() == 100, "honest whole-canvas fallback");
        check(!applet.applyStandaloneMobileScale(125), "no repeated relayout at stable scale");
        Applet_Sub1.applyStretchedLogicalSize();
        check(Class321.anInt4017 == 640 && Class348_Sub42_Sub8_Sub2.anInt10432 == 480, "software logical dimensions");
        check(Applet_Sub1.scaleMouseX(400) == 320 && Applet_Sub1.scaleMouseY(300) == 240, "scaled input matches render dimensions");
        check(applet.applyStandaloneMobileScale(100) && !applet.isStretchedEnabled(), "100% restores unscaled policy");
        System.clearProperty("void.mobile");
        check(!applet.applyStandaloneMobileScale(150), "desktop ignores standalone scaling");
        System.setProperty("void.mobile", "true"); System.setProperty("void.mobile.browser", "true");
        check(!applet.applyStandaloneMobileScale(150), "browser scale contract unaffected");
        System.clearProperty("void.mobile.browser");
        Class348_Sub8.aHa6654.method3635((byte) -115); Class348_Sub8.aHa6654 = null;
        Class348_Sub40_Sub9.anApplet_Sub1_9169 = null; Class305.aCanvas3869 = null;
        // An ordinary-mouse user can still arm Actions, then tap a target without its default action firing.
        Field singleton = MobileRuntime.class.getDeclaredField("INSTANCE"); singleton.setAccessible(true); Object runtime = singleton.get(null);
        Field armed = MobileRuntime.class.getDeclaredField("contextNextTap"); armed.setAccessible(true); armed.setBoolean(runtime, true);
        System.setProperty("void.mobile.emulateTouch", "false");
        Class373_Sub1 mouse = new Class373_Sub1(canvas, true); MobileBridge.drain();
        mouse.mousePressed(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 1, MouseEvent.BUTTON1_DOWN_MASK, 20, 30, 1, false, MouseEvent.BUTTON1));
        mouse.mouseReleased(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 20, 30, 1, false, MouseEvent.BUTTON1));
        java.util.List<MobileBridge.Command> queued = MobileBridge.drain();
        check(queued.size() == 2 && "down".equals(queued.get(0).type) && "up".equals(queued.get(1).type), "Actions routes ordinary mouse through context-safe recognizer");
        armed.setBoolean(runtime, false); mouse.mobileCancel();
        System.setProperty("void.mobile.emulateTouch", "true"); MobileBridge.drain();
        mouse.mousePressed(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 3, MouseEvent.BUTTON1_DOWN_MASK, 20, 30, 1, false, MouseEvent.BUTTON1));
        System.setProperty("void.mobile.emulateTouch", "false");
        mouse.mouseReleased(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 4, 0, 20, 30, 1, false, MouseEvent.BUTTON1));
        queued = MobileBridge.drain();
        check(queued.size() == 2 && "up".equals(queued.get(1).type), "pointer route retained to release");
        mouse.method3592(0);
        String report = "Standalone host assertions: " + checks + "\nActual Swing host and controls tested under Xvfb; injected AWT display bounds.\nNot tested: Android Jar Runner, phone keyboard/rotation delivery, cache/server gameplay.\n";
        Files.createDirectories(Paths.get("build/reports/mobile")); Files.write(Paths.get("build/reports/mobile/jarrunner.txt"), report.getBytes("UTF-8")); System.out.print(report);
    }
}
