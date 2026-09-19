import com.voidclient.mobile.*;
import javax.swing.*;
import javax.accessibility.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual Swing and Java accessibility, synthetic native model. No Android or live game screenshots. */
public final class MobilePanelsRegression {
    static int checks;static Throwable edtFailure;
    static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
    static void edt(Runnable r)throws Exception{SwingUtilities.invokeAndWait(r);}
    static void settle()throws Exception{Thread.sleep(300);edt(()->{});if(edtFailure!=null)throw new AssertionError("EDT failure",edtFailure);}
    static Object get(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static List<Component> descendants(Container p){List<Component> all=new ArrayList<>();for(Component c:p.getComponents()){all.add(c);if(c instanceof Container)all.addAll(descendants((Container)c));}return all;}
    static JButton button(Container p,String name){for(Component c:descendants(p))if(c instanceof JButton&&(name.equals(((JButton)c).getAccessibleContext().getAccessibleName())||name.equals(((JButton)c).getText())))return(JButton)c;throw new AssertionError("Missing "+name);}
    static UiFrameSnapshot fixture(int group,int count,boolean items,int width,int height){
        List<UiFrameSnapshot.Node> nodes=new ArrayList<>();
        for(int i=0;i<count;i++){
            String name=items?"Item "+(i+1)+" with a readable name":"Conversation choice "+(i+1);
            nodes.add(new UiFrameSnapshot.Node(i+1,0,10,group<<16,i,items?5:4,0,items?100+i:-1,items?i+1:0,i,new UiFrameSnapshot.Bounds(i%4*40,i/4*36,36,32),new UiFrameSnapshot.Bounds(i%4*40,i/4*36,36,32),items?UiFrameSnapshot.Role.ITEM:UiFrameSnapshot.Role.TEXT,name,InterfaceRegistry.name(group),InterfaceRegistry.family(group),items?"":"An example dialogue option that must wrap without hiding the final choice.",false,items,true,Arrays.asList(new UiFrameSnapshot.Action(1,items?"Use":"Continue"))));
        }
        return new UiFrameSnapshot(1,1,new ViewportState(1,0,0,width,height,width,height,width,height),nodes,"Synthetic fixture; not a live game or Android screenshot",false,false);
    }
    public static void main(String[] args)throws Exception{
        try{Thread.setDefaultUncaughtExceptionHandler((t,e)->{edtFailure=e;e.printStackTrace();});System.setProperty("void.mobile","true");System.setProperty("user.home",Files.createTempDirectory("mobile-ui-").toString());Files.createDirectories(Paths.get("build/reports/mobile"));panels();text();scroll();accessibility();}
        catch(Throwable t){t.printStackTrace();System.exit(1);}finally{edt(()->{for(Window w:Window.getWindows())w.dispose();});}
        String report="Responsive panels, text, swipe and Java-accessibility assertions: "+checks+" passed\nActual Swing controls under Xvfb; synthetic interface data, not Android/TalkBack/live cache.\n";Files.write(Paths.get("build/reports/mobile/panels.txt"),report.getBytes("UTF-8"));System.out.print(report);
    }
    static void panels()throws Exception{
        for(int[] v:new int[][]{{320,568},{390,844},{844,390},{1024,768}}){
            JFrame frame=new JFrame();final MobilePanels[] panels={null};
            edt(()->{MobileBridge.publishUi(fixture(149,28,true,v[0],v[1]));panels[0]=new MobilePanels();frame.add(panels[0]);frame.setSize(v[0],v[1]);frame.setVisible(true);});settle();
            MobilePanels p=panels[0];check(button(p,"Text").isShowing(),"editor accessible from active panel");
            JScrollPane sc=(JScrollPane)get(p,"scroll");check(sc.getViewport().getHeight()>60,"usable scene-panel body at "+v[0]+"x"+v[1]);
            check(sc.getViewport().getViewSize().width<=sc.getViewport().getExtentSize().width,"no horizontal host panning at "+v[0]);
            int itemButtons=0;for(Component c:descendants(p))if(c instanceof JButton){JButton b=(JButton)c;if(b.getAccessibleContext().getAccessibleName().startsWith("Item")){itemButtons++;check(b.getHeight()>=48,"measured touch row");}}
            check(itemButtons==28,"all inventory items present at "+v[0]);
            edt(()->{BufferedImage image=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();p.printAll(g);g.dispose();try{ImageIO.write(image,"png",Paths.get("build/reports/mobile/panels-"+v[0]+"x"+v[1]+"-synthetic.png").toFile());}catch(Exception e){throw new RuntimeException(e);}});
            edt(()->{p.close();frame.dispose();});settle();
        }
        JFrame f=new JFrame();final MobilePanels[] p={null};edt(()->{MobileBridge.publishUi(fixture(762,800,true,390,844));p[0]=new MobilePanels();f.add(p[0]);f.setSize(390,844);f.setVisible(true);});settle();
        int count=0;for(Component c:descendants(p[0]))if(c instanceof JButton&&((JButton)c).getAccessibleContext().getAccessibleName().startsWith("Item"))count++;
        check(count==96,"large bank window bounded to 96 native identities per page");MobileBridge.drain();edt(()->button(p[0],"Next").doClick());settle();
        check(button(p[0],"Item 97 with a readable name × 97")!=null,"next page keeps original slot identity rather than renumbering");
        edt(()->{p[0].close();f.dispose();});settle();
        AccessibilityPreferences a=AccessibilityPreferences.current();a.textPercent=200;a.highContrast=true;AccessibilityPreferences.apply(a,false);
        JFrame big=new JFrame();final MobilePanels[] q={null};edt(()->{MobileBridge.publishUi(fixture(64,5,false,390,844));q[0]=new MobilePanels();big.add(q[0]);big.setSize(390,844);big.setVisible(true);});settle();
        check(((JScrollPane)get(q[0],"scroll")).getViewport().getHeight()>60,"200% text keeps content accessible through scroll");
        edt(()->{q[0].close();big.dispose();});AccessibilityPreferences.apply(new AccessibilityPreferences(),false);settle();
    }
    static void text()throws Exception{
        MobileBridge.cancel();MobileBridge.drain();MobileTextEditor[] e={null};JFrame f=new JFrame();edt(()->{e[0]=new MobileTextEditor(700);f.add(new TouchScrollPane(e[0]));f.setSize(390,700);f.setVisible(true);});settle();
        JPasswordField field=(JPasswordField)get(e[0],"field");edt(()->{field.setText("keep this draft");button(e[0],"Insert once").doClick();});
        List<MobileBridge.Command> queued=MobileBridge.drain();check(queued.size()==1,"Insert enqueues one edit");MobileBridge.Command first=queued.get(0);check(new String(field.getPassword()).equals("keep this draft"),"A6 draft not erased while queued");
        EditReceipts.update(first.id,EditReceipts.State.REJECTED,0,700,"Unsupported character");settle();check(new String(field.getPassword()).equals("keep this draft")&&button(e[0],"Insert once").isEnabled(),"rejected text preserved and retry explicit");
        edt(()->button(e[0],"Next field").doClick());queued=MobileBridge.drain();MobileBridge.Command tab=queued.get(0);EditReceipts.update(tab.id,EditReceipts.State.DELIVERED,0,701,"Next delivered");settle();
        edt(()->{field.setText("next field draft");button(e[0],"Insert once").doClick();});queued=MobileBridge.drain();check(queued.get(0).revision==701,"A5 open editor adopts only its own acknowledged transition");
        EditReceipts.update(queued.get(0).id,EditReceipts.State.DELIVERED,16,701,"Delivered, not proven accepted");settle();check(!button(e[0],"Insert once").isEnabled(),"successful delivery cannot be accidentally duplicated");check(new String(field.getPassword()).equals("next field draft"),"unobservable field acceptance retains backup draft");
        edt(()->{e[0].close();f.dispose();});settle();
    }
    static void scroll()throws Exception{
        JFrame frame=new JFrame();JButton[] button={null};TouchScrollPane[] pane={null};AtomicInteger activated=new AtomicInteger();
        edt(()->{JPanel content=new JPanel(new GridLayout(30,1));for(int i=0;i<30;i++){JButton b=new JButton("Row "+i);b.setPreferredSize(new Dimension(200,60));b.addActionListener(e->activated.incrementAndGet());content.add(b);if(i==4)button[0]=b;}pane[0]=new TouchScrollPane(content);frame.add(pane[0]);frame.setSize(320,500);frame.setVisible(true);});settle();
        Robot robot=new Robot();Point origin=button[0].getLocationOnScreen();robot.mouseMove(origin.x+80,origin.y+30);robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);for(int dy=0;dy<100;dy+=10){robot.mouseMove(origin.x+80,origin.y+30-dy);robot.delay(20);}robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);settle();
        check(pane[0].getViewport().getViewPosition().y>40,"actual host swipe scrolls");check(activated.get()==0,"swiping button does not trigger its action on release");edt(frame::dispose);settle();
    }
    static void accessibility()throws Exception{
        Canvas canvas=new Canvas();canvas.setSize(400,600);MobileBridge.setHostOverlayActive(false);MobileBridge.setSuspended(false);MobileBridge.drain();MobileBridge.publishUi(fixture(149,3,true,400,600));
        AccessibleContext root=MobileAccessibleCanvas.forCanvas(canvas);check(root.getAccessibleChildrenCount()==3,"Java virtual children reflect painted snapshot");AccessibleContext first=root.getAccessibleChild(0).getAccessibleContext();check(first.getAccessibleName().startsWith("Item 1"),"Java accessibility has meaningful item label");
        check(first.getAccessibleAction().doAccessibleAction(0),"accessible operation queues validated native action request");check(MobileBridge.drain().get(0).target==1,"accessible request preserves target token");
        MobileBridge.setHostOverlayActive(true);check(!first.getAccessibleAction().doAccessibleAction(0),"accessibility does not bypass host modal ownership");MobileBridge.setHostOverlayActive(false);
        MobileBridge.publishUi(UiFrameSnapshot.empty());check(!first.getAccessibleAction().doAccessibleAction(0),"removed virtual child cannot act");
    }
}
