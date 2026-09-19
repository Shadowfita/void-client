package com.voidclient.mobile;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Pinned public metadata is a label/layout hint, never a substitute for native action authorization. */
public final class InterfaceRegistry {
    private static final JsonObject GROUPS=load();
    private static JsonObject load() {
        try(InputStream in=InterfaceRegistry.class.getResourceAsStream("/mobile/interface-registry.json")) {
            if(in==null) return new JsonObject();
            try(Reader reader=new InputStreamReader(in,StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("groups");
            }
        } catch(IOException | RuntimeException ex) { return new JsonObject(); }
    }
    public static boolean known(int group) { return GROUPS.has(Integer.toString(group)); }
    private static JsonObject group(int group) { return known(group)?GROUPS.getAsJsonObject(Integer.toString(group)):new JsonObject(); }
    private static String text(JsonObject o,String key,String fallback) { return o.has(key)&&o.get(key).isJsonPrimitive()?o.get(key).getAsString():fallback; }
    public static String name(int id) { return title(text(group(id),"name","Interface "+id)); }
    public static String family(int id) { return text(group(id),"family","generic"); }
    public static String component(int packed) {
        JsonObject g=group(packed>>>16);
        if(!g.has("components")) return "";
        JsonObject children=g.getAsJsonObject("components");
        String key=Integer.toString(packed&65535);
        return children.has(key)?title(text(children.getAsJsonObject(key),"name","")):"";
    }
    public static boolean inventory(int packed) {
        JsonObject g=group(packed>>>16); if(!g.has("components")) return false;
        JsonObject cs=g.getAsJsonObject("components"); String key=Integer.toString(packed&65535);
        return cs.has(key) && !text(cs.getAsJsonObject(key),"inventory","").isEmpty();
    }
    public static boolean textAllowed(int group) { return known(group) && !"sensitive".equals(family(group)); }
    public static boolean consequential(int group) { return "confirmation".equals(family(group)); }
    private static String title(String s) { return s.replace('_',' '); }
    public static int groupCount() { return GROUPS.size(); }
    private InterfaceRegistry() {}
}
