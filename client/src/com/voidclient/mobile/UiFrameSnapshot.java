package com.voidclient.mobile;

import java.util.*;

/** Host-neutral immutable presentation tree. Native widget references never cross the client-thread boundary. */
public final class UiFrameSnapshot {
    public static final class Bounds {
        public final int x,y,width,height;
        public Bounds(int x,int y,int width,int height) { this.x=x; this.y=y; this.width=Math.max(0,width); this.height=Math.max(0,height); }
        public boolean contains(int px,int py) { return px>=x && py>=y && px<(long)x+width && py<(long)y+height; }
    }
    public enum Role { SCENE, MAP, MINIMAP, CONTAINER, SCROLL, SLIDER, ITEM, BUTTON, TEXT, MODEL, IMAGE }
    public static final class Action {
        public final int operation;
        public final String label;
        public Action(int operation,String label) { this.operation=operation; this.label=label; }
    }
    public static final class Node {
        public final long token,parent,version;
        public final int id,child,group,type,contentType,item,quantity,order;
        public final Bounds bounds,clip;
        public final Role role;
        public final String label,groupLabel,family,text;
        public final boolean blocking,visible,movable,enabled;
        public final List<Action> actions;
        public Node(long token,long parent,long version,int id,int child,int type,int contentType,int item,int quantity,
                    int order,Bounds bounds,Bounds clip,Role role,String label,String groupLabel,String family,String text,
                    boolean blocking,boolean movable,boolean enabled,List<Action> actions) {
            this.token=token; this.parent=parent; this.version=version; this.id=id; this.child=child; this.group=id>>>16;
            this.type=type; this.contentType=contentType; this.item=item; this.quantity=quantity; this.order=order;
            this.bounds=bounds; this.clip=clip; this.role=role; this.label=label; this.groupLabel=groupLabel;
            this.family=family; this.text=text; this.blocking=blocking; this.movable=movable;this.enabled=enabled;
            this.visible=clip.width>0 && clip.height>0; this.actions=Collections.unmodifiableList(new ArrayList<>(actions));
        }
    }
    public final long serial,layoutRevision;
    public final ViewportState viewport;
    public final List<Node> nodes;
    public final String status;
    public final boolean targetArmed,moveArmed;
    private final Map<Long,Node> byToken;
    public UiFrameSnapshot(long serial,long layoutRevision,ViewportState viewport,List<Node> nodes,String status,boolean targetArmed,boolean moveArmed) {
        this.serial=serial; this.layoutRevision=layoutRevision; this.viewport=viewport;
        this.nodes=Collections.unmodifiableList(new ArrayList<>(nodes)); this.status=status;
        this.targetArmed=targetArmed; this.moveArmed=moveArmed;
        Map<Long,Node> map=new LinkedHashMap<>(); for(Node n:nodes) map.put(n.token,n);
        byToken=Collections.unmodifiableMap(map);
    }
    public Node node(long token) { return byToken.get(token); }
    public static UiFrameSnapshot empty() { return new UiFrameSnapshot(0,0,null,Collections.emptyList(),"Waiting for a painted interface",false,false); }
    /** Intentional export excludes text, labels, item names, action labels, usernames and drafts. */
    public Map<String,Object> redactedCatalogue() {
        Map<String,Object> result=new LinkedHashMap<>(); result.put("format","void-mobile-interface-catalogue-v1");
        result.put("build","JR2-5 candidate"); result.put("viewport",viewport); result.put("layoutRevision",layoutRevision);
        result.put("cacheFingerprint","See loaded interface group digests; full cache identity not established by geometry");
        List<Map<String,Object>> out=new ArrayList<>();
        for(Node n:nodes) {
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("token",n.token); row.put("parentToken",n.parent); row.put("packedId",n.id); row.put("child",n.child);
            row.put("type",n.type); row.put("contentType",n.contentType); row.put("bounds",n.bounds); row.put("clip",n.clip);
            row.put("order",n.order); row.put("role",n.role); row.put("visible",n.visible); row.put("blocking",n.blocking);
            row.put("actionCount",n.actions.size()); out.add(row);
        }
        result.put("nodes",out); return result;
    }
}
