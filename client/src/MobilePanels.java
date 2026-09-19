import com.voidclient.mobile.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/** Responsive live-native panels: labels/layout are replaceable; native identities and permissions are not. */
final class MobilePanels extends JPanel implements AutoCloseable {
    private final JComboBox<Group> groups=new JComboBox<>();
    private final JTextField filter=new JTextField();
    private final JCheckBox empty=new JCheckBox("Show empty slots for Move");
    private final JPanel body=new MobileWidgets.Vertical();
    private final TouchScrollPane scroll=new TouchScrollPane(body);
    private final JPanel header=new JPanel(),commands=new JPanel();
    private final JTextArea status=MobileWidgets.text("");
    private final javax.swing.Timer timer;
    private final Map<Integer,Integer> scrollPositions=new HashMap<>();
    private long fingerprint=Long.MIN_VALUE;
    private int activeGroup=-1,lastWidth,itemPage;
    private static final int ITEMS_PER_PAGE=96;
    private boolean closing,updating;
    private static final class Group {
        final int id;final String label;
        Group(int id,String label) {this.id=id;this.label=label;}
        public String toString() {return label;}
    }
    MobilePanels() {
        super(new BorderLayout(6,6));
        header.setLayout(new GridLayout(0,1,4,4));
        groups.getAccessibleContext().setAccessibleName("Open native interface");
        filter.getAccessibleContext().setAccessibleName("Filter this panel by item or text. Does not send a server search.");
        filter.setBorder(BorderFactory.createTitledBorder("Filter this panel (local)"));
        header.add(groups);header.add(filter);header.add(empty);
        add(header,BorderLayout.NORTH);add(scroll,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(4,4));JPanel controls=commands;controls.setLayout(new GridLayout(0,3,4,4));
        controls.add(MobileWidgets.button("Up",()->scroll.page(-1)));controls.add(MobileWidgets.button("Down",()->scroll.page(1)));
        controls.add(MobileWidgets.button("Cancel",()->MobileBridge.nodeAction("cancelMode",0,0,0)));
        controls.add(MobileWidgets.button("Text",MobileLauncher::textDialog));
        controls.add(MobileWidgets.button("Previous",()->{if(itemPage>0){itemPage--;scrollPositions.put(activeGroup,0);scroll.getViewport().setViewPosition(new Point());fingerprint=Long.MIN_VALUE;refresh();}}));
        controls.add(MobileWidgets.button("Next",()->{itemPage++;scrollPositions.put(activeGroup,0);scroll.getViewport().setViewPosition(new Point());fingerprint=Long.MIN_VALUE;refresh();}));
        footer.add(controls,BorderLayout.CENTER);status.setRows(2);footer.add(status,BorderLayout.SOUTH);add(footer,BorderLayout.SOUTH);
        groups.addActionListener(e->{if(!updating) {rememberScroll();Group g=(Group)groups.getSelectedItem();activeGroup=g==null?-1:g.id;itemPage=0;fingerprint=Long.MIN_VALUE;refresh();}});
        empty.addActionListener(e->{fingerprint=Long.MIN_VALUE;refresh();});
        filter.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){update();}public void removeUpdate(DocumentEvent e){update();}public void changedUpdate(DocumentEvent e){update();}void update(){itemPage=0;fingerprint=Long.MIN_VALUE;refresh();}});
        addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){if(Math.abs(lastWidth-getWidth())>8){lastWidth=getWidth();boolean wide=getWidth()>=700;
            header.setLayout(new GridLayout(0,wide?3:1,4,4));commands.setLayout(new GridLayout(0,wide?6:3,4,4));status.setRows(wide?1:2);
            fingerprint=Long.MIN_VALUE;revalidate();refresh();}}});
        timer=new javax.swing.Timer(200,e->refresh());timer.start();MobileBridge.setPanelsActive(true);refresh();
    }
    private void rememberScroll() {if(activeGroup>=0)scrollPositions.put(activeGroup,scroll.getViewport().getViewPosition().y);}
    private void updateGroups(UiFrameSnapshot frame) {
        LinkedHashMap<Integer,String> available=new LinkedHashMap<>();
        for(UiFrameSnapshot.Node n:frame.nodes) if(relevant(n)) available.put(n.group,n.groupLabel);
        boolean changed=available.size()!=groups.getItemCount();
        if(!changed){int i=0;for(int id:available.keySet())if(groups.getItemAt(i++).id!=id){changed=true;break;}}
        if(!changed)return;
        updating=true;try {
            rememberScroll();groups.removeAllItems();
            for(Map.Entry<Integer,String> e:available.entrySet())groups.addItem(new Group(e.getKey(),e.getValue()));
            if(!available.containsKey(activeGroup)) {
                activeGroup=-1;
                // Prefer the currently open consequential/main panel over the background HUD, without invoking anything.
                for(UiFrameSnapshot.Node n:frame.nodes)if(available.containsKey(n.group)&&Arrays.asList("confirmation","bank","shop","dialogue").contains(n.family))activeGroup=n.group;
                if(activeGroup<0&&!available.isEmpty())activeGroup=available.keySet().iterator().next();
            }
            for(int i=0;i<groups.getItemCount();i++)if(groups.getItemAt(i).id==activeGroup)groups.setSelectedIndex(i);
            fingerprint=Long.MIN_VALUE;
        } finally {updating=false;}
    }
    private boolean relevant(UiFrameSnapshot.Node n) {
        return n.role==UiFrameSnapshot.Role.ITEM||!n.actions.isEmpty()||!n.text.trim().isEmpty()||n.role==UiFrameSnapshot.Role.MAP||n.role==UiFrameSnapshot.Role.SCROLL||n.role==UiFrameSnapshot.Role.SLIDER;
    }
    private void refresh() {
        if(closing)return;UiFrameSnapshot frame=MobileBridge.ui();updateGroups(frame);
        status.setText((frame.moveArmed?"MOVE: choose the destination slot. ":frame.targetArmed?"TARGET MODE: choose a target or cancel. ":"")+frame.status);
        List<UiFrameSnapshot.Node> selected=new ArrayList<>();String query=filter.getText().trim().toLowerCase(Locale.ROOT);
        long hash=31L*activeGroup+itemPage;int itemCount=0;
        for(UiFrameSnapshot.Node n:frame.nodes)if(n.group==activeGroup&&relevant(n)) {
            if(n.role==UiFrameSnapshot.Role.ITEM){itemCount++;if(n.item<0&&!empty.isSelected()&&!frame.moveArmed)continue;}
            if(!query.isEmpty()&&!(n.label+" "+n.text).toLowerCase(Locale.ROOT).contains(query))continue;
            selected.add(n);hash=31*hash+Objects.hash(n.token,n.version,n.text,n.actions.size(),n.movable,n.enabled);
        }
        AccessibilityPreferences preferences=AccessibilityPreferences.current();
        hash=31*hash+MobileVisuals.revision();
        hash=31*hash+Objects.hash(frame.moveArmed,frame.targetArmed,preferences.textPercent,preferences.highContrast,preferences.cellWidth,preferences.profile,lastWidth);
        if(hash==fingerprint)return;fingerprint=hash;
        int position=scrollPositions.getOrDefault(activeGroup,0);if(scroll.getViewport().getViewPosition().y>0)position=scroll.getViewport().getViewPosition().y;
        body.removeAll();body.setLayout(new BorderLayout());MobileWidgets.Vertical rows=new MobileWidgets.Vertical();
        if(selected.isEmpty())rows.row(MobileWidgets.text(frame.nodes.isEmpty()?"No painted game interface yet. Load the client, then reopen Panels.":"No matching controls in this open interface. The original game remains available using Close."));
        else {
            String family=selected.get(0).family;
            if("confirmation".equals(family))rows.row(MobileWidgets.text("Review the current offer and native confirmation screens. Changes invalidate pending actions."));
            if(!InterfaceRegistry.known(activeGroup))rows.row(MobileWidgets.text("Uncatalogued interface: conservative live-control list, not a verified screen adaptation."));
            if(itemCount>0) {
                List<UiFrameSnapshot.Node> items=new ArrayList<>();for(UiFrameSnapshot.Node n:selected)if(n.role==UiFrameSnapshot.Role.ITEM)items.add(n);
                if(!items.isEmpty()) {
                    itemPage=Math.max(0,Math.min(itemPage,(items.size()-1)/ITEMS_PER_PAGE));int start=itemPage*ITEMS_PER_PAGE,end=Math.min(items.size(),start+ITEMS_PER_PAGE);
                    rows.row(MobileWidgets.text("Items "+(start+1)+"–"+end+" of "+items.size()+". Previous/Next items changes page without renumbering slots."));
                    rows.row(new ItemGrid(items.subList(start,end),frame.moveArmed,preferences));
                }
            }
            Set<String> displayed=new HashSet<>();
            for(UiFrameSnapshot.Node n:selected)if(n.role!=UiFrameSnapshot.Role.ITEM) {
                if(!n.text.trim().isEmpty()&&displayed.add(n.text))rows.row(MobileWidgets.text(n.text));
                boolean nativeOptions=false;
                for(UiFrameSnapshot.Action action:n.actions) {
                    if(action.operation==-1) {
                        JButton activate=MobileWidgets.button((n.text.isEmpty()?n.label:n.text)+" — activate",()->MobileBridge.nodeAction("nodeOp",-1,n.token,n.version));
                        activate.setEnabled(n.enabled);rows.row(activate);
                    } else nativeOptions=true;
                }
                if(nativeOptions) {
                    // Never pick an option automatically. The native action list keeps all current semantics.
                    String label=n.text.isEmpty()?n.label:n.text;
                    JButton button=MobileWidgets.button(label+" — actions",()->MobileBridge.nodeAction("nodeActions",0,n.token,n.version));
                    button.setEnabled(n.enabled);rows.row(button);
                }
                if(n.role==UiFrameSnapshot.Role.MAP)rows.row(mapControls());
                if(n.role==UiFrameSnapshot.Role.SCROLL) {
                    JPanel pages=new JPanel(new GridLayout(1,2,4,4));pages.add(MobileWidgets.button("Original panel up",()->MobileBridge.nodeAction("scrollNode",-1,n.token,n.version)));pages.add(MobileWidgets.button("Original panel down",()->MobileBridge.nodeAction("scrollNode",1,n.token,n.version)));rows.row(pages);
                }
                if(n.role==UiFrameSnapshot.Role.SLIDER) {
                    JPanel adjust=new JPanel(new GridLayout(1,2,4,4));adjust.add(MobileWidgets.button("Decrease "+n.label,()->MobileBridge.nodeAction("adjustNode",-1,n.token,n.version)));adjust.add(MobileWidgets.button("Increase "+n.label,()->MobileBridge.nodeAction("adjustNode",1,n.token,n.version)));rows.row(adjust);
                }
            }
        }
        body.add(rows,BorderLayout.NORTH);body.revalidate();body.repaint();
        final int restore=position;SwingUtilities.invokeLater(()->{if(!closing){int max=Math.max(0,body.getPreferredSize().height-scroll.getViewport().getHeight());scroll.getViewport().setViewPosition(new Point(0,Math.min(restore,max)));}});
    }
    static JPanel mapControls() {
        JPanel panel=new JPanel(new GridLayout(0,2,4,4));
        String[][] directions={{"Map left","mapLeft"},{"Map right","mapRight"},{"Map up","mapUp"},{"Map down","mapDown"},{"Map zoom in","mapZoomIn"},{"Map zoom out","mapZoomOut"}};
        for(String[] d:directions)panel.add(MobileWidgets.button(d[0],()->MobileBridge.action(d[1],0,MobileBridge.revision())));return panel;
    }
    private final class ItemGrid extends JPanel {
        final List<UiFrameSnapshot.Node> items;final int minimum;final String profile;
        ItemGrid(List<UiFrameSnapshot.Node> items,boolean moving,AccessibilityPreferences prefs) {
            this.items=items;minimum=Math.max(prefs.cellWidth,72*prefs.textPercent/100);
            profile=ResponsivePanelLayout.profile(Math.max(1,MobilePanels.this.getWidth()),Math.max(1,MobilePanels.this.getHeight()),prefs.profile);
            setLayout(null);MobileWidgets.theme(this);
            for(UiFrameSnapshot.Node n:items){
                JPanel cell=new JPanel(new BorderLayout(2,2));MobileWidgets.theme(cell);
                String label=n.item<0?"Empty slot "+(n.child+1):n.label+(n.quantity>1?" × "+n.quantity:"");
                JButton choose=MobileWidgets.button(label,()->MobileBridge.nodeAction(moving?"moveDest":"nodeActions",0,n.token,n.version));
                choose.setEnabled(n.enabled&&(moving||n.item>=0||!n.actions.isEmpty()));
                javax.swing.Icon icon=MobileVisuals.icon(n.id,n.child,n.item,n.quantity);if(icon!=null){choose.setIcon(icon);choose.setVerticalTextPosition(SwingConstants.BOTTOM);choose.setHorizontalTextPosition(SwingConstants.CENTER);}
                cell.add(choose,BorderLayout.CENTER);
                if(n.movable&&n.item>=0&&!moving)cell.add(MobileWidgets.button("Move",()->MobileBridge.nodeAction("moveSource",0,n.token,n.version)),BorderLayout.SOUTH);
                add(cell);
            }
        }
        @Override public void doLayout(){int width=Math.max(1,getWidth()),rowHeight=rowHeight();List<Long> ids=new ArrayList<>();for(UiFrameSnapshot.Node n:items)ids.add(n.token);
            List<ResponsivePanelLayout.Cell> layout=ResponsivePanelLayout.grid(ids,width,minimum,rowHeight,profile);
            for(int i=0;i<layout.size();i++){UiFrameSnapshot.Bounds b=layout.get(i).bounds;getComponent(i).setBounds(b.x,b.y,b.width,b.height);}}
        int rowHeight(){
            int available=Math.max(160,scroll.getViewport().getWidth()-12),columns=ResponsivePanelLayout.columns(available,minimum,profile);
            int width=Math.max(40,(available-8-(columns-1)*6)/columns),height=100;
            for(Component child:getComponents()) {
                JPanel cell=(JPanel)child;int required=2;
                for(Component c:cell.getComponents()) if(c instanceof MobileWidgets.WrappingButton)
                    required+=((MobileWidgets.WrappingButton)c).preferredHeightForWidth(width);
                height=Math.max(height,required);
            }
            return Math.max(height,140*AccessibilityPreferences.current().textPercent/100);
        }
        @Override public Dimension getPreferredSize(){int width=Math.max(160,scroll.getViewport().getWidth()-12),cols=ResponsivePanelLayout.columns(width,minimum,profile);return new Dimension(width,8+((items.size()+cols-1)/cols)*(rowHeight()+6));}
    }
    public void close(){closing=true;timer.stop();MobileBridge.setPanelsActive(false);}
}
