package lavi.minecraft.task.container.deposit;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllContainerSelectorTest {
    @Test
    void appliesTheProvidedPredicateAndReturnsAnImmutableCandidate() {
        BlockPos.Mutable candidate = new BlockPos.Mutable(12, 64, 8);
        AtomicBoolean predicateInvoked = new AtomicBoolean();
        DepositAllContainerSelector selector = new DepositAllContainerSelector(
                (mod, eligibility, targetBlocks) -> eligibility.test(candidate)
                        ? Optional.of(candidate)
                        : Optional.empty()
        );

        Optional<BlockPos> selected = selector.select(null, position -> {
            predicateInvoked.set(true);
            return position.equals(candidate);
        });

        assertTrue(predicateInvoked.get());
        assertTrue(selected.isPresent());
        assertNotSame(candidate, selected.orElseThrow());
        candidate.set(30, 70, 30);
        assertEquals(new BlockPos(12, 64, 8), selected.orElseThrow());
    }

    @Test
    void preservesAnEmptyFilteredResult() {
        DepositAllContainerSelector selector = new DepositAllContainerSelector(
                (mod, eligibility, targetBlocks) -> Optional.empty()
        );

        assertFalse(selector.select(null, position -> true).isPresent());
    }
}
