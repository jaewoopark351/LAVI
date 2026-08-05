package lavi.minecraft.diagnostics.interaction;

import net.minecraft.util.math.BlockPos;

//20260805_kpopmodder: Store observed block target identity without owning interaction behavior.
public final class BlockInteractionTargetInfo {
    private final boolean screenOpeningTarget;
    private final String targetKind;
    private final String targetBlockId;
    private final String targetBlockDescription;
    private final String targetBlockState;
    private final BlockPos targetPosition;

    public BlockInteractionTargetInfo(boolean screenOpeningTarget,
                                      String targetKind,
                                      String targetBlockId,
                                      String targetBlockDescription,
                                      String targetBlockState,
                                      BlockPos targetPosition) {
        this.screenOpeningTarget = screenOpeningTarget;
        this.targetKind = targetKind;
        this.targetBlockId = targetBlockId;
        this.targetBlockDescription = targetBlockDescription;
        this.targetBlockState = targetBlockState;
        this.targetPosition = targetPosition == null ? null : targetPosition.toImmutable();
    }

    public boolean screenOpeningTarget() {
        return screenOpeningTarget;
    }

    public String targetKind() {
        return targetKind;
    }

    public String targetBlockId() {
        return targetBlockId;
    }

    public String targetBlockDescription() {
        return targetBlockDescription;
    }

    public String targetBlockState() {
        return targetBlockState;
    }

    public BlockPos targetPosition() {
        return targetPosition;
    }
}
