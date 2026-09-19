import com.voidclient.mobile.*;
import java.awt.Rectangle;
import java.util.*;

/** Native text/choice and equipment reflow, restricted to compatible existing sibling structures. */
final class MobileNativeFlow {
    private static final Map<Class46,Rectangle> geometry=new WeakHashMap<>();
    private static final Map<Class46,State> states=new WeakHashMap<>();
    private static final Set<Integer> pendingFonts=new HashSet<>();
    private static long fontRetryAt;
    /** Cache fonts can finish loading without a script editing the text again. Poll only outstanding metrics. */
    static boolean fontsReady() {
        if(!MobileNativeHud.enabled()){pendingFonts.clear();return false;}
        long now=System.nanoTime();
        if(pendingFonts.isEmpty()||Class348_Sub8.aHa6654==null||now<fontRetryAt)return false;
        fontRetryAt=now+500_000_000L;boolean ready=false;
        for(Iterator<Integer> it=pendingFonts.iterator();it.hasNext();) {
            if(Class135.method1151(-25411,Class348_Sub8.aHa6654,it.next())!=null){it.remove();ready=true;}
        }
        return ready;
    }
    private static final class State {
        int authoredW,authoredH,appliedW,appliedH;
        final List<java.lang.ref.WeakReference<Class46>> children=new ArrayList<>();
        State(Class46 w){authoredW=appliedW=w.anInt698;authoredH=appliedH=w.anInt791;}
    }
    static void restore(Class46 p){State s=states.get(p);if(s==null)return;
        for(java.lang.ref.WeakReference<Class46> ref:s.children){Class46 w=ref.get();if(w!=null)geometry.remove(w);}s.children.clear();
        if(p.anInt698!=s.appliedW)s.authoredW=p.anInt698;if(p.anInt791!=s.appliedH)s.authoredH=p.anInt791;
        p.anInt698=s.authoredW;p.anInt791=s.authoredH;s.appliedW=p.anInt698;s.appliedH=p.anInt791;
    }
    static boolean prepare(Class46 parent,Class46[] widgets){
        restore(parent);
        if(!MobileNativeHud.enabled()||widgets==null||parent.aBoolean813||parent.anInt774!=0||parent.anInt765!=0||parent.anInt709<MobileNativeHud.unit(120))return false;
        int group=parent.anInt830>>>16;String family=InterfaceRegistry.family(group);
        if(group==387)return equipment(parent,widgets);
        if(group==137||!"dialogue".equals(family))return false;
        List<Class46> content=new ArrayList<>(),background=new ArrayList<>();
        for(Class46 w:widgets)if(w!=null&&w.anInt834==parent.anInt830&&!w.aBoolean813){
            // Script-bound fields, special views, nested containers and animated item/text replacement are not guessed.
            if(w.anInt765!=0||w.anObjectArray822!=null||w.anInt812!=-1||w.anInt806!=-1)return false;
            if(w.anInt774==4)content.add(w);
            else if(w.anInt774==3&&w.anObjectArray763==null&&w.aStringArray833==null)background.add(w);
            else return false;
        }
        if(content.isEmpty()||content.size()>32)return false;
        content.sort(Comparator.comparingInt((Class46 w)->w.anInt739).thenComparingInt(w->w.anInt788));
        int gap=MobileNativeHud.unit(6),margin=MobileNativeHud.unit(8),width=Math.max(1,parent.anInt709-2*margin),y=margin;
        Map<Class46,Rectangle> planned=new IdentityHashMap<>();
        for(Class46 w:content){int height=textHeight(w,width);if(height<0)return false;
            height=Math.max(height,interactive(w)?MobileNativeHud.unit(48):MobileNativeHud.unit(20));
            planned.put(w,new Rectangle(margin,y,width,height));y+=height+gap;
            if(y>32768)return false;
        }
        for(Class46 w:background)planned.put(w,new Rectangle(0,0,parent.anInt709,Math.max(parent.anInt789,y+margin)));
        commit(parent,planned,y+margin);return true;
    }
    private static boolean interactive(Class46 w){return w.anObjectArray763!=null||w.aStringArray833!=null||client.method105(w).anInt7098!=0;}
    private static int textHeight(Class46 w,int width){
        String text=w.aString792==null?"":w.aString792;
        if(text.length()>8192)return -1;
        if(Class348_Sub8.aHa6654!=null&&w.anInt702>=0){
            Class143 metrics=Class135.method1151(-25411,Class348_Sub8.aHa6654,w.anInt702);
            if(metrics!=null){pendingFonts.remove(w.anInt702);return Math.max(1,metrics.method1185(Class113.aClass105Array1744,0,w.anInt673,width,text))+MobileNativeHud.unit(8);}
            if(pendingFonts.size()<64)pendingFonts.add(w.anInt702);
            return -1; // Keep authored geometry until the actual font metrics become available.
        }
        // Pre-font-loading fixture/fallback: conservatively reserve a full line for every wrap and explicit break.
        int columns=Math.max(1,width/MobileNativeHud.unit(10)),lines=0;
        for(String line:text.replaceAll("(?i)<br\\s*/?>","\n").replaceAll("<[^>]*>","").split("\n",-1))lines+=Math.max(1,(line.length()+columns-1)/columns);
        return lines*MobileNativeHud.unit(20)+MobileNativeHud.unit(8);
    }
    private static final Map<Integer,int[]> SLOTS=new LinkedHashMap<>();
    static {SLOTS.put(8,new int[]{1,0});SLOTS.put(11,new int[]{0,1});SLOTS.put(14,new int[]{1,1});SLOTS.put(38,new int[]{2,1});SLOTS.put(17,new int[]{0,2});SLOTS.put(20,new int[]{1,2});SLOTS.put(23,new int[]{2,2});SLOTS.put(26,new int[]{1,3});SLOTS.put(29,new int[]{0,4});SLOTS.put(32,new int[]{1,4});SLOTS.put(35,new int[]{2,4});}
    private static boolean equipment(Class46 parent,Class46[] widgets){
        Map<Class46,int[]> slots=new IdentityHashMap<>();List<Class46> backgrounds=new ArrayList<>(),controls=new ArrayList<>();
        for(Class46 w:widgets)if(w!=null&&w.anInt834==parent.anInt830&&!w.aBoolean813){
            if(w.anInt765!=0||w.anObjectArray822!=null)return false;
            int[] at=SLOTS.get(w.anInt830&65535);
            if(at!=null&&w.anInt774==5&&w.anInt842==w.anInt728&&w.anInt842>=16&&w.anInt842<=64)slots.put(w,at);
            else if(w.anInt774==3&&!interactive(w))backgrounds.add(w);
            else if(w.anInt774==4||((w.anInt830&65535)==39||(w.anInt830&65535)==42||(w.anInt830&65535)==45))controls.add(w);
            else return false;
        }
        if(slots.size()!=SLOTS.size())return false;
        int cell=MobileNativeHud.unit(48),gap=MobileNativeHud.unit(8),grid=3*cell+2*gap;
        if(grid>parent.anInt709)return false;
        int left=(parent.anInt709-grid)/2,top=gap,y=top+5*(cell+gap);Map<Class46,Rectangle> plan=new IdentityHashMap<>();
        for(Map.Entry<Class46,int[]> e:slots.entrySet())plan.put(e.getKey(),new Rectangle(left+e.getValue()[0]*(cell+gap),top+e.getValue()[1]*(cell+gap),cell,cell));
        controls.sort(Comparator.comparingInt(w->w.anInt739));
        for(Class46 w:controls){plan.put(w,new Rectangle(gap,y,parent.anInt709-2*gap,cell));y+=cell+gap;}
        for(Class46 w:backgrounds)plan.put(w,new Rectangle(0,0,parent.anInt709,Math.max(parent.anInt789,y)));
        commit(parent,plan,y);return true;
    }
    private static void commit(Class46 p,Map<Class46,Rectangle> plan,int height){State s=states.get(p);if(s==null){s=new State(p);states.put(p,s);}for(Map.Entry<Class46,Rectangle> e:plan.entrySet()){geometry.put(e.getKey(),e.getValue());s.children.add(new java.lang.ref.WeakReference<>(e.getKey()));}
        p.anInt698=p.anInt709;p.anInt791=Math.max(p.anInt789,height);p.anInt747=0;p.anInt755=Math.max(0,Math.min(p.anInt755,p.anInt791-p.anInt789));s.appliedW=p.anInt698;s.appliedH=p.anInt791;
    }
    static boolean size(Class46 w){Rectangle r=MobileNativeHud.enabled()?geometry.get(w):null;if(r==null)return false;w.anInt709=r.width;w.anInt789=r.height;return true;}
    static boolean position(Class46 w){Rectangle r=MobileNativeHud.enabled()?geometry.get(w):null;if(r==null)return false;w.anInt800=r.x;w.anInt750=r.y;return true;}
    private MobileNativeFlow(){}
}
