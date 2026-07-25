package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this class to track LAVI bridge action state separately from HTTP routing.

import adris.altoclef.lavibridge.actionstate.LaviActionClock;
import adris.altoclef.lavibridge.actionstate.LaviActionIdGenerator;
import adris.altoclef.lavibridge.actionstate.LaviActionRecord;
import adris.altoclef.lavibridge.actionstate.LaviActionSnapshotFactory;
import adris.altoclef.lavibridge.actionstate.LaviActionStatus;
import adris.altoclef.lavibridge.actionstate.LaviActionStatusMutator;

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviActionRegistry {

    private final LaviActionIdGenerator idGenerator;
    private final LaviActionClock clock;
    private final LaviActionSnapshotFactory snapshotFactory;
    private final LaviActionStatusMutator statusMutator;
    private LaviActionRecord currentAction;

    public LaviActionRegistry() {
        this(
                new LaviActionIdGenerator(),
                new LaviActionClock(),
                new LaviActionSnapshotFactory(),
                new LaviActionStatusMutator()
        );
    }

    public LaviActionRegistry(
            LaviActionIdGenerator idGenerator,
            LaviActionClock clock,
            LaviActionSnapshotFactory snapshotFactory,
            LaviActionStatusMutator statusMutator
    ) {
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.snapshotFactory = snapshotFactory;
        this.statusMutator = statusMutator;
    }

    public synchronized Map<String, Object> createAction(String type, String command, Map<String, Object> request) {
        currentAction = new LaviActionRecord(idGenerator.nextActionId(), type, command, request, clock.now());
        return snapshotFactory.toMap(currentAction);
    }

    public synchronized void markRunning(String actionId) {
        if (matchesCurrent(actionId)) {
            statusMutator.running(currentAction, clock.now());
        }
    }

    public synchronized void markSucceeded(String actionId, String message) {
        if (matchesCurrent(actionId)) {
            statusMutator.succeeded(currentAction, message, clock.now());
        }
    }

    public synchronized void markFailed(String actionId, String error) {
        if (matchesCurrent(actionId)) {
            statusMutator.failed(currentAction, error, clock.now());
        }
    }

    public synchronized void markCancelled(String actionId, String message) {
        if (matchesCurrent(actionId)) {
            statusMutator.cancelled(currentAction, message, clock.now());
        }
    }

    public synchronized Map<String, Object> cancelCurrentIfRunning(String message) {
        if (hasRunningAction()) {
            statusMutator.cancelled(currentAction, message, clock.now());
            return snapshotFactory.toMap(currentAction);
        }
        return null;
    }

    public synchronized boolean hasRunningAction() {
        return currentAction != null
                && (currentAction.getStatus() == LaviActionStatus.QUEUED
                || currentAction.getStatus() == LaviActionStatus.RUNNING);
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
        result.put("action", snapshotFactory.toMap(currentAction));
        return result;
    }

    public synchronized Map<String, Object> currentActionSnapshotOnly() {
        return currentAction == null ? null : snapshotFactory.toMap(currentAction);
    }

    private boolean matchesCurrent(String actionId) {
        return currentAction != null && currentAction.getActionId().equals(actionId);
    }
}
