/** The final dispatch seam. Production always uses the original native script/packet paths. */
final class MobileNativeOperations {
    interface Dispatcher {
        void action(Class348_Sub42_Sub12 entry,int x,int y);
        void move(Class46 source,Class46 destination,int x,int y);
        void adjust(Class46 source,int x,int y);
        void activate(Class46 source,int x,int y);
    }
    private static Dispatcher dispatcher=new Dispatcher() {
        public void action(Class348_Sub42_Sub12 entry,int x,int y){Class325.method2599((byte)109,entry,y,x);}
        public void move(Class46 source,Class46 destination,int x,int y){Class347.completeDrag(source,destination,x,y);}
        public void adjust(Class46 source,int x,int y){Class347.updateDrag(source,x,y);}
        public void activate(Class46 source,int x,int y){
            // The original press listener, with its native widget and relative coordinates.
            Class348_Sub36 event=new Class348_Sub36();event.aBoolean6993=true;event.aClass46_6989=source;
            event.anInt6984=x;event.anInt6995=y;event.anObjectArray6987=source.anObjectArray763;Class66.method705(event);
        }
    };
    static void action(Class348_Sub42_Sub12 entry,int x,int y){dispatcher.action(entry,x,y);}
    static void move(Class46 source,Class46 destination,int x,int y){dispatcher.move(source,destination,x,y);}
    static void adjust(Class46 source,int x,int y){dispatcher.adjust(source,x,y);}
    static void activate(Class46 source,int x,int y){dispatcher.activate(source,x,y);}
    private MobileNativeOperations(){}
}
