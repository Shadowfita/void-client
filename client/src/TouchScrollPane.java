import com.voidclient.mobile.MobileConfig;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;

/** Swipe scrolling for actual host controls; listeners are removed when a window is disposed. */
final class TouchScrollPane extends JScrollPane {
    private Point down,origin;
    private AbstractButton pressed;
    private boolean dragging,installed;
    private final AWTEventListener listener=this::event;
    TouchScrollPane(Component view) { super(view);setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);getVerticalScrollBar().setUnitIncrement(24); }
    @Override public void addNotify() {
        super.addNotify();
        try { Toolkit.getDefaultToolkit().addAWTEventListener(listener,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_MOTION_EVENT_MASK);installed=true; }
        catch(SecurityException ex) { installed=false; } // Page buttons remain available when global observation is unavailable.
    }
    @Override public void removeNotify() {
        if(installed) Toolkit.getDefaultToolkit().removeAWTEventListener(listener);installed=false;reset();super.removeNotify();
    }
    private void event(AWTEvent event) {
        if(!(event instanceof MouseEvent)||!isShowing()) return;
        MouseEvent e=(MouseEvent)event;
        if(!(e.getSource() instanceof Component)) return;
        Component source=(Component)e.getSource();
        if(e.getID()==MouseEvent.MOUSE_PRESSED) {
            reset();
            if(e.getButton()!=MouseEvent.BUTTON1||!SwingUtilities.isDescendingFrom(source,getViewport())) return;
            for(Component c=source;c!=null&&c!=getViewport();c=c.getParent()) {
                if(c instanceof JTextComponent && ((JTextComponent)c).isEditable() || c instanceof JSlider || c instanceof JScrollBar) return;
                if(c instanceof JScrollPane && c!=this) return;
                if(c instanceof AbstractButton) pressed=(AbstractButton)c;
            }
            down=SwingUtilities.convertPoint(source,e.getPoint(),this);origin=getViewport().getViewPosition();
        } else if(down!=null && (e.getID()==MouseEvent.MOUSE_DRAGGED||e.getID()==MouseEvent.MOUSE_RELEASED)) {
            Point p=SwingUtilities.convertPoint(source,e.getPoint(),this);
            if(!dragging&&p.distance(down)>MobileConfig.slopPixels()) dragging=true;
            if(dragging) {
                if(pressed!=null) { pressed.getModel().setArmed(false);pressed.getModel().setPressed(false); }
                int max=Math.max(0,getViewport().getViewSize().height-getViewport().getExtentSize().height);
                getViewport().setViewPosition(new Point(origin.x,Math.max(0,Math.min(max,origin.y+down.y-p.y))));e.consume();
            }
            if(e.getID()==MouseEvent.MOUSE_RELEASED) reset();
        }
    }
    private void reset() {down=null;origin=null;pressed=null;dragging=false;}
    void page(int direction) {
        JViewport vp=getViewport();Point p=vp.getViewPosition();
        int max=Math.max(0,vp.getViewSize().height-vp.getExtentSize().height);
        vp.setViewPosition(new Point(p.x,Math.max(0,Math.min(max,p.y+direction*Math.max(1,vp.getExtentSize().height*3/4)))));
    }
}
