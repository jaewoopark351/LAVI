package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this class to track LAVI bridge action state separately from HTTP routing.

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class LaviActionRegistry {

    public enum ActionStatus {
        QUEUED,
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

    private final AtomicLong sequence = new AtomicLong(1L);
    private Action currentAction;

    public synchronized Map<String, Object> createAction(String type, String command, Map<String, Object> request) {
        currentAction = new Action(nextActionId(), type, command, request);
        return currentAction.toMap();
    }

    public synchronized void markRunning(String actionId) {
        if (matchesCurrent(actionId)) {
            currentAction.status = ActionStatus.RUNNING;
            currentAction.updatedAt = Instant.now();
        }
    }

    public synchronized void markSucceeded(String actionId, String message) {
        if (matchesCurrent(actionId)) {
            currentAction.status = ActionStatus.SUCCEEDED;
            currentAction.message = message;
            currentAction.updatedAt = Instant.now();
        }
    }

    public synchronized void markFailed(String actionId, String error) {
        if (matchesCurrent(actionId)) {
            currentAction.status = ActionStatus.FAILED;
            currentAction.error = error;
            currentAction.updatedAt = Instant.now();
        }
    }

    public synchronized void markCancelled(String actionId, String message) {
        if (matchesCurrent(actionId)) {
            currentAction.status = ActionStatus.CANCELLED;
            currentAction.message = message;
            currentAction.updatedAt = Instant.now();
        }
    }

    public synchronized Map<String, Object> cancelCurrentIfRunning(String message) {
        if (hasRunningAction()) {
            currentAction.status = ActionStatus.CANCELLED;
            currentAction.message = message;
            currentAction.updatedAt = Instant.now();
            return currentAction.toMap();
        }
        return null;
    }

    public synchronized boolean hasRunningAction() {
        return currentAction != null
                && (currentAction.status == ActionStatus.QUEUED || currentAction.status == ActionStatus.RUNNING);
    }

    public synchronized Map<String, Object> currentAction() {
        if (currentAction == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("action", null);
            return result;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("action", currentAction.toMap());
        return result;
    }

    public synchronized Map<String, Object> currentActionSnapshotOnly() {
        return currentAction == null ? null : currentAction.toMap();
    }

    private boolean matchesCurrent(String actionId) {
        return currentAction != null && currentAction.actionId.equals(actionId);
    }

    private String nextActionId() {
        return "lavi-" + sequence.getAndIncrement();
    }

    private static class Action {
        private final String actionId;
        private final String type;
        private final String command;
        private final Map<String, Object> request;
        private final Instant createdAt;
        private Instant updatedAt;
        private ActionStatus status;
        private String message;
        private String error;

        private Action(String actionId, String type, String command, Map<String, Object> request) {
            this.actionId = actionId;
            this.type = type;
            this.command = command;
            this.request = request;
            this.createdAt = Instant.now();
            this.updatedAt = createdAt;
            this.status = ActionStatus.QUEUED;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("action_id", actionId);
            result.put("type", type);
            result.put("status", status.name().toLowerCase());
            result.put("command", command);
            result.put("request", request);
            result.put("message", message);
            result.put("error", error);
            result.put("created_at", createdAt.toString());
            result.put("updated_at", updatedAt.toString());
            return result;
        }
    }
}
