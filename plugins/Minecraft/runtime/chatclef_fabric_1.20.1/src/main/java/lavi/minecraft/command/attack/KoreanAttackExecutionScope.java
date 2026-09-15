package lavi.minecraft.command.attack;

import java.util.Map;

//20260915_kpopmodder: Freeze the Korean mob-only permission at native command construction.
public final class KoreanAttackExecutionScope implements AutoCloseable {
    private static final ThreadLocal<Boolean> MOB_ONLY = new ThreadLocal<>();
    private final Boolean previous;

    private KoreanAttackExecutionScope(boolean mobOnly) {
        previous = MOB_ONLY.get();
        MOB_ONLY.set(mobOnly);
    }

    public static KoreanAttackExecutionScope begin(String command, Map<String, Object> metadata) {
        Object natural = metadata == null ? null : metadata.get("natural_language");
        boolean korean = natural instanceof Map<?, ?> values && "ko".equals(values.get("language"));
        return new KoreanAttackExecutionScope(korean && "attack".equals(command.trim().split("\\s+", 2)[0]));
    }

    public static boolean mobOnly() { return Boolean.TRUE.equals(MOB_ONLY.get()); }
    public static boolean mayTarget(boolean mobOnly, boolean player) { return !mobOnly || !player; }

    @Override public void close() {
        if (previous == null) MOB_ONLY.remove(); else MOB_ONLY.set(previous);
    }
}
