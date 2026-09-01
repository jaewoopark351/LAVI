package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Bound exact candidate/active Task references to eight command scopes.
public final class FabricChatClefCraftResourceContainerActivationRegistry {
    private static final int ACTIVE_SCOPE_LIMIT = 8;

    private final Map<IronPickaxeAcquisitionScopeKey,
            FabricChatClefCraftResourceContainerActivationState> states =
            new LinkedHashMap<>();

    public synchronized boolean observeCandidate(
            FabricChatClefCraftResourceContainerActivationCandidate candidate) {
        if (candidate == null) {
            return false;
        }
        FabricChatClefCraftResourceContainerActivationState state = states.get(
                candidate.scopeKey()
        );
        if (state == null) {
            if (states.size() >= ACTIVE_SCOPE_LIMIT) {
                return false;
            }
            state = new FabricChatClefCraftResourceContainerActivationState();
            states.put(candidate.scopeKey(), state);
        }
        state.pending = candidate;
        return true;
    }

    public synchronized FabricChatClefCraftResourceContainerActivationDecision reconcile(
            IronPickaxeAcquisitionScopeKey scopeKey,
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean replacementApplied,
            Task activeChildAfter,
            boolean childCleared,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceContainerActivationState state = states.get(scopeKey);
        if (state == null) {
            return emptyDecision(sourceEmissionCompleted);
        }

        FabricChatClefCraftResourceContainerActiveTarget closed = null;
        boolean activeTransitionForParent = state.active != null
                && state.active.parentTask() == parent
                && (replacementApplied || childCleared)
                && activeChildAfter != state.active.activeChildTask();
        boolean activeIdentityMismatch = activeTransitionForParent
                && state.active.activeChildTask() != activeChildBefore;
        boolean activeClosureObserved = activeTransitionForParent
                && !activeIdentityMismatch;
        if (activeTransitionForParent) {
            if (sourceEmissionCompleted && activeClosureObserved) {
                closed = state.active;
            }
            state.active = null;
        }

        FabricChatClefCraftResourceContainerActivationCandidate installed = null;
        FabricChatClefCraftResourceContainerActivationCandidate pending = state.pending;
        boolean pendingBelongsToParent = pending != null
                && pending.parentTask() == parent;
        boolean candidateReconciled = pendingBelongsToParent
                && pending.candidateTask() == candidateChild;
        boolean pendingIdentityMismatch = pendingBelongsToParent
                && pending.candidateTask() != candidateChild;
        boolean candidateInstallationObserved = candidateReconciled
                && replacementApplied
                && activeChildAfter == candidateChild;
        if (pendingBelongsToParent) {
            if (sourceEmissionCompleted && candidateInstallationObserved) {
                installed = pending;
            }
            state.pending = null;
        }
        boolean sourceTransitionSuppressed = !sourceEmissionCompleted
                && (activeClosureObserved || candidateInstallationObserved);
        boolean identityMismatchObserved = activeIdentityMismatch
                || pendingIdentityMismatch;

        return new FabricChatClefCraftResourceContainerActivationDecision(
                Optional.ofNullable(closed),
                Optional.ofNullable(installed),
                sourceEmissionCompleted,
                sourceTransitionSuppressed,
                identityMismatchObserved
        );
    }

    public synchronized boolean markActive(
            IronPickaxeAcquisitionScopeKey scopeKey,
            FabricChatClefCraftResourceContainerActivationCandidate candidate,
            long targetAttemptSequence) {
        FabricChatClefCraftResourceContainerActivationState state = states.get(scopeKey);
        if (state == null
                || candidate == null
                || targetAttemptSequence <= 0L
                || state.active != null
                || !scopeKey.equals(candidate.scopeKey())) {
            return false;
        }
        state.active = new FabricChatClefCraftResourceContainerActiveTarget(
                candidate.parentTask(),
                candidate.candidateTask(),
                candidate.targetTuple(),
                targetAttemptSequence
        );
        return true;
    }

    public synchronized boolean canActivateCandidate(
            IronPickaxeAcquisitionScopeKey scopeKey,
            FabricChatClefCraftResourceContainerActivationCandidate candidate) {
        FabricChatClefCraftResourceContainerActivationState state = states.get(scopeKey);
        return state != null
                && state.active == null
                && candidate != null
                && scopeKey.equals(candidate.scopeKey());
    }

    public synchronized FabricChatClefCraftResourceContainerOwnerExitDecision
            observeOwnerExit(Task owner, boolean sourceEmissionCompleted) {
        if (owner == null) {
            return emptyOwnerExitDecision(sourceEmissionCompleted);
        }
        IronPickaxeAcquisitionScopeKey matchedScopeKey = null;
        FabricChatClefCraftResourceContainerActiveTarget matchedActive = null;
        int matchingScopeCount = 0;
        List<IronPickaxeAcquisitionScopeKey> ambiguousScopeKeys = new ArrayList<>(
                ACTIVE_SCOPE_LIMIT
        );
        Iterator<Map.Entry<IronPickaxeAcquisitionScopeKey,
                FabricChatClefCraftResourceContainerActivationState>> iterator =
                states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<IronPickaxeAcquisitionScopeKey,
                    FabricChatClefCraftResourceContainerActivationState> entry =
                    iterator.next();
            FabricChatClefCraftResourceContainerActivationState state =
                    entry.getValue();
            boolean pendingMatches = state.pending != null
                    && state.pending.parentTask() == owner;
            FabricChatClefCraftResourceContainerActiveTarget active = state.active;
            boolean activeMatches = active != null && active.parentTask() == owner;
            if (!pendingMatches && !activeMatches) {
                continue;
            }
            matchingScopeCount++;
            ambiguousScopeKeys.add(entry.getKey());
            if (matchingScopeCount == 1) {
                matchedScopeKey = entry.getKey();
                matchedActive = activeMatches ? active : null;
            } else {
                matchedScopeKey = null;
                matchedActive = null;
            }
            if (pendingMatches) {
                state.pending = null;
            }
            if (activeMatches) {
                state.active = null;
            }
            if (state.pending == null && state.active == null) {
                iterator.remove();
            }
        }
        boolean ownerIdentityAmbiguous = matchingScopeCount > 1;
        if (matchingScopeCount == 0 || ownerIdentityAmbiguous) {
            return new FabricChatClefCraftResourceContainerOwnerExitDecision(
                    Optional.empty(),
                    Optional.empty(),
                    sourceEmissionCompleted,
                    false,
                    ownerIdentityAmbiguous,
                    ownerIdentityAmbiguous ? ambiguousScopeKeys : List.of()
            );
        }
        boolean sourceTransitionSuppressed = !sourceEmissionCompleted
                && matchedActive != null;
        return new FabricChatClefCraftResourceContainerOwnerExitDecision(
                Optional.of(matchedScopeKey),
                sourceEmissionCompleted
                        ? Optional.ofNullable(matchedActive)
                        : Optional.empty(),
                sourceEmissionCompleted,
                sourceTransitionSuppressed,
                false,
                List.of()
        );
    }

    public synchronized boolean hasOwner(Task owner) {
        if (owner == null) {
            return false;
        }
        return states.values().stream().anyMatch(state ->
                (state.pending != null && state.pending.parentTask() == owner)
                        || (state.active != null && state.active.parentTask() == owner)
        );
    }

    public synchronized void retire(IronPickaxeAcquisitionScopeKey scopeKey) {
        states.remove(scopeKey);
    }

    public synchronized void clearForModeOff() {
        states.clear();
    }

    private static FabricChatClefCraftResourceContainerActivationDecision emptyDecision(
            boolean sourceEmissionCompleted) {
        return new FabricChatClefCraftResourceContainerActivationDecision(
                Optional.empty(),
                Optional.empty(),
                sourceEmissionCompleted,
                false,
                false
        );
    }

    private static FabricChatClefCraftResourceContainerOwnerExitDecision
            emptyOwnerExitDecision(boolean sourceEmissionCompleted) {
        return new FabricChatClefCraftResourceContainerOwnerExitDecision(
                Optional.empty(),
                Optional.empty(),
                sourceEmissionCompleted,
                false,
                false,
                List.of()
        );
    }
}
