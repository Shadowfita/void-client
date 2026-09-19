import com.voidclient.mobile.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Direct game-canvas interaction tests. All game state is synthetic; dispatch is recorded, not sent. */
public final class NativeTouchRegression {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static MobileBridge.Command cmd(String type,int id,double x,double y,long menu){return new MobileBridge.Command(type,id,x,y,menu,"");}
    static CanvasActionMenu.Frame paint(ViewportState v){return CanvasActionMenu.layout(v,s->s.length()*8,18,1,v.nativeWidth/2,100);}
    static int event(String type,int id,double x,double y){return CanvasActionMenu.handle(cmd(type,id,x,y,CanvasActionMenu.frame().menuId));}
    static int click(Rectangle r){double x=r.getCenterX(),y=r.getCenterY();event("menuDown",1,x,y);return event("menuUp",1,x,y);}
    static void menus(){
        List<String> labels=new ArrayList<>();for(int i=0;i<35;i++)labels.add(i==5?"Examine an unusually long unbrokenitemnamethatmustwrapwithoutchangingwhichnativeoperationisselected":"Native action "+i);
        for(int[] dims:new int[][]{{320,568},{390,844},{844,390},{1024,768},{240,240}})for(int scale:new int[]{1,2}){
            ViewportState v=new ViewportState(1,0,0,dims[0],dims[1],dims[0]/scale,dims[1]/scale,dims[0]/scale,dims[1]/scale);
            CanvasActionMenu.open(11,labels);CanvasActionMenu.Frame f=paint(v);
            check(new Rectangle(0,0,v.nativeWidth,v.nativeHeight).contains(f.bounds),"menu remains within native raster at "+Arrays.toString(dims)+" / "+scale);
            check(f.rows.get(0).height*(double)v.height/v.nativeHeight>=48,"touch rows retain display size");
            check(f.rows.get(5).lines.size()>1,"long label wraps without losing native index");
            check(event("menuUp",1,f.body.getCenterX(),f.body.y+10)==CanvasActionMenu.NONE,"the long-press opening release cannot select");
            check(click(f.rowBounds(f.rows.get(0)))==0,"same-row down/up selects original operation zero");
            double x=f.body.getCenterX(),y=f.body.y+f.rows.get(0).height-1;
            event("menuDown",1,x,y);check(event("menuUp",1,x,y+2)==CanvasActionMenu.NONE,"two-pixel crossing cannot select neighbouring row");
            check(CanvasActionMenu.handle(cmd("menuDown",1,x,y,10))==CanvasActionMenu.NONE,"stale menu id rejected");
            event("menuDown",1,x,f.body.getCenterY());event("menuMove",1,x,f.body.y+2);check(event("menuUp",1,x,f.body.y+2)==CanvasActionMenu.NONE,"swipe never activates row");
            f=paint(v);check(f.scroll>0,"swipe scrolls native action list");
            List<UiFrameSnapshot.Node> nodes=CanvasActionMenu.nodes();check(!nodes.isEmpty(),"native action rows have Java semantic nodes");
            UiFrameSnapshot.Node old=nodes.get(0);event("menuWheel",1,0,0);f=paint(v);check(!CanvasActionMenu.current(old),"old semantic geometry invalid after scrolling");
            event("menuKey",KeyEvent.VK_END,0,0);f=paint(v);check(event("menuKey",KeyEvent.VK_ENTER,0,0)==labels.size()-1,"keyboard End/Enter chooses actual final operation");
            check(f.scroll==f.maxScroll(),"keyboard navigation reveals final action");
            check(click(f.cancel)==CanvasActionMenu.DISMISS,"Cancel reachable without scrolling back");
            event("menuDown",1,-2,-2);check(event("menuUp",1,-2,-2)==CanvasActionMenu.DISMISS,"outside tap dismisses without game click");
            event("menuDown",1,x,f.body.y+5);event("menuDown",2,x,f.body.y+8);event("menuUp",2,x,f.body.y+8);check(event("menuUp",1,x,f.body.y+5)==CanvasActionMenu.NONE,"second finger cancels action tap");
            event("menuDown",1,x,f.body.y+5);paint(new ViewportState(2,0,0,v.width,v.height,v.nativeWidth,v.nativeHeight,v.logicalWidth,v.logicalHeight));check(event("menuUp",1,x,f.body.y+5)==CanvasActionMenu.NONE,"layout revision invalidates held action");
            check(event("menuKey",KeyEvent.VK_ESCAPE,0,0)==CanvasActionMenu.DISMISS,"Escape closes native menu");CanvasActionMenu.close();
        }
        CanvasActionMenu.open(40,labels);check(CanvasActionMenu.handle(cmd("menuKey",KeyEvent.VK_ESCAPE,0,0,40))==CanvasActionMenu.DISMISS,"Escape also works before font/first paint");
        ViewportState v=new ViewportState(5,0,0,390,844,390,844,390,844);CanvasActionMenu.Frame f=paint(v);MobileBridge.drain();
        check(CanvasActionMenu.pointer("menuDown",1,f.body.x+5,f.body.y+5,390,844),"AWT producer consumes menu pointer");
        for(int i=0;i<1000;i++)CanvasActionMenu.pointer("menuMove",1,f.body.x+5,f.body.y+5-i%100,390,844);
        CanvasActionMenu.pointer("menuUp",1,f.body.x+5,f.body.y+5,390,844);List<MobileBridge.Command> queue=MobileBridge.drain();
        check(queue.size()==3&&queue.get(0).type.equals("menuDown")&&queue.get(1).type.equals("menuMove")&&queue.get(2).type.equals("menuUp"),"movement coalesces without losing press/release boundaries");
        UiFrameSnapshot.Node node=CanvasActionMenu.nodes().get(0);MobileBridge.drain();check(CanvasActionMenu.activate(node),"current native semantic action accepted");
        List<MobileBridge.Command> semantic=MobileBridge.drain();check(semantic.size()==1&&semantic.get(0).revision==40,"semantic command uses native menu id, not paint generation");
        CanvasActionMenu.Frame fallback=CanvasActionMenu.fallback(v);check(fallback.rows.isEmpty()&&CanvasActionMenu.nodes().size()==1,"missing fonts expose only visible cancellation, never invisible actions");
        check(click(fallback.cancel)==CanvasActionMenu.DISMISS,"missing fonts retain touch-only recovery");paint(v);
        Canvas canvas=new Canvas();canvas.setSize(390,844);Class373_Sub1 mouse=new Class373_Sub1(canvas,true);
        MouseEvent down=new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,1,MouseEvent.BUTTON3_DOWN_MASK,100,200,1,true,MouseEvent.BUTTON3);
        mouse.mousePressed(down);CanvasActionMenu.close();MouseEvent up=new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,2,0,100,200,1,true,MouseEvent.BUTTON3);mouse.mouseReleased(up);
        check(down.isConsumed()&&up.isConsumed(),"secondary release remains consumed after menu closes");
        try{Field p=Class373_Sub1.class.getDeclaredField("aClass262_7420");p.setAccessible(true);check(((Class262)p.get(mouse)).method1998(0)==0,"no raw game mouse events from native popup");}catch(Exception e){throw new RuntimeException(e);}
        MobileBridge.drain();CanvasActionMenu.close();
    }
    static void fonts()throws Exception{
        byte[] bytes=new byte[263];Arrays.fill(bytes,2,258,(byte)8);bytes[258]=12;bytes[261]=12;bytes[262]=4;
        Class143 metrics=new Class143(bytes),scaled=metrics.scaledForMobile(150);
        check(metrics.method1183(true,"ABC")==24&&scaled.method1183(true,"ABC")==36,"native font advances enlarge without mutating original cache metrics");
        check(scaled.anInt1988==18&&scaled.anInt1993==6,"native font ascent/descent scale consistently");
        Class207 glyph=new Class207();glyph.anInt2702=2;glyph.anInt2696=2;glyph.anInt2703=1;glyph.anInt2700=2;glyph.aByteArray2699=new byte[]{0,1,1,0};glyph.aByteArray2695=new byte[]{0,90,(byte)255,0};glyph.anIntArray2697=new int[]{0,0xffffff};
        Class207 g=MobileMenuFont.scale(glyph,200);check(g.anInt2702==4&&g.anInt2696==4&&g.anInt2703==2&&g.anInt2700==4,"glyph dimensions and bearings scale together");
        check(g.aByteArray2699[2]==1&&g.aByteArray2695[2]==90&&g.aByteArray2695[12]==(byte)255,"palette pixels and alpha preserved");
        g.aByteArray2699[0]=1;g.anIntArray2697[1]=0;check(glyph.aByteArray2699[0]==0&&glyph.anIntArray2697[1]==0xffffff,"scaled glyph does not mutate cache assets");
        check(MobileMenuFont.literal("<col=ff0000>Use").contains("<lt>"),"action labels cannot inject native markup");
        Class207[] glyphs=new Class207[256];Arrays.fill(glyphs,glyph);
        check(MobileMenuFont.safeGlyphSet(glyphs,200),"bounded valid font can use copied native glyphs");
        glyphs[100]=null;check(!MobileMenuFont.safeGlyphSet(glyphs,200),"missing glyph rejects copied atlas without allocating");glyphs[100]=glyph;
        Class207 large=new Class207();large.anInt2702=large.anInt2696=256;large.aByteArray2699=new byte[65536];large.anIntArray2697=new int[]{0,0xffffff};Arrays.fill(glyphs,large);
        check(!MobileMenuFont.safeGlyphSet(glyphs,400),"aggregate expanded font memory is bounded");

    }
    static void set(Object o,String name,Object value)throws Exception{MobileWorkflowRegression.set(o,name,value);}
    static Object get(Object o,String name)throws Exception{return MobileWorkflowRegression.get(o,name);}
    static Object call(Object o,String name,Class<?>[] types,Object...args)throws Exception{return MobileWorkflowRegression.call(o,name,types,args);}
    static void sliders()throws Exception{
        MobileRuntime rt=(MobileRuntime)MobileWorkflowRegression.field(MobileRuntime.class,"INSTANCE").get(null);
        set(rt,"viewport",new ViewportState(1,0,0,400,600,400,600,400,600));set(rt,"gameState",10);set(rt,"root",548);Class240.anInt4674=10;r.anInt9721=548;
        Class46 p=MobileNativeNextRegression.widget(743,0,-1,0,0,0,200,24),knob=MobileNativeNextRegression.widget(743,1,p.anInt830,5,40,0,24,24);
        knob.anObjectArray823=new Object[]{123};knob.aBoolean682=true;knob.aClass46_782=p; // Mirrors native script-set drag parent, no invented permission.
        Class46[][] groups=Class348_Sub40_Sub33.aClass46ArrayArray9427;Class348_Sub40_Sub33.aClass46ArrayArray9427=new Class46[744][];Class348_Sub40_Sub33.aClass46ArrayArray9427[743]=new Class46[]{p,knob};
        Object wp=MobileWorkflowRegression.captured(p,0,0,200,24,null,41),wk=MobileWorkflowRegression.captured(knob,40,0,24,24,p,42);MobileWorkflowRegression.frame(rt,wp,wk);
        Field dispatcher=MobileWorkflowRegression.field(MobileNativeOperations.class,"dispatcher");Object original=dispatcher.get(null);
        class Record implements MobileNativeOperations.Dispatcher{int count,x,y;public void action(Class348_Sub42_Sub12 e,int x,int y){throw new AssertionError("slider must not perform game operation");}public void move(Class46 s,Class46 d,int x,int y){throw new AssertionError("slider must not move inventory");}public void activate(Class46 s,int x,int y){throw new AssertionError("slider must not click");}public void adjust(Class46 s,int nx,int ny){check(s==knob,"native handle identity preserved");count++;x=nx;y=ny;knob.anInt800=nx;}}
        Record rec=new Record();dispatcher.set(null,rec);
        try{
            GestureRecognizer.Target t=rt.hit(45,10);check(t!=null&&t.kind==GestureRecognizer.Kind.SLIDER,"known native simple settings drag handle detected structurally");
            rt.adjust(t,0,0);check(rec.x==40&&rec.y==0,"grab does not jump handle to pointer centre");rt.adjust(t,20,0);check(rec.x==60,"native drag script receives parent-relative position");
            check(rt.valid(t),"script moving knob on its own axis does not invalidate active drag");rt.adjust(t,40,0);check(rec.x==80,"drag displacement remains relative to original grab, not compounded per event");
            int old=rec.count;rt.adjust(t,40,0);check(rec.count==old,"duplicate unchanged drag update not dispatched");rt.adjust(t,1000,0);check(rec.x==176,"handle stops at native track extent");
            knob.anObjectArray823=new Object[]{321};rt.adjust(t,50,0);check(!rt.valid(t)&&rec.x==176,"changed script cancels capture");knob.anObjectArray823=new Object[]{123};
            knob.anInt800=40;MobileWorkflowRegression.frame(rt,wp,wk);t=rt.hit(45,10);p.aBoolean813=true;rt.adjust(t,60,0);check(rec.x==176,"hidden track cannot receive native adjustment");p.aBoolean813=false;
            knob.anObjectArray692=new Object[]{987};check(rt.hit(45,10).kind!=GestureRecognizer.Kind.SLIDER,"completion-dependent drags are not approximated as continuous sliders");knob.anObjectArray692=null;
            knob.aClass46_782=null;check(rt.hit(45,10).kind!=GestureRecognizer.Kind.SLIDER,"missing native drag parent rejects override");knob.aClass46_782=p;
            knob.anInt830=999<<16;check(rt.hit(45,10).kind!=GestureRecognizer.Kind.SLIDER,"unknown interface retains native path");knob.anInt830=(743<<16)|1;
            MobileWorkflowRegression.frame(rt,wp,wk);Class348_Sub40_Sub4.aClass262_9111=new Class262();MobileRuntime.openMenu(45,10);
            List<?> entries=(List<?>)get(rt,"menu");int plus=-1;for(int i=0;i<entries.size();i++)if(((Integer)get(entries.get(i),"localOperation"))==5)plus=i;
            check(plus>=0&&CanvasActionMenu.active(),"long press offers native Increase/Decrease without Camera/Panel windows");set(rt,"selected",plus);int before=rec.count;MobileRuntime.afterMenuBuild();check(rec.count==before+1&&rec.x>40,"non-drag setting adjustment invokes same native listener");
            knob.anInt800=40;MobileWorkflowRegression.frame(rt,wp,wk);MobileRuntime.openMenu(45,10);entries=(List<?>)get(rt,"menu");set(rt,"selected",1);knob.anObjectArray823=new Object[]{999};before=rec.count;MobileRuntime.afterMenuBuild();check(rec.count==before,"menu nudge revalidates drag listener before action");knob.anObjectArray823=new Object[]{123};
        }finally{dispatcher.set(null,original);Class348_Sub40_Sub33.aClass46ArrayArray9427=groups;call(rt,"cancelAll",new Class<?>[0]);}
        // Gesture model: whole-displacement coordinates, no tap on drag or cancellation.
        class Sink implements GestureRecognizer.Sink{int tap,adjust,cancel;double dx;public GestureRecognizer.Target hit(double x,double y){return new GestureRecognizer.Target(1,GestureRecognizer.Kind.SLIDER);}public boolean valid(GestureRecognizer.Target t){return true;}public void tap(GestureRecognizer.Target t,double x,double y){tap++;}public void context(GestureRecognizer.Target t,double x,double y){}public void scroll(GestureRecognizer.Target t,double x,double y){}public void camera(double x,double y){}public void zoom(double d){}public void cancel(){cancel++;}public void adjust(GestureRecognizer.Target t,double x,double y){adjust++;dx=x;}}
        Sink sink=new Sink();GestureRecognizer g=new GestureRecognizer(sink,10,550);g.down(1,50,10,0,1);g.move(1,55,10,10,1);check(sink.adjust==0,"slop does not accidentally change setting");g.move(1,75,10,20,1);g.up(1,80,10,30,1);check(sink.dx==30&&sink.tap==0,"slider gesture carries total displacement and no release click");g.down(1,50,10,40,1);g.move(1,90,10,50,2);check(sink.cancel==1&&sink.tap==0,"viewport change cancels without a click");
    }
    static int operation(MobileRuntime rt,int op)throws Exception {
        List<?> menu=(List<?>)get(rt,"menu");for(int i=0;i<menu.size();i++)if(((Integer)get(menu.get(i),"localOperation"))==op)return i;
        throw new AssertionError("Expected local operation "+op);
    }
    static void cameraAndMap()throws Exception {
        MobileRuntime rt=(MobileRuntime)MobileWorkflowRegression.field(MobileRuntime.class,"INSTANCE").get(null);
        set(rt,"viewport",new ViewportState(1,0,0,400,600,400,600,400,600));set(rt,"root",548);set(rt,"gameState",10);r.anInt9721=548;Class240.anInt4674=10;
        Class46 scene=MobileNativeNextRegression.widget(746,0,-1,0,0,0,400,600);scene.anInt765=Class239_Sub10.anInt5943;
        Object ws=MobileWorkflowRegression.captured(scene,0,0,400,600,null,101);MobileWorkflowRegression.frame(rt,ws);
        Class348_Sub40_Sub4.aClass262_9111=new Class262();Class348_Sub40_Sub21.anInt9282=1;Class320.zoomStep=0;
        MobileRuntime.openMenu(180,180);check(((List<?>)get(rt,"menu")).size()==3,"scene context offers one-pointer zoom alternatives without Camera window");
        set(rt,"selected",operation(rt,1));MobileRuntime.afterMenuBuild();check(Class320.zoomStep==3*Loader.ZOOM_OFFSET_STEP,"native context zoom calls existing camera setting");
        MobileRuntime.openMenu(180,180);set(rt,"selected",operation(rt,3));Class348_Sub40_Sub21.anInt9282=2;int zoom=Class320.zoomStep;MobileRuntime.afterMenuBuild();check(Class320.zoomStep==zoom,"cutscene camera mode invalidates prior zoom action");Class348_Sub40_Sub21.anInt9282=1;
        Class46 map=MobileNativeNextRegression.widget(755,0,-1,0,0,0,400,600);map.anInt765=Class348_Sub45.anInt7102;Object wm=MobileWorkflowRegression.captured(map,0,0,400,600,null,102);MobileWorkflowRegression.frame(rt,wm);
        Class75.aFloat1249=4;MobileRuntime.openMenu(180,180);set(rt,"selected",operation(rt,6));MobileRuntime.afterMenuBuild();check(Class75.aFloat1249==5&&Class320.zoomStep==zoom,"map context zoom does not become world-camera zoom");
        MobileRuntime.openMenu(180,180);set(rt,"selected",operation(rt,7));MobileWorkflowRegression.frame(rt);MobileRuntime.afterMenuBuild();check(Class75.aFloat1249==5,"closed/replaced map rejects queued map zoom");call(rt,"cancelAll",new Class<?>[0]);
    }
    static void keyboard()throws Exception {
        Class297.aString3782=System.getProperty("java.vendor","OpenJDK");Canvas canvas=new Canvas();Class346_Sub1 keyboard=new Class346_Sub1(canvas);ViewportState v=new ViewportState(1,0,0,390,844,390,844,390,844);
        CanvasActionMenu.open(80,Arrays.asList("Use","Examine"));paint(v);MobileBridge.drain();
        KeyEvent down=new KeyEvent(canvas,KeyEvent.KEY_PRESSED,1,0,KeyEvent.VK_ENTER,'\n');keyboard.keyPressed(down);CanvasActionMenu.close();
        KeyEvent typed=new KeyEvent(canvas,KeyEvent.KEY_TYPED,2,0,KeyEvent.VK_UNDEFINED,'\n');keyboard.keyTyped(typed);KeyEvent up=new KeyEvent(canvas,KeyEvent.KEY_RELEASED,3,0,KeyEvent.VK_ENTER,'\n');keyboard.keyReleased(up);
        check(down.isConsumed()&&typed.isConsumed()&&up.isConsumed(),"menu Enter does not leak a later typed/released event into chat");
        keyboard.method2695(60);check(keyboard.method2697(0)==null,"native keyboard queue empty after menu-only interaction");
        CanvasActionMenu.open(81,Arrays.asList("Use"));paint(v);keyboard.keyPressed(down);keyboard.focusLost(new FocusEvent(canvas,FocusEvent.FOCUS_LOST));CanvasActionMenu.close();
        keyboard.keyTyped(new KeyEvent(canvas,KeyEvent.KEY_TYPED,4,0,KeyEvent.VK_UNDEFINED,'a'));keyboard.method2695(60);
        boolean found=false;for(Interface6 e=keyboard.method2697(0);e!=null;e=keyboard.method2697(0))found|=e.method28((byte)116)=='a';check(found,"focus loss clears stale popup-held keys; ordinary game text works again");MobileBridge.drain();
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("void.mobile","true");System.setProperty("void.mobile.browser","false");menus();fonts();sliders();cameraAndMap();keyboard();
        String report="Native action menu, font and direct slider assertions: "+checks+" passed\nSynthetic state, actual native dispatch adapters; no live cache/server/Android claims.\n";
        Files.createDirectories(Paths.get("build/reports/mobile"));Files.write(Paths.get("build/reports/mobile/native-touch.txt"),report.getBytes(StandardCharsets.UTF_8));System.out.print(report);
    }
}
