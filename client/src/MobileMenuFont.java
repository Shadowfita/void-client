import com.voidclient.mobile.*;

/** Enlarged copies of the game's font glyphs, created by the active native renderer.
 * No system font, Swing text rendering, scene scaling, or GPU readback is involved. */
final class MobileMenuFont {
    static final class Face {
        final Class324 font;final Class143 metrics;final int key;
        Face(Class324 font,Class143 metrics,int key){this.font=font;this.metrics=metrics;this.key=key;}
        int height(){return Math.max(1,Math.max(metrics.anInt1992,metrics.anInt1988+metrics.anInt1993));}
        int width(String text){return metrics.method1183(true,literal(text));}
        void draw(String text,int x,int baseline,int colour){font.method2576(literal(text),colour,baseline,x,0xff000000,-125);}
    }
    private static ha owner;
    private static Face cached;
    private static int requested,id=-1;
    private static long retry;
    static String literal(String text){
        if(text==null)return "";
        StringBuilder out=new StringBuilder(text.length());
        for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='<')out.append("<lt>");else if(c=='>')out.append("<gt>");else out.append(c);}
        return out.toString();
    }
    static Face get(ha renderer,ViewportState v){
        if(renderer==null||v==null)return null;
        int percent=Math.max(50,Math.min(400,(int)Math.round(150.0*v.nativeHeight/v.height*AccessibilityPreferences.current().textPercent/100)));
        if(renderer==owner&&requested==percent&&id==Class291.anInt3736&&cached!=null)return cached;
        if(renderer!=owner||requested!=percent||id!=Class291.anInt3736){owner=renderer;requested=percent;id=Class291.anInt3736;cached=null;retry=0;}
        long now=System.nanoTime();
        if(now>=retry&&Class39.aClass45_518!=null&&s.aClass45_4585!=null&&id>=0){
            retry=now+500_000_000L;
            Class207[] glyphs=Class207.method1523(Class39.aClass45_518,id);
            Class143 metrics=Class239_Sub10.method1766((byte)-39,id,s.aClass45_4585);
            if(metrics!=null&&safeGlyphSet(glyphs,percent)){
                Class207[] scaled=new Class207[256];for(int i=0;i<scaled.length;i++)scaled[i]=scale(glyphs[i],percent);
                metrics=metrics.scaledForMobile(percent);
                cached=new Face(renderer.method3686(metrics,scaled,true),metrics,percent);return cached;
            }
        }
        return Class262.aClass324_3326!=null&&Class369.aClass143_4962!=null
            ?new Face(Class262.aClass324_3326,Class369.aClass143_4962,100):null;
    }
    /** Prevent a malformed/custom cache font from allocating an unbounded expanded atlas. */
    static boolean safeGlyphSet(Class207[] glyphs,int percent){
        if(glyphs==null||glyphs.length!=256||percent<50||percent>400)return false;
        long pixels=0;
        for(Class207 glyph:glyphs){
            if(glyph==null||glyph.anInt2702<0||glyph.anInt2696<0||glyph.anInt2702>256||glyph.anInt2696>256
                ||glyph.aByteArray2699==null||glyph.aByteArray2699.length!=glyph.anInt2702*glyph.anInt2696
                ||glyph.anIntArray2697==null||glyph.anIntArray2697.length>256
                ||(glyph.aByteArray2695!=null&&glyph.aByteArray2695.length!=glyph.aByteArray2699.length))return false;
            pixels+=(long)scaled(glyph.anInt2702,percent)*scaled(glyph.anInt2696,percent);
            if(pixels>4_194_304L)return false; // At most 4 MiB each for indexed pixels and optional alpha.
        }
        return true;
    }
    static Class207 scale(Class207 src,int percent){
        if(src==null||percent<50||percent>400)throw new IllegalArgumentException("Invalid glyph or scale");
        Class207 dst=new Class207();
        dst.anInt2702=scaled(src.anInt2702,percent);dst.anInt2696=scaled(src.anInt2696,percent);
        dst.anInt2703=scaled(src.anInt2703,percent);dst.anInt2700=scaled(src.anInt2700,percent);
        dst.anInt2698=scaled(src.anInt2698,percent);dst.anInt2701=scaled(src.anInt2701,percent);
        if(src.anInt2702<0||src.anInt2696<0||src.anInt2702>256||src.anInt2696>256||src.aByteArray2699==null||src.aByteArray2699.length!=src.anInt2702*src.anInt2696)
            throw new IllegalArgumentException("Invalid font glyph dimensions");
        dst.aByteArray2699=new byte[dst.anInt2702*dst.anInt2696];
        if(src.aByteArray2695!=null){if(src.aByteArray2695.length!=src.aByteArray2699.length)throw new IllegalArgumentException("Invalid glyph alpha");dst.aByteArray2695=new byte[dst.aByteArray2699.length];}
        dst.anIntArray2697=src.anIntArray2697==null?null:src.anIntArray2697.clone();
        for(int y=0;y<dst.anInt2696;y++)for(int x=0;x<dst.anInt2702;x++){
            int source=(y*src.anInt2696/dst.anInt2696)*src.anInt2702+x*src.anInt2702/dst.anInt2702,dest=y*dst.anInt2702+x;
            dst.aByteArray2699[dest]=src.aByteArray2699[source];if(dst.aByteArray2695!=null)dst.aByteArray2695[dest]=src.aByteArray2695[source];
        }
        return dst;
    }
    private static int scaled(int n,int p){return Math.max(0,Math.round(n*p/100f));}
    private MobileMenuFont(){}
}
