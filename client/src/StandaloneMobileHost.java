import com.google.gson.*;
import com.voidclient.mobile.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.*;
import javax.swing.*;

/** AWT-visible viewport fitting. Does not pretend to receive Android display, inset or touch APIs. */
final class StandaloneMobileHost {
    private static StandaloneSettings settings;
    private static JFrame frame;
    private static JPanel dock;
    private static javax.swing.Timer monitor;
    private static Rectangle lastScreen;
    private static Dimension lastContent;
    private static int toolScale = 100, lastColumns;
    private static String persistenceStatus = "";
    private static final Set<Window> overlays = new HashSet<>();
    private static volatile Dimension publishedSize = new Dimension();
    // Tests can supply changing AWT display bounds without pretending to emulate Android.
    interface ScreenSource { Rectangle bounds(JFrame owner); }
    static ScreenSource screens = StandaloneMobileHost::readScreen;
    private StandaloneMobileHost() { }

    static StandaloneSettings loadSettings() {
        try { return StandaloneSettings.load(StandaloneSettings.defaultPath(), Loader.address, Loader.port); }
        catch (Exception ex) { persistenceStatus = "Saved mobile settings unavailable; defaults loaded (" + ex.getClass().getSimpleName() + ")."; }
        return new StandaloneSettings(Loader.address, Loader.port);
    }
    static void applySettings(StandaloneSettings value, boolean persist) {
        value.validate(); settings = value.copy();
        MobileBridge.cancel();
        System.setProperty("void.mobile.emulateTouch", Boolean.toString(settings.touch));
        System.setProperty("void.mobile.gameScale", Integer.toString(settings.gameScale));
        if (persist) {
            try { Path path = StandaloneSettings.defaultPath(); settings.save(path); persistenceStatus = "Mobile settings saved."; }
            catch (Exception ex) { persistenceStatus = "Applied for this session; saving unavailable (" + ex.getClass().getSimpleName() + ")."; }
        }
        if (frame != null) fitNow();
    }
    static boolean configureStartup(StandaloneSettings value) throws Exception {
        final boolean[] launch = {false};
        Runnable show = () -> {
            JDialog d = new JDialog((Frame) null, "Void — Android / Jar Runner JR2–JR5 candidate", true);
            Rectangle area = screens.bounds(null);
            int scale = value.effectiveControlScale(area);
            JPanel panel = new JPanel(new BorderLayout(8, 8)); panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            VerticalForm fields = new VerticalForm();
            JTextField address = new JTextField(value.address); address.getAccessibleContext().setAccessibleName("Server hostname or IP address");
            JTextField port = new JTextField(Integer.toString(value.port)); port.getAccessibleContext().setAccessibleName("Server TCP port");
            fields.add(new JLabel("Server hostname / IP (not a URL)")); fields.add(address);
            fields.add(new JLabel("TCP port")); fields.add(port);
            JTextArea hint = note("Mobile mode is automatic. Open More → Display in the game toolbar to adjust sizing and input.");
            fields.add(hint);
            JLabel error = new JLabel(persistenceStatus); fields.add(error);
            panel.add(new TouchScrollPane(fields), BorderLayout.CENTER);
            JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
            buttons.add(makeButton("Cancel", d::dispose));
            buttons.add(makeButton("Start client", () -> {
                try {
                    value.address = address.getText().trim(); value.port = Integer.parseInt(port.getText().trim()); value.validate();
                    applySettings(value, true); launch[0] = true; d.dispose();
                } catch (RuntimeException ex) { error.setText("Check the address and TCP port (1–65535)."); }
            }));
            panel.add(buttons, BorderLayout.SOUTH); style(panel, scale); d.add(panel);
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.setSize(Math.min(area.width, 500 * scale / 100), Math.min(area.height, 380 * scale / 100));
            d.setLocation(area.x + (area.width - d.getWidth()) / 2, area.y + (area.height - d.getHeight()) / 2);
            d.setVisible(true);
        };
        if (SwingUtilities.isEventDispatchThread()) show.run(); else SwingUtilities.invokeAndWait(show);
        return launch[0];
    }
    static void install(JFrame owner, JPanel toolbar) {
        frame = owner; dock = toolbar;
        if (settings == null) {
            // Ordinary --mobile retains explicit input/scale choices; the dedicated entry point supplies its own defaults.
            settings = new StandaloneSettings(Loader.address, Loader.port);
            settings.touch = Boolean.getBoolean("void.mobile.emulateTouch");
            settings.gameScale = MobileConfig.integer("void.mobile.gameScale", 100, 100, 200);
        }
        frame.setMinimumSize(new Dimension(1, 1)); frame.setResizable(true);
        frame.getContentPane().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { contentChanged(); }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (monitor != null) monitor.stop();
                for (Window w : new ArrayList<>(overlays)) w.dispose();
                overlays.clear(); MobileBridge.setHostOverlayActive(false); MobileBridge.cancel();
                frame = null; dock = null; lastContent = null; lastScreen = null; publishedSize = new Dimension();
            }
        });
        toolbar.add(makeButton("More", MobileLauncher::moreDialog));
        fitNow();
        monitor = new javax.swing.Timer(750, e -> {
            if (frame == null || !frame.isDisplayable() || (frame.getExtendedState() & Frame.ICONIFIED) != 0) return;
            Rectangle current = screens.bounds(frame);
            if (!current.equals(lastScreen)) {
                lastScreen = current;
                if (settings.fit) fitNow(); else { updateDock(); constrainDialogs(); }
            }
        });
        monitor.start();
    }
    static Dimension contentSize() { return new Dimension(publishedSize); }
    static void fitNow() {
        if (frame == null) return;
        lastScreen = screens.bounds(frame); Rectangle target = settings.windowBounds(lastScreen);
        MobileBridge.cancel(); frame.setBounds(target); frame.validate(); updateDock(); contentChanged(); constrainDialogs();
    }
    private static void contentChanged() {
        if (frame == null) return;
        Dimension size = frame.getContentPane().getSize(); publishedSize = new Dimension(size);
        if (!size.equals(lastContent)) { lastContent = new Dimension(size); MobileBridge.cancel(); updateDock(); constrainDialogs(); }
    }
    private static void updateDock() {
        if (frame == null || dock == null) return;
        Dimension current = frame.getContentPane().getSize();
        Rectangle available = new Rectangle(0, 0, Math.max(1, current.width), Math.max(1, current.height));
        toolScale = StandaloneSettings.fittingDockScale(settings.effectiveControlScale(available), available.width, available.height);
        style(dock, toolScale);
        int width = Math.max(1, frame.getContentPane().getWidth() - 8);
        int columns = StandaloneSettings.dockColumns(width, toolScale);
        if (columns != lastColumns) { lastColumns = columns; dock.setLayout(new GridLayout(0, columns, 4, 4)); }
        dock.revalidate(); dock.repaint();
    }
    static int controlScale() { return toolScale; }
    static JButton makeButton(String label, Runnable action) {
        JButton b = new JButton(label); b.getAccessibleContext().setAccessibleName(label);
        b.addActionListener(e -> action.run()); style(b, toolScale); return b;
    }
    static void style(Component component, int scale) {
        if(component instanceof MobilePanels || component instanceof MobileWidgets.WrappingButton || component instanceof MobileTextEditor) return;
        float fontSize = Math.max(16, 16f * scale / 100);
        if (component.getFont() != null) component.setFont(component.getFont().deriveFont(fontSize));
        int height = Math.max(48, 48 * scale / 100);
        if (component instanceof AbstractButton || component instanceof JComboBox || component instanceof JTextField) {
            component.setMinimumSize(new Dimension(48, height));
            // Recompute rather than compounding the previous requested size when changing scale.
            component.setPreferredSize(null);
            int naturalWidth = component.getPreferredSize().width;
            component.setPreferredSize(new Dimension(Math.max(48, naturalWidth), height));
            return; // Do not enlarge combo arrows and other UI-delegate internals into separate touch targets.
        }
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) style(child, scale);
    }
    private static JTextArea note(String text) {
        JTextArea area = new JTextArea(text, 3, 20); area.setEditable(false); area.setFocusable(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setOpaque(false); return area;
    }
    /** Natural-height rows, always tracking viewport width; no horizontal panning of settings. */
    private static final class VerticalForm extends JPanel implements Scrollable {
        private int row;
        VerticalForm() { super(new GridBagLayout()); }
        @Override public Component add(Component component) {
            GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row++;
            c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(4, 0, 4, 0); super.add(component, c); return component;
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(320, 500); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 32; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(32, r.height - 48); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
    static void track(JDialog dialog) {
        overlays.add(dialog); MobileBridge.setHostOverlayActive(true); MobileInterfaceManager.track(dialog);
        style(dialog.getContentPane(), toolScale);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                overlays.remove(dialog); MobileBridge.setHostOverlayActive(!overlays.isEmpty());
            }
        });
        constrain(dialog);
    }
    private static void constrainDialogs() { for (Window window : new ArrayList<>(overlays)) if (window instanceof JDialog) constrain((JDialog) window); }
    private static void constrain(JDialog dialog) {
        if (frame == null) return;
        int w = Math.max(1, Math.min(dialog.getWidth(), frame.getWidth()));
        int h = Math.max(1, Math.min(dialog.getHeight(), frame.getHeight()));
        dialog.setBounds(frame.getX() + (frame.getWidth() - w) / 2, frame.getY() + (frame.getHeight() - h) / 2, w, h);
    }
    private static Rectangle readScreen(JFrame owner) {
        try {
            GraphicsConfiguration gc = owner == null ? null : owner.getGraphicsConfiguration();
            GraphicsDevice device = gc == null ? GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice() : gc.getDevice();
            gc = device.getDefaultConfiguration();
            Insets insets; try { insets = Toolkit.getDefaultToolkit().getScreenInsets(gc); }
            catch (RuntimeException ex) { insets = new Insets(0, 0, 0, 0); }
            return StandaloneSettings.usable(gc.getBounds(), insets);
        } catch (RuntimeException ex) {
            try { Dimension size = Toolkit.getDefaultToolkit().getScreenSize(); return new Rectangle(0, 0, size.width, size.height); }
            catch (RuntimeException unavailable) { return new Rectangle(0, 0, 960, 540); }
        }
    }
    static void displayDialog() {
        MobileBridge.cancel();
        StandaloneSettings edited = settings.copy();
        VerticalForm form = new VerticalForm(); form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JCheckBox fit = new JCheckBox("Fit Java-visible display", edited.fit);
        JTextField width = new JTextField(Integer.toString(edited.width));
        JTextField height = new JTextField(Integer.toString(edited.height));
        width.getAccessibleContext().setAccessibleName("Manual window width"); height.getAccessibleContext().setAccessibleName("Manual window height");
        JComboBox<String> input = new JComboBox<>(new String[] {"Touch gestures", "Ordinary mouse"}); input.setSelectedIndex(edited.touch ? 0 : 1);
        JComboBox<String> game = new JComboBox<>(new String[] {"100%", "125%", "150%", "175%", "200%"});
        game.setSelectedIndex((edited.gameScale - 100) / 25);
        game.getAccessibleContext().setAccessibleName("Game interface size percent");
        JComboBox<String> controls = new JComboBox<>(new String[] {"Auto", "100%", "125%", "150%", "175%", "200%", "225%", "250%"});
        int controlIndex = edited.controlScale == 0 ? 0 : 1 + (edited.controlScale - 100) / 25; controls.setSelectedIndex(controlIndex);
        controls.getAccessibleContext().setAccessibleName("Toolbar control size"); input.getAccessibleContext().setAccessibleName("Input profile");
        Runnable enabled = () -> { width.setEnabled(!fit.isSelected()); height.setEnabled(!fit.isSelected()); };
        fit.addActionListener(e -> enabled.run()); enabled.run();
        form.add(fit); form.add(new JLabel("Manual window width / height (Java units)")); form.add(width); form.add(height);
        form.add(new JLabel("Input profile")); form.add(input);
        form.add(new JLabel("Game interface size (%)")); form.add(game);
        form.add(new JLabel("Toolbar / dialog size")); form.add(controls);
        form.add(note("Software rendering scales the whole game, not just interfaces. Fitting cannot change Jar Runner's own virtual desktop. Tool size is capped when needed to keep Display reachable."));
        JTextArea info = new JTextArea(5, 20); info.setEditable(false); info.setLineWrap(true); info.setWrapStyleWord(true); form.add(info);
        JLabel result = new JLabel(persistenceStatus); form.add(result);
        JPanel content = new JPanel(new BorderLayout(4, 4)); content.add(new TouchScrollPane(form), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 4)); content.add(buttons, BorderLayout.SOUTH);
        JDialog dialog = new JDialog(frame, "Display and input", false);
        Runnable apply = () -> {
            try {
                
                edited.fit = fit.isSelected(); edited.width = Integer.parseInt(width.getText().trim()); edited.height = Integer.parseInt(height.getText().trim());
                edited.touch = input.getSelectedIndex() == 0; edited.gameScale = 100 + game.getSelectedIndex() * 25;
                edited.controlScale = controls.getSelectedIndex() == 0 ? 0 : 100 + (controls.getSelectedIndex() - 1) * 25;
                applySettings(edited, true); result.setText(persistenceStatus); style(dialog.getContentPane(), toolScale);
                dialog.validate(); constrain(dialog);
            } catch (Exception ex) { result.setText("Invalid dimensions or scale; changes not applied."); }
        };
        buttons.add(makeButton("Apply", apply));
        buttons.add(makeButton("Reset", () -> {
            fit.setSelected(true); width.setText("960"); height.setText("540"); input.setSelectedIndex(0); game.setSelectedIndex(0); controls.setSelectedIndex(0); enabled.run(); apply.run();
        }));
        buttons.add(makeButton("Close", () -> { MobileBridge.cancel(); dialog.dispose(); }));
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); dialog.add(content);
        dialog.setSize(Math.min(frame.getWidth(), 520 * toolScale / 100), Math.min(frame.getHeight(), 660 * toolScale / 100));
        track(dialog);
        javax.swing.Timer diagnostics = new javax.swing.Timer(500, e -> info.setText(diagnostics())); diagnostics.start(); info.setText(diagnostics());
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { MobileBridge.cancel(); }
            @Override public void windowClosed(WindowEvent e) { diagnostics.stop(); }
        });
        dialog.setVisible(true);
    }
    static String diagnostics() {
        Rectangle screen = screens.bounds(frame); Dimension content = contentSize();
        String text = "Java display: " + screen.width + " x " + screen.height + "\nHost: " + content.width + " x " + content.height;
        try {
            JsonObject snapshot = JsonParser.parseString(MobileBridge.snapshot()).getAsJsonObject();
            if (snapshot.has("viewport") && snapshot.get("viewport").isJsonObject()) {
                JsonObject v = snapshot.getAsJsonObject("viewport");
                text += "\nRender: " + v.get("nativeWidth") + " x " + v.get("nativeHeight")
                    + " | UI: " + v.get("logicalWidth") + " x " + v.get("logicalHeight");
            }
            if (snapshot.has("scaleMode")) text += "\n" + snapshot.get("scaleMode").getAsString();
        } catch (RuntimeException ignored) { text += "\nWaiting for client viewport."; }
        return text + "\n" + (Boolean.getBoolean("void.mobile.emulateTouch") ? "Touch gestures" : "Ordinary mouse")
            + " | Tools " + toolScale + "%";
    }
}
