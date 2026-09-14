//#if MC == 12001
package lavi.minecraft.find.observation.entity;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lavi.minecraft.find.diagnostics.FindLog;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.find.model.FindCandidate;
import lavi.minecraft.find.model.FindRequest;
import lavi.minecraft.find.observation.FindObservationPort.Binding;
import lavi.minecraft.find.observation.FindObservationPort.EntityScan;
import lavi.minecraft.find.observation.MinecraftFindObservationPort;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Test production observation filtering and real iterator limits, separately from live Minecraft.
class MinecraftFindEntityObservationTest {
    private static FindRequest request(String kind, String target, String mode) {
        return new FindRequest(kind, target, mode, "", 0);
    }
    private static FindRequest report() { return request("entity", "minecraft:villager", "report"); }
    private static FindRequest approach() { return request("entity", "minecraft:villager", "approach"); }
    private static List<FindRequest> mobRequests() { return List.of(report(), approach()); }
    private static FindLog log() { return new FindLog("entity-observation-test", (event, fields) -> { }); }
    private static MinecraftFindEntityObservation observation(FindEntityObservationFixture fixture) {
        return new MinecraftFindEntityObservation(fixture.client, () -> fixture.tracker);
    }
    private static EntityScan scan(FindEntityObservationFixture fixture, FindRequest request) {
        return observation(fixture).scan(request, fixture.binding(), 4096, () -> true, log());
    }

    @Test void bothEntityModesObserveAndLiveRevalidateBeyond64WithoutReachabilityFiltering() {
        try (var fixture = new FindEntityObservationFixture()) {
            VillagerEntity resident = fixture.villager(80, 0, 0);
            for (var request : mobRequests()) {
                EntityScan result = scan(fixture, request);
                assertTrue(result.complete()); assertEquals(1, result.visited()); assertEquals(1, result.matched());
                assertNotNull(result.nearest()); assertEquals(80, result.nearest().x());
                assertEquals(6400, result.nearest().distanceSquared());
                FindCandidate current = observation(fixture).revalidate(request, fixture.binding(), result.nearest(), log());
                assertNotNull(current); assertEquals(resident.getId(), current.entityId());
            }
            assertEquals(0, fixture.tracker.refreshCalls); assertEquals(0, fixture.tracker.reachabilityCalls);
            assertEquals(2, fixture.tracker.getterCalls);
            assertTrue(resident.isAlive()); assertFalse(resident.isRemoved());
            assertEquals(80, resident.getX());
        }
    }

    @Test void publicPortDelegationUsesTheSameMobPolicyDuringScanAndFinalRevalidation() {
        try (var fixture = new FindEntityObservationFixture()) {
            fixture.villager(80, 0, 0);
            var port = new MinecraftFindObservationPort(fixture.client, () -> fixture.tracker);
            var result = port.scanEntities(approach(), fixture.binding(), 4096, () -> true, log(), "DISCOVERY_LOOP_OBSERVATION");
            assertEquals(1, result.matched());
            assertNotNull(port.revalidate(report(), fixture.binding(), result.nearest(), log()));
            assertNotNull(port.revalidate(approach(), fixture.binding(), result.nearest(), log(), "HANDOFF_REVALIDATION"));
            var expired = port.scanEntities(report(), fixture.binding(), 4096, () -> false, log());
            assertEquals("elapsed_budget_exhausted", expired.reason());
            assertEquals(1, fixture.world.nextCalls); // Expired delegation performs no second entity read.
        }
    }

    @Test void bothEntityModesLoseTheRadiusCutoffWhilePlayersAndDroppedItemsRetain64() {
        try (var fixture = new FindEntityObservationFixture()) {
            fixture.villager(80, 0, 0); fixture.player("Steve", 80, 0, 0); fixture.diamond(80, 0, 0);
            for (FindRequest request : List.of(request("player", "Steve", "report"), request("player", "Steve", "approach"),
                    request("item", "minecraft:diamond", "report"), request("item", "minecraft:diamond", "approach"))) {
                EntityScan result = scan(fixture, request);
                assertTrue(result.complete()); assertEquals(0, result.matched()); assertNull(result.nearest());
            }
            for (var request : mobRequests()) assertEquals(1, scan(fixture, request).matched());
        }
    }

    @Test void retained64RadiusIsInclusiveAndUsesThreeDimensionsForSelectionAndRevalidation() {
        for (var request : List.of(request("player", "Steve", "report"), request("player", "Steve", "approach"),
                request("item", "minecraft:diamond", "report"), request("item", "minecraft:diamond", "approach"))) {
            try (var fixture = new FindEntityObservationFixture()) {
                Entity target = request.kind().equals("player") ? fixture.player("Steve", 0, 64, 0) : fixture.diamond(0, 64, 0);
                var result = scan(fixture, request);
                assertEquals(1, result.matched());
                assertNotNull(observation(fixture).revalidate(request, fixture.binding(), result.nearest(), log()));
                target.setPosition(0, 65, 0);
                assertEquals(0, scan(fixture, request).matched());
                assertNull(observation(fixture).revalidate(request, fixture.binding(), result.nearest(), log()));
                target.setPosition(63, 0, 0);
                assertEquals(1, scan(fixture, request).matched());
                target.setPosition(64, 1, 0);
                assertEquals(0, scan(fixture, request).matched());
            }
        }
    }

    @Test void realSubclassIdLivenessAndDroppedStackFiltersDoNotTurnLoopVisitsIntoMatches() {
        try (var fixture = new FindEntityObservationFixture()) {
            var specialized = fixture.subclassVillager(10, 0, 0);
            var dead = fixture.villager(1, 0, 0); dead.setHealth(0);
            var removed = fixture.villager(2, 0, 0); removed.setRemoved(Entity.RemovalReason.DISCARDED);
            fixture.player("Steve", 3, 0, 0);
            fixture.diamond(4, 0, 0);
            fixture.world.members.add(fixture.self);
            var result = scan(fixture, report());
            assertEquals(6, result.visited()); assertEquals(1, result.matched());
            assertEquals(specialized.getId(), result.nearest().entityId());
            assertEquals(0, scan(fixture, request("entity", "minecraft:zombie", "report")).matched());
            var drop = scan(fixture, request("item", "minecraft:diamond", "report"));
            assertEquals(1, drop.matched());
            ((ItemEntity) fixture.world.byId.get(drop.nearest().entityId())).setStack(ItemStack.EMPTY);
            assertEquals(0, scan(fixture, request("item", "minecraft:diamond", "report")).matched());
            assertNull(observation(fixture).revalidate(request("item", "minecraft:diamond", "report"),
                    fixture.binding(), drop.nearest(), log()));
        }
    }

    @Test void frozenOriginAndUuidTieRuleSurvivePlayerMovementAndReversedSourceOrder() {
        try (var fixture = new FindEntityObservationFixture()) {
            var left = fixture.villager(-80, 0, 0); left.setUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            var right = fixture.villager(80, 0, 0); right.setUuid(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
            var binding = fixture.binding();
            // Change the live player's position while keeping the admission binding unchanged.
            lavi.minecraft.testsupport.TestObjects.setField(fixture.self, Entity.class, "pos", new net.minecraft.util.math.Vec3d(79, 0, 0));
            for (var request : mobRequests()) {
                var first = observation(fixture).scan(request, binding, 4096, () -> true, log());
                assertEquals(left.getId(), first.nearest().entityId());
                Collections.reverse(fixture.world.members);
                assertEquals(left.getId(), observation(fixture).scan(request, binding, 4096, () -> true, log()).nearest().entityId());
                assertEquals(6400, observation(fixture).revalidate(request, binding, first.nearest(), log()).distanceSquared());
            }
        }
    }

    @Test void reobservationRanksAgainstItsNewFrozenOriginWithoutA512CandidateCutoff() {
        try (var fixture = new FindEntityObservationFixture()) {
            var left = fixture.villager(100, 0, 0);
            var right = fixture.villager(600, 0, 0);
            var initial = fixture.binding();
            var local = new Binding(initial.world(), initial.player(), initial.dimension(), 590, 0, 0, initial.bottomY(), initial.topY());
            for (var request : mobRequests()) {
                var first = observation(fixture).scan(request, initial, 4096, () -> true, log());
                assertTrue(first.complete()); assertEquals(2, first.matched()); assertEquals(left.getId(), first.nearest().entityId());
                var next = observation(fixture).scan(request, local, 4096, () -> true, log(), "DISCOVERY_LOOP_OBSERVATION");
                assertTrue(next.complete()); assertEquals(2, next.matched()); assertEquals(right.getId(), next.nearest().entityId());
                assertEquals(100, next.nearest().distanceSquared());
                assertEquals(100, observation(fixture).revalidate(request, local, next.nearest(), log(), "HANDOFF_REVALIDATION").distanceSquared());
            }
        }
    }

    @Test void physicalVisitLimitDistinguishesComplete4096FromPartial4097WithoutReadingTheExtraEntity() {
        try (var fixture = new FindEntityObservationFixture()) {
            var resident = fixture.villager(80, 0, 0);
            fixture.members(Collections.nCopies(4096, resident));
            var complete = scan(fixture, report());
            assertTrue(complete.complete()); assertEquals(4096, complete.visited());
            assertEquals(4096, fixture.world.nextCalls); assertEquals(1, fixture.world.iteratorCalls);
            fixture.world.nextCalls = 0; fixture.world.iteratorCalls = 0;
            fixture.members(Collections.nCopies(4097, resident));
            var partial = scan(fixture, report());
            assertFalse(partial.complete()); assertEquals(4096, partial.visited());
            assertEquals("entity_visit_limit_exhausted", partial.reason());
            assertEquals(4096, fixture.world.nextCalls); assertEquals(1, fixture.world.iteratorCalls);
            assertNotNull(partial.nearest()); // The operation, not the scanner, rejects partial success.
        }
    }

    @Test void expiredBudgetCannotEvenAcquireTheTrackerSource() {
        try (var fixture = new FindEntityObservationFixture()) {
            fixture.villager(1, 0, 0);
            for (var request : mobRequests()) {
                var result = observation(fixture).scan(request, fixture.binding(), 4096, () -> false, log());
                assertFalse(result.complete()); assertEquals(0, result.visited()); assertNull(result.nearest());
                assertEquals("elapsed_budget_exhausted", result.reason());
            }
            assertEquals(0, fixture.tracker.getterCalls); assertEquals(0, fixture.world.nextCalls);
        }
    }

    @Test void budgetExpiryDuringEnumerationStopsActualReadsAndCannotMarkTheScopeComplete() {
        for (var request : mobRequests()) {
            try (var fixture = new FindEntityObservationFixture()) {
                var resident = fixture.villager(80, 0, 0);
                fixture.members(Collections.nCopies(10, resident));
                var result = observation(fixture).scan(request, fixture.binding(), 4096,
                        () -> fixture.world.nextCalls < 2, log(), "DISCOVERY_LOOP_OBSERVATION");
                assertFalse(result.complete()); assertEquals(2, result.visited()); assertEquals(2, fixture.world.nextCalls);
                assertEquals("elapsed_budget_exhausted", result.reason()); assertNotNull(result.nearest());
                assertEquals(1, fixture.tracker.getterCalls);
            }
        }
    }

    @Test void smallerRemainingVisitBudgetIsAppliedToActualReadsWithoutResetOrAnExtraEntityRead() {
        for (var request : mobRequests()) {
            try (var fixture = new FindEntityObservationFixture()) {
                var resident = fixture.villager(80, 0, 0);
                fixture.members(Collections.nCopies(3, resident));
                var partial = observation(fixture).scan(request, fixture.binding(), 2, () -> true, log(), "DISCOVERY_LOOP_OBSERVATION");
                assertFalse(partial.complete()); assertEquals(2, partial.visited()); assertEquals(2, fixture.world.nextCalls);
                assertEquals("entity_visit_limit_exhausted", partial.reason()); assertNotNull(partial.nearest());
                fixture.world.nextCalls = 0;
                fixture.members(Collections.nCopies(2, resident));
                var complete = observation(fixture).scan(request, fixture.binding(), 2, () -> true, log(), "DISCOVERY_LOOP_OBSERVATION");
                assertTrue(complete.complete()); assertEquals(2, complete.visited()); assertEquals(2, fixture.world.nextCalls);
                fixture.world.nextCalls = 0;
                var none = observation(fixture).scan(request, fixture.binding(), 0, () -> true, log(), "DISCOVERY_LOOP_OBSERVATION");
                assertFalse(none.complete()); assertEquals(0, none.visited()); assertEquals(0, fixture.world.nextCalls);
            }
        }
    }

    @Test void changedUuidIdMembershipAndWorldBindingCannotRevalidateACachedPosition() {
        for (var request : mobRequests()) {
            try (var fixture = new FindEntityObservationFixture()) {
                var resident = fixture.villager(80, 0, 0);
                var candidate = scan(fixture, request).nearest();
                resident.setPosition(90, 0, 0);
                assertEquals(90, observation(fixture).revalidate(request, fixture.binding(), candidate, log()).x());
                var originalUuid = resident.getUuid(); resident.setUuid(UUID.randomUUID());
                assertNull(observation(fixture).revalidate(request, fixture.binding(), candidate, log()));
                resident.setUuid(originalUuid); fixture.world.byId.clear();
                assertNull(observation(fixture).revalidate(request, fixture.binding(), candidate, log()));
                fixture.world.byId.put(resident.getId(), resident);
                var oldBinding = fixture.binding(); fixture.client.world = FindEntityObservationFixture.world();
                assertNull(observation(fixture).revalidate(request, oldBinding, candidate, log()));
            }
        }
    }

    @Test void bothModesRejectDeadRemovedAndChangedKindTargetsAtHandoff() {
        for (var request : mobRequests()) {
            try (var fixture = new FindEntityObservationFixture()) {
                var resident = fixture.villager(80, 0, 0);
                var candidate = scan(fixture, request).nearest();
                resident.setHealth(0);
                assertNull(observation(fixture).revalidate(request, fixture.binding(), candidate, log(), "HANDOFF_REVALIDATION"));
                resident.setHealth(20);
                assertNotNull(observation(fixture).revalidate(request, fixture.binding(), candidate, log(), "HANDOFF_REVALIDATION"));
                var wrongKind = fixture.player("Steve", 80, 0, 0);
                fixture.world.byId.put(candidate.entityId(), wrongKind);
                assertNull(observation(fixture).revalidate(request, fixture.binding(), candidate, log(), "HANDOFF_REVALIDATION"));
                fixture.world.byId.put(candidate.entityId(), resident);
                resident.setRemoved(Entity.RemovalReason.DISCARDED);
                assertNull(observation(fixture).revalidate(request, fixture.binding(), candidate, log(), "HANDOFF_REVALIDATION"));
            }
        }
    }

    @Test void everyExplicitObservationBorrowsCurrentMembershipInsteadOfAStaleClassBucket() {
        try (var fixture = new FindEntityObservationFixture()) {
            fixture.villager(80, 0, 0);
            assertEquals(1, scan(fixture, report()).matched());
            fixture.members(List.of());
            var result = scan(fixture, report());
            assertTrue(result.complete()); assertEquals(0, result.visited()); assertEquals(0, result.matched());
            assertEquals(2, fixture.tracker.getterCalls); assertEquals(0, fixture.tracker.refreshCalls);
        }
    }

    @Test void loggingFailureCannotChangeSelectedCandidateOrIterationBudget() {
        for (var request : mobRequests()) {
            try (var fixture = new FindEntityObservationFixture()) {
                var resident = fixture.villager(80, 0, 0);
                var broken = new FindLog("broken-log", (event, fields) -> { throw new IllegalStateException("sink_failed"); });
                var result = observation(fixture).scan(request, fixture.binding(), 4096, () -> true, broken, "DISCOVERY_LOOP_OBSERVATION");
                assertTrue(result.complete()); assertEquals(resident.getId(), result.nearest().entityId());
                assertEquals(1, fixture.world.nextCalls);
                assertNotNull(observation(fixture).revalidate(request, fixture.binding(), result.nearest(), broken, "HANDOFF_REVALIDATION"));
            }
        }
    }

    @Test void productionBoundaryOutputRetainsSelectionCountersSamplesAndMovedRevalidationWithoutPrivateIdentity() {
        try (var fixture = new FindEntityObservationFixture()) {
            var resident = fixture.villager(80, 0, 0);
            var privateUuid = resident.getUuid().toString();
            fixture.player("PrivateSteve", 2, 0, 0); fixture.diamond(3, 0, 0);
            ChatClefDiagnostics.setBoundaryEnabled(false);
            ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                FindLog log = new FindLog("00000000-0000-0000-0000-000000000001");
                var scanner = observation(fixture);
                var result = scanner.scan(report(), fixture.binding(), 4096, () -> true, log);
                resident.setPosition(90, 1, 2);
                var current = scanner.revalidate(report(), fixture.binding(), result.nearest(), log);
                assertNotNull(current); assertEquals(90, current.x());
                log.close("matching_task_finished");
                String output = bytes.toString(StandardCharsets.UTF_8);
                String query = output.lines().filter(line -> line.contains("event=FIND_ENTITY_QUERY_COMPLETED")).findFirst().orElseThrow();
                for (String required : List.of("scopeComplete=true", "visited=3", "matched=1", "excludedSelf=0",
                        "excludedDead=0", "excludedRemoved=0", "excludedKind=2", "excludedTargetId=0",
                        "excludedRadius=0", "rawIdCompared=3", "rawIdMatches=1", "rawIdUnavailable=0",
                        "rawIdNotEvaluated=0", "selectedLocalId=" + resident.getId(), "selectedX=80",
                        "selectedY=0", "selectedZ=0", "selectedDistanceSq=6400.0", "diagnosticValues=CAPTURED",
                        "sample0=", "sample1=", "sample2=", "distancePolicy=CLIENT_OBSERVABLE_NO_RADIUS")) {
                    assertTrue(query.contains(required), required + " missing from: " + query);
                }
                String revalidation = output.lines().filter(line -> line.contains("event=FIND_ENTITY_REVALIDATION_RESULT")).findFirst().orElseThrow();
                for (String required : List.of("selectedX=80", "currentX=90", "currentY=1", "currentZ=2",
                        "distanceSquared=8105.0", "revalidated=true")) assertTrue(revalidation.contains(required), revalidation);
                assertTrue(output.contains("event=FIND_ENTITY_QUERY_STARTED"), output);
                assertTrue(output.contains("event=FIND_TRACE_TERMINAL"), output);
                assertFalse(output.contains(privateUuid), output); assertFalse(output.contains("PrivateSteve"), output);
                assertFalse(output.contains("stableSortKey"), output);
            } finally {
                System.setOut(previous);
                ChatClefDiagnostics.setBoundaryEnabled(false);
            }
        }
    }

    @Test void productionBoundaryOutputSeparatesInitialLoopHandoffAndArrivalRolesForEntityApproach() {
        try (var fixture = new FindEntityObservationFixture()) {
            var resident = fixture.villager(80, 0, 0);
            ChatClefDiagnostics.setBoundaryEnabled(false);
            ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                FindLog log = new FindLog("00000000-0000-0000-0000-000000000003");
                var port = new MinecraftFindObservationPort(fixture.client, () -> fixture.tracker);
                var initial = port.scanEntities(approach(), fixture.binding(), 4096, () -> true, log, "INITIAL_OBSERVATION");
                var loop = port.scanEntities(approach(), fixture.binding(), 4096, () -> true, log, "DISCOVERY_LOOP_OBSERVATION");
                assertTrue(initial.complete()); assertTrue(loop.complete());
                assertEquals(initial.nearest(), loop.nearest()); assertEquals(2, fixture.world.nextCalls);
                assertNotNull(port.revalidate(approach(), fixture.binding(), loop.nearest(), log, "INITIAL_CANDIDATE_REVALIDATION"));
                resident.setPosition(90, 0, 0);
                assertNotNull(port.revalidate(approach(), fixture.binding(), loop.nearest(), log, "HANDOFF_REVALIDATION"));
                resident.setHealth(0);
                assertNull(port.revalidate(approach(), fixture.binding(), loop.nearest(), log, "ARRIVAL_REVALIDATION"));
                log.close("matching_task_finished");
                String output = bytes.toString(StandardCharsets.UTF_8);
                var completed = output.lines().filter(line -> line.contains("event=FIND_ENTITY_QUERY_COMPLETED")).toList();
                assertEquals(2, completed.size(), output);
                assertTrue(completed.get(0).contains("phaseRole=INITIAL_OBSERVATION"), output);
                assertTrue(completed.get(1).contains("phaseRole=DISCOVERY_LOOP_OBSERVATION"), output);
                for (String role : List.of("INITIAL_CANDIDATE_REVALIDATION", "HANDOFF_REVALIDATION", "ARRIVAL_REVALIDATION")) {
                    assertTrue(output.lines().anyMatch(line -> line.contains("event=FIND_ENTITY_REVALIDATION_RESULT")
                            && line.contains("phaseRole=" + role)), output);
                }
                assertTrue(output.contains("mode=approach"), output);
                assertTrue(output.contains("reason=selected_entity_dead"), output);
                assertFalse(output.contains(resident.getUuid().toString()), output);
            } finally {
                System.setOut(previous);
                ChatClefDiagnostics.setBoundaryEnabled(false);
            }
        }
    }

    @Test void rejectedNonMobDiagnosticGetterFailuresRemainUnavailableAndCannotFailTheQuery() {
        try (var fixture = new FindEntityObservationFixture()) {
            var resident = fixture.villager(80, 0, 0);
            fixture.world.members.add(new FaultyDiagnosticItem(fixture.world));
            List<Object[]> entries = new ArrayList<>();
            var log = new FindLog("diagnostic-only-failure", (event, fields) -> {
                if (event.equals("ENTITY_QUERY_COMPLETED")) entries.add(fields);
            });
            var result = observation(fixture).scan(report(), fixture.binding(), 4096, () -> true, log);
            assertTrue(result.complete()); assertEquals(2, result.visited()); assertEquals(1, result.matched());
            assertEquals(resident.getId(), result.nearest().entityId());
            assertEquals(1, entries.size());
            assertEquals(1, field(entries.get(0), "rawIdCompared"));
            assertEquals(1, field(entries.get(0), "rawIdUnavailable"));
            assertEquals(1, field(entries.get(0), "excludedKind"));
            assertTrue(String.valueOf(field(entries.get(0), "sample1")).contains("id=UNAVAILABLE;type=UNAVAILABLE;first=wrong_kind"));
        }
    }

    @Test void productionBoundaryReadFailureRetainsActualPrefixCountsAndMarksIncompleteDiagnosticsUnavailable() {
        try (var fixture = new FindEntityObservationFixture()) {
            var resident = fixture.villager(80, 0, 0);
            fixture.world.members.add(null);
            ChatClefDiagnostics.setBoundaryEnabled(false);
            ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                FindLog log = new FindLog("00000000-0000-0000-0000-000000000002");
                var result = observation(fixture).scan(report(), fixture.binding(), 4096, () -> true, log);
                assertFalse(result.complete()); assertEquals("entity_read_failed", result.reason());
                assertEquals(2, result.visited()); assertEquals(1, result.matched());
                assertEquals(2, fixture.world.nextCalls); assertEquals(resident.getId(), result.nearest().entityId());
                log.close("matching_task_finished");
                String output = bytes.toString(StandardCharsets.UTF_8);
                String query = output.lines().filter(line -> line.contains("event=FIND_ENTITY_QUERY_COMPLETED")).findFirst().orElseThrow();
                for (String required : List.of("reason=entity_read_failed", "failureStage=entity_liveness",
                        "failureType=java.lang.NullPointerException", "visited=2", "matched=1", "scopeComplete=false",
                        "excludedSelf=UNAVAILABLE", "excludedDead=UNAVAILABLE", "excludedRemoved=UNAVAILABLE",
                        "excludedKind=UNAVAILABLE", "excludedTargetId=UNAVAILABLE", "excludedRadius=UNAVAILABLE",
                        "rawIdCompared=UNAVAILABLE", "rawIdMatches=UNAVAILABLE", "rawIdUnavailable=UNAVAILABLE",
                        "rawIdNotEvaluated=UNAVAILABLE", "diagnosticValues=UNAVAILABLE",
                        "sample0=id%3D" + resident.getId(), "type%3Dminecraft:villager", "first%3Dmatched")) {
                    assertTrue(query.contains(required), required + " missing from: " + query);
                }
                assertTrue(output.contains("event=FIND_TRACE_TERMINAL"), output);
                assertFalse(output.contains(resident.getUuid().toString()), output);
            } finally {
                System.setOut(previous);
                ChatClefDiagnostics.setBoundaryEnabled(false);
            }
        }
    }

    private static Object field(Object[] values, String key) {
        for (int index = 0; index + 1 < values.length; index += 2) if (key.equals(values[index])) return values[index + 1];
        throw new AssertionError("Missing field: " + key);
    }

    private static final class FaultyDiagnosticItem extends ItemEntity {
        FaultyDiagnosticItem(World world) { super(world, 0, 0, 0, new ItemStack(Items.DIAMOND)); }
        @Override public EntityType<?> getType() { throw new IllegalStateException("diagnostic_type_unavailable"); }
        @Override public int getId() { throw new IllegalStateException("diagnostic_id_unavailable"); }
    }
}
//#endif
