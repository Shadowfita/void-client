import com.google.gson.*;
import com.voidclient.mobile.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class MobileDiagnostics {
    static Map<String,Object> report(){
        Map<String,Object> r=new LinkedHashMap<>();r.put("build","JR2-5 candidate");
        r.put("javaVersion",System.getProperty("java.version","unknown"));r.put("javaVendor",System.getProperty("java.vendor","unknown"));
        r.put("displayUnits","Java/AWT units; Android dp and physical density not established");
        r.put("input",Boolean.getBoolean("void.mobile.emulateTouch")?"Single virtual-mouse pointer adapted to touch":"Ordinary mouse");
        r.put("rawAndroidMultitouch","Not exposed by this JVM integration");r.put("androidImeInsets","Not exposed; text entry uses the host runtime");
        r.put("javaAccessibility","Virtual Java AccessibleContext nodes implemented; TalkBack/Switch Access export must be verified on the host");
        r.put("metadataGroups",InterfaceRegistry.groupCount());r.put("timings",MobileMetrics.snapshot());r.put("cache",InterfaceCatalogue.snapshot());
        r.put("interface",MobileBridge.ui().redactedCatalogue());return r;
    }
    static void export(JFrame owner){
        UiFrameSnapshot snapshot=MobileBridge.ui();
        if(snapshot.nodes.isEmpty()){JOptionPane.showMessageDialog(owner,"Open Panels after a game interface is painted, then export again.");return;}
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Save redacted interface catalogue (no screenshots or text)");chooser.setSelectedFile(new File("void-mobile-catalogue.json"));
        if(chooser.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION)return;
        Path path=chooser.getSelectedFile().toPath();
        if(Files.exists(path)&&JOptionPane.showConfirmDialog(owner,"Replace this catalogue file?","Confirm replacement",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{String json=new GsonBuilder().setPrettyPrinting().create().toJson(report());Files.write(path,json.getBytes(StandardCharsets.UTF_8));JOptionPane.showMessageDialog(owner,"Saved redacted catalogue. No screenshot, game text, credentials or item contents were exported.");}
        catch(IOException ex){JOptionPane.showMessageDialog(owner,"Could not save catalogue: "+ex.getClass().getSimpleName());}
    }
    static void show(JFrame owner){
        JDialog d=new JDialog(owner,"Runtime capabilities and local metrics",false);
        JTextArea text=new JTextArea();text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);
        Runnable update=()->text.setText(StandaloneMobileHost.diagnostics()+"\n\n"+
            "Java accessibility nodes: implemented. Android TalkBack/Switch Access: not verified.\n"+
            "Raw multitouch and Android keyboard insets: not exposed by this host integration.\n"+
            "Loaded public metadata is not proof of matching cache layout.\n\n"+new GsonBuilder().setPrettyPrinting().create().toJson(MobileMetrics.snapshot()));
        update.run();d.add(new TouchScrollPane(text),BorderLayout.CENTER);
        d.add(StandaloneMobileHost.makeButton("Close",d::dispose),BorderLayout.SOUTH);d.setSize(Math.min(640,owner.getWidth()),Math.min(600,owner.getHeight()));d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        StandaloneMobileHost.track(d);javax.swing.Timer timer=new javax.swing.Timer(1000,e->update.run());timer.start();d.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){timer.stop();}});d.setVisible(true);
    }
    private MobileDiagnostics(){}
}
