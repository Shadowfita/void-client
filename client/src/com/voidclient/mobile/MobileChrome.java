package com.voidclient.mobile;

import java.awt.Rectangle;
import java.util.*;
import javax.swing.SwingUtilities;

/** Small native-canvas controls; geometry is published by the paint which actually draws them. */
public final class MobileChrome {
    public static final long MENU = Long.MIN_VALUE + 1, CANCEL = Long.MIN_VALUE + 2;
    private static volatile Frame frame;
    private static volatile Runnable menuAction;
    private static long generation;
    private static boolean keyDown;
    public static final class Frame {
        public final ViewportState viewport;
        public final Rectangle menu, cancel;
        public final long generation;
        public final boolean move;
        Frame(ViewportState v, Rectangle m, Rectangle c, long g, boolean move) {
            viewport=v;menu=m;cancel=c;generation=g;this.move=move;
        }
        public long at(int x,int y) { return menu.contains(x,y)?MENU:cancel.contains(x,y)?CANCEL:0; }
        public Rectangle bounds(long token) {return token==MENU?new Rectangle(menu):token==CANCEL?new Rectangle(cancel):new Rectangle();}
    }
    public static final class Capture {
        final Frame frame; final long target; final int canvasWidth,canvasHeight;
        boolean cancelled;
        Capture(Frame frame,long target,int w,int h) {this.frame=frame;this.target=target;canvasWidth=w;canvasHeight=h;}
        public void cancel() {cancelled=true;}
    }
    public static boolean enabled() {return MobileConfig.enabled()&&!MobileConfig.browser();}
    public static boolean available() {return enabled()&&!MobileBridge.hostOverlayActive()&&!MobileBridge.suspended()&&!MobileBridge.textFocused();}
    public static synchronized void install(Runnable action) { menuAction=action; keyDown=false; if(action==null)clear(); }
    public static synchronized void clear() {if(frame!=null){frame=null;generation++;MobileAccessibleCanvas.changed();}}
    public static Frame frame() {return available()?frame:null;}
    public static synchronized Frame publish(ViewportState v,boolean armed,boolean move,boolean leftHanded,int scale) {
        if(!available()||v==null) {clear();return null;}
        // Native buffer pixels, not interface-scale pixels. Displayed target remains at least 48 AWT units.
        int size=Math.max(48,48*Math.max(100,Math.min(200,scale))/100);
        int w=Math.min(v.nativeWidth,(int)Math.ceil((double)size*v.nativeWidth/v.width));
        int h=Math.min(v.nativeHeight,(int)Math.ceil((double)size*v.nativeHeight/v.height));
        int gap=Math.max(2,(int)Math.ceil(6.0*v.nativeWidth/v.width));
        int top=Math.min(gap,Math.max(0,v.nativeHeight-h));
        int x=leftHanded?Math.max(0,v.nativeWidth-w-gap):Math.min(gap,Math.max(0,v.nativeWidth-w));
        Rectangle m=new Rectangle(x,top,w,h),c=new Rectangle();
        if(armed&&v.nativeHeight>=2*h+2*gap) c=new Rectangle(x,top+h+gap,w,h);
        Frame old=frame;
        if(old==null||!v.sameGeometry(old.viewport)||v.revision!=old.viewport.revision||!m.equals(old.menu)||!c.equals(old.cancel)||move!=old.move) {
            frame=new Frame(v,m,c,++generation,move);MobileAccessibleCanvas.changed();
        }
        return frame;
    }
    public static Capture press(int x,int y,int canvasWidth,int canvasHeight) {
        Frame f=frame();if(f==null||canvasWidth!=f.viewport.width||canvasHeight!=f.viewport.height)return null;
        long target=f.at(nativeX(f,x),nativeY(f,y));return target==0?null:new Capture(f,target,canvasWidth,canvasHeight);
    }
    public static boolean hit(int x,int y,int width,int height) {return press(x,y,width,height)!=null;}
    private static int nativeX(Frame f,int x) {return (int)Math.floor((double)x*f.viewport.nativeWidth/f.viewport.width);}
    private static int nativeY(Frame f,int y) {return (int)Math.floor((double)y*f.viewport.nativeHeight/f.viewport.height);}
    public static void move(Capture c,int x,int y,int width,int height) {
        if(c==null)return;Frame f=frame();
        if(f==null||f.generation!=c.frame.generation||width!=c.canvasWidth||height!=c.canvasHeight||f.at(nativeX(f,x),nativeY(f,y))!=c.target)c.cancel();
    }
    public static boolean release(Capture c,int x,int y,int width,int height) {
        if(c==null)return false;move(c,x,y,width,height);
        if(c.cancelled)return false;c.cancelled=true;return activate(c.target,c.frame.generation);
    }
    public static boolean activate(long token,long expected) {
        Frame f=frame();if(f==null||f.generation!=expected||f.bounds(token).isEmpty())return false;
        if(token==MENU) {openMenu();return true;}
        if(token==CANCEL) {MobileBridge.cancel();MobileBridge.nodeAction("cancelMode",0,0,0);return true;}
        return false;
    }
    public static void openMenu() {
        if(!available()||menuAction==null)return;
        // Claim input synchronously, before the EDT constructs the dialog. No world click-through window.
        MobileBridge.cancel();MobileBridge.setHostOverlayActive(true);
        SwingUtilities.invokeLater(()->{
            Runnable action=menuAction;
            try { if(action!=null&&enabled())action.run();else MobileBridge.setHostOverlayActive(false); }
            catch(RuntimeException failure) { MobileBridge.setHostOverlayActive(false); throw failure; }
        });
    }
    public static synchronized boolean key(boolean pressed) {
        if(!enabled())return false;
        if(!pressed) {keyDown=false;return true;}
        if(!keyDown) {keyDown=true;openMenu();}return true;
    }
    public static boolean isChrome(long token) {return token==MENU||token==CANCEL;}
    public static boolean current(UiFrameSnapshot.Node n) {Frame f=frame();return f!=null&&f.generation==n.version&&!f.bounds(n.token).isEmpty();}
    public static List<UiFrameSnapshot.Node> nodes() {
        Frame f=frame();if(f==null)return Collections.emptyList();List<UiFrameSnapshot.Node> out=new ArrayList<>(2);
        add(out,f,MENU,"Mobile menu (F10)");if(!f.cancel.isEmpty())add(out,f,CANCEL,f.move?"Cancel item move":"Cancel item or spell target");return out;
    }
    private static void add(List<UiFrameSnapshot.Node> out,Frame f,long token,String label) {
        Rectangle r=f.bounds(token);ViewportState v=f.viewport;
        UiFrameSnapshot.Bounds b=new UiFrameSnapshot.Bounds((int)Math.floor((double)r.x*v.logicalWidth/v.nativeWidth),(int)Math.floor((double)r.y*v.logicalHeight/v.nativeHeight),
            (int)Math.ceil((double)r.width*v.logicalWidth/v.nativeWidth),(int)Math.ceil((double)r.height*v.logicalHeight/v.nativeHeight));
        out.add(new UiFrameSnapshot.Node(token,0,f.generation,-1,-1,0,0,-1,0,Integer.MAX_VALUE,b,b,UiFrameSnapshot.Role.BUTTON,label,"Mobile controls","host","",true,false,true,Collections.singletonList(new UiFrameSnapshot.Action(0,label))));
    }
    private MobileChrome() {}
}
