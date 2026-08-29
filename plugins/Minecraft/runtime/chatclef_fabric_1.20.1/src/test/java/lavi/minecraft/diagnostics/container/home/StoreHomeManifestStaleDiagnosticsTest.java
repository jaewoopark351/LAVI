package lavi.minecraft.diagnostics.container.home;

import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventFormatter;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventText;
import lavi.minecraft.task.container.home.execution.HomeStorageManifestProgress;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.planning.HomeStorageDisposition;
import lavi.minecraft.task.container.home.planning.HomeStorageManifest;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreHomeManifestStaleDiagnosticsTest {
    @Test
    void appendsCommandContextAndEmitsExactlyOnce() {
        AtomicInteger contextCalls = new AtomicInteger();
        AtomicInteger eventCalls = new AtomicInteger();
        AtomicReference<Object[]> emittedRequired = new AtomicReference<>();
        AtomicReference<Object[]> emittedOptional = new AtomicReference<>();
        StoreHomeManifestStaleDiagnostics diagnostics =
                new StoreHomeManifestStaleDiagnostics(
                        42L,
                        () -> true,
                        () -> 105L,
                        fields -> {
                            contextCalls.incrementAndGet();
                            return append(fields, new Object[]{
                                    "commandContextAvailable", true,
                                    "commandRequestId", "request-7",
                                    "commandCorrelationId", "correlation-7"
                            });
                        },
                        (task, reason, required, optional) -> {
                            eventCalls.incrementAndGet();
                            emittedRequired.set(required);
                            emittedOptional.set(optional);
                        }
                );

        diagnostics.emitPrepared(null, event());
        diagnostics.emitPrepared(null, event());

        assertEquals(1, contextCalls.get());
        assertEquals(1, eventCalls.get());
        assertEquals(0, emittedRequired.get().length % 2);
        assertEquals(0, emittedOptional.get().length % 2);
        DiagnosticBoundedEventText encoded = DiagnosticBoundedEventFormatter.format(
                "ALTO CLEF: [LAVI ChatClefBoundary] ",
                new Object[]{"event", StoreHomeManifestStaleDiagnostics.EVENT_NAME},
                emittedRequired.get(),
                emittedOptional.get(),
                StoreHomeManifestStaleDiagnostics.MAX_EVENT_UTF8_BYTES
        );
        assertTrue(encoded.text().contains("failureStage=ROOT_MANIFEST_REVALIDATION"));
        assertTrue(encoded.text().contains("commandRequestId=request-7"));
        assertTrue(encoded.text().contains("expectedMetadataDigest=not_available"));
        assertTrue(encoded.text().contains("actualMetadataDigest=not_available"));
        assertTrue(encoded.text().contains("diagnosticCaptureStatus=partial"));
        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(encoded.text())
                <= StoreHomeManifestStaleDiagnostics.MAX_EVENT_UTF8_BYTES);
    }

    @Test
    void emitsNothingWhenBoundaryDiagnosticsAreDisabled() {
        AtomicInteger contextCalls = new AtomicInteger();
        AtomicInteger eventCalls = new AtomicInteger();
        StoreHomeManifestStaleDiagnostics diagnostics =
                new StoreHomeManifestStaleDiagnostics(
                        42L,
                        () -> false,
                        () -> 105L,
                        fields -> {
                            contextCalls.incrementAndGet();
                            return fields;
                        },
                        (task, reason, required, optional) -> eventCalls.incrementAndGet()
                );

        diagnostics.emitPrepared(null, event());

        assertEquals(0, contextCalls.get());
        assertEquals(0, eventCalls.get());
    }

    @Test
    void doesNotDedupeAcrossSeparateOperations() {
        AtomicInteger eventCalls = new AtomicInteger();
        StoreHomeManifestStaleDiagnostics first = diagnosticsFor(eventCalls, 1L);
        StoreHomeManifestStaleDiagnostics second = diagnosticsFor(eventCalls, 2L);

        first.emitPrepared(null, event());
        second.emitPrepared(null, event());

        assertEquals(2, eventCalls.get());
    }

    @Test
    void preservesPendingTransferValuesInTheImmutableEventFields() {
        StoreHomePendingTransferSnapshot pending =
                new StoreHomePendingTransferSnapshot(
                        true, "destination-2", 8, 63, 64, 12, 3
                );
        StoreHomeManifestStaleEventSnapshot event = event(pending);
        Object[] required = StoreHomeManifestStaleEventFields.required(event);
        Object[] optional = StoreHomeManifestStaleEventFields.optional(event);

        assertEquals(Boolean.TRUE, value(required, "transferPending"));
        assertEquals(8, value(required, "pendingLogicalSlot"));
        assertEquals("destination-2", value(optional, "pendingDestinationKey"));
        assertEquals(63, value(optional, "pendingSourceWindowSlot"));
        assertEquals(64, value(optional, "pendingSourceCountBefore"));
        assertEquals(12, value(optional, "pendingDestinationCountBefore"));
        assertEquals(3, value(optional, "pendingElapsedTicks"));
    }

    @Test
    void laterContainerSessionReplacesTheDiagnosticBaseline() {
        AtomicLong tick = new AtomicLong(100L);
        AtomicInteger eventCalls = new AtomicInteger();
        AtomicReference<Object[]> required = new AtomicReference<>();
        StoreHomeManifestStaleDiagnostics diagnostics =
                new StoreHomeManifestStaleDiagnostics(
                        42L,
                        () -> true,
                        tick::get,
                        fields -> fields,
                        (task, reason, requiredFields, optionalFields) -> {
                            eventCalls.incrementAndGet();
                            required.set(requiredFields);
                        }
                );
        HomeStorageManifestStep firstStep = step(7L);
        HomeStorageManifestStep secondStep = step(8L);
        diagnostics.captureSessionBaseline(
                1,
                plan(firstStep),
                StoreHomeHandlerSnapshot.unavailable("test_fixture"),
                "test-world",
                "OVERWORLD"
        );
        tick.set(104L);
        diagnostics.captureSessionBaseline(
                2,
                plan(secondStep),
                StoreHomeHandlerSnapshot.unavailable("test_fixture"),
                "test-world",
                "OVERWORLD"
        );
        tick.set(105L);

        diagnostics.recordExecutorStale(
                null,
                StoreHomePhase.TRANSFER_EXACT_SLOTS,
                "exact_source_changed_before_click",
                new HomeStorageManifestProgress(
                        new HomeStorageManifest(8L, List.of(secondStep))
                ),
                secondStep,
                null
        );

        assertEquals(1, eventCalls.get());
        assertEquals(2, value(required.get(), "containerSessionOrdinal"));
        assertEquals(8L, value(required.get(), "planRevision"));
        assertEquals(104L, value(required.get(), "planCapturedClientTickId"));
    }

    @Test
    void disabledSessionCaptureInvalidatesThePreviousBaseline() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        AtomicLong tick = new AtomicLong(100L);
        AtomicReference<Object[]> required = new AtomicReference<>();
        StoreHomeManifestStaleDiagnostics diagnostics =
                new StoreHomeManifestStaleDiagnostics(
                        42L,
                        enabled::get,
                        tick::get,
                        fields -> fields,
                        (task, reason, requiredFields, optionalFields) ->
                                required.set(requiredFields)
                );
        HomeStorageManifestStep firstStep = step(7L);
        HomeStorageManifestStep secondStep = step(8L);
        diagnostics.captureSessionBaseline(
                1,
                plan(firstStep),
                StoreHomeHandlerSnapshot.unavailable("test_fixture"),
                "test-world",
                "OVERWORLD"
        );
        enabled.set(false);
        diagnostics.captureSessionBaseline(
                2,
                plan(secondStep),
                StoreHomeHandlerSnapshot.unavailable("test_fixture"),
                "test-world",
                "OVERWORLD"
        );
        enabled.set(true);
        tick.set(105L);

        diagnostics.recordExecutorStale(
                null,
                StoreHomePhase.TRANSFER_EXACT_SLOTS,
                "exact_source_changed_before_click",
                new HomeStorageManifestProgress(
                        new HomeStorageManifest(8L, List.of(secondStep))
                ),
                secondStep,
                null
        );

        assertEquals(
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                value(required.get(), "containerSessionOrdinal")
        );
        assertEquals(8L, value(required.get(), "planRevision"));
        assertEquals(
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                value(required.get(), "planCapturedClientTickId")
        );
    }

    private static StoreHomeManifestStaleEventSnapshot event() {
        return event(StoreHomePendingTransferSnapshot.none());
    }

    private static StoreHomeManifestStaleEventSnapshot event(
            StoreHomePendingTransferSnapshot pending) {
        return new StoreHomeManifestStaleEventSnapshot(
                42L,
                2,
                7L,
                100L,
                105L,
                "REVALIDATE_AFTER_RESUME",
                StoreHomeManifestFailureStage.ROOT_MANIFEST_REVALIDATION,
                "planned_fingerprint_changed",
                3,
                8,
                0,
                0,
                StoreHomeManifestMismatchSnapshot.unavailable("test_fixture"),
                pending,
                StoreHomeHandlerSnapshot.unavailable("test_fixture"),
                StoreHomeHandlerSnapshot.unavailable("test_fixture"),
                StoreHomeScreenSlotSnapshot.notObserved("test_fixture"),
                "test-world",
                "OVERWORLD",
                StoreHomeDestinationSnapshot.none()
        );
    }

    private static HomeStorageManifestStep step(long revision) {
        return new HomeStorageManifestStep(
                8,
                HomeStorageStackFingerprint.of("minecraft:redstone", 0, null),
                64,
                HomeStorageManifestStep.TransferMode.WHOLE_STACK_QUICK_MOVE,
                HomeStorageDisposition.STORE_HOME,
                "manual_home_surplus",
                revision
        );
    }

    private static HomeStoragePlan plan(HomeStorageManifestStep step) {
        return new HomeStoragePlan(
                step.loadoutPlanRevision(),
                List.of(),
                new HomeStorageManifest(
                        step.loadoutPlanRevision(), List.of(step)
                )
        );
    }

    private static Object[] append(Object[] left, Object[] right) {
        Object[] result = new Object[left.length + right.length];
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    private static StoreHomeManifestStaleDiagnostics diagnosticsFor(
            AtomicInteger eventCalls,
            long operationId) {
        return new StoreHomeManifestStaleDiagnostics(
                operationId,
                () -> true,
                () -> 105L,
                fields -> fields,
                (task, reason, required, optional) -> eventCalls.incrementAndGet()
        );
    }

    private static Object value(Object[] fields, String key) {
        for (int index = 0; index < fields.length; index += 2) {
            if (key.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field " + key);
    }
}
