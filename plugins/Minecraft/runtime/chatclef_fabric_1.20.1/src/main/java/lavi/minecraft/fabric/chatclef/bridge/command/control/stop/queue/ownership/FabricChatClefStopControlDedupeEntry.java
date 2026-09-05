package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Keep one dedupe fingerprint and terminal tombstone in its own type.
final class FabricChatClefStopControlDedupeEntry {
    private final String fingerprint;
    private boolean terminal;

    FabricChatClefStopControlDedupeEntry(String fingerprint) {
        this.fingerprint = fingerprint == null ? "" : fingerprint;
    }

    boolean matchesFingerprint(String candidate) {
        return fingerprint.equals(candidate == null ? "" : candidate);
    }

    boolean terminal() {
        return terminal;
    }

    void markTerminal() {
        terminal = true;
    }
}
