package lavi.minecraft.diagnostics.baritone.builder;

import baritone.api.pathing.calc.IPath;
import java.util.List;

//20260913_kpopmodder: Freeze counts and lookup availability; raw empty lists are not consumer failures.
public record BuilderPathSnapshot(String identity, String type, int positions, int movements,
                                  String lookup, String verified) {
    public static BuilderPathSnapshot capture(IPath path) {
        if (path == null) return new BuilderPathSnapshot("none", "none", -1, -1, "ABSENT", "UNKNOWN");
        if (path instanceof BuilderPathView raw) {
            return new BuilderPathSnapshot(id(path), path.getClass().getName(), raw.lavi$positionsCount(),
                    raw.lavi$movementsCount(), "RAW_FIELDS", Boolean.toString(raw.lavi$verified()));
        }
        int positions = -1;
        int movements = -1;
        String status = "AVAILABLE";
        try { positions = path.positions().size(); }
        catch (RuntimeException | LinkageError error) { status = "POSITIONS_UNAVAILABLE"; }
        try { movements = path.movements().size(); }
        catch (RuntimeException | LinkageError error) { status = "MOVEMENTS_UNAVAILABLE"; }
        return new BuilderPathSnapshot(id(path), path.getClass().getName(), positions, movements, status, "NOT_RAW_PATH");
    }

    public Object[] fields(String prefix) {
        return new Object[]{prefix + "PathIdentity", identity, prefix + "PathClass", type,
                prefix + "PositionsCount", positions, prefix + "MovementsCount", movements,
                prefix + "LookupStatus", lookup, prefix + "Verified", verified};
    }

    public static String accessReason(int index, int size) {
        return size < 0 ? "MOVEMENT_COUNT_UNAVAILABLE"
                : index < 0 || index >= size ? "MOVEMENT_INDEX_OUT_OF_RANGE" : "MOVEMENT_INDEX_IN_RANGE";
    }

    public static int size(List<?> values) { return values == null ? -1 : values.size(); }
    public static String id(Object object) {
        return object == null ? "none" : Integer.toHexString(System.identityHashCode(object));
    }
}
