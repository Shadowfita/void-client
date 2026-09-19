package com.voidclient.mobile;
import java.util.*;
/** Responsive presentation geometry for immutable native identities, independent of the scene raster. */
public final class ResponsivePanelLayout {
    public static final class Cell {
        public final long token;public final UiFrameSnapshot.Bounds bounds;
        Cell(long token,UiFrameSnapshot.Bounds bounds) { this.token=token;this.bounds=bounds; }
    }
    public static String profile(int width,int height,String override) {
        if(width<1||height<1) throw new IllegalArgumentException("Positive viewport required");
        if(!"auto".equals(override)) return override;
        return Math.min(width,height)>=600?"expanded":width>=height?"landscape":"portrait";
    }
    public static int columns(int width,int minimum,String profile) {
        int max="portrait".equals(profile)?4:"expanded".equals(profile)?10:8;
        return Math.max(1,Math.min(max,(Math.max(1,width)-8)/Math.max(72,minimum)));
    }
    public static List<Cell> grid(List<Long> tokens,int width,int minimum,int rowHeight,String profile) {
        if(width<1||rowHeight<1||minimum<1) throw new IllegalArgumentException("Invalid layout extent");
        int columns=columns(width,minimum,profile),gap=6,inner=Math.max(1,width-8),cell=Math.max(1,(inner-(columns-1)*gap)/columns);
        List<Cell> result=new ArrayList<>();
        for(int i=0;i<tokens.size();i++) result.add(new Cell(tokens.get(i),new UiFrameSnapshot.Bounds(4+(i%columns)*(cell+gap),4+(i/columns)*(rowHeight+gap),cell,rowHeight)));
        return Collections.unmodifiableList(result);
    }
    private ResponsivePanelLayout() {}
}
