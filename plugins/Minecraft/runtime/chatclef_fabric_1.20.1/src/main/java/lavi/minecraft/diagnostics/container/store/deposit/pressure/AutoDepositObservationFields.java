package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import java.util.Set;
import net.minecraft.util.math.BlockPos;

//20260913_kpopmodder: Format bounded semantic values separately from scope ownership and event dispatch.
public final class AutoDepositObservationFields {
    private static final Set<String> SEMANTIC_FIELDS = Set.of(
            "previousState", "nextState", "occupiedSlots", "totalSlots", "activeObserved",
            "priorityObserved", "selectedChainClass", "selectedPriority", "previousPhase", "nextPhase",
            "endingOccupiedSlots", "confirmedTotal", "remainingTargetTypes", "candidatePosition");

    private AutoDepositObservationFields() {
    }

    public static String identity(Object value) {
        return value == null ? "UNAVAILABLE"
                : value.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }

    public static String requestId(Object[] fields) {
        for (int i = 0; i + 1 < fields.length; i += 2) {
            if ("commandRequestId".equals(fields[i]) && fields[i + 1] instanceof String request
                    && !request.isBlank()) return request;
        }
        return "automatic-pressure-unbound";
    }

    public static String fingerprint(String event, String reason, Object[] fields) {
        StringBuilder result = new StringBuilder(event).append('|').append(reason);
        for (int i = 0; i + 1 < fields.length && result.length() < 1024; i += 2) {
            if (SEMANTIC_FIELDS.contains(String.valueOf(fields[i]))) {
                result.append('|').append(fields[i]).append('=').append(frozenValue(fields[i + 1]));
            }
        }
        return result.length() <= 1024 ? result.toString() : result.substring(0, 1024);
    }

    public static Object[] concat(Object[] first, Object[] second) {
        Object[] result = new Object[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static Object[] freezeValues(Object[] fields) {
        Object[] result = fields.clone();
        for (int i = 1; i < result.length; i += 2) result[i] = frozenValue(result[i]);
        return result;
    }

    private static Object frozenValue(Object value) {
        if (value instanceof BlockPos position) {
            return position.getX() + "," + position.getY() + "," + position.getZ();
        }
        if (value == null || value instanceof String || value instanceof Number
                || value instanceof Boolean || value instanceof Enum<?>) return value;
        return identity(value);
    }
}
