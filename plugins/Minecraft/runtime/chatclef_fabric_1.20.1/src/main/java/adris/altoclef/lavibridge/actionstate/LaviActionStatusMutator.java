package adris.altoclef.lavibridge.actionstate;

//20260725_kpopmodder: Added transition helper for LAVI action status updates.

import java.time.Instant;

public class LaviActionStatusMutator {

    public void running(LaviActionRecord action, Instant updatedAt) {
        action.setStatus(LaviActionStatus.RUNNING);
        action.setUpdatedAt(updatedAt);
    }

    public void succeeded(LaviActionRecord action, String message, Instant updatedAt) {
        action.setStatus(LaviActionStatus.SUCCEEDED);
        action.setMessage(message);
        action.setUpdatedAt(updatedAt);
    }

    public void failed(LaviActionRecord action, String error, Instant updatedAt) {
        action.setStatus(LaviActionStatus.FAILED);
        action.setError(error);
        action.setUpdatedAt(updatedAt);
    }

    public void cancelled(LaviActionRecord action, String message, Instant updatedAt) {
        action.setStatus(LaviActionStatus.CANCELLED);
        action.setMessage(message);
        action.setUpdatedAt(updatedAt);
    }
}
