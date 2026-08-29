package lavi.minecraft.task.container.home.execution.task.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeOperationNoProgressCheckpoint;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeTimeoutAction;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeTimeoutDecision;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeTimeoutEvaluationOrder;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeTimeoutResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Directly lock the production StoreHomeTask lifecycle decision order.
class StoreHomeTaskLifecycleControllerDecisionOrderTest {
    @Test
    void activeRootAndSafetyChecksAlwaysRunBeforeBehavior() {
        List<String> events = new ArrayList<>();
        Task expected = new DummyTask("safety-sequence-result");

        Task actual = StoreHomeTaskLifecycleController.runSafetySequence(
                event(events, "active_root_tick"),
                decision(events, "pending_ownership", false),
                decision(events, "context", false),
                decision(events, "cursor", false),
                decision(events, "emergency_hard_cap", false),
                () -> {
                    events.add("behavior");
                    return expected;
                }
        );

        assertEquals(List.of(
                "active_root_tick",
                "pending_ownership",
                "context",
                "cursor",
                "emergency_hard_cap",
                "behavior"
        ), events);
        assertSame(expected, actual);
    }

    @Test
    void everySafetyStopTransitionsOnceAndSuppressesLaterBehavior() {
        List<String> guards = List.of(
                "pending_ownership",
                "context",
                "cursor",
                "emergency_hard_cap"
        );

        for (int stopIndex = 0; stopIndex < guards.size(); stopIndex++) {
            List<String> events = new ArrayList<>();
            AtomicInteger terminalTransitions = new AtomicInteger();
            AtomicInteger behaviorCalls = new AtomicInteger();

            Task result = StoreHomeTaskLifecycleController.runSafetySequence(
                    event(events, "active_root_tick"),
                    stoppingDecision(
                            events,
                            "pending_ownership",
                            stopIndex == 0,
                            terminalTransitions
                    ),
                    stoppingDecision(
                            events,
                            "context",
                            stopIndex == 1,
                            terminalTransitions
                    ),
                    stoppingDecision(
                            events,
                            "cursor",
                            stopIndex == 2,
                            terminalTransitions
                    ),
                    stoppingDecision(
                            events,
                            "emergency_hard_cap",
                            stopIndex == 3,
                            terminalTransitions
                    ),
                    () -> {
                        behaviorCalls.incrementAndGet();
                        return new DummyTask("unreachable-behavior");
                    }
            );

            List<String> expected = new ArrayList<>();
            expected.add("active_root_tick");
            expected.addAll(guards.subList(0, stopIndex + 1));
            assertEquals(expected, events);
            assertEquals(1, terminalTransitions.get());
            assertEquals(0, behaviorCalls.get());
            assertNull(result);
        }
    }

    @Test
    void candidateObservationPrecedesExactActivationAndSuccessfulActivationRunsOnce() {
        List<String> events = new ArrayList<>();
        AtomicInteger activationAttempts = new AtomicInteger();
        AtomicInteger activationTransitions = new AtomicInteger();
        AtomicInteger fullStepCalls = new AtomicInteger();

        assertEquals(
                StoreHomeOperationNoProgressCheckpoint
                        .OBSERVE_POSITION_AND_TRY_EXACT_ACTIVATION_ONLY,
                StoreHomeTimeoutEvaluationOrder.operationNoProgressCheckpoint(
                        true, false, false
                )
        );

        boolean entryStopped =
                StoreHomeTaskLifecycleController.runCandidateEntrySequence(
                        decision(events, "operation_no_progress_before", false),
                        decision(events, "candidate_acquisition", false),
                        event(events, "candidate_position_observation")
                );
        boolean boundaryStopped = entryStopped
                || StoreHomeTaskLifecycleController.runNoProgressActivationSequence(
                        () -> {
                            events.add("exact_activation");
                            activationAttempts.incrementAndGet();
                            activationTransitions.incrementAndGet();
                            return true;
                        },
                        decision(events, "root_pending", true),
                        decision(events, "operation_no_progress_after", false),
                        event(events, "activation_progress")
                );
        if (!boundaryStopped) {
            fullStepCalls.incrementAndGet();
        }

        assertEquals(List.of(
                "operation_no_progress_before",
                "candidate_acquisition",
                "candidate_position_observation",
                "exact_activation",
                "root_pending",
                "operation_no_progress_after",
                "activation_progress"
        ), events);
        assertTrue(boundaryStopped);
        assertEquals(1, activationAttempts.get());
        assertEquals(1, activationTransitions.get());
        assertEquals(0, fullStepCalls.get());
    }

    @Test
    void failedExactActivationAtNoProgressBoundaryNeverRunsAFullStep() {
        List<String> events = new ArrayList<>();
        AtomicInteger activationAttempts = new AtomicInteger();
        AtomicInteger terminalTransitions = new AtomicInteger();
        AtomicInteger progressCalls = new AtomicInteger();
        AtomicInteger fullStepCalls = new AtomicInteger();

        boolean boundaryStopped = StoreHomeTaskLifecycleController
                .runNoProgressActivationSequence(
                        () -> {
                            events.add("exact_activation");
                            activationAttempts.incrementAndGet();
                            return false;
                        },
                        decision(events, "root_pending", true),
                        () -> {
                            events.add("operation_no_progress_after");
                            terminalTransitions.incrementAndGet();
                            return true;
                        },
                        () -> progressCalls.incrementAndGet()
                );
        if (!boundaryStopped) {
            fullStepCalls.incrementAndGet();
        }

        assertEquals(List.of(
                "exact_activation",
                "root_pending",
                "operation_no_progress_after"
        ), events);
        assertTrue(boundaryStopped);
        assertEquals(1, activationAttempts.get());
        assertEquals(1, terminalTransitions.get());
        assertEquals(0, progressCalls.get());
        assertEquals(0, fullStepCalls.get());
    }

    @Test
    void normalStepRunsBeforeOperationAndCandidateTimeoutAndPreservesTaskIdentity() {
        List<String> events = new ArrayList<>();
        Task expected = new DummyTask("normal-step-result");
        AtomicReference<Task> progressTask = new AtomicReference<>();

        boolean entryStopped =
                StoreHomeTaskLifecycleController.runCandidateEntrySequence(
                        decision(events, "operation_no_progress_before", false),
                        decision(events, "candidate_acquisition", false),
                        event(events, "candidate_position_observation")
                );
        Task actual = StoreHomeTaskLifecycleController.runNormalStepSequence(
                () -> {
                    events.add("navigation_or_session_step");
                    return expected;
                },
                decision(events, "root_pending", true),
                decision(events, "operation_no_progress", false),
                decision(events, "candidate_active", true),
                decision(events, "candidate_timeout", false),
                decision(events, "progress_eligible", true),
                nextTask -> {
                    events.add("progress_diagnostics");
                    progressTask.set(nextTask);
                }
        );

        assertFalse(entryStopped);
        assertEquals(List.of(
                "operation_no_progress_before",
                "candidate_acquisition",
                "candidate_position_observation",
                "navigation_or_session_step",
                "root_pending",
                "operation_no_progress",
                "candidate_active",
                "candidate_timeout",
                "progress_eligible",
                "progress_diagnostics"
        ), events);
        assertSame(expected, progressTask.get());
        assertSame(expected, actual);
    }

    @Test
    void candidateTimeoutRejectsExactlyOnceAfterOperationNoProgressCheck() {
        List<String> events = new ArrayList<>();
        AtomicInteger rejectionTransitions = new AtomicInteger();
        AtomicInteger progressCalls = new AtomicInteger();

        Task actual = StoreHomeTaskLifecycleController.runNormalStepSequence(
                () -> {
                    events.add("navigation_or_session_step");
                    return new DummyTask("rejected-step-result");
                },
                decision(events, "root_pending", true),
                decision(events, "operation_no_progress", false),
                decision(events, "candidate_active", true),
                () -> {
                    events.add("candidate_timeout_rejection");
                    rejectionTransitions.incrementAndGet();
                    return true;
                },
                () -> {
                    events.add("unexpected_progress_eligibility");
                    return true;
                },
                ignored -> progressCalls.incrementAndGet()
        );

        assertEquals(List.of(
                "navigation_or_session_step",
                "root_pending",
                "operation_no_progress",
                "candidate_active",
                "candidate_timeout_rejection"
        ), events);
        assertEquals(1, rejectionTransitions.get());
        assertEquals(0, progressCalls.get());
        assertNull(actual);
    }

    @Test
    void pendingOperationTimeoutTransitionsToTransferUnconfirmedExactlyOnce() {
        List<String> events = new ArrayList<>();
        AtomicInteger terminalTransitions = new AtomicInteger();
        AtomicInteger candidateAcquisitions = new AtomicInteger();
        AtomicInteger candidateObservations = new AtomicInteger();
        AtomicReference<StoreHomeResult> terminalResult = new AtomicReference<>();

        assertEquals(
                StoreHomeOperationNoProgressCheckpoint.RESOLVE_BEFORE_BEHAVIOR,
                StoreHomeTimeoutEvaluationOrder.operationNoProgressCheckpoint(
                        true, true, true
                )
        );

        boolean stopped =
                StoreHomeTaskLifecycleController.runCandidateEntrySequence(
                        () -> {
                            events.add("operation_no_progress_with_pending");
                            StoreHomeTimeoutDecision decision =
                                    StoreHomeTimeoutResolver.resolveOperation(
                                            true,
                                            Optional.of(
                                                    StoreHomeTimeoutReason
                                                            .OPERATION_NO_PROGRESS
                                            )
                                    ).orElseThrow();
                            assertEquals(
                                    StoreHomeTimeoutAction
                                            .FINISH_TRANSFER_UNCONFIRMED,
                                    decision.action()
                            );
                            terminalResult.set(
                                    StoreHomeResult.TRANSFER_UNCONFIRMED
                            );
                            terminalTransitions.incrementAndGet();
                            return true;
                        },
                        () -> {
                            candidateAcquisitions.incrementAndGet();
                            return false;
                        },
                        candidateObservations::incrementAndGet
                );

        assertTrue(stopped);
        assertEquals(
                List.of("operation_no_progress_with_pending"),
                events
        );
        assertEquals(StoreHomeResult.TRANSFER_UNCONFIRMED, terminalResult.get());
        assertEquals(1, terminalTransitions.get());
        assertEquals(0, candidateAcquisitions.get());
        assertEquals(0, candidateObservations.get());
    }

    @Test
    void rootTerminationAfterExactActivationSkipsTimeoutAndProgress() {
        List<String> events = new ArrayList<>();
        AtomicInteger timeoutCalls = new AtomicInteger();
        AtomicInteger progressCalls = new AtomicInteger();

        boolean stopped = StoreHomeTaskLifecycleController
                .runNoProgressActivationSequence(
                        decision(events, "exact_activation", true),
                        decision(events, "root_pending", false),
                        () -> {
                            timeoutCalls.incrementAndGet();
                            return true;
                        },
                        progressCalls::incrementAndGet
                );

        assertTrue(stopped);
        assertEquals(
                List.of("exact_activation", "root_pending"),
                events
        );
        assertEquals(0, timeoutCalls.get());
        assertEquals(0, progressCalls.get());
    }

    private static Runnable event(List<String> events, String value) {
        return () -> events.add(value);
    }

    private static BooleanSupplier decision(
            List<String> events,
            String value,
            boolean result) {
        return () -> {
            events.add(value);
            return result;
        };
    }

    private static BooleanSupplier stoppingDecision(
            List<String> events,
            String value,
            boolean stops,
            AtomicInteger terminalTransitions) {
        return () -> {
            events.add(value);
            if (stops) {
                terminalTransitions.incrementAndGet();
            }
            return stops;
        };
    }

    private static final class DummyTask extends Task {
        private final String description;

        private DummyTask(String description) {
            this.description = description;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return description;
        }
    }
}
