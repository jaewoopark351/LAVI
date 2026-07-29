package adris.altoclef.tasks.movement.escape;

import java.util.Objects;
import java.util.Optional;

//20260729_kpopmodder: Added this result object so plan selection and failure diagnostics stay tied together.
public class EscapePlanSearchResult {
    private final EscapePlan plan;
    private final String failureReason;

    private EscapePlanSearchResult(EscapePlan plan, String failureReason) {
        this.plan = plan;
        this.failureReason = failureReason;
    }

    static EscapePlanSearchResult selected(EscapePlan plan) {
        return new EscapePlanSearchResult(Objects.requireNonNull(plan), null);
    }

    static EscapePlanSearchResult unavailable(String failureReason) {
        return new EscapePlanSearchResult(null, Objects.requireNonNull(failureReason));
    }

    public Optional<EscapePlan> getPlan() {
        return Optional.ofNullable(plan);
    }

    public boolean hasPlan() {
        return plan != null;
    }

    public String describeFailure() {
        if (plan != null) {
            return "plan selected: " + plan.describe();
        }
        return failureReason;
    }
}
