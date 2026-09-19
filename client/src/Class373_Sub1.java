/* Class373_Sub1 - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */

import java.awt.*;
import java.awt.event.*;

final class Class373_Sub1 extends Class373 implements MouseListener, MouseMotionListener, MouseWheelListener {
    private int anInt7416;
    private int anInt7417;
    private Class262 aClass262_7418 = new Class262();
    private int anInt7419;
    private Class262 aClass262_7420 = new Class262();
    private int anInt7421;
    private int anInt7422;
    private int anInt7423;
    private final boolean aBoolean7424;
    private Component aComponent7425;
    private boolean mobilePointerDown;
    private com.voidclient.mobile.MobileChrome.Capture chromeCapture;
    private int chromeSuppressedButtons;
    private int menuSuppressedButtons;

    final boolean method3588(int i) {
        int i_0_ = -59 % ((i - -38) / 48);
        return (anInt7419 & 0x2) != 0;
    }

    private final void method3598(int i, int i_1_, int i_2_, int i_3_, boolean bool) {
        Class348_Sub45_Sub1 class348_sub45_sub1 = new Class348_Sub45_Sub1();
        class348_sub45_sub1.anInt9725 = i_3_;
        class348_sub45_sub1.anInt9728 = i_1_;
        class348_sub45_sub1.anInt9729 = i;
        class348_sub45_sub1.aLong9726 = Class62.method599(-115);
        class348_sub45_sub1.anInt9727 = i_2_;
        if (bool) mouseDragged(null);
        aClass262_7420.method1999(class348_sub45_sub1, -20180);
    }

    public final synchronized void mouseReleased(MouseEvent mouseevent) {
        int menuBit=mouseevent.getButton()>0&&mouseevent.getButton()<4?1<<mouseevent.getButton():0;
        boolean menuRelease=(menuSuppressedButtons&menuBit)!=0;menuSuppressedButtons&=~menuBit;
        if(com.voidclient.mobile.CanvasActionMenu.active()) {
            if(mouseevent.getButton()==MouseEvent.BUTTON1)com.voidclient.mobile.CanvasActionMenu.pointer("menuUp",1,mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight());
            mobilePointerDown=false;mouseevent.consume();return;
        }
        if(menuRelease){mouseevent.consume();return;}
        int chromeBit=mouseevent.getButton()>0&&mouseevent.getButton()<4?1<<mouseevent.getButton():0;
        boolean chromeSuppressed=(chromeSuppressedButtons&chromeBit)!=0;chromeSuppressedButtons&=~chromeBit;
        if (chromeCapture != null && mouseevent.getButton() == MouseEvent.BUTTON1) {
            com.voidclient.mobile.MobileChrome.Capture capture=chromeCapture;chromeCapture=null;
            com.voidclient.mobile.MobileChrome.release(capture,mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight());mouseevent.consume();return;
        }
        if(chromeSuppressed||chromeCapture!=null){mouseevent.consume();return;}
        if(com.voidclient.mobile.MobileChrome.hit(mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight())){com.voidclient.mobile.MobileBridge.cancel();mobilePointerDown=false;mouseevent.consume();return;}
        if (MobileRuntime.blocksMouse()) { mobilePointerDown = false; mouseevent.consume(); return; }
        if (mobilePointerDown && mouseevent.getButton() == MouseEvent.BUTTON1) {
            Point origin = MobileLauncher.canvasOrigin(aComponent7425);
            mobilePointerDown = false;
            com.voidclient.mobile.MobileBridge.pointer("up", 1, origin.x + mouseevent.getX(), origin.y + mouseevent.getY(), com.voidclient.mobile.MobileBridge.revision());
            mouseevent.consume(); return;
        }
        int i = method3600(mouseevent, -75);
        int x = Applet_Sub1.scaleMouseX(mouseevent.getX());
        int y = Applet_Sub1.scaleMouseY(mouseevent.getY());
        if ((i & anInt7422) == 0) i = anInt7422;
        if (0 != (0x1 & i)) method3598(x, 3, mouseevent.getClickCount(), y, false);
        if ((i & 0x4) != 0) method3598(x, 5, mouseevent.getClickCount(), y, false);
        if ((0x2 & i) != 0) method3598(x, 4, mouseevent.getClickCount(), y, false);
        anInt7422 &= ~i;
        if (mouseevent.isPopupTrigger()) mouseevent.consume();
    }

    final int method3594(byte i) {
        if (i < 69) method3598(92, 34, 59, 2, false);
        return Applet_Sub1.scaleInterfaceInputY(anInt7416);
    }

    private final void method3599(int i, int i_4_, int i_5_) {
        if (i_4_ == -1) {
            int x = Applet_Sub1.scaleMouseX(i);
            int y = Applet_Sub1.scaleMouseY(i_5_);
            anInt7421 = y;
            anInt7423 = x;
            if (aBoolean7424) method3598(x, -1, 0, y, false);
        }
    }

    final boolean method3590(byte i) {
        if (i <= 112) return false;
        return (anInt7419 & 0x4) != 0;
    }

    public final synchronized void mousePressed(MouseEvent mouseevent) {
        if(com.voidclient.mobile.CanvasActionMenu.active()) {
            if(mouseevent.getButton()>0&&mouseevent.getButton()<4)menuSuppressedButtons|=1<<mouseevent.getButton();
            if(mouseevent.getButton()==MouseEvent.BUTTON1)com.voidclient.mobile.CanvasActionMenu.pointer("menuDown",1,mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight());
            else com.voidclient.mobile.CanvasActionMenu.keyboard(java.awt.event.KeyEvent.VK_ESCAPE);
            mobilePointerDown=false;mouseevent.consume();return;
        }
        if(chromeCapture!=null||com.voidclient.mobile.MobileChrome.hit(mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight())) {
            if(mouseevent.getButton()>0&&mouseevent.getButton()<4)chromeSuppressedButtons|=1<<mouseevent.getButton();
            if(mouseevent.getButton()!=MouseEvent.BUTTON1){if(chromeCapture!=null)chromeCapture.cancel();mouseevent.consume();return;}
        }
        if(mouseevent.getButton()==MouseEvent.BUTTON1) {
            chromeCapture=com.voidclient.mobile.MobileChrome.press(mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight());
            if(chromeCapture!=null){mobilePointerDown=false;mouseevent.consume();return;}
        }
        if (MobileRuntime.blocksMouse()) { mouseevent.consume(); return; }
        if (MobileRuntime.touchPointerRequired() && mouseevent.getButton() == MouseEvent.BUTTON1) {
            Point origin = MobileLauncher.canvasOrigin(aComponent7425);
            mobilePointerDown = true;
            com.voidclient.mobile.MobileBridge.pointer("down", 1, origin.x + mouseevent.getX(), origin.y + mouseevent.getY(), com.voidclient.mobile.MobileBridge.revision());
            mouseevent.consume(); return;
        }
        int i = method3600(mouseevent, -90);
        int x = Applet_Sub1.scaleMouseX(mouseevent.getX());
        int y = Applet_Sub1.scaleMouseY(mouseevent.getY());
        if (1 == i) method3598(x, 0, mouseevent.getClickCount(), y, false);
        else if (i == 4) method3598(x, 2, mouseevent.getClickCount(), y, false);
        else if (i == 2) method3598(x, 1, mouseevent.getClickCount(), y, false);
        anInt7422 |= i;
        if (mouseevent.isPopupTrigger()) mouseevent.consume();
    }

    final int method3597(boolean bool) {
        if (bool != true) return 27;
        return Applet_Sub1.scaleInterfaceInputX(anInt7417);
    }

    private final int method3600(MouseEvent mouseevent, int i) {
        if (mouseevent.getButton() == 1) {
            if (mouseevent.isMetaDown()) return 4;
            return 1;
        }
        if (mouseevent.getButton() == 2) return 2;
        int i_6_ = -27 % ((57 - i) / 63);
        if (mouseevent.getButton() == 3) return 4;
        return 0;
    }

    public final synchronized void mouseEntered(MouseEvent mouseevent) {
        if (MobileRuntime.blocksMouse()) { mouseevent.consume(); return; }
        method3599(mouseevent.getX(), -1, mouseevent.getY());
    }

    public final synchronized void mouseClicked(MouseEvent mouseevent) {
        if (mouseevent.isPopupTrigger()) mouseevent.consume();
    }

    final boolean method3595(int i) {
        if (i >= -67) mouseMoved(null);
        return (anInt7419 & 0x1) != 0;
    }

    final Class348_Sub45 method3596(int i) {
        if (i != 0) mouseReleased(null);
        return (Class348_Sub45) aClass262_7418.method1997(8);
    }

    public final synchronized void mouseMoved(MouseEvent mouseevent) {
        if(com.voidclient.mobile.MobileChrome.hit(mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight())){mouseevent.consume();return;}
        if (MobileRuntime.blocksMouse()) { mouseevent.consume(); return; }
        method3599(mouseevent.getX(), -1, mouseevent.getY());
    }

    final synchronized void mobileClick(int x, int y, boolean context) {
        anInt7423 = x; anInt7421 = y;
        method3598(x, -1, 0, y, false);
        method3598(x, context ? 2 : 0, 1, y, false);
        method3598(x, context ? 5 : 3, 1, y, false);
    }

    final synchronized void mobileCancel() {
        anInt7422 = anInt7419 = 0; mobilePointerDown = false;
        if(chromeCapture!=null)chromeCapture.cancel();
        if (aClass262_7420 != null) aClass262_7420.method1996(127);
        if (aClass262_7418 != null) aClass262_7418.method1996(127);
        if (Class318_Sub1_Sub3.aClass262_8744 != null) Class318_Sub1_Sub3.aClass262_8744.method1996(127);
    }

    private final void method3601(int i) {
        if (com.voidclient.mobile.MobileConfig.enabled()) com.voidclient.mobile.MobileBridge.cancel();
        if (null != aComponent7425) {
            int i_7_ = 11 % ((i - -21) / 55);
            aComponent7425.removeMouseWheelListener(this);
            aComponent7425.removeMouseMotionListener(this);
            aComponent7425.removeMouseListener(this);
            aComponent7425 = null;
            anInt7423 = anInt7421 = anInt7422 = 0;
            anInt7417 = anInt7416 = anInt7419 = 0;
            aClass262_7418 = null;
            aClass262_7420 = null;
        }
    }

    final synchronized void method3589(int i) {
        MobileRuntime.tick(this, aComponent7425);
        anInt7416 = anInt7421;
        anInt7417 = anInt7423;
        anInt7419 = anInt7422;
        if (i == 0) {
            Class262 class262 = aClass262_7418;
            aClass262_7418 = aClass262_7420;
            aClass262_7420 = class262;
            aClass262_7420.method1996(127);
        }
    }

    public final synchronized void mouseWheelMoved(MouseWheelEvent mousewheelevent) {
        if(com.voidclient.mobile.CanvasActionMenu.active()) {
            com.voidclient.mobile.CanvasActionMenu.pointer("menuWheel",Math.max(-120,Math.min(120,mousewheelevent.getWheelRotation())),mousewheelevent.getX(),mousewheelevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight());
            mousewheelevent.consume();return;
        }
        if(chromeCapture!=null||com.voidclient.mobile.MobileChrome.hit(mousewheelevent.getX(),mousewheelevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight())){mousewheelevent.consume();return;}
        if (MobileRuntime.blocksMouse()) { mousewheelevent.consume(); return; }
        int i = Applet_Sub1.scaleMouseX(mousewheelevent.getX());
        int i_8_ = Applet_Sub1.scaleMouseY(mousewheelevent.getY());
        int i_9_ = mousewheelevent.getWheelRotation();
        method3598(i, 6, i_9_, i_8_, false);
        mousewheelevent.consume();
    }

    public final synchronized void mouseExited(MouseEvent mouseevent) {
        if (MobileRuntime.blocksMouse()) { mouseevent.consume(); return; }
        method3599(mouseevent.getX(), -1, mouseevent.getY());
    }

    public final synchronized void mouseDragged(MouseEvent mouseevent) {
        if(com.voidclient.mobile.CanvasActionMenu.active()) {
            if((mouseevent.getModifiersEx()&MouseEvent.BUTTON1_DOWN_MASK)!=0)com.voidclient.mobile.CanvasActionMenu.pointer("menuMove",1,mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight());
            mouseevent.consume();return;
        }
        if(chromeCapture!=null){com.voidclient.mobile.MobileChrome.move(chromeCapture,mouseevent.getX(),mouseevent.getY(),aComponent7425.getWidth(),aComponent7425.getHeight());mouseevent.consume();return;}
        if (MobileRuntime.blocksMouse()) { mouseevent.consume(); return; }
        if (mobilePointerDown && (mouseevent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            Point origin = MobileLauncher.canvasOrigin(aComponent7425);
            com.voidclient.mobile.MobileBridge.pointer("move", 1, origin.x + mouseevent.getX(), origin.y + mouseevent.getY(), com.voidclient.mobile.MobileBridge.revision());
            mouseevent.consume(); return;
        }
        method3599(mouseevent.getX(), -1, mouseevent.getY());
    }

    final void method3592(int i) {
        if (i == 0) method3601(46);
    }

    private final void method3602(int i, Component component) {
        method3601(i ^ 0x6e);
        aComponent7425 = component;
        if (i != 0) aComponent7425 = null;
        aComponent7425.addMouseListener(this);
        aComponent7425.addMouseMotionListener(this);
        aComponent7425.addMouseWheelListener(this);
    }

    Class373_Sub1(Component component, boolean bool) {
        method3602(0, component);
        aBoolean7424 = bool;
    }
}
