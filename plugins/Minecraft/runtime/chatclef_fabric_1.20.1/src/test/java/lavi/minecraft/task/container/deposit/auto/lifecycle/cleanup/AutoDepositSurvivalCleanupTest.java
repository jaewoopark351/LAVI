package lavi.minecraft.task.container.deposit.auto.lifecycle.cleanup;

import adris.altoclef.AltoClef;
import baritone.api.utils.input.Input;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Prove exact survival claims survive old-tree cleanup without restoring unrelated storage inputs.
class AutoDepositSurvivalCleanupTest {
    @Test
    void preservesShieldClaimsButNeverRestoresOldStorageMovementOrForcedAttack() {
        Inputs inputs = new Inputs();
        inputs.shield = true;
        inputs.keys.put(Input.SNEAK, true);
        inputs.keys.put(Input.CLICK_RIGHT, true);
        inputs.keys.put(Input.MOVE_FORWARD, true);
        inputs.forces.put(Input.CLICK_LEFT, true);
        AutoDepositSurvivalCleanup.run(inputs, inputs::cancelOldTree);
        assertTrue(inputs.pressed(Input.SNEAK));
        assertTrue(inputs.pressed(Input.CLICK_RIGHT));
        assertFalse(inputs.pressed(Input.MOVE_FORWARD));
        assertFalse(inputs.forced(Input.CLICK_LEFT));
        assertEquals(List.of("cleanup", "key:SNEAK=true", "key:CLICK_RIGHT=true"), inputs.trace);
    }

    @Test
    void preservesOnlyNativeForcedAttackWhenPuttingOutFire() {
        Inputs inputs = new Inputs();
        inputs.fire = true;
        inputs.forces.put(Input.CLICK_LEFT, true);
        inputs.forces.put(Input.MOVE_FORWARD, true);
        inputs.keys.put(Input.SNEAK, true);
        AutoDepositSurvivalCleanup.run(inputs, inputs::cancelOldTree);
        assertTrue(inputs.forced(Input.CLICK_LEFT));
        assertFalse(inputs.forced(Input.MOVE_FORWARD));
        assertFalse(inputs.pressed(Input.SNEAK));
        assertEquals(List.of("cleanup", "force:CLICK_LEFT=true"), inputs.trace);
    }

    @Test
    void foodAndChorusClaimsPreserveUseWithoutRevivingStorageSneak() {
        for (boolean eating : new boolean[]{true, false}) {
            Inputs inputs = new Inputs();
            inputs.eat = eating;
            inputs.chorus = !eating;
            inputs.keys.put(Input.CLICK_RIGHT, true);
            inputs.keys.put(Input.SNEAK, true);
            AutoDepositSurvivalCleanup.run(inputs, inputs::cancelOldTree);
            assertTrue(inputs.pressed(Input.CLICK_RIGHT));
            assertFalse(inputs.pressed(Input.SNEAK));
            assertEquals(List.of("cleanup", "key:CLICK_RIGHT=true"), inputs.trace);
        }
    }

    @Test
    void semanticFlagDoesNotInventAPressThatWasNotPresentAtHandoff() {
        Inputs inputs = new Inputs();
        inputs.shield = true;
        inputs.fire = true;
        AutoDepositSurvivalCleanup.run(inputs, () -> {
            inputs.keys.put(Input.SNEAK, true);
            inputs.keys.put(Input.CLICK_RIGHT, true);
            inputs.forces.put(Input.CLICK_LEFT, true);
        });
        assertFalse(inputs.pressed(Input.SNEAK));
        assertFalse(inputs.pressed(Input.CLICK_RIGHT));
        assertFalse(inputs.forced(Input.CLICK_LEFT));
    }

    @Test
    void noClaimPerformsCleanupOnceAndNoRestoration() {
        Inputs inputs = new Inputs();
        inputs.keys.put(Input.CLICK_RIGHT, true);
        AutoDepositSurvivalCleanup.run(inputs, inputs::cancelOldTree);
        assertEquals(List.of("cleanup"), inputs.trace);
        assertFalse(inputs.pressed(Input.CLICK_RIGHT));
    }

    @Test
    void cleanupFailurePropagatesAfterClaimsAreRestored() {
        Inputs inputs = new Inputs();
        inputs.shield = true;
        inputs.keys.put(Input.SNEAK, true);
        inputs.keys.put(Input.CLICK_RIGHT, true);
        IllegalStateException failure = new IllegalStateException("owned cleanup failed");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> AutoDepositSurvivalCleanup.run(inputs, () -> {
                    inputs.cancelOldTree();
                    throw failure;
                })));
        assertTrue(inputs.pressed(Input.SNEAK));
        assertTrue(inputs.pressed(Input.CLICK_RIGHT));
    }

    @Test
    void restoreFailureDoesNotReplaceTheOriginalCleanupFailure() {
        Inputs inputs = new Inputs();
        inputs.eat = true;
        inputs.restoreFailure = new IllegalArgumentException("restore failed");
        IllegalStateException failure = new IllegalStateException("owned cleanup failed");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> AutoDepositSurvivalCleanup.run(inputs, () -> { throw failure; })));
        assertArrayEquals(new Throwable[]{inputs.restoreFailure}, failure.getSuppressed());
    }

    @Test
    void headlessNullModHasNoClaimsAndStillRunsCleanup() {
        int[] calls = {0};
        AutoDepositSurvivalCleanup.run((AltoClef) null, () -> calls[0]++);
        assertEquals(1, calls[0]);
    }

    private static final class Inputs implements AutoDepositSurvivalInputPort {
        private boolean shield;
        private boolean fire;
        private boolean eat;
        private boolean chorus;
        private RuntimeException restoreFailure;
        private final EnumMap<Input, Boolean> keys = new EnumMap<>(Input.class);
        private final EnumMap<Input, Boolean> forces = new EnumMap<>(Input.class);
        private final List<String> trace = new ArrayList<>();

        @Override public boolean shielding() { return shield; }
        @Override public boolean puttingOutFire() { return fire; }
        @Override public boolean eating() { return eat; }
        @Override public boolean chorusFruiting() { return chorus; }
        @Override public boolean pressed(Input input) { return keys.getOrDefault(input, false); }
        @Override public boolean forced(Input input) { return forces.getOrDefault(input, false); }
        @Override public void setPressed(Input input, boolean value) {
            if (restoreFailure != null) throw restoreFailure;
            trace.add("key:" + input + "=" + value);
            keys.put(input, value);
        }
        @Override public void setForced(Input input, boolean value) {
            trace.add("force:" + input + "=" + value);
            forces.put(input, value);
        }

        private void cancelOldTree() {
            trace.add("cleanup");
            keys.clear();
            forces.clear();
        }
    }
}
