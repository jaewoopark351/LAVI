package adris.altoclef.trackers.blacklisting;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import lavi.minecraft.diagnostics.mining.MiningPathDiagnostics;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;

/**
 * Sometimes we will try to access something and fail TOO many times.
 * <p>
 * This lets us know that a block is unreachable, and will ignore it from the search intelligently.
 */
public abstract class AbstractObjectBlacklist<T> {

    private final HashMap<T, BlacklistEntry> entries = new HashMap<>();

    public void blackListItem(AltoClef mod, T item, int numberOfFailuresAllowed) {
        boolean entryCreated = !entries.containsKey(item);
        if (entryCreated) {
            BlacklistEntry entry = new BlacklistEntry();
            entry.numberOfFailuresAllowed = numberOfFailuresAllowed;
            entry.numberOfFailures = 0;
            entry.bestDistanceSq = Double.POSITIVE_INFINITY;
            entry.bestTool = MiningRequirement.HAND;
            entries.put(item, entry);
        }
        BlacklistEntry entry = entries.get(item);
        int failureCountBefore = entry.numberOfFailures;
        int allowedFailuresBefore = entry.numberOfFailuresAllowed;
        boolean unreachableBefore = entry.numberOfFailures > entry.numberOfFailuresAllowed;
        double bestDistanceSqBefore = entry.bestDistanceSq;
        MiningRequirement bestToolBefore = entry.bestTool;
        double newDistance = getPos(item).squaredDistanceTo(mod.getPlayer().getPos());
        MiningRequirement newTool = StorageHelper.getCurrentMiningRequirement();
        // For distance, add a slight threshold so it doesn't reset EVERY time we move a tiny bit closer.
        boolean toolImproved = newTool.ordinal() > entry.bestTool.ordinal();
        boolean distanceImproved = newDistance < entry.bestDistanceSq - 1;
        boolean resetApplied = toolImproved || distanceImproved;
        if (resetApplied) {
            if (newTool.ordinal() > entry.bestTool.ordinal()) entry.bestTool = newTool;
            if (newDistance < entry.bestDistanceSq) entry.bestDistanceSq = newDistance;
            entry.numberOfFailures = 0;
            Debug.logMessage("Blacklist RESET: " + item.toString());
        }
        entry.numberOfFailures++;
        entry.numberOfFailuresAllowed = numberOfFailuresAllowed;
        Debug.logMessage("Blacklist: " + item.toString() + ": Try " + entry.numberOfFailures + " / " + entry.numberOfFailuresAllowed);
        MiningPathDiagnostics.logBlacklistStateChanged(
                mod,
                item,
                entryCreated,
                failureCountBefore,
                entry.numberOfFailures,
                allowedFailuresBefore,
                numberOfFailuresAllowed,
                entry.numberOfFailuresAllowed,
                unreachableBefore,
                entry.numberOfFailures > entry.numberOfFailuresAllowed,
                newDistance,
                bestDistanceSqBefore,
                entry.bestDistanceSq,
                newTool,
                bestToolBefore,
                entry.bestTool,
                resetApplied,
                resetReason(entryCreated, distanceImproved, toolImproved));
    }

    private String resetReason(boolean entryCreated, boolean distanceImproved, boolean toolImproved) {
        if (entryCreated) {
            return "NEW_ENTRY";
        }
        if (distanceImproved && toolImproved) {
            return "DISTANCE_AND_TOOL_IMPROVED";
        }
        if (distanceImproved) {
            return "DISTANCE_IMPROVED";
        }
        if (toolImproved) {
            return "TOOL_IMPROVED";
        }
        return "NONE";
    }

    protected abstract Vec3d getPos(T item);

    public boolean unreachable(T item) {
        if (entries.containsKey(item)) {
            BlacklistEntry entry = entries.get(item);
            return entry.numberOfFailures > entry.numberOfFailuresAllowed;
        }
        return false;
    }

    public void clear() {
        entries.clear();
    }

    // Key: BlockPos
    private static class BlacklistEntry {
        public int numberOfFailuresAllowed;
        public int numberOfFailures;
        public double bestDistanceSq;
        public MiningRequirement bestTool;
    }
}
