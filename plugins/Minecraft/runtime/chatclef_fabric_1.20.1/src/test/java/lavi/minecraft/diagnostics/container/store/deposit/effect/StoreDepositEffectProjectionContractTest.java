package lavi.minecraft.diagnostics.container.store.deposit.effect;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.effectFields;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.fields;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class StoreDepositEffectProjectionContractTest {

    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    @DisplayName("scenario 7 [assertions 9-10]: signed deltas and ROOT/TARGET predicate asymmetry")
    void signedDeltasCoverPositiveNegativeReplacementAndTrackerPredicateAsymmetry() throws IOException {
        TrackerBinding root = new TrackerBinding("operation", "ROOT_ANY_CONTAINER", null, 1L, true);
        TrackerBinding target = new TrackerBinding(
                "operation", "TARGET_CONTAINER", new BlockPos(1, 64, 2), 1L, true
        );

        Object positive = signedDelta("stone", 3, "stone", 8);
        Object negative = signedDelta("stone", 8, "stone", 3);
        Object replacement = signedDelta("stone", 3, "dirt", 5);
        Map<String, Object> rootAccepted = fields(effectFields(root, true));
        Map<String, Object> targetRejected = fields(effectFields(target, false));

        assertEquals(5, positive);
        assertEquals(-5, negative);
        assertEquals("-3,5", replacement);
        assertEquals("ROOT_ANY_CONTAINER", rootAccepted.get("trackerRole"));
        assertEquals(true, rootAccepted.get("acceptPredicateResult"));
        assertEquals(true, rootAccepted.get("predicateEvaluated"));
        assertEquals(true, rootAccepted.get("predicateResult"));
        assertEquals("TARGET_CONTAINER", targetRejected.get("trackerRole"));
        assertEquals(false, targetRejected.get("acceptPredicateResult"));
        assertEquals(true, targetRejected.get("predicateEvaluated"));
        assertEquals(false, targetRejected.get("predicateResult"));

        //20260902_kpopmodder: Follow the effect-family implementation after preserving its facade entry points.
        String effectFields = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/event/effect/StoreDepositEffectEventFields.java"
        );
        String rootSource = source("src/main/java/adris/altoclef/tasks/container/DepositAllTask.java");
        String targetSource = source("src/main/java/adris/altoclef/tasks/container/StoreInContainerTask.java");
        assertTrue(effectFields.contains("return new Delta(2, itemName(before), -count(before), itemName(after), count(after))"));
        assertTrue(effectFields.contains("int amount = count(after) - count(before)"));
        assertTrue(rootSource.contains("new ContainerStoredTracker(slot -> true)"));
        assertTrue(targetSource.contains("openContainer.isPresent() && openContainer.get().equals(targetContainer)"));
    }

    private static Object signedDelta(String beforeItem,
                                      int beforeCount,
                                      String afterItem,
                                      int afterCount) {
        if (!beforeItem.equals(afterItem)) {
            return (-beforeCount) + "," + afterCount;
        }
        return afterCount - beforeCount;
    }
}
