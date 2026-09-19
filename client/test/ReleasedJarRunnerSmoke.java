import java.awt.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

/** Run with ONLY test output + the obfuscated Jar Runner JAR, never main source-set output. */
public final class ReleasedJarRunnerSmoke {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static java.util.List<Component> descendants(Container root) {
        java.util.List<Component> result = new ArrayList<>();
        for (Component c : root.getComponents()) { result.add(c); if (c instanceof Container) result.addAll(descendants((Container) c)); }
        return result;
    }
    public static void main(String[] args) throws Exception {
        try { run(); }
        catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
        finally { SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) w.dispose(); }); }
    }
    private static void run() throws Exception {
        System.setProperty("user.home", Files.createTempDirectory("void-release-smoke-").toString());
        Class<?> entry = Class.forName("JarRunnerLauncher");
        String source = entry.getProtectionDomain().getCodeSource().getLocation().toString();
        check(source.endsWith("void-client-jarrunner-mobile.jar"), "test really loads released JAR, not development classes");
        Method main = entry.getMethod("main", String[].class);
        AtomicReference<Throwable> failed = new AtomicReference<>();
        Thread launch = new Thread(() -> {
            try { main.invoke(null, (Object) new String[] {"--address", "127.0.0.1", "--port", "43594"}); }
            catch (Throwable error) { failed.set(error); }
        }, "release-launch-test");
        launch.start();
        final JDialog[] dialog = {null};
        for (int i = 0; i < 100 && dialog[0] == null && failed.get() == null; i++) {
            SwingUtilities.invokeAndWait(() -> {
                for (Window w : Window.getWindows())
                    if (w instanceof JDialog && w.isVisible() && ((JDialog) w).getTitle().contains("Jar Runner JR2")) dialog[0] = (JDialog) w;
            });
            Thread.sleep(100);
        }
        if (failed.get() != null) throw new AssertionError("release launch failed", failed.get());
        check(dialog[0] != null, "obfuscated entry point displays setup without arguments required");
        SwingUtilities.invokeAndWait(() -> {
            JDialog d = dialog[0]; Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            check(d.getWidth() <= screen.width && d.getHeight() <= screen.height, "setup fits available display");
            JButton cancel = null; boolean address = false, port = false, start = false;
            for (Component c : descendants(d)) {
                if (c instanceof JTextField) {
                    address |= "127.0.0.1".equals(((JTextField) c).getText());
                    port |= "43594".equals(((JTextField) c).getText());
                }
                if (c instanceof JButton) {
                    JButton b = (JButton) c;
                    if ("Start client".equals(b.getText())) { start = true; check(b.getHeight() >= 48, "start touch target"); }
                    if ("Cancel".equals(b.getText())) cancel = b;
                }
            }
            check(address && port, "server fields receive explicit overrides");
            check(start && cancel != null, "setup actions reachable");
            cancel.doClick();
        });
        launch.join(3000);
        check(!launch.isAlive() && failed.get() == null, "cancel returns without starting game or connecting");
        Files.createDirectories(Paths.get("build/reports/mobile"));
        String report = "Released JAR assertions: " + checks + "\nActual obfuscated entry point and Swing startup dialog; cancelled before networking or gameplay.\n";
        Files.write(Paths.get("build/reports/mobile/jarrunner-release.txt"), report.getBytes("UTF-8"));
        System.out.print(report);
    }
}
