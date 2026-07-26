package adris.altoclef.util.logging;

import adris.altoclef.Debug;

import java.util.Objects;

//20260727_kpopmodder: Added a tiny state-change logger for troubleshooting task decisions without tick-spamming logs.
public class StateChangeLogger {
    private final String label;
    private String lastState = "";

    public StateChangeLogger(String label) {
        this.label = label;
    }

    public void state(String state) {
        if (Objects.equals(lastState, state)) {
            return;
        }

        lastState = state;
        Debug.logWarning("[" + label + "] " + state);
    }

    public void event(String message) {
        lastState = "";
        Debug.logWarning("[" + label + "] " + message);
    }

    public void reset() {
        lastState = "";
    }
}
