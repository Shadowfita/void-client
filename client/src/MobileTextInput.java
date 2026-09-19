import com.voidclient.mobile.*;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;

/** Ordered, client-thread editing. No game text or secrets are included in receipts. */
final class MobileTextInput {
    private static final class Edit {
        final int id,key; final long session; final String text;
        int dispatched, consumed; boolean keySeen;
        Edit(int id,int key,long session,String text) { this.id=id; this.key=key; this.session=session; this.text=text; }
    }
    private final ArrayDeque<Edit> edits=new ArrayDeque<>();
    private Edit active;
    boolean accept(MobileBridge.Command c,long session,boolean eligible) {
        int id="key".equals(c.type)?EditReceipts.create(c.revision):c.id;
        int key="editKey".equals(c.type)?(int)c.x:"key".equals(c.type)?c.id:0;
        String text=key==0?c.text:"";
        String reason=null;
        if(c.revision!=session || !eligible) reason="The editing context changed. Draft retained; focus the intended field again.";
        else if(key!=0 && key!=KeyEvent.VK_TAB && key!=KeyEvent.VK_ENTER && key!=KeyEvent.VK_BACK_SPACE && key!=KeyEvent.VK_ESCAPE)
            reason="Unsupported command key";
        else if(key==0 && !validText(text)) reason="Unsupported character. Draft retained.";
        else if(edits.size()>=16 || pendingCharacters()+text.length()>2048) reason="Input queue full. Draft retained.";
        if(reason!=null) { EditReceipts.update(id,EditReceipts.State.REJECTED,0,session,reason); return false; }
        edits.add(new Edit(id,key,session,text));
        EditReceipts.update(id,EditReceipts.State.VALIDATED,0,session,"Validated; not yet delivered to the game"); return true;
    }
    private int pendingCharacters() {
        int size=active==null?0:active.text.length()-active.consumed;
        for(Edit e:edits) size+=e.text.length(); return size;
    }
    void pump(Class346_Sub1 keyboard,long session) {
        if(active==null) active=edits.poll();
        if(active==null) return;
        if(active.session!=session) { cancel("Field transition cancelled queued edits"); return; }
        Edit e=active;
        if(e.key!=0) {
            if(e.dispatched==0) {
                e.dispatched=1;
                if(!keyboard.mobileReceiptKey(e.key,e.id)) {
                    EditReceipts.update(e.id,EditReceipts.State.REJECTED,0,session,"No native keyboard target; draft retained"); active=null;
                }
            }
        } else if(e.dispatched<e.text.length()) {
            int end=Math.min(e.dispatched+24,e.text.length());
            if(!keyboard.mobileReceiptText(e.text.substring(e.dispatched,end),e.id)) {
                EditReceipts.update(e.id,EditReceipts.State.REJECTED,e.consumed,session,"Keyboard target unavailable; draft retained"); active=null; return;
            }
            e.dispatched=end;
            EditReceipts.update(e.id,EditReceipts.State.DISPATCHING,e.consumed,session,"Delivering; draft retained until you confirm the field");
        }
    }
    void consumed(int id,int kind) {
        if(active==null || active.id!=id) return;
        if(active.key==0 && kind==3) active.consumed++;
        else if(active.key!=0 && kind==2) active.keySeen=true;
    }
    long finishCycle(long session) {
        if(active==null) return session;
        Edit e=active;
        if(e.session!=session) { cancel("Editing context changed"); return session; }
        if(e.key==0?e.consumed>=e.text.length():e.keySeen) {
            boolean transition=e.key==KeyEvent.VK_TAB || e.key==KeyEvent.VK_ENTER || e.key==KeyEvent.VK_ESCAPE;
            long next=transition?session+1:session;
            EditReceipts.update(e.id,EditReceipts.State.DELIVERED,e.consumed,next,
                e.key==0?"Delivered to the game's keyboard. Field acceptance is not observable; draft retained."
                :"Command delivered. Confirm that the intended field is focused.");
            active=null;
            if(transition) {
                for(Edit queued:edits) EditReceipts.update(queued.id,EditReceipts.State.CANCELLED,0,next,"Field changed; submit the retained draft again");
                edits.clear();
            }
            return next;
        }
        return session;
    }
    void cancel(String reason) {
        if(active!=null) EditReceipts.update(active.id,EditReceipts.State.CANCELLED,active.consumed,active.session,reason);
        for(Edit e:edits) EditReceipts.update(e.id,EditReceipts.State.CANCELLED,0,e.session,reason);
        active=null; edits.clear();
    }
    static boolean validText(String value) {
        if(value==null || value.length()>2048) return false;
        for(int i=0;i<value.length();i++) if(Character.isISOControl(value.charAt(i)) || !Class122.method1089(-125,value.charAt(i))) return false;
        return true;
    }
}
