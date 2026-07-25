package adris.altoclef.lavibridge.actionstate;

//20260725_kpopmodder: Added action id generator separate from action storage.

import java.util.concurrent.atomic.AtomicLong;

public class LaviActionIdGenerator {

    private final AtomicLong sequence = new AtomicLong(1L);

    public String nextActionId() {
        return "lavi-" + sequence.getAndIncrement();
    }
}
