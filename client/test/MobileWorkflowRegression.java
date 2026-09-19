import com.voidclient.mobile.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Native policy tests stop at a captured dispatch seam, not a live server or actual network packet. */
public final class MobileWorkflowRegression {
    static int checks;
    static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    static void set(Object o,String n,Object v)throws Exception{field(o.getClass(),n).set(o,v);}
    static Object get(Object o,String n)throws Exception{return field(o.getClass(),n).get(o);}
    static Object call(Object o,String n,Class<?>[] t,Object...a)throws Exception{Method m=o.getClass().getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(o,a);}
    static Class46 parent(int id,int width,int height){Class46 p=new Class46();p.anInt830=id;p.anInt704=-1;p.anInt834=-1;p.anInt774=0;p.anInt765=0;p.anInt842=width;p.anInt728=height;p.anInt709=width;p.anInt789=height;p.anInt794=-999;p.anInt698=200;p.anInt791=270;return p;}
    static Class46 item(Class46 p,int slot,int x,int y){Class46 w=new Class46();w.anInt830=p.anInt830;w.anInt834=p.anInt830;w.anInt704=slot;w.anInt774=5;w.anInt765=0;w.anInt842=32;w.anInt728=32;w.anInt788=x;w.anInt739=y;w.anInt709=32;w.anInt789=32;w.anInt812=100+slot;w.anInt781=slot+1;w.anInt794=-999;w.aStringArray833=new String[]{"Use"};w.aString752="Synthetic item";w.aClass348_Sub44_748=new Class348_Sub44((1<<18)|(1<<21)|2,-1);return w;}
    static void layout(Class46 p,int width,int height){p.anInt709=p.anInt842;p.anInt789=p.anInt728;MobileLayouts.size(p,width,height);MobileItemGridLayout.prepare(p);for(Class46 w:p.aClass46Array798)if(w!=null){w.anInt709=w.anInt842;w.anInt789=w.anInt728;w.anInt800=w.anInt788;w.anInt750=w.anInt739;MobileLayouts.size(w,p.anInt698,p.anInt791);MobileLayouts.position(w,p.anInt698,p.anInt791);}}
    static void grids(){
        MobileBridge.publishUi(UiFrameSnapshot.empty());System.setProperty("void.mobile.nativeGrids","true");
        Class46 p=parent(149<<16,200,270);p.aClass46Array798=new Class46[28];for(int i=0;i<28;i++)p.aClass46Array798[i]=item(p,i,(i%4)*42,(i/4)*36);
        layout(p,200,270);int extent=p.anInt791;
        for(int run=0;run<10;run++){layout(p,200,270);check(p.anInt791==extent,"repeated native layout does not compound scroll extent");for(int i=0;i<28;i++){Class46 w=p.aClass46Array798[i];check(w.anInt704==i&&w.anInt788==(i%4)*42&&w.anInt739==(i/4)*36,"native slot and authored script coordinates unchanged");check(w.anInt709==w.anInt789&&w.anInt709>=48&&w.anInt800+w.anInt709<=p.anInt709,"square native icons remain proportionate and inside parent");}}
        System.setProperty("void.mobile.nativeGrids","false");layout(p,200,270);check(p.anInt791==270&&p.anInt698==200,"turning grids off restores authored root content extent");for(Class46 w:p.aClass46Array798)check(w.anInt800==w.anInt788&&w.anInt750==w.anInt739&&w.anInt709==32,"rollback restores each native item position/size");
        System.setProperty("void.mobile.nativeGrids","true");layout(p,120,270);check(p.anInt709==120&&p.anInt791>extent,"narrow portrait reflows grid to more rows without horizontal overflow");
        p.aClass46Array798[0].anInt774=4;layout(p,120,270);check(p.aClass46Array798[1].anInt800==42,"mixed unknown structure uses original authored positions");p.aClass46Array798[0].anInt774=5;
        // A server/script changes ordering and visibility; layout must follow those values, not slot number.
        p.aClass46Array798[0].aBoolean813=true;p.aClass46Array798[1].anInt739=9999;layout(p,200,270);check(p.aClass46Array798[2].anInt750==0&&p.aClass46Array798[2].anInt800==0,"filtered bank-like ordering uses visible authored order");check(p.aClass46Array798[1].anInt750>p.aClass46Array798[2].anInt750,"reordered slot keeps native identity at new visual location");
        p.anInt791=1234;System.setProperty("void.mobile.nativeGrids","false");layout(p,200,270);check(p.anInt791==1234,"explicit script extent updates remain authoritative");
        System.clearProperty("void.mobile");layout(p,200,270);check(p.aClass46Array798[2].anInt709==32,"desktop mode retains original dimensions");System.setProperty("void.mobile","true");System.setProperty("void.mobile.nativeGrids","true");
    }
    static Object captured(Class46 w,int x,int y,int width,int height,Object parent,long handle)throws Exception{
        Constructor<?> c=Class.forName("MobileRuntime$Widget").getDeclaredConstructor(Class46.class,int.class,int.class,int.class,int.class);c.setAccessible(true);Object out=c.newInstance(w,x,y,x+width,y+height);set(out,"parent",parent);set(out,"handle",handle);set(out,"version",handle*10);return out;
    }
    @SuppressWarnings("unchecked") static void frame(MobileRuntime rt,Object...nodes)throws Exception{set(rt,"visible",new ArrayList<>(Arrays.asList(nodes)));call(rt,"indexVisible",new Class<?>[0]);Map<Long,Object> map=(Map<Long,Object>)get(rt,"frameNodes");map.clear();for(Object n:nodes)map.put((Long)get(n,"handle"),n);}
    static void command(MobileRuntime rt,String type,int op,Object node)throws Exception{long token=node==null?0:(Long)get(node,"handle"),version=node==null?0:(Long)get(node,"version");call(rt,"handleNodeCommand",new Class<?>[]{MobileBridge.Command.class},new MobileBridge.Command(type,op,0,0,version,"",token));}
    static void workflows()throws Exception{
        MobileRuntime rt=(MobileRuntime)field(MobileRuntime.class,"INSTANCE").get(null);set(rt,"viewport",new ViewportState(1,0,0,400,600,400,600,400,600));set(rt,"gameState",10);set(rt,"root",548);Class240.anInt4674=10;r.anInt9721=548;r.aBoolean9722=false;
        Class46 p=parent(149<<16,200,270),a=item(p,0,0,0),b=item(p,1,60,0);p.aClass46Array798=new Class46[]{a,b};Class46[][] oldGroups=Class348_Sub40_Sub33.aClass46ArrayArray9427;Class348_Sub40_Sub33.aClass46ArrayArray9427=new Class46[150][];Class348_Sub40_Sub33.aClass46ArrayArray9427[149]=new Class46[]{p};
        Object wp=captured(p,0,0,200,270,null,1),wa=captured(a,0,0,48,48,p,2),wb=captured(b,60,0,48,48,p,3);frame(rt,wp,wa,wb);
        class Recording implements MobileNativeOperations.Dispatcher{int moves,actions,activations;Class46 from,to;Class348_Sub42_Sub12 action;public void move(Class46 s,Class46 d,int x,int y){moves++;from=s;to=d;}public void action(Class348_Sub42_Sub12 e,int x,int y){actions++;action=e;}public void adjust(Class46 s,int x,int y){}public void activate(Class46 s,int x,int y){activations++;}}
        Recording recording=new Recording();Field seam=field(MobileNativeOperations.class,"dispatcher");Object original=seam.get(null);seam.set(null,recording);
        try{
            command(rt,"moveSource",0,wa);check(get(rt,"moveSource")!=null&&recording.moves==0,"selecting Move performs no native operation");command(rt,"moveDest",0,wb);check(recording.moves==1&&recording.from==a&&recording.to==b,"two-tap Move retains exact native source/destination objects");
            command(rt,"moveSource",0,wa);a.anInt781++;command(rt,"moveDest",0,wb);check(recording.moves==1&&get(rt,"moveSource")==null,"changed source quantity cancels Move");a.anInt781--;
            command(rt,"moveSource",0,wa);command(rt,"cancelMode",0,null);command(rt,"moveDest",0,wb);check(recording.moves==1,"cancelled selection cannot later commit a Move");
            command(rt,"moveSource",0,wa);b.aClass348_Sub44_748=new Class348_Sub44(0,-1);command(rt,"moveDest",0,wb);check(recording.moves==1,"destination permissions checked again at commit");b.aClass348_Sub44_748=new Class348_Sub44((1<<18)|(1<<21)|2,-1);
            command(rt,"nodeOp",1,wa);MobileRuntime.afterMenuBuild();check(recording.actions==1&&recording.action.anInt9602==0&&recording.action.anInt9607==a.anInt830,"native action preserves slot/packed id through responsive panel");
            command(rt,"nodeOp",1,wa);b.anInt781++;MobileRuntime.afterMenuBuild();check(recording.actions==1,"another item changing in the same offer/group invalidates confirmation/action");b.anInt781--;
            frame(rt,wp,wa);command(rt,"nodeOp",1,wa);b.anInt781++;MobileRuntime.afterMenuBuild();check(recording.actions==1,"offscreen group item changes invalidate action even when not painted");b.anInt781--;frame(rt,wp,wa,wb);
            command(rt,"nodeOp",1,wa);a.aClass348_Sub44_748=new Class348_Sub44(0,-1);MobileRuntime.afterMenuBuild();check(recording.actions==1,"native permission removal rejects previously chosen operation");a.aClass348_Sub44_748=new Class348_Sub44(2,-1);
            command(rt,"nodeActions",0,wa);check(!((List<?>)get(rt,"menu")).isEmpty(),"action list opens with native permitted entries");set(rt,"menuDeadline",0L);check(!((List<?>)get(rt,"menu")).isEmpty(),"action list remains until dismissal rather than timer expiry");command(rt,"cancelMode",0,null);
            a.anInt704=99;command(rt,"nodeOp",1,wa);check(((List<?>)get(rt,"menu")).isEmpty()&&recording.actions==1,"reused widget with different native slot is ineligible");a.anInt704=0;
            Class46 tab=parent((548<<16)|1,48,48);tab.anInt774=5;tab.anObjectArray763=new Object[]{1234};
            Object wt=captured(tab,0,0,48,48,null,10);frame(rt,wt);command(rt,"nodeOp",-1,wt);check(recording.activations==1,"script-only native tab can be activated without a coordinate guess");
            tab.anObjectArray742=new Object[]{4321};command(rt,"nodeOp",-1,wt);check(recording.activations==1,"complex release-dependent controls retain original pointer path");
            tab.anObjectArray742=null;tab.anObjectArray763=new Object[]{9999};command(rt,"nodeOp",-1,wt);check(recording.activations==1,"changed native press listener invalidates stale activation");
        }finally{seam.set(null,original);Class348_Sub40_Sub33.aClass46ArrayArray9427=oldGroups;}
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("void.mobile","true");grids();workflows();
        String report="Native grid and guarded workflow assertions: "+checks+" passed\nSynthetic native fixtures; captured final dispatch seam, no live network or Android claims.\n";
        Files.createDirectories(Paths.get("build/reports/mobile"));Files.write(Paths.get("build/reports/mobile/workflows.txt"),report.getBytes(StandardCharsets.UTF_8));System.out.print(report);
    }
}
