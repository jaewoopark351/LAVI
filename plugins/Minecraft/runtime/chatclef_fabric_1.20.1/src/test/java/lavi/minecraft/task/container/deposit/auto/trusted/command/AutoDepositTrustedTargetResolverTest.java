package lavi.minecraft.task.container.deposit.auto.trusted.command;

import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
final class AutoDepositTrustedTargetResolverTest {
    @Test
    void exactOpenBindingWinsWithoutReadingCrosshairFallback() {
        BlockPos exactPosition = new BlockPos(1, 64, 2);
        AtomicInteger crosshairReads = new AtomicInteger();
        AutoDepositTrustedTargetResolver resolver = new AutoDepositTrustedTargetResolver(
                () -> Optional.of(exactPosition),
                ignored -> {
                    crosshairReads.incrementAndGet();
                    return Optional.of(new BlockPos(9, 70, 9));
                }
        );

        Optional<AutoDepositTrustedTarget> result = resolver.resolve(null);

        assertTrue(result.isPresent());
        assertEquals(exactPosition, result.get().position());
        assertEquals(AutoDepositTrustedTargetSource.EXACT_OPEN_CONTAINER, result.get().source());
        assertEquals(0, crosshairReads.get());
    }

    @Test
    void emptyExactBindingFallsBackToTheCurrentCrosshairTarget() {
        BlockPos crosshairPosition = new BlockPos(-3, 65, 7);
        AutoDepositTrustedTargetResolver resolver = new AutoDepositTrustedTargetResolver(
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                ignored -> Optional.of(crosshairPosition)
        );

        Optional<AutoDepositTrustedTarget> result = resolver.resolve(null);

        assertTrue(result.isPresent());
        assertEquals(crosshairPosition, result.get().position());
        assertEquals(AutoDepositTrustedTargetSource.CROSSHAIR, result.get().source());
    }

    @Test
    void absentExactAndCrosshairTargetsReturnEmpty() {
        AutoDepositTrustedTargetResolver resolver = new AutoDepositTrustedTargetResolver(
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                ignored -> Optional.empty()
        );

        assertTrue(resolver.resolve(null).isEmpty());
    }
}
