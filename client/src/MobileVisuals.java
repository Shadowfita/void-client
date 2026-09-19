import com.voidclient.mobile.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;
/** Copies already-rendered software item sprites only. No GPU readback or rendering on the EDT. */
final class MobileVisuals {
    private static final Map<String,ImageIcon> icons=new LinkedHashMap<String,ImageIcon>(256,0.75f,true){protected boolean removeEldestEntry(Map.Entry<String,ImageIcon> e){return size()>256;}};
    private static volatile long revision;
    static long revision(){return revision;}
    private static String key(int id,int child,int item,int qty){return id+":"+child+":"+item+":"+qty;}
    static void capture(Class46 w,Class105 sprite){
        if(!MobileConfig.enabled()||!MobileBridge.panelsActive()||w.anInt812<0||!(sprite instanceof Class105_Sub3))return;
        String key=key(w.anInt830,w.anInt704,w.anInt812,w.anInt781);
        synchronized(icons){if(icons.containsKey(key))return;}
        Class105_Sub3 software=(Class105_Sub3)sprite;
        int width=software.anInt8471,height=software.anInt8470;
        if(width<1||height<1||width>128||height>128)return;
        int[] pixels;
        // RGB and ARGB subclasses have different transparency rules. Only established formats are exported.
        if(sprite instanceof Class105_Sub3_Sub1){pixels=((Class105_Sub3_Sub1)sprite).anIntArray9933.clone();for(int i=0;i<pixels.length;i++)if(pixels[i]!=0)pixels[i]|=0xff000000;}
        else if(sprite instanceof Class105_Sub3_Sub3)pixels=((Class105_Sub3_Sub3)sprite).anIntArray9936.clone();
        else return;
        if(pixels.length!=width*height)return;
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,width,height,pixels,0,width);
        synchronized(icons){icons.put(key,new ImageIcon(image));revision++;}
    }
    static Icon icon(int id,int child,int item,int qty){synchronized(icons){return icons.get(key(id,child,item,qty));}}
    static void clear(){synchronized(icons){icons.clear();revision++;}}
    private MobileVisuals(){}
}
