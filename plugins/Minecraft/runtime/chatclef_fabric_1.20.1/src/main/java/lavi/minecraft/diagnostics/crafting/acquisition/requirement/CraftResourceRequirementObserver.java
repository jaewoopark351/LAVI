package lavi.minecraft.diagnostics.crafting.acquisition.requirement;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Behavior-neutral seam at the existing quantity decision.
 *
 * <p>The source target is accepted only so the caller can prove this observation occurs next to
 * the existing decision object. It is neither retained nor rendered. The installed listener gets
 * only immutable values that were already computed by the caller.</p>
 */
public final class CraftResourceRequirementObserver {
    private static final CraftResourceRequirementListener NO_OP =
            (requestedItem, requestedCount, currentItemCount, targetItemCount) -> {
            };
    private static final AtomicReference<CraftResourceRequirementListener> LISTENER =
            new AtomicReference<>(NO_OP);

    private CraftResourceRequirementObserver() {
    }

    public static void install(CraftResourceRequirementListener listener) {
        LISTENER.set(Objects.requireNonNull(listener, "listener"));
    }

    public static void reset() {
        LISTENER.set(NO_OP);
    }

    public static void observeCapturedQuantity(
            Object sourceTarget,
            Supplier<String> requestedItemSupplier,
            int requestedCount,
            int currentItemCount,
            int targetItemCount) {
        // sourceTarget intentionally stays opaque and unretained.
        CraftResourceRequirementListener listener = LISTENER.get();
        try {
            String requestedItem = requestedItemSupplier.get();
            listener.onCapturedQuantity(
                    requestedItem,
                    requestedCount,
                    currentItemCount,
                    targetItemCount
            );
        } catch (RuntimeException | LinkageError ignored) {
            // Diagnostic observation must never alter the authoritative quantity decision.
        }
    }
}
