package adris.altoclef.lavibridge.actionstate;

//20260725_kpopmodder: Added mutable action record so registry orchestration is separate from fields.

import java.time.Instant;
import java.util.Map;

public class LaviActionRecord {

    private final String actionId;
    private final String type;
    private final String command;
    private final Map<String, Object> request;
    private final Instant createdAt;
    private Instant updatedAt;
    private LaviActionStatus status;
    private String message;
    private String error;

    public LaviActionRecord(
            String actionId,
            String type,
            String command,
            Map<String, Object> request,
            Instant createdAt
    ) {
        this.actionId = actionId;
        this.type = type;
        this.command = command;
        this.request = request;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = LaviActionStatus.QUEUED;
    }

    public String getActionId() {
        return actionId;
    }

    public String getType() {
        return type;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, Object> getRequest() {
        return request;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public LaviActionStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public void setStatus(LaviActionStatus status) {
        this.status = status;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setError(String error) {
        this.error = error;
    }
}
