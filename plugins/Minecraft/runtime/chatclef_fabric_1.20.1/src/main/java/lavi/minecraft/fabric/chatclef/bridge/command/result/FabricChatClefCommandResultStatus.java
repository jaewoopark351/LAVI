package lavi.minecraft.fabric.chatclef.bridge.command.result;

//20260804_kpopmodder: Type Fabric ChatClef command result status while preserving v1 wire values.
public enum FabricChatClefCommandResultStatus {
    ACCEPTED("accepted", true),
    RUNNING("running", true),
    COMPLETED("completed", true),
    REJECTED("rejected", false),
    FAILED("failed", false),
    CANCELLED("cancelled", false),
    DEADLINE_EXCEEDED("deadline_exceeded", false),
    UNKNOWN("unknown", false);

    private final String wireValue;
    private final boolean ok;

    FabricChatClefCommandResultStatus(String wireValue, boolean ok) {
        this.wireValue = wireValue;
        this.ok = ok;
    }

    public String wireValue() {
        return wireValue;
    }

    public boolean ok() {
        return ok;
    }
}
