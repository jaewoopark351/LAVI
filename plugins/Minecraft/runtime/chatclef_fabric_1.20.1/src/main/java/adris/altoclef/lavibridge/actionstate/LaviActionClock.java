package adris.altoclef.lavibridge.actionstate;

//20260725_kpopmodder: Added clock wrapper for LAVI action timestamps.

import java.time.Instant;

public class LaviActionClock {
    public Instant now() {
        return Instant.now();
    }
}
