import com.voidclient.mobile.MobileBridge;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/** One explicit host-surface stack. Only the top host surface accepts input. All calls on the EDT. */
final class MobileInterfaceManager {
    private static final java.util.List<Window> stack=new ArrayList<>();
    private static final Map<Window,Component> restore=new IdentityHashMap<>();
    static void track(Window window) {
        if(stack.contains(window)) return;
        restore.put(window,KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        for(Window old:stack) if(old.isDisplayable()) old.setEnabled(false);
        stack.add(window);window.setEnabled(true);
        if(window instanceof RootPaneContainer) {
            JRootPane root=((RootPaneContainer)window).getRootPane();
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"mobile-back");
            root.getActionMap().put("mobile-back",new AbstractAction() { public void actionPerformed(ActionEvent e) { back(); } });
        }
        window.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                stack.remove(window);Component focus=restore.remove(window);
                Window top=top();if(top!=null) {top.setEnabled(true);top.toFront();}
                if(focus!=null&&focus.isShowing()&&focus.isEnabled()) focus.requestFocusInWindow();
            }
        });
    }
    static Window top() { for(int i=stack.size()-1;i>=0;i--) if(stack.get(i).isDisplayable()) return stack.get(i);return null; }
    static void back() { Window top=top();if(top!=null) {MobileBridge.cancel();top.dispose();} }
    static void closeAll() {for(Window w:new ArrayList<>(stack))if(w.isDisplayable())w.dispose();}
    static int depth() {return stack.size();}
    private MobileInterfaceManager() {}
}
