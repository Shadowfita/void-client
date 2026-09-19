import com.voidclient.mobile.*;
import java.awt.Rectangle;

/** Draws directly in the active game renderer, after interface scaling has been restored. */
final class MobileCanvasMenu {
    private static MobileChrome.Frame previous;
    static void paint() {
        ha renderer=Class348_Sub8.aHa6654;
        UiFrameSnapshot ui=MobileBridge.ui();ViewportState v=ui.viewport;
        if(renderer==null||v==null||v.nativeWidth!=Class321.anInt4017||v.nativeHeight!=Class348_Sub42_Sub8_Sub2.anInt10432) {MobileChrome.clear();return;}
        MobileActionOverlay.paint(renderer,v);
        MobileChrome.Frame f=MobileChrome.publish(v,ui.targetArmed||ui.moveArmed,ui.moveArmed,AccessibilityPreferences.current().leftHanded,StandaloneMobileHost.controlScale());
        if((previous==null)!=(f==null)||(previous!=null&&f!=null&&previous.generation!=f.generation))Class49.aBoolean4726=true;
        previous=f;
        if(f==null)return;
        int[] clip=new int[4];renderer.K(clip);
        try {
            renderer.KA(0,0,v.nativeWidth,v.nativeHeight);
            draw(renderer,f.menu,false);if(!f.cancel.isEmpty())draw(renderer,f.cancel,true);
        } finally {renderer.KA(clip[0],clip[1],clip[2],clip[3]);}
    }
    static void draw(ha renderer,Rectangle r,boolean cancel) {
        renderer.aa(r.x,r.y,r.width,r.height,cancel?0xff613028:0xff202c34,0);
        renderer.method3628(r.x,r.y,r.width,r.height,0xffd1b880,0);
        int inset=Math.max(5,r.width/4),line=Math.max(2,r.height/20);
        if(cancel) {
            // An unambiguous X, raster primitives only; no cache font/sprite dependency.
            int n=Math.max(1,Math.min(r.width,r.height)/2);
            for(int i=0;i<n;i++) {int x=r.x+(r.width-n)/2+i,y=r.y+(r.height-n)/2+i;renderer.aa(x,y,line,line,0xfffaf4df,0);renderer.aa(x,r.y+(r.height+n)/2-i-line,line,line,0xfffaf4df,0);}
        } else for(int i=1;i<=3;i++)renderer.aa(r.x+inset,r.y+r.height*i/4-line/2,Math.max(1,r.width-2*inset),line,0xfffaf4df,0);
    }
    private MobileCanvasMenu() {}
}
