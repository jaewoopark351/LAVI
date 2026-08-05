package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

final class FabricChatClefLifecycleDetailValues {
    private FabricChatClefLifecycleDetailValues() {
    }

    static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static String nullSafeMessage(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return "";
        }
        return error.getMessage();
    }
}
