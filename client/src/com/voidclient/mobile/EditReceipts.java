package com.voidclient.mobile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded, text-free receipts. DELIVERED means consumed by the native keyboard, not field acceptance. */
public final class EditReceipts {
    public enum State { QUEUED, VALIDATED, DISPATCHING, DELIVERED, REJECTED, CANCELLED }
    public static final class Receipt {
        public final int id, delivered;
        public final long session, nextSession;
        public final State state;
        public final String message;
        Receipt(int id, long session, State state, int delivered, long nextSession, String message) {
            this.id=id; this.session=session; this.state=state; this.delivered=delivered;
            this.nextSession=nextSession; this.message=message;
        }
        public boolean terminal() { return state==State.DELIVERED || state==State.REJECTED || state==State.CANCELLED; }
    }
    private static final AtomicInteger ids=new AtomicInteger();
    private static final Map<Integer,Receipt> receipts=new LinkedHashMap<>();
    private EditReceipts() {}
    public static synchronized int create(long session) {
        int id=ids.updateAndGet(n -> n==Integer.MAX_VALUE ? 1 : n+1);
        while(receipts.size()>=64) receipts.remove(receipts.keySet().iterator().next());
        receipts.put(id,new Receipt(id,session,State.QUEUED,0,session,"Queued")); return id;
    }
    public static synchronized Receipt get(int id) { return receipts.get(id); }
    public static synchronized void update(int id,State state,int delivered,long nextSession,String message) {
        Receipt old=receipts.get(id);
        if(old==null || old.terminal()) return;
        receipts.put(id,new Receipt(id,old.session,state,delivered,nextSession,message));
    }
    public static synchronized void cancelPending(String reason) {
        for(Integer id:receipts.keySet().toArray(new Integer[0])) {
            Receipt r=receipts.get(id);
            if(!r.terminal()) update(id,State.CANCELLED,r.delivered,r.session,reason);
        }
    }
}
