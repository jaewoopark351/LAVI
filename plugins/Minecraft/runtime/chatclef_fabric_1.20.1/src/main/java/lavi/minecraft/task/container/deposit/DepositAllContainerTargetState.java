package lavi.minecraft.task.container.deposit;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

//20260826_kpopmodder: Added value-based sticky target ownership for deposit_all.
public final class DepositAllContainerTargetState {
    private BlockPos selectedTarget;

    public Optional<BlockPos> selectedTarget() {
        return Optional.ofNullable(selectedTarget);
    }

    public boolean select(BlockPos candidate) {
        if (candidate == null) {
            return false;
        }
        BlockPos immutableCandidate = candidate.toImmutable();
        if (immutableCandidate.equals(selectedTarget)) {
            return false;
        }
        selectedTarget = immutableCandidate;
        return true;
    }

    public boolean selectIfWithin(BlockPos candidate, Vec3d origin, double range) {
        return candidate != null && candidate.isWithinDistance(origin, range) && select(candidate);
    }

    public boolean matches(BlockPos candidate) {
        return selectedTarget != null && selectedTarget.equals(candidate);
    }

    public boolean isSelectedWithin(Vec3d origin, double range) {
        return selectedTarget != null && selectedTarget.isWithinDistance(origin, range);
    }

    public boolean clear() {
        if (selectedTarget == null) {
            return false;
        }
        selectedTarget = null;
        return true;
    }
}
