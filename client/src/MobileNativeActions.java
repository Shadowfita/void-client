import java.util.*;

/** Collects the native builder's candidates without modifying its live menu. Client thread only. */
final class MobileNativeActions {
    private static List<Class348_Sub42_Sub12> capture;
    static boolean capturing() { return capture!=null; }
    static void collect(Class348_Sub42_Sub12 entry) { if(capture!=null && capture.size()<32) capture.add(entry); }
    static List<Class348_Sub42_Sub12> forWidget(Class46 widget) {
        if(widget==null || widget.aBoolean813 || capture!=null) return Collections.emptyList();
        List<Class348_Sub42_Sub12> result=new ArrayList<>(); capture=result;
        try { Class239_Sub17.method1797(0,0,widget,(byte)-95); }
        finally { capture=null; }
        Collections.reverse(result); return result;
    }
    static List<Object> signature(Class348_Sub42_Sub12 e) {
        return Arrays.asList(e.anInt9608,e.anInt9602,e.anInt9607,e.aLong9605,e.aLong9600,e.anInt9599,e.anInt9609,
            e.aString9595,e.aString9593,e.aString9601,e.aBoolean9597,e.aBoolean9610,e.aBoolean9611);
    }
    private MobileNativeActions() {}
}
