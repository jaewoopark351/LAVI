package lavi.minecraft.diagnostics.mining.gold;

import java.lang.ref.WeakReference;

//20260913_kpopmodder: Keep cumulative tool-loop evidence independent of child restarts and output admission.
public final class GoldToolLoopState {
    private Boolean lastReady;
    private long preparationReentries;
    private long readyReturns;
    private long childRuns;
    private long childStops;
    private long preparationInterruptions;
    private WeakReference<Object> activeChild = new WeakReference<>(null);
    private boolean childStopObserved;
    private String currentTarget = "UNAVAILABLE";
    private long sameTargetChildRuns;
    private long sameTargetPreparationInterruptions;
    private long latestEquipAttempt = -1;
    private String latestEquip = "UNAVAILABLE";
    private String firstCausalLink = "UNAVAILABLE";
    private String latestPreparation = "UNAVAILABLE";
    private long linkedEquipAttempt = -1;
    private String linkedEquip = "UNAVAILABLE";
    private Integer previousRawGoldCount;
    private int rawGoldNetChange;
    private long lastQuantityChangeTick = -1;

    public String preparation(boolean ready, String detail, int rawGoldCount, long tick) {
        boolean reentry = Boolean.TRUE.equals(lastReady) && !ready;
        boolean becameReady = Boolean.FALSE.equals(lastReady) && ready;
        if (reentry) {
            preparationReentries++;
            latestPreparation = bound(detail);
            linkedEquipAttempt = latestEquipAttempt;
            linkedEquip = latestEquip;
        }
        if (becameReady) readyReturns++;
        if (previousRawGoldCount != null) {
            int delta = rawGoldCount - previousRawGoldCount;
            rawGoldNetChange += delta;
            if (delta != 0) lastQuantityChangeTick = tick;
        } else {
            lastQuantityChangeTick = tick;
        }
        previousRawGoldCount = rawGoldCount;
        lastReady = ready;
        return reentry ? "READY_TO_PREPARATION" : becameReady ? "PREPARATION_TO_READY" : ready ? "READY" : "PREPARATION";
    }

    public void equip(long attempt, String details) {
        latestEquipAttempt = attempt;
        latestEquip = bound(details);
    }

    public long childStarted(Object child) {
        return childStarted(child, "UNAVAILABLE");
    }

    public long childStarted(Object child, String target) {
        String observedTarget = bound(target);
        if (!currentTarget.equals(observedTarget)) {
            currentTarget = observedTarget;
            sameTargetChildRuns = 0;
            sameTargetPreparationInterruptions = 0;
        }
        activeChild = new WeakReference<>(child);
        childStopObserved = false;
        sameTargetChildRuns++;
        return ++childRuns;
    }

    public boolean childStopped(Object child, boolean interruptedForPreparation, String details) {
        if (child != activeChild.get() || childStopObserved) return false;
        childStopObserved = true;
        childStops++;
        if (interruptedForPreparation) {
            preparationInterruptions++;
            sameTargetPreparationInterruptions++;
            if (firstCausalLink.equals("UNAVAILABLE") && linkedEquipAttempt >= 0) {
                firstCausalLink = bound("equipAttempt=" + linkedEquipAttempt + ";" + linkedEquip
                        + ";preparation=" + latestPreparation + ";stop=" + details);
            }
        }
        return true;
    }

    public long preparationReentries() { return preparationReentries; }
    public long childRuns() { return childRuns; }
    public long preparationInterruptions() { return preparationInterruptions; }
    public String firstCausalLink() { return firstCausalLink; }
    public long latestEquipAttempt() { return latestEquipAttempt; }

    public Object[] fields(long tick) {
        return new Object[]{"readyToPreparationCount", preparationReentries,
                "preparationToReadyCount", readyReturns, "destroyChildRunCount", childRuns,
                "destroyChildStopCount", childStops, "preparationInterruptCount", preparationInterruptions,
                "currentTarget", currentTarget, "sameTargetConsecutiveChildRuns", sameTargetChildRuns,
                "sameTargetConsecutivePreparationInterruptions", sameTargetPreparationInterruptions,
                "latestEquipAttemptId", latestEquipAttempt, "firstCausalLink", firstCausalLink,
                "equipToPreparationAssociation", "OBSERVED_SEQUENCE_NOT_INDEPENDENT_CAUSAL_PROOF",
                "rawGoldSignedQuantityChange", rawGoldNetChange,
                "rawGoldQuantityUnchangedTicks", lastQuantityChangeTick < 0 ? "UNAVAILABLE" : tick - lastQuantityChangeTick,
                "quantityChangeAttribution", "UNVERIFIED_SOURCE_DROP_PICKUP",
                "blockProgressCoverage", "SEPARATE_DESTROY_OBSERVATION"};
    }

    private static String bound(String value) {
        return value == null ? "UNAVAILABLE" : value.substring(0, Math.min(value.length(), 1800));
    }
}
