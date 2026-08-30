package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Optional;

/**
 * Use this whenever you want to travel to a target position that may change.
 * <p>
 * https://www.notion.so/Closest-threshold-ing-system-utility-c3816b880402494ba9209c9f9b62b8bf
 */
public abstract class AbstractDoToClosestObjectTask<T> extends Task {

    private final HashMap<T, CachedHeuristic> heuristicMap = new HashMap<>();
    private T currentlyPursuing = null;
    private boolean wasWandering;
    private Task goalTask = null;

    protected abstract Vec3d getPos(AltoClef mod, T obj);

    protected abstract Optional<T> getClosestTo(AltoClef mod, Vec3d pos);

    protected abstract Vec3d getOriginPos(AltoClef mod);

    protected abstract Task getGoalTask(T obj);

    protected abstract boolean isValid(AltoClef mod, T obj);

    // Virtual
    protected Task getWanderTask(AltoClef mod) {
        return new TimeoutWanderTask(true);
    }

    public void resetSearch() {
        currentlyPursuing = null;
        heuristicMap.clear();
        goalTask = null;
    }

    public boolean wasWandering() {
        return wasWandering;
    }

    private double getCurrentCalculatedHeuristic(AltoClef mod) {
        Optional<Double> ticksRemainingOp = mod.getClientBaritone().getPathingBehavior().ticksRemainingInSegment();
        return ticksRemainingOp.orElse(Double.POSITIVE_INFINITY);
    }

    @Override
    protected Task onTick() {
        wasWandering = false;
        AltoClef mod = AltoClef.getInstance();

        //20260730_kpopmodder: Diagnostics-only LAVI log for closest-object pursuit loop investigation; no behavior change.
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        // Do not invoke a Task completion predicate solely to populate diagnostics.
        ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "ON_TICK", "closest_object_tick_begin", this,
                "currentlyPursuing", objectSummary(mod, currentlyPursuing),
                "heuristicCacheSize", heuristicMap.size(),
                "goalTask", ChatClefDiagnostics.taskSummary(goalTask),
                "goalTaskActive", ChatClefDiagnostics.safeValue(() -> goalTask != null && goalTask.isActive()),
                "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()),
                "playerPosition", ChatClefDiagnostics.playerPosition(mod));

        // Reset our pursuit if our pursuing object no longer is pursuable.
        if (currentlyPursuing != null && !isValid(mod, currentlyPursuing)) {
            ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "DECISION", "current_pursuit_invalidated", this,
                    "invalidPursuit", objectSummary(mod, currentlyPursuing),
                    "heuristicCacheSizeBefore", heuristicMap.size());
            // This is probably a good idea, no?
            heuristicMap.remove(currentlyPursuing);
            currentlyPursuing = null;
        }

        // Get closest object
        Optional<T> checkNewClosest = getClosestTo(mod, getOriginPos(mod));
        ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "OBSERVE", "closest_object_candidate", this,
                "candidatePresent", checkNewClosest.isPresent(),
                "candidate", checkNewClosest.map(candidate -> objectSummary(mod, candidate)).orElse("none"),
                "currentlyPursuing", objectSummary(mod, currentlyPursuing));

        // Receive closest object and position
        if (checkNewClosest.isPresent() && !checkNewClosest.get().equals(currentlyPursuing)) {
            T newClosest = checkNewClosest.get();
            // Different closest object
            if (currentlyPursuing == null) {
                // We don't have a closest object
                ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "DECISION", "select_initial_pursuit", this,
                        "newClosest", objectSummary(mod, newClosest));
                currentlyPursuing = newClosest;
            } else {
                if (goalTask != null /*isMovingToClosestPos(mod)*/) {
                    setDebugState("Moving towards closest...");
                    double currentHeuristic = getCurrentCalculatedHeuristic(mod);
                    double closestDistanceSqr = getPos(mod, currentlyPursuing).squaredDistanceTo(mod.getPlayer().getPos());
                    int lastTick = WorldHelper.getTicks();
                    ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "OBSERVE", "current_pursuit_heuristic", this,
                            "currentPursuit", objectSummary(mod, currentlyPursuing),
                            "newClosest", objectSummary(mod, newClosest),
                            "currentHeuristic", currentHeuristic,
                            "closestDistanceSqr", closestDistanceSqr,
                            "worldTick", lastTick,
                            "heuristicCacheSize", heuristicMap.size());

                    if (!heuristicMap.containsKey(currentlyPursuing)) {
                        heuristicMap.put(currentlyPursuing, new CachedHeuristic());
                    }
                    CachedHeuristic h = heuristicMap.get(currentlyPursuing);
                    h.updateHeuristic(currentHeuristic);
                    h.updateDistance(closestDistanceSqr);
                    h.setTickAttempted(lastTick);
                    if (heuristicMap.containsKey(newClosest)) {
                        // Our new object has a past potential heuristic calculated, if it's better try it out.
                        CachedHeuristic maybeReAttempt = heuristicMap.get(newClosest);
                        double maybeClosestDistance = getPos(mod, newClosest).squaredDistanceTo(mod.getPlayer().getPos());
                        boolean oldHeuristicIsBetter = maybeReAttempt.getHeuristicValue() < h.getHeuristicValue();
                        boolean gotConsiderablyCloser = maybeClosestDistance < maybeReAttempt.getClosestDistanceSqr() / 4;
                        ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "OBSERVE", "previous_pursuit_candidate", this,
                                "currentPursuit", objectSummary(mod, currentlyPursuing),
                                "newClosest", objectSummary(mod, newClosest),
                                "currentHeuristic", h.getHeuristicValue(),
                                "newPreviousHeuristic", maybeReAttempt.getHeuristicValue(),
                                "currentBestDistanceSqr", h.getClosestDistanceSqr(),
                                "newClosestDistanceSqr", maybeClosestDistance,
                                "newPreviousBestDistanceSqr", maybeReAttempt.getClosestDistanceSqr(),
                                "oldHeuristicIsBetter", oldHeuristicIsBetter,
                                "gotConsiderablyCloser", gotConsiderablyCloser);
                        // Get considerably closer (divide distance by 2)
                        if (oldHeuristicIsBetter || gotConsiderablyCloser) {
                            setDebugState("Retrying old heuristic!");
                            ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "DECISION", "retry_old_heuristic", this,
                                    "previousPursuit", objectSummary(mod, currentlyPursuing),
                                    "nextPursuit", objectSummary(mod, newClosest),
                                    "oldHeuristicIsBetter", oldHeuristicIsBetter,
                                    "gotConsiderablyCloser", gotConsiderablyCloser);
                            // The currently closest previously calculated heuristic is better, move towards it!
                            currentlyPursuing = newClosest;
                            // In theory, this next line shouldn't need to be run,
                            // but it's CRITICAL to making this work for some reason
                            maybeReAttempt.updateDistance(maybeClosestDistance);
                        }
                    } else {
                        setDebugState("Trying out NEW pursuit");
                        // Our new object does not have a heuristic, TRY IT OUT!
                        ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "DECISION", "try_new_pursuit", this,
                                "previousPursuit", objectSummary(mod, currentlyPursuing),
                                "nextPursuit", objectSummary(mod, newClosest));
                        currentlyPursuing = newClosest;
                    }
                } else {
                    setDebugState("Waiting for move task to kick in...");
                    // We should keep moving towards our object until we get some new info.
                    ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "DECISION", "wait_for_goal_task_to_start", this,
                            "currentlyPursuing", objectSummary(mod, currentlyPursuing),
                            "newClosest", objectSummary(mod, newClosest),
                            "goalTask", ChatClefDiagnostics.taskSummary(goalTask));
                }
            }
        }

        if (currentlyPursuing != null) {
            goalTask = getGoalTask(currentlyPursuing);
            StoreDepositDiagnostics.logPursuitDecision(this, currentlyPursuing, checkNewClosest.orElse(null), "INVOKE_GOAL_CALLBACK");
            ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "DECISION", "return_goal_task", this,
                    "currentlyPursuing", objectSummary(mod, currentlyPursuing),
                    "goalTask", ChatClefDiagnostics.taskSummary(goalTask));
            return goalTask;
        } else {
            goalTask = null;
        }


        if (checkNewClosest.isEmpty()) {
            setDebugState("Waiting for calculations I think (wandering)");
            wasWandering = true;
            StoreDepositDiagnostics.logPursuitDecision(this, currentlyPursuing, null, "RETURN_WANDER_TASK_NO_CANDIDATE");
            ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "DECISION", "return_wander_task_no_candidate", this,
                    "heuristicCacheSize", heuristicMap.size());
            return getWanderTask(mod);
        }

        setDebugState("Waiting for calculations I think (NOT wandering)");
        StoreDepositDiagnostics.logPursuitDecision(this, currentlyPursuing, checkNewClosest.orElse(null), "RETURN_NULL_WAIT");
        ChatClefDiagnostics.logEvent("CLOSEST_OBJECT", "DECISION", "return_null_candidate_wait", this,
                "candidate", checkNewClosest.map(candidate -> objectSummary(mod, candidate)).orElse("none"));
        return null;
    }

    private String objectSummary(AltoClef mod, T obj) {
        if (obj == null) {
            return "none";
        }
        if (obj instanceof Entity entity) {
            return ChatClefDiagnostics.entitySummary(entity)
                    + "#distanceSqrToPlayer=" + ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, entity);
        }
        return ChatClefDiagnostics.className(obj) + "#" + ChatClefDiagnostics.safeValue(() -> obj);
    }

    private static class CachedHeuristic {

        private double _closestDistanceSqr;
        private int _tickAttempted;
        private double _heuristicValue;

        public CachedHeuristic() {
            _closestDistanceSqr = Double.POSITIVE_INFINITY;
            _heuristicValue = Double.POSITIVE_INFINITY;
        }

        public CachedHeuristic(double closestDistanceSqr, int tickAttempted, double heuristicValue) {
            _closestDistanceSqr = closestDistanceSqr;
            _tickAttempted = tickAttempted;
            _heuristicValue = heuristicValue;
        }

        public double getHeuristicValue() {
            return _heuristicValue;
        }

        public void updateHeuristic(double heuristicValue) {
            _heuristicValue = Math.min(_heuristicValue, heuristicValue);
        }

        public double getClosestDistanceSqr() {
            return _closestDistanceSqr;
        }

        public void updateDistance(double closestDistanceSqr) {
            _closestDistanceSqr = Math.min(_closestDistanceSqr, closestDistanceSqr);
        }

        public int getTickAttempted() {
            return _tickAttempted;
        }

        public void setTickAttempted(int tickAttempted) {
            _tickAttempted = tickAttempted;
        }
    }
}
