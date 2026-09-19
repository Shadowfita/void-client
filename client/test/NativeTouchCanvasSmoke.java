import com.voidclient.mobile.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Real native software rendering + AWT event path, also executed against ONLY the obfuscated JAR.
 * Font bitmaps and action entries are synthetic fixtures, never claims of live-cache/device validation. */
public final class NativeTouchCanvasSmoke {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static Class<?> type(String name)throws Exception{return NativeCanvasSmoke.type(name);}
    static Object make(String name,Class<?>[] types,Object...args)throws Exception{return NativeCanvasSmoke.make(name,types,args);}
    static Object call(String name,String method,Object target,Class<?>[] types,Object...args)throws Exception{return NativeCanvasSmoke.call(name,method,target,types,args);}
    static Object get(String name,String field,Object target)throws Exception{return NativeCanvasSmoke.get(name,field,target);}
    static void set(String name,String field,Object target,Object value)throws Exception{NativeCanvasSmoke.set(name,field,target,value);}
    static int[] pixels(Object renderer,int width,int height)throws Exception{return (int[])call("ha","na",renderer,new Class<?>[]{int.class,int.class,int.class,int.class},0,0,width,height);}
    static void paint(Object renderer)throws Exception{call("ha","aa",renderer,new Class<?>[]{int.class,int.class,int.class,int.class,int.class,int.class},0,0,390,844,0xff223a34,0);call("MobileCanvasMenu","paint",null,new Class<?>[0]);}
    static void font(Object renderer)throws Exception{
        byte[] encoded=new byte[263];Arrays.fill(encoded,2,258,(byte)8);encoded[258]=12;encoded[261]=12;encoded[262]=4;
        Object metrics=make("Class143",new Class<?>[]{byte[].class},(Object)encoded);
        metrics=call("Class143","scaledForMobile",metrics,new Class<?>[]{int.class},150);
        Object glyphs=Array.newInstance(type("Class207"),256);
        for(int c=0;c<256;c++){
            BufferedImage image=new BufferedImage(8,16,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,8,16);g.setColor(Color.WHITE);g.setFont(new Font("Monospaced",Font.PLAIN,11));g.drawString(String.valueOf((char)c),0,12);g.dispose();
            byte[] data=new byte[128];for(int y=0;y<16;y++)for(int x=0;x<8;x++)data[y*8+x]=(byte)((image.getRGB(x,y)&0xffffff)==0?0:1);
            Object glyph=make("Class207",new Class<?>[0]);set("Class207","anInt2702",glyph,8);set("Class207","anInt2696",glyph,16);set("Class207","aByteArray2699",glyph,data);set("Class207","anIntArray2697",glyph,new int[]{0,0xffffff});
            Array.set(glyphs,c,call("MobileMenuFont","scale",null,new Class<?>[]{type("Class207"),int.class},glyph,150));
        }
        Object nativeFont=call("ha","method3686",renderer,new Class<?>[]{type("Class143"),glyphs.getClass(),boolean.class},metrics,glyphs,true);
        Object face=make("MobileMenuFont$Face",new Class<?>[]{type("Class324"),type("Class143"),int.class},nativeFont,metrics,150);
        set("MobileMenuFont","owner",null,renderer);set("MobileMenuFont","requested",null,150);set("MobileMenuFont","id",null,get("Class291","anInt3736",null));set("MobileMenuFont","cached",null,face);
    }
    static int dialogs(){int n=0;for(Window w:Window.getWindows())if(w instanceof JDialog&&w.isVisible())n++;return n;}
    public static void main(String[] args)throws Exception{
        try{NativeCanvasSmoke.released=args.length>0;if(args.length>0)NativeCanvasSmoke.mapping=Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8);run();}
        catch(Throwable t){t.printStackTrace();System.exit(1);}
        finally{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});}
    }
    static void run()throws Exception{
        System.setProperty("void.mobile","true");System.setProperty("void.mobile.jarRunner","true");System.setProperty("void.mobile.gameScale","100");System.setProperty("user.home",Files.createTempDirectory("native-actions-").toString());
        if(NativeCanvasSmoke.released)for(String name:new String[]{"MobileRuntime","MobileActionOverlay","MobileMenuFont","Class143","Class373_Sub1"})check(type(name).getProtectionDomain().getCodeSource().getLocation().toString().endsWith("void-client-jarrunner-mobile.jar"),"release classpath isolated: "+name);
        Container loader=(Container)make("Loader",new Class<?>[0]);call("MobileLauncher","open",null,new Class<?>[]{type("Loader")},loader);NativeCanvasSmoke.flush();
        JFrame host=null;for(Window w:Window.getWindows())if(w instanceof JFrame&&w.isVisible())host=(JFrame)w;check(host!=null,"real toolbar-free host created");final JFrame frame=host;
        Canvas canvas=new Canvas();SwingUtilities.invokeAndWait(()->{frame.setBounds(0,0,390,844);loader.setLayout(new BorderLayout());loader.add(canvas);frame.validate();});NativeCanvasSmoke.flush();
        int width=canvas.getWidth(),height=canvas.getHeight();check(width==390&&height==844,"entire portrait canvas available, no footer");
        Object renderer=make("ha_Sub1",new Class<?>[]{Canvas.class,type("d"),int.class,int.class},canvas,null,width,height);
        set("Class348_Sub8","aHa6654",null,renderer);set("Class321","anInt4017",null,width);set("Class348_Sub42_Sub8_Sub2","anInt10432",null,height);
        Object runtime=get("MobileRuntime","INSTANCE",null);Object mouse=make("Class373_Sub1",new Class<?>[]{Component.class,boolean.class},canvas,true);
        set("r","anInt9721",null,548);set("Class240","anInt4674",null,10);set("MobileRuntime","root",runtime,548);set("MobileRuntime","gameState",runtime,10);
        set("MobileRuntime","revision",runtime,7L);set("MobileRuntime","appliedLayoutPreferences",runtime,AccessibilityPreferences.revision());
        ViewportState v=new ViewportState(7,0,0,width,height,width,height,width,height);set("MobileRuntime","viewport",runtime,v);set("MobileRuntime","input",runtime,mouse);
        MobileBridge.publishUi(new UiFrameSnapshot(1,7,v,Collections.emptyList(),"",false,false));MobileBridge.drain();
        Object entries=make("Class262",new Class<?>[0]);set("Class348_Sub40_Sub4","aClass262_9111",null,entries);
        for(int i=0;i<14;i++){
            Object e=make("Class348_Sub42_Sub12",new Class<?>[]{String.class,String.class,int.class,int.class,int.class,long.class,int.class,int.class,boolean.class,boolean.class,long.class,boolean.class},"Native action "+i,"Synthetic item",0,18,0,1L,i,149<<16,true,false,0L,false);
            call("Class262","method1999",entries,new Class<?>[]{type("Node"),int.class},e,-20180);
        }
        call("MobileRuntime","openMenu",null,new Class<?>[]{int.class,int.class},190,80);check(CanvasActionMenu.active(),"native builder opened in-canvas menu");
        int beforeDialogs=dialogs();paint(renderer);CanvasActionMenu.Frame missing=CanvasActionMenu.frame();check(missing!=null&&missing.rows.isEmpty(),"before font availability only a recovery X is actionable");
        font(renderer);paint(renderer); // A fresh game frame clears the earlier recovery-only glyph.
        call("ha","KA",renderer,new Class<?>[]{int.class,int.class,int.class,int.class},40,60,350,750);
        call("MobileCanvasMenu","paint",null,new Class<?>[0]);int[] clip=new int[4];call("ha","K",renderer,new Class<?>[]{int[].class},(Object)clip);
        check(Arrays.equals(clip,new int[]{40,60,350,750}),"action overlay restores native clipping");
        CanvasActionMenu.Frame menu=CanvasActionMenu.frame();check(menu!=null&&menu.rows.size()==14&&menu.maxScroll()>0,"all native operations available in scrollable touch menu");check(menu.lineHeight>=24,"native copied glyph font larger independently of scene");check(dialogs()==beforeDialogs,"opening actions creates no Swing dialog");
        int[] px=pixels(renderer,width,height);int textPixels=0;for(int value:px)if((value&0xffffff)==0xfff2d4)textPixels++;
        check(textPixels>100,"actual native raster contains rendered label glyphs, not only boxes");
        BufferedImage screenshot=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);screenshot.setRGB(0,0,width,height,px,0,width);Files.createDirectories(Paths.get("build/reports/mobile"));String prefix=NativeCanvasSmoke.released?"released-":"";ImageIO.write(screenshot,"png",Paths.get("build/reports/mobile/"+prefix+"native-actions-SYNTHETIC.png").toFile());
        // Actual AWT listener -> bridge -> client tick -> native menu validation -> original dispatch seam.
        Field dispatcher=NativeCanvasSmoke.field("MobileNativeOperations","dispatcher");Object original=dispatcher.get(null);int[] dispatches={0};Object[] dispatched={null};
        Class<?> seam=type("MobileNativeOperations$Dispatcher");Object recording=Proxy.newProxyInstance(seam.getClassLoader(),new Class<?>[]{seam},(proxy,method,args)->{dispatches[0]++;dispatched[0]=args[0];return null;});dispatcher.set(null,recording);
        try{
            Rectangle first=menu.rowBounds(menu.rows.get(0));int x=(int)first.getCenterX(),y=(int)first.getCenterY();
            MouseEvent up=new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,2,0,x,y,1,false,MouseEvent.BUTTON1);((MouseListener)mouse).mouseReleased(up);
            call("MobileRuntime","tick",null,new Class<?>[]{type("Class373_Sub1"),Component.class},mouse,canvas);call("MobileRuntime","afterMenuBuild",null,new Class<?>[0]);check(dispatches[0]==0&&CanvasActionMenu.active(),"opening hold-release cannot trigger native action");
            MouseEvent down=new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,3,MouseEvent.BUTTON1_DOWN_MASK,x,y,1,false,MouseEvent.BUTTON1);((MouseListener)mouse).mousePressed(down);((MouseListener)mouse).mouseReleased(up);
            call("MobileRuntime","tick",null,new Class<?>[]{type("Class373_Sub1"),Component.class},mouse,canvas);call("MobileRuntime","afterMenuBuild",null,new Class<?>[0]);
            check(dispatches[0]==1&&!CanvasActionMenu.active(),"touch action executes exactly once and closes native menu");check(((Integer)get("Class348_Sub42_Sub12","anInt9602",dispatched[0]))==13,"display order retains original operation slot arguments");
            check(down.isConsumed()&&up.isConsumed()&&dialogs()==beforeDialogs,"touch action neither leaks mouse input nor opens a helper window");
            call("MobileRuntime","openMenu",null,new Class<?>[]{int.class,int.class},190,80);paint(renderer);menu=CanvasActionMenu.frame();MobileBridge.action("select",0,menu.menuId);call("Class262","method1996",entries,new Class<?>[]{int.class},127);
            call("MobileRuntime","tick",null,new Class<?>[]{type("Class373_Sub1"),Component.class},mouse,canvas);call("MobileRuntime","afterMenuBuild",null,new Class<?>[0]);check(dispatches[0]==1&&!CanvasActionMenu.active(),"rebuilt native actions no longer matching rejects stale selection");
        }finally{dispatcher.set(null,original);}
        call("ha","method3635",renderer,new Class<?>[]{byte.class},(byte)-115);set("Class348_Sub8","aHa6654",null,null);
        String report=(NativeCanvasSmoke.released?"Release":"Development")+" native action canvas assertions: "+checks+" passed\nActual software renderer, enlarged bitmap font path, AWT pointer delivery and native dispatch seam. Synthetic glyphs/entries; no live cache, Android or server connection.\n";
        Files.write(Paths.get("build/reports/mobile/"+prefix+"native-action-canvas.txt"),report.getBytes(StandardCharsets.UTF_8));System.out.print(report);
    }
}
