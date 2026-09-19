import com.voidclient.mobile.*;
import com.google.gson.Gson;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Adversarial JR2–JR5 native-hook tests. Synthetic fixtures; not server or phone certification. */
public final class MobileMilestoneRegression {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    static Object get(Object o,String n)throws Exception{return field(o.getClass(),n).get(o);}
    static void set(Object o,String n,Object v)throws Exception{field(o.getClass(),n).set(o,v);}
    static Object call(Object o,String n,Class<?>[] t,Object...a)throws Exception{Method m=o.getClass().getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(o,a);}
    static Class46 widget(int id,int child,boolean scroll,boolean blocking){Class46 w=new Class46();w.anInt830=id;w.anInt704=child;w.anInt834=-1;w.anInt774=0;w.anInt765=0;w.anInt709=200;w.anInt789=200;w.anInt791=scroll?600:0;w.aBoolean682=true;w.aBoolean776=blocking;w.anInt794=-98765;return w;}
    static Object captured(Class46 w,int l,int t,int r,int b)throws Exception{Constructor<?> c=Class.forName("MobileRuntime$Widget").getDeclaredConstructor(Class46.class,int.class,int.class,int.class,int.class);c.setAccessible(true);return c.newInstance(w,l,t,r,b);}
    static MobileRuntime runtime(int ratio)throws Exception{Constructor<MobileRuntime> c=MobileRuntime.class.getDeclaredConstructor();c.setAccessible(true);MobileRuntime rt=c.newInstance();set(rt,"viewport",new ViewportState(1,0,0,400,600,400/ratio,600/ratio,400/ratio,600/ratio));set(rt,"gameState",3);return rt;}
    static void visible(MobileRuntime rt,Object...w)throws Exception{set(rt,"visible",new ArrayList<>(Arrays.asList(w)));}
    public static void main(String[] args)throws Exception{
        System.setProperty("void.mobile","true");ownership();scroll();scaling();gestures();editing();catalogue();nativeActions();painting();
        String report="JR2–JR5 adversarial/native-model assertions: "+checks+" passed\nSynthetic widget/input fixtures, no live cache/server/Android claim.\n";
        Files.createDirectories(Paths.get("build/reports/mobile"));Files.write(Paths.get("build/reports/mobile/milestones-native.txt"),report.getBytes(StandardCharsets.UTF_8));System.out.print(report);
    }
    static void ownership()throws Exception{
        MobileRuntime rt=runtime(1);Class46 a=widget(65536,-1,true,false),b=widget(65537,-1,false,false);
        Object wa=captured(a,0,0,200,200),wb=captured(b,0,0,200,200);visible(rt,wa,wb);
        check(rt.hit(50,50).kind==GestureRecognizer.Kind.CONTROL,"A1 unrelated background cannot own foreground scroll");
        set(wb,"parent",a);visible(rt,wa,wb);check(rt.hit(50,50).kind==GestureRecognizer.Kind.SCROLL,"actual parent scroller owns child gesture");
        rt=runtime(1);a=widget(65536,-1,false,false);wa=captured(a,0,0,100,100);visible(rt,wa);GestureRecognizer.Target target=rt.hit(50,50);
        b=widget(65537,-1,false,true);visible(rt,wa,captured(b,0,0,100,100));check(!rt.valid(target),"A2 later blocking overlay invalidates capture");
        rt=runtime(1);a=widget(65536,-1,false,false);b=widget(65537,-1,false,false);visible(rt,captured(a,0,0,100,100),captured(b,100,0,200,100));
        Canvas canvas=new Canvas();canvas.setSize(400,600);Class373_Sub1 mouse=new Class373_Sub1(canvas,true);set(rt,"input",mouse);
        GestureRecognizer recognizer=new GestureRecognizer(rt,10,550);recognizer.down(1,98,50,0,1);recognizer.up(1,102,50,100,1);
        System.clearProperty("void.mobile");mouse.method3589(0);check(mouse.method3596(0)==null,"A3 neighbour below slop cannot receive release");System.setProperty("void.mobile","true");
        target=rt.hit(20,20);a.anInt704=5;check(!rt.valid(target),"slot identity change invalidates captured widget even with same item");mouse.method3592(0);
        rt=runtime(1);a=widget(65536,-1,false,false);wa=captured(a,0,0,100,100);visible(rt,wa);target=rt.hit(20,20);a.aBoolean813=true;check(!rt.valid(target),"hidden target invalidated");
        rt=runtime(1);a=widget(65536,-1,false,false);wa=captured(a,0,0,100,100);visible(rt,wa);target=rt.hit(20,20);a.anInt800++;check(!rt.valid(target),"script movement between paint and release cancels stale-coordinate tap");
    }
    static void scroll()throws Exception{
        MobileRuntime rt=runtime(2);Class46 a=widget(65536,-1,true,false);visible(rt,captured(a,0,0,200,200));GestureRecognizer.Target t=rt.hit(50,50);
        for(int sign:new int[]{1,-1}){a.anInt755=100;for(int i=0;i<10;i++)rt.scroll(t,0,sign);int small=a.anInt755;a.anInt755=100;rt.scroll(t,0,10*sign);check(small==100-5*sign&&a.anInt755==small,"A4 symmetric partition-independent scrolling "+sign);}
        a.anInt755=0;rt.scroll(t,0,1);rt.scroll(t,0,-2);check(a.anInt755==1,"boundary residual does not stick on reversal");
        int before=a.anInt755;rt.scroll(t,0,Double.NaN);check(a.anInt755==before,"non-finite scroll rejected");
    }
    static void scaling(){
        for(int width:new int[]{256,320,390,844,1920})for(int height:new int[]{192,390,736,1080})for(int percent:new int[]{100,125,150,175,200}){
            double scale=UniformScale.effective(width,height,percent/100.0);
            check(scale<=percent/100.0+.00001,"scale never exceeds request");check(width/scale>=256-.001&&height/scale>=192-.001,"minimum raster uniformly respected");
        }
        double effective=UniformScale.effective(390,736,2);check(Math.abs(effective-390.0/256)<.000001,"A7 uniform effective scale at narrow viewport");
    }
    static void gestures(){
        class Sink implements GestureRecognizer.Sink{int taps,contexts,maps,cameras,zooms;public GestureRecognizer.Target hit(double x,double y){return new GestureRecognizer.Target(1,GestureRecognizer.Kind.MAP);}public boolean valid(GestureRecognizer.Target t){return true;}public void tap(GestureRecognizer.Target t,double x,double y){taps++;}public void context(GestureRecognizer.Target t,double x,double y){contexts++;}public void scroll(GestureRecognizer.Target t,double dx,double dy){}public void camera(double x,double y){cameras++;}public void zoom(double v){}public void map(double x,double y){maps++;}public void mapZoom(double v){zooms++;}public void cancel(){}}
        Sink s=new Sink();GestureRecognizer g=new GestureRecognizer(s,8,550);g.down(1,10,10,0,1);g.move(1,50,10,20,1);g.up(1,50,10,40,1);check(s.maps>0&&s.cameras==0&&s.taps==0,"map drag never becomes camera or tap");
        g.down(1,10,10,100,1);g.down(2,50,10,120,1);g.move(2,80,10,150,1);g.up(1,10,10,200,1);g.up(2,80,10,220,1);check(s.zooms>0&&s.taps==0,"optional map pinch has no residual tap");
        g.down(1,10,10,250,1);g.move(1,11,10,260,2);g.up(1,11,10,270,2);check(s.taps==0,"viewport transition cancels gesture");
    }
    static void editing()throws Exception{
        MobileRuntime rt=(MobileRuntime)field(MobileRuntime.class,"INSTANCE").get(null);set(rt,"textSession",100L);set(rt,"root",-1);set(rt,"gameState",10);r.anInt9721=-1;Class240.anInt4674=10;
        MobileTextInput input=(MobileTextInput)get(rt,"textInput");input.cancel("test reset");MobileBridge.cancel();MobileBridge.drain();
        Canvas canvas=new Canvas();Class297.aString3782=System.getProperty("java.vendor");Class346_Sub1 keyboard=new Class346_Sub1(canvas);
        int id=MobileBridge.insertText("hello",100);MobileBridge.Command command=MobileBridge.drain().get(0);check(input.accept(command,100,true),"valid text accepted for dispatch");input.pump(keyboard,100);
        check(MobileBridge.editReceipt(id).state!=EditReceipts.State.DELIVERED,"queueing is not reported as delivery");keyboard.method2695(67);while(keyboard.method2697(0)!=null){}MobileRuntime.afterClientCycle();
        check(MobileBridge.editReceipt(id).state==EditReceipts.State.DELIVERED&&MobileBridge.editReceipt(id).delivered==5,"receipt acknowledged after actual native keyboard consumption");
        int next=MobileBridge.editKey(KeyEvent.VK_TAB,100);command=MobileBridge.drain().get(0);input.accept(command,100,true);input.pump(keyboard,100);keyboard.method2695(67);while(keyboard.method2697(0)!=null){}MobileRuntime.afterClientCycle();
        check(MobileBridge.editReceipt(next).state==EditReceipts.State.DELIVERED&&MobileBridge.editReceipt(next).nextSession==101,"A5 acknowledged Tab supplies exactly next editing session");
        id=MobileBridge.insertText("next",101);check(input.accept(MobileBridge.drain().get(0),101,true),"insertion after acknowledged Next remains valid");input.cancel("test");
        id=MobileBridge.insertText("stale",100);check(!input.accept(MobileBridge.drain().get(0),101,true)&&MobileBridge.editReceipt(id).state==EditReceipts.State.REJECTED,"unrelated or old editor session is not silently rebound");
        id=MobileBridge.insertText("\ud83d\ude00",101);check(!input.accept(MobileBridge.drain().get(0),101,true)&&MobileBridge.editReceipt(id).state==EditReceipts.State.REJECTED,"A6 unsupported character rejected without consuming editor draft");
        MobileBridge.action("key",KeyEvent.VK_TAB,MobileBridge.revision());command=MobileBridge.drain().get(0);command=new MobileBridge.Command(command.type,command.id,0,0,101,"");
        check(input.accept(command,101,true),"legacy browser command supported");input.pump(keyboard,101);keyboard.method2695(67);while(keyboard.method2697(0)!=null){}MobileRuntime.afterClientCycle();check((Long)get(rt,"textSession")==102L,"legacy key completes instead of blocking future input");
        id=MobileBridge.insertText("cancelled",102);input.accept(MobileBridge.drain().get(0),102,true);input.pump(keyboard,102);keyboard.method2695(67);keyboard.mobileCancel();input.cancel("cancel");check(keyboard.method2697(0)==null,"cancellation clears already-transferred native event queue");check(MobileBridge.editReceipt(id).state==EditReceipts.State.CANCELLED,"cancellation retained in matching receipt");
        keyboard.mobileCancel();
    }
    static void catalogue()throws Exception{
        check(InterfaceRegistry.groupCount()==408,"pinned public metadata group count");check(InterfaceRegistry.inventory(149<<16),"inventory component mapped from public source");check(InterfaceRegistry.inventory((762<<16)|93),"bank component mapped from public source");check(!InterfaceRegistry.known(65000),"unmapped group stays unknown");
        List<Long> ids=new ArrayList<>();for(long i=1;i<=28;i++)ids.add(i);
        for(int w:new int[]{240,320,390,844,1024})for(String profile:new String[]{"portrait","landscape","expanded"}){
            List<ResponsivePanelLayout.Cell> cells=ResponsivePanelLayout.grid(ids,w,96,140,profile);check(cells.size()==28,"every slot gets exactly one identity-preserving cell");
            for(int i=0;i<cells.size();i++){ResponsivePanelLayout.Cell c=cells.get(i);check(c.token==i+1&&c.bounds.x>=0&&c.bounds.x+c.bounds.width<=w,"cell within width and identity preserved");}
        }
        UiFrameSnapshot.Node n=new UiFrameSnapshot.Node(1,0,2,149<<16,0,5,0,999,99,0,new UiFrameSnapshot.Bounds(0,0,32,32),new UiFrameSnapshot.Bounds(0,0,32,32),UiFrameSnapshot.Role.ITEM,"SECRET_ITEM_NAME","inventory","inventory","PRIVATE_TEXT",false,true,true,Arrays.asList(new UiFrameSnapshot.Action(1,"SECRET_ACTION")));
        UiFrameSnapshot frame=new UiFrameSnapshot(1,1,new ViewportState(1,0,0,400,600,400,600,400,600),Arrays.asList(n),"PRIVATE_STATUS",false,false);String json=new Gson().toJson(frame.redactedCatalogue());check(!json.contains("SECRET")&&!json.contains("PRIVATE")&&!json.contains("quantity")&&!json.contains("999"),"manual catalogue excludes game content and text");
        InterfaceCatalogue.record(1,0,"PRIVATE_CACHE_BYTES".getBytes(StandardCharsets.UTF_8));json=new Gson().toJson(InterfaceCatalogue.snapshot());check(!json.contains("PRIVATE_CACHE_BYTES")&&json.contains("sha256"),"cache catalogue exports digest rather than bytes");
        Path p=Files.createTempDirectory("mobile-accessibility-").resolve("prefs.properties");AccessibilityPreferences a=new AccessibilityPreferences();a.textPercent=175;a.holdMillis=900;a.highContrast=true;a.save(p);AccessibilityPreferences b=AccessibilityPreferences.read(p);check(b.textPercent==175&&b.holdMillis==900&&b.highContrast,"accessibility settings persist independently");
    }
    static void nativeActions(){
        Class46 w=widget((149<<16),2,false,false);w.anInt774=5;w.anInt812=100;w.aStringArray833=new String[]{"Eat"};w.aString752="Test item";w.aClass348_Sub44_748=new Class348_Sub44(2,-1);r.aBoolean9722=false;
        int before=Class73.anInt4776;List<Class348_Sub42_Sub12> candidates=MobileNativeActions.forWidget(w);check(candidates.size()==1,"native builder exposes only the permitted option");check(Class73.anInt4776==before,"collecting candidates does not modify live menu");
        Class348_Sub42_Sub12 e=candidates.get(0);check(e.anInt9608==18&&e.aLong9605==1&&e.anInt9602==2&&e.anInt9607==(149<<16),"native candidate preserves opcode, operation, slot and interface");
        w.aClass348_Sub44_748=new Class348_Sub44(0,-1);check(MobileNativeActions.forWidget(w).isEmpty(),"removing native permission removes action without metadata grants");
    }
    static void painting()throws Exception{
        Class46 root=widget(149<<16,-1,true,false),child=widget(149<<16,0,false,false);root.anInt774=0;child.anInt774=5;child.anInt812=100;
        MobileRuntime.beginPaint();MobileRuntime.recordPaint(root,0,0,0,0,100,100);MobileRuntime.paintParent(root);MobileRuntime.recordPaint(child,0,200,0,200,32,100);MobileRuntime.paintParentEnd();MobileRuntime.endPaint(true);
        Object rt=field(MobileRuntime.class,"INSTANCE").get(null);List<?> list=(List<?>)get(rt,"painted");check(list.size()==2,"render traversal captures clipped children for alternative panel layout");check(get(list.get(1),"parent")==root,"render snapshot preserves actual parent identity");
        long token=(Long)get(list.get(1),"handle");MobileRuntime.beginPaint();MobileRuntime.recordPaint(child,0,0,0,0,32,32);MobileRuntime.endPaint(true);list=(List<?>)get(rt,"painted");check((Long)get(list.get(0),"handle")==token,"native instance identity stable across relayout");
        MobileRuntime.beginPaint();MobileRuntime.recordPaint(child,0,0,0,0,32,32);MobileRuntime.endPaint(false);check(((List<?>)get(rt,"painted")).isEmpty(),"failed render does not publish partially eligible controls");
    }
}
