package lavi.minecraft.diagnostics.postplace;

import net.minecraft.util.math.BlockPos;

//20260731_kpopmodder: Move post-place container intent state out of the generic diagnostic facade.
public final class PostPlaceContainerOpenIntent {
    private final long operationId;
    private final String containerType;
    private final BlockPos targetPosition;
    private final String targetBlockState;
    private final long startClientTickId;
    private int attemptCount;
    private String lastInteractResult = "unavailable";
    private boolean guiOpened;
    private boolean timedOut;
    private boolean warningLogged;

    PostPlaceContainerOpenIntent(long operationId,
                                 String containerType,
                                 BlockPos targetPosition,
                                 String targetBlockState,
                                 long startClientTickId) {
        this.operationId = operationId;
        this.containerType = containerType;
        this.targetPosition = targetPosition;
        this.targetBlockState = targetBlockState;
        this.startClientTickId = startClientTickId;
    }

    public long operationId() {
        return operationId;
    }

    public String containerType() {
        return containerType;
    }

    public BlockPos targetPosition() {
        return targetPosition;
    }

    public String targetBlockState() {
        return targetBlockState;
    }

    public long startClientTickId() {
        return startClientTickId;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public String lastInteractResult() {
        return lastInteractResult;
    }

    boolean guiOpened() {
        return guiOpened;
    }

    boolean timedOut() {
        return timedOut;
    }

    boolean warningLogged() {
        return warningLogged;
    }

    void incrementAttemptCount() {
        attemptCount++;
    }

    void setLastInteractResult(String lastInteractResult) {
        this.lastInteractResult = lastInteractResult;
    }

    void markGuiOpened() {
        guiOpened = true;
    }

    void markTimedOut() {
        timedOut = true;
    }

    void markWarningLogged() {
        warningLogged = true;
    }
}
