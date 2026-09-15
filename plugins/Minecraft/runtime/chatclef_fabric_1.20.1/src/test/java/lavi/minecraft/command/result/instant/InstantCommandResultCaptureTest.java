package lavi.minecraft.command.result.instant;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InstantCommandResultCaptureTest {
    @Test void exactInvocationKeepsFirstNativeDecisionAndOriginalCommand() {
        try (var capture = InstantCommandResultCapture.begin("scan STONE")) {
            InstantCommandResultCapture.record("overlay", true, "WRONG_COMMAND", Map.of());
            InstantCommandResultCapture.record("scan", false, "NOT_FOUND", Map.of("block", "STONE"));
            InstantCommandResultCapture.record("scan", true, "LATE_SUCCESS", Map.of());
            assertEquals("scan STONE", capture.result().command());
            assertEquals("NOT_FOUND", capture.result().reason());
            assertEquals("failed", capture.result().outcome());
        }
    }

    @Test void missingImmediateEvidenceIsUnknownButTaskCommandHasNoSyntheticResult() {
        try (var capture = InstantCommandResultCapture.begin("overlay off")) {
            assertEquals("unknown", capture.result().outcome());
            assertEquals("RESULT_UNAVAILABLE", capture.result().reason());
        }
        for (String command : new String[]{"give Alex iron_ingot 3", "follow Alex", "gamer", "get iron_ingot 3"}) {
            try (var capture = InstantCommandResultCapture.begin(command)) { assertNull(capture.result()); }
        }
    }

    @Test void earlyTaskRejectionIsRecordedWithoutWaitingForATaskThatWasNeverStarted() {
        try (var capture = InstantCommandResultCapture.begin("give Alex missing 3")) {
            InstantCommandResultCapture.record("give", false, "ITEM_UNAVAILABLE", Map.of());
            assertEquals("failed", capture.result().outcome());
        }
    }

    @Test void closedAndOtherThreadObservationsCannotContaminateNextInvocation() throws Exception {
        try (var first = InstantCommandResultCapture.begin("overlay off")) {
            InstantCommandResultCapture.record("overlay", true, "SETTING_APPLIED", Map.of("enabled", false));
        }
        InstantCommandResultCapture.record("overlay", true, "STALE", Map.of());
        try (var next = InstantCommandResultCapture.begin("overlay on")) {
            Thread foreign = new Thread(() -> InstantCommandResultCapture.record("overlay", true, "FOREIGN", Map.of()));
            foreign.start();
            foreign.join();
            assertEquals("RESULT_UNAVAILABLE", next.result().reason());
        }
    }

    @Test void nestedInvocationRestoresItsParentObservationScope() {
        try (var parent = InstantCommandResultCapture.begin("overlay on")) {
            try (var nested = InstantCommandResultCapture.begin("scan DIRT")) {
                InstantCommandResultCapture.record("scan", false, "NOT_FOUND", Map.of());
                assertEquals("scan", nested.result().commandName());
            }
            InstantCommandResultCapture.record("overlay", true, "SETTING_APPLIED", Map.of("enabled", true));
            assertEquals("overlay", parent.result().commandName());
        }
    }

    @Test void invalidObservationCannotThrowThroughNativeCommand() {
        try (var capture = InstantCommandResultCapture.begin("scan DIRT")) {
            assertDoesNotThrow(() -> InstantCommandResultCapture.record("scan", true, "BAD", null));
            assertEquals("unknown", capture.result().outcome());
        }
    }
}
