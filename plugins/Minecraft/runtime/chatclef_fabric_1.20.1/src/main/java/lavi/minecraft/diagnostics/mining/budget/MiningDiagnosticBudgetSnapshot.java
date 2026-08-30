package lavi.minecraft.diagnostics.mining.budget;

//20260830_kpopmodder: Expose one immutable view of bounded mining session consumption.
public record MiningDiagnosticBudgetSnapshot(int sessionEmissions,
                                             int detailEmissions,
                                             int criticalEmissions,
                                             int capSignalEmissions,
                                             int sessionHardCap,
                                             int sessionDetailLimit,
                                             int reservedCriticalEvents,
                                             boolean sessionCapSignalClaimed) {
}
