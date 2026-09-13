package lavi.minecraft.integration.mining.operation.hotbar;
import lavi.minecraft.integration.mining.operation.hotbar.layout.MiningHotbarLayout;
import lavi.minecraft.integration.mining.operation.hotbar.lifecycle.MiningHotbarProgress;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
//20260913_kpopmodder: Verify coexistence and finite unresolved-placement behavior without changing tool eligibility.
class MiningHotbarLayoutTest {
    @Test void temporaryToolDoesNotEvictAccessOrTargetOrCurrentWeapon() {
        boolean[] empty = new boolean[9]; Arrays.fill(empty, true);
        int destination = MiningHotbarLayout.choose(19, 6, 1, 3, empty);
        assertEquals(2, destination);
        for (int protectedSlot : new int[]{0, 1, 3, 6, 8}) assertFalse(MiningHotbarLayout.available(protectedSlot, 19, 6, 1, 3));
    }
    @Test void fullHotbarStillAllowsAnInventorySwapWithoutDiscard() {
        assertEquals(2, MiningHotbarLayout.choose(19, 6, 1, 3, new boolean[9]));
    }
    @Test void existingQualifiedHotbarToolIsKeptEvenInAnEngineWorkSlot() {
        assertEquals(0, MiningHotbarLayout.choose(0, 0, 1, 3, new boolean[9]));
        assertTrue(MiningHotbarLayout.available(0, 0, 0, 1, 3));
    }
    @Test void emptyCandidateIsPreferredOverDisplacingAnUnreservedItem() {
        boolean[] empty = new boolean[9]; empty[5] = true;
        assertEquals(5, MiningHotbarLayout.choose(19, 6, 1, 3, empty));
    }
    @Test void unresolvedPlacementStopsAfterExactlyOneHundredActiveEvaluations() {
        MiningHotbarProgress progress = new MiningHotbarProgress();
        for (int i = 0; i < 99; i++) progress.blocked("HOTBAR_LAYOUT_UNAVAILABLE");
        assertTrue(progress.failure().isEmpty()); progress.blocked("HOTBAR_LAYOUT_UNAVAILABLE");
        assertEquals("HOTBAR_LAYOUT_UNAVAILABLE", progress.failure().orElseThrow());
        progress.confirmedProgress(); assertEquals(100, progress.blockedEvaluations()); assertTrue(progress.failure().isPresent());
    }
    @Test void confirmedProgressResetsOnlyTheUnresolvedBudgetWhileConfirmationFailureStaysTerminal() {
        MiningHotbarProgress progress = new MiningHotbarProgress();
        for (int i = 0; i < 20; i++) progress.blocked("blocked");
        progress.confirmedProgress(); assertEquals(0, progress.blockedEvaluations());
        progress.confirmationFailed(); progress.confirmedProgress();
        assertEquals("HOTBAR_CONFIRMATION_TIMEOUT", progress.failure().orElseThrow());
    }
}
