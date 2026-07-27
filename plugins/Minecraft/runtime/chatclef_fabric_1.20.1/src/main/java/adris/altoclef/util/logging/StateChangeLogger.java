package adris.altoclef.util.logging;

import adris.altoclef.Debug;

import java.util.Objects;

//20260727_kpopmodder: Added a tiny state-change logger for troubleshooting task decisions without tick-spamming logs.
public class StateChangeLogger {
    private final String label;
    private String lastStateKey = "";

    public StateChangeLogger(String label) {
        this.label = label;
    }

    public void state(String state) {
        state(state, state);
    }

    public void state(String stateKey, String detail) {
        if (Objects.equals(lastStateKey, stateKey)) {
            return;
        }

        lastStateKey = stateKey;
        Debug.logWarning("[" + label + "] " + detail);
    }

    public void event(String message) {
        lastStateKey = "";
        Debug.logWarning("[" + label + "] " + message);
    }

    public void reset() {
        lastStateKey = "";
    }
}
