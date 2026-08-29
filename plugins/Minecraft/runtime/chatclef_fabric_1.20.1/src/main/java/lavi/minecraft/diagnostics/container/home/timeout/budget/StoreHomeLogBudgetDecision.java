package lavi.minecraft.diagnostics.container.home.timeout.budget;

//20260829_kpopmodder: Expose one immutable STORE_HOME session-budget reservation result.
public record StoreHomeLogBudgetDecision(
        boolean emitOriginal,
        boolean emitCap,
        int emittedCount,
        boolean capEventEmitted,
        int suppressedCount,
        String suppressedByEvent,
        String firstSuppressedEvent,
        String lastSuppressedEvent,
        boolean terminalReservationAvailable,
        boolean exceptionReservationAvailable) {
}
