//#if MC == 12001
package lavi.minecraft.integration.mining.operation.hotbar.lifecycle;
import java.util.Optional;
//20260913_kpopmodder: Bound unresolved placement by active evaluations, never by defense suspension time.
public final class MiningHotbarProgress {
    public static final int MAX_BLOCKED_EVALUATIONS = 100;
    private int blockedEvaluations;
    private String failure;
    public void blocked(String reason) {
        if (failure == null && ++blockedEvaluations >= MAX_BLOCKED_EVALUATIONS) failure = reason;
    }
    public void confirmationFailed() { if (failure == null) failure = "HOTBAR_CONFIRMATION_TIMEOUT"; }
    public void confirmedProgress() { if (failure == null) blockedEvaluations = 0; }
    public Optional<String> failure() { return Optional.ofNullable(failure); }
    public int blockedEvaluations() { return blockedEvaluations; }
}
//#endif
