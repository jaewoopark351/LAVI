package lavi.minecraft.diagnostics.crafting.acquisition.requirement;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

//20260901_kpopmodder: Keep the existing visible-return source boundary backend-neutral.
public final class CraftResourceRequirementSourceEventObserver {
    private static final CraftResourceRequirementSourceEventListener NO_OP = sourceTask -> {
    };
    private static final AtomicReference<CraftResourceRequirementSourceEventListener> LISTENER =
            new AtomicReference<>(NO_OP);

    private CraftResourceRequirementSourceEventObserver() {
    }

    public static void install(CraftResourceRequirementSourceEventListener listener) {
        LISTENER.set(Objects.requireNonNull(listener, "listener"));
    }

    public static void observeVisibleTaskReturn(Object sourceTask) {
        try {
            LISTENER.get().onVisibleTaskReturn(sourceTask);
        } catch (RuntimeException | LinkageError ignored) {
            // Isolate only the diagnostics callback from the existing source event.
        }
    }
}
