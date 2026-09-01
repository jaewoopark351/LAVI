package lavi.minecraft.diagnostics.crafting.acquisition.scope;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceArtifactProof;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementObserver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

//20260901_kpopmodder: Own exact activation and bounded identity state for iron-pickaxe diagnostics.
public final class IronPickaxeAcquisitionScopeDiagnostics {
    private static final int MAX_PENDING_REQUIREMENTS = 8;
    private static final IronPickaxeAcquisitionScopeRegistry REGISTRY =
            new IronPickaxeAcquisitionScopeRegistry();
    private static final Map<IronPickaxeAcquisitionCommandKey, CraftResourceRequirementDecision>
            PENDING_REQUIREMENTS = new LinkedHashMap<>();
    private static final Map<IronPickaxeAcquisitionScopeKey, IronPickaxeAcquisitionScopeBinding>
            ACTIVE_BINDINGS = new LinkedHashMap<>();
    private static boolean requirementObserverInstalled;

    private IronPickaxeAcquisitionScopeDiagnostics() {
    }

    public static synchronized void installRequirementObserver() {
        if (requirementObserverInstalled) {
            return;
        }
        CraftResourceRequirementObserver.install(
                IronPickaxeAcquisitionScopeDiagnostics::captureRequirement
        );
        requirementObserverInstalled = true;
    }

    public static void captureRequirement(
            String requestedItem,
            int requestedCount,
            int currentItemCount,
            int targetItemCount
    ) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        if (!"minecraft:iron_pickaxe".equals(requestedItem)) {
            return;
        }
        Object[] contextFields = ChatClefDiagnostics.withCommandContextFields();
        boolean contextAvailable = booleanField(contextFields, "commandContextAvailable");
        String commandText = stringField(contextFields, "commandText");
        if (!contextAvailable || !IronPickaxeAcquisitionCommandMatcher.matches(commandText)) {
            return;
        }
        IronPickaxeAcquisitionCommandKey commandKey = new IronPickaxeAcquisitionCommandKey(
                stringField(contextFields, "commandSessionId"),
                longField(contextFields, "commandConnectionGeneration", -1L),
                stringField(contextFields, "commandRequestId"),
                stringField(contextFields, "commandCorrelationId")
        );
        CraftResourceRequirementDecision decision = CraftResourceRequirementDecision.fromCapturedDecision(
                requestedItem,
                OptionalInt.of(requestedCount),
                OptionalInt.of(currentItemCount),
                OptionalInt.of(targetItemCount),
                OptionalInt.of(requestedCount),
                CraftResourceArtifactProof.UNVERIFIED,
                OptionalInt.empty(),
                OptionalInt.empty(),
                "UNAVAILABLE",
                OptionalInt.empty()
        );
        synchronized (IronPickaxeAcquisitionScopeDiagnostics.class) {
            if (!PENDING_REQUIREMENTS.containsKey(commandKey)
                    && PENDING_REQUIREMENTS.size() >= MAX_PENDING_REQUIREMENTS) {
                return;
            }
            PENDING_REQUIREMENTS.put(commandKey, decision);
        }
    }

    public static synchronized IronPickaxeAcquisitionActivation activate(
            boolean commandOwnedRoot,
            String normalizedCommand,
            String commandSessionId,
            long commandConnectionGeneration,
            String commandRequestId,
            String commandCorrelationId,
            String rootAssignmentId,
            long rootGeneration,
            String boundRootTaskInstanceId,
            Object boundRootTask,
            String rootTaskClass,
            long clientTick,
            long monotonicNanos
    ) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return unavailable("DIAGNOSTICS_OFF");
        }
        if (!IronPickaxeAcquisitionCommandMatcher.matches(normalizedCommand)) {
            return unavailable("COMMAND_OUT_OF_SCOPE");
        }
        if (!commandOwnedRoot || boundRootTask == null) {
            return unavailable("COMMAND_ROOT_NOT_PROVEN");
        }
        IronPickaxeAcquisitionCommandKey commandKey = new IronPickaxeAcquisitionCommandKey(
                commandSessionId,
                commandConnectionGeneration,
                commandRequestId,
                commandCorrelationId
        );
        CraftResourceRequirementDecision captured = PENDING_REQUIREMENTS.remove(commandKey);
        boolean requirementAvailable = captured != null;
        CraftResourceRequirementDecision requirement = requirementAvailable
                ? captured
                : unavailableRequirement();
        IronPickaxeAcquisitionScopeKey scopeKey = new IronPickaxeAcquisitionScopeKey(
                commandSessionId,
                commandConnectionGeneration,
                commandRequestId,
                commandCorrelationId,
                rootAssignmentId,
                rootGeneration,
                boundRootTaskInstanceId
        );
        IronPickaxeAcquisitionScopeDecision decision = REGISTRY.activate(
                true,
                "minecraft:iron_pickaxe",
                scopeKey,
                clientTick,
                monotonicNanos / 1_000_000L
        );
        if (!decision.activated()) {
            return new IronPickaxeAcquisitionActivation(decision, Optional.empty(), requirementAvailable);
        }
        IronPickaxeAcquisitionScopeBinding binding = new IronPickaxeAcquisitionScopeBinding(
                scopeKey,
                boundRootTask,
                rootTaskClass,
                requirement,
                clientTick,
                monotonicNanos
        );
        ACTIVE_BINDINGS.put(scopeKey, binding);
        return new IronPickaxeAcquisitionActivation(decision, Optional.of(binding), requirementAvailable);
    }

    public static synchronized Optional<IronPickaxeAcquisitionScopeBinding> activeBinding(
            String commandSessionId,
            long commandConnectionGeneration,
            String commandRequestId,
            String commandCorrelationId
    ) {
        return ACTIVE_BINDINGS.values().stream()
                .filter(binding -> sameCommand(
                        binding.key(),
                        commandSessionId,
                        commandConnectionGeneration,
                        commandRequestId,
                        commandCorrelationId
                ))
                .findFirst();
    }

    public static synchronized Optional<IronPickaxeAcquisitionScopeBinding> activeBinding(
            IronPickaxeAcquisitionScopeKey key
    ) {
        return Optional.ofNullable(ACTIVE_BINDINGS.get(key));
    }

    public static synchronized List<IronPickaxeAcquisitionScopeBinding> activeBindings() {
        return List.copyOf(ACTIVE_BINDINGS.values());
    }

    public static synchronized void observeRetention(
            long clientTick,
            long monotonicNanos
    ) {
        REGISTRY.expireTombstones(clientTick, monotonicNanos / 1_000_000L);
    }

    public static synchronized boolean retire(
            IronPickaxeAcquisitionScopeKey key,
            long clientTick,
            long monotonicNanos
    ) {
        ACTIVE_BINDINGS.remove(key);
        return REGISTRY.retire(key, clientTick, monotonicNanos / 1_000_000L);
    }

    public static synchronized void clearForModeOff() {
        PENDING_REQUIREMENTS.clear();
        ACTIVE_BINDINGS.clear();
        REGISTRY.clearForModeOff();
    }

    public static IronPickaxeAcquisitionScopeSnapshot snapshot() {
        return REGISTRY.snapshot();
    }

    private static IronPickaxeAcquisitionActivation unavailable(String reason) {
        return new IronPickaxeAcquisitionActivation(
                new IronPickaxeAcquisitionScopeDecision(false, reason, null),
                Optional.empty(),
                false
        );
    }

    private static CraftResourceRequirementDecision unavailableRequirement() {
        return CraftResourceRequirementDecision.fromCapturedDecision(
                "minecraft:iron_pickaxe",
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                CraftResourceArtifactProof.UNVERIFIED,
                OptionalInt.empty(),
                OptionalInt.empty(),
                "UNAVAILABLE",
                OptionalInt.empty()
        );
    }

    private static boolean sameCommand(
            IronPickaxeAcquisitionScopeKey key,
            String sessionId,
            long generation,
            String requestId,
            String correlationId
    ) {
        return key.commandConnectionGeneration() == generation
                && safeEquals(key.commandSessionId(), sessionId)
                && safeEquals(key.commandRequestId(), requestId)
                && safeEquals(key.commandCorrelationId(), correlationId);
    }

    private static boolean safeEquals(String first, String second) {
        return first != null && first.equals(second);
    }

    private static String stringField(Object[] fields, String key) {
        Object value = field(fields, key);
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean booleanField(Object[] fields, String key) {
        Object value = field(fields, key);
        return value instanceof Boolean && (Boolean) value;
    }

    private static long longField(Object[] fields, String key, long fallback) {
        Object value = field(fields, key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
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
