import com.voidclient.mobile.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/** An explicit editing session; only this session's acknowledged command may authorize rebinding. */
final class MobileTextEditor extends JPanel implements Scrollable {
    private final JPasswordField field=new JPasswordField();
    private final JTextArea status=MobileWidgets.text("Focus the intended game field before inserting text.");
    private final JButton insert=new JButton("Insert once"), next=new JButton("Next field"), enter=new JButton("Enter");
    private final Timer poll;
    private long session;
    private int request;
    private boolean changed=true, closed;
    MobileTextEditor(long initialSession) {
        super(new BorderLayout(6,6)); session=initialSession;
        field.setFont(field.getFont().deriveFont(18f));
        field.getAccessibleContext().setAccessibleName("Draft for the explicitly focused game field");
        status.getAccessibleContext().setAccessibleName("Text delivery status");
        JPanel header=new JPanel(new GridLayout(0,1,4,4));
        header.add(MobileWidgets.text("Drafts are retained: delivery does not prove field acceptance."));
        JCheckBox show=new JCheckBox("Show draft text (not for passwords)");
        show.addActionListener(e -> field.setEchoChar(show.isSelected()?'\0':'\u2022')); header.add(show);
        add(header,BorderLayout.NORTH); add(field,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(4,4));
        JPanel keys=new JPanel(new GridLayout(0,2,4,4));
        keys.add(insert); keys.add(enter); keys.add(next);
        JButton back=new JButton("Backspace"), escape=new JButton("Escape"), clear=new JButton("Clear draft");
        keys.add(back); keys.add(escape); keys.add(clear);
        for(Component component:keys.getComponents()){component.setFont(component.getFont().deriveFont(MobileWidgets.fontSize()));component.setPreferredSize(new Dimension(100,52));}
        show.setPreferredSize(new Dimension(120,48));status.setRows(3);field.setPreferredSize(new Dimension(100,52));
        footer.add(keys,BorderLayout.CENTER); footer.add(status,BorderLayout.SOUTH); add(footer,BorderLayout.SOUTH);
        insert.addActionListener(e -> {
            if(request!=0 || !changed) return;
            char[] draft=field.getPassword();
            try { request=MobileBridge.insertText(new String(draft),session); changed=false; updateEnabled(); }
            catch(IllegalArgumentException ex) { message(ex.getMessage()); }
            finally { java.util.Arrays.fill(draft,'\0'); }
        });
        next.addActionListener(e -> key(KeyEvent.VK_TAB)); enter.addActionListener(e -> key(KeyEvent.VK_ENTER));
        back.addActionListener(e -> key(KeyEvent.VK_BACK_SPACE)); escape.addActionListener(e -> key(KeyEvent.VK_ESCAPE));
        clear.addActionListener(e -> { if(request==0) { field.setText(""); changed=true; updateEnabled(); } });
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { change(); }
            public void removeUpdate(DocumentEvent e) { change(); }
            public void changedUpdate(DocumentEvent e) { change(); }
            void change() { changed=true; updateEnabled(); }
        });
        poll=new Timer(75,e -> refresh()); poll.start();
    }
    private void key(int code) {
        if(request!=0) return;
        request=MobileBridge.editKey(code,session); updateEnabled();
    }
    private void refresh() {
        if(closed || request==0) return;
        EditReceipts.Receipt receipt=MobileBridge.editReceipt(request);
        if(receipt==null) { message("Receipt no longer available. Draft retained; verify the client before retrying."); request=0; changed=false; updateEnabled(); return; }
        message(receipt.message);
        if(!receipt.terminal()) return;
        if(receipt.state==EditReceipts.State.DELIVERED && receipt.session==session) {
            // Never adopt an arbitrary newest session. This token belongs to our own explicit key command.
            if(receipt.nextSession!=session) { session=receipt.nextSession; changed=false; }
        } else changed=true;
        request=0; updateEnabled();
    }
    private void message(String value) {
        status.setText(value);
        status.getAccessibleContext().setAccessibleDescription(value);
    }
    private static String escape(String value) { return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    private void updateEnabled() {
        insert.setEnabled(request==0 && changed); enter.setEnabled(request==0); next.setEnabled(request==0);
        field.setEditable(request==0);
    }
    public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
    public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 24;}
    public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(24,r.height*3/4);}
    public boolean getScrollableTracksViewportWidth(){return true;}
    public boolean getScrollableTracksViewportHeight(){return false;}
    void focusEditor() { field.requestFocusInWindow(); }
    void close() { closed=true; poll.stop(); field.setText(""); MobileBridge.setTextFocus(false); }
}
