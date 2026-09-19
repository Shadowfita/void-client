import com.voidclient.mobile.*;
import java.awt.Rectangle;

/** RuneScape-style native action surface and selection feedback, after interface transforms end. */
final class MobileActionOverlay {
    static void paint(ha renderer,ViewportState v){
        MobileMenuFont.Face face=MobileMenuFont.get(renderer,v);
        int[] clip=new int[4];renderer.K(clip);
        try {
            renderer.KA(0,0,v.nativeWidth,v.nativeHeight);
            if(face==null){
                CanvasActionMenu.Frame recovery=CanvasActionMenu.fallback(v);
                if(recovery!=null)MobileCanvasMenu.draw(renderer,recovery.cancel,true);
                return;
            }
            if(CanvasActionMenu.active()){
                int[] anchor=MobileRuntime.menuAnchor();
                CanvasActionMenu.Frame f=CanvasActionMenu.layout(v,face::width,face.height()+2,face.key,anchor[0],anchor[1]);
                if(f==null)return;
                boolean contrast=AccessibilityPreferences.current().highContrast;
                int panel=contrast?0xff141414:0xff5d5447,heading=contrast?0xff000000:0xff302b23;
                int text=contrast?0xffffffff:0xfffff2d4,divider=contrast?0xffffffff:0xff40372b;
                fill(renderer,f.bounds,panel);fill(renderer,f.header,heading);
                face.draw("Choose Option",f.header.x+f.padding,f.header.y+(f.header.height-face.height())/2+face.metrics.anInt1988,0xffffc75b);
                renderer.KA(f.body.x,f.body.y,f.body.x+f.body.width,f.body.y+f.body.height);
                for(CanvasActionMenu.Row row:f.rows){
                    Rectangle r=f.rowBounds(row);if(!r.intersects(f.body))continue;
                    fill(renderer,r,row.index==f.focused?(contrast?0xff505050:0xff79694e):panel);
                    int y=r.y+(r.height-row.lines.size()*f.lineHeight)/2+face.metrics.anInt1988;
                    for(String line:row.lines){face.draw(line,r.x+f.padding,y,text);y+=f.lineHeight;}
                    renderer.aa(r.x,r.y+r.height-1,r.width,1,divider,0);
                }
                if(f.maxScroll()>0){
                    int width=Math.max(3,f.padding/2),height=Math.max(f.padding,f.body.height*f.body.height/f.contentHeight);
                    int y=f.body.y+(f.body.height-height)*f.scroll/f.maxScroll();
                    renderer.aa(f.body.x+f.body.width-width,f.body.y,width,f.body.height,0xff302b23,0);
                    renderer.aa(f.body.x+f.body.width-width,y,width,height,0xffb7a075,0);
                }
                renderer.KA(0,0,v.nativeWidth,v.nativeHeight);fill(renderer,f.cancel,contrast?0xff000000:0xff40372b);
                face.draw("Cancel",f.cancel.x+(f.cancel.width-face.width("Cancel"))/2,f.cancel.y+(f.cancel.height-face.height())/2+face.metrics.anInt1988,0xffffc75b);
                renderer.method3628(f.bounds.x,f.bounds.y,f.bounds.width,f.bounds.height,0xffb7a075,0);
            }else{
                Rectangle selected=MobileRuntime.moveHighlight();
                if(selected!=null)renderer.method3628(selected.x,selected.y,selected.width,selected.height,0xffffc75b,0);
                String hint=MobileRuntime.nativeHint();
                if(!hint.isEmpty()){
                    // Contextual, time-limited feedback, not a permanent footer/control dock.
                    int max=Math.max(1,v.nativeWidth-16),pad=6;
                    java.util.List<String> lines=CanvasActionMenu.wrap(hint,max-2*pad,face::width);
                    int count=Math.min(3,lines.size()),height=count*(face.height()+2)+2*pad;
                    int y=Math.max(0,v.nativeHeight-height-8);
                    renderer.aa(8,y,max,height,0xe8302b23,1);
                    for(int i=0;i<count;i++)face.draw(lines.get(i),8+pad,y+pad+face.metrics.anInt1988+i*(face.height()+2),0xffffe5a8);
                }
            }
        }finally{renderer.KA(clip[0],clip[1],clip[2],clip[3]);}
    }
    private static void fill(ha renderer,Rectangle r,int colour){renderer.aa(r.x,r.y,r.width,r.height,colour,0);}
    private MobileActionOverlay(){}
}
