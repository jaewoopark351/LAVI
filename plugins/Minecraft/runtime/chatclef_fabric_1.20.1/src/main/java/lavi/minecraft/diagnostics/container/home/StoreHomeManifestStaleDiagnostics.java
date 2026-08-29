package lavi.minecraft.diagnostics.container.home;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.slot.StoreHomeScreenSlotSnapshotReader;
import lavi.minecraft.diagnostics.container.home.transfer.StoreHomePendingTransferSnapshotConverter;
import lavi.minecraft.diagnostics.container.home.transfer.StoreHomeTransferStaleSnapshotAssembler;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageManifestProgress;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.session.HomeStorageManifestValidation;
import lavi.minecraft.task.container.home.execution.transfer.failure.HomeStorageTransferFailureObservation;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import lavi.minecraft.task.container.home.planning.HomeStorageStackLocation;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;

//20260828_kpopmodder: Emit one bounded MANIFEST_STALE event per store-home operation.
public final class StoreHomeManifestStaleDiagnostics {
    static final String EVENT_NAME = "STORE_HOME_MANIFEST_STALE";
    static final int MAX_EVENT_UTF8_BYTES = 8 * 1024;

    private final long operationId;
    private final BooleanSupplier boundaryEnabled;
    private final LongSupplier clientTick;
    private final UnaryOperator<Object[]> commandContextAppender;
    private final EventSink eventSink;
    private final StoreHomePendingTransferSnapshotConverter pendingSnapshotConverter =
            new StoreHomePendingTransferSnapshotConverter();
    private final StoreHomeTransferStaleSnapshotAssembler transferSnapshotAssembler;
    private PlanBaseline baseline = PlanBaseline.unavailable();
    private boolean emitted;

    public StoreHomeManifestStaleDiagnostics() {
        this(
                ChatClefDiagnostics.nextOperationId(),
                ChatClefDiagnostics::isBoundaryEnabled,
                ChatClefDiagnostics::currentClientTickId,
                fields -> ChatClefDiagnostics.withCommandContextFields(fields),
                (task, reason, required, optional) ->
                        ChatClefDiagnostics.logBoundedBoundary(
                                EVENT_NAME,
                                reason,
                                task,
                                MAX_EVENT_UTF8_BYTES,
                                required,
                                optional
                        ),
                new StoreHomeTransferStaleSnapshotAssembler(
                        new HomeStorageScreenSlotResolver()
                )
        );
    }

    //20260828_kpopmodder: Share the behavior-owned operation identity with diagnostics.
    public StoreHomeManifestStaleDiagnostics(long operationId) {
        this(
                operationId,
                ChatClefDiagnostics::isBoundaryEnabled,
                ChatClefDiagnostics::currentClientTickId,
                fields -> ChatClefDiagnostics.withCommandContextFields(fields),
                (task, reason, required, optional) ->
                        ChatClefDiagnostics.logBoundedBoundary(
                                EVENT_NAME,
                                reason,
                                task,
                                MAX_EVENT_UTF8_BYTES,
                                required,
                                optional
                        ),
                new StoreHomeTransferStaleSnapshotAssembler(
                        new HomeStorageScreenSlotResolver()
                )
        );
    }

    public StoreHomeManifestStaleDiagnostics(
            long operationId,
            HomeStorageScreenSlotResolver slotResolver) {
        this(
                operationId,
                ChatClefDiagnostics::isBoundaryEnabled,
                ChatClefDiagnostics::currentClientTickId,
                fields -> ChatClefDiagnostics.withCommandContextFields(fields),
                (task, reason, required, optional) ->
                        ChatClefDiagnostics.logBoundedBoundary(
                                EVENT_NAME,
                                reason,
                                task,
                                MAX_EVENT_UTF8_BYTES,
                                required,
                                optional
                        ),
                new StoreHomeTransferStaleSnapshotAssembler(slotResolver)
        );
    }

    StoreHomeManifestStaleDiagnostics(
            long operationId,
            BooleanSupplier boundaryEnabled,
            LongSupplier clientTick,
            UnaryOperator<Object[]> commandContextAppender,
            EventSink eventSink) {
        this(
                operationId,
                boundaryEnabled,
                clientTick,
                commandContextAppender,
                eventSink,
                new StoreHomeTransferStaleSnapshotAssembler(
                        new HomeStorageScreenSlotResolver()
                )
        );
    }

    private StoreHomeManifestStaleDiagnostics(
            long operationId,
            BooleanSupplier boundaryEnabled,
            LongSupplier clientTick,
            UnaryOperator<Object[]> commandContextAppender,
            EventSink eventSink,
            StoreHomeTransferStaleSnapshotAssembler transferSnapshotAssembler) {
        this.operationId = operationId;
        this.boundaryEnabled = Objects.requireNonNull(boundaryEnabled, "boundaryEnabled");
        this.clientTick = Objects.requireNonNull(clientTick, "clientTick");
        this.commandContextAppender = Objects.requireNonNull(
                commandContextAppender, "commandContextAppender"
        );
        this.eventSink = Objects.requireNonNull(eventSink, "eventSink");
        this.transferSnapshotAssembler = Objects.requireNonNull(
                transferSnapshotAssembler, "transferSnapshotAssembler"
        );
    }

    public void captureSessionBaseline(
            AltoClef mod,
            int containerSessionOrdinal,
            HomeStoragePlan plan,
            HomeStorageOperationContext context) {
        baseline = PlanBaseline.unavailable();
        if (!boundaryCaptureEnabled()) {
            return;
        }
        captureSessionBaseline(
                containerSessionOrdinal,
                plan,
                StoreHomeHandlerSnapshot.capture(mod),
                context.worldKey(),
                String.valueOf(context.dimension())
        );
    }

    void captureSessionBaseline(
            int containerSessionOrdinal,
            HomeStoragePlan plan,
            StoreHomeHandlerSnapshot handler,
            String worldKey,
            String dimension) {
        baseline = PlanBaseline.unavailable();
        if (!boundaryCaptureEnabled()) {
            return;
        }
        baseline = new PlanBaseline(
                true,
                containerSessionOrdinal,
                plan.revision(),
                currentClientTick(),
                Objects.requireNonNull(handler, "handler"),
                Objects.requireNonNull(worldKey, "worldKey"),
                Objects.requireNonNull(dimension, "dimension")
        );
    }

    public boolean captureEnabled() {
        return boundaryCaptureEnabled();
    }

    public long operationId() {
        return operationId;
    }

    public void recordRootStale(
            Task task,
            AltoClef mod,
            StoreHomePhase phaseBeforeFailure,
            HomeStorageManifestValidation validation,
            HomeStorageManifestProgress progress,
            HomeStorageScreenSlotResolver slotResolver,
            HomeStorageTransferExecutor transferExecutor,
            AutoDepositTrustedDestinationCandidate activeCandidate,
            AutoDepositExactOpenContainerBinding binding) {
        if (!canEmit()) {
            return;
        }
        long failureTick = currentClientTick();
        StoreHomeScreenSlotSnapshot mapping;
        if (validation.logicalSlot() < 0) {
            mapping = StoreHomeScreenSlotSnapshot.notAttempted(
                    "logical_slot_unavailable"
            );
        } else if (validation.location()
                .filter(location -> location == HomeStorageStackLocation.MAIN)
                .isEmpty()) {
            mapping = StoreHomeScreenSlotSnapshot.notAttempted(
                    "non_main_baseline_slot"
            );
        } else {
            mapping = new StoreHomeScreenSlotSnapshotReader(slotResolver).inspect(
                    mod, validation.logicalSlot()
            );
        }
        StoreHomeManifestMismatchSnapshot mismatch =
                StoreHomeManifestMismatchSnapshot.fromValidation(validation);
        emitPrepared(task, snapshot(
                phaseBeforeFailure,
                StoreHomeManifestFailureStage.ROOT_MANIFEST_REVALIDATION,
                validation.reason(),
                failureTick,
                progress,
                mismatch,
                pendingSnapshotConverter.convert(
                        transferExecutor.pendingObservation()
                ),
                StoreHomeHandlerSnapshot.capture(mod),
                mapping,
                StoreHomeDestinationSnapshot.capture(activeCandidate, binding)
        ));
    }

    public void recordExecutorStale(
            Task task,
            AltoClef mod,
            StoreHomePhase phaseBeforeFailure,
            String reason,
            HomeStorageManifestProgress progress,
            HomeStorageManifestStep currentStep,
            AutoDepositTrustedDestination destination,
            AutoDepositExactOpenContainerBinding binding,
            HomeStorageTransferFailureObservation observation) {
        if (!canEmit()) {
            return;
        }
        StoreHomeTransferStaleSnapshot stale =
                transferSnapshotAssembler.assemble(
                        mod,
                        destination,
                        binding,
                        currentStep,
                        observation
                );
        recordExecutorStale(
                task,
                phaseBeforeFailure,
                reason,
                progress,
                currentStep,
                stale
        );
    }

    public void recordExecutorStale(
            Task task,
            StoreHomePhase phaseBeforeFailure,
            String reason,
            HomeStorageManifestProgress progress,
            HomeStorageManifestStep currentStep,
            StoreHomeTransferStaleSnapshot stale) {
        if (!canEmit()) {
            return;
        }
        StoreHomeTransferStaleSnapshot evidence = stale == null
                ? unavailableExecutorSnapshot(currentStep, progress, reason)
                : stale;
        emitPrepared(task, snapshot(
                phaseBeforeFailure,
                evidence.failureStage(),
                reason,
                currentClientTick(),
                progress,
                evidence.mismatch(),
                evidence.pending(),
                evidence.handler(),
                evidence.mapping(),
                evidence.destination()
        ));
    }

    private StoreHomeManifestStaleEventSnapshot snapshot(
            StoreHomePhase phaseBeforeFailure,
            StoreHomeManifestFailureStage failureStage,
            String reason,
            long failureTick,
            HomeStorageManifestProgress progress,
            StoreHomeManifestMismatchSnapshot mismatch,
            StoreHomePendingTransferSnapshot pending,
            StoreHomeHandlerSnapshot failureHandler,
            StoreHomeScreenSlotSnapshot mapping,
            StoreHomeDestinationSnapshot destination) {
        long revision = baseline.available()
                ? baseline.planRevision()
                : progress.manifestRevision();
        return new StoreHomeManifestStaleEventSnapshot(
                operationId,
                baseline.containerSessionOrdinal(),
                revision,
                baseline.planCapturedClientTickId(),
                failureTick,
                phaseBeforeFailure.name(),
                failureStage,
                reason,
                progress.manifestStepCount(),
                progress.currentStepLogicalSlot(),
                progress.confirmedItemCount(),
                progress.touchedStackCount(),
                mismatch,
                pending,
                baseline.handler(),
                failureHandler,
                mapping,
                baseline.worldKey(),
                baseline.dimension(),
                destination
        );
    }

    void emitPrepared(Task task, StoreHomeManifestStaleEventSnapshot event) {
        if (!canEmit()) {
            return;
        }
        emitted = true;
        try {
            Object[] requiredWithCommandContext = commandContextAppender.apply(
                    StoreHomeManifestStaleEventFields.required(event)
            );
            eventSink.emit(
                    task,
                    event.validationReason(),
                    requiredWithCommandContext,
                    StoreHomeManifestStaleEventFields.optional(event)
            );
        } catch (RuntimeException | LinkageError ignored) {
            // Diagnostics formatting or emission must not change STORE_HOME behavior.
        }
    }

    private boolean canEmit() {
        return !emitted && boundaryCaptureEnabled();
    }

    private boolean boundaryCaptureEnabled() {
        try {
            return boundaryEnabled.getAsBoolean();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private long currentClientTick() {
        try {
            return clientTick.getAsLong();
        } catch (RuntimeException | LinkageError ignored) {
            return -1L;
        }
    }

    private static StoreHomeTransferStaleSnapshot unavailableExecutorSnapshot(
            HomeStorageManifestStep currentStep,
            HomeStorageManifestProgress progress,
            String reason) {
        StoreHomeManifestMismatchSnapshot mismatch = currentStep == null
                ? StoreHomeManifestMismatchSnapshot.unavailable("executor_evidence_unavailable")
                : StoreHomeManifestMismatchSnapshot.unavailableForStep(
                        currentStep,
                        progress.manifestStepIndex(currentStep),
                        progress.expectedCount(currentStep),
                        "executor_evidence_unavailable"
                );
        return new StoreHomeTransferStaleSnapshot(
                stageForReason(reason),
                mismatch,
                StoreHomeScreenSlotSnapshot.notAttempted("executor_evidence_unavailable"),
                StoreHomePendingTransferSnapshot.none(),
                StoreHomeHandlerSnapshot.unavailable("executor_evidence_unavailable"),
                StoreHomeDestinationSnapshot.none()
        );
    }

    private static StoreHomeManifestFailureStage stageForReason(String reason) {
        if ("logical_slot_mapping_unavailable".equals(reason)) {
            return StoreHomeManifestFailureStage.EXECUTOR_SLOT_RESOLUTION;
        }
        if ("source_fingerprint_changed_after_click".equals(reason)) {
            return StoreHomeManifestFailureStage.EXECUTOR_POST_CLICK_SOURCE_VALIDATION;
        }
        return StoreHomeManifestFailureStage.EXECUTOR_PRE_CLICK_SOURCE_VALIDATION;
    }

    @FunctionalInterface
    interface EventSink {
        void emit(Task task, String reason, Object[] requiredFields, Object[] optionalFields);
    }

    private record PlanBaseline(
            boolean available,
            int containerSessionOrdinal,
            long planRevision,
            long planCapturedClientTickId,
            StoreHomeHandlerSnapshot handler,
            String worldKey,
            String dimension) {

        private static PlanBaseline unavailable() {
            return new PlanBaseline(
                    false,
                    -1,
                    -1L,
                    -1L,
                    StoreHomeHandlerSnapshot.unavailable("plan_baseline_unavailable"),
                    StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                    StoreHomeStackIdentitySnapshot.NOT_AVAILABLE
            );
        }
    }
}
