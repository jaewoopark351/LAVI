package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState.RouteCandidateReconciliation;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState.RouteMovementObservation;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.fields;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.occurrences;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class StoreDepositRouteInvalidationContractTest {

    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    @DisplayName("scenario 11 [assertions 16-18]: invalidation, reconciliation, and unavailable provenance stay separate")
    void checkFalseInvalidationDoesNotCloseRouteChildOrReconstructProgressProvenance() throws IOException {
        StoreContainerRouteState route = new StoreContainerRouteState(0L);
        Object routeChild = new Object();
        BlockPos target = new BlockPos(4, 64, 8);
        route.stageRouteCandidate(routeChild, target, 3, true);
        RouteCandidateReconciliation installed = route.reconcileRouteCandidate(
                routeChild, false, true, routeChild
        );
        route.recordChildReconciliation("ROOT_ROUTE", null, routeChild, true, false);
        int replacementCountBeforeInvalidation = route.rootRouteChildReplacementCount();

        RouteMovementObservation invalidated = route.recordMovementObservation(
                target, "MOVEMENT_PROGRESS_FAILED", true, false, true
        );

        assertEquals("CANDIDATE_INSTALLED", installed.outcome());
        assertEquals(1L, invalidated.progressCheckInvocationId());
        assertTrue(invalidated.progressCheckEvaluated());
        assertFalse(invalidated.progressCheckOk());
        assertEquals(1L, invalidated.selectedCandidateGenerationBefore());
        assertEquals(0L, invalidated.selectedCandidateGenerationAfter());
        assertEquals(replacementCountBeforeInvalidation, route.rootRouteChildReplacementCount());
        assertTrue(route.isCurrentRouteChild(routeChild));

        route.recordChildReconciliation("ROOT_ROUTE", routeChild, null, true, true);
        assertFalse(route.isCurrentRouteChild(routeChild));
        assertEquals(replacementCountBeforeInvalidation + 1, route.rootRouteChildReplacementCount());

        String depositAll = source("src/main/java/adris/altoclef/tasks/container/DepositAllTask.java");
        String movement = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/route/StoreDepositMovementDiagnostics.java"
        );
        assertEquals(1, occurrences(depositAll, "_progressChecker.check(mod)"));
        assertEquals(1, occurrences(depositAll, "StoreDepositDiagnostics.observeAutomaticMovementResult("));
        assertFalse(movement.contains("_progressChecker"));
        assertFalse(movement.contains(".check("));
        assertFalse(movement.contains(".reset("));
        assertFalse(movement.contains("setProgress("));
        assertTrue(movement.contains("\"progressMode\", \"UNAVAILABLE\""));
        assertTrue(movement.contains("\"progressBaseline\", \"UNAVAILABLE\""));
        assertTrue(movement.contains("\"progressElapsed\", \"UNAVAILABLE\""));
        assertTrue(movement.contains("PROGRESS_MODE,BASELINE,ELAPSED"));
        assertFalse(movement.contains("String semanticKey = invocationId"));

        Task autoRoot = new TestTask("auto-root");
        StoreDepositOperationState autoState = new StoreDepositOperationState(
                new StoreDepositOperationContext("auto-store", "AUTO_DEPOSIT_ALL_CHAIN", autoRoot, 0L, 0L)
        );
        autoState.attachAutomaticContext(new StoreDepositAutomaticContext(
                true, 1L, 1L, "auto-1", "maintenance-1", "child-1", 0, "pressure-1"
        ));
        StoreContainerRouteState autoRoute = autoState.routeState();
        Task firstRouteChild = new TestTask("route-1");
        Task nextRouteChild = new TestTask("route-2");
        autoRoute.stageRouteCandidate(firstRouteChild, target, 1, true);
        autoRoute.reconcileRouteCandidate(firstRouteChild, false, true, firstRouteChild);
        autoRoute.recordChildReconciliation("ROOT_ROUTE", null, firstRouteChild, true, false);
        autoRoute.stageRouteCandidate(nextRouteChild, target.add(1, 0, 0), 2, true);
        Map<String, Object> activeRouteIdentity = fields(
                StoreDepositEventFields.activeRouteIdentityFields(autoState)
        );
        assertEquals("auto-store-candidate-1", activeRouteIdentity.get("selectedCandidateGenerationId"));
        assertEquals("auto-store-attempt-1", activeRouteIdentity.get("storeAttemptId"));
        assertEquals("auto-store-route-child-1", activeRouteIdentity.get("routeChildLifecycleId"));
    }
}
