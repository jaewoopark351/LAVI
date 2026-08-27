package lavi.minecraft.task.container.home.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedCandidateQueue;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.planning.HomeLoadoutPlanner;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshotReader;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Own the explicit trusted-only home-storage operation and its terminal result.
public final class StoreHomeTask extends Task {
    private static final int MAX_CANDIDATE_TICKS = 2400;
    private static final int MAX_OPERATION_TICKS = 12000;

    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;
    private final AutoDepositWorldKeyReader worldKeyReader;
    private final HomeStorageInventorySnapshotReader snapshotReader;
    private final HomeLoadoutPlanner planner;
    private final HomeStorageDestinationSelector destinationSelector;
    private final HomeStorageTransferExecutor transferExecutor;

    private StoreHomePhase phase = StoreHomePhase.ACCEPT_REQUEST;
    private StoreHomeResult result = StoreHomeResult.PENDING;
    private HomeStorageOperationContext context;
    private HomeStoragePlan plan;
    private HomeStorageManifestProgress progress;
    private AutoDepositTrustedCandidateQueue candidateQueue;
    private AutoDepositTrustedDestinationCandidate activeCandidate;
    private InteractWithBlockTask activeOpenTask;
    private boolean initialized;
    private int candidateTicks;
    private int operationTicks;
    private int capacityFailures;
    private int unavailableFailures;

    public StoreHomeTask(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositWorldKeyReader worldKeyReader,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeLoadoutPlanner planner,
            HomeStorageDestinationSelector destinationSelector,
            HomeStorageTransferExecutor transferExecutor) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.exactOpenContainerBinding = Objects.requireNonNull(
                exactOpenContainerBinding, "exactOpenContainerBinding"
        );
        this.worldKeyReader = Objects.requireNonNull(worldKeyReader, "worldKeyReader");
        this.snapshotReader = Objects.requireNonNull(snapshotReader, "snapshotReader");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.destinationSelector = Objects.requireNonNull(
                destinationSelector, "destinationSelector"
        );
        this.transferExecutor = Objects.requireNonNull(transferExecutor, "transferExecutor");
    }

    @Override
    protected void onStart() {
        if (result != StoreHomeResult.PENDING) {
            return;
        }
        phase = initialized
                ? StoreHomePhase.REVALIDATE_AFTER_RESUME
                : StoreHomePhase.ACCEPT_REQUEST;
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (result != StoreHomeResult.PENDING) {
            return null;
        }
        if (!initialized) {
            initialize(mod);
            if (result != StoreHomeResult.PENDING || progress.complete()) {
                if (result == StoreHomeResult.PENDING) {
                    finish(mod, StoreHomeResult.COMPLETED, "nothing_to_store");
                }
                return null;
            }
        }

        operationTicks++;
        if (operationTicks >= MAX_OPERATION_TICKS) {
            if (transferExecutor.hasPending()) {
                finish(mod, StoreHomeResult.TRANSFER_UNCONFIRMED,
                        "operation_timeout_with_pending_transfer");
            } else {
                finishExhausted(mod, "operation_timeout");
            }
            return null;
        }
        if (!context.matches(mod, worldKeyReader)) {
            finish(mod, StoreHomeResult.CONTEXT_CHANGED, "world_or_dimension_changed");
            return null;
        }
        if (!cursorEmpty(mod)) {
            finish(mod, StoreHomeResult.CURSOR_NOT_EMPTY, "cursor_not_empty_during_operation");
            return null;
        }

        HomeStorageManifestProgress.Validation validation = progress.validate(
                mod, transferExecutor.pendingLogicalSlot()
        );
        if (!validation.valid()) {
            finish(mod, StoreHomeResult.MANIFEST_STALE,
                    validation.reason() + ":slot=" + validation.logicalSlot());
            return null;
        }
        if (progress.complete()) {
            finish(mod, StoreHomeResult.COMPLETED, "manifest_complete");
            return null;
        }

        if (!ensureActiveCandidate(mod)) {
            finishExhausted(mod, "trusted_candidates_exhausted");
            return null;
        }
        candidateTicks++;
        if (candidateTicks >= MAX_CANDIDATE_TICKS) {
            if (transferExecutor.hasPending()) {
                finish(mod, StoreHomeResult.TRANSFER_UNCONFIRMED,
                        "candidate_timeout_with_pending_transfer");
            } else {
                rejectActive("candidate_timeout", FailureKind.UNAVAILABLE);
            }
            return null;
        }

        HomeStorageManifestStep step = progress.currentStep().orElse(null);
        if (step == null) {
            finish(mod, StoreHomeResult.COMPLETED, "manifest_complete");
            return null;
        }

        if (transferExecutor.hasPending()) {
            phase = StoreHomePhase.VERIFY_TRANSFER;
            return handleTransferResult(mod, step, transferExecutor.tick(
                    mod,
                    activeCandidate.destination(),
                    exactOpenContainerBinding,
                    step,
                    progress.expectedCount(step)
            ));
        }

        String refusal = preflightRefusal(mod, activeCandidate);
        if (refusal != null) {
            rejectActive(refusal, FailureKind.UNAVAILABLE);
            return null;
        }
        if (!exactOpenContainerBinding.matches(activeCandidate.position())) {
            phase = StoreHomePhase.NAVIGATE_AND_OPEN;
            return openTask();
        }

        phase = StoreHomePhase.VALIDATE_CONTAINER;
        String transferRefusal = preflightRefusal(mod, activeCandidate);
        if (transferRefusal != null) {
            rejectActive(transferRefusal, FailureKind.UNAVAILABLE);
            return null;
        }
        phase = StoreHomePhase.TRANSFER_EXACT_SLOTS;
        return handleTransferResult(mod, step, transferExecutor.tick(
                mod,
                activeCandidate.destination(),
                exactOpenContainerBinding,
                step,
                progress.expectedCount(step)
        ));
    }

    private void initialize(AltoClef mod) {
        phase = StoreHomePhase.ACCEPT_REQUEST;
        if (!cursorEmpty(mod)) {
            finish(mod, StoreHomeResult.CURSOR_NOT_EMPTY, "cursor_not_empty_at_acceptance");
            return;
        }
        phase = StoreHomePhase.SNAPSHOT_CONTEXT;
        Optional<HomeStorageOperationContext> captured = HomeStorageOperationContext.capture(
                mod, worldKeyReader
        );
        if (captured.isEmpty()) {
            finish(mod, StoreHomeResult.CONTEXT_CHANGED, "world_context_unavailable");
            return;
        }
        context = captured.get();
        phase = StoreHomePhase.PLAN_LOADOUT;
        plan = planner.plan(snapshotReader.read(mod));
        progress = new HomeStorageManifestProgress(plan.manifest());
        phase = StoreHomePhase.BUILD_DESTINATION_QUEUE;
        List<AutoDepositTrustedDestinationCandidate> candidates =
                destinationSelector.snapshot(mod, context);
        candidateQueue = new AutoDepositTrustedCandidateQueue(candidates);
        initialized = true;
    }

    private Task handleTransferResult(
            AltoClef mod,
            HomeStorageManifestStep step,
            HomeStorageTransferExecutor.Result transfer) {
        switch (transfer.status()) {
            case WAITING, CLICK_REQUESTED -> {
                return null;
            }
            case CONTAINER_NOT_OPEN -> {
                if (!repository.containsEnabled(activeCandidate.destination())) {
                    finish(mod, StoreHomeResult.TRANSFER_UNCONFIRMED,
                            "pending_destination_removed_before_verification");
                    return null;
                }
                phase = StoreHomePhase.NAVIGATE_AND_OPEN;
                return openTask();
            }
            case TRANSFERRED -> {
                progress.confirm(
                        step, transfer.transferredCount(), transfer.sourceCountAfter()
                );
                candidateTicks = 0;
                if (progress.complete()) {
                    finish(mod, StoreHomeResult.COMPLETED, transfer.reason());
                }
                return null;
            }
            case NO_CAPACITY -> {
                rejectActive(transfer.reason(), FailureKind.CAPACITY);
                return null;
            }
            case NO_PROGRESS -> {
                rejectActive(transfer.reason(), FailureKind.UNAVAILABLE);
                return null;
            }
            case MANIFEST_STALE -> {
                finish(mod, StoreHomeResult.MANIFEST_STALE, transfer.reason());
                return null;
            }
            case CURSOR_NOT_EMPTY -> {
                finish(mod, StoreHomeResult.CURSOR_NOT_EMPTY, transfer.reason());
                return null;
            }
            case TRANSFER_UNCONFIRMED -> {
                finish(mod, StoreHomeResult.TRANSFER_UNCONFIRMED, transfer.reason());
                return null;
            }
        }
        throw new IllegalStateException("Unhandled transfer status " + transfer.status());
    }

    private boolean ensureActiveCandidate(AltoClef mod) {
        while (activeCandidate == null) {
            phase = StoreHomePhase.SELECT_DESTINATION;
            AutoDepositTrustedDestinationCandidate candidate = candidateQueue.current()
                    .orElse(null);
            if (candidate == null) {
                return false;
            }
            String refusal = preflightRefusal(mod, candidate);
            if (refusal != null) {
                candidateQueue.rejectCurrent(refusal);
                unavailableFailures++;
                continue;
            }
            activeCandidate = candidate;
            activeOpenTask = new InteractWithBlockTask(candidate.position());
            candidateTicks = 0;
        }
        return true;
    }

    private String preflightRefusal(
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate) {
        if (!repository.containsEnabled(candidate.destination())) {
            return "registration_removed_or_disabled";
        }
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            return "runtime_unavailable";
        }
        if (mod.getBlockScanner().isUnreachable(candidate.position())) {
            return "known_unreachable";
        }
        if (mod.getChunkTracker().isChunkLoaded(candidate.position())
                && !AutoDepositTrustedContainerSupport.isSupported(
                mod.getWorld().getBlockState(candidate.position()).getBlock())) {
            return "container_missing";
        }
        return null;
    }

    private Task openTask() {
        if (activeOpenTask == null || activeOpenTask.isFinished() || activeOpenTask.stopped()) {
            activeOpenTask = new InteractWithBlockTask(activeCandidate.position());
        }
        return activeOpenTask;
    }

    private void rejectActive(String reason, FailureKind kind) {
        if (kind == FailureKind.CAPACITY) {
            capacityFailures++;
        } else {
            unavailableFailures++;
        }
        candidateQueue.rejectCurrent(reason);
        activeCandidate = null;
        activeOpenTask = null;
        candidateTicks = 0;
        transferExecutor.clearPending();
    }

    private void finishExhausted(AltoClef mod, String reason) {
        if (progress.confirmedItemCount() > 0) {
            finish(mod,
                    capacityFailures > 0
                            ? StoreHomeResult.PARTIAL_TRUSTED_CAPACITY_EXHAUSTED
                            : StoreHomeResult.PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE,
                    reason);
            return;
        }
        finish(mod,
                capacityFailures > 0 && unavailableFailures == 0
                        ? StoreHomeResult.NO_TRUSTED_CAPACITY
                        : StoreHomeResult.NO_USABLE_TRUSTED_DESTINATION,
                reason);
    }

    private void finish(AltoClef mod, StoreHomeResult terminal, String reason) {
        if (result != StoreHomeResult.PENDING) {
            return;
        }
        result = terminal;
        phase = StoreHomePhase.TERMINAL;
        int storedItems = progress == null ? 0 : progress.confirmedItemCount();
        int storedStacks = progress == null ? 0 : progress.touchedStackCount();
        int remainingStacks = progress == null ? 0 : progress.remainingStackCount();
        String message = "Store home: result=" + terminal
                + ", storedItems=" + storedItems
                + ", touchedStacks=" + storedStacks
                + ", remainingStacks=" + remainingStacks
                + ", reason=" + reason;
        if (mod != null) {
            if (terminal == StoreHomeResult.COMPLETED) {
                mod.log(message);
            } else {
                mod.logWarning(message);
            }
        }
    }

    private static boolean cursorEmpty(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null
                || mod.getPlayer().currentScreenHandler == null) {
            return false;
        }
        ItemStack cursor = mod.getPlayer().currentScreenHandler.getCursorStack();
        return cursor == null || cursor.isEmpty();
    }

    @Override
    protected void onStop(Task interruptTask) {
        if (result != StoreHomeResult.PENDING) {
            return;
        }
        if (interruptTask != null) {
            finish(AltoClef.getInstance(), StoreHomeResult.INTERRUPTED,
                    "replaced_by_new_user_task");
        } else {
            phase = StoreHomePhase.SUSPENDED;
        }
    }

    @Override
    public boolean isFinished() {
        return result != StoreHomeResult.PENDING;
    }

    @Override
    protected boolean isEqual(Task other) {
        return this == other;
    }

    @Override
    protected String toDebugString() {
        String destination = activeCandidate == null
                ? "none"
                : activeCandidate.destinationId();
        return "Store home: phase=" + phase
                + ", result=" + result
                + ", destination=" + destination;
    }

    public StoreHomeResult result() {
        return result;
    }

    public StoreHomePhase phase() {
        return phase;
    }

    public HomeStoragePlan plan() {
        return plan;
    }

    private enum FailureKind {
        CAPACITY,
        UNAVAILABLE
    }
}
