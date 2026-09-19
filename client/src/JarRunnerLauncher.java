import com.voidclient.mobile.StandaloneSettings;
import java.util.*;

/** Dedicated JAR entry point. Loader remains the desktop/browser artifact's entry point. */
public final class JarRunnerLauncher {
    private JarRunnerLauncher() { }
    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--help")) {
            System.out.println("Void Jar Runner native-mobile candidate 3 (JR2–JR5)\nOpen this JAR without arguments to configure server and display.\n"
                + "Optional: --address HOST --port PORT --skip-setup\nMobile mode is automatic. No Android APIs or raw multitouch are assumed.");
            return;
        }
        com.voidclient.mobile.AccessibilityPreferences.load();
        StandaloneSettings settings = StandaloneMobileHost.loadSettings();
        List<String> forwarded = new ArrayList<>(); boolean skip = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i], value = null; int equals = arg.indexOf('=');
            if (equals > 0) { value = arg.substring(equals + 1); arg = arg.substring(0, equals); }
            if ("--skip-setup".equals(arg)) { skip = true; continue; }
            if ("--address".equals(arg) || "-ip".equals(arg) || "--port".equals(arg) || "-p".equals(arg)) {
                if (value == null) { if (++i >= args.length) throw new IllegalArgumentException(arg + " needs a value"); value = args[i]; }
                if ("--port".equals(arg) || "-p".equals(arg)) settings.port = Integer.parseInt(value); else settings.address = value;
            } else if (!"--mobile".equals(arg) && !"--touch-mouse".equals(arg) && !"--classic".equals(arg)) forwarded.add(args[i]);
        }
        settings.validate();
        if (!skip && !StandaloneMobileHost.configureStartup(settings)) return;
        System.setProperty("void.mobile", "true");
        System.setProperty("void.mobile.browser", "false");
        System.setProperty("void.mobile.jarRunner", "true");
        StandaloneMobileHost.applySettings(settings, false);
        forwarded.addAll(Arrays.asList("--mobile", "--address", settings.address, "--port", Integer.toString(settings.port)));
        Loader.main(forwarded.toArray(new String[0]));
    }
}
