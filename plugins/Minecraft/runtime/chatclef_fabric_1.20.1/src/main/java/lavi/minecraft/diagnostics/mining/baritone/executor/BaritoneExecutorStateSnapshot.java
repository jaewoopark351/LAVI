package lavi.minecraft.diagnostics.mining.baritone.executor;

//20260830_kpopmodder: Store only one executor and its permit-gated path detail values.
final class BaritoneExecutorStateSnapshot {
    final boolean present;
    final String identity;
    final String type;
    final int position;
    final String positionText;
    final String failed;
    final String finished;
    final String pathIdentity;
    final String pathType;
    final String pathLength;
    final String pathMovementCount;
    final String pathPositionCount;
    final String pathSrc;
    final String pathDest;
    final String pathGoal;

    BaritoneExecutorStateSnapshot(boolean present,
                                  String identity,
                                  String type,
                                  int position,
                                  String positionText,
                                  String failed,
                                  String finished,
                                  String pathIdentity,
                                  String pathType,
                                  String pathLength,
                                  String pathMovementCount,
                                  String pathPositionCount,
                                  String pathSrc,
                                  String pathDest,
                                  String pathGoal) {
        this.present = present;
        this.identity = identity;
        this.type = type;
        this.position = position;
        this.positionText = positionText;
        this.failed = failed;
        this.finished = finished;
        this.pathIdentity = pathIdentity;
        this.pathType = pathType;
        this.pathLength = pathLength;
        this.pathMovementCount = pathMovementCount;
        this.pathPositionCount = pathPositionCount;
        this.pathSrc = pathSrc;
        this.pathDest = pathDest;
        this.pathGoal = pathGoal;
    }
}
