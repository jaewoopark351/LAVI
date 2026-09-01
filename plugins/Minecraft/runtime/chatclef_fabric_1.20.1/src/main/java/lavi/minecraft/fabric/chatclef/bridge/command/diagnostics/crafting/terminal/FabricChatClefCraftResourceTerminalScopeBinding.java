package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalKey;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalTextBound;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

import java.util.Objects;

//20260901_kpopmodder: Retain only bounded diagnostic identity needed after command unbind.
public record FabricChatClefCraftResourceTerminalScopeBinding(
        CraftResourceTerminalKey terminalKey,
        IronPickaxeAcquisitionScopeKey scopeKey,
        FabricChatClefCommandContext commandContext,
        String rootTaskClass,
        long activatedAtClientTick,
        long activatedAtMonotonicNanos,
        boolean finalized,
        long finalizedAtClientTick,
        long finalizedAtMonotonicNanos,
        String firstTerminalSourceEvent,
        String primaryCauseExceptionType,
        String primaryCauseExceptionMessage
) {
    private static final long RETENTION_TICKS = 200L;
    private static final long RETENTION_NANOS = 10_000_000_000L;

    public FabricChatClefCraftResourceTerminalScopeBinding {
        Objects.requireNonNull(terminalKey, "terminalKey");
        Objects.requireNonNull(scopeKey, "scopeKey");
        Objects.requireNonNull(commandContext, "commandContext");
        rootTaskClass = unavailableIfBlank(rootTaskClass);
        firstTerminalSourceEvent = safe(firstTerminalSourceEvent);
        primaryCauseExceptionType = safe(primaryCauseExceptionType);
        primaryCauseExceptionMessage = safe(primaryCauseExceptionMessage);
    }

    FabricChatClefCraftResourceTerminalScopeBinding withFirstTerminalSourceEvent(
            String sourceEvent
    ) {
        if (!firstTerminalSourceEvent.isBlank()) {
            return this;
        }
        return new FabricChatClefCraftResourceTerminalScopeBinding(
                terminalKey,
                scopeKey,
                commandContext,
                rootTaskClass,
                activatedAtClientTick,
                activatedAtMonotonicNanos,
                finalized,
                finalizedAtClientTick,
                finalizedAtMonotonicNanos,
                unavailableIfBlank(sourceEvent),
                primaryCauseExceptionType,
                primaryCauseExceptionMessage
        );
    }

    FabricChatClefCraftResourceTerminalScopeBinding withExceptionEvidence(
            String exceptionType,
            String exceptionMessage
    ) {
        if (!primaryCauseExceptionType.isBlank()) {
            return this;
        }
        return new FabricChatClefCraftResourceTerminalScopeBinding(
                terminalKey,
                scopeKey,
                commandContext,
                rootTaskClass,
                activatedAtClientTick,
                activatedAtMonotonicNanos,
                finalized,
                finalizedAtClientTick,
                finalizedAtMonotonicNanos,
                firstTerminalSourceEvent,
                unavailableIfBlank(exceptionType),
                unavailableIfBlank(exceptionMessage)
        );
    }

    FabricChatClefCraftResourceTerminalScopeBinding finalizedAt(
            long clientTick,
            long monotonicNanos
    ) {
        return new FabricChatClefCraftResourceTerminalScopeBinding(
                terminalKey,
                scopeKey,
                commandContext,
                rootTaskClass,
                activatedAtClientTick,
                activatedAtMonotonicNanos,
                true,
                clientTick,
                monotonicNanos,
                firstTerminalSourceEvent,
                primaryCauseExceptionType,
                primaryCauseExceptionMessage
        );
    }

    boolean expired(long clientTick, long monotonicNanos) {
        if (!finalized) {
            return false;
        }
        return elapsedAtLeast(clientTick, finalizedAtClientTick, RETENTION_TICKS)
                || elapsedAtLeast(monotonicNanos, finalizedAtMonotonicNanos, RETENTION_NANOS);
    }

    private static boolean elapsedAtLeast(long current, long start, long bound) {
        return current >= start && current - start >= bound;
    }

    private static String unavailableIfBlank(String value) {
        return CraftResourceTerminalTextBound.unavailableIfBlank(value);
    }

    private static String safe(String value) {
        return CraftResourceTerminalTextBound.safe(value);
    }
}
