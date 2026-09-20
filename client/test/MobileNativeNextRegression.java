import com.voidclient.mobile.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

/** Offline native-layout/input regression; no cache, credentials, server or Android device required. */
public final class MobileNativeNextRegression {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
    static Class46 widget(int group,int component,int parent,int type,int x,int y,int w,int h){Class46 n=new Class46();n.anInt830=(group<<16)|component;n.anInt834=parent;n.anInt774=type;n.anInt765=0;n.anInt788=x;n.anInt739=y;n.anInt842=w;n.anInt728=h;n.anInt709=w;n.anInt789=h;n.anInt800=x;n.anInt750=y;n.anInt794=-999;return n;}
    static void layout(Class46 w,int width,int height){Class239_Sub28.method1843(width,-326,w,false,height);Class14_Sub1.method239((byte)115,width,height,w);}
    static void chrome()throws Exception{
        AtomicInteger opened=new AtomicInteger();MobileChrome.install(()->{opened.incrementAndGet();MobileBridge.setHostOverlayActive(false);});
        for(int width:new int[]{240,320,390,844,1024})for(int height:new int[]{240,390,844})for(int scale:new int[]{1,2})for(boolean edge:new boolean[]{false,true}){
            ViewportState v=new ViewportState(8,0,0,width,height,width/scale,height/scale,width/scale,height/scale);MobileBridge.publishUi(new UiFrameSnapshot(1,8,v,Collections.emptyList(),"",false,false));
            MobileChrome.Frame f=MobileChrome.publish(v,true,false,edge,100);Rectangle r=f.menu;
            check(r.x>=0&&r.y>=0&&r.x+r.width<=v.nativeWidth&&r.y+r.height<=v.nativeHeight,"painted menu inside native raster");
            check(r.width*(double)width/v.nativeWidth>=48&&r.height*(double)height/v.nativeHeight>=48,"menu target keeps display size through magnification");
            int x=(int)((r.getCenterX())*v.width/v.nativeWidth),y=(int)((r.getCenterY())*v.height/v.nativeHeight);
            MobileChrome.Capture c=MobileChrome.press(x,y,width,height);check(c!=null,"capture uses painted coordinates");
            int before=opened.get();check(MobileChrome.release(c,x,y,width,height),"same-target release activates");flush();check(opened.get()==before+1,"one activation per release");
            check(!MobileChrome.release(c,x,y,width,height),"replayed release cannot activate twice");
            c=MobileChrome.press(x,y,width,height);MobileChrome.move(c,-5,y,width,height);MobileChrome.move(c,x,y,width,height);check(!MobileChrome.release(c,x,y,width,height),"leaving and returning stays cancelled");
            c=MobileChrome.press(x,y,width,height);MobileChrome.publish(new ViewportState(9,0,0,width,height,v.nativeWidth,v.nativeHeight,v.logicalWidth,v.logicalHeight),true,false,edge,100);
            check(!MobileChrome.release(c,x,y,width,height),"layout revision invalidates capture");
        }
        MobileBridge.setHostOverlayActive(false);ViewportState v=new ViewportState(1,0,0,390,844,390,844,390,844);MobileChrome.Frame f=MobileChrome.publish(v,true,true,false,100);
        MobileBridge.drain();check(MobileChrome.activate(MobileChrome.CANCEL,f.generation),"mode cancellation exposed only while armed");List<MobileBridge.Command> q=MobileBridge.drain();check(q.size()==2&&"cancel".equals(q.get(0).type)&&"cancelMode".equals(q.get(1).type),"cancel cannot become a native operation");
        MobileChrome.publish(v,false,false,false,100);check(MobileChrome.nodes().size()==1,"no permanent mode strip");
        Canvas canvas=new Canvas();canvas.setSize(390,844);Class373_Sub1 mouse=new Class373_Sub1(canvas,true);MobileBridge.drain();System.setProperty("void.mobile.emulateTouch","false");
        MouseEvent down=new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,1,MouseEvent.BUTTON1_DOWN_MASK,20,20,1,false,MouseEvent.BUTTON1),up=new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,2,0,20,20,1,false,MouseEvent.BUTTON1);
        int before=opened.get();mouse.mousePressed(down);mouse.mouseReleased(up);flush();check(down.isConsumed()&&up.isConsumed()&&opened.get()==before+1,"ordinary mouse also owns the native menu gesture");
        MobileBridge.drain();mouse.mousePressed(down);mouse.mobileCancel();mouse.mouseReleased(up);flush();check(opened.get()==before+1,"cancelled AWT capture consumes late release");
        mouse.mousePressed(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,1,MouseEvent.BUTTON3_DOWN_MASK,20,20,1,true,MouseEvent.BUTTON3));mouse.mouseReleased(new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,2,0,20,20,1,true,MouseEvent.BUTTON3));
        Field pending=Class373_Sub1.class.getDeclaredField("aClass262_7420");pending.setAccessible(true);check(((Class262)pending.get(mouse)).method1995(4)==null,"menu never generates a hidden game mouse event, including secondary button");
        before=opened.get();MobileChrome.key(false);MobileChrome.key(true);MobileChrome.key(true);flush();check(opened.get()==before+1,"F10 autorepeat opens at most once");MobileChrome.key(false);
        System.setProperty("void.mobile.browser","true");check(MobileChrome.frame()==null&&!MobileChrome.key(true),"browser retains its separate chrome contract");System.clearProperty("void.mobile.browser");
        MobileChrome.clear();MobileChrome.install(null);mouse.method3592(0);MobileBridge.setHostOverlayActive(false);MobileBridge.drain();
    }
    static void docking(){
        Class46 root=widget(746,0,-1,0,0,0,765,503),
            holder=widget(746,1,root.anInt830,0,500,180,200,270),
            chat=widget(746,2,root.anInt830,0,0,350,500,150),
            bank=widget(746,3,root.anInt830,0,120,70,512,334);
        Class46[] group={root,holder,chat,bank};
        MobileNativeHud.AttachmentLookup lookup=w->w==holder?149:w==chat?752:w==bank?762:-1;
        for(int width:new int[]{320,390,844,1024})for(int height:new int[]{390,844,1080}){
            MobileBridge.publishUi(new UiFrameSnapshot(1,1,new ViewportState(1,0,0,width,height,width,height,width,height),Collections.emptyList(),"",false,false));
            MobileNativeHud.prepare(746,width,height,group,lookup);
            layout(root,width,height);layout(holder,width,height);layout(chat,width,height);layout(bank,width,height);
            check(root.anInt709==width&&root.anInt789==height,"only the unambiguous game-frame shell expands to the viewport");
            for(Class46 w:new Class46[]{holder,chat,bank}){
                check(w.anInt800>=0&&w.anInt750>=0&&w.anInt800+w.anInt709<=width&&w.anInt750+w.anInt789<=height,"bounded mobile attachment stays in viewport");
                check(w.anInt834==root.anInt830,"native parent identity retained");
            }
            int margin=MobileNativeHud.unit(8);
            check(chat.anInt709<width-2*margin,"chat is never stretched across the viewport");
            check(holder.anInt709<width-2*margin,"side panel is never viewport-wide");
            Rectangle sideRect=new Rectangle(holder.anInt800,holder.anInt750,holder.anInt709,holder.anInt789);
            Rectangle chatRect=new Rectangle(chat.anInt800,chat.anInt750,chat.anInt709,chat.anInt789);
            Rectangle bankRect=new Rectangle(bank.anInt800,bank.anInt750,bank.anInt709,bank.anInt789);
            check(!sideRect.intersects(chatRect),"bounded chat and side panel occupy independent HUD lanes");
            check(!sideRect.intersects(bankRect),"central modal reserves the stable right-side panel lane");
            if(width>=height) {
                check(chat.anInt709<=Math.min(MobileNativeHud.unit(360),(width-2*margin)*45/100),"landscape chat respects OSRS-style width cap");
                check(holder.anInt709<=Math.min(MobileNativeHud.unit(300),(width-2*margin)*35/100),"landscape side panel respects fixed right-rail cap");
            }
            check(holder.anInt788==500&&holder.anInt739==180&&holder.anInt842==200&&holder.anInt728==270,"script-authored side geometry untouched");
            check(chat.anInt788==0&&chat.anInt739==350&&chat.anInt842==500&&chat.anInt728==150,"script-authored chat geometry untouched");
            check(!MobileNativeHud.stretch(holder,width,height),"generic widget stretching is permanently disabled");
        }

        holder.aBoolean813=true;MobileNativeHud.prepare(746,390,844,group,lookup);layout(holder,765,503);
        check(holder.anInt709==200&&holder.anInt800==500,"hidden tab is not revealed or docked");holder.aBoolean813=false;

        root.anObjectArray763=new Object[]{123};MobileNativeHud.prepare(746,390,844,group,lookup);layout(holder,765,503);
        check(holder.anInt709==200,"interactive game-frame shell fails closed instead of stretching descendants");root.anObjectArray763=null;

        Class46 other=widget(746,4,root.anInt830,0,20,30,200,270);
        MobileNativeHud.prepare(746,390,844,new Class46[]{root,holder,other},w->w==root?-1:149);layout(holder,765,503);
        check(holder.anInt709==200,"multiple visible side owners retain authored layout rather than overlap");

        root.anInt834=holder.anInt830;MobileNativeHud.prepare(746,390,844,group,lookup);layout(holder,765,503);
        check(holder.anInt709==200,"cycles fail closed");root.anInt834=-1;

        MobileNativeHud.prepare(746,390,844,group,lookup);holder.anInt788++;layout(holder,765,503);
        check(holder.anInt800==501,"new script coordinates invalidate old placement");holder.anInt788--;

        Class46 wrapper=widget(746,5,root.anInt830,0,0,0,765,503);
        Class46 nested=widget(746,6,wrapper.anInt830,0,500,180,200,270);
        MobileNativeHud.prepare(746,844,390,new Class46[]{root,wrapper,nested},w->w==nested?149:-1);layout(root,844,390);layout(wrapper,844,390);layout(nested,765,503);
        check(nested.anInt800==500&&nested.anInt709==200,"fixed nested coordinate spaces are not mutated to fake viewport anchoring");
        wrapper.aByte778=1;wrapper.anInt842=0;wrapper.aByte724=1;wrapper.anInt728=0;
        MobileNativeHud.prepare(746,844,390,new Class46[]{root,wrapper,nested},w->w==nested?149:-1);layout(root,844,390);layout(wrapper,844,390);layout(nested,wrapper.anInt709,wrapper.anInt789);
        check(nested.anInt800+ nested.anInt709<=844&&nested.anInt709<844,"parent-filling wrapper permits bounded native anchoring");

        Class46 map=widget(746,7,root.anInt830,0,0,0,765,503);
        MobileNativeHud.prepare(746,844,390,new Class46[]{root,map},w->w==map?755:-1);layout(root,844,390);layout(map,844,390);
        check(map.anInt800>0&&map.anInt750>0&&map.anInt709<844&&map.anInt789<390,"world map uses a safe full-screen inset instead of altering the HUD shell");

        check("inventory_tab".equals(InterfaceRegistry.type(149))&&"main_screen".equals(InterfaceRegistry.type(620)),"registry exposes native interface types for role classification");

        System.setProperty("void.mobile.nativeHud","false");MobileNativeHud.prepare(746,390,844,group,lookup);layout(holder,765,503);
        check(holder.anInt800==500&&holder.anInt709==200&&holder.anInt698==0,"native layout rollback restores geometry and original extents");System.setProperty("void.mobile.nativeHud","true");

        MobileNativeHud.prepare(999,390,844,group,lookup);layout(holder,765,503);
        check(holder.anInt709==200,"unknown root keeps legacy layout");
        MobileNativeHud.reset();
    }

    static void recursiveNativeLayout() {
        Class46[][] oldGroups=Class348_Sub40_Sub33.aClass46ArrayArray9427;
        boolean[] oldLoaded=Class163.aBooleanArray2162;IterableHashTable oldAttachments=Class125.aClass356_4915;int oldRoot=r.anInt9721;
        Class46 root=widget(746,0,-1,0,0,0,765,503),holder=widget(746,1,root.anInt830,0,10,40,600,400);
        Class46 bank=widget(762,0,-1,0,0,0,600,400),container=widget(762,93,bank.anInt830,0,15,50,570,320);
        container.aClass46Array798=new Class46[120];for(int i=0;i<120;i++)container.aClass46Array798[i]=MobileWorkflowRegression.item(container,i,(i%10)*42,(i/10)*36);
        Class348_Sub40_Sub33.aClass46ArrayArray9427=new Class46[763][];
        Class348_Sub40_Sub33.aClass46ArrayArray9427[746]=new Class46[]{root,holder};Class348_Sub40_Sub33.aClass46ArrayArray9427[762]=new Class46[94];Class348_Sub40_Sub33.aClass46ArrayArray9427[762][0]=bank;Class348_Sub40_Sub33.aClass46ArrayArray9427[762][93]=container;
        Class163.aBooleanArray2162=new boolean[763];Class163.aBooleanArray2162[746]=Class163.aBooleanArray2162[762]=true;
        Class125.aClass356_4915=new IterableHashTable(16);Class348_Sub41 attachment=new Class348_Sub41();attachment.anInt7050=762;Class125.aClass356_4915.method3483((byte)25,holder.anInt830,attachment);r.anInt9721=746;
        try {
            for(int[] size:new int[][]{{390,844},{844,390},{320,568},{1024,768},{390,844}}) {
                MobileBridge.publishUi(new UiFrameSnapshot(1,1,new ViewportState(1,0,0,size[0],size[1],size[0],size[1],size[0],size[1]),Collections.emptyList(),"",false,false));
                int stable=-1;
                for(int pass=0;pass<3;pass++) {
                    Class239_Sub3.method1728(size[1],-1,746,false,size[0]);
                    check(root.anInt709==size[0]&&root.anInt789==size[1],"actual recursive HUD fills viewport");
                    check(bank.anInt709==holder.anInt709&&bank.anInt789==holder.anInt789,"attachment root receives docked bounds through native recursion");
                    check(container.anInt800>=0&&container.anInt800+container.anInt709<=bank.anInt709,"nested bank-like container stretches inside dock");
                    check(container.anInt750+container.anInt789<=bank.anInt789,"nested scroll area height stays inside dock");
                    check(container.anInt791>=container.anInt789,"native inventory scroll extent remains valid");
                    if(stable>=0)check(container.anInt791==stable,"native recursive relayout does not compound scrolling");stable=container.anInt791;
                    for(Class46 slot:container.aClass46Array798)check(slot.anInt709==slot.anInt789&&slot.anInt709>=48&&slot.anInt800+slot.anInt709<=container.anInt709,"all native dynamic items fit reflowed container and preserve squares");
                }
            }
            MobileNativeHud.tick();RuntimeException_Sub1.aBoolean4604=false;container.anInt842--;MobileNativeHud.tick();check(RuntimeException_Sub1.aBoolean4604,"script changes inside docked group schedule native relayout");container.anInt842++;
            System.setProperty("void.mobile.nativeHud","false");System.setProperty("void.mobile.nativeGrids","false");
            Class239_Sub3.method1728(900,-1,746,false,1200);
            check(holder.anInt800==10&&holder.anInt750==40&&holder.anInt709==600&&holder.anInt789==400,"native docking rollback restores authored holder");
            check(bank.anInt709==600&&container.anInt709==570&&container.anInt791==0,"nested grid rollback restores original content extent");
            check(container.aClass46Array798[119].anInt704==119&&container.aClass46Array798[119].anInt709==32,"slot identities survive recursive layout and rollback");
        } finally {MobileNativeHud.reset();Class348_Sub40_Sub33.aClass46ArrayArray9427=oldGroups;Class163.aBooleanArray2162=oldLoaded;Class125.aClass356_4915=oldAttachments;r.anInt9721=oldRoot;System.setProperty("void.mobile.nativeHud","true");System.setProperty("void.mobile.nativeGrids","true");}
    }
    static void flow(){
        MobileBridge.publishUi(UiFrameSnapshot.empty());Class46 p=widget(210,0,-1,0,0,0,300,100),title=widget(210,1,p.anInt830,4,10,10,280,16),choice=widget(210,2,p.anInt830,4,10,60,280,16);
        title.aString792="A long native dialogue message which needs enough room to wrap rather than disappearing off the edge of a narrow mobile interface.";choice.aString792="Continue";choice.anObjectArray763=new Object[]{123};Class46[] ws={p,title,choice};
        for(int width:new int[]{160,240,320,390}){p.anInt709=width;check(MobileNativeFlow.prepare(p,ws),"simple native dialogue structure reflows");layout(title,width,p.anInt791);layout(choice,width,p.anInt791);check(title.anInt800+title.anInt709<=width&&choice.anInt800+choice.anInt709<=width,"native text width follows parent");check(choice.anInt750>=title.anInt750+title.anInt789&&choice.anInt789>=48,"wrapped content and touch choice do not overlap");check(p.anInt791>=choice.anInt750+choice.anInt789,"long dialogue remains vertically scrollable");int extent=p.anInt791;MobileNativeFlow.prepare(p,ws);check(p.anInt791==extent,"repeated layout does not compound extent");}
        title.anObjectArray822=new Object[]{456};check(!MobileNativeFlow.prepare(p,ws),"script keyboard fields keep their native layout");layout(title,300,100);check(title.anInt709==280&&title.anInt788==10,"unsupported flow restores authored text geometry");title.anObjectArray822=null;
        p.anInt830=137<<16;title.anInt834=choice.anInt834=p.anInt830;check(!MobileNativeFlow.prepare(p,ws),"chat history is not mistaken for a dialogue form");
        Class46 eq=widget(387,0,-1,0,0,0,220,270);int[] ids={8,11,14,17,20,23,26,29,32,35,38};Class46[] equipment=new Class46[ids.length+1];equipment[0]=eq;
        for(int i=0;i<ids.length;i++)equipment[i+1]=widget(387,ids[i],eq.anInt830,5,30,40,32,32);
        check(MobileNativeFlow.prepare(eq,equipment),"complete same-parent equipment slots preserve paper-doll layout");List<Rectangle> bounds=new ArrayList<>();for(int i=1;i<equipment.length;i++){Class46 w=equipment[i];layout(w,220,eq.anInt791);Rectangle r=new Rectangle(w.anInt800,w.anInt750,w.anInt709,w.anInt789);check(w.anInt709==48&&w.anInt789==48,"equipment touch size");for(Rectangle old:bounds)check(!r.intersects(old),"equipment slots remain distinct");bounds.add(r);}
        equipment[1].anInt774=6;check(!MobileNativeFlow.prepare(eq,equipment),"mixed 3D equipment structure is not flattened");
        System.setProperty("void.mobile.nativeHud","false");MobileNativeFlow.prepare(eq,equipment);layout(equipment[2],220,270);check(equipment[2].anInt709==32&&equipment[2].anInt800==30,"native flow rollback restores slot geometry");System.setProperty("void.mobile.nativeHud","true");
    }
    static void nativeMove()throws Exception{
        MobileWorkflowRegression.workflows(); // Native permission, offer-content and stale-identity dispatch adversaries.
        MobileRuntime rt=(MobileRuntime)MobileWorkflowRegression.field(MobileRuntime.class,"INSTANCE").get(null);
        Class46 p=MobileWorkflowRegression.parent(149<<16,200,270),a=MobileWorkflowRegression.item(p,0,0,0),b=MobileWorkflowRegression.item(p,1,60,0);p.aClass46Array798=new Class46[]{a,b};
        Object wp=MobileWorkflowRegression.captured(p,0,0,200,270,null,101),wa=MobileWorkflowRegression.captured(a,0,0,48,48,p,102),wb=MobileWorkflowRegression.captured(b,60,0,48,48,p,103);
        Class46[][] groupsBefore=Class348_Sub40_Sub33.aClass46ArrayArray9427;Class348_Sub40_Sub33.aClass46ArrayArray9427=new Class46[150][];Class348_Sub40_Sub33.aClass46ArrayArray9427[149]=new Class46[]{p};
        MobileWorkflowRegression.frame(rt,wp,wa,wb);MobileWorkflowRegression.command(rt,"nodeActions",0,wa);
        List<?> menu=(List<?>)MobileWorkflowRegression.get(rt,"menu");int move=-1;for(int i=0;i<menu.size();i++)if(MobileWorkflowRegression.get(menu.get(i),"moveWidget")!=null)move=i;
        check(move>=0,"native item action list includes Move without opening Panels");MobileWorkflowRegression.set(rt,"selected",move);MobileRuntime.afterMenuBuild();check(MobileWorkflowRegression.get(rt,"moveSource")!=null,"selecting Move arms source but sends no item packet");
        class Recorder implements MobileNativeOperations.Dispatcher{int moves;public void move(Class46 s,Class46 d,int x,int y){check(s==a&&d==b,"native tap destination retains exact item identities");moves++;}public void action(Class348_Sub42_Sub12 e,int x,int y){throw new AssertionError("unexpected ordinary action");}public void activate(Class46 w,int x,int y){throw new AssertionError("unexpected control");}public void adjust(Class46 w,int x,int y){throw new AssertionError("unexpected adjustment");}}
        Recorder recorder=new Recorder();Field seam=MobileWorkflowRegression.field(MobileNativeOperations.class,"dispatcher");Object old=seam.get(null);seam.set(null,recorder);
        try{GestureRecognizer.Target hit=rt.hit(75,20);rt.tap(hit,75,20);check(recorder.moves==1&&MobileWorkflowRegression.get(rt,"moveSource")==null,"second native game tap performs exactly one guarded move");
            MobileWorkflowRegression.command(rt,"moveSource",0,wa);a.anInt781++;hit=rt.hit(75,20);rt.tap(hit,75,20);check(recorder.moves==1,"changed source cannot be moved by a later destination tap");
            a.anInt781--;MobileWorkflowRegression.command(rt,"moveSource",0,wa);Class348_Sub40_Sub33.aClass46ArrayArray9427=null;hit=rt.hit(75,20);rt.tap(hit,75,20);check(recorder.moves==1,"unloaded native drag parent cancels without throwing or sending");
        }finally{seam.set(null,old);Class348_Sub40_Sub33.aClass46ArrayArray9427=groupsBefore;}
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("void.mobile","true");System.setProperty("void.mobile.nativeHud","true");chrome();docking();recursiveNativeLayout();flow();nativeMove();
        String report="Native-mobile next-step assertions: "+checks+" passed\nIncludes additional native workflow adversaries: "+MobileWorkflowRegression.checks+"\nSynthetic native fixtures and actual adapters; no live cache/server or Android certification.\n";
        Files.createDirectories(Paths.get("build/reports/mobile"));Files.write(Paths.get("build/reports/mobile/native-next.txt"),report.getBytes(StandardCharsets.UTF_8));System.out.print(report);
    }
}
