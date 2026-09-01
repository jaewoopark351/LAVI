package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.util.MiningRequirement;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260806_kpopmodder: Observe block blacklist state changes without changing blacklist thresholds.
final class BlockBlacklistDiagnostics {
    private BlockBlacklistDiagnostics() {
    }

    static boolean log(AltoClef mod,
                    Object item,
                    boolean entryCreated,
                    int failureCountBefore,
                    int failureCountAfter,
                    int allowedFailuresBefore,
                    int requestedAllowedFailures,
                    int allowedFailuresAfter,
                    boolean unreachableBefore,
                    boolean unreachableAfter,
                    double currentDistanceSq,
                    double bestDistanceSqBefore,
                    double bestDistanceSqAfter,
                    MiningRequirement currentMiningRequirement,
                    MiningRequirement bestToolBefore,
                    MiningRequirement bestToolAfter,
                    boolean resetApplied,
                    String resetReason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || !(item instanceof BlockPos target)) {
            return false;
        }
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BLOCK_BLACKLIST_STATE_CHANGED",
                ChatClefDiagnostics.blockPos(target),
                Integer.toString(failureCountAfter),
                Integer.toString(allowedFailuresAfter),
                Boolean.toString(unreachableAfter),
                Boolean.toString(resetApplied),
                resetReason
        );
        return MiningDiagnosticEmitter.emitLazyWithPhysicalOutcome(
                "BLOCK_BLACKLIST_STATE_CHANGED", "block_blacklist_state_changed", null,
                "block_blacklist|" + ChatClefDiagnostics.blockPos(target),
                fingerprint,
                () -> new Object[]{
                        "owner", "block_blacklist",
                        "trigger", "blacklist_item_updated",
                        "targetPosition", ChatClefDiagnostics.blockPos(target),
                        "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target)),
                        "entryCreated", entryCreated,
                        "failureCountBefore", failureCountBefore,
                        "failureCountAfter", failureCountAfter,
                        "allowedFailuresBefore", allowedFailuresBefore,
                        "requestedAllowedFailures", requestedAllowedFailures,
                        "allowedFailuresAfter", allowedFailuresAfter,
                        "unreachableBefore", unreachableBefore,
                        "unreachableAfter", unreachableAfter,
                        "currentDistanceSq", currentDistanceSq,
                        "bestDistanceSqBefore", bestDistanceSqBefore,
                        "bestDistanceSqAfter", bestDistanceSqAfter,
                        "currentMiningRequirement", currentMiningRequirement,
                        "bestToolBefore", bestToolBefore,
                        "bestToolAfter", bestToolAfter,
                        "resetApplied", resetApplied,
                        "resetReason", resetReason
                });
    }
}
