package lavi.minecraft.diagnostics.interaction;

import adris.altoclef.tasksystem.Task;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

//20260805_kpopmodder: Correlate one interactBlock HEAD/RETURN pair for diagnostics only.
public final class BlockInteractionContext {
    private final long interactionId;
    private final long startClientTickId;
    private final BlockInteractionTargetInfo target;
    private final String hand;
    private final String hitSide;
    private final String hitType;
    private final BlockInteractionScreenSnapshot screenBefore;
    private final boolean matchedHead;
    private final Task sourceTask;

    public BlockInteractionContext(long interactionId,
                                   long startClientTickId,
                                   BlockInteractionTargetInfo target,
                                   Object hand,
                                   BlockHitResult hitResult,
                                   BlockInteractionScreenSnapshot screenBefore,
                                   boolean matchedHead) {
        this(
                interactionId,
                startClientTickId,
                target,
                hand,
                hitResult,
                screenBefore,
                matchedHead,
                null
        );
    }

    public BlockInteractionContext(long interactionId,
                                   long startClientTickId,
                                   BlockInteractionTargetInfo target,
                                   Object hand,
                                   BlockHitResult hitResult,
                                   BlockInteractionScreenSnapshot screenBefore,
                                   boolean matchedHead,
                                   Task sourceTask) {
        this.interactionId = interactionId;
        this.startClientTickId = startClientTickId;
        this.target = target;
        this.hand = value(hand);
        this.hitSide = hitResult == null ? "unavailable" : value(hitResult.getSide());
        this.hitType = hitResult == null ? "unavailable" : value(hitResult.getType());
        this.screenBefore = screenBefore;
        this.matchedHead = matchedHead;
        this.sourceTask = sourceTask;
    }

    public long interactionId() {
        return interactionId;
    }

    public long startClientTickId() {
        return startClientTickId;
    }

    public boolean screenOpeningTarget() {
        return target != null && target.screenOpeningTarget();
    }

    public String targetKind() {
        return target == null ? "unavailable" : target.targetKind();
    }

    public String targetBlockId() {
        return target == null ? "unavailable" : target.targetBlockId();
    }

    public String targetBlockDescription() {
        return target == null ? "unavailable" : target.targetBlockDescription();
    }

    public String targetBlockState() {
        return target == null ? "unavailable" : target.targetBlockState();
    }

    public BlockPos targetPosition() {
        return target == null ? null : target.targetPosition();
    }

    public String hand() {
        return hand;
    }

    public String hitSide() {
        return hitSide;
    }

    public String hitType() {
        return hitType;
    }

    public BlockInteractionScreenSnapshot screenBefore() {
        return screenBefore;
    }

    public boolean matchedHead() {
        return matchedHead;
    }

    public Task sourceTask() {
        return sourceTask;
    }

    public boolean targetMatches(BlockPos targetPosition) {
        BlockPos ownTarget = targetPosition();
        return ownTarget != null && ownTarget.equals(targetPosition);
    }

    private static String value(Object rawValue) {
        return rawValue == null ? "unavailable" : String.valueOf(rawValue);
    }
}
