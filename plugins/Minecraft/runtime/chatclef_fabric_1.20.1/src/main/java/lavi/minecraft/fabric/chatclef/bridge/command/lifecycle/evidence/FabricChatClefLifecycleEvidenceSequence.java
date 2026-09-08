package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

//20260907_kpopmodder: Keep one monotonic lifecycle-evidence sequence per command execution.
public final class FabricChatClefLifecycleEvidenceSequence {
    private static final int INITIAL_DISPATCH_SEQUENCE = 1;

    private int lastSequence = INITIAL_DISPATCH_SEQUENCE;

    public int initialDispatchSequence() {
        return INITIAL_DISPATCH_SEQUENCE;
    }

    public synchronized int nextSequence() {
        if (lastSequence == Integer.MAX_VALUE) {
            throw new IllegalStateException("lifecycle evidence sequence exhausted");
        }
        return ++lastSequence;
    }
}
