from pathlib import Path
import re


def read(path):
    return Path(path).read_text()


def write(path, text):
    Path(path).write_text(text)


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)

# ---------------------------------------------------------------------------
# 1. Native 634 interface layout: do not trust a size-only cache. Interface
#    scripts can reflow attached groups after interaction without changing the
#    outer canvas dimensions. Re-establish logical root layout before render.
# ---------------------------------------------------------------------------
p = 'client/src/Applet_Sub1.java'
s = read(p)
s = replace_once(
    s,
    'import java.awt.event.WindowListener;\n',
    'import java.awt.event.WindowListener;\nimport java.awt.image.BufferedImage;\nimport java.awt.image.DataBufferInt;\n',
    'Applet imports')
s = replace_once(
    s,
    '    private static int lastInterfaceLayoutWidth = -1;\n    private static int lastInterfaceLayoutHeight = -1;\n',
    '    private static int lastInterfaceLayoutWidth = -1;\n    private static int lastInterfaceLayoutHeight = -1;\n    private static int lastInterfaceLayoutFrame = -1;\n',
    'layout frame field')
old = '''    private static void ensureInterfaceLayout() {
        if (r.anInt9721 == -1) {
            return;
        }
        if (lastInterfaceLayoutWidth == interfaceLogicalWidth && lastInterfaceLayoutHeight == interfaceLogicalHeight) {
            return;
        }
        Class239_Sub3.method1728(interfaceLogicalHeight, -1, r.anInt9721, true, interfaceLogicalWidth);
        lastInterfaceLayoutWidth = interfaceLogicalWidth;
        lastInterfaceLayoutHeight = interfaceLogicalHeight;
    }
'''
new = '''    private static void ensureInterfaceLayout(boolean force) {
        if (r.anInt9721 == -1) {
            return;
        }

        // Canvas dimensions alone are not a sufficient cache key. Native 634
        // interface scripts can reopen/reflow attached groups (settings/audio,
        // chat modes, tab content, etc.) while the outer window size remains
        // unchanged. If that happens after our logical UI layout pass, right-
        // and bottom-anchored roots can retain a physical-space layout until a
        // window resize. Always re-establish the logical root immediately
        // before drawing; input traversal only needs one relayout per frame.
        int frame = Class367_Sub11.anInt7396;
        if (!force
                && lastInterfaceLayoutFrame == frame
                && lastInterfaceLayoutWidth == interfaceLogicalWidth
                && lastInterfaceLayoutHeight == interfaceLogicalHeight) {
            return;
        }

        Class239_Sub3.method1728(interfaceLogicalHeight, -1, r.anInt9721, true, interfaceLogicalWidth);
        lastInterfaceLayoutWidth = interfaceLogicalWidth;
        lastInterfaceLayoutHeight = interfaceLogicalHeight;
        lastInterfaceLayoutFrame = frame;
    }
'''
s = replace_once(s, old, new, 'ensureInterfaceLayout')
s = replace_once(s, '        ensureInterfaceLayout();\n        interfaceRenderScaleActive = true;\n',
                 '        ensureInterfaceLayout(true);\n        interfaceRenderScaleActive = true;\n', 'render layout call')
s = replace_once(s, '        ensureInterfaceLayout();\n        interfaceInputScaleActive = true;\n',
                 '        ensureInterfaceLayout(false);\n        interfaceInputScaleActive = true;\n', 'input layout call')
s = replace_once(s, '        lastInterfaceLayoutWidth = -1;\n        lastInterfaceLayoutHeight = -1;\n',
                 '        lastInterfaceLayoutWidth = -1;\n        lastInterfaceLayoutHeight = -1;\n        lastInterfaceLayoutFrame = -1;\n', 'scale cache reset')

# Add a bridge which renders RuneLite Java2D overlays to an ARGB buffer and
# hands the pixels back to the active hardware renderer while its GL context is
# current on the client thread.
anchor = '''    private void fireRuneLiteClientLoop() {
        try {
            if (RuneLite.getInjector() != null) {
                RuneLite.getInjector().getInstance(Callbacks.class).clientMainLoop();
            }
        } catch (Throwable ignored) {
        }
    }
'''
bridge = anchor + '''
    static void renderRuneLiteHardwareOverlay(boolean aboveWidgets) {
        try {
            if (RuneLite.getInjector() == null || Class348_Sub8.aHa6654 == null || Class305.aCanvas3869 == null) {
                return;
            }

            int width = Math.max(1, Class305.aCanvas3869.getWidth());
            int height = Math.max(1, Class305.aCanvas3869.getHeight());
            BufferedImage image = RuneLite.getInjector().getInstance(Callbacks.class)
                    .renderHardwareOverlay(width, height, aboveWidgets);
            if (image == null || !(image.getRaster().getDataBuffer() instanceof DataBufferInt)) {
                return;
            }

            int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            Class348_Sub8.aHa6654.drawRuneLiteHardwareOverlay(pixels, width, height);
        } catch (Throwable throwable) {
            if (Loader.trace) {
                throwable.printStackTrace();
            }
        }
    }
'''
s = replace_once(s, anchor, bridge, 'hardware overlay bridge')
write(p, s)

# ---------------------------------------------------------------------------
# 2. RuneLite callback API gets a hardware-overlay render method.
# ---------------------------------------------------------------------------
p = 'client/src/net/runelite/api/hooks/Callbacks.java'
s = read(p)
s = replace_once(s, 'import java.awt.event.MouseWheelEvent;\n',
                 'import java.awt.event.MouseWheelEvent;\nimport java.awt.image.BufferedImage;\n', 'Callbacks BufferedImage import')
anchor = '''\tvoid drawAboveOverheads();
'''
s = replace_once(s, anchor, anchor + '''
\t/**
\t * Render RuneLite overlays into a transparent framebuffer for composition by
\t * the native 634 hardware renderer.
\t */
\tBufferedImage renderHardwareOverlay(int width, int height, boolean aboveWidgets);
''', 'Callbacks hardware overlay method')
write(p, s)

# ---------------------------------------------------------------------------
# 3. Hooks renders standard RuneLite overlays into a reusable ARGB image.
# ---------------------------------------------------------------------------
p = 'client/src/net/runelite/client/callback/Hooks.java'
s = read(p)
s = replace_once(s, '    private static Graphics2D lastGraphics;\n',
                 '    private static Graphics2D lastGraphics;\n    private BufferedImage hardwareOverlayBuffer;\n', 'Hooks overlay buffer field')
anchor = '''    public void drawInterface(int interfaceId, List<WidgetItem> widgetItems)
'''
method = '''    @Override
    public BufferedImage renderHardwareOverlay(int width, int height, boolean aboveWidgets)
    {
        if (width <= 0 || height <= 0)
        {
            return null;
        }

        if (hardwareOverlayBuffer == null
                || hardwareOverlayBuffer.getWidth() != width
                || hardwareOverlayBuffer.getHeight() != height)
        {
            hardwareOverlayBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D graphics2d = hardwareOverlayBuffer.createGraphics();
        try
        {
            graphics2d.setComposite(AlphaComposite.Clear);
            graphics2d.fillRect(0, 0, width, height);
            graphics2d.setComposite(AlphaComposite.SrcOver);

            if (aboveWidgets)
            {
                renderer.renderOverlayLayer(graphics2d, OverlayLayer.ABOVE_WIDGETS);
                renderer.renderOverlayLayer(graphics2d, OverlayLayer.ALWAYS_ON_TOP);
                notifier.processFlash(graphics2d);
                clientUi.paintOverlays(graphics2d);
            }
            else
            {
                renderer.renderOverlayLayer(graphics2d, OverlayLayer.ABOVE_SCENE);
                renderer.renderOverlayLayer(graphics2d, OverlayLayer.UNDER_WIDGETS);
            }
        }
        catch (Exception ex)
        {
            log.warn("Error during hardware overlay rendering", ex);
        }
        finally
        {
            graphics2d.dispose();
        }

        return hardwareOverlayBuffer;
    }

'''
s = replace_once(s, anchor, method + anchor, 'Hooks hardware overlay renderer')
write(p, s)

# ---------------------------------------------------------------------------
# 4. Renderer base exposes an optional native overlay composition entry point.
# ---------------------------------------------------------------------------
p = 'client/src/ha.java'
s = read(p)
anchor = '''    void refreshNativeInterfaceScaling() {
        /* Unsupported renderer. */
    }
'''
s = replace_once(s, anchor, anchor + '''
    void drawRuneLiteHardwareOverlay(int[] pixels, int width, int height) {
        /* Unsupported renderer. */
    }
''', 'ha hardware overlay hook')
write(p, s)

# ---------------------------------------------------------------------------
# 5. OpenGL sprite supports updating an existing ARGB overlay texture.
# ---------------------------------------------------------------------------
p = 'client/src/Class105_Sub2.java'
s = read(p)
anchor = '''    final void method984(int[] is) {
'''
update = '''    final void updateRuneLiteOverlayPixels(int[] pixels, int width, int height) {
        if (pixels == null || width != aClass258_Sub3_Sub1_8434.anInt9940 || height != aClass258_Sub3_Sub1_8434.anInt9939) {
            throw new IllegalArgumentException("RuneLite overlay dimensions changed");
        }
        // Match the constructor's top-left Java image orientation by flipping
        // rows during the GL texture sub-image upload.
        aClass258_Sub3_Sub1_8434.method1964(true, height, 0, pixels, 0, (byte) -73, width, 0, width);
    }

'''
s = replace_once(s, anchor, update + anchor, 'Class105_Sub2 update method')
write(p, s)

# ---------------------------------------------------------------------------
# 6. JAGGL renderer uploads/draws the reusable RuneLite ARGB overlay texture.
#    The top overlay is composited immediately before swapBuffers().
# ---------------------------------------------------------------------------
p = 'client/src/ha_Sub2.java'
s = read(p)
# Add cache fields next to the GL object.
s = replace_once(s, '    private OpenGL anOpenGL7664;\n',
                 '    private OpenGL anOpenGL7664;\n    private Class105_Sub2 runeLiteOverlaySprite;\n    private int runeLiteOverlayWidth = -1;\n    private int runeLiteOverlayHeight = -1;\n', 'ha_Sub2 overlay fields')
# Add renderer method beside native scale overrides.
anchor = '''    @Override
    final void refreshNativeInterfaceScaling() {
        method3745((byte) 127);
    }
'''
override = anchor + '''
    @Override
    final void drawRuneLiteHardwareOverlay(int[] pixels, int width, int height) {
        if (pixels == null || width <= 0 || height <= 0) {
            return;
        }

        if (runeLiteOverlaySprite == null || runeLiteOverlayWidth != width || runeLiteOverlayHeight != height) {
            runeLiteOverlaySprite = new Class105_Sub2(this, width, height, pixels, 0, width);
            runeLiteOverlayWidth = width;
            runeLiteOverlayHeight = height;
        } else {
            runeLiteOverlaySprite.updateRuneLiteOverlayPixels(pixels, width, height);
        }

        // Overlay pixels are already in physical canvas coordinates. The native
        // UI projection is not active at either composition point.
        KA(0, 0, width, height);
        runeLiteOverlaySprite.method972(0, 0, width, height);
    }
'''
s = replace_once(s, anchor, override, 'ha_Sub2 overlay renderer')
old = '''    final void method3626(int i, int i_457_) throws Exception_Sub1 {
        try {
            anOpenGL7664.swapBuffers();
'''
new = '''    final void method3626(int i, int i_457_) throws Exception_Sub1 {
        try {
            // ABOVE_WIDGETS / ALWAYS_ON_TOP must be part of the actual JAGGL
            // backbuffer; Canvas.getGraphics() is not a valid overlay path for
            // this hardware renderer.
            Applet_Sub1.renderRuneLiteHardwareOverlay(true);
            anOpenGL7664.swapBuffers();
'''
s = replace_once(s, old, new, 'ha_Sub2 pre-swap overlay call')
write(p, s)

# ---------------------------------------------------------------------------
# 7. Scene/under-widget overlays are composited before the native 634 UI.
# ---------------------------------------------------------------------------
p = 'client/src/Class302.java'
s = read(p)
old = '''            if (r.anInt9721 != -1) {
                Class348_Sub38.anInt7008 = 0;
                Class88.method842(false);
            }
'''
new = '''            // RuneLite scene overlays (NPC names, tile indicators, object/NPC
            // markers, etc.) must be drawn into the JAGGL backbuffer before the
            // native RuneScape interface so chat/tabs remain above them.
            Applet_Sub1.renderRuneLiteHardwareOverlay(false);
            if (r.anInt9721 != -1) {
                Class348_Sub38.anInt7008 = 0;
                Class88.method842(false);
            }
'''
s = replace_once(s, old, new, 'Class302 pre-widget overlay call')
write(p, s)

# ---------------------------------------------------------------------------
# 8. Remove FlatLaf properties unsupported by 3.2.5; these currently throw on
#    every navigation-button creation and obscure useful runtime diagnostics.
# ---------------------------------------------------------------------------
p = 'client/src/net/runelite/client/ui/laf/RuneLiteLAF.properties'
s = read(p)
s = s.replace('rolloverIconAlpha:0.72; ', '')
write(p, s)

print('native UI render-pipeline corrections applied')
