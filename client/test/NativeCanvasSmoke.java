import com.voidclient.mobile.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Same test runs against development output and, using its mapping, ONLY the obfuscated JAR.
 * Real software renderer and native widget traversal, with synthetic rectangles, not a game cache. */
public final class NativeCanvasSmoke {
    static int checks;static List<String> mapping=Collections.emptyList();static boolean released;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static Class<?> type(String name)throws Exception {
        if(!released)return Class.forName(name);
        for(String line:mapping)if(line.startsWith(name+" -> ")&&line.endsWith(":"))return Class.forName(line.substring((name+" -> ").length(),line.length()-1));
        throw new AssertionError("Missing class mapping: "+name);
    }
    static String member(String owner,String name,boolean method)throws Exception {
        if(!released)return name;boolean in=false;
        for(String line:mapping){if(line.trim().isEmpty()||line.trim().startsWith("#"))continue;if(!line.startsWith(" ")){in=line.startsWith(owner+" -> ");continue;}if(!in||!line.contains(" -> "))continue;
            String[] parts=line.trim().split(" -> ");String left=parts[0];
            if(method?left.contains(" "+name+"("):left.endsWith(" "+name))return parts[1];
        }
        throw new AssertionError("Missing member mapping: "+owner+"."+name);
    }
    static Field field(String owner,String name)throws Exception{Field f=type(owner).getDeclaredField(member(owner,name,false));f.setAccessible(true);return f;}
    static void set(String owner,String name,Object instance,Object value)throws Exception{field(owner,name).set(instance,value);}
    static Object get(String owner,String name,Object instance)throws Exception{return field(owner,name).get(instance);}
    static Object call(String owner,String name,Object instance,Class<?>[] params,Object... args)throws Exception{Method m=type(owner).getDeclaredMethod(member(owner,name,true),params);m.setAccessible(true);return m.invoke(instance,args);}
    static Object make(String owner,Class<?>[] params,Object... args)throws Exception{Constructor<?> c=type(owner).getDeclaredConstructor(params);c.setAccessible(true);return c.newInstance(args);}
    static List<Component> children(Container root){List<Component> out=new ArrayList<>();for(Component c:root.getComponents()){out.add(c);if(c instanceof Container)out.addAll(children((Container)c));}return out;}
    static JButton button(Container root,String text){for(Component c:children(root))if(c instanceof JButton&&(text.equals(((JButton)c).getText())||text.equals(c.getAccessibleContext().getAccessibleName())))return (JButton)c;throw new AssertionError("Missing button: "+text);}
    static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});Thread.sleep(100);SwingUtilities.invokeAndWait(()->{});}
    static Object rectangle(int id,int x,int y,int width,int height,int colour)throws Exception{
        Object w=make("Class46",new Class<?>[0]);String[] names={"anInt830","anInt834","anInt774","anInt765","anInt800","anInt750","anInt709","anInt789","anInt749","anInt794"};int[] values={id,-1,3,0,x,y,width,height,colour,-999};
        for(int i=0;i<names.length;i++)set("Class46",names[i],w,values[i]);set("Class46","aBoolean810",w,true);return w;
    }
    public static void main(String[] args)throws Exception{
        try{released=args.length>0;if(released)mapping=Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8);run();}
        catch(Throwable t){t.printStackTrace();System.exit(1);}
        finally{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});}
    }
    static void run()throws Exception{
        System.setProperty("void.mobile","true");System.setProperty("void.mobile.jarRunner","true");System.setProperty("user.home",Files.createTempDirectory("void-native-canvas-").toString());
        if(released)for(String name:new String[]{"MobileLauncher","MobileCanvasMenu","MobileNativeHud","MobileNativeFlow","MobileRuntime","ha_Sub1"})check(type(name).getProtectionDomain().getCodeSource().getLocation().toString().endsWith("void-client-jarrunner-mobile.jar"),"actual release implementation: "+name);
        Container loader=(Container)make("Loader",new Class<?>[0]);call("MobileLauncher","open",null,new Class<?>[]{type("Loader")},loader);flush();
        JFrame host=null;for(Window w:Window.getWindows())if(w instanceof JFrame&&w.isVisible())host=(JFrame)w;check(host!=null,"real standalone host opened without connecting to server");final JFrame frame=host;
        check(loader.getHeight()==frame.getContentPane().getHeight()&&loader.getWidth()==frame.getContentPane().getWidth(),"no reserved footer pixels");
        check(children(frame).stream().noneMatch(c->c instanceof JButton),"no persistent Swing toolbar");
        Canvas canvas=new Canvas();SwingUtilities.invokeAndWait(()->{loader.setLayout(new BorderLayout());loader.add(canvas);frame.validate();});flush();
        int width=canvas.getWidth(),height=canvas.getHeight();check(width>0&&height==frame.getContentPane().getHeight(),"actual heavyweight game Canvas gets full height");
        Object renderer=make("ha_Sub1",new Class<?>[]{Canvas.class,type("d"),int.class,int.class},canvas,null,width,height);
        set("Class348_Sub8","aHa6654",null,renderer);set("Class321","anInt4017",null,width);set("Class348_Sub42_Sub8_Sub2","anInt10432",null,height);set("ha_Sub3","anInt8045",null,2);
        Object nativeWidgets=Array.newInstance(type("Class46"),2);Array.set(nativeWidgets,0,rectangle(900<<16,0,0,width,height,0xff263d37));Array.set(nativeWidgets,1,rectangle((900<<16)|1,65,80,90,60,0xff987046));
        call("MobileRuntime","beginPaint",null,new Class<?>[0]);
        call("Class348_Sub40_Sub7","method3064",null,new Class<?>[]{int.class,int.class,boolean.class,int.class,int.class,int.class,int.class,boolean.class,nativeWidgets.getClass(),int.class,int.class},0,0,false,0,0,0,width,false,nativeWidgets,-1,height);
        call("MobileRuntime","endPaint",null,new Class<?>[]{boolean.class},true);
        Object rt=get("MobileRuntime","INSTANCE",null);List<?> painted=(List<?>)get("MobileRuntime","painted",rt);
        check(painted.size()==2,"actual native render traversal commits both synthetic controls");
        check(((Integer)get("MobileRuntime$Widget","x",painted.get(1)))==65&&((Integer)get("MobileRuntime$Widget","y",painted.get(1)))==80,"committed geometry matches actual raster position");
        int[] before=(int[])call("ha","na",renderer,new Class<?>[]{int.class,int.class,int.class,int.class},0,0,width,height);
        check((before[90*width+80]&0xffffff)==0x987046,"native widget traversal paints intended rectangle");
        ViewportState v=new ViewportState(7,0,0,width,height,width,height,width,height);set("MobileRuntime","viewport",rt,v);
        MobileBridge.publishUi(new UiFrameSnapshot(1,7,v,Collections.emptyList(),"Synthetic native render fixture",false,false));
        call("ha","KA",renderer,new Class<?>[]{int.class,int.class,int.class,int.class},70,80,width-10,height-10);
        call("MobileCanvasMenu","paint",null,new Class<?>[0]);int[] clip=new int[4];call("ha","K",renderer,new Class<?>[]{int[].class},(Object)clip);
        check(Arrays.equals(clip,new int[]{70,80,width-10,height-10}),"native menu restores renderer clip");
        MobileChrome.Frame chrome=MobileChrome.frame();check(chrome!=null&&chrome.cancel.isEmpty(),"only one menu glyph while no selection is armed");
        int[] after=(int[])call("ha","na",renderer,new Class<?>[]{int.class,int.class,int.class,int.class},0,0,width,height);int changed=0;boolean outside=false;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(before[y*width+x]!=after[y*width+x]){changed++;outside|=!chrome.menu.contains(x,y);}
        check(changed>0&&!outside,"menu changes only its small native raster rectangle, not the scene scale");
        Files.createDirectories(Paths.get("build/reports/mobile"));BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,width,height,after,0,width);ImageIO.write(image,"png",Paths.get("build/reports/mobile/"+(released?"released-":"")+"native-canvas-synthetic.png").toFile());
        // Drive the same runtime hit/capture path as bridge pointer input, not only the host mouse shortcut.
        double x=chrome.menu.getCenterX(),y=chrome.menu.getCenterY();Object target=call("MobileRuntime","hit",rt,new Class<?>[]{double.class,double.class},x,y);
        call("MobileRuntime","tap",rt,new Class<?>[]{GestureRecognizer.Target.class,double.class,double.class},target,x,y);
        check(MobileBridge.hostOverlayActive(),"menu tap claims input before EDT dialog construction");flush();
        JDialog tools=null;for(Window w:Window.getWindows())if(w instanceof JDialog&&w.isVisible()&&"Client settings".equals(((JDialog)w).getTitle()))tools=(JDialog)w;
        check(tools!=null,"native glyph opens real utility menu");final JDialog menu=tools;
        check(MobileChrome.frame()==null,"menu glyph cannot be reactivated behind host dialog");
        for(Component c:children(menu))if(c instanceof JButton){String text=((JButton)c).getText();check(!text.equals("Text entry")&&!text.equals("Camera controls")&&!text.equals("Alternative Panels")&&!text.equals("Actions on next game tap"),"settings have no helper gameplay routes");}
        MobileBridge.drain();SwingUtilities.invokeAndWait(()->button(menu,"Close").doClick());flush();
        check(!MobileBridge.hostOverlayActive(),"closing settings releases input ownership");
        for(MobileBridge.Command command:MobileBridge.drain())check("cancel".equals(command.type),"closing settings creates no game command");
        MobileChrome.key(true);MobileChrome.key(true);flush();int count=0;for(Window w:Window.getWindows())if(w instanceof JDialog&&w.isVisible()&&"Client settings".equals(((JDialog)w).getTitle()))count++;
        check(count==1,"F10 recovery opens exactly one menu despite key repeat");MobileChrome.key(false);
        check(children(frame).stream().noneMatch(c->c instanceof JButton),"toolbar stays absent after interactions");
        call("ha","method3635",renderer,new Class<?>[]{byte.class},(byte)-115);set("Class348_Sub8","aHa6654",null,null);
        String report=(released?"Released":"Development")+" native Canvas assertions: "+checks+" passed\nReal AWT host, heavyweight Canvas, native software renderer and widget traversal. Synthetic rectangles only; no cache/server/Android test.\n";
        Files.write(Paths.get("build/reports/mobile/"+(released?"released-":"")+"native-canvas.txt"),report.getBytes(StandardCharsets.UTF_8));System.out.print(report);
    }
}
