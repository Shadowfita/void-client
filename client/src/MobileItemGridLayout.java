import com.voidclient.mobile.*;
import java.util.*;

/** Guarded native item-grid reflow. Script/authored positions are never rewritten. Unknown structures fall back. */
final class MobileItemGridLayout {
    private static final class Geometry { final int x,y,width,height;Geometry(int x,int y,int width,int height){this.x=x;this.y=y;this.width=width;this.height=height;} }
    private static final class State { int authoredW,authoredH,appliedW,appliedH;final List<java.lang.ref.WeakReference<Class46>> children=new ArrayList<>(); }
    private static final WeakHashMap<Class46,State> states=new WeakHashMap<>();
    private static final WeakHashMap<Class46,Geometry> geometry=new WeakHashMap<>();
    static void restoreContent(Class46 parent) {
        State s=states.get(parent);if(s==null)return;
        if(parent.anInt698==s.appliedW)parent.anInt698=s.authoredW;else s.authoredW=parent.anInt698;
        if(parent.anInt791==s.appliedH)parent.anInt791=s.authoredH;else s.authoredH=parent.anInt791;
        s.appliedW=parent.anInt698;s.appliedH=parent.anInt791;
    }
    static boolean prepare(Class46 parent) {
        State old=states.get(parent);
        if(old!=null) {
            for(java.lang.ref.WeakReference<Class46> reference:old.children){Class46 c=reference.get();if(c!=null)geometry.remove(c);}old.children.clear();
            if(parent.anInt698!=old.appliedW)old.authoredW=parent.anInt698;
            if(parent.anInt791!=old.appliedH)old.authoredH=parent.anInt791;
            parent.anInt698=old.authoredW;parent.anInt791=old.authoredH;old.appliedW=old.authoredW;old.appliedH=old.authoredH;
        }
        if(!MobileConfig.enabled()||!Boolean.parseBoolean(System.getProperty("void.mobile.nativeGrids","true"))
            ||parent.anInt774!=0||parent.anInt709<72||parent.aClass46Array798==null||!InterfaceRegistry.inventory(parent.anInt830))return false;
        List<Class46> items=new ArrayList<>();
        for(Class46 child:parent.aClass46Array798)if(child!=null&&!child.aBoolean813) {
            if(child.anInt830!=parent.anInt830||child.anInt704<0||child.anInt774!=5||child.anInt765!=0
                ||child.aClass46Array798!=null||child.anInt842<8||child.anInt728<8||child.anInt842>128||child.anInt728>128
                ||child.aByte778!=0||child.aByte724!=0)return false;
            items.add(child);
        }
        if(items.size()<2||items.size()>2000)return false;
        // Preserve the script's current visible ordering (including bank filtering/tabs), not a guessed global slot order.
        items.sort(Comparator.comparingInt((Class46 w)->w.anInt739).thenComparingInt(w->w.anInt788).thenComparingInt(w->w.anInt704));
        ViewportState viewport=MobileBridge.ui().viewport;
        double displayRatio=viewport==null?1:Math.min((double)viewport.width/viewport.logicalWidth,(double)viewport.height/viewport.logicalHeight);
        int minimum=Math.max(16,(int)Math.ceil(48/Math.max(.25,displayRatio))),gap=Math.max(2,(int)Math.ceil(4/Math.max(.25,displayRatio)));
        int width=minimum,height=minimum;
        for(Class46 w:items){double scale=Math.max((double)minimum/w.anInt842,(double)minimum/w.anInt728);width=Math.max(width,(int)Math.ceil(w.anInt842*scale));height=Math.max(height,(int)Math.ceil(w.anInt728*scale));}
        if(width>parent.anInt709)return false; // Never create an unscrollable horizontal target larger than its parent.
        int columns=Math.max(1,(parent.anInt709+gap)/(width+gap));
        State state=old==null?new State():old;
        if(old==null){state.authoredW=parent.anInt698;state.authoredH=parent.anInt791;states.put(parent,state);}
        for(int i=0;i<items.size();i++) {
            Class46 child=items.get(i);double scale=Math.min((double)width/child.anInt842,(double)height/child.anInt728);
            int w=Math.max(1,(int)Math.round(child.anInt842*scale)),h=Math.max(1,(int)Math.round(child.anInt728*scale));
            geometry.put(child,new Geometry((i%columns)*(width+gap)+(width-w)/2,(i/columns)*(height+gap)+(height-h)/2,w,h));state.children.add(new java.lang.ref.WeakReference<>(child));
        }
        parent.anInt698=parent.anInt709;parent.anInt791=Math.max(parent.anInt789,((items.size()+columns-1)/columns)*(height+gap)-gap);
        state.appliedW=parent.anInt698;state.appliedH=parent.anInt791;
        parent.anInt747=0;parent.anInt755=Math.max(0,Math.min(parent.anInt755,parent.anInt791-parent.anInt789));return true;
    }
    static boolean size(Class46 w){Geometry g=MobileConfig.enabled()?geometry.get(w):null;if(g==null)return false;w.anInt709=g.width;w.anInt789=g.height;return true;}
    static boolean position(Class46 w){Geometry g=MobileConfig.enabled()?geometry.get(w):null;if(g==null)return false;w.anInt800=g.x;w.anInt750=g.y;return true;}
    private MobileItemGridLayout(){}
}
