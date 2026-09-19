import com.voidclient.mobile.*;
import java.awt.Rectangle;
import java.util.*;

/** Native attachment docking. No widget is reparented, hidden, or granted an operation by this layer. */
final class MobileNativeHud {
    private static final Map<Class46,Placement> placements=new WeakHashMap<>();
    private static final Set<Integer> dockedGroups=new HashSet<>();
    private static final Set<Integer> SIDES=new HashSet<>(Arrays.asList(149,387,192,193,271,430,884,950,320,190,261,34,550,551,589,763,621,336,670,957));
    private static final Set<Integer> MAINS=new HashSet<>(Arrays.asList(11,762,620,956,335,334,667,755));
    private static volatile String status="Waiting for a compatible native HUD";
    private static long observed=Long.MIN_VALUE;
    private static final class Placement {
        final Rectangle bounds; final int parent,rawX,rawY,rawW,rawH;final byte xp,yp,wp,hp;
        int savedW,savedH,appliedW,appliedH;boolean applied;
        Placement(Class46 w,Rectangle b){bounds=new Rectangle(b);parent=w.anInt834;rawX=w.anInt788;rawY=w.anInt739;rawW=w.anInt842;rawH=w.anInt728;xp=w.aByte817;yp=w.aByte681;wp=w.aByte778;hp=w.aByte724;savedW=w.anInt698;savedH=w.anInt791;}
        boolean matches(Class46 w){return parent==w.anInt834&&rawX==w.anInt788&&rawY==w.anInt739&&rawW==w.anInt842&&rawH==w.anInt728&&xp==w.aByte817&&yp==w.aByte681&&wp==w.aByte778&&hp==w.aByte724&&!w.aBoolean813;}
        void restore(Class46 w){if(!applied)return;if(w.anInt698==appliedW)w.anInt698=savedW;else savedW=w.anInt698;if(w.anInt791==appliedH)w.anInt791=savedH;else savedH=w.anInt791;applied=false;}
    }
    static final class Attachment {
        final Class46 holder;final int group;final String zone;final List<Class46> chain;
        Attachment(Class46 w,int g,String z,List<Class46> c){holder=w;group=g;zone=z;chain=c;}
    }
    interface AttachmentLookup {int group(Class46 w);}
    private static final AttachmentLookup NATIVE=w->{if(Class125.aClass356_4915==null)return -1;Class348_Sub41 a=(Class348_Sub41)Class125.aClass356_4915.method3480(w.anInt830,-6008);return a==null?-1:a.anInt7050;};
    static boolean enabled(){return MobileConfig.enabled()&&Boolean.parseBoolean(System.getProperty("void.mobile.nativeHud","true"));}
    static String status(){return status;}
    static int unit(int units){ViewportState v=MobileBridge.ui().viewport;double ratio=v==null?1:Math.min((double)v.width/v.logicalWidth,(double)v.height/v.logicalHeight);return Math.max(1,(int)Math.ceil(units/Math.max(.25,ratio)));}
    static void reset(){for(Map.Entry<Class46,Placement> e:placements.entrySet()){MobileNativeFlow.restore(e.getKey());MobileItemGridLayout.restoreContent(e.getKey());e.getValue().restore(e.getKey());}placements.clear();dockedGroups.clear();}
    static void prepareGroup(int id,int width,int height,Class46[] widgets){
        if(id==r.anInt9721)prepare(id,width,height,widgets,NATIVE);
        if(!enabled()||!dockedGroups.contains(id)||widgets==null)return;
        for(Class46 w:widgets)if(w!=null&&w.anInt834==-1&&!w.aBoolean813&&w.anInt774==0&&w.anInt765==0)put(w,new Rectangle(0,0,width,height));
    }
    static void prepare(int root,int width,int height,Class46[] widgets,AttachmentLookup lookup){
        reset();
        if(!enabled()){status="Native docking disabled";return;}
        if((root!=548&&root!=746)||widgets==null||widgets.length>4096||width<unit(240)||height<unit(240)){status="Original layout: unsupported root or viewport";return;}
        Map<Integer,Class46> byId=new HashMap<>();for(Class46 w:widgets)if(w!=null){if(byId.put(w.anInt830,w)!=null){status="Original layout: duplicate native IDs";return;}}
        List<Attachment> candidates=new ArrayList<>();Map<String,Integer> counts=new HashMap<>();
        for(Class46 w:widgets)if(w!=null&&passive(w)){
            int group=lookup.group(w);String zone=zone(group);if(zone==null)continue;
            List<Class46> chain=chain(w,byId);if(chain==null)continue;
            candidates.add(new Attachment(w,group,zone,chain));counts.put(zone,counts.getOrDefault(zone,0)+1);
        }
        // Multiple visible owners of the same area are ambiguous. Keep the original layout, rather than overlap them.
        candidates.removeIf(a->counts.get(a.zone)!=1);
        Set<Attachment> conflicting=Collections.newSetFromMap(new IdentityHashMap<Attachment,Boolean>());
        for(Attachment a:candidates)for(Attachment b:candidates)if(a!=b&&(a.chain.contains(b.holder)||b.chain.contains(a.holder))){conflicting.add(a);conflicting.add(b);}
        candidates.removeAll(conflicting);
        boolean side=has(candidates,"side"),main=has(candidates,"main"),chat=has(candidates,"chat"),dialogue=has(candidates,"dialogue");
        Map<String,Rectangle> zones=zones(width,height,side,main,chat,dialogue);
        for(Attachment a:candidates){
            Rectangle bounds=zones.get(a.zone);if(bounds==null||bounds.width<unit(72)||bounds.height<unit(48))continue;
            // Only passive, non-scrolling native ancestors may expand to accommodate the dock.
            for(int i=a.chain.size()-1;i>=1;i--)put(a.chain.get(i),new Rectangle(0,0,width,height));
            put(a.holder,bounds);dockedGroups.add(a.group);
        }
        status=dockedGroups.isEmpty()?"Original layout: no unambiguous compatible visible attachments":"Native docked groups: "+new TreeSet<>(dockedGroups);
    }
    private static boolean passive(Class46 w){return w.anInt774==0&&w.anInt765==0&&!w.aBoolean813&&w.anInt747==0&&w.anInt755==0&&w.anObjectArray763==null&&w.anObjectArray822==null&&w.aStringArray833==null;}
    private static List<Class46> chain(Class46 w,Map<Integer,Class46> ids){
        List<Class46> out=new ArrayList<>();Set<Class46> seen=Collections.newSetFromMap(new IdentityHashMap<Class46,Boolean>());
        for(Class46 n=w;n!=null;n=ids.get(n.anInt834)){
            if(out.size()>=64||!seen.add(n)||!passive(n))return null;out.add(n);
            if(n.anInt834==-1)return out;
        }
        return null;
    }
    private static String zone(int group){if(group==752)return "chat";if(SIDES.contains(group))return "side";if(MAINS.contains(group))return "main";
        if(group!=137&&InterfaceRegistry.known(group)&&"dialogue".equals(InterfaceRegistry.family(group)))return "dialogue";return null;}
    private static boolean has(List<Attachment> list,String zone){for(Attachment a:list)if(zone.equals(a.zone))return true;return false;}
    static Map<String,Rectangle> zones(int width,int height,boolean side,boolean main,boolean chat,boolean dialogue){
        Map<String,Rectangle> z=new HashMap<>();int m=unit(8),gap=unit(8),usableW=Math.max(1,width-2*m),usableH=Math.max(1,height-2*m);
        boolean portrait=width<height;
        int chatH=chat?Math.min(unit(144),usableH/4):0;
        int sideW=side&&!portrait?Math.min(unit(300),usableW/3):0;
        int sideH=side&&portrait?Math.min(unit(340),(usableH-chatH)*(main?35:50)/100):0;
        int leftW=usableW-(sideW>0?sideW+gap:0);
        if(chat)z.put("chat",new Rectangle(m,height-m-chatH,portrait?usableW:leftW,chatH));
        if(side){if(portrait)z.put("side",new Rectangle(m,height-m-chatH-(chat?gap:0)-sideH,usableW,sideH));else z.put("side",new Rectangle(width-m-sideW,m,sideW,usableH));}
        int upperH=usableH-chatH-(chat?gap:0)-sideH-(sideH>0?gap:0);
        if(main)z.put("main",new Rectangle(m,m,leftW,Math.max(1,upperH)));
        if(dialogue){int h=Math.min(unit(260),Math.max(unit(96),upperH/2));h=Math.min(h,upperH);z.put("dialogue",new Rectangle(m,m+upperH-h,leftW,Math.max(1,h)));}
        return z;
    }
    private static void put(Class46 w,Rectangle r){Placement old=placements.get(w);if(old!=null)old.restore(w);placements.put(w,new Placement(w,r));}
    static boolean size(Class46 w){Placement p=placements.get(w);if(p==null)return false;p.restore(w);if(!enabled()||!p.matches(w))return false;
        w.anInt709=p.bounds.width;w.anInt789=p.bounds.height;w.anInt698=p.bounds.width;w.anInt791=p.bounds.height;p.appliedW=w.anInt698;p.appliedH=w.anInt791;p.applied=true;return true;}
    /** Stretch wide native containers against the resized parent while retaining authored edge margins. */
    static boolean stretch(Class46 w,int parentWidth,int parentHeight) {
        if(!enabled()||!dockedGroups.contains(w.anInt830>>>16)||w.anInt774!=0||w.anInt765!=0||w.anInt834<0||w.aBoolean813||w.aByte817!=0||w.aByte681!=0)return false;
        Class46[][] groups=Class348_Sub40_Sub33.aClass46ArrayArray9427;int group=w.anInt834>>>16,index=w.anInt834&65535;
        if(groups==null||group>=groups.length||groups[group]==null||index>=groups[group].length)return false;
        Class46 parent=groups[group][index];if(parent==null||parent.anInt774!=0)return false;
        int width=w.anInt709,height=w.anInt789,x=w.anInt788,y=w.anInt739;boolean changed=false;
        if(w.aByte778==0&&w.aByte817==0&&parent.anInt842>0&&w.anInt842*2L>=parent.anInt842&&w.anInt788>=0) {
            int right=parent.anInt842-w.anInt788-w.anInt842;
            if(right>=0) {int left=Math.min(w.anInt788,Math.max(0,(parentWidth-unit(72))/2));right=Math.min(right,Math.max(0,parentWidth-left-unit(72)));width=Math.max(1,parentWidth-left-right);x=left;changed=true;}
        }
        if(w.aByte724==0&&w.aByte681==0&&parent.anInt728>0&&w.anInt728*2L>=parent.anInt728&&w.anInt739>=0) {
            int bottom=parent.anInt728-w.anInt739-w.anInt728;
            if(bottom>=0) {int top=Math.min(w.anInt739,Math.max(0,(parentHeight-unit(72))/2));bottom=Math.min(bottom,Math.max(0,parentHeight-top-unit(72)));height=Math.max(1,parentHeight-top-bottom);y=top;changed=true;}
        }
        if(!changed)return false;
        // Position normally follows the same raw offsets. Only adjusted margins receive an override.
        if(w.aByte817==0)x=Math.max(0,Math.min(x,parentWidth-width));
        if(w.aByte681==0)y=Math.max(0,Math.min(y,parentHeight-height));
        put(w,new Rectangle(x,y,width,height));return size(w);
    }
    static boolean position(Class46 w){Placement p=placements.get(w);if(!enabled()||p==null||!p.matches(w))return false;w.anInt800=p.bounds.x;w.anInt750=p.bounds.y;return true;}
    /** Detect native visibility/attachment/script layout changes; excludes our effective coordinates. */
    static void tick(){
        if(!MobileConfig.enabled())return;Class46[][] groups=Class348_Sub40_Sub33.aClass46ArrayArray9427;int root=r.anInt9721;
        if(groups==null||root<0||root>=groups.length)return;Class46[] widgets=groups[root];if(widgets==null||widgets.length>4096)return;
        long hash=31L*root+AccessibilityPreferences.revision();
        for(Class46 w:widgets)if(w!=null){hash=authoredHash(hash,w);hash=31*hash+w.anInt747;hash=31*hash+w.anInt755;hash=31*hash+NATIVE.group(w);}
        for(Integer id:dockedGroups)if(id>=0&&id<groups.length&&groups[id]!=null&&groups[id].length<1024)for(Class46 w:groups[id])if(w!=null){
            hash=authoredHash(hash,w);
            if("dialogue".equals(InterfaceRegistry.family(id)))hash=31*hash+(w.aString792==null?0:w.aString792.hashCode());
        }
        if(observed!=hash||MobileNativeFlow.fontsReady()){observed=hash;RuntimeException_Sub1.aBoolean4604=true;Class49.aBoolean4726=true;}
    }
    private static long authoredHash(long hash,Class46 w) {
        hash=31*hash+System.identityHashCode(w);hash=31*hash+w.anInt834;hash=31*hash+(w.aBoolean813?1:0);
        hash=31*hash+w.anInt788;hash=31*hash+w.anInt739;hash=31*hash+w.anInt842;hash=31*hash+w.anInt728;
        hash=31*hash+w.aByte817;hash=31*hash+w.aByte681;hash=31*hash+w.aByte778;hash=31*hash+w.aByte724;
        hash=31*hash+w.anInt774;hash=31*hash+w.anInt765;hash=31*hash+w.anInt702;
        hash=31*hash+System.identityHashCode(w.anObjectArray763);hash=31*hash+System.identityHashCode(w.anObjectArray822);
        return 31*hash+Arrays.hashCode(w.aStringArray833);
    }
    private MobileNativeHud(){}
}
