package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Close the STOP target-scope vocabulary at the Java trust boundary.
public enum FabricChatClefStopControlTargetScope {
    TRACKED_COMMAND("tracked_command"),
    CURRENT_GLOBAL_AUTOMATION("current_global_automation");

    private final String wireValue;

    FabricChatClefStopControlTargetScope(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static FabricChatClefStopControlTargetScope fromWireValue(String value) {
        for (FabricChatClefStopControlTargetScope scope : values()) {
            if (scope.wireValue.equals(value)) {
                return scope;
            }
        }
        return null;
    }
}
