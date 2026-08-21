package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

//20260822_kpopmodder: Keep result_fidelity truthful for every command lifecycle result path.
public enum FabricChatClefCommandResultFidelity {
    DISPATCH_STARTED_ONLY("dispatch_started_only"),
    CALLBACK_WITHOUT_MATCHING_USER_TASK_EVENT("callback_without_matching_user_task_event"),
    CALLBACK_WITHOUT_USER_TASK("callback_without_user_task"),
    CALLBACK_PLUS_MATCHING_USER_TASK_EVENT("callback_plus_matching_user_task_event"),
    CALLBACK_PLUS_NONMATCHING_USER_TASK_EVENT("callback_plus_nonmatching_user_task_event"),
    COMMAND_EXCEPTION_OBSERVED("command_exception_observed"),
    DISPATCH_EXCEPTION_OBSERVED("dispatch_exception_observed"),
    DEADLINE_WITHOUT_VERIFIED_TERMINAL("deadline_without_verified_terminal"),
    UNKNOWN("unknown");

    private final String wireValue;

    FabricChatClefCommandResultFidelity(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
