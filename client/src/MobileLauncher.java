import com.google.gson.*;
import com.voidclient.mobile.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Optional mobile host; normal Loader/RuneLite startup is untouched unless explicitly enabled. */
public final class MobileLauncher {
    private static volatile JFrame host;
    private static volatile int requestedWidth = 844, requestedHeight = 390;
    private static boolean started;
    private static volatile double displayScale = 1.0;
    private static JDialog actions;
    private static long shownMenu = -1;
    private MobileLauncher() { }

    /** Kept stable for CheerpJ library-mode access in the obfuscated release. */
    public static synchronized void start(String address, int port) throws Exception {
        if (started) throw new IllegalStateException("Client already started");
        if (address == null || address.trim().isEmpty() || port < 1 || port > 65535)
            throw new IllegalArgumentException("A server address and TCP port are required");
        started = true;
        System.setProperty("void.mobile", "true");
        System.setProperty("void.mobile.browser", "true");
        Loader.main(new String[] {"--mobile", "--address", address, "--port", Integer.toString(port)});
    }
    public static void setDisplayScale(double scale) {
        if (!Double.isFinite(scale) || scale < 1 || scale > 2) throw new IllegalArgumentException("Display scale must be 1–2");
        displayScale = scale; com.voidclient.mobile.MobileBridge.cancel();
    }
    static double displayScale() { return displayScale; }
    public static void resizeHost(int width, int height) {
        if (width < 1 || height < 1 || width > 8192 || height > 8192)
            throw new IllegalArgumentException("Invalid host size");
        requestedWidth = width; requestedHeight = height;
        if (!MobileConfig.browser()) return;
        SwingUtilities.invokeLater(() -> { if (host != null) { host.setBounds(0, 0, requestedWidth, requestedHeight); host.validate(); } });
    }
    static Point canvasOrigin(Component canvas) {
        JFrame frame = host;
        if (frame == null) return new Point(0, 0);
        return SwingUtilities.convertPoint(canvas, 0, 0, frame.getContentPane());
    }
    static int[] hostSize() {
        JFrame frame = host;
        return frame == null ? new int[] {0, 0} : new int[] {frame.getContentPane().getWidth(), frame.getContentPane().getHeight()};
    }
    static void open(Loader loader) {
        Runnable build = () -> {
            JFrame frame = new JFrame("Void — Mobile preview");
            host = frame; loader.aJFrame2 = frame;
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(MobileConfig.browser());
            frame.setMinimumSize(new Dimension(240, 240));
            if (MobileConfig.browser()) frame.setMinimumSize(new Dimension(1, 1));
            loader.setMinimumSize(new Dimension(1, 1));
            loader.aJPanel3.setLayout(new BorderLayout()); loader.aJPanel3.setMinimumSize(new Dimension(1, 1));
            loader.aJPanel3.add(loader, BorderLayout.CENTER);
            frame.add(loader.aJPanel3, BorderLayout.CENTER);
            if (!MobileConfig.browser()) {
                JPanel dock = new JPanel(new GridLayout(1, 4, 4, 4));
                dock.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                dock.add(button("Actions", () -> send("context", 0)));
                dock.add(button("Camera", MobileLauncher::cameraDialog));
                dock.add(button("Text", MobileLauncher::textDialog));
                dock.add(button("Inspect", MobileLauncher::inspectDialog));
                frame.add(dock, BorderLayout.SOUTH);
                new Timer(100, event -> refreshActions()).start();
                frame.addWindowStateListener(event -> MobileBridge.setSuspended((event.getNewState() & Frame.ICONIFIED) != 0));
                frame.addWindowFocusListener(new WindowAdapter() {
                    @Override public void windowLostFocus(WindowEvent e) {
                        Window other = e.getOppositeWindow();
                        while (other != null && other != frame) other = other.getOwner();
                        if (other == null) MobileBridge.cancel();
                    }
                });
            }
            frame.setSize(requestedWidth, MobileConfig.browser() ? requestedHeight : 560);
            if (MobileConfig.browser()) frame.setLocation(0, 0);
            frame.setVisible(true);
        };
        try { if (SwingUtilities.isEventDispatchThread()) build.run(); else SwingUtilities.invokeAndWait(build); }
        catch (Exception ex) { throw new IllegalStateException("Cannot create mobile host", ex); }
    }
    private static JButton button(String label, Runnable action) {
        JButton b = new JButton(label);
        b.setFont(b.getFont().deriveFont(16f));
        b.setPreferredSize(new Dimension(112, 48)); b.setMinimumSize(new Dimension(48, 48));
        b.getAccessibleContext().setAccessibleName(label);
        b.addActionListener(event -> action.run()); return b;
    }
    private static void send(String name, int id) { MobileBridge.action(name, id, MobileBridge.revision()); }
    private static JDialog dialog(String name, JPanel content) {
        JDialog d = new JDialog(host, name, false);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { MobileBridge.cancel(); }
        });
        d.add(new JScrollPane(content));
        d.setSize(Math.min(420, host.getWidth()), Math.min(440, host.getHeight()));
        d.setLocationRelativeTo(host); d.setVisible(true); return d;
    }
    private static void cameraDialog() {
        MobileBridge.cancel();
        JPanel p = new JPanel(new GridLayout(0, 2, 4, 4));
        String[][] options = {{"Left", "cameraLeft"}, {"Right", "cameraRight"}, {"Up", "cameraUp"},
            {"Down", "cameraDown"}, {"Zoom in", "zoomIn"}, {"Zoom out", "zoomOut"}};
        for (String[] option : options) p.add(button(option[0], () -> send(option[1], 0)));
        dialog("Camera", p);
    }
    private static void textDialog() {
        MobileBridge.cancel();
        final long session = JsonParser.parseString(MobileBridge.snapshot()).getAsJsonObject().get("textSession").getAsLong();
        MobileBridge.setTextFocus(true);
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        JPasswordField field = new JPasswordField(); field.setFont(field.getFont().deriveFont(18f));
        field.getAccessibleContext().setAccessibleName("Text to insert into the focused game field");
        JPanel controls = new JPanel(new GridLayout(0, 2, 4, 4));
        JCheckBox show = new JCheckBox("Show text"); show.addActionListener(e -> field.setEchoChar(show.isSelected() ? '\0' : '\u2022'));
        panel.add(new JLabel("Focus a game field first. Insert does not press Enter."), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        controls.add(show);
        controls.add(button("Insert", () -> {
            char[] value = field.getPassword();
            try { MobileBridge.insertText(new String(value), session); }
            finally { java.util.Arrays.fill(value, '\0'); field.setText(""); }
        }));
        controls.add(button("Enter", () -> MobileBridge.action("key", KeyEvent.VK_ENTER, session)));
        controls.add(button("Backspace", () -> MobileBridge.action("key", KeyEvent.VK_BACK_SPACE, session)));
        controls.add(button("Next field", () -> MobileBridge.action("key", KeyEvent.VK_TAB, session)));
        controls.add(button("Escape", () -> MobileBridge.action("key", KeyEvent.VK_ESCAPE, session)));
        panel.add(controls, BorderLayout.SOUTH);
        JDialog d = dialog("Text entry", panel);
        d.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { field.setText(""); MobileBridge.setTextFocus(false); } });
        field.requestFocusInWindow();
    }
    private static void inspectDialog() {
        send("inspect", 0);
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea text = new JTextArea("Collecting widget geometry…"); text.setEditable(false); text.setLineWrap(true);
        panel.add(text, BorderLayout.CENTER);
        JDialog d = dialog("Widget geometry (no field text)", panel);
        Timer timer = new Timer(250, e -> text.setText(MobileBridge.snapshot())); timer.start();
        d.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { timer.stop(); MobileBridge.cancel(); } });
    }
    private static void refreshActions() {
        JsonObject state = JsonParser.parseString(MobileBridge.snapshot()).getAsJsonObject();
        if (!state.has("menu")) return;
        JsonArray entries = state.getAsJsonArray("menu");
        if (entries.size() == 0) { if (actions != null) { actions.dispose(); actions = null; } return; }
        long id = state.get("menuId").getAsLong();
        if (shownMenu == id) return;
        shownMenu = id;
        if (actions != null) actions.dispose();
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        for (JsonElement element : entries) {
            JsonObject row = element.getAsJsonObject();
            int index = row.get("id").getAsInt();
            panel.add(button(row.get("label").getAsString(), () -> MobileBridge.action("select", index, id)));
        }
        panel.add(button("Cancel", MobileBridge::cancel));
        actions = dialog("Choose action", panel);
    }
}
