package com.voidclient.mobile;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.accessibility.*;
import javax.swing.SwingUtilities;

/** Java virtual accessibility tree. Export to Android services depends on the embedding JVM, not this class. */
public final class MobileAccessibleCanvas extends AccessibleContext implements AccessibleComponent {
    private static final Map<Canvas,MobileAccessibleCanvas> roots=Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicBoolean scheduled=new AtomicBoolean();
    private final java.lang.ref.WeakReference<Canvas> canvas;
    private volatile long focus;
    public static AccessibleContext forCanvas(Canvas c) {
        synchronized(roots){MobileAccessibleCanvas a=roots.get(c);if(a==null){a=new MobileAccessibleCanvas(c);roots.put(c,a);}return a;}
    }
    private MobileAccessibleCanvas(Canvas c){canvas=new java.lang.ref.WeakReference<>(c);setAccessibleName("Void game interface");setAccessibleDescription("Open Mobile Panels for reflowed text and controls. Native game operations are revalidated before execution.");}
    public static void changed(){
        if(roots.isEmpty()||!scheduled.compareAndSet(false,true))return;
        SwingUtilities.invokeLater(()->{scheduled.set(false);synchronized(roots){for(MobileAccessibleCanvas a:roots.values())a.firePropertyChange(ACCESSIBLE_VISIBLE_DATA_PROPERTY,null,MobileBridge.ui().layoutRevision);}});
    }
    private List<UiFrameSnapshot.Node> nodes(){List<UiFrameSnapshot.Node> result=new ArrayList<>();for(UiFrameSnapshot.Node n:MobileBridge.ui().nodes)if(n.visible&&(n.role==UiFrameSnapshot.Role.ITEM||!n.actions.isEmpty()||!n.text.isEmpty()))result.add(n);result.addAll(MobileChrome.nodes());return result;}
    public AccessibleRole getAccessibleRole(){return AccessibleRole.CANVAS;}
    public AccessibleStateSet getAccessibleStateSet(){AccessibleStateSet s=new AccessibleStateSet();if(isEnabled())s.add(AccessibleState.ENABLED);if(isShowing())s.add(AccessibleState.SHOWING);s.add(AccessibleState.FOCUSABLE);return s;}
    public Accessible getAccessibleParent(){Canvas c=canvas.get();return c!=null&&c.getParent() instanceof Accessible?(Accessible)c.getParent():null;}
    public int getAccessibleIndexInParent(){Canvas c=canvas.get();if(c==null||c.getParent()==null)return -1;Component[] children=c.getParent().getComponents();for(int i=0;i<children.length;i++)if(children[i]==c)return i;return -1;}
    public int getAccessibleChildrenCount(){return nodes().size();}
    public Accessible getAccessibleChild(int i){List<UiFrameSnapshot.Node> n=nodes();return i<0||i>=n.size()?null:new Node(n.get(i),i);}
    public Locale getLocale(){Canvas c=canvas.get();return c==null?Locale.getDefault():c.getLocale();}
    public AccessibleComponent getAccessibleComponent(){return this;}
    public Color getBackground(){Canvas c=canvas.get();return c==null?Color.BLACK:c.getBackground();}
    public void setBackground(Color value){}
    public Color getForeground(){Canvas c=canvas.get();return c==null?Color.WHITE:c.getForeground();}
    public void setForeground(Color value){}
    public Cursor getCursor(){Canvas c=canvas.get();return c==null?Cursor.getDefaultCursor():c.getCursor();}
    public void setCursor(Cursor value){}
    public Font getFont(){Canvas c=canvas.get();return c==null?new Font("Dialog",Font.PLAIN,16):c.getFont();}
    public void setFont(Font value){}
    public FontMetrics getFontMetrics(Font font){Canvas c=canvas.get();return c==null?null:c.getFontMetrics(font);}
    public boolean isEnabled(){Canvas c=canvas.get();return c!=null&&c.isEnabled()&&!MobileBridge.suspended()&&!MobileBridge.hostOverlayActive();}
    public void setEnabled(boolean value){}
    public boolean isVisible(){Canvas c=canvas.get();return c!=null&&c.isVisible();}
    public void setVisible(boolean value){}
    public boolean isShowing(){Canvas c=canvas.get();return c!=null&&c.isShowing();}
    public boolean contains(Point p){return new Rectangle(getSize()).contains(p);}
    public Point getLocationOnScreen(){Canvas c=canvas.get();return c!=null&&c.isShowing()?c.getLocationOnScreen():null;}
    public Point getLocation(){Canvas c=canvas.get();return c==null?new Point():c.getLocation();}
    public void setLocation(Point p){}
    public Rectangle getBounds(){return new Rectangle(getLocation(),getSize());}
    public void setBounds(Rectangle b){}
    public Dimension getSize(){Canvas c=canvas.get();return c==null?new Dimension():c.getSize();}
    public void setSize(Dimension d){}
    public Accessible getAccessibleAt(Point p){List<UiFrameSnapshot.Node> n=nodes();for(int i=n.size()-1;i>=0;i--){Node child=new Node(n.get(i),i);if(child.getBounds().contains(p))return child;}return null;}
    public boolean isFocusTraversable(){return true;}
    public void requestFocus(){Canvas c=canvas.get();if(c!=null)c.requestFocusInWindow();}
    public void addFocusListener(FocusListener l){Canvas c=canvas.get();if(c!=null)c.addFocusListener(l);}
    public void removeFocusListener(FocusListener l){Canvas c=canvas.get();if(c!=null)c.removeFocusListener(l);}

    private final class Node extends AccessibleContext implements Accessible,AccessibleAction,AccessibleComponent {
        final UiFrameSnapshot.Node node;final int index;
        Node(UiFrameSnapshot.Node n,int index){node=n;this.index=index;setAccessibleName(n.text.isEmpty()?n.label+(n.quantity>1?" quantity "+n.quantity:""):n.text);setAccessibleDescription(n.groupLabel+", "+n.role.toString().toLowerCase(Locale.ROOT));}
        public AccessibleContext getAccessibleContext(){return this;}
        private boolean current(){if(MobileChrome.isChrome(node.token))return MobileChrome.current(node);UiFrameSnapshot.Node now=MobileBridge.ui().node(node.token);return now!=null&&now.version==node.version&&now.enabled;}
        public AccessibleRole getAccessibleRole(){return node.actions.isEmpty()&&node.role==UiFrameSnapshot.Role.TEXT?AccessibleRole.LABEL:AccessibleRole.PUSH_BUTTON;}
        public AccessibleStateSet getAccessibleStateSet(){AccessibleStateSet s=new AccessibleStateSet();if(isEnabled())s.add(AccessibleState.ENABLED);if(isVisible())s.add(AccessibleState.VISIBLE);if(isShowing())s.add(AccessibleState.SHOWING);s.add(AccessibleState.FOCUSABLE);if(focus==node.token)s.add(AccessibleState.FOCUSED);return s;}
        public Accessible getAccessibleParent(){return canvas.get();}
        public int getAccessibleIndexInParent(){return index;}
        public int getAccessibleChildrenCount(){return 0;}
        public Accessible getAccessibleChild(int i){return null;}
        public Locale getLocale(){return MobileAccessibleCanvas.this.getLocale();}
        public AccessibleAction getAccessibleAction(){return this;}
        public AccessibleComponent getAccessibleComponent(){return this;}
        public int getAccessibleActionCount(){if(MobileChrome.isChrome(node.token))return 1;return node.actions.size()+(node.role==UiFrameSnapshot.Role.TEXT&&node.actions.isEmpty()?0:1);}
        public String getAccessibleActionDescription(int i){return i==0&&MobileChrome.isChrome(node.token)?node.label:i==0?"Choose native actions":i>0&&i<=node.actions.size()?node.actions.get(i-1).label:null;}
        public boolean doAccessibleAction(int i){if(!isEnabled()||i<0||i>=getAccessibleActionCount())return false;if(MobileChrome.isChrome(node.token))return MobileChrome.activate(node.token,node.version);MobileBridge.nodeAction(i==0?"nodeActions":"nodeOp",i==0?0:node.actions.get(i-1).operation,node.token,node.version);return true;}
        public Color getBackground(){return MobileAccessibleCanvas.this.getBackground();}public void setBackground(Color c){}
        public Color getForeground(){return MobileAccessibleCanvas.this.getForeground();}public void setForeground(Color c){}
        public Cursor getCursor(){return MobileAccessibleCanvas.this.getCursor();}public void setCursor(Cursor c){}
        public Font getFont(){return MobileAccessibleCanvas.this.getFont();}public void setFont(Font f){}
        public FontMetrics getFontMetrics(Font f){return MobileAccessibleCanvas.this.getFontMetrics(f);}
        public boolean isEnabled(){return current()&&MobileAccessibleCanvas.this.isEnabled();}public void setEnabled(boolean value){}
        public boolean isVisible(){return node.visible&&current();}public void setVisible(boolean value){}
        public boolean isShowing(){return isVisible()&&MobileAccessibleCanvas.this.isShowing();}
        public boolean contains(Point p){return new Rectangle(getSize()).contains(p);}
        public Point getLocationOnScreen(){Point origin=MobileAccessibleCanvas.this.getLocationOnScreen();if(origin==null)return null;Point p=getLocation();return new Point(origin.x+p.x,origin.y+p.y);}
        public Point getLocation(){return getBounds().getLocation();}public void setLocation(Point p){}
        public Rectangle getBounds(){ViewportState v=MobileBridge.ui().viewport;if(v==null)return new Rectangle();return new Rectangle((int)Math.floor((double)node.clip.x*v.width/v.logicalWidth),(int)Math.floor((double)node.clip.y*v.height/v.logicalHeight),(int)Math.ceil((double)node.clip.width*v.width/v.logicalWidth),(int)Math.ceil((double)node.clip.height*v.height/v.logicalHeight));}
        public void setBounds(Rectangle b){}public Dimension getSize(){return getBounds().getSize();}public void setSize(Dimension d){}
        public Accessible getAccessibleAt(Point p){return null;}public boolean isFocusTraversable(){return isEnabled();}
        public void requestFocus(){if(isEnabled()){long old=focus;focus=node.token;MobileAccessibleCanvas.this.requestFocus();MobileAccessibleCanvas.this.firePropertyChange(ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY,old,this);}}
        public void addFocusListener(FocusListener l){MobileAccessibleCanvas.this.addFocusListener(l);}public void removeFocusListener(FocusListener l){MobileAccessibleCanvas.this.removeFocusListener(l);}
    }
}
