from pathlib import Path
import re

p = Path('client/src/net/runelite/client/ui/ClientUI.java')
s = p.read_text()

def once(old, new):
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit('expected exactly one match, got %d: %r' % (count, old[:80]))
    s = s.replace(old, new, 1)

once('import net.runelite.client.config.ConfigManager;\n',
     'import net.runelite.client.callback.ClientThread;\nimport net.runelite.client.config.ConfigManager;\n')

once('\tprivate final ConfigManager configManager;\n\t//private final Provider<ClientThread> clientThreadProvider;\n',
     '\tprivate final ConfigManager configManager;\n\tprivate final ClientThread clientThread;\n')

once('\t\tConfigManager configManager,\n\t\t//Provider<ClientThread> clientThreadProvider,\n\t\tEventBus eventBus,\n',
     '\t\tConfigManager configManager,\n\t\tClientThread clientThread,\n\t\tEventBus eventBus,\n')

once('\t\tthis.configManager = configManager;\n\t\t//this.clientThreadProvider = clientThreadProvider;\n\t\tthis.eventBus = eventBus;\n',
     '\t\tthis.configManager = configManager;\n\t\tthis.clientThread = clientThread;\n\t\tthis.eventBus = eventBus;\n')

pattern = re.compile(r'\tprivate void finishClientResize\(\)\n\t\{.*?\n\tprivate void togglePluginPanel\(\)', re.S)
replacement = '''\tprivate void finishClientResize()
\t{
\t\t// AWT/Swing owns frame and Canvas hierarchy changes. JAGGL/OpenGL does not:
\t\t// renderer state must only be touched on the 634 client thread where its
\t\t// GL context is current.
\t\tframe.refreshNativePeer();
\t\trefreshSwingClient(false);
\t\trequestRendererResize();

\t\tSwingUtilities.invokeLater(() ->
\t\t{
\t\t\tframe.refreshNativePeer();
\t\t\trefreshSwingClient(false);
\t\t\trequestRendererResize();

\t\t\tTimer timer = new Timer(75, event ->
\t\t\t{
\t\t\t\tframe.refreshNativePeer();
\t\t\t\trefreshSwingClient(false);
\t\t\t\trequestRendererResize();
\t\t\t});
\t\t\ttimer.setRepeats(false);
\t\t\ttimer.start();
\t\t});
\t}

\tprivate void repaintSidebarSideSwitch()
\t{
\t\tcontainer.revalidate();
\t\tcontainer.repaint();
\t\tclientPanel.revalidate();
\t\tclientPanel.repaint();
\t\tframe.revalidate();
\t\tframe.repaint();

\t\tSwingUtilities.invokeLater(() ->
\t\t{
\t\t\tcontainer.repaint();
\t\t\tclientPanel.repaint();
\t\t\tframe.repaint();
\t\t\trefreshSwingClient(false);
\t\t\trequestRendererResize();
\t\t});
\t}

\tprivate void redrawClient()
\t{
\t\tif (!SwingUtilities.isEventDispatchThread())
\t\t{
\t\t\tSwingUtilities.invokeLater(this::redrawClient);
\t\t\treturn;
\t\t}

\t\trefreshSwingClient(true);
\t\trequestRendererResize();
\t}

\tprivate void redrawClientNow()
\t{
\t\tif (!SwingUtilities.isEventDispatchThread())
\t\t{
\t\t\tSwingUtilities.invokeLater(this::redrawClientNow);
\t\t\treturn;
\t\t}

\t\trefreshSwingClient(false);
\t\trequestRendererResize();
\t}

\tprivate void requestRendererResize()
\t{
\t\tif (!(client instanceof GameClient))
\t\t{
\t\t\treturn;
\t\t}

\t\tfinal GameClient gameClient = (GameClient) client;
\t\tclientThread.invokeLater(() -> gameClient.invalidateStretching(true));
\t}

\tprivate void refreshSwingClient(boolean requestFocus)
\t{
\t\tassert SwingUtilities.isEventDispatchThread() : "Swing client refresh must run on EDT";
\t\ttry
\t\t{
\t\t\tif (client instanceof GameClient)
\t\t\t{
\t\t\t\tCanvas canvas = ((GameClient) client).getCanvas();
\t\t\t\tif (canvas != null)
\t\t\t\t{
\t\t\t\t\tcanvas.invalidate();
\t\t\t\t\tcanvas.validate();
\t\t\t\t\tcanvas.repaint();

\t\t\t\t\tContainer parent = canvas.getParent();
\t\t\t\t\twhile (parent != null)
\t\t\t\t\t{
\t\t\t\t\t\tparent.invalidate();
\t\t\t\t\t\tparent.validate();
\t\t\t\t\t\tparent.doLayout();
\t\t\t\t\t\tparent.repaint();
\t\t\t\t\t\tparent = parent.getParent();
\t\t\t\t\t}
\t\t\t\t}
\t\t\t}

\t\t\tclient.invalidate();
\t\t\tclient.validate();
\t\t\tclient.repaint();
\t\t\tframe.invalidate();
\t\t\tframe.validate();
\t\t\tframe.repaint();
\t\t\tif (requestFocus)
\t\t\t{
\t\t\t\tgiveClientFocus();
\t\t\t}
\t\t\tToolkit.getDefaultToolkit().sync();
\t\t}
\t\tcatch (RuntimeException ex)
\t\t{
\t\t\tlog.debug("Unable to refresh Swing client after sidebar resize", ex);
\t\t}
\t}

\tprivate void togglePluginPanel()'''

s2, n = pattern.subn(lambda m: replacement, s, count=1)
if n != 1:
    raise SystemExit('failed to replace resize/redraw block: %d' % n)
p.write_text(s2)

# Static guard: the EDT-only helper must never call renderer invalidation.
block = s2.split('private void refreshSwingClient(boolean requestFocus)', 1)[1].split('private void togglePluginPanel()', 1)[0]
if 'invalidateStretching' in block:
    raise SystemExit('renderer invalidation still present in EDT Swing helper')
