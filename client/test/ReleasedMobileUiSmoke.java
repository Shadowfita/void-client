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

/** Reflection uses the generated mapping; the classpath must exclude unobfuscated main output. */
public final class ReleasedMobileUiSmoke {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static String mapped(Path map,String name)throws Exception{for(String line:Files.readAllLines(map,StandardCharsets.UTF_8))if(line.startsWith(name+" -> ")&&line.endsWith(":"))return line.substring((name+" -> ").length(),line.length()-1);throw new AssertionError("Mapping missing "+name);}
    static List<Component> components(Container c){List<Component> out=new ArrayList<>();for(Component x:c.getComponents()){out.add(x);if(x instanceof Container)out.addAll(components((Container)x));}return out;}
    public static void main(String[] args)throws Exception {
        try {run(Paths.get(args[0]));}catch(Throwable t){t.printStackTrace();System.exit(1);}finally{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});}
    }
    static void run(Path map)throws Exception {
        System.setProperty("void.mobile","true");System.setProperty("user.home",Files.createTempDirectory("released-mobile-ui-").toString());
        check(InterfaceRegistry.groupCount()==408,"public metadata included in final obfuscated artifact");
        Class<?> panels=Class.forName(mapped(map,"MobilePanels"));
        check(panels.getProtectionDomain().getCodeSource().getLocation().toString().endsWith("void-client-jarrunner-mobile.jar"),"responsive implementation loaded only from release JAR");
        Constructor<?> constructor=panels.getDeclaredConstructor();constructor.setAccessible(true);
        List<UiFrameSnapshot.Node> items=new ArrayList<>();for(int i=0;i<28;i++)items.add(new UiFrameSnapshot.Node(i+1,0,1,149<<16,i,5,0,100+i,1,i,new UiFrameSnapshot.Bounds(0,0,32,32),new UiFrameSnapshot.Bounds(0,0,32,32),UiFrameSnapshot.Role.ITEM,"Release item "+i,"Inventory","inventory","",false,true,true,Arrays.asList(new UiFrameSnapshot.Action(1,"Use"))));
        MobileBridge.publishUi(new UiFrameSnapshot(1,1,new ViewportState(1,0,0,390,844,390,844,390,844),items,"Synthetic release fixture; no live game data",false,false));
        JFrame frame=new JFrame();JPanel[] panel={null};SwingUtilities.invokeAndWait(()->{try{panel[0]=(JPanel)constructor.newInstance();frame.add(panel[0]);frame.setSize(390,844);frame.setVisible(true);}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(700);
        SwingUtilities.invokeAndWait(()->{
            int count=0;JButton first=null;
            for(Component c:components(panel[0]))if(c instanceof JButton){JButton b=(JButton)c;String n=b.getAccessibleContext().getAccessibleName();if(n!=null&&n.startsWith("Release item")){count++;check(b.getHeight()>=48,"release item touch size");if(first==null)first=b;}}
            check(count==28,"all 28 identities survive packaged build");MobileBridge.drain();first.doClick();List<MobileBridge.Command> commands=MobileBridge.drain();check(commands.size()==1&&commands.get(0).target==1,"obfuscated button retains native identity command");
            try {Files.createDirectories(Paths.get("build/reports/mobile"));BufferedImage image=new BufferedImage(panel[0].getWidth(),panel[0].getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel[0].printAll(g);g.dispose();ImageIO.write(image,"png",Paths.get("build/reports/mobile/released-panels-synthetic.png").toFile());((AutoCloseable)panel[0]).close();}catch(Exception e){throw new RuntimeException(e);}frame.dispose();
        });
        String text="Obfuscated responsive UI assertions: "+checks+" passed\nActual release JAR; synthetic widgets and captured command, no Android/cache/server claim.\n";
        Files.write(Paths.get("build/reports/mobile/released-ui.txt"),text.getBytes(StandardCharsets.UTF_8));System.out.print(text);
    }
}
