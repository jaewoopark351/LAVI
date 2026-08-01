package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Observe ChatClef task identity without owning or mutating engine state.
public final class FabricChatClefTaskSnapshot {
    private final boolean available;
    private final String className;
    private final String description;
    private final String identity;
    private final String error;

    private FabricChatClefTaskSnapshot(
            boolean available,
            String className,
            String description,
            String identity,
            String error
    ) {
        this.available = available;
        this.className = className;
        this.description = description;
        this.identity = identity;
        this.error = error;
    }

    public static FabricChatClefTaskSnapshot capture(Object task) {
        if (task == null) {
            return new FabricChatClefTaskSnapshot(false, "", "", "", "");
        }
        return new FabricChatClefTaskSnapshot(
                true,
                task.getClass().getName(),
                safeDescription(task),
                Integer.toHexString(System.identityHashCode(task)),
                ""
        );
    }

    public static FabricChatClefTaskSnapshot unavailable(Throwable error) {
        return new FabricChatClefTaskSnapshot(
                false,
                "",
                "",
                "",
                error.getClass().getSimpleName() + ": " + nullSafeMessage(error)
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("available", available);
        payload.put("class_name", className);
        payload.put("description", description);
        payload.put("identity", identity);
        payload.put("error", error);
        return payload;
    }

    private static String safeDescription(Object task) {
        try {
            return String.valueOf(task);
        } catch (Throwable error) {
            return "<toString failed: " + error.getClass().getSimpleName() + ">";
        }
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }
}
