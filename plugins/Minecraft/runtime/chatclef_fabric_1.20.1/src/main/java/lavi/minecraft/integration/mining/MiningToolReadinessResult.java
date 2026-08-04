package lavi.minecraft.integration.mining;

import adris.altoclef.util.MiningRequirement;

//20260805_kpopmodder: Keep mining readiness result data separate from target-aware readiness evaluation logic.
public class MiningToolReadinessResult {
    private final MiningRequirement requirement;
    private final boolean broadRequirementMet;
    private final boolean selectableToolPresent;
    private final boolean rejectedBySavePolicy;
    private final boolean requiresAcquisition;

    protected MiningToolReadinessResult(
            MiningRequirement requirement,
            boolean broadRequirementMet,
            boolean selectableToolPresent,
            boolean rejectedBySavePolicy,
            boolean requiresAcquisition
    ) {
        this.requirement = requirement;
        this.broadRequirementMet = broadRequirementMet;
        this.selectableToolPresent = selectableToolPresent;
        this.rejectedBySavePolicy = rejectedBySavePolicy;
        this.requiresAcquisition = requiresAcquisition;
    }

    public MiningRequirement requirement() {
        return requirement;
    }

    public boolean broadRequirementMet() {
        return broadRequirementMet;
    }

    public boolean selectableToolPresent() {
        return selectableToolPresent;
    }

    public boolean rejectedBySavePolicy() {
        return rejectedBySavePolicy;
    }

    public boolean requiresAcquisition() {
        return requiresAcquisition;
    }
}
