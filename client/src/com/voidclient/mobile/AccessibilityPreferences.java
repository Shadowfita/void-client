package com.voidclient.mobile;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit mobile-only preferences. Display units are AWT units, not claimed Android dp. */
public final class AccessibilityPreferences {
    public int textPercent=100, holdMillis=550, slop=10, scrollSpeed=100, cameraSensitivity=100, cellWidth=96;
    public boolean nativeHud=true, nativeGrids=true, highContrast=false, reducedMotion=false, invertCamera=false, leftHanded=false;
    public String profile="auto";
    private static final java.util.concurrent.atomic.AtomicLong revision=new java.util.concurrent.atomic.AtomicLong();
    public static long revision(){return revision.get();}
    private static volatile AccessibilityPreferences current=new AccessibilityPreferences();
    public static AccessibilityPreferences current() { return current.copy(); }
    public AccessibilityPreferences copy() {
        AccessibilityPreferences p=new AccessibilityPreferences(); p.textPercent=textPercent;p.holdMillis=holdMillis;p.slop=slop;
        p.scrollSpeed=scrollSpeed;p.cameraSensitivity=cameraSensitivity;p.cellWidth=cellWidth;p.highContrast=highContrast;
        p.nativeHud=nativeHud;p.nativeGrids=nativeGrids;p.reducedMotion=reducedMotion;p.invertCamera=invertCamera;p.leftHanded=leftHanded;p.profile=profile;return p;
    }
    public void validate() {
        if(textPercent<100||textPercent>200||holdMillis<250||holdMillis>1500||slop<4||slop>40||scrollSpeed<25||scrollSpeed>300
            ||cameraSensitivity<25||cameraSensitivity>250||cellWidth<72||cellWidth>160
            || !Arrays.asList("auto","portrait","landscape","expanded").contains(profile)) throw new IllegalArgumentException("Accessibility setting outside supported range");
    }
    public static Path path() { return Paths.get(System.getProperty("user.home","."),".void-client","mobile-accessibility.properties"); }
    public static void load() {
        try { apply(read(path()),false); } catch(IOException|RuntimeException ex) { apply(new AccessibilityPreferences(),false); }
    }
    public static AccessibilityPreferences read(Path path) throws IOException {
        AccessibilityPreferences a=new AccessibilityPreferences();Properties p=new Properties();
        if(!Files.exists(path)) return a;
        try(InputStream in=Files.newInputStream(path)) { p.load(in); }
        a.textPercent=number(p,"textPercent",100);a.holdMillis=number(p,"holdMillis",550);a.slop=number(p,"slop",10);
        a.scrollSpeed=number(p,"scrollSpeed",100);a.cameraSensitivity=number(p,"cameraSensitivity",100);a.cellWidth=number(p,"cellWidth",96);
        a.highContrast=Boolean.parseBoolean(p.getProperty("highContrast","false"));a.reducedMotion=Boolean.parseBoolean(p.getProperty("reducedMotion","false"));
        a.invertCamera=Boolean.parseBoolean(p.getProperty("invertCamera","false"));a.leftHanded=Boolean.parseBoolean(p.getProperty("leftHanded","false"));
        a.nativeHud=Boolean.parseBoolean(p.getProperty("nativeHud","true"));a.nativeGrids=Boolean.parseBoolean(p.getProperty("nativeGrids","true"));a.profile=p.getProperty("profile","auto");a.validate();return a;
    }
    private static int number(Properties p,String k,int d) { return Integer.parseInt(p.getProperty(k,Integer.toString(d))); }
    public static void apply(AccessibilityPreferences a,boolean persist) {
        a.validate(); current=a.copy();revision.incrementAndGet();
        System.setProperty("void.mobile.nativeHud",Boolean.toString(a.nativeHud));
        System.setProperty("void.mobile.nativeGrids",Boolean.toString(a.nativeGrids));
        System.setProperty("void.mobile.holdMillis",Integer.toString(a.holdMillis));System.setProperty("void.mobile.slop",Integer.toString(a.slop));
        System.setProperty("void.mobile.scrollSpeed",Integer.toString(a.scrollSpeed));System.setProperty("void.mobile.cameraSensitivity",Integer.toString(a.cameraSensitivity));
        System.setProperty("void.mobile.invertCamera",Boolean.toString(a.invertCamera));System.setProperty("void.mobile.reducedMotion",Boolean.toString(a.reducedMotion));
        MobileBridge.cancel();
        if(persist) try { a.save(path()); } catch(IOException ex) { throw new IllegalStateException("Applied for this session, but preferences could not be saved",ex); }
    }
    public void save(Path path) throws IOException {
        validate();Properties p=new Properties();
        p.setProperty("nativeHud",""+nativeHud);p.setProperty("nativeGrids",""+nativeGrids);p.setProperty("textPercent",""+textPercent);p.setProperty("holdMillis",""+holdMillis);p.setProperty("slop",""+slop);
        p.setProperty("scrollSpeed",""+scrollSpeed);p.setProperty("cameraSensitivity",""+cameraSensitivity);p.setProperty("cellWidth",""+cellWidth);
        p.setProperty("highContrast",""+highContrast);p.setProperty("reducedMotion",""+reducedMotion);p.setProperty("invertCamera",""+invertCamera);
        p.setProperty("leftHanded",""+leftHanded);p.setProperty("profile",profile);
        Path absolute=path.toAbsolutePath();Files.createDirectories(absolute.getParent());Path tmp=Files.createTempFile(absolute.getParent(),"access-",".tmp");
        try {
            try(OutputStream out=Files.newOutputStream(tmp)) { p.store(out,"Mobile accessibility preferences; no game data"); }
            try { Files.move(tmp,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException ex) { Files.move(tmp,absolute,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
