import com.google.gson.Gson;
import com.voidclient.mobile.*;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.*;

/** Client-thread adapter. This is the only mobile layer permitted to access native game state. */
final class MobileRuntime implements GestureRecognizer.Sink {
    private static final MobileRuntime INSTANCE = new MobileRuntime();
    private GestureRecognizer gestures = new GestureRecognizer(this, MobileConfig.slopPixels(), MobileConfig.holdMillis());
    private int gestureSlop = MobileConfig.slopPixels(), gestureHold = MobileConfig.holdMillis();
    private final ArrayDeque<Class46> recordParents = new ArrayDeque<>();
    private final ArrayDeque<Class46> paintParents=new ArrayDeque<>();
    private List<Widget> painting=new ArrayList<>();
    private volatile List<Widget> painted;
    private boolean paintObserved,paintOverflow;
    private final WeakHashMap<Class46,Long> identities=new WeakHashMap<>();
    private long identityCounter,frameSerial;
    private final Map<Long,Widget> frameNodes=new HashMap<>();
    private Widget proxyMenuWidget;
    private volatile Widget moveSource;
    private long appliedLayoutPreferences=-1;
    private List<ContentGuard> menuGuard=Collections.emptyList();
    private final Gson gson = new Gson();
    private final Map<Class46,Widget> visibleIndex=new IdentityHashMap<>();
    private final List<Widget> blockers=new ArrayList<>();
    private List<Widget> indexedVisible;
    private double mapXResidual,mapYResidual,mapZoomResidual;
    private List<Widget> visible = new ArrayList<>(), building = new ArrayList<>();
    private final Map<Long, Hit> owners = new HashMap<>();
    private long token, revision = 1, textSession = 1, menuSerial, menuDeadline;
    private int root = -2, gameState = -1, ticks;
    private ViewportState viewport;
    private Class373_Sub1 input;
    private Hit lastTarget;
    private double pointerX, pointerY, zoomRemainder;
    private String status = "Mobile mode enabled", textPending = "";
    private final ArrayDeque<Integer> keysPending = new ArrayDeque<>();
    private volatile List<Entry> menu = Collections.emptyList();
    private int menuX, menuY, selected = -1;
    private volatile boolean inspecting;
    private final MobileTextInput textInput=new MobileTextInput();
    private int textAck;
    private boolean textAccepted;
    private volatile boolean contextNextTap;

    private static final class Widget {
        final Class46 nativeWidget;
        final int left, top, right, bottom, item, quantity,packedId,childIndex;
        final int nativeX,nativeY,nativeWidth,nativeHeight,nativeScrollX,nativeScrollY;
        int x,y,width,height;
        long handle,version;
        int order;
        String textAtCapture,renderedText;
        String[] operations;
        Object[] pressListener;
        Class46 parent;
        final boolean scroll, interactive, scene, blocking;
        Widget(Class46 w, int l, int t, int r, int b) {
            nativeWidget = w; packedId=w.anInt830;childIndex=w.anInt704; left = l; top = t; right = r; bottom = b; item = w.anInt812; quantity = w.anInt781;
            nativeX=w.anInt800;nativeY=w.anInt750;nativeWidth=w.anInt709;nativeHeight=w.anInt789;
            nativeScrollX=w.anInt747;nativeScrollY=w.anInt755;
            x=l; y=t; width=Math.max(0,r-l); height=Math.max(0,b-t);
            pressListener=w.anObjectArray763==null?null:w.anObjectArray763.clone();
            textAtCapture=w.aString792; operations=w.aStringArray833==null?null:w.aStringArray833.clone();
            scroll = w.anInt774 == 0 && (w.anInt791 > w.anInt789 || w.anInt698 > w.anInt709);
            scene = w.anInt765 == Class239_Sub10.anInt5943 || w.anInt765 == Class312.anInt3932;
            blocking = w.aBoolean776;
            interactive = scene || w.anInt765 == Class348_Sub45.anInt7102 || w.anInt765 == Class290.anInt3717 || blocking || scroll || w.aBoolean682 || w.anInt774 == 2
                || (w.aStringArray833 != null) || client.method105(w).anInt7098 != 0;
        }
        boolean geometryCurrent() {return nativeWidget.anInt800==nativeX && nativeWidget.anInt750==nativeY
            && nativeWidget.anInt709==nativeWidth && nativeWidget.anInt789==nativeHeight;}
        boolean contains(int x, int y) { return x >= left && y >= top && x < right && y < bottom; }
    }
    private static final class ContentGuard {
        final Class46 widget; final int item,quantity; final String text; final String[] actions; final boolean hidden;
        ContentGuard(Class46 w) { widget=w; item=w.anInt812; quantity=w.anInt781; text=w.aString792;
            actions=w.aStringArray833==null?null:w.aStringArray833.clone(); hidden=w.aBoolean813; }
        boolean valid() { return widget.anInt812==item && widget.anInt781==quantity && widget.aBoolean813==hidden
            && Objects.equals(text,widget.aString792) && Arrays.equals(actions,widget.aStringArray833); }
    }
    private static final class Hit {
        final long token;
        final Widget target, scroller;
        final GestureRecognizer.Kind kind;
        double downX, downY, scrollX, scrollY;
        long chromeToken,chromeGeneration;
        boolean scrolling;
        Hit(long token, Widget target, Widget scroller, GestureRecognizer.Kind kind) {
            this.token = token; this.target = target; this.scroller = scroller; this.kind = kind;
        }
    }
    private static final class Entry {
        final int index, arg1, arg2, opcode, extra1, extra2;
        final long identifier;
        final String label;
        final java.util.List<Object> signature;
        final Widget moveWidget;
        Entry(int index, Class348_Sub42_Sub12 nativeEntry) {
            moveWidget=null; this.index = index; arg1 = nativeEntry.anInt9602; arg2 = nativeEntry.anInt9607;
            opcode = nativeEntry.anInt9608; identifier = nativeEntry.aLong9605;
            extra1 = nativeEntry.anInt9599; extra2 = nativeEntry.anInt9609;
            label = plain(Class316.method2367((byte) -126, nativeEntry));
            signature = signature(nativeEntry);
        }
        Entry(int index,Widget widget) {
            this.index=index;moveWidget=widget;arg1=arg2=opcode=extra1=extra2=0;identifier=0;
            label="Move item: tap a destination slot";signature=Collections.emptyList();
        }
        static java.util.List<Object> signature(Class348_Sub42_Sub12 e) {
            return Arrays.asList(e.anInt9608, e.anInt9602, e.anInt9607, e.aLong9605, e.aLong9600,
                e.anInt9599, e.anInt9609, e.aString9595, e.aString9593, e.aString9601,
                e.aBoolean9597, e.aBoolean9610, e.aBoolean9611);
        }
    }
    private MobileRuntime() { }
    static void record(Class46 w, int l, int t, int r, int b) {
        if (MobileConfig.enabled() && !INSTANCE.paintObserved && l < r && t < b && INSTANCE.building.size() < 10000) {
            Widget node = new Widget(w, l, t, r, b);
            node.parent = INSTANCE.recordParents.peek();
            INSTANCE.building.add(node);
        }
    }
    static void pushParent(Class46 w) { if (MobileConfig.enabled()) INSTANCE.recordParents.push(w); }
    static void popParent() { if (MobileConfig.enabled() && !INSTANCE.recordParents.isEmpty()) INSTANCE.recordParents.pop(); }
    static void beginPaint() {
        if(!MobileConfig.enabled()) return;
        MobileMetrics.beginPaint(); INSTANCE.painting=new ArrayList<>(); INSTANCE.paintParents.clear(); INSTANCE.paintOverflow=false;
    }
    static void paintParent(Class46 w) { if(MobileConfig.enabled()) INSTANCE.paintParents.push(w); }
    static void paintParentEnd() { if(MobileConfig.enabled() && !INSTANCE.paintParents.isEmpty()) INSTANCE.paintParents.pop(); }
    static void recordPaint(Class46 w,int x,int y,int l,int t,int r,int b) {
        if(!MobileConfig.enabled()) return;
        MobileRuntime rt=INSTANCE;
        if(rt.painting.size()>=10000) { rt.paintOverflow=true; return; }
        Widget node=new Widget(w,l,t,r,b); node.parent=rt.paintParents.peek();
        node.x=x; node.y=y; node.width=w.anInt709; node.height=w.anInt789;
        Long id=rt.identities.get(w); if(id==null) { id=++rt.identityCounter; rt.identities.put(w,id); }
        node.handle=id;
        node.version=Objects.hash(w.anInt830,w.anInt704,w.anInt812,w.anInt781,w.aBoolean813,w.aString792,
            Arrays.hashCode(w.aStringArray833),Arrays.deepHashCode(w.anObjectArray763),System.identityHashCode(node.parent));
        rt.painting.add(node);
    }
    static void renderedText(Class46 w,String text) {
        if(!MobileConfig.enabled())return;
        for(int i=INSTANCE.painting.size()-1;i>=0;i--){Widget node=INSTANCE.painting.get(i);if(node.nativeWidget==w){node.renderedText=text;return;}}
    }
    static void endPaint(boolean success) {
        if(!MobileConfig.enabled()) return;
        MobileRuntime rt=INSTANCE; rt.paintObserved=true;
        rt.painted=success && !rt.paintOverflow?Collections.unmodifiableList(rt.painting):Collections.emptyList();
        rt.paintParents.clear(); MobileMetrics.endPaint();
    }
    static boolean touchPointerRequired() {
        return MobileConfig.enabled() && (Boolean.getBoolean("void.mobile.emulateTouch") || INSTANCE.contextNextTap || INSTANCE.moveSource!=null);
    }
    static boolean blocksMouse() {
        return MobileConfig.enabled() && (MobileConfig.browser() || MobileBridge.suspended()
            || MobileBridge.hostOverlayActive() || MobileBridge.textFocused()
            || !INSTANCE.menu.isEmpty() || INSTANCE.inspecting);
    }
    static void tick(Class373_Sub1 mouse, Component canvas) {
        if (!MobileConfig.enabled()) return;
        INSTANCE.input = mouse;
        try { INSTANCE.update(canvas); }
        catch (RuntimeException ex) {
            INSTANCE.cancelAll(); INSTANCE.status = "Mobile input stopped: " + ex.getClass().getSimpleName();
            INSTANCE.publish();
            throw ex; // Do not silently continue with half-applied input state.
        }
    }
    private void update(Component canvas) {
        MobileNativeHud.tick();
        if (gestureSlop != MobileConfig.slopPixels() || gestureHold != MobileConfig.holdMillis()) {
            gestures.cancel(); gestureSlop = MobileConfig.slopPixels(); gestureHold = MobileConfig.holdMillis();
            gestures = new GestureRecognizer(this, gestureSlop, gestureHold);
        }
        long layoutPreferences=AccessibilityPreferences.revision();
        if(appliedLayoutPreferences!=layoutPreferences) {
            appliedLayoutPreferences=layoutPreferences;
            // On the client thread: trigger the existing relayout path after grid/profile preferences change.
            RuntimeException_Sub1.aBoolean4604=true;Class49.aBoolean4726=true;cancelAll();
        }
        Applet_Sub1 applet = Class348_Sub40_Sub9.anApplet_Sub1_9169;
        if (!MobileConfig.browser() && applet != null
                && applet.applyStandaloneMobileScale(MobileConfig.integer("void.mobile.gameScale", 100, 100, 200))) {
            cancelAll(); revision++;
        }
        if (canvas == null || canvas.getWidth() < 1 || canvas.getHeight() < 1
                || Class321.anInt4017 < 1 || Class348_Sub42_Sub8_Sub2.anInt10432 < 1) return;
        Point p = MobileLauncher.canvasOrigin(canvas);
        double scale = MobileLauncher.displayScale();
        p.x = (int) Math.round(p.x * scale); p.y = (int) Math.round(p.y * scale);
        int displayWidth = Math.max(1, (int) Math.round(canvas.getWidth() * scale));
        int displayHeight = Math.max(1, (int) Math.round(canvas.getHeight() * scale));
        ViewportState next = new ViewportState(revision, p.x, p.y, displayWidth, displayHeight,
            Class321.anInt4017, Class348_Sub42_Sub8_Sub2.anInt10432,
            Math.max(1, Applet_Sub1.getConfiguredInterfaceLayoutWidth()),
            Math.max(1, Applet_Sub1.getConfiguredInterfaceLayoutHeight()));
        boolean changed = !next.sameGeometry(viewport) || root != r.anInt9721 || gameState != Class240.anInt4674;
        if (changed) {
            if (root != r.anInt9721 || gameState != Class240.anInt4674) textSession++;
            cancelAll(); revision++; root = r.anInt9721; gameState = Class240.anInt4674;
            next = new ViewportState(revision, p.x, p.y, displayWidth, displayHeight,
                next.nativeWidth, next.nativeHeight, next.logicalWidth, next.logicalHeight);
            building.clear(); visible=new ArrayList<>(); painted=null; lastTarget=null; frameNodes.clear();
        }
        viewport = next;
        if(paintObserved) {
            if(painted!=null) { visible=painted; painted=null; }
            building.clear();
        } else { List<Widget> old=visible; visible=building; building=old; building.clear(); }
        indexVisible();
        frameNodes.clear(); for(Widget w:visible) if(w.handle!=0) frameNodes.put(w.handle,w);
        long now = System.nanoTime() / 1000000;
        if (MobileBridge.suspended()) cancelAll();
        for (MobileBridge.Command c : MobileBridge.drain()) {
            MobileMetrics.queueAge(c.time);
            if ("cancel".equals(c.type)) { cancelAll(); continue; }
            if (MobileBridge.suspended()) continue;
            if (c.target!=0 || "cancelMode".equals(c.type)) {
                handleNodeCommand(c); continue;
            }
            if ("select".equals(c.type)) {
                if (c.revision == menuSerial && c.id >= 0 && c.id < menu.size()) selected = c.id;
                continue;
            }
            if ("text".equals(c.type) || "key".equals(c.type) || "editKey".equals(c.type)) {
                boolean accepted=textInput.accept(c,textSession,menu.isEmpty() && !inspecting);
                if ("text".equals(c.type)) { textAck=c.id; textAccepted=accepted; }
                status=accepted?"Text command validated; draft retained":"Text command rejected; draft retained";
                continue;
            }
            if (c.revision != revision) { cancelAll(); status = "View changed; try again"; continue; }
            if ("dismiss".equals(c.type)) { cancelAll(); continue; }
            if (!menu.isEmpty()) continue;
            switch (c.type) {
                case "down":
                    if (!viewport.contains(c.x, c.y) || inspecting) break;
                    textPending = ""; keysPending.clear(); textInput.cancel("Input context cancelled; draft retained");
                    gestures.down(c.id, c.x, c.y, c.time, revision); break;
                case "move":
                    if (!viewport.contains(c.x, c.y)) gestures.cancel();
                    else gestures.move(c.id, c.x, c.y, c.time, revision); break;
                case "up":
                    if (!viewport.contains(c.x, c.y)) gestures.cancel();
                    else gestures.up(c.id, c.x, c.y, c.time, revision); break;
                case "wheel":
                    GestureRecognizer.Target wheelTarget = hit(c.x, c.y);
                    if (wheelTarget != null && wheelTarget.kind == GestureRecognizer.Kind.SCROLL) scroll(wheelTarget, 0, -c.id);
                    else if (wheelTarget != null && wheelTarget.kind == GestureRecognizer.Kind.WORLD) zoom(-c.id);
                    break;
                case "contextAt":
                    GestureRecognizer.Target pointed = hit(c.x, c.y);
                    if (pointed != null) context(pointed, c.x, c.y);
                    break;
                case "context": armContext(); break;
                case "inspect": gestures.cancel(); inspecting = !inspecting; break;
                case "cameraLeft": camera(-32, 0); break;
                case "cameraRight": camera(32, 0); break;
                case "cameraUp": camera(0, -32); break;
                case "cameraDown": camera(0, 32); break;
                case "zoomIn": zoom(32); break;
                case "zoomOut": zoom(-32); break;
                case "mapLeft": map(48,0); break;
                case "mapRight": map(-48,0); break;
                case "mapUp": map(0,48); break;
                case "mapDown": map(0,-48); break;
                case "mapZoomIn": mapZoom(24); break;
                case "mapZoomOut": mapZoom(-24); break;
                default: break;
            }
        }
        gestures.tick(now, revision);
        // Action lists remain open until dismissed or invalidated; there is no reading deadline.
        if (!gestures.active()) owners.entrySet().removeIf(e -> lastTarget == null || e.getKey() != lastTarget.token);
        if (Class182.aClass346_2449 instanceof Class346_Sub1 && menu.isEmpty() && !inspecting)
            textInput.pump((Class346_Sub1)Class182.aClass346_2449,textSession);
        if (++ticks % 3 == 0 || changed) publish();
    }
    static void textConsumed(int receipt,int kind) {
        if(MobileConfig.enabled()) INSTANCE.textInput.consumed(receipt,kind);
    }
    static void afterClientCycle() {
        if(!MobileConfig.enabled()) return;
        MobileRuntime rt=INSTANCE;
        if(rt.root!=r.anInt9721 || rt.gameState!=Class240.anInt4674) {
            rt.textInput.cancel("Interface changed during text delivery"); return;
        }
        long next=rt.textInput.finishCycle(rt.textSession);
        if(next!=rt.textSession) { rt.textSession=next; rt.publish(); }
    }
    private boolean eligibleNode(Widget captured) {
        if(captured==null || viewport==null || root!=r.anInt9721 || gameState!=Class240.anInt4674) return false;
        Widget current=find(captured.nativeWidget);
        if(current==null || current.parent!=captured.parent || current.nativeWidget.anInt830!=captured.packedId || current.nativeWidget.anInt704!=captured.childIndex || current.nativeWidget.aBoolean813
            || current.nativeWidget.anInt812!=captured.item || current.nativeWidget.anInt781!=captured.quantity
            || !Objects.equals(current.nativeWidget.aString792,captured.textAtCapture)
            || !Arrays.equals(current.nativeWidget.aStringArray833,captured.operations)
            || !Arrays.deepEquals(current.nativeWidget.anObjectArray763,captured.pressListener)) return false;
        // Never operate through a later unrelated blocking interface. Parent blocking is intentional.
        int index=current.order;
        for(Widget other:blockers) {
            if(other.order<=index)continue;
            if(other.blocking && !other.nativeWidget.aBoolean813 && !descendantOf(other,current)
                && other.left<current.right && other.right>current.left && other.top<current.bottom && other.bottom>current.top)
                return false;
        }
        return true;
    }
    private void handleNodeCommand(MobileBridge.Command c) {
        if("cancelMode".equals(c.type)) {
            moveSource=null; cancelAll();
            if(r.aBoolean9722) Class341.method2678(-2049);
            status="Selection cancelled"; publish(); return;
        }
        Widget w=frameNodes.get(c.target);
        if(w==null || w.version!=c.revision || !eligibleNode(w)) {
            status="That interface changed or is covered. Refresh before acting."; moveSource=null; publish(); return;
        }
        if("scrollNode".equals(c.type)) {
            Widget ancestor=w;
            Set<Class46> seen=Collections.newSetFromMap(new IdentityHashMap<Class46,Boolean>());
            while(ancestor!=null && !ancestor.scroll && seen.add(ancestor.nativeWidget)) ancestor=parentOf(ancestor);
            if(ancestor!=null) {
                Class46 n=ancestor.nativeWidget;
                n.anInt755=clamp(n.anInt755+c.id*Math.max(1,n.anInt789*3/4),0,Math.max(0,n.anInt791-n.anInt789));
                Class251.method1916(-9343,n);
            }
        } else if("adjustNode".equals(c.type)) {
            Widget parent=parentOf(w);Class46 n=w.nativeWidget;
            if(slider(w) && parent!=null && eligibleNode(parent)) {
                boolean horizontal=parent.width>=parent.height;int range=Math.max(0,(horizontal?parent.width-n.anInt709:parent.height-n.anInt789));
                int x=w.x-parent.x+parent.nativeWidget.anInt747,y=w.y-parent.y+parent.nativeWidget.anInt755;
                int delta=Integer.signum(c.id)*Math.max(1,range/10);
                if(horizontal)x=clamp(x+delta,parent.nativeWidget.anInt747,parent.nativeWidget.anInt747+range);else y=clamp(y+delta,parent.nativeWidget.anInt755,parent.nativeWidget.anInt755+range);
                MobileNativeOperations.adjust(n,x,y);status="Native setting adjustment submitted";
            }else status="This setting does not expose a supported drag control.";
        } else if("moveSource".equals(c.type)) {
            if(movable(w) && w.item>=0) { if(r.aBoolean9722)Class341.method2678(-2049);moveSource=w; gestures.cancel(); status="Move selected: choose a destination slot in the same container, or Cancel selection."; }
            else status="This component does not expose a supported native move operation.";
        } else if("moveDest".equals(c.type)) {
            Widget source=moveSource; moveSource=null;
            if(source==null || !eligibleNode(source) || !movable(source) || !movableDestination(w)
                || source.nativeWidget==w.nativeWidget || source.nativeWidget.anInt830!=w.nativeWidget.anInt830 || gameState!=10) {
                status="Move cancelled: source or destination changed or is not in the same movable container.";
            } else {
                Class46 parent=loadedDragParent(source.nativeWidget); Widget parentNode=find(parent);
                if(parentNode==null) status="Move unavailable: native drag parent is not present.";
                else {
                    MobileNativeOperations.move(source.nativeWidget,w.nativeWidget,
                        w.x-parentNode.x+parent.anInt747,w.y-parentNode.y+parent.anInt755);
                    status="Native move submitted; waiting for the server's inventory update.";
                }
            }
        } else if("nodeOp".equals(c.type) && c.id==-1) {
            if(pressOnly(w)) {
                gestures.cancel();textSession++;
                MobileNativeOperations.activate(w.nativeWidget,Math.max(0,w.width/2),Math.max(0,w.height/2));
                status="Native control activated";
            } else status="This control requires the original pointer interaction.";
        } else if("nodeActions".equals(c.type) || "nodeOp".equals(c.type)) {
            List<Class348_Sub42_Sub12> candidates=MobileNativeActions.forWidget(w.nativeWidget);
            List<Entry> choices=new ArrayList<>();
            for(Class348_Sub42_Sub12 candidate:candidates) choices.add(new Entry(choices.size(),candidate));
            if("nodeActions".equals(c.type) && movable(w) && w.item>=0) choices.add(new Entry(choices.size(),w));
            if(choices.isEmpty()) { status="No currently permitted native actions for this component."; publish(); return; }
            gestures.cancel(); proxyMenuWidget=w; menu=choices; selected=-1; menuSerial++;
            menuX=Math.max(0,w.x+w.width/2); menuY=Math.max(0,w.y+w.height/2);
            menuGuard=guardGroup(w.nativeWidget.anInt830>>>16);
            if("nodeOp".equals(c.type)) {
                for(int i=0;i<candidates.size();i++) {
                    Class348_Sub42_Sub12 candidate=candidates.get(i);
                    if(((candidate.anInt9608==18 || candidate.anInt9608==1011) && candidate.aLong9605==c.id) || (candidate.anInt9608==16 && c.id==0)) { selected=i; break; }
                }
            }
            status="Choose an action for "+nodeLabel(w);
        }
        publish();
    }
    private boolean pressOnly(Widget w) {
        Class46 n=w.nativeWidget;
        // Do not reinterpret drag, hold/repeat, release, special-content, item or container interactions.
        return w.item<0&&n.anInt774!=0&&n.anInt765==0&&n.anObjectArray763!=null
            &&n.anObjectArray742==null&&n.anObjectArray785==null&&n.anObjectArray805==null&&n.anObjectArray823==null;
    }
    private boolean slider(Widget w) {
        if(w==null||w.item>=0||w.nativeWidget.anObjectArray823==null||!"settings".equals(InterfaceRegistry.family(w.packedId>>>16)))return false;
        String name=InterfaceRegistry.component(w.packedId).toLowerCase(Locale.ROOT);
        return (name.contains("volume")||name.contains("slider")||name.contains("brightness")) && parentOf(w)!=null;
    }
    private Class46 loadedDragParent(Class46 widget) {
        int levels=client.method105(widget).method3304((byte)125);
        if(levels<=0)return null;
        Class46[][] groups=Class348_Sub40_Sub33.aClass46ArrayArray9427;
        for(int i=0;i<levels;i++) {
            int packed=widget.anInt834,group=packed>>>16,child=packed&65535;
            if(packed<0||groups==null||group>=groups.length||groups[group]==null||child>=groups[group].length)return null;
            widget=groups[group][child];if(widget==null)return null;
        }
        return widget;
    }
    private boolean movable(Widget w) {
        return w!=null && w.nativeWidget.anInt704>=0 && InterfaceRegistry.inventory(w.nativeWidget.anInt830)
            && client.method105(w.nativeWidget).method3304((byte)125)>0;
    }
    private boolean movableDestination(Widget w) {
        return w!=null && w.nativeWidget.anInt704>=0 && InterfaceRegistry.inventory(w.nativeWidget.anInt830)
            && client.method105(w.nativeWidget).method3302(17356);
    }
    private Set<Class46> groupWidgets(int group) {
        Set<Class46> result=Collections.newSetFromMap(new IdentityHashMap<Class46,Boolean>());
        ArrayDeque<Class46> pending=new ArrayDeque<>();
        Class46[][] loaded=Class348_Sub40_Sub33.aClass46ArrayArray9427;
        if(loaded!=null&&group>=0&&group<loaded.length&&loaded[group]!=null)
            for(Class46 w:loaded[group])if(w!=null)pending.add(w);
        // Runtime/synthetic snapshots may not expose a complete archive. Include every observed member too.
        for(Widget w:visible)if((w.packedId>>>16)==group)pending.add(w.nativeWidget);
        while(!pending.isEmpty()) {
            Class46 w=pending.remove();if(!result.add(w))continue;
            if(result.size()>20000)return null; // Fail closed instead of validating a truncated offer.
            if(w.aClass46Array798!=null)for(Class46 child:w.aClass46Array798)if(child!=null)pending.add(child);
        }
        return result;
    }
    private List<ContentGuard> guardGroup(int group) {
        Set<Class46> widgets=groupWidgets(group);if(widgets==null)return null;
        List<ContentGuard> guards=new ArrayList<>();for(Class46 w:widgets)guards.add(new ContentGuard(w));return guards;
    }
    private boolean guardValid(List<ContentGuard> guards,int group) {
        if(guards==null)return false;
        Set<Class46> current=groupWidgets(group);
        if(current==null||current.size()!=guards.size())return false;
        for(ContentGuard g:guards)if(!current.contains(g.widget)||!g.valid())return false;
        return true;
    }
    private String nodeLabel(Widget w) {
        Class46 n=w.nativeWidget;
        if(w.item>=0 && Exception_Sub1.aClass255_112!=null) {
            ObjType definition=Exception_Sub1.aClass255_112.getItemDefinitions(-67,w.item);
            if(definition!=null && definition.name!=null) return plain(definition.name);
        }
        String label=InterfaceRegistry.component(n.anInt830);
        if(label.isEmpty()) label=n.anInt774==4?"Text":n.anInt774==6?"Model":"Component "+(n.anInt830&65535);
        if(n.anInt704>=0) label+=" · slot "+(n.anInt704+1);
        return label;
    }
    private void indexVisible() {
        if(indexedVisible==visible)return;
        indexedVisible=visible;visibleIndex.clear();blockers.clear();
        for(int i=0;i<visible.size();i++){Widget w=visible.get(i);w.order=i;visibleIndex.put(w.nativeWidget,w);if(w.blocking)blockers.add(w);}
    }
    private void publishUi() {
        indexVisible();
        List<UiFrameSnapshot.Node> nodes=new ArrayList<>();
        for(int i=0;i<visible.size();i++) {
            Widget w=visible.get(i); Class46 n=w.nativeWidget;
            if(n.aBoolean813 || w.handle==0) continue;
            UiFrameSnapshot.Role role=w.scene?UiFrameSnapshot.Role.SCENE
                :n.anInt765==Class348_Sub45.anInt7102?UiFrameSnapshot.Role.MAP
                :n.anInt765==Class290.anInt3717?UiFrameSnapshot.Role.MINIMAP
                :w.item>=0 || (n.anInt704>=0 && InterfaceRegistry.inventory(n.anInt830))?UiFrameSnapshot.Role.ITEM
                :slider(w)?UiFrameSnapshot.Role.SLIDER:w.scroll?UiFrameSnapshot.Role.SCROLL:n.anInt774==0?UiFrameSnapshot.Role.CONTAINER
                :n.anInt774==4?UiFrameSnapshot.Role.TEXT:n.anInt774==6?UiFrameSnapshot.Role.MODEL
                :w.interactive?UiFrameSnapshot.Role.BUTTON:UiFrameSnapshot.Role.IMAGE;
            List<UiFrameSnapshot.Action> actions=new ArrayList<>();
            for(int op=0;op<10;op++) {
                String name=Class368.method3561(op,n,true);
                if(name!=null) actions.add(new UiFrameSnapshot.Action(op+1,plain(name)));
            }
            if(client.method105(n).method3305(0)) actions.add(new UiFrameSnapshot.Action(0,"Continue"));
            if(pressOnly(w)) actions.add(new UiFrameSnapshot.Action(-1,"Activate native control"));
            String text="";
            if(n.anInt774==4 && n.anObjectArray822==null && InterfaceRegistry.textAllowed(n.anInt830>>>16))
                text=plain(w.renderedText!=null?w.renderedText:n.aString792==null?"":n.aString792);
            long parent=w.parent==null?0:identities.getOrDefault(w.parent,0L);
            nodes.add(new UiFrameSnapshot.Node(w.handle,parent,w.version,n.anInt830,n.anInt704,n.anInt774,n.anInt765,
                w.item,w.quantity,i,new UiFrameSnapshot.Bounds(w.x,w.y,w.width,w.height),
                new UiFrameSnapshot.Bounds(w.left,w.top,w.right-w.left,w.bottom-w.top),role,nodeLabel(w),
                InterfaceRegistry.name(n.anInt830>>>16),InterfaceRegistry.family(n.anInt830>>>16),text,
                w.blocking,movable(w),eligibleNode(w),actions));
        }
        MobileBridge.publishUi(new UiFrameSnapshot(++frameSerial,revision,viewport,nodes,status,r.aBoolean9722,moveSource!=null));
    }
    private Widget topAt(int x, int y) {
        for (int i = visible.size() - 1; i >= 0; i--) {
            Widget w = visible.get(i);
            if (w.contains(x, y) && !w.nativeWidget.aBoolean813 && (w.interactive || w.blocking)) return w;
        }
        return null;
    }
    private Widget find(Class46 nativeWidget) {
        indexVisible();
        if(visibleIndex.containsKey(nativeWidget))return visibleIndex.get(nativeWidget);
        for (int i = visible.size() - 1; i >= 0; i--) if (visible.get(i).nativeWidget == nativeWidget) return visible.get(i);
        return null;
    }
    private Widget parentOf(Widget node) {
        if (node.parent != null) return find(node.parent);
        // Static children in legacy/synthetic snapshots. Never infer ancestry from overlap.
        if (node.nativeWidget.anInt834 == -1) return null;
        for (int i = visible.indexOf(node) - 1; i >= 0; i--) {
            Widget p = visible.get(i);
            if (p.nativeWidget.anInt830 == node.nativeWidget.anInt834 && p.nativeWidget != node.nativeWidget) return p;
        }
        return null;
    }
    private boolean descendantOf(Widget child, Widget ancestor) {
        Set<Class46> visited = Collections.newSetFromMap(new IdentityHashMap<Class46, Boolean>());
        for (Widget w = child; w != null && visited.add(w.nativeWidget); w = parentOf(w))
            if (w.nativeWidget == ancestor.nativeWidget) return true;
        return false;
    }
    public GestureRecognizer.Target hit(double x, double y) {
        if (viewport == null || !viewport.contains(x, y) || !menu.isEmpty() || inspecting) return null;
        MobileChrome.Frame chrome=MobileChrome.frame();
        if(chrome!=null&&chrome.viewport.sameGeometry(viewport)&&chrome.viewport.revision==viewport.revision) {
            long local=chrome.at(viewport.nativeX(x),viewport.nativeY(y));
            if(local!=0){Hit h=new Hit(++token,null,null,GestureRecognizer.Kind.CONTROL);h.chromeToken=local;h.chromeGeneration=chrome.generation;h.downX=x;h.downY=y;owners.put(h.token,h);return new GestureRecognizer.Target(h.token,GestureRecognizer.Kind.CONTROL);}
        }
        Widget target = topAt(viewport.logicalX(x), viewport.logicalY(y)), scroller = null;
        Set<Class46> visited = Collections.newSetFromMap(new IdentityHashMap<Class46, Boolean>());
        for (Widget w = target; w != null && visited.add(w.nativeWidget); w = parentOf(w)) {
            if (w.scroll) { scroller = w; break; }
            if (w.blocking) break;
        }
        GestureRecognizer.Kind kind = target != null && target.scene ? GestureRecognizer.Kind.WORLD
            : target != null && target.nativeWidget.anInt765 == Class348_Sub45.anInt7102 ? GestureRecognizer.Kind.MAP
            : scroller != null ? GestureRecognizer.Kind.SCROLL : GestureRecognizer.Kind.CONTROL;
        if (target == null && (gameState == 3 || gameState == 10)) return null;
        Hit h = new Hit(++token, target, scroller, kind); h.downX = x; h.downY = y; owners.put(h.token, h);
        return new GestureRecognizer.Target(h.token, kind);
    }
    public boolean valid(GestureRecognizer.Target target) {
        if (target == null || viewport == null) return false;
        Hit h = owners.get(target.token);
        if (h == null || !viewport.contains(h.downX, h.downY)) return false;
        if(h.chromeToken!=0) {MobileChrome.Frame f=MobileChrome.frame();return f!=null&&f.generation==h.chromeGeneration&&f.viewport.sameGeometry(viewport);}
        Widget captured = h.scrolling ? h.scroller : h.target;
        if (!stillVisible(captured)) return false;
        // Scripts can move a surface between a paint and the next input cycle. Do not click stale coordinates.
        Set<Class46> chain=Collections.newSetFromMap(new IdentityHashMap<Class46,Boolean>());
        for(Widget w=captured;w!=null&&chain.add(w.nativeWidget);w=parentOf(w)) {
            if(!w.geometryCurrent())return false;
            if(!h.scrolling&&(w.nativeWidget.anInt747!=w.nativeScrollX||w.nativeWidget.anInt755!=w.nativeScrollY))return false;
        }
        Widget top = topAt(viewport.logicalX(h.downX), viewport.logicalY(h.downY));
        if (captured == null) return top == null;
        if (!h.scrolling) return top != null && top.nativeWidget == captured.nativeWidget;
        return top != null && descendantOf(top, captured) && !newBlockerAbove(captured, top);
    }
    private boolean newBlockerAbove(Widget ancestor, Widget top) {
        // A captured scroll surface cannot hand ownership to a newly opened blocking descendant.
        Set<Class46> visited = Collections.newSetFromMap(new IdentityHashMap<Class46, Boolean>());
        for (Widget w = top; w != null && w.nativeWidget != ancestor.nativeWidget && visited.add(w.nativeWidget); w = parentOf(w))
            if (w.blocking) return true;
        return false;
    }
    private boolean stillVisible(Widget captured) {
        if (captured == null) return gameState != 3 && gameState != 10;
        Widget w = find(captured.nativeWidget);
        return w != null && captured.geometryCurrent() && w.nativeWidget.anInt830==captured.packedId && w.nativeWidget.anInt704==captured.childIndex && w.item == captured.item && w.quantity == captured.quantity
            && w.nativeWidget.anInt812 == captured.item && w.nativeWidget.anInt781 == captured.quantity
            && w.parent == captured.parent && w.left == captured.left && w.top == captured.top
            && w.right == captured.right && w.bottom == captured.bottom && !w.nativeWidget.aBoolean813;
    }
    private boolean releaseEligible(Hit h, double x, double y) {
        if (h == null || !viewport.contains(x, y)) return false;
        if(h.chromeToken!=0){MobileChrome.Frame f=MobileChrome.frame();return f!=null&&f.generation==h.chromeGeneration&&f.at(viewport.nativeX(x),viewport.nativeY(y))==h.chromeToken;}
        Widget top = topAt(viewport.logicalX(x), viewport.logicalY(y));
        return h.target == null ? top == null
            : top != null && top.nativeWidget == h.target.nativeWidget && h.target.contains(viewport.logicalX(x), viewport.logicalY(y));
    }
    private void armContext() {
        gestures.cancel(); contextNextTap = !contextNextTap;
        status = contextNextTap ? "Tap a target to choose its actions; Actions again cancels" : "Normal tapping restored";
    }
    public void tap(GestureRecognizer.Target t, double x, double y) {
        if (!valid(t) || !viewport.contains(x, y)) return;
        Hit h = owners.get(t.token);
        if(h!=null&&h.chromeToken!=0){if(releaseEligible(h,x,y))MobileChrome.activate(h.chromeToken,h.chromeGeneration);return;}
        if (h == null || !stillVisible(h.target) || !releaseEligible(h, x, y)) return;
        lastTarget = h; pointerX = x; pointerY = y; textSession++;
        if (moveSource!=null) {
            if(h.target!=null&&h.target.handle!=0)handleNodeCommand(new MobileBridge.Command("moveDest",0,0,0,h.target.version,"",h.target.handle));
            else {moveSource=null;status="Move cancelled: choose a slot in the same inventory.";publish();}
            return; // A destination/cancellation tap never becomes a walk, use, drop or equip action.
        }
        if (contextNextTap) { contextNextTap = false; context(t, x, y); return; }
        input.mobileClick(viewport.nativeX(x), viewport.nativeY(y), false);
    }
    public void context(GestureRecognizer.Target t, double x, double y) {
        if (!valid(t) || !viewport.contains(x, y)) return;
        Hit captured = owners.get(t.token);
        if (!releaseEligible(captured, x, y)) return;
        if(captured.chromeToken!=0){MobileChrome.activate(captured.chromeToken,captured.chromeGeneration);return;}
        lastTarget = captured; pointerX = x; pointerY = y;
        input.mobileClick(viewport.nativeX(x), viewport.nativeY(y), true);
    }
    public void scroll(GestureRecognizer.Target target, double dx, double dy) {
        Hit h = owners.get(target.token);
        if (h == null || h.scroller == null) return;
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !stillVisible(h.scroller)) return;
        h.scrolling = true;
        Class46 w = h.scroller.nativeWidget;
        double speed = MobileConfig.integer("void.mobile.scrollSpeed", 100, 25, 300) / 100.0;
        h.scrollX += dx * viewport.logicalWidth / viewport.width * speed;
        h.scrollY += dy * viewport.logicalHeight / viewport.height * speed;
        int ix = (int) h.scrollX, iy = (int) h.scrollY; // Truncate symmetrically; keep fractional displacement.
        h.scrollX -= ix; h.scrollY -= iy;
        int oldX = w.anInt747, oldY = w.anInt755;
        w.anInt747 = clamp(w.anInt747 - ix, 0, Math.max(0, w.anInt698 - w.anInt709));
        w.anInt755 = clamp(w.anInt755 - iy, 0, Math.max(0, w.anInt791 - w.anInt789));
        if (w.anInt747 == 0 && h.scrollX > 0 || w.anInt747 == Math.max(0, w.anInt698 - w.anInt709) && h.scrollX < 0) h.scrollX = 0;
        if (w.anInt755 == 0 && h.scrollY > 0 || w.anInt755 == Math.max(0, w.anInt791 - w.anInt789) && h.scrollY < 0) h.scrollY = 0;
        if (oldX != w.anInt747 || oldY != w.anInt755) Class251.method1916(-9343, w);
    }
    public void camera(double dx, double dy) {
        if (gameState != 10 || Class348_Sub40_Sub21.anInt9282 != 1 || !menu.isEmpty()) return;
        double sensitivity = MobileConfig.integer("void.mobile.cameraSensitivity", 100, 25, 250) / 100.0;
        if (Boolean.getBoolean("void.mobile.reducedMotion")) sensitivity *= 0.5;
        dx *= sensitivity; dy *= sensitivity * (Boolean.getBoolean("void.mobile.invertCamera") ? -1 : 1);
        Class314.aFloat3938 = (float) ((Class314.aFloat3938 - dx * 16 + 16384 * 100) % 16384);
        Class76.aFloat1287 = Math.max(1024, Math.min(3072, Class76.aFloat1287 + (float) dy * 10));
    }
    public void zoom(double delta) {
        if (gameState != 10 || Class348_Sub40_Sub21.anInt9282 != 1 || !menu.isEmpty()) return;
        zoomRemainder += delta;
        int steps = (int) (zoomRemainder / 12);
        if (steps != 0) { Class320.zoomStep = clamp(Class320.zoomStep + steps * Loader.ZOOM_OFFSET_STEP, -2000, 2000); zoomRemainder -= steps * 12; }
    }
    private Widget mapSurface() {
        for(int i=visible.size()-1;i>=0;i--) {
            Widget w=visible.get(i);
            if(w.nativeWidget.anInt765==Class348_Sub45.anInt7102 && eligibleNode(w))return w;
        }
        return null;
    }
    public void map(double dx,double dy) {
        if(viewport==null || mapSurface()==null || !menu.isEmpty() || !Double.isFinite(dx) || !Double.isFinite(dy) || Class75.aFloat1249<=0)return;
        mapXResidual+=dx*viewport.logicalWidth/viewport.width*2/Class75.aFloat1249;
        mapYResidual+=dy*viewport.logicalHeight/viewport.height*2/Class75.aFloat1249;
        int x=(int)mapXResidual,y=(int)mapYResidual;mapXResidual-=x;mapYResidual-=y;
        if(x!=0)Class64_Sub3.method689((byte)-59,Class348_Sub36.anInt6992-x);
        if(y!=0)Class286_Sub8.method2170(Class245.anInt3170+y,(byte)3);
    }
    public void mapZoom(double delta) {
        if(mapSurface()==null || !menu.isEmpty() || !Double.isFinite(delta))return;
        mapZoomResidual+=delta;int steps=(int)(mapZoomResidual/24);mapZoomResidual-=steps*24;
        if(steps!=0)Class75.aFloat1249=Math.max(3,Math.min(16,Class75.aFloat1249+steps));
    }
    public void cancel() {
        // Release without invoking the legacy release-to-activate or drop path.
        Class289.aClass46_3701 = null; Class331.aClass46_4130 = null;
        Class318_Sub1_Sub3_Sub4.aClass46_10336 = null; Class300.aBoolean3819 = false;
        if (input != null) input.mobileCancel();
        if (Class182.aClass346_2449 instanceof Class346_Sub1) ((Class346_Sub1) Class182.aClass346_2449).mobileCancel();
        textPending = ""; keysPending.clear(); textInput.cancel("Input context cancelled; draft retained");
        mapXResidual=mapYResidual=mapZoomResidual=0;
    }
    private void cancelAll() { MobileMetrics.cancel(); contextNextTap = false; gestures.cancel(); closeMenu(); inspecting = false; owners.clear(); lastTarget = null; moveSource=null; }
    private void closeMenu() { menu=Collections.emptyList(); selected=-1; proxyMenuWidget=null; menuGuard=Collections.emptyList(); }
    static boolean openMenu(int x, int y) {
        if (!MobileConfig.enabled()) return false;
        MobileRuntime rt = INSTANCE;
        List<Class348_Sub42_Sub12> nativeEntries = entries();
        List<Entry> result = new ArrayList<>();
        Collections.reverse(nativeEntries);
        for (Class348_Sub42_Sub12 entry : nativeEntries) {
            if (result.size() >= 500) break;
            result.add(new Entry(result.size(), entry));
        }
        rt.gestures.cancel(); rt.contextNextTap = false;
        rt.proxyMenuWidget=null;
        Widget pointed=rt.viewport==null?null:rt.topAt(x,y);
        if(pointed!=null && rt.eligibleNode(pointed) && rt.movable(pointed) && pointed.item>=0)result.add(new Entry(result.size(),pointed));
        rt.menuGuard=pointed==null?Collections.emptyList():rt.guardGroup(pointed.nativeWidget.anInt830>>>16);
        rt.menu = result; rt.menuX = x; rt.menuY = y; rt.selected = -1; rt.menuSerial++;
        rt.menuDeadline = Long.MAX_VALUE;
        rt.textPending = ""; rt.keysPending.clear();
        rt.publish(); return true;
    }
    /** Called after the native client rebuilds/sorts candidates, not on the browser/EDT thread. */
    static boolean afterMenuBuild() {
        if (!MobileConfig.enabled()) return false;
        MobileRuntime rt = INSTANCE;
        if (rt.menu.isEmpty()) return rt.inspecting || MobileBridge.suspended();
        if (rt.selected >= 0) {
            Widget proxy=rt.proxyMenuWidget;
            boolean guarded=rt.menuGuard!=null && (proxy==null || (rt.eligibleNode(proxy) && rt.guardValid(rt.menuGuard,proxy.nativeWidget.anInt830>>>16)));
            if(proxy==null && rt.menuGuard!=null && !rt.menuGuard.isEmpty()) guarded=rt.guardValid(rt.menuGuard,rt.menuGuard.get(0).widget.anInt830>>>16);
            Entry chosen = rt.menu.get(rt.selected);
            if(chosen.moveWidget!=null) {
                Widget source=chosen.moveWidget;
                boolean allowed=guarded&&rt.eligibleNode(source)&&rt.movable(source)&&rt.root==r.anInt9721&&rt.gameState==Class240.anInt4674;
                rt.closeMenu();
                if(allowed)rt.handleNodeCommand(new MobileBridge.Command("moveSource",0,0,0,source.version,"",source.handle));
                else {rt.status="That item changed; open its action list again";rt.publish();}
                return true;
            }
            Class348_Sub42_Sub12 matched = null;
            for (Class348_Sub42_Sub12 current : proxy==null?entries():MobileNativeActions.forWidget(proxy.nativeWidget)) if (chosen.signature.equals(Entry.signature(current))) { matched = current; break; }
            rt.closeMenu(); rt.textSession++;
            if (guarded && matched != null && rt.root == r.anInt9721 && rt.gameState == Class240.anInt4674)
                MobileNativeOperations.action(matched,rt.menuX,rt.menuY);
            else rt.status = "That action changed; open the menu again";
            rt.publish();
        }
        return true; // Consume this cycle even after selection/dismissal. No world click-through.
    }
    private static List<Class348_Sub42_Sub12> entries() {
        List<Class348_Sub42_Sub12> result = new ArrayList<>();
        if (Class348_Sub40_Sub4.aClass262_9111 == null) return result;
        Class312 iterator = new Class312(Class348_Sub40_Sub4.aClass262_9111);
        for (Node n = iterator.method2327((byte) -53); n != null && result.size() < 500; n = iterator.method2329(10))
            if (n instanceof Class348_Sub42_Sub12) result.add((Class348_Sub42_Sub12) n);
        return result;
    }
    private void publish() {
        publishUi();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nativeHud", MobileNativeHud.status()); result.put("ready", viewport != null); result.put("status", status); result.put("revision", revision);
        result.put("viewport", viewport); result.put("menuId", menuSerial);
        result.put("hostSize", MobileLauncher.hostSize()); result.put("textSession", textSession);
        result.put("textAck", textAck); result.put("textAccepted", textAccepted);
        result.put("displayScale", MobileLauncher.displayScale());
        if (!MobileConfig.browser()) {
            int scale = MobileConfig.integer("void.mobile.gameScale", 100, 100, 200);
            result.put("gameScale", scale);
            result.put("scaleMode", Class348_Sub8.aHa6654 == null ? "Waiting for renderer"
                : scale == 100 ? "Game size 100%"
                : Class348_Sub8.aHa6654.supportsNativeInterfaceScaling() ? "Interface-only scale " + scale + "%"
                : "Whole-game scale " + scale + "% (software fallback)");
        }
        result.put("contextNextTap", contextNextTap);
        List<Map<String, Object>> choices = new ArrayList<>();
        for (Entry e : menu) { Map<String, Object> row = new LinkedHashMap<>(); row.put("id", e.index); row.put("label", e.label); choices.add(row); }
        result.put("menu", choices); result.put("inspecting", inspecting); result.put("root", root);
        if (inspecting) {
            List<Map<String, Object>> widgets = new ArrayList<>();
            for (Widget w : visible) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", w.nativeWidget.anInt830); row.put("child", w.nativeWidget.anInt704);
                row.put("type", w.nativeWidget.anInt774); row.put("contentType", w.nativeWidget.anInt765);
                row.put("bounds", new int[] {w.left, w.top, w.right - w.left, w.bottom - w.top}); row.put("scroll", w.scroll);
                widgets.add(row);
            }
            result.put("widgets", widgets); // No chat, credentials or editable field text is exported.
        }
        MobileBridge.publish(gson.toJson(result), revision);
    }
    private static boolean validText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c) || !Class122.method1089(-125, c)) return false;
        }
        return true;
    }
    private static String plain(String value) { return value == null ? "Action" : value.replaceAll("<[^>]*>", "").trim(); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
