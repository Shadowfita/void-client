import com.voidclient.mobile.AccessibilityPreferences;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Measured, wrapping host controls. Native game font resources are not modified by these controls. */
final class MobileWidgets {
    static String html(String text) {return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br>");}
    static float fontSize() {return 16f*AccessibilityPreferences.current().textPercent/100f;}
    static JTextArea text(String text) {
        JTextArea t=new JTextArea(text);t.setEditable(false);t.setLineWrap(true);t.setWrapStyleWord(true);t.setOpaque(false);
        t.setFont(t.getFont().deriveFont(fontSize()));t.getAccessibleContext().setAccessibleName(text);theme(t);return t;
    }
    static JButton button(String label,Runnable action) {return new WrappingButton(label,action);}
    static void theme(Component c) {
        if(AccessibilityPreferences.current().highContrast) {c.setBackground(Color.BLACK);c.setForeground(Color.WHITE);}
    }
    /** Paint measured lines directly: Swing HTML's CSS-pixel conversion can overflow narrow buttons. */
    static class WrappingButton extends JButton {
        private final String label;
        private Icon picture;
        WrappingButton(String label,Runnable action) {
            this.label=label;getAccessibleContext().setAccessibleName(label);setFont(getFont().deriveFont(fontSize()));
            setMargin(new Insets(8,8,8,8));setHorizontalAlignment(LEFT);setFocusPainted(true);setText("");theme(this);
            addActionListener(e -> action.run());
            addComponentListener(new ComponentAdapter() {public void componentResized(ComponentEvent e) { revalidate(); }});
        }
        @Override public void setIcon(Icon icon) {picture=icon;revalidate();repaint();}
        // The default ButtonUI paints the background/focus only; this class places both icon and text.
        @Override public Icon getIcon() {return null;}
        java.util.List<String> linesForWidth(int available) {
            java.util.List<String> result=new java.util.ArrayList<>();FontMetrics fm=getFontMetrics(getFont());
            int width=Math.max(1,available);
            for(String paragraph:label.split("\\n",-1)) {
                if(paragraph.isEmpty()){result.add("");continue;}
                String remaining=paragraph;
                while(!remaining.isEmpty()) {
                    int fit=0,lastSpace=-1;
                    for(int i=0;i<remaining.length();) {
                        int next=i+Character.charCount(remaining.codePointAt(i));
                        if(fm.stringWidth(remaining.substring(0,next))>width&&fit>0)break;
                        fit=next;if(Character.isWhitespace(remaining.charAt(i)))lastSpace=next;i=next;
                    }
                    if(fit<remaining.length()&&lastSpace>0)fit=lastSpace;
                    if(fit==0)fit=Character.charCount(remaining.codePointAt(0));
                    result.add(remaining.substring(0,fit).trim());remaining=remaining.substring(fit).trim();
                }
            }
            return result;
        }
        int preferredHeightForWidth(int width) {
            Insets in=getInsets();int textWidth=Math.max(1,width-in.left-in.right-4);
            int icon=picture==null?0:picture.getIconHeight()+6;
            return Math.max(48,in.top+in.bottom+4+icon+linesForWidth(textWidth).size()*getFontMetrics(getFont()).getHeight());
        }
        @Override public Dimension getPreferredSize() {
            int width=getWidth()>0?getWidth():180;
            return new Dimension(Math.max(72,width),preferredHeightForWidth(width));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g=(Graphics2D)graphics.create();
            try {
                Insets in=getInsets();int left=in.left+2,top=in.top+2;
                int width=Math.max(1,getWidth()-left-in.right-2),height=Math.max(1,getHeight()-top-in.bottom-2);
                g.clipRect(left,top,width,height);g.setFont(getFont());
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color disabled=UIManager.getColor("Button.disabledText");g.setColor(isEnabled()?getForeground():disabled==null?Color.GRAY:disabled);
                FontMetrics fm=g.getFontMetrics();java.util.List<String> lines=linesForWidth(width);
                int icon=picture==null?0:picture.getIconHeight()+6;
                int y=top+Math.max(0,(height-icon-lines.size()*fm.getHeight())/2);
                if(picture!=null){picture.paintIcon(this,g,left+Math.max(0,(width-picture.getIconWidth())/2),y);y+=icon;}
                y+=fm.getAscent();for(String line:lines){g.drawString(line,left,y);y+=fm.getHeight();}
            } finally {g.dispose();}
        }
    }
    static final class Vertical extends JPanel implements Scrollable {
        Vertical() {super(new GridBagLayout());setOpaque(true);theme(this);}
        void row(Component component) {GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=getComponentCount();c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.NORTHWEST;c.insets=new Insets(3,4,3,4);add(component,c);}
        public Dimension getPreferredScrollableViewportSize() {return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int o,int d) {return 24;}
        public int getScrollableBlockIncrement(Rectangle r,int o,int d) {return Math.max(1,r.height*3/4);}
        public boolean getScrollableTracksViewportWidth() {return true;}
        public boolean getScrollableTracksViewportHeight() {return false;}
    }
    private MobileWidgets() {}
}
