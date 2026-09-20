import com.voidclient.mobile.*;
import java.awt.Rectangle;
import java.util.*;

/**
 * Bounded mobile HUD shell.
 *
 * The scene remains the primary viewport. Known attached RuneScape interfaces are moved into
 * stable edge/modal regions; arbitrary widget ancestors are never stretched to fill the screen.
 * Unknown structures keep their authored layout.
 */
final class MobileNativeHud {
    private enum Role { CHAT, SIDE, MODAL, DIALOGUE, FULLSCREEN }

    private static final Map<Class46,Placement> placements=new WeakHashMap<>();
    private static final Set<Integer> dockedGroups=new HashSet<>();
    private static final Map<Integer,Rectangle> groupBounds=new HashMap<>();
    private static final Map<Integer,Role> groupRoles=new HashMap<>();
    private static final Map<Integer,Class46> groupRoots=new HashMap<>();
    private static volatile String status="Waiting for a compatible native HUD";
    private static long observed=Long.MIN_VALUE;

    private static final Set<Integer> SIDE_FALLBACK=new HashSet<>(Arrays.asList(
        149,387,192,193,271,430,884,950,320,190,261,34,550,551,589,763,621,336,670,957));
    private static final Set<Integer> MODAL_FALLBACK=new HashSet<>(Arrays.asList(
        11,762,620,956,335,334,667));

    private static final class Placement {
        final Rectangle bounds;
        final int parent,rawX,rawY,rawW,rawH;
        final byte xp,yp,wp,hp;
        int savedW,savedH,appliedW,appliedH;
        boolean applied;
        Placement(Class46 w,Rectangle b) {
            bounds=new Rectangle(b);
            parent=w.anInt834;rawX=w.anInt788;rawY=w.anInt739;rawW=w.anInt842;rawH=w.anInt728;
            xp=w.aByte817;yp=w.aByte681;wp=w.aByte778;hp=w.aByte724;
            savedW=w.anInt698;savedH=w.anInt791;
        }
        boolean matches(Class46 w) {
            return parent==w.anInt834&&rawX==w.anInt788&&rawY==w.anInt739&&rawW==w.anInt842&&rawH==w.anInt728
                &&xp==w.aByte817&&yp==w.aByte681&&wp==w.aByte778&&hp==w.aByte724&&!w.aBoolean813;
        }
        void restore(Class46 w) {
            if(!applied)return;
            if(w.anInt698==appliedW)w.anInt698=savedW;else savedW=w.anInt698;
            if(w.anInt791==appliedH)w.anInt791=savedH;else savedH=w.anInt791;
            applied=false;
        }
    }

    static final class Attachment {
        final Class46 holder;
        final int group;
        final String zone;
        final List<Class46> chain;
        final Role role;
        Attachment(Class46 w,int g,Role r,List<Class46> c) {
            holder=w;group=g;role=r;zone=r.name().toLowerCase(Locale.ROOT);chain=c;
        }
    }

    interface AttachmentLookup {int group(Class46 w);}
    private static final AttachmentLookup NATIVE=w->{
        if(Class125.aClass356_4915==null)return -1;
        Class348_Sub41 a=(Class348_Sub41)Class125.aClass356_4915.method3480(w.anInt830,-6008);
        return a==null?-1:a.anInt7050;
    };

    static boolean enabled() {
        return MobileConfig.enabled()&&Boolean.parseBoolean(System.getProperty("void.mobile.nativeHud","true"));
    }
    static String status(){return status;}

    /** Convert display-oriented mobile sizes into this interface's logical coordinates. */
    static int unit(int units) {
        ViewportState v=MobileBridge.ui().viewport;
        double ratio=v==null?1:Math.min((double)v.width/v.logicalWidth,(double)v.height/v.logicalHeight);
        return Math.max(1,(int)Math.ceil(units/Math.max(.25,ratio)));
    }

    static void reset() {
        for(Map.Entry<Class46,Placement> e:placements.entrySet()) {
            MobileNativeFlow.restore(e.getKey());
            MobileItemGridLayout.restoreContent(e.getKey());
            e.getValue().restore(e.getKey());
        }
        placements.clear();
        dockedGroups.clear();
        groupBounds.clear();
        groupRoles.clear();
        groupRoots.clear();
    }

    static void prepareGroup(int id,int width,int height,Class46[] widgets) {
        if(id==r.anInt9721)prepare(id,width,height,widgets,NATIVE);
        if(!enabled()||!dockedGroups.contains(id)||widgets==null)return;

        // Attached interfaces get their holder's local coordinate space, but only when there is
        // one unambiguous passive root container. Multiple roots keep authored geometry.
        Class46 only=null;
        for(Class46 w:widgets) if(w!=null&&w.anInt834==-1&&!w.aBoolean813&&passive(w)) {
            if(only!=null){only=null;break;}
            only=w;
        }
        if(only!=null){groupRoots.put(id,only);put(only,new Rectangle(0,0,width,height));}
    }

    static void prepare(int root,int width,int height,Class46[] widgets,AttachmentLookup lookup) {
        reset();
        if(!enabled()){status="Native HUD shell disabled";return;}
        if((root!=548&&root!=746)||widgets==null||widgets.length>4096||width<unit(240)||height<unit(240)) {
            status="Original layout: unsupported root or viewport";return;
        }

        Map<Integer,Class46> byId=new HashMap<>();
        for(Class46 w:widgets) if(w!=null) {
            if(byId.put(w.anInt830,w)!=null){status="Original layout: duplicate native IDs";return;}
        }

        // The one supported expansion is the actual game-frame shell itself. We no longer
        // inflate every passive ancestor between a holder and the root.
        Class46 shell=singleShell(widgets);
        if(shell==null){status="Original layout: ambiguous game-frame shell";return;}
        put(shell,new Rectangle(0,0,width,height));

        List<Attachment> candidates=new ArrayList<>();
        Map<Role,Integer> counts=new EnumMap<>(Role.class);
        for(Class46 w:widgets) if(w!=null&&w!=shell&&passive(w)) {
            int group=lookup.group(w);
            Role role=role(group);
            if(role==null)continue;
            List<Class46> chain=chain(w,byId,shell);
            if(chain==null||!safeCoordinateChain(chain,shell))continue;
            Attachment a=new Attachment(w,group,role,chain);
            candidates.add(a);
            counts.put(role,counts.containsKey(role)?counts.get(role)+1:1);
        }

        // One visible owner per lane. If the cache exposes competing owners, do nothing rather
        // than stack or stretch them.
        candidates.removeIf(a->counts.get(a.role)!=1);

        Attachment chat=find(candidates,Role.CHAT);
        Attachment side=find(candidates,Role.SIDE);
        Attachment modal=find(candidates,Role.MODAL);
        Attachment dialogue=find(candidates,Role.DIALOGUE);
        Attachment fullscreen=find(candidates,Role.FULLSCREEN);

        Map<Role,Rectangle> plan=plan(width,height,chat,side,modal,dialogue,fullscreen);
        for(Attachment a:candidates) {
            Rectangle bounds=plan.get(a.role);
            if(bounds==null||bounds.width<unit(72)||bounds.height<unit(48))continue;
            put(a.holder,bounds);
            dockedGroups.add(a.group);
            groupBounds.put(a.group,new Rectangle(bounds));
            groupRoles.put(a.group,a.role);
        }

        status=dockedGroups.isEmpty()
            ?"Original layout: no unambiguous compatible visible attachments"
            :"Bounded mobile HUD groups: "+new TreeSet<>(dockedGroups);
    }

    private static Class46 singleShell(Class46[] widgets) {
        Class46 only=null;
        for(Class46 w:widgets) if(w!=null&&w.anInt834==-1&&!w.aBoolean813&&passive(w)) {
            if(only!=null)return null;
            only=w;
        }
        return only;
    }

    private static boolean passive(Class46 w) {
        return w.anInt774==0&&w.anInt765==0&&!w.aBoolean813&&w.anInt747==0&&w.anInt755==0
            &&w.anObjectArray763==null&&w.anObjectArray822==null&&w.aStringArray833==null;
    }

    private static List<Class46> chain(Class46 w,Map<Integer,Class46> ids,Class46 shell) {
        List<Class46> out=new ArrayList<>();
        Set<Class46> seen=Collections.newSetFromMap(new IdentityHashMap<Class46,Boolean>());
        for(Class46 n=w;n!=null;n=ids.get(n.anInt834)) {
            if(out.size()>=64||!seen.add(n)||!passive(n))return null;
            out.add(n);
            if(n==shell)return out;
            if(n.anInt834==-1)return null;
        }
        return null;
    }

    /**
     * Direct shell children are ideal. Nested wrappers are only accepted when they are authored
     * as zero-offset parent-filling containers, so our holder rectangle still means viewport
     * coordinates without mutating the wrapper.
     */
    private static boolean safeCoordinateChain(List<Class46> chain,Class46 shell) {
        if(chain.isEmpty()||chain.get(chain.size()-1)!=shell)return false;
        for(int i=1;i<chain.size()-1;i++) {
            Class46 w=chain.get(i);
            if(w.anInt788!=0||w.anInt739!=0||w.aByte817!=0||w.aByte681!=0)return false;
            boolean fillsW=(w.aByte778==1&&w.anInt842==0)||(w.aByte778==2&&w.anInt842>=16300);
            boolean fillsH=(w.aByte724==1&&w.anInt728==0)||(w.aByte724==2&&w.anInt728>=16300);
            if(!fillsW||!fillsH)return false;
        }
        return true;
    }

    private static Role role(int group) {
        if(group<0)return null;
        String family=InterfaceRegistry.family(group);
        String type=InterfaceRegistry.type(group);
        if(group==752||"chat_box".equals(type))return Role.CHAT;
        if(group!=137&&"dialogue".equals(family))return Role.DIALOGUE;
        if("map".equals(family)&&"full_screen".equals(type))return Role.FULLSCREEN;
        if(SIDE_FALLBACK.contains(group)||"overlay_tab".equals(type)||type.endsWith("_tab")
            ||"spellbook_tab".equals(type)||"prayer_list_tab".equals(type))return Role.SIDE;
        if(MODAL_FALLBACK.contains(group)||"main_screen".equals(type)
            ||"bank".equals(family)||"shop".equals(family)||"confirmation".equals(family))return Role.MODAL;
        return null;
    }

    private static Attachment find(List<Attachment> list,Role role) {
        for(Attachment a:list)if(a.role==role)return a;
        return null;
    }

    private static int authoredW(Attachment a,int fallback) {
        if(a==null)return fallback;
        int v=a.holder.anInt842;
        return v>0?v:fallback;
    }
    private static int authoredH(Attachment a,int fallback) {
        if(a==null)return fallback;
        int v=a.holder.anInt728;
        return v>0?v:fallback;
    }
    private static int clamp(int value,int min,int max) {
        if(max<min)return Math.max(1,max);
        return Math.max(min,Math.min(max,value));
    }

    /** OSRS-style bounded HUD lanes. The scene is never reduced to a generic dashboard grid. */
    private static Map<Role,Rectangle> plan(int width,int height,Attachment chat,Attachment side,
                                            Attachment modal,Attachment dialogue,Attachment fullscreen) {
        Map<Role,Rectangle> out=new EnumMap<>(Role.class);
        int m=unit(8),gap=unit(8);
        boolean portrait=width<height;
        int usableW=Math.max(1,width-2*m),usableH=Math.max(1,height-2*m);

        if(chat!=null) {
            int maxW=Math.min(unit(portrait?330:360),usableW*(portrait?80:45)/100);
            int minW=Math.min(maxW,unit(220));
            int w=clamp(authoredW(chat,unit(320)),minW,maxW);
            int maxH=Math.min(unit(160),usableH*35/100);
            int minH=Math.min(maxH,unit(96));
            int h=clamp(authoredH(chat,unit(140)),minH,maxH);
            out.put(Role.CHAT,new Rectangle(m,height-m-h,w,h));
        }

        if(side!=null) {
            int maxW=Math.min(unit(portrait?260:300),usableW*(portrait?72:35)/100);
            int minW=Math.min(maxW,unit(180));
            int w=clamp(authoredW(side,unit(250)),minW,maxW);
            int topReserve=Math.min(unit(110),Math.max(unit(56),height/4));
            int bottom=height-m;
            Rectangle c=out.get(Role.CHAT);
            if(portrait&&c!=null)bottom=Math.min(bottom,c.y-gap);
            int available=Math.max(1,bottom-topReserve);
            int maxH=Math.min(unit(portrait?360:330),available);
            int minH=Math.min(maxH,unit(160));
            int h=clamp(authoredH(side,unit(280)),minH,maxH);
            int y=Math.max(m,topReserve+(available-h)/2);
            out.put(Role.SIDE,new Rectangle(width-m-w,y,w,h));
        }

        if(fullscreen!=null)out.put(Role.FULLSCREEN,new Rectangle(m,m,usableW,usableH));

        if(modal!=null) {
            Rectangle s=out.get(Role.SIDE);
            int left=m,right=s==null?width-m:s.x-gap;
            int availableW=Math.max(1,right-left);
            int maxW=Math.min(unit(640),availableW);
            int minW=Math.min(maxW,unit(300));
            int w=clamp(authoredW(modal,unit(520)),minW,maxW);
            int maxH=Math.min(unit(420),usableH);
            int minH=Math.min(maxH,unit(220));
            int h=clamp(authoredH(modal,unit(334)),minH,maxH);
            out.put(Role.MODAL,new Rectangle(left+(availableW-w)/2,m+(usableH-h)/2,w,h));
        }

        if(dialogue!=null) {
            Rectangle s=out.get(Role.SIDE);
            int left=m,right=s==null?width-m:s.x-gap;
            int availableW=Math.max(1,right-left);
            int maxW=Math.min(unit(560),availableW*82/100);
            int minW=Math.min(maxW,unit(260));
            int w=clamp(authoredW(dialogue,unit(460)),minW,maxW);
            int maxH=Math.min(unit(220),usableH*42/100);
            int minH=Math.min(maxH,unit(96));
            int h=clamp(authoredH(dialogue,unit(160)),minH,maxH);
            int y=height-m-h;
            Rectangle c=out.get(Role.CHAT);
            if(c!=null&&c.y-gap-h>=m)y=c.y-gap-h;
            out.put(Role.DIALOGUE,new Rectangle(left+(availableW-w)/2,Math.max(m,y),w,h));
        }
        return out;
    }

    /** Retained for regression tooling; now reports bounded lanes rather than dashboard regions. */
    static Map<String,Rectangle> zones(int width,int height,boolean side,boolean main,boolean chat,boolean dialogue) {
        Map<Role,Rectangle> p=plan(width,height,
            chat?fixture(Role.CHAT):null,side?fixture(Role.SIDE):null,
            main?fixture(Role.MODAL):null,dialogue?fixture(Role.DIALOGUE):null,null);
        Map<String,Rectangle> out=new HashMap<>();
        if(p.containsKey(Role.CHAT))out.put("chat",p.get(Role.CHAT));
        if(p.containsKey(Role.SIDE))out.put("side",p.get(Role.SIDE));
        if(p.containsKey(Role.MODAL))out.put("main",p.get(Role.MODAL));
        if(p.containsKey(Role.DIALOGUE))out.put("dialogue",p.get(Role.DIALOGUE));
        return out;
    }

    private static Attachment fixture(Role role) {
        return new Attachment(new Class46(),-1,role,Collections.<Class46>emptyList());
    }

    private static void put(Class46 w,Rectangle r) {
        Placement old=placements.get(w);
        if(old!=null)old.restore(w);
        placements.put(w,new Placement(w,r));
    }

    static boolean size(Class46 w) {
        Placement p=placements.get(w);
        if(p==null)return false;
        p.restore(w);
        if(!enabled()||!p.matches(w))return false;
        w.anInt709=p.bounds.width;w.anInt789=p.bounds.height;
        w.anInt698=p.bounds.width;w.anInt791=p.bounds.height;
        p.appliedW=w.anInt698;p.appliedH=w.anInt791;p.applied=true;
        return true;
    }

    /**
     * Candidate 3 stretched arbitrary descendants. Candidate 4 permits only one narrow case:
     * a passive, fixed-geometry frame that is a direct child of the single attached-group root.
     * Its authored edge margins are retained inside the already-bounded mobile panel/modal.
     */
    static boolean stretch(Class46 w,int parentWidth,int parentHeight) {
        if(!enabled()||w==null||w.aBoolean813||w.anInt774!=0||w.anInt765!=0||w.anInt834<0
            ||w.aByte817!=0||w.aByte681!=0||w.aByte778!=0||w.aByte724!=0
            ||w.anObjectArray763!=null||w.anObjectArray822!=null||w.aStringArray833!=null)return false;
        int group=w.anInt830>>>16;
        Class46 root=groupRoots.get(group);
        if(root==null||w.anInt834!=root.anInt830||!groupRoles.containsKey(group))return false;
        if(root.anInt842<=0||root.anInt728<=0||parentWidth<1||parentHeight<1)return false;

        int x=w.anInt788,y=w.anInt739,width=w.anInt842,height=w.anInt728;
        boolean changed=false;
        if(width*10L>=root.anInt842*7L) {
            int right=root.anInt842-x-width;
            if(x>=0&&right>=0) {
                int candidate=parentWidth-x-right;
                if(candidate>=unit(72)){width=candidate;changed=true;}
            }
        }
        if(height*10L>=root.anInt728*7L) {
            int bottom=root.anInt728-y-height;
            if(y>=0&&bottom>=0) {
                int candidate=parentHeight-y-bottom;
                if(candidate>=unit(48)){height=candidate;changed=true;}
            }
        }
        if(!changed)return false;
        put(w,new Rectangle(Math.max(0,x),Math.max(0,y),width,height));
        return size(w);
    }

    static boolean position(Class46 w) {
        Placement p=placements.get(w);
        if(!enabled()||p==null||!p.matches(w))return false;
        w.anInt800=p.bounds.x;w.anInt750=p.bounds.y;
        return true;
    }

    /** Detect native visibility/attachment/script changes; excludes our effective coordinates. */
    static void tick() {
        if(!MobileConfig.enabled())return;
        Class46[][] groups=Class348_Sub40_Sub33.aClass46ArrayArray9427;
        int root=r.anInt9721;
        if(groups==null||root<0||root>=groups.length)return;
        Class46[] widgets=groups[root];
        if(widgets==null||widgets.length>4096)return;
        long hash=31L*root+AccessibilityPreferences.revision();
        for(Class46 w:widgets)if(w!=null) {
            hash=authoredHash(hash,w);hash=31*hash+w.anInt747;hash=31*hash+w.anInt755;hash=31*hash+NATIVE.group(w);
        }
        for(Integer id:dockedGroups)if(id>=0&&id<groups.length&&groups[id]!=null&&groups[id].length<1024)
            for(Class46 w:groups[id])if(w!=null) {
                hash=authoredHash(hash,w);
                if("dialogue".equals(InterfaceRegistry.family(id)))hash=31*hash+(w.aString792==null?0:w.aString792.hashCode());
            }
        if(observed!=hash||MobileNativeFlow.fontsReady()) {
            observed=hash;RuntimeException_Sub1.aBoolean4604=true;Class49.aBoolean4726=true;
        }
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
