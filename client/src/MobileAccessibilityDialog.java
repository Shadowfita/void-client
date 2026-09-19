import com.voidclient.mobile.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

final class MobileAccessibilityDialog {
    static void open(JFrame owner){
        AccessibilityPreferences original=AccessibilityPreferences.current();MobileWidgets.Vertical form=new MobileWidgets.Vertical();
        Map<String,JSpinner> values=new LinkedHashMap<>();
        spin(form,values,"Mobile action text (%)",original.textPercent,100,200,25);
        spin(form,values,"Hold delay (milliseconds)",original.holdMillis,250,1500,50);
        spin(form,values,"Movement threshold (AWT units)",original.slop,4,40,1);
        spin(form,values,"Scroll speed (%)",original.scrollSpeed,25,300,25);
        spin(form,values,"Camera sensitivity (%)",original.cameraSensitivity,25,250,25);
        JCheckBox contrast=new JCheckBox("High contrast mobile actions",original.highContrast),motion=new JCheckBox("Reduce camera movement",original.reducedMotion),invert=new JCheckBox("Invert vertical camera",original.invertCamera),left=new JCheckBox("Menu on right edge",original.leftHanded);
        JCheckBox hud=new JCheckBox("Adapt supported native HUD attachments and dialogue",original.nativeHud);form.row(hud);
        JCheckBox grids=new JCheckBox("Reflow structurally supported native item grids",original.nativeGrids);form.row(grids);
        form.row(contrast);form.row(motion);form.row(invert);form.row(left);
        JComboBox<String> profile=new JComboBox<>(new String[]{"auto","portrait","landscape","expanded"});profile.setSelectedItem(original.profile);profile.getAccessibleContext().setAccessibleName("Native interface layout profile");form.row(new JLabel("Native interface layout profile"));form.row(profile);
        form.row(MobileWidgets.text("Mobile action text size is independent of game-scene scaling. These are AWT units, not Android dp. Other original cache-rendered text is unchanged. New settings cancel active gestures."));
        JLabel status=new JLabel("Settings apply to native action menus. Existing dialogs may need reopening.");
        JDialog dialog=new JDialog(owner,"Accessibility and gestures",false);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);TouchScrollPane scroll=new TouchScrollPane(form);dialog.add(scroll,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(4,4));JPanel buttons=new JPanel(new GridLayout(0,3,4,4));
        buttons.add(StandaloneMobileHost.makeButton("Apply",()->{try{
            AccessibilityPreferences p=new AccessibilityPreferences();Iterator<JSpinner> i=values.values().iterator();p.textPercent=(Integer)i.next().getValue();p.cellWidth=original.cellWidth;p.holdMillis=(Integer)i.next().getValue();p.slop=(Integer)i.next().getValue();p.scrollSpeed=(Integer)i.next().getValue();p.cameraSensitivity=(Integer)i.next().getValue();
            p.nativeHud=hud.isSelected();p.nativeGrids=grids.isSelected();p.highContrast=contrast.isSelected();p.reducedMotion=motion.isSelected();p.invertCamera=invert.isSelected();p.leftHanded=left.isSelected();p.profile=(String)profile.getSelectedItem();AccessibilityPreferences.apply(p,true);status.setText("Applied and saved. Existing drafts are retained; gestures cancelled.");
        }catch(RuntimeException e){status.setText(e.getMessage());}}));
        buttons.add(StandaloneMobileHost.makeButton("Page down",()->scroll.page(1)));buttons.add(StandaloneMobileHost.makeButton("Close",dialog::dispose));
        footer.add(buttons,BorderLayout.CENTER);footer.add(status,BorderLayout.SOUTH);dialog.add(footer,BorderLayout.SOUTH);
        dialog.setSize(Math.min(600,owner.getWidth()),Math.min(700,owner.getHeight()));StandaloneMobileHost.track(dialog);dialog.setVisible(true);
    }
    private static void spin(MobileWidgets.Vertical form,Map<String,JSpinner> map,String label,int value,int min,int max,int step){JSpinner s=new JSpinner(new SpinnerNumberModel(value,min,max,step));s.getAccessibleContext().setAccessibleName(label);map.put(label,s);form.row(new JLabel(label));form.row(s);}
    private MobileAccessibilityDialog(){}
}
