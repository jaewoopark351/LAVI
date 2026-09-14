package lavi.minecraft.find.approach.policy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Supported safety profiles do not generalize to angry neutral mobs or mod threats.
class FindApproachEnvelopeTest {
    @Test void unknownModAndRangedThreatHaveNoInventedSafetyDistance() {
        assertNull(FindApproachEnvelope.forKind("entity", "example:peaceful_looking_mob", true));
        assertNull(FindApproachEnvelope.forKind("entity", "minecraft:skeleton", false));
        assertNull(FindApproachEnvelope.forKind("entity", "minecraft:wolf", true));
        assertNull(FindApproachEnvelope.forKind("item", "minecraft:diamond", false));
    }
    @Test void closeRangeAndNonfiniteDistancesCannotBecomeSafeSuccess() {
        var profile = FindApproachEnvelope.forKind("entity", "minecraft:creeper", false);
        assertNotNull(profile);
        assertFalse(profile.contains(3 * 3));
        assertFalse(profile.contains(Double.NaN));
        assertTrue(profile.contains(13 * 13));
        assertFalse(profile.contains(17 * 17));
        assertTrue(FindApproachEnvelope.forKind("entity", "minecraft:villager", true).contains(3 * 3));
    }
}
