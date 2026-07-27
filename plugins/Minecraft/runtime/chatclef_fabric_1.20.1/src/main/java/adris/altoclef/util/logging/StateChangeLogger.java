package adris.altoclef.util.logging;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.Settings;

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
        if (!isDebugLoggingEnabled()) {
            return;
        }
        logState(stateKey, detail);
    }

    public void debugState(String state) {
        state(state, state);
    }

    public void debugState(String stateKey, String detail) {
        state(stateKey, detail);
    }

    public void event(String message) {
        lastStateKey = "";
        Debug.logWarning("[" + label + "] " + message);
    }

    public void debugEvent(String message) {
        if (!isDebugLoggingEnabled()) {
            return;
        }
        event(message);
    }

    public void reset() {
        lastStateKey = "";
    }

    private void logState(String stateKey, String detail) {
        if (Objects.equals(lastStateKey, stateKey)) {
            return;
        }

        lastStateKey = stateKey;
        Debug.logWarning("[" + label + "] " + detail);
    }

    private boolean isDebugLoggingEnabled() {
        AltoClef mod = AltoClef.getInstance();
        if (mod == null) {
            return false;
        }
        Settings settings = mod.getModSettings();
        return settings != null && settings.shouldLogChatClefDebug();
    }
}
