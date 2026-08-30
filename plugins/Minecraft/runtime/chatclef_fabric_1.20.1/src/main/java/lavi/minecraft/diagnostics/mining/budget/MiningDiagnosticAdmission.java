package lavi.minecraft.diagnostics.mining.budget;

//20260830_kpopmodder: Carry one immutable mining budget admission decision.
public record MiningDiagnosticAdmission(boolean admitted,
                                        boolean reportSessionCap,
                                        String reason,
                                        String correlationKey,
                                        int correlationDetailEmissions,
                                        int correlationDetailLimit,
                                        MiningDiagnosticBudgetSnapshot snapshot) {
}
