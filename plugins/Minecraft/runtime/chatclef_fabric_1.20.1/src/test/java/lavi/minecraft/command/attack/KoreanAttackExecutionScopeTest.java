package lavi.minecraft.command.attack;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class KoreanAttackExecutionScopeTest {
    @Test void koreanAttackPermissionIsFrozenAndStillRejectsLaterPlayerCandidates() {
        boolean frozen;
        try (var scope = KoreanAttackExecutionScope.begin("attack zombie 3", Map.of("natural_language", Map.of("language", "ko")))) {
            frozen = KoreanAttackExecutionScope.mobOnly();
            assertTrue(frozen);
            assertTrue(KoreanAttackExecutionScope.mayTarget(frozen, false));
        }
        assertFalse(KoreanAttackExecutionScope.mobOnly());
        assertFalse(KoreanAttackExecutionScope.mayTarget(frozen, true));
    }

    @Test void rawEnglishAndOtherCommandsKeepExistingPlayerTargetPolicy() {
        for (Map<String, Object> metadata : java.util.List.of(Map.<String,Object>of(), Map.<String,Object>of("natural_language", Map.of("language", "en")))) {
            try (var scope = KoreanAttackExecutionScope.begin("attack zombie 3", metadata)) {
                assertFalse(KoreanAttackExecutionScope.mobOnly());
                assertTrue(KoreanAttackExecutionScope.mayTarget(KoreanAttackExecutionScope.mobOnly(), true));
            }
        }
        try (var scope = KoreanAttackExecutionScope.begin("follow zombie", Map.of("natural_language", Map.of("language", "ko")))) {
            assertFalse(KoreanAttackExecutionScope.mobOnly());
        }
    }
}
