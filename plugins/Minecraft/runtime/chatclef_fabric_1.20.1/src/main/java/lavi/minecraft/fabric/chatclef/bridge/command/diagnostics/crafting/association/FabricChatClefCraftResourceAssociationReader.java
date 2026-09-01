package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationClassifier;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationEvidence;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceSelectedChainKind;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//20260901_kpopmodder: Reduce live Task identities to immutable fail-closed association evidence.
public final class FabricChatClefCraftResourceAssociationReader {
    private static final int RETAINED_TASK_PATH_LIMIT = 8;
    private static final CraftResourceAssociationClassifier CLASSIFIER =
            new CraftResourceAssociationClassifier();
    private static final FabricChatClefTaskStateReader TASK_STATE_READER =
            new FabricChatClefTaskStateReader();

    private FabricChatClefCraftResourceAssociationReader() {
    }

    public static FabricChatClefCraftResourceAssociationSnapshot capture(Task emittingTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return unavailable("DIAGNOSTICS_OFF");
        }
        Optional<IronPickaxeAcquisitionScopeBinding> binding = Optional.empty();
        try {
            binding = activeBinding();
            if (binding.isEmpty()) {
                return unavailable("NO_ACTIVE_IRON_PICKAXE_SCOPE");
            }
            return captureEligible(emittingTask, binding);
        } catch (RuntimeException | LinkageError error) {
            String reason = errorName(error);
            observeProjectionFailure(binding, "ASSOCIATION_CAPTURE", reason);
            return unavailable(binding, reason);
        }
    }

    public static void observeProjectionFailure(
            String observationBoundary,
            Throwable error) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            observeProjectionFailure(activeBinding(), observationBoundary, errorName(error));
        } catch (RuntimeException | LinkageError ignored) {
            // This protects only diagnostic coverage-gap bookkeeping.
        }
    }

    private static Optional<IronPickaxeAcquisitionScopeBinding> activeBinding() {
        Object[] commandFields = ChatClefDiagnostics.withCommandContextFields();
        return IronPickaxeAcquisitionScopeDiagnostics.activeBinding(
                stringField(commandFields, "commandSessionId"),
                longField(commandFields, "commandConnectionGeneration", -1L),
                stringField(commandFields, "commandRequestId"),
                stringField(commandFields, "commandCorrelationId")
        );
    }

    private static FabricChatClefCraftResourceAssociationSnapshot captureEligible(
            Task emittingTask,
            Optional<IronPickaxeAcquisitionScopeBinding> binding) {
        if (emittingTask == null) {
            observeProjectionFailure(
                    binding,
                    "ASSOCIATION_CAPTURE",
                    "EMITTING_TASK_UNAVAILABLE"
            );
            return unavailable(binding, "EMITTING_TASK_UNAVAILABLE");
        }
        IronPickaxeAcquisitionScopeBinding active = binding.get();
        AltoClef mod = AltoClef.getInstance();
        TaskChain selectedChain = mod == null || mod.getTaskRunner() == null
                ? null
                : mod.getTaskRunner().getCurrentTaskChain();
        List<Task> sourcePath = selectedChain == null
                ? List.of()
                : selectedChain.getTasks();
        boolean pathTruncated = sourcePath.size() > RETAINED_TASK_PATH_LIMIT;
        int retainedPathSize = Math.min(sourcePath.size(), RETAINED_TASK_PATH_LIMIT);
        List<Task> actualPath = sourcePath.isEmpty()
                ? List.of()
                : new ArrayList<>(sourcePath.subList(0, retainedPathSize));
        Task sourceTask = emittingTask;
        FabricChatClefTaskOwnershipEvidence currentRoot = TASK_STATE_READER.ownershipEvidence();

        boolean sourceInPath = containsIdentity(actualPath, sourceTask);
        boolean pathRootMatches = !actualPath.isEmpty()
                && actualPath.get(0) == active.boundRootTask();
        CraftResourceSelectedChainKind chainKind = selectedChain instanceof UserTaskChain
                ? CraftResourceSelectedChainKind.USER_TASK_CHAIN
                : selectedChain == null
                        ? CraftResourceSelectedChainKind.UNKNOWN
                        : CraftResourceSelectedChainKind.CONCURRENT_NON_USER_CHAIN;
        CraftResourceAssociationEvidence evidence = new CraftResourceAssociationEvidence(
                true,
                currentRoot.available()
                        && active.key().rootAssignmentId().equals(currentRoot.userTaskRootAssignmentId()),
                currentRoot.available()
                        && active.key().rootGeneration() == currentRoot.userTaskRootGeneration(),
                currentRoot.available() && currentRoot.rootTask() == active.boundRootTask(),
                chainKind,
                sourceInPath,
                pathRootMatches,
                pathTruncated,
                sourceInPath && pathRootMatches,
                currentRoot.available() && currentRoot.rootTask() == active.boundRootTask(),
                true,
                false,
                false
        );
        CraftResourceAssociationDecision decision = CLASSIFIER.classify(evidence);
        FabricChatClefCraftResourceAssociationScopeDiagnostics.observeClassification(
                active.key(),
                decision.status()
        );
        return new FabricChatClefCraftResourceAssociationSnapshot(
                binding,
                decision,
                ChatClefDiagnostics.currentClientTickId(),
                Thread.currentThread().getName(),
                className(selectedChain),
                instanceId(selectedChain),
                className(sourceTask),
                instanceId(sourceTask),
                sourceInPath,
                pathRootMatches,
                pathTruncated,
                sourcePath.size(),
                ""
        );
    }

    private static FabricChatClefCraftResourceAssociationSnapshot unavailable(String reason) {
        return unavailable(Optional.empty(), reason);
    }

    private static FabricChatClefCraftResourceAssociationSnapshot unavailable(
            Optional<IronPickaxeAcquisitionScopeBinding> binding,
            String reason) {
        return new FabricChatClefCraftResourceAssociationSnapshot(
                binding,
                new CraftResourceAssociationDecision(
                        CraftResourceAssociationStatus.UNKNOWN,
                        reason
                ),
                ChatClefDiagnostics.currentClientTickId(),
                Thread.currentThread().getName(),
                "UNAVAILABLE",
                "UNAVAILABLE",
                "UNAVAILABLE",
                "UNAVAILABLE",
                false,
                false,
                false,
                0,
                reason
        );
    }

    private static void observeProjectionFailure(
            Optional<IronPickaxeAcquisitionScopeBinding> binding,
            String observationBoundary,
            String reason) {
        if (binding.isPresent()) {
            IronPickaxeAcquisitionScopeBinding active = binding.get();
            FabricChatClefCraftResourceAssociationScopeDiagnostics.observeClassification(
                    active.key(),
                    CraftResourceAssociationStatus.UNKNOWN
            );
            FabricChatClefCraftResourceAssociationScopeDiagnostics.observeObservationGap(
                    active.key(),
                    observationBoundary,
                    reason
            );
        }
    }

    private static String errorName(Throwable error) {
        return error == null ? "UNAVAILABLE" : error.getClass().getName();
    }

    private static boolean containsIdentity(List<Task> path, Task task) {
        if (task == null) {
            return false;
        }
        for (Task candidate : path) {
            if (candidate == task) {
                return true;
            }
        }
        return false;
    }

    private static String className(Object value) {
        return value == null ? "UNAVAILABLE" : value.getClass().getName();
    }

    private static String instanceId(Object value) {
        return value == null
                ? "UNAVAILABLE"
                : value.getClass().getName()
                        + "@"
                        + Integer.toHexString(System.identityHashCode(value));
    }

    private static String stringField(Object[] fields, String key) {
        Object value = field(fields, key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long longField(Object[] fields, String key, long fallback) {
        Object value = field(fields, key);
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Object field(Object[] fields, String key) {
        if (fields == null) {
            return null;
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        return null;
    }
}
