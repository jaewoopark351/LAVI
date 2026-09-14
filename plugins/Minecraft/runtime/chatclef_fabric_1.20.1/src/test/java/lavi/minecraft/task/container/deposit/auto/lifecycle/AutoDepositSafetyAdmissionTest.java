package lavi.minecraft.task.container.deposit.auto.lifecycle;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.FoodChain;
import adris.altoclef.chains.MLGBucketFallChain;
import adris.altoclef.chains.MobDefenseChain;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Observe async survival claims without calling side-effectful priority evaluation.
class AutoDepositSafetyAdmissionTest {
    @Test
    void missingOptionalChainReferencesDoNotInventADefenseDemand() {
        assertFalse(AutoDepositSafetyAdmission.claimed(TestObjects.allocate(Mod.class)));
    }

    @Test
    void cachedDefenseToolClaimDefersEvenWithoutSelectingADefenseTask() {
        Mod mod = TestObjects.allocate(Mod.class);
        mod.defense = TestObjects.allocate(Defense.class);
        mod.defense.toolClaim = true;
        assertTrue(AutoDepositSafetyAdmission.claimed(mod));
        mod.defense.toolClaim = false;
        assertFalse(AutoDepositSafetyAdmission.claimed(mod));
        assertEquals(0, mod.defense.priorityCalls);
    }

    @Test
    void fireClaimDefersIndependentlyOfTheToolClaim() {
        Mod mod = TestObjects.allocate(Mod.class);
        mod.defense = TestObjects.allocate(Defense.class);
        mod.defense.fire = true;
        assertTrue(AutoDepositSafetyAdmission.claimed(mod));
        assertEquals(0, mod.defense.priorityCalls);
    }

    @Test
    void asynchronousEatingAndChorusUseDefersWithoutReevaluatingPriority() {
        Mod mod = TestObjects.allocate(Mod.class);
        mod.food = TestObjects.allocate(Food.class);
        mod.mlg = TestObjects.allocate(Mlg.class);
        mod.food.eating = true;
        assertTrue(AutoDepositSafetyAdmission.claimed(mod));
        mod.food.eating = false;
        mod.mlg.chorus = true;
        assertTrue(AutoDepositSafetyAdmission.claimed(mod));
        mod.mlg.chorus = false;
        assertFalse(AutoDepositSafetyAdmission.claimed(mod));
        assertEquals(0, mod.food.priorityCalls);
        assertEquals(0, mod.mlg.priorityCalls);
    }

    private static final class Mod extends AltoClef {
        private Defense defense;
        private Food food;
        private Mlg mlg;
        @Override public MobDefenseChain getMobDefenseChain() { return defense; }
        @Override public FoodChain getFoodChain() { return food; }
        @Override public MLGBucketFallChain getMLGBucketChain() { return mlg; }
    }

    private static final class Defense extends MobDefenseChain {
        private boolean toolClaim;
        private boolean fire;
        private int priorityCalls;
        private Defense() { super(null); }
        @Override public boolean isToolInputClaimed() { return toolClaim; }
        @Override public boolean isPuttingOutFire() { return fire; }
        @Override public float getPriority() {
            priorityCalls++;
            throw new AssertionError("Safety observation evaluated defense priority");
        }
    }

    private static final class Food extends FoodChain {
        private boolean eating;
        private int priorityCalls;
        private Food() { super(null); }
        @Override public boolean isTryingToEat() { return eating; }
        @Override public float getPriority() {
            priorityCalls++;
            throw new AssertionError("Safety observation evaluated food priority");
        }
    }

    private static final class Mlg extends MLGBucketFallChain {
        private boolean chorus;
        private int priorityCalls;
        private Mlg() { super(null); }
        @Override public boolean isChorusFruiting() { return chorus; }
        @Override public float getPriority() {
            priorityCalls++;
            throw new AssertionError("Safety observation evaluated MLG priority");
        }
    }
}
