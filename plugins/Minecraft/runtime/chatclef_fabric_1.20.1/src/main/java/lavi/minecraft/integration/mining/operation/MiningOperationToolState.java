package lavi.minecraft.integration.mining.operation;

import java.util.Optional;

public final class MiningOperationToolState {
    private final String operationType;
    private final int targetCount;
    private final int targetInventoryCount;
    private final int targetDurabilityReserve;
    private final int ironPickaxeCount;
    private final int stonePickaxeCount;
    private final Optional<MiningToolCandidate> targetToolCandidate;
    private final boolean targetToolReady;
    private final boolean targetToolHotbarVisible;
    private final Optional<MiningToolCandidate> accessToolCandidate;
    private final boolean accessToolReady;
    private final boolean accessToolHotbarVisible;

    public MiningOperationToolState(
            String operationType,
            int targetCount,
            int targetInventoryCount,
            int targetDurabilityReserve,
            int ironPickaxeCount,
            int stonePickaxeCount,
            Optional<MiningToolCandidate> targetToolCandidate,
            boolean targetToolReady,
            boolean targetToolHotbarVisible,
            Optional<MiningToolCandidate> accessToolCandidate,
            boolean accessToolReady,
            boolean accessToolHotbarVisible
    ) {
        this.operationType = operationType;
        this.targetCount = targetCount;
        this.targetInventoryCount = targetInventoryCount;
        this.targetDurabilityReserve = targetDurabilityReserve;
        this.ironPickaxeCount = ironPickaxeCount;
        this.stonePickaxeCount = stonePickaxeCount;
        this.targetToolCandidate = targetToolCandidate == null ? Optional.empty() : targetToolCandidate;
        this.targetToolReady = targetToolReady;
        this.targetToolHotbarVisible = targetToolHotbarVisible;
        this.accessToolCandidate = accessToolCandidate == null ? Optional.empty() : accessToolCandidate;
        this.accessToolReady = accessToolReady;
        this.accessToolHotbarVisible = accessToolHotbarVisible;
    }

    public String operationType() {
        return operationType;
    }

    public int targetCount() {
        return targetCount;
    }

    public int targetInventoryCount() {
        return targetInventoryCount;
    }

    public int targetDurabilityReserve() {
        return targetDurabilityReserve;
    }

    public int requiredIronPickaxeCount() {
        return targetToolReady ? ironPickaxeCount : Math.max(1, ironPickaxeCount + 1);
    }

    public int requiredStonePickaxeCount() {
        return accessToolReady ? stonePickaxeCount : Math.max(1, stonePickaxeCount + 1);
    }

    public Optional<MiningToolCandidate> targetToolCandidate() {
        return targetToolCandidate;
    }

    public boolean targetToolReady() {
        return targetToolReady;
    }

    public boolean targetToolHotbarVisible() {
        return targetToolHotbarVisible;
    }

    public Optional<MiningToolCandidate> accessToolCandidate() {
        return accessToolCandidate;
    }

    public boolean accessToolReady() {
        return accessToolReady;
    }

    public boolean accessToolHotbarVisible() {
        return accessToolHotbarVisible;
    }

    public boolean ready() {
        return targetToolReady && targetToolHotbarVisible && accessToolReady && accessToolHotbarVisible;
    }

    public MiningToolPreparationStep nextStep() {
        if (!targetToolReady) {
            return MiningToolPreparationStep.ACQUIRE_TARGET_PICKAXE;
        }
        if (!targetToolHotbarVisible) {
            return MiningToolPreparationStep.MOVE_TARGET_PICKAXE_TO_HOTBAR;
        }
        if (!accessToolReady) {
            return MiningToolPreparationStep.ACQUIRE_ACCESS_PICKAXE;
        }
        if (!accessToolHotbarVisible) {
            return MiningToolPreparationStep.MOVE_ACCESS_PICKAXE_TO_HOTBAR;
        }
        return MiningToolPreparationStep.READY;
    }

    public Optional<MiningToolCandidate> candidateFor(MiningToolRole role) {
        return role == MiningToolRole.TARGET ? targetToolCandidate : accessToolCandidate;
    }

    public boolean hotbarVisibleFor(MiningToolRole role) {
        return role == MiningToolRole.TARGET ? targetToolHotbarVisible : accessToolHotbarVisible;
    }

    public int preferredHotbarSlotFor(MiningToolRole role) {
        if (role == MiningToolRole.TARGET) {
            return 0;
        }
        int targetSlot = targetToolCandidate
                .filter(MiningToolCandidate::hotbarVisible)
                .map(candidate -> candidate.slot().getInventorySlot())
                .orElse(-1);
        return targetSlot == 1 ? 2 : 1;
    }

    public String signature() {
        return operationType
                + "|target=" + targetToolReady + "," + targetToolHotbarVisible + "," + candidateSignature(targetToolCandidate)
                + "|access=" + accessToolReady + "," + accessToolHotbarVisible + "," + candidateSignature(accessToolCandidate)
                + "|reserve=" + targetDurabilityReserve
                + "|step=" + nextStep();
    }

    private String candidateSignature(Optional<MiningToolCandidate> candidate) {
        return candidate
                .map(value -> value.item() + "@"
                        + value.slot().getInventorySlot() + "/"
                        + value.slot().getWindowSlot() + ":"
                        + value.remainingDurability() + ":"
                        + value.hotbarVisible())
                .orElse("none");
    }
}
