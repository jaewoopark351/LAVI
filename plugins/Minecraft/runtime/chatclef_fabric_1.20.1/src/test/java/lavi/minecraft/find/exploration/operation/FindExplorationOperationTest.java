//#if MC == 12001
package lavi.minecraft.find.exploration.operation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import lavi.minecraft.find.approach.movement.FindApproachMovementPort;
import lavi.minecraft.find.diagnostics.FindLog;
import lavi.minecraft.find.exploration.movement.FindExplorationMovementPort;
import lavi.minecraft.find.model.FindCandidate;
import lavi.minecraft.find.model.FindRequest;
import lavi.minecraft.find.observation.FindObservationPort;
import lavi.minecraft.find.result.FindTerminalPhaseEvidence.Phase;
import lavi.minecraft.find.result.FindTerminalReasonContract;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Exercise phase lifetime, finite budgets and late collaborator results without world mutation.
class FindExplorationOperationTest {
    private static FindCandidate candidate(int x) { return new FindCandidate(7, "uuid_digest", "uuid", x, 65, 0, x * (double)x); }
    private static final class Observations implements FindObservationPort {
        Binding position = new Binding(new Object(), new Object(), "minecraft:overworld", 0, 65, 0, -64, 320);
        ArrayDeque<EntityScan> responses = new ArrayDeque<>();
        List<Integer> budgets = new ArrayList<>();
        List<Binding> origins = new ArrayList<>();
        boolean bindingMatches = true, resourceMatches = true, lost, throwRead;
        Runnable scanHook = () -> { }, revalidationHook = () -> { }, bindingHook = () -> { };
        @Override public Binding binding() { bindingHook.run(); return position; }
        @Override public boolean matches(Binding initial) { return bindingMatches; }
        @Override public boolean resourceBindingMatches(FindRequest request) { return resourceMatches; }
        @Override public EntityScan scanEntities(FindRequest request, Binding origin, int limit) {
            return scanEntities(request, origin, limit, () -> true, null);
        }
        @Override public EntityScan scanEntities(FindRequest request, Binding origin, int limit, BooleanSupplier budget, FindLog log) {
            budgets.add(limit); origins.add(origin); scanHook.run();
            if (throwRead) throw new IllegalStateException("read");
            return responses.isEmpty() ? new EntityScan(Math.min(1, limit), 0, true, null) : responses.removeFirst();
        }
        @Override public FindCandidate readBlock(FindRequest request, Binding origin, int x, int y, int z) { fail("entity must not read blocks"); return null; }
        @Override public FindCandidate revalidate(FindRequest request, Binding origin, FindCandidate selected) {
            revalidationHook.run(); return lost ? null : candidate(selected.x() + 1);
        }
        void move(double x, double z) {
            position = new Binding(position.world(), position.player(), position.dimension(), x, 65, z, -64, 320);
        }
        void found(int x) { responses.add(new EntityScan(3, 1, true, candidate(x))); }
    }
    private static final class Exploration implements FindExplorationMovementPort {
        int starts, ticks, releases;
        boolean active, quietFailure, throwCleanup, reached;
        String failure;
        BooleanSupplier budget = () -> false;
        Runnable tickHook = () -> { }, cleanupHook = () -> { };
        @Override public void begin(FindObservationPort.Binding original, BooleanSupplier budget) { starts++; active = true; this.budget = budget; }
        @Override public Step tick() {
            ticks++; tickHook.run();
            return new Step(failure, reached, !active, new Progress(1000000, 1000000, 0, true));
        }
        @Override public void suspend() { releases++; active = false; cleanupHook.run(); if (throwCleanup) throw new IllegalStateException("cleanup"); }
        @Override public boolean quiet() { return !active && !quietFailure; }
    }
    private static final class Approach implements FindApproachMovementPort {
        int starts, ticks, releases;
        boolean active, cleanupObserved;
        FindObservationPort.Binding origin;
        Step response = Step.waiting();
        Runnable beginHook = () -> { }, tickHook = () -> { }, cleanupHook = () -> { };
        @Override public void begin(FindObservationPort.Binding origin, FindRequest request, FindCandidate selected) {
            starts++; this.origin = origin; active = true; beginHook.run();
        }
        @Override public Step tick() { ticks++; tickHook.run(); return response; }
        @Override public void suspend() { releases++; active = false; cleanupObserved = true; cleanupHook.run(); }
        @Override public boolean quiet() { return !active; }
    }
    private static final class Fixture {
        final AtomicLong clock = new AtomicLong();
        final AtomicBoolean owns = new AtomicBoolean(true);
        final Object root = new Object();
        final Observations reads = new Observations();
        final Exploration exploration = new Exploration();
        final Approach approach = new Approach();
        final List<String> events = new ArrayList<>();
        final FindExplorationOperation operation;
        Fixture(String mode) { this(mode, limits(4, 40, 20, 8, 512, 1024), false); }
        Fixture(String mode, FindExplorationLimits limits, boolean badLog) {
            operation = new FindExplorationOperation(new FindRequest("entity", "minecraft:villager", mode, "catalog", 1),
                "op", root, reads, exploration, approach, clock::get,
                new FindLog("op", (event, values) -> { if (badLog) throw new IllegalStateException("sink"); events.add(event); }),
                0, limits, owns::get);
        }
        void tick(long delta) { clock.addAndGet(delta); operation.tick(owns.get()); }
        void assertReason(String expected) {
            assertNotNull(operation.outcome()); assertEquals(expected, operation.outcome().reason());
            assertTrue(FindTerminalReasonContract.validates(operation.outcome(), root), operation.outcome().toString());
        }
    }
    private static FindExplorationLimits limits(int local, int cumulative, int scans, int starts, double displacement, double travel) {
        return new FindExplorationLimits(1000, 600, 300, 100, 50, 10, 100, 10000,
            local, cumulative, scans, starts, displacement, travel);
    }
    @Test void immediateReportHasNoExplorationAndUsesFinalLiveCoordinatesOnce() {
        Fixture f = new Fixture("report"); f.reads.found(80); f.tick(0);
        assertTrue(f.operation.outcome().satisfied()); assertEquals(82, f.operation.outcome().candidate().x());
        f.assertReason("discovery_target_revalidated_and_exploration_quiet");
        assertEquals(0, f.exploration.starts); assertEquals(0, f.approach.starts);
        var assigned = f.operation.outcome(); f.tick(10); f.operation.start(); f.operation.suspend();
        assertSame(assigned, f.operation.outcome()); assertEquals(1, f.events.stream().filter("PARENT_OUTCOME"::equals).count());
    }
    @Test void internalMissTravelsAndReobservesFromCurrentOriginBeforeReport() {
        Fixture f = new Fixture("report"); f.tick(0);
        assertNull(f.operation.outcome()); assertFalse(f.events.contains("PARENT_OUTCOME"));
        f.exploration.tickHook = () -> f.reads.move(24, 0); f.tick(1);
        assertEquals(1, f.exploration.starts); assertNull(f.operation.outcome());
        f.reads.found(90); f.tick(9);
        f.assertReason("discovery_target_revalidated_and_exploration_quiet");
        assertEquals(24, f.reads.origins.get(1).x()); assertFalse(f.exploration.active);
        assertTrue(f.operation.outcome().terminalEvidence().explorationQuiet());
    }
    @Test void approachHandoffUsesFreshOriginAndNeverRestartsExplorationAfterLoss() {
        Fixture f = new Fixture("approach"); f.tick(0);
        f.exploration.tickHook = () -> f.reads.move(24, 0); f.tick(1); f.reads.found(90); f.tick(9);
        assertNull(f.operation.outcome()); assertEquals(24, f.approach.origin.x()); assertFalse(f.exploration.active);
        f.approach.response = new FindApproachMovementPort.Step("selected_target_lost", false, null); f.tick(1);
        f.assertReason("selected_target_lost"); assertEquals(1, f.exploration.starts);
        assertEquals(Phase.APPROACHING, f.operation.outcome().terminalEvidence().phase());
    }
    @Test void approachMotionDoesNotExtendDiscoverySamplingOrItsDistanceBudget() {
        Fixture f = new Fixture("approach"); f.reads.found(80); f.tick(0);
        long samples = f.events.stream().filter("DISCOVERY_MOVEMENT_SAMPLED"::equals).count();
        assertTrue(samples > 0); assertNull(f.operation.outcome());
        f.reads.move(2048, 0); f.tick(1); f.operation.observeClientTick(true);
        f.operation.suspend(); f.reads.move(0, 0); f.tick(1);
        assertNull(f.operation.outcome()); assertEquals(1, f.approach.starts);
        assertEquals(samples, f.events.stream().filter("DISCOVERY_MOVEMENT_SAMPLED"::equals).count());
        f.tick(298); f.assertReason("approach_deadline_exhausted");
    }
    @Test void distantApproachMayReportNativeRouteFailureRatherThanDiscoveryLoss() {
        Fixture f = new Fixture("approach"); f.reads.found(80); f.tick(0);
        assertEquals(1, f.approach.starts); assertNull(f.operation.outcome());
        f.approach.response = new FindApproachMovementPort.Step("native_finite_plan_unreachable", false, null); f.tick(1);
        f.assertReason("post_discovery_approach_unreachable"); assertEquals("UNREACHABLE", f.operation.outcome().findResult());
    }
    @Test void safeApproachArrivalRequiresSameIdentityAndCleanup() {
        Fixture f = new Fixture("approach"); f.reads.found(3); f.tick(0);
        f.approach.response = new FindApproachMovementPort.Step("ALREADY_IN_SAFE_RANGE", true, candidate(5)); f.tick(1);
        f.assertReason("same_target_safe_range_and_owned_cleanup"); assertTrue(f.operation.outcome().satisfied());
        assertTrue(f.approach.cleanupObserved); assertTrue(f.operation.outcome().terminalEvidence().approachArrived());
    }
    @Test void partialScanWithSeenMobNeverDiscoversOrExplores() {
        Fixture f = new Fixture("report");
        f.reads.responses.add(new FindObservationPort.EntityScan(4, 1, false, candidate(80)));
        f.tick(0); f.assertReason("local_entity_visit_limit_exhausted");
        assertNull(f.operation.outcome().candidate()); assertFalse(f.operation.outcome().terminalEvidence().discoveryComplete());
        assertEquals(0, f.exploration.starts);
    }
    @Test void observationSliceExhaustionRejectsEvenCompletePrefix() {
        Fixture f = new Fixture("report"); f.reads.found(80); f.reads.scanHook = () -> f.clock.set(50); f.tick(0);
        f.assertReason("local_observation_deadline_exhausted"); assertFalse(f.operation.outcome().satisfied());
    }
    @Test void readFailureWinsOverSeenPrefixAndConcurrentDeadline() {
        Fixture f = new Fixture("report");
        f.reads.responses.add(new FindObservationPort.EntityScan(2, 1, false, candidate(80), "entity_read_failed"));
        f.reads.scanHook = () -> f.clock.set(600); f.tick(0);
        f.assertReason("observation_read_failed"); assertEquals(2, f.operation.outcome().visited());
    }
    @Test void discoveryClockStartsAtSubmissionBeforeFirstEvaluation() {
        Fixture f = new Fixture("report"); f.tick(600);
        f.assertReason("discovery_deadline_exhausted"); assertTrue(f.reads.budgets.isEmpty());
    }
    @Test void cleanupCrossingDiscoveryDeadlineCannotReportOrHandoff() {
        for (String mode : List.of("report", "approach")) {
            Fixture f = new Fixture(mode); f.reads.found(80);
            f.exploration.cleanupHook = () -> f.clock.set(600); f.tick(0);
            f.assertReason("discovery_deadline_exhausted"); assertFalse(f.operation.outcome().satisfied()); assertEquals(0, f.approach.starts);
        }
    }
    @Test void revalidationCrossingDiscoveryDeadlineCannotHandoff() {
        Fixture f = new Fixture("approach"); f.reads.found(80);
        f.reads.revalidationHook = () -> f.clock.addAndGet(301); f.tick(0);
        f.assertReason("discovery_deadline_exhausted"); assertEquals(0, f.approach.starts);
    }
    @Test void approachBeginCannotHideExpiredDiscoveryClock() {
        Fixture f = new Fixture("approach"); f.reads.found(80); f.approach.beginHook = () -> f.clock.set(600); f.tick(0);
        f.assertReason("discovery_deadline_exhausted"); assertFalse(f.operation.outcome().terminalEvidence().approachHandedOff()); assertFalse(f.approach.active);
    }
    @Test void approachClockStartsAtHandoffAndParentStillApplies() {
        Fixture f = new Fixture("approach"); f.tick(0); f.tick(1); f.operation.suspend(); f.reads.found(80); f.tick(499);
        assertNull(f.operation.outcome()); f.tick(299); assertNull(f.operation.outcome()); f.tick(1);
        f.assertReason("approach_deadline_exhausted");
        Fixture parent = new Fixture("approach"); parent.tick(0); parent.operation.suspend(); parent.reads.found(80); parent.tick(599); parent.tick(401);
        parent.assertReason("parent_deadline_exhausted");
    }
    @Test void globalVisitsPassOnlyRemainingAllowanceAndRetainActualCounts() {
        Fixture f = new Fixture("report", limits(4, 6, 20, 8, 512, 1024), false);
        f.reads.responses.add(new FindObservationPort.EntityScan(4, 0, true, null)); f.tick(0);
        f.reads.responses.add(new FindObservationPort.EntityScan(2, 1, false, candidate(80))); f.tick(10);
        f.assertReason("cumulative_entity_visit_limit_exhausted");
        assertEquals(List.of(4, 2), f.reads.budgets); assertEquals(6, f.operation.outcome().visited());
    }
    @Test void repeatedMissesRespectScanStartCountWithoutFinalMissResponses() {
        Fixture f = new Fixture("report", limits(4, 40, 3, 8, 512, 1024), false);
        f.tick(0); f.tick(10); f.tick(10); assertNull(f.operation.outcome()); f.tick(10);
        f.assertReason("scan_count_limit_exhausted"); assertEquals(3, f.reads.budgets.size());
    }
    @Test void routeStartCountSurvivesWaypointsAndDefenseResumes() {
        Fixture f = new Fixture("report", limits(4, 40, 20, 2, 512, 1024), false);
        f.tick(0); f.tick(1); f.operation.suspend(); f.tick(1); assertEquals(2, f.exploration.starts);
        f.operation.suspend(); f.tick(1); f.assertReason("exploration_start_limit_exhausted"); assertEquals(2, f.exploration.starts);
    }
    @Test void diagnosticProgressCannotResetNoProgressLimit() {
        Fixture f = new Fixture("report"); f.tick(0); f.tick(1); f.tick(99);
        f.assertReason("exploration_no_progress"); assertEquals("UNREACHABLE", f.operation.outcome().findResult());
    }
    @Test void waypointArrivalResetsOnlyActiveNoProgressTime() {
        Fixture f = new Fixture("report"); f.tick(0); f.exploration.reached = true; f.tick(90);
        f.exploration.reached = false; f.tick(90); assertNull(f.operation.outcome());
        f.tick(10); f.assertReason("exploration_no_progress");
    }
    @Test void defenseSamplesTravelButPausesOnlyActiveProgressAndPreservesDeadline() {
        Fixture f = new Fixture("report"); f.tick(0); f.tick(20); f.operation.suspend();
        f.reads.move(40, 0); f.clock.addAndGet(400); f.operation.observeClientTick(true); assertNull(f.operation.outcome());
        f.tick(1); assertNull(f.operation.outcome()); assertEquals(2, f.exploration.starts);
        f.operation.suspend(); f.clock.set(600); f.operation.observeClientTick(true); f.assertReason("discovery_deadline_exhausted");
    }
    @Test void defenseDisplacementEndsDiscoveryWithoutResettingOriginalOrigin() {
        Fixture f = new Fixture("report"); f.tick(0); f.operation.suspend(); f.reads.move(512, 0); f.operation.observeClientTick(true);
        f.assertReason("exploration_displacement_limit_exhausted");
    }
    @Test void sampledTravelCountsBacktrackingIncludingDefense() {
        Fixture f = new Fixture("report", limits(4, 40, 20, 8, 512, 50), false);
        f.tick(0); f.operation.suspend(); f.reads.move(30, 0); f.operation.observeClientTick(true);
        f.reads.move(0, 0); f.operation.observeClientTick(true); f.assertReason("exploration_sampled_movement_limit_exhausted");
    }
    @Test void unavailableOrNonFiniteSampleDoesNotAssumeZero() {
        Fixture f = new Fixture("report"); f.tick(0); f.reads.move(Double.NaN, 0); f.operation.observeClientTick(true);
        f.assertReason("movement_sample_unavailable");
    }
    @Test void missingTickGapHasTypedFailure() {
        Fixture f = new Fixture("report"); f.tick(0); f.clock.set(10001); f.operation.observeClientTick(true);
        f.assertReason("movement_sample_unavailable");
    }
    @Test void sameTargetLostDuringCleanupDoesNotResumeExploration() {
        Fixture f = new Fixture("report"); f.tick(0); f.tick(1); f.reads.found(80);
        f.exploration.cleanupHook = () -> f.reads.lost = true; f.tick(9);
        f.assertReason("selected_target_lost"); assertEquals(1, f.exploration.starts);
    }
    @Test void worldOrCatalogChangeDuringReadWinsBeforePublication() {
        Fixture f = new Fixture("report"); f.reads.found(80); f.reads.revalidationHook = () -> f.reads.resourceMatches = false; f.tick(0);
        f.assertReason("world_player_or_catalog_binding_changed"); assertFalse(f.operation.outcome().satisfied());
    }
    @Test void synchronousRootReplacementFencesScanAndEveryLaterMovement() {
        Fixture f = new Fixture("report"); f.reads.found(80); f.reads.scanHook = () -> f.owns.set(false); f.tick(0);
        assertNull(f.operation.outcome()); assertTrue(f.operation.retired()); assertTrue(f.operation.ownedQuiet());
        f.tick(1); assertEquals(0, f.exploration.starts); assertEquals(0, f.approach.starts);
    }
    @Test void explorationBudgetReadsFreshRootBeforeInputCommit() {
        Fixture f = new Fixture("report"); f.tick(0);
        f.exploration.tickHook = () -> { f.owns.set(false); assertFalse(f.exploration.budget.getAsBoolean()); }; f.tick(1);
        assertTrue(f.operation.retired()); assertNull(f.operation.outcome()); assertFalse(f.exploration.active);
    }
    @Test void cleanupReentrantRetirementCannotWriteLateOutcome() {
        Fixture f = new Fixture("report"); f.reads.found(80);
        f.exploration.cleanupHook = () -> { f.exploration.cleanupHook = () -> { }; f.operation.retireOwnedResources("stop"); };
        f.tick(0); assertTrue(f.operation.retired()); assertNull(f.operation.outcome()); assertFalse(f.events.contains("PARENT_OUTCOME"));
    }
    @Test void cleanupFailureStillAttemptsApproachReleaseAndNeverClaimsSuccess() {
        Fixture f = new Fixture("report"); f.reads.found(80); f.exploration.throwCleanup = true; f.approach.active = true; f.tick(0);
        assertTrue(f.approach.cleanupObserved); assertFalse(f.approach.active); assertNull(f.operation.outcome()); assertTrue(f.operation.retired());
    }
    @Test void missingCleanupProofRemainsUnclassified() {
        Fixture f = new Fixture("report"); f.reads.found(80); f.exploration.quietFailure = true; f.tick(0);
        assertTrue(f.operation.retired()); assertNull(f.operation.outcome()); assertFalse(f.operation.ownedQuiet());
    }
    @Test void nativeStepTimeoutsRetainTheirPhaseAndTimeoutMeaning() {
        Fixture explore = new Fixture("report"); explore.tick(0); explore.exploration.failure = "native_step_no_progress_timeout"; explore.tick(1);
        explore.assertReason("exploration_step_deadline_exhausted");
        Fixture approach = new Fixture("approach"); approach.reads.found(80); approach.tick(0);
        approach.approach.response = new FindApproachMovementPort.Step("native_step_no_progress_timeout", false, null); approach.tick(1);
        approach.assertReason("native_approach_step_deadline_exhausted");
    }
    @Test void nativePlanningBoundsAreNotRouteStartCountFailure() {
        for (String reason : List.of("native_planner_read_limit", "native_path_bounds_exhausted", "exploration_waypoint_work_limit")) {
            Fixture f = new Fixture("report"); f.tick(0); f.exploration.failure = reason; f.tick(1);
            f.assertReason("exploration_route_work_limit_exhausted");
        }
    }
    @Test void outputFailureDoesNotChangeDecisionOrCauseRetry() {
        Fixture f = new Fixture("report", limits(4, 40, 20, 8, 512, 1024), true); f.reads.found(80); f.tick(0);
        assertTrue(f.operation.outcome().satisfied()); f.assertReason("discovery_target_revalidated_and_exploration_quiet");
        assertEquals(1, f.reads.budgets.size()); assertEquals(0, f.exploration.starts);
    }
}
//#endif
