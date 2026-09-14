//#if MC == 12001
package lavi.minecraft.find.exploration.movement;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import lavi.minecraft.find.approach.input.FindOwnedMovementInputs;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.find.diagnostics.FindLog;
import lavi.minecraft.find.observation.FindObservationPort;
import lavi.minecraft.integration.input.lease.ForcedInputLeaseChannel;
import lavi.minecraft.integration.input.lease.ForcedInputLeaseLedger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Run the real route-generation owner and real input lease ledger at reentrant native/effect boundaries.
class MinecraftFindExplorationMovementTest {
    static class Channel implements ForcedInputLeaseChannel {
        final ForcedInputLeaseLedger ledger = new ForcedInputLeaseLedger();
        final EnumMap<Input, Boolean> forced = new EnumMap<>(Input.class);
        int claims, writes;
        boolean failRelease;
        public boolean lavi$claimForcedInput(Input input,Object owner) {
            claims++; return ledger.claim(input,owner,forced.getOrDefault(input,false));
        }
        public boolean lavi$writeForcedInput(Input input,Object owner,boolean down) {
            if (!ledger.owns(input,owner)) return false;
            writes++; forced.put(input,down); return true;
        }
        public boolean lavi$releaseForcedInput(Input input,Object owner) {
            if (failRelease || !lavi$writeForcedInput(input,owner,false)) return false;
            return ledger.retire(input,owner);
        }
        public boolean lavi$ownsForcedInput(Input input,Object owner) { return ledger.owns(input,owner); }
        void external(Input input,boolean down) { ledger.externalWrite(input); forced.put(input,down); }
    }
    static class Native implements FindExplorationNativeSession {
        final Channel channel = new Channel();
        Vec3d position = new Vec3d(.5,64,.5);
        BlockPos block = new BlockPos(0,64,0), goal;
        FindObservationPort.Binding local;
        boolean active, valid = true, supported = true, contended, free = true, safe = true;
        int starts, ticks, looks, releases, completedSteps;
        Runnable nativeHook = () -> {}, lookHook = () -> {};
        Decision next = new Decision(null,Map.of(Input.MOVE_FORWARD,true),new Rotation(90,0),false);
        double velocity;
        public boolean matches(FindObservationPort.Binding binding) { return valid; }
        public boolean supportedPlayer() { return supported; }
        public boolean contended() { return contended; }
        public boolean safeWaypoint(BlockPos goal) { return safe; }
        public boolean movementKeysFree(FindOwnedMovementInputs inputs) { return free; }
        public BlockPos playerBlock() { return block; }
        public Vec3d playerPosition() { return position; }
        public double horizontalVelocitySquared() { return velocity; }
        public ForcedInputLeaseChannel inputChannel() { return channel; }
        public void start(FindObservationPort.Binding local,BlockPos start,BlockPos goal) {
            starts++; active = true; this.local = local; this.goal = goal;
        }
        public Decision tick(BooleanSupplier withinBudget) { ticks++; nativeHook.run(); return next; }
        public void look(Rotation rotation) { looks++; lookHook.run(); }
        public void suspend() { releases++; active = false; }
        public boolean quiet() { return !active; }
        public FindExplorationMovementPort.Progress progress() {
            return new FindExplorationMovementPort.Progress(completedSteps,5,16,false);
        }
        void arrive() { block = goal; position = new Vec3d(goal.getX()+.5,goal.getY(),goal.getZ()+.5); }
    }
    final FindObservationPort.Binding admission = new FindObservationPort.Binding(new Object(),new Object(),
            "minecraft:overworld",.5,64,.5,-64,320);
    FindLog log() { return new FindLog("explore-test",(event,values) -> {}); }
    MinecraftFindExplorationMovement movement(Native nativeSession) {
        return new MinecraftFindExplorationMovement(nativeSession,log());
    }
    @Test void NoTickBeforeExplicitBeginOrAfterSuspendCanPerformNativeLookOrInputWork() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        assertTrue(movement.tick().quiet()); assertEquals(0,nativeSession.ticks);
        movement.begin(admission,() -> true); assertEquals(1,nativeSession.starts);
        movement.tick(); assertTrue(nativeSession.channel.forced.get(Input.MOVE_FORWARD));
        movement.suspend(); assertTrue(movement.quiet());
        assertFalse(nativeSession.channel.forced.get(Input.MOVE_FORWARD));
        int ticks = nativeSession.ticks, looks = nativeSession.looks, claims = nativeSession.channel.claims;
        for(int index=0;index<3;index++) assertTrue(movement.tick().quiet());
        assertEquals(ticks,nativeSession.ticks); assertEquals(looks,nativeSession.looks);
        assertEquals(claims,nativeSession.channel.claims);
    }
    @Test void AlreadyExpiredAdmissionCreatesNoNativeRouteAndCannotBeImplicitlyRenewed() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,() -> false);
        var result = movement.tick();
        assertEquals("elapsed_budget_exhausted",result.reason()); assertTrue(result.quiet());
        assertEquals(0,nativeSession.starts); assertEquals(0,nativeSession.channel.claims);
        assertEquals(0,nativeSession.ticks);
    }
    @Test void ReentrantRetirementDuringNativeDecisionDropsAllLateStagedEffectsAndDoesNotResumeItself() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,() -> true);
        nativeSession.nativeHook = movement::suspend;
        var result = movement.tick();
        assertNull(result.reason()); assertTrue(result.quiet()); assertFalse(result.waypointReached());
        assertEquals(0,nativeSession.looks); assertEquals(0,nativeSession.channel.claims);
        movement.tick(); assertEquals(1,nativeSession.ticks);
    }
    @Test void DeadlineExpirationInsideNativeCallOrAfterLookPreventsLateInputClaims() {
        var allowed = new AtomicBoolean(true);
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,allowed::get);
        nativeSession.nativeHook = () -> allowed.set(false);
        var expired = movement.tick();
        assertEquals("elapsed_budget_exhausted",expired.reason()); assertTrue(expired.quiet());
        assertEquals(0,nativeSession.looks); assertEquals(0,nativeSession.channel.claims);
        allowed.set(true);
        var other = new Native(); var afterLook = movement(other);
        afterLook.begin(admission,allowed::get);
        other.lookHook = () -> allowed.set(false);
        assertEquals("elapsed_budget_exhausted",afterLook.tick().reason());
        assertEquals(1,other.looks); assertEquals(0,other.channel.claims); assertTrue(afterLook.quiet());
    }
    @Test void BindingLossInsideNativeCallPreventsLateInputAndKeepsTheBindingReason() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,() -> true);
        nativeSession.nativeHook = () -> nativeSession.valid = false;
        var result = movement.tick();
        assertEquals("world_or_player_binding_changed",result.reason()); assertTrue(result.quiet());
        assertEquals(0,nativeSession.looks); assertEquals(0,nativeSession.channel.claims);
    }
    @Test void ForbiddenAttackOrJumpRequestIsRejectedBeforeAnyMovementAndDefenseInputSurvives() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        nativeSession.channel.external(Input.CLICK_RIGHT,true);
        movement.begin(admission,() -> true);
        nativeSession.next = new FindExplorationNativeSession.Decision(null,
                Map.of(Input.MOVE_FORWARD,true,Input.CLICK_LEFT,true,Input.JUMP,true),null,false);
        var rejected = movement.tick();
        assertEquals("native_step_requested_forbidden_input",rejected.reason());
        assertEquals(0,nativeSession.channel.claims);
        assertTrue(nativeSession.channel.forced.get(Input.CLICK_RIGHT));
        assertFalse(nativeSession.channel.forced.getOrDefault(Input.CLICK_LEFT,false));
        assertTrue(rejected.quiet());
    }
    @Test void SupersedingNativeWriterKeepsItsMovementValueWhenExplorationRetires() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,() -> true); movement.tick();
        nativeSession.channel.external(Input.MOVE_FORWARD,true);
        var rejected = movement.tick();
        assertEquals("forced_input_lease_rejected_or_superseded",rejected.reason());
        assertTrue(nativeSession.channel.forced.get(Input.MOVE_FORWARD));
        assertTrue(movement.quiet());
    }
    @Test void FailedOwnLeaseReleaseCannotFabricateQuiescenceOrAdmitAnotherRoute() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,() -> true); movement.tick();
        nativeSession.channel.failRelease = true;
        movement.suspend();
        assertFalse(movement.quiet()); assertFalse(movement.tick().quiet());
        assertThrows(IllegalStateException.class,() -> movement.begin(admission,() -> true));
        nativeSession.channel.failRelease = false;
        movement.suspend(); assertTrue(movement.quiet());
    }
    @Test void VerifiedArrivalRequiresTwoSlowDryTicksAndIsPublishedOnlyOnce() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,() -> true); nativeSession.arrive();
        assertFalse(movement.tick().waypointReached());
        nativeSession.velocity = .1; assertFalse(movement.tick().waypointReached());
        nativeSession.velocity = 0; assertFalse(movement.tick().waypointReached());
        var result = movement.tick();
        assertTrue(result.waypointReached()); assertTrue(result.quiet()); assertNull(result.reason());
        assertFalse(movement.tick().waypointReached());
        assertEquals(0,nativeSession.ticks);
    }
    @Test void ResumeAdmitsOneFreshLocalPlanAndRetainsVisitedGoalsWithoutRebasingAdmission() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,() -> true);
        BlockPos firstGoal = nativeSession.goal;
        assertTrue(movement.tick().progress().movementInputIssued());
        movement.suspend();
        nativeSession.position = new Vec3d(80.5,64,.5); nativeSession.block = new BlockPos(80,64,0);
        nativeSession.next = FindExplorationNativeSession.Decision.waiting();
        movement.begin(admission,() -> true);
        assertFalse(movement.tick().progress().movementInputIssued());
        assertEquals(2,nativeSession.starts); assertEquals(80.5,nativeSession.local.x());
        assertSame(admission.world(),nativeSession.local.world());
        assertNotEquals(firstGoal,nativeSession.goal);
        assertThrows(IllegalStateException.class,() -> movement.begin(admission,() -> true));
    }
    @Test void InvalidBindingAndOutOfAdmissionScopePreventAnyNewNativePlanOrInput() {
        var nativeSession = new Native(); nativeSession.valid = false;
        var movement = movement(nativeSession); movement.begin(admission,() -> true);
        assertEquals("world_or_player_binding_changed",movement.tick().reason());
        assertEquals(0,nativeSession.starts); assertEquals(0,nativeSession.channel.claims);
        var other = new Native(); other.position = new Vec3d(513.5,64,.5);
        other.block = new BlockPos(513,64,0);
        var outside = movement(other); outside.begin(admission,() -> true);
        assertEquals("exploration_displacement_limit_exhausted",outside.tick().reason());
        assertEquals(0,other.starts); assertTrue(outside.quiet());
    }
    @Test void SuspendedContendedTicksReleaseOnlyOwnInputsAndDoNotRunNativeOrReplan() {
        var nativeSession = new Native(); var movement = movement(nativeSession);
        movement.begin(admission,() -> true); movement.tick();
        nativeSession.channel.external(Input.CLICK_RIGHT,true); nativeSession.contended = true;
        int ticks = nativeSession.ticks;
        movement.tick();
        assertEquals(ticks,nativeSession.ticks); assertEquals(1,nativeSession.starts);
        assertFalse(nativeSession.channel.forced.get(Input.MOVE_FORWARD));
        assertTrue(nativeSession.channel.forced.get(Input.CLICK_RIGHT));
        movement.suspend(); assertTrue(movement.quiet());
    }
    @Test void LoggingAndProgressSnapshotFailureCannotChangeArrivalOrOwnedCleanup() {
        var nativeSession = new Native() {
            public FindExplorationMovementPort.Progress progress() { throw new IllegalStateException("diagnostic_only"); }
        };
        var movement = new MinecraftFindExplorationMovement(nativeSession,
                new FindLog("explore-test",(event,values) -> { throw new IllegalStateException("sink_only"); }));
        movement.begin(admission,() -> true); nativeSession.arrive(); movement.tick();
        var result = movement.tick();
        assertTrue(result.waypointReached()); assertTrue(result.quiet());
        assertEquals(-1,result.progress().completedSteps()); assertNull(result.reason());
    }
    @Test void RealBoundaryOutputContainsRouteGoalAndCleanupEvidenceWithoutPrivateIdentity() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
        ChatClefDiagnostics.setBoundaryEnabled(true);
        PrintStream previous = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        var nativeSession = new Native();
        FindLog trace = new FindLog("00000000-0000-0000-0000-000000000010");
        var movement = new MinecraftFindExplorationMovement(nativeSession,trace);
        try (PrintStream capture = new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            System.setOut(capture);
            try {
                movement.begin(admission,() -> true); movement.tick(); movement.suspend();
                String output = bytes.toString(StandardCharsets.UTF_8);
                assertTrue(output.contains("event=FIND_EXPLORATION_ROUTE_STARTED"),output);
                String goal = output.lines().filter(line -> line.contains("event=FIND_EXPLORATION_WAYPOINT_SELECTED"))
                        .findFirst().orElseThrow();
                for(String required : List.of("candidatesVisited=16","minimumTravel=16.0","maximumLocalRadius=48",
                        "waypointX=","waypointY=64","waypointZ=","routeGeneration="))
                    assertTrue(goal.contains(required),goal);
                String cleanup = output.lines().filter(line -> line.contains("event=FIND_EXPLORATION_MOVEMENT_SUSPENDED"))
                        .findFirst().orElseThrow();
                assertTrue(cleanup.contains("nativeSessionQuiet=true"),cleanup);
                assertTrue(cleanup.contains("ownedInputsQuiet=true"),cleanup);
                assertTrue(output.contains("nativeWorkerSubmitted=false"),output);
                assertFalse(output.contains("stableSortKey"),output); assertFalse(output.contains("PrivateSteve"),output);
                assertTrue(movement.quiet());
            } finally {
                try { movement.suspend(); }
                finally { trace.close("movement_test_scope_retired"); }
                assertEquals(0,ChatClefDiagnostics.diagnosticSessionSnapshot().emissionPending());
                assertEquals(0,ChatClefDiagnostics.diagnosticSessionSnapshot().emissionInProgress());
            }
        } finally {
            System.setOut(previous);
            ChatClefDiagnostics.setBoundaryEnabled(false);
        }
    }
}
//#endif
