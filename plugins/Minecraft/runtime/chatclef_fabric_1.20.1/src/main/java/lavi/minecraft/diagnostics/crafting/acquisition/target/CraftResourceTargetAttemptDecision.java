package lavi.minecraft.diagnostics.crafting.acquisition.target;

import java.util.Objects;
import java.util.OptionalLong;

//20260901_kpopmodder: Report whether behavior evidence, rather than log admission, began an attempt.
public record CraftResourceTargetAttemptDecision(
        boolean startedNewAttempt,
        OptionalLong targetAttemptSequence,
        boolean detailEligible) {

    public CraftResourceTargetAttemptDecision {
        targetAttemptSequence = Objects.requireNonNull(
                targetAttemptSequence,
                "targetAttemptSequence"
        );
    }
}
