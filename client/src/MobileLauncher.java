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
    private static JDialog panels,tools;
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
        if (!MobileConfig.browser()) {
            Dimension size = StandaloneMobileHost.contentSize(); return new int[] {size.width, size.height};
        }
        return frame == null ? new int[] {0, 0} : new int[] {frame.getContentPane().getWidth(), frame.getContentPane().getHeight()};
    }
    static void open(Loader loader) {
        Runnable build = () -> {
            JFrame frame = new JFrame(Boolean.getBoolean("void.mobile.jarRunner") ? "Void — Android / Jar Runner JR2–JR5 native-mobile candidate 4 — bounded HUD shell" : "Void — Mobile preview");
            host = frame; loader.aJFrame2 = frame;
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(MobileConfig.browser() || Boolean.getBoolean("void.mobile.jarRunner"));
            frame.setMinimumSize(new Dimension(240, 240));
            if (MobileConfig.browser()) frame.setMinimumSize(new Dimension(1, 1));
            loader.setMinimumSize(new Dimension(1, 1));
            loader.aJPanel3.setLayout(new BorderLayout()); loader.aJPanel3.setMinimumSize(new Dimension(1, 1));
            loader.aJPanel3.add(loader, BorderLayout.CENTER);
            frame.add(loader.aJPanel3, BorderLayout.CENTER);
            if (!MobileConfig.browser()) {
                // No host footer: the native game Canvas receives the complete content rectangle.
                MobileChrome.install(MobileLauncher::moreDialog);
                StandaloneMobileHost.install(frame);
                frame.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosed(WindowEvent e) { CanvasActionMenu.close(); MobileChrome.clear(); MobileChrome.install(null); host=null; }
                });
                frame.addWindowStateListener(event -> MobileBridge.setSuspended((event.getNewState() & Frame.ICONIFIED) != 0));
                frame.addWindowFocusListener(new WindowAdapter() {
                    @Override public void windowLostFocus(WindowEvent e) {
                        Window other = e.getOppositeWindow();
                        while (other != null && other != frame) other = other.getOwner();
                        if (other == null) MobileBridge.cancel();
                    }
                });
            }
            if (MobileConfig.browser()) frame.setBounds(0, 0, requestedWidth, requestedHeight);
            frame.setVisible(true);
        };
        try { if (SwingUtilities.isEventDispatchThread()) build.run(); else SwingUtilities.invokeAndWait(build); }
        catch (Exception ex) { throw new IllegalStateException("Cannot create mobile host", ex); }
    }
    private static JButton button(String label, Runnable action) {
        return StandaloneMobileHost.makeButton(label, action);
    }
    private static void send(String name, int id) { MobileBridge.action(name, id, MobileBridge.revision()); }
    private static JDialog dialog(String name, JPanel content) {
        JDialog d = new JDialog(host, name, false);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { MobileBridge.cancel(); }
        });
        TouchScrollPane scroll=new TouchScrollPane(content);d.add(scroll, BorderLayout.CENTER);
        JPanel pages=new JPanel(new GridLayout(1,2,4,4));pages.add(button("Page up",()->scroll.page(-1)));pages.add(button("Page down",()->scroll.page(1)));d.add(pages,BorderLayout.NORTH);
        d.add(button("Close", () -> { MobileBridge.cancel(); d.dispose(); }), BorderLayout.SOUTH);
        int scale = StandaloneMobileHost.controlScale();
        d.setSize(Math.min(420 * scale / 100, host.getWidth()), Math.min(440 * scale / 100, host.getHeight()));
        d.setLocationRelativeTo(host); StandaloneMobileHost.track(d); d.setVisible(true); return d;
    }
    private static void cameraDialog() {
        MobileBridge.cancel();
        JPanel p = new JPanel(new GridLayout(0, 2, 4, 4));
        String[][] options = {{"Left", "cameraLeft"}, {"Right", "cameraRight"}, {"Up", "cameraUp"},
            {"Down", "cameraDown"}, {"Zoom in", "zoomIn"}, {"Zoom out", "zoomOut"}};
        for (String[] option : options) p.add(button(option[0], () -> send(option[1], 0)));
        dialog("Camera", p);
    }
    static void textDialog() {
        MobileBridge.cancel();
        JsonObject snapshot=JsonParser.parseString(MobileBridge.snapshot()).getAsJsonObject();
        if(!snapshot.has("textSession")) return;
        MobileBridge.setTextFocus(true);
        MobileTextEditor editor=new MobileTextEditor(snapshot.get("textSession").getAsLong());
        JDialog d=dialog("Text entry",editor);
        d.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { editor.close(); }
        });
        editor.focusEditor();
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
    private static void panelsDialog() {
        if(panels!=null&&panels.isDisplayable()){panels.toFront();return;}
        MobileBridge.cancel();MobilePanels content=new MobilePanels();
        JDialog d=new JDialog(host,"Mobile panels — live interfaces",false);panels=d;
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);d.add(content,BorderLayout.CENTER);
        d.add(button("Close panels / return to game",()->{MobileBridge.cancel();d.dispose();}),BorderLayout.SOUTH);
        d.setSize(Math.max(1,host.getWidth()),Math.max(1,host.getHeight()));StandaloneMobileHost.track(d);
        d.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){content.close();panels=null;}});d.setVisible(true);
    }
    static void moreDialog() {
        if(host==null||!host.isDisplayable()) {MobileBridge.setHostOverlayActive(false);return;}
        if(tools!=null&&tools.isDisplayable()) {tools.toFront();return;}
        MobileBridge.cancel();MobileWidgets.Vertical p=new MobileWidgets.Vertical();
        p.row(MobileWidgets.text("Client settings. Gameplay actions stay in the game: tap to act, hold for options, drag the scene to turn the camera."));
        p.row(MobileWidgets.button("Display and input",StandaloneMobileHost::displayDialog));
        p.row(MobileWidgets.button("Accessibility and gestures",()->MobileAccessibilityDialog.open(host)));
        p.row(MobileWidgets.button("Inspect current interface",MobileLauncher::inspectDialog));
        p.row(MobileWidgets.button("Export redacted catalogue",()->MobileDiagnostics.export(host)));
        p.row(MobileWidgets.button("Runtime capabilities",()->MobileDiagnostics.show(host)));
        tools=dialog("Client settings",p);
        tools.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){tools=null;}});
    }
    private static void returnToGame(Runnable command) {
        MobileBridge.cancel();MobileInterfaceManager.closeAll();
        // Window-closed handlers must finish cancellation before the action is queued.
        SwingUtilities.invokeLater(()->{if(host!=null&&host.isDisplayable()){MobileBridge.setHostOverlayActive(false);command.run();if(Class305.aCanvas3869!=null)Class305.aCanvas3869.requestFocusInWindow();}});
    }
    private static void navigationDialog() {
        MobileWidgets.Vertical p=new MobileWidgets.Vertical();int count=0;
        for(UiFrameSnapshot.Node n:MobileBridge.ui().nodes) {
            if(!n.visible||!n.enabled||!"navigation".equals(n.family)||n.actions.isEmpty())continue;
            String name=InterfaceRegistry.component(n.id);if(name.isEmpty())continue;
            p.row(MobileWidgets.button(name,()->returnToGame(()->{
                // Native action rebuilding rechecks widget instance, visibility, permissions and scripts.
                if(n.actions.size()==1)MobileBridge.nodeAction("nodeOp",n.actions.get(0).operation,n.token,n.version);
                else MobileBridge.nodeAction("nodeActions",0,n.token,n.version);
            })));count++;
        }
        if(count==0)p.row(MobileWidgets.text("No currently painted game tabs are exposed. The original game controls remain active; hidden controls are not guessed."));
        p.row(MobileWidgets.button("Alternative accessible panels",MobileLauncher::panelsDialog));
        dialog("Game navigation",p);
    }
}
