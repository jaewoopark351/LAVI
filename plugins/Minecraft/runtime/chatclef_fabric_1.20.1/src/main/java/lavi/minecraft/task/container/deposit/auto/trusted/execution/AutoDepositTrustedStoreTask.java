package lavi.minecraft.task.container.deposit.auto.trusted.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.DepositAllAutoDiagnostics;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

//20260827_kpopmodder: Own sequential trusted-candidate execution without a general-container fallback.
public final class AutoDepositTrustedStoreTask extends Task {
    private static final int MAX_CANDIDATE_TICKS = 2400;
    private static final int MAX_OPEN_NO_PROGRESS_TICKS = 200;
    private static final int MAX_OPERATION_TICKS = 6000;

    private final AutoDepositContextSnapshot context;
    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositTrustedCandidateQueue candidateQueue;
    private final ItemTarget[] requestedTargets;
    private final AutoDepositTrustedContainerAcceptanceInspector acceptanceInspector;
    private final AutoDepositTrustedTransferTracker transferTracker;

    private AutoDepositTrustedDestinationCandidate activeCandidate;
    private InteractWithBlockTask activeOpenTask;
    private StoreInContainerTask activeStoreTask;
    private AutoDepositTrustedStoreOutcome outcome = AutoDepositTrustedStoreOutcome.PENDING;
    private int candidateTicks;
    private int openNoProgressTicks;
    private int lastConfirmedCount;
    private int operationTicks;
    private boolean liveAcceptanceLogged;

    public AutoDepositTrustedStoreTask(
            AutoDepositContextSnapshot context,
            AutoDepositTrustedDestinationRepository repository,
            List<AutoDepositTrustedDestinationCandidate> candidates,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            ItemTarget... requestedTargets) {
        this.context = Objects.requireNonNull(context, "context");
        this.repository = Objects.requireNonNull(repository, "repository");
        candidateQueue = new AutoDepositTrustedCandidateQueue(candidates);
        this.requestedTargets = mergeEquivalentTargets(requestedTargets);
        acceptanceInspector = new AutoDepositTrustedContainerAcceptanceInspector(
                exactOpenContainerBinding
        );
        transferTracker = new AutoDepositTrustedTransferTracker(
                AltoClef.getInstance(),
                context.worldIdentity(),
                context.dimension(),
                exactOpenContainerBinding
        );
    }

    @Override
    protected void onStart() {
        transferTracker.start();
        if (activeCandidate != null) {
            transferTracker.observeCandidate(activeCandidate.position());
        }
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (!context.matches(mod)) {
            finish(AutoDepositTrustedStoreOutcome.CONTEXT_CHANGED, "automatic_context_changed");
            return null;
        }
        operationTicks++;
        if (operationTicks >= MAX_OPERATION_TICKS) {
            finish(AutoDepositTrustedStoreOutcome.CANDIDATES_EXHAUSTED,
                    "trusted_operation_timeout");
            return null;
        }

        ItemTarget[] remaining = remainingTargets();
        if (remaining.length == 0) {
            finish(AutoDepositTrustedStoreOutcome.ALL_STORED, "trusted_transfer_confirmed");
            return null;
        }

        if (!ensureActiveCandidate(mod, remaining)) {
            finish(AutoDepositTrustedStoreOutcome.CANDIDATES_EXHAUSTED,
                    "trusted_candidates_exhausted");
            return null;
        }
        String activeRefusal = preflightRefusal(mod, activeCandidate);
        if (activeRefusal != null) {
            rejectActive(activeRefusal, remaining.length);
            if (!ensureActiveCandidate(mod, remaining)) {
                finish(AutoDepositTrustedStoreOutcome.CANDIDATES_EXHAUSTED,
                        "trusted_candidates_exhausted");
                return null;
            }
        }

        candidateTicks++;
        int confirmedCount = confirmedCount();
        if (confirmedCount > lastConfirmedCount) {
            DepositAllAutoDiagnostics.logTrustedTransferProgress(
                    context.epoch(),
                    activeCandidate,
                    confirmedCount - lastConfirmedCount,
                    confirmedCount,
                    remaining.length
            );
            lastConfirmedCount = confirmedCount;
            openNoProgressTicks = 0;
        }

        AutoDepositTrustedContainerAcceptanceInspector.Result acceptance =
                acceptanceInspector.inspect(mod, activeCandidate.position(), remaining);
        if (acceptance == AutoDepositTrustedContainerAcceptanceInspector.Result.NOT_OPEN) {
            if (candidateTicks >= MAX_CANDIDATE_TICKS) {
                rejectActive("candidate_timeout", remaining.length);
                return ensureActiveCandidate(mod, remaining) ? activeOpenTask : null;
            }
            return activeOpenTask;
        }
        if (acceptance == AutoDepositTrustedContainerAcceptanceInspector.Result.CAN_ACCEPT) {
            if (!liveAcceptanceLogged) {
                liveAcceptanceLogged = true;
                DepositAllAutoDiagnostics.logTrustedCandidateAccepted(
                        context.epoch(), activeCandidate, remaining.length
                );
            }
            openNoProgressTicks++;
        } else if (acceptance
                == AutoDepositTrustedContainerAcceptanceInspector.Result.NO_WHOLE_STACK_CAPACITY) {
            rejectActive("live_gui_has_no_whole_stack_capacity", remaining.length);
            return ensureActiveCandidate(mod, remaining) ? activeOpenTask : null;
        } else if (acceptance
                == AutoDepositTrustedContainerAcceptanceInspector.Result.NO_SOURCE_ITEMS) {
            rejectActive("source_items_missing_without_confirmed_transfer", remaining.length);
            return ensureActiveCandidate(mod, remaining) ? activeOpenTask : null;
        } else if (acceptance
                == AutoDepositTrustedContainerAcceptanceInspector.Result.INVALID_CONTAINER) {
            rejectActive("live_gui_container_invalid", remaining.length);
            return ensureActiveCandidate(mod, remaining) ? activeOpenTask : null;
        }

        if (activeStoreTask.isFinished()) {
            ItemTarget[] afterTerminal = remainingTargets();
            if (afterTerminal.length == 0) {
                finish(AutoDepositTrustedStoreOutcome.ALL_STORED,
                        "trusted_child_finished_with_confirmed_delta");
                return null;
            }
            rejectActive("child_terminal_without_complete_confirmed_delta", afterTerminal.length);
            return ensureActiveCandidate(mod, afterTerminal) ? activeOpenTask : null;
        }
        if (candidateTicks >= MAX_CANDIDATE_TICKS) {
            rejectActive("candidate_timeout", remaining.length);
            return ensureActiveCandidate(mod, remaining) ? activeOpenTask : null;
        }
        if (openNoProgressTicks >= MAX_OPEN_NO_PROGRESS_TICKS) {
            rejectActive("open_container_no_confirmed_progress", remaining.length);
            return ensureActiveCandidate(mod, remaining) ? activeOpenTask : null;
        }
        return activeStoreTask;
    }

    private boolean ensureActiveCandidate(AltoClef mod, ItemTarget[] remaining) {
        while (activeCandidate == null) {
            AutoDepositTrustedDestinationCandidate candidate = candidateQueue.current().orElse(null);
            if (candidate == null) {
                return false;
            }
            String refusal = preflightRefusal(mod, candidate);
            if (refusal != null) {
                DepositAllAutoDiagnostics.logTrustedCandidateTerminal(
                        context.epoch(), candidate, refusal, remaining.length,
                        candidateQueue.remainingCandidateCount() - 1
                );
                candidateQueue.rejectCurrent(refusal);
                continue;
            }
            activeCandidate = candidate;
            activeOpenTask = new InteractWithBlockTask(candidate.position());
            activeStoreTask = new StoreInContainerTask(
                    candidate.position(), false, remaining
            );
            candidateTicks = 0;
            openNoProgressTicks = 0;
            liveAcceptanceLogged = false;
            lastConfirmedCount = confirmedCount();
            transferTracker.observeCandidate(candidate.position());
            DepositAllAutoDiagnostics.logTrustedCandidateSelected(
                    context.epoch(), candidate, remaining.length,
                    candidateQueue.remainingCandidateCount()
            );
        }
        return true;
    }

    private String preflightRefusal(
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate) {
        if (!repository.containsEnabled(candidate.destination())) {
            return "registration_removed_or_disabled";
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

    private void rejectActive(String reason, int remainingTargetTypes) {
        AutoDepositTrustedDestinationCandidate rejected = activeCandidate;
        DepositAllAutoDiagnostics.logTrustedCandidateTerminal(
                context.epoch(), rejected, reason, remainingTargetTypes,
                Math.max(0, candidateQueue.remainingCandidateCount() - 1)
        );
        candidateQueue.rejectCurrent(reason);
        activeCandidate = null;
        activeOpenTask = null;
        activeStoreTask = null;
        transferTracker.observeCandidate(null);
        candidateTicks = 0;
        openNoProgressTicks = 0;
        liveAcceptanceLogged = false;
    }

    private ItemTarget[] remainingTargets() {
        List<ItemTarget> remaining = new ArrayList<>();
        for (ItemTarget target : requestedTargets) {
            int confirmed = transferTracker.confirmedCount(target.getMatches());
            int count = Math.max(0, target.getTargetCount() - confirmed);
            if (count > 0) {
                remaining.add(new ItemTarget(target, count));
            }
        }
        return remaining.toArray(ItemTarget[]::new);
    }

    private int confirmedCount() {
        int total = 0;
        for (ItemTarget target : requestedTargets) {
            total += Math.min(
                    target.getTargetCount(),
                    transferTracker.confirmedCount(target.getMatches())
            );
        }
        return total;
    }

    private void finish(AutoDepositTrustedStoreOutcome terminalOutcome, String reason) {
        if (outcome != AutoDepositTrustedStoreOutcome.PENDING) {
            return;
        }
        outcome = terminalOutcome;
        DepositAllAutoDiagnostics.logTrustedCandidateTerminal(
                context.epoch(), activeCandidate, reason,
                remainingTargetTypeCount(), candidateQueue.remainingCandidateCount()
        );
        activeCandidate = null;
        activeOpenTask = null;
        activeStoreTask = null;
        transferTracker.observeCandidate(null);
        transferTracker.stop();
    }

    private int remainingTargetTypeCount() {
        int count = 0;
        for (ItemTarget target : requestedTargets) {
            if (transferTracker.confirmedCount(target.getMatches()) < target.getTargetCount()) {
                count++;
            }
        }
        return count;
    }

    private static ItemTarget[] mergeEquivalentTargets(ItemTarget[] targets) {
        List<ItemTarget> merged = new ArrayList<>();
        if (targets == null) {
            return new ItemTarget[0];
        }
        for (ItemTarget target : targets) {
            if (target == null || target.isEmpty() || target.getTargetCount() <= 0) {
                continue;
            }
            int existingIndex = -1;
            for (int index = 0; index < merged.size(); index++) {
                if (Arrays.equals(merged.get(index).getMatches(), target.getMatches())) {
                    existingIndex = index;
                    break;
                }
            }
            if (existingIndex < 0) {
                merged.add(new ItemTarget(target, target.getTargetCount()));
            } else {
                ItemTarget existing = merged.get(existingIndex);
                long combined = (long) existing.getTargetCount() + target.getTargetCount();
                merged.set(existingIndex, new ItemTarget(
                        existing,
                        combined > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) combined
                ));
            }
        }
        return merged.toArray(ItemTarget[]::new);
    }

    @Override
    protected void onStop(Task interruptTask) {
        transferTracker.stop();
    }

    @Override
    public boolean isFinished() {
        return outcome != AutoDepositTrustedStoreOutcome.PENDING;
    }

    @Override
    protected boolean isEqual(Task other) {
        return this == other;
    }

    @Override
    protected String toDebugString() {
        String candidate = activeCandidate == null
                ? "none"
                : activeCandidate.destinationId();
        return "Trusted automatic storage: " + outcome + ", candidate=" + candidate;
    }

    public AutoDepositTrustedStoreOutcome outcome() {
        return outcome;
    }

    public StoreInContainerTask activeStoreTask() {
        return activeStoreTask;
    }

    public int rejectedCandidateCount() {
        return candidateQueue.rejected().size();
    }
}
