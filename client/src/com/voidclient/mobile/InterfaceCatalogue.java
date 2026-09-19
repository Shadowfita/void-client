package com.voidclient.mobile;
import java.security.*;
import java.util.*;
/** Only SHA-256 digests of raw interface definitions. Never stores cache bytes or private fields. */
public final class InterfaceCatalogue {
    private static final Map<Integer,String> files=new TreeMap<>();
    public static synchronized void record(int group,int file,byte[] data) {
        if(!MobileConfig.enabled()||data==null||group<0||group>65535||file<0||file>65535||files.size()>=32768)return;
        try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] hash=md.digest(data);StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format("%02x",b&255));files.put((group<<16)|file,out.toString());}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    public static synchronized Map<String,Object> snapshot() {
        Map<String,Object> result=new LinkedHashMap<>();result.put("algorithm","sha256-per-loaded-interface-file-v1");
        result.put("coverage","Loaded raw interface definitions only, not the full cache or scripts");result.put("digests",new TreeMap<>(files));return result;
    }
    private InterfaceCatalogue(){}
}
