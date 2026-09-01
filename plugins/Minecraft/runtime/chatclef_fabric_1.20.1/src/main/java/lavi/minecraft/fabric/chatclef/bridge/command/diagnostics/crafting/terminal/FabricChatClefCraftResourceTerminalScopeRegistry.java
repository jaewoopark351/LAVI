package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalKey;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Resolve opaque terminal identities without correlating by request IDs alone.
final class FabricChatClefCraftResourceTerminalScopeRegistry {
    private static final int MAX_ACTIVE = 8;
    private static final int MAX_TOMBSTONES = 8;

    private final Map<CraftResourceTerminalKey, FabricChatClefCraftResourceTerminalScopeBinding>
            bindings = new LinkedHashMap<>();

    synchronized Optional<FabricChatClefCraftResourceTerminalScopeBinding> activate(
            IronPickaxeAcquisitionScopeBinding scopeBinding,
            FabricChatClefCommandContext commandContext
    ) {
        if (scopeBinding == null || commandContext == null) {
            return Optional.empty();
        }
        IronPickaxeAcquisitionScopeKey scopeKey = scopeBinding.key();
        CraftResourceTerminalKey terminalKey = new CraftResourceTerminalKey(
                scopeKey.commandSessionId(),
                scopeKey.commandConnectionGeneration(),
                scopeKey.commandRequestId(),
                scopeKey.commandCorrelationId(),
                scopeKey.rootAssignmentId()
        );
        FabricChatClefCraftResourceTerminalScopeBinding existing = bindings.get(terminalKey);
        if (existing != null) {
            return Optional.of(existing);
        }
        if (activeCount() >= MAX_ACTIVE) {
            return Optional.empty();
        }
        FabricChatClefCraftResourceTerminalScopeBinding created =
                new FabricChatClefCraftResourceTerminalScopeBinding(
                        terminalKey,
                        scopeKey,
                        commandContext,
                        scopeBinding.rootTaskClass(),
                        scopeBinding.activatedAtClientTick(),
                        scopeBinding.activatedAtMonotonicNanos(),
                        false,
                        -1L,
                        -1L,
                        "",
                        "",
                        ""
                );
        bindings.put(terminalKey, created);
        return Optional.of(created);
    }

    synchronized Optional<FabricChatClefCraftResourceTerminalScopeBinding> exact(
            FabricChatClefCommandContext context
    ) {
        if (context == null) {
            return Optional.empty();
        }
        FabricChatClefCraftResourceTerminalScopeBinding match = null;
        for (FabricChatClefCraftResourceTerminalScopeBinding candidate : bindings.values()) {
            CraftResourceTerminalKey key = candidate.terminalKey();
            if (key.commandConnectionGeneration() != context.connectionGeneration()
                    || !same(key.commandSessionId(), context.sessionId())
                    || !same(key.commandRequestId(), context.requestId())
                    || !same(key.commandCorrelationId(), context.correlationId())) {
                continue;
            }
            if (candidate.commandContext() != context) {
                continue;
            }
            if (match != null) {
                return Optional.empty();
            }
            match = candidate;
        }
        return Optional.ofNullable(match);
    }

    synchronized Optional<FabricChatClefCraftResourceTerminalScopeBinding> exact(
            CraftResourceTerminalKey key
    ) {
        return Optional.ofNullable(bindings.get(key));
    }

    synchronized void recordFirstTerminalSource(
            CraftResourceTerminalKey key,
            String sourceEvent
    ) {
        FabricChatClefCraftResourceTerminalScopeBinding current = bindings.get(key);
        if (current != null) {
            bindings.put(key, current.withFirstTerminalSourceEvent(sourceEvent));
        }
    }

    synchronized void recordExceptionEvidence(
            CraftResourceTerminalKey key,
            String exceptionType,
            String exceptionMessage
    ) {
        FabricChatClefCraftResourceTerminalScopeBinding current = bindings.get(key);
        if (current != null) {
            bindings.put(
                    key,
                    current.withExceptionEvidence(exceptionType, exceptionMessage)
            );
        }
    }

    synchronized Optional<FabricChatClefCraftResourceTerminalScopeBinding> markFinalized(
            CraftResourceTerminalKey key,
            long clientTick,
            long monotonicNanos
    ) {
        FabricChatClefCraftResourceTerminalScopeBinding current = bindings.get(key);
        if (current == null) {
            return Optional.empty();
        }
        FabricChatClefCraftResourceTerminalScopeBinding finalized =
                current.finalizedAt(clientTick, monotonicNanos);
        bindings.put(key, finalized);
        evictOldestFinalizedExcept(key);
        return Optional.of(finalized);
    }

    synchronized void expire(
            long clientTick,
            long monotonicNanos,
            List<CraftResourceTerminalKey> pureRegistryExpiredKeys
    ) {
        List<CraftResourceTerminalKey> expired = new ArrayList<>();
        for (Map.Entry<CraftResourceTerminalKey,
                FabricChatClefCraftResourceTerminalScopeBinding> entry : bindings.entrySet()) {
            if (entry.getValue().expired(clientTick, monotonicNanos)) {
                expired.add(entry.getKey());
            }
        }
        if (pureRegistryExpiredKeys != null) {
            expired.addAll(pureRegistryExpiredKeys);
        }
        expired.forEach(bindings::remove);
    }

    synchronized void clear() {
        bindings.clear();
    }

    private int activeCount() {
        int active = 0;
        for (FabricChatClefCraftResourceTerminalScopeBinding binding : bindings.values()) {
            if (!binding.finalized()) {
                active++;
            }
        }
        return active;
    }

    private void evictOldestFinalizedExcept(CraftResourceTerminalKey retainedKey) {
        List<FabricChatClefCraftResourceTerminalScopeBinding> finalized = bindings.values().stream()
                .filter(FabricChatClefCraftResourceTerminalScopeBinding::finalized)
                .toList();
        if (finalized.size() <= MAX_TOMBSTONES) {
            return;
        }
        FabricChatClefCraftResourceTerminalScopeBinding oldest = finalized.stream()
                .filter(candidate -> !candidate.terminalKey().equals(retainedKey))
                .min((first, second) -> Long.compare(
                        first.finalizedAtMonotonicNanos(),
                        second.finalizedAtMonotonicNanos()
                ))
                .orElse(null);
        if (oldest != null) {
            bindings.remove(oldest.terminalKey());
        }
    }

    private static boolean same(String first, String second) {
        return first != null && first.equals(second);
    }
}
