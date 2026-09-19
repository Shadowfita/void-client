package com.voidclient.mobile;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.*;

/** Touch/keyboard controller for the in-game action menu. It never executes game operations.
 * Mutable state belongs to the client thread; the EDT sees only the published paint snapshot. */
public final class CanvasActionMenu {
    public interface Measure { int width(String text); }
    public static final int NONE=-2, DISMISS=-1;
    public static final long TOKEN_BASE=Long.MIN_VALUE+4096;
    public static final class Row {
        public final int index, y, height;
        public final List<String> lines;
        public final String label;
        Row(int index,int y,int height,List<String> lines,String label){this.label=label;this.index=index;this.y=y;this.height=height;this.lines=Collections.unmodifiableList(lines);}
    }
    public static final class Frame {
        public final long menuId, generation;
        public final ViewportState viewport;
        public final Rectangle bounds, header, body, cancel;
        public final List<Row> rows;
        public final int scroll, contentHeight, lineHeight, padding, focused;
        Frame(long id,long generation,ViewportState v,Rectangle bounds,Rectangle header,Rectangle body,Rectangle cancel,
              List<Row> rows,int scroll,int contentHeight,int lineHeight,int padding,int focused){
            this.menuId=id;this.generation=generation;this.viewport=v;this.bounds=bounds;this.header=header;this.body=body;this.cancel=cancel;
            this.rows=rows;this.scroll=scroll;this.contentHeight=contentHeight;this.lineHeight=lineHeight;this.padding=padding;this.focused=focused;
        }
        public Rectangle rowBounds(Row row){return new Rectangle(body.x,body.y+row.y-scroll,body.width,row.height);}
        public int at(double x,double y){
            if(cancel.contains(x,y)||!bounds.contains(x,y))return DISMISS;
            if(!body.contains(x,y))return NONE;
            int content=(int)Math.floor(y-body.y)+scroll;
            for(Row row:rows)if(content>=row.y&&content<row.y+row.height)return row.index;
            return NONE;
        }
        public int maxScroll(){return Math.max(0,contentHeight-body.height);}
    }
    private static volatile boolean opened;
    private static volatile Frame painted;
    private static long menuId,generation;
    private static List<String> labels=Collections.emptyList();
    private static List<Row> rows=Collections.emptyList();
    private static ViewportState geometry;
    private static int fontKey,contentHeight,focused=-1,pressed=NONE,primary=-1;
    private static double scroll,downX,downY,lastY;
    private static boolean dragged,multi;
    private static Frame downFrame;
    private static final Set<Integer> pointers=new HashSet<>();
    private static Rectangle bounds,header,body,cancel;

    public static boolean active(){return opened&&MobileConfig.enabled()&&!MobileConfig.browser();}
    public static Frame frame(){return active()?painted:null;}
    public static void open(long id,List<String> choices){
        menuId=id;List<String> safe=new ArrayList<>();
        if(choices.size()>512)throw new IllegalArgumentException("Too many menu entries");
        for(String label:choices){String value=label==null?"":label;safe.add(value.length()>2048?value.substring(0,2047)+"…":value);}
        labels=Collections.unmodifiableList(safe);opened=!choices.isEmpty();
        painted=null;geometry=null;rows=Collections.emptyList();scroll=0;focused=-1;resetPointer();generation++;
        MobileAccessibleCanvas.changed();
    }
    public static void close(){opened=false;painted=null;geometry=null;labels=Collections.emptyList();rows=Collections.emptyList();resetPointer();generation++;MobileAccessibleCanvas.changed();}
    private static void resetPointer(){pointers.clear();primary=-1;pressed=NONE;dragged=multi=false;downFrame=null;}
    /** Called from the actual native overlay paint, never from a host UI timer. */
    public static Frame layout(ViewportState v,Measure measure,int lineHeight,int key,int anchorX,int anchorY){
        if(!active()||v==null||MobileBridge.suspended()||MobileBridge.hostOverlayActive())return null;
        if(geometry==null||!geometry.sameGeometry(v)||geometry.revision!=v.revision||fontKey!=key){
            geometry=v;fontKey=key;resetPointer();generation++;
            int gap=Math.min(Math.min(v.nativeWidth/8,v.nativeHeight/8),Math.max(2,(int)Math.round(8.0*v.nativeWidth/v.width)));
            int minRow=Math.max(1,(int)Math.ceil(48.0*v.nativeHeight/v.height));
            int padding=Math.max(3,(int)Math.round(9.0*v.nativeHeight/v.height));
            int width=Math.min(v.nativeWidth-2*gap,(int)Math.round(390.0*v.nativeWidth/v.width));
            width=Math.max(1,width);
            int cancelHeight=Math.max(minRow,lineHeight+2*padding);
            int headHeight=Math.max(minRow,lineHeight+2*padding);
            int usable=Math.max(3,v.nativeHeight-2*gap);
            headHeight=Math.min(headHeight,usable/3);cancelHeight=Math.min(cancelHeight,usable/3);
            int x=Math.max(gap,Math.min(v.nativeWidth-gap-width,anchorX-width/2));
            int textWidth=Math.max(1,width-3*padding);
            List<Row> laid=new ArrayList<>();int y=0;
            for(int i=0;i<labels.size()&&i<512;i++){
                List<String> lines=wrap(labels.get(i),textWidth,measure);
                int h=Math.max(minRow,lines.size()*lineHeight+2*padding);
                laid.add(new Row(i,y,h,lines,labels.get(i)));y+=h;
            }
            rows=Collections.unmodifiableList(laid);contentHeight=y;
            int available=Math.max(1,v.nativeHeight-2*gap-headHeight-cancelHeight);
            int bodyHeight=Math.min(available,Math.max(minRow,contentHeight));
            int height=headHeight+bodyHeight+cancelHeight;
            int top=Math.max(gap,Math.min(v.nativeHeight-gap-height,anchorY));
            bounds=new Rectangle(x,top,width,height);header=new Rectangle(x,top,width,headHeight);
            body=new Rectangle(x,top+headHeight,width,bodyHeight);cancel=new Rectangle(x,body.y+bodyHeight,width,cancelHeight);
            scroll=Math.max(0,Math.min(scroll,Math.max(0,contentHeight-body.height)));
        }
        int padding=Math.max(3,(int)Math.round(9.0*v.nativeHeight/v.height));
        painted=new Frame(menuId,generation,v,new Rectangle(bounds),new Rectangle(header),new Rectangle(body),new Rectangle(cancel),rows,(int)scroll,contentHeight,lineHeight,padding,focused);
        return painted;
    }
    /** A visible native X remains available while game fonts are missing; no invisible actions. */
    public static Frame fallback(ViewportState v){
        if(!active()||v==null||MobileBridge.suspended()||MobileBridge.hostOverlayActive())return null;
        if(geometry==null||!geometry.sameGeometry(v)||geometry.revision!=v.revision||fontKey!=Integer.MIN_VALUE){
            geometry=v;fontKey=Integer.MIN_VALUE;generation++;resetPointer();rows=Collections.emptyList();focused=-1;
        }
        int w=Math.min(v.nativeWidth,Math.max(1,(int)Math.ceil(48.0*v.nativeWidth/v.width)));
        int h=Math.min(v.nativeHeight,Math.max(1,(int)Math.ceil(48.0*v.nativeHeight/v.height)));
        Rectangle x=new Rectangle(Math.max(0,v.nativeWidth-w-4),Math.min(4,Math.max(0,v.nativeHeight-h)),w,h);
        painted=new Frame(menuId,generation,v,x,new Rectangle(),new Rectangle(),x,Collections.emptyList(),0,0,1,1,-1);
        return painted;
    }
    /** Wraps literal labels, including long unbroken names, without changing their action identity. */
    public static List<String> wrap(String label,int width,Measure measure){
        String text=label==null?"":label;List<String> lines=new ArrayList<>();
        int start=0;
        while(start<text.length()){
            int end=start,lastSpace=-1;
            while(end<text.length()&&measure.width(text.substring(start,end+1))<=width){if(Character.isWhitespace(text.charAt(end)))lastSpace=end;end++;}
            if(end==start)end++; // At least one glyph even in an unusually narrow view; the clip still applies.
            else if(end<text.length()&&lastSpace>start)end=lastSpace;
            lines.add(text.substring(start,end));start=end;
            while(start<text.length()&&Character.isWhitespace(text.charAt(start)))start++;
        }
        if(lines.isEmpty())lines.add("");return lines;
    }
    /** Result is an entry index or DISMISS; the native runtime performs final state validation. */
    public static int handle(MobileBridge.Command c){
        Frame f=frame();if(!active()||c.revision!=menuId)return NONE;
        if("menuKey".equals(c.type)&&c.id==KeyEvent.VK_ESCAPE)return DISMISS;
        if(f==null)return NONE;
        if("menuKey".equals(c.type)){resetPointer();return key(c.id,f);}
        if("menuWheel".equals(c.type)){resetPointer();scroll=Math.max(0,Math.min(f.maxScroll(),scroll+c.id*Math.max(1,f.lineHeight*3)));return NONE;}
        if(!Double.isFinite(c.x)||!Double.isFinite(c.y)){resetPointer();return NONE;}
        if("menuDown".equals(c.type)){
            if(!pointers.add(c.id)){resetPointer();return NONE;}
            if(pointers.size()>1){multi=true;return NONE;}
            if(multi)return NONE;
            primary=c.id;downFrame=f;pressed=f.at(c.x,c.y);downX=c.x;downY=lastY=c.y;dragged=false;focused=pressed>=0?pressed:-1;
        }else if("menuMove".equals(c.type)||"menuUp".equals(c.type)){
            if(!pointers.contains(c.id))return NONE; // The release which opened the menu is never a selection.
            if(c.id==primary&&downFrame!=null&&!multi){
                double sx=(c.x-downX)*f.viewport.width/f.viewport.nativeWidth;
                double sy=(c.y-downY)*f.viewport.height/f.viewport.nativeHeight;
                if(Math.hypot(sx,sy)>MobileConfig.slopPixels())dragged=true;
                if(dragged&&downFrame.body.contains(downX,downY))scroll=Math.max(0,Math.min(f.maxScroll(),scroll-(c.y-lastY)));
                lastY=c.y;
            }
            if("menuUp".equals(c.type)){
                int result=NONE;
                if(!multi&&!dragged&&c.id==primary&&downFrame!=null&&downFrame.generation==f.generation&&pressed==f.at(c.x,c.y))result=pressed;
                pointers.remove(c.id);if(pointers.isEmpty())resetPointer();
                return result;
            }
        }
        return NONE;
    }
    private static int key(int code,Frame f){
        if(code==KeyEvent.VK_ESCAPE)return DISMISS;
        if(code==KeyEvent.VK_ENTER||code==KeyEvent.VK_SPACE)return focused>=0?focused:NONE;
        if(code==KeyEvent.VK_UP)focused=focused<0?rows.size()-1:Math.max(0,focused-1);
        else if(code==KeyEvent.VK_DOWN)focused=Math.min(rows.size()-1,focused+1);
        else if(code==KeyEvent.VK_HOME)focused=0;
        else if(code==KeyEvent.VK_END)focused=rows.size()-1;
        else if(code==KeyEvent.VK_PAGE_DOWN){scroll=Math.min(f.maxScroll(),scroll+f.body.height);return NONE;}
        else if(code==KeyEvent.VK_PAGE_UP){scroll=Math.max(0,scroll-f.body.height);return NONE;}
        if(focused>=0&&focused<rows.size()){
            Row row=rows.get(focused);if(row.y<scroll)scroll=row.y;
            else if(row.y+row.height>scroll+f.body.height)scroll=Math.min(f.maxScroll(),row.y+row.height-f.body.height);
        }
        return NONE;
    }
    /** EDT producer: every pointer is intercepted while open, including outside Cancel taps. */
    public static boolean pointer(String phase,int id,int x,int y,int canvasWidth,int canvasHeight){
        if(!active())return false;Frame f=frame();
        if(f!=null&&canvasWidth==f.viewport.width&&canvasHeight==f.viewport.height&&!MobileBridge.hostOverlayActive())
            MobileBridge.menuInput(phase,id,(double)x*f.viewport.nativeWidth/canvasWidth,(double)y*f.viewport.nativeHeight/canvasHeight,f.menuId);
        return true;
    }
    public static boolean keyboard(int key){Frame f=frame();if(!active())return false;MobileBridge.menuInput("menuKey",key,0,0,f==null?menuId:f.menuId);return true;}
    public static List<UiFrameSnapshot.Node> nodes(){
        Frame f=frame();if(f==null)return Collections.emptyList();List<UiFrameSnapshot.Node> out=new ArrayList<>();
        for(Row row:f.rows){Rectangle r=f.rowBounds(row).intersection(f.body);if(r.width>0&&r.height>0)addNode(out,f,row.index,r,row.label);}
        addNode(out,f,DISMISS,f.cancel,"Cancel");return out;
    }
    private static void addNode(List<UiFrameSnapshot.Node> out,Frame f,int index,Rectangle r,String label){
        ViewportState v=f.viewport;UiFrameSnapshot.Bounds b=new UiFrameSnapshot.Bounds((int)Math.floor((double)r.x*v.logicalWidth/v.nativeWidth),(int)Math.floor((double)r.y*v.logicalHeight/v.nativeHeight),(int)Math.ceil((double)r.width*v.logicalWidth/v.nativeWidth),(int)Math.ceil((double)r.height*v.logicalHeight/v.nativeHeight));
        out.add(new UiFrameSnapshot.Node(TOKEN_BASE+index+1,0,f.generation,-1,index,0,0,-1,0,Integer.MAX_VALUE,b,b,UiFrameSnapshot.Role.BUTTON,label,"Choose option","native-menu","",true,false,true,Collections.singletonList(new UiFrameSnapshot.Action(index,label))));
    }
    public static boolean isNode(long token){return token>=TOKEN_BASE&&token<TOKEN_BASE+514;}
    public static boolean current(UiFrameSnapshot.Node n){Frame f=frame();if(f==null||!isNode(n.token)||n.version!=f.generation)return false;
        for(UiFrameSnapshot.Node visible:nodes())if(visible.token==n.token&&visible.clip.x==n.clip.x&&visible.clip.y==n.clip.y&&visible.clip.width==n.clip.width&&visible.clip.height==n.clip.height)return true;
        return false;}
    public static boolean activate(UiFrameSnapshot.Node n){Frame f=frame();if(f==null||!current(n))return false;MobileBridge.action(n.child<0?"dismiss":"select",n.child,f.menuId);return true;}
    private CanvasActionMenu(){}
}
