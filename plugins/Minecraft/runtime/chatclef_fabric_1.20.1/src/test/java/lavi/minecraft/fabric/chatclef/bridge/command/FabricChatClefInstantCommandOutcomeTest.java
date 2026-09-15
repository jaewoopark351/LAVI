package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.command.result.instant.InstantCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FabricChatClefInstantCommandOutcomeTest {
    @Test void synchronousSettingClearsWithoutBorrowingPreexistingIdleTaskCompletion() {
        var execution = execution("overlay off", true,
                FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT);
        execution.attachInstantResult(new InstantCommandResult("overlay off", "overlay", "completed", "SETTING_APPLIED", Map.of("enabled", false)));
        var decision = new FabricChatClefCommandOutcomeClassifier().classify(execution);
        assertTrue(decision.terminal());
        var payload = decision.result().toMap();
        assertEquals("completed", payload.get("status"));
        var data = (Map<?, ?>) payload.get("data");
        assertEquals("instant_command_observed", data.get("result_reason"));
        assertEquals("command_callback_plus_native_result", data.get("result_fidelity"));
        assertEquals("overlay off", ((Map<?, ?>) data.get("instant_command")).get("command"));
    }

    @Test void nativeFailureDoesNotBecomeCompletedBecauseFinishWasCalled() {
        var execution = execution("scan STONE", true, FabricChatClefRootOwnershipClassification.NO_ROOT_VISIBLE);
        execution.attachInstantResult(new InstantCommandResult("scan STONE", "scan", "failed", "NOT_FOUND", Map.of("block", "STONE")));
        var result = new FabricChatClefCommandOutcomeClassifier().classify(execution).result().toMap();
        assertEquals("failed", result.get("status"));
        assertEquals(false, result.get("ok"));
    }

    @Test void receiptDoesNotReplaceActualFinishCallbackOrTakeAnOwnedTask() {
        var result = new InstantCommandResult("overlay off", "overlay", "completed", "SETTING_APPLIED", Map.of("enabled", false));
        var noCallback = execution("overlay off", false, FabricChatClefRootOwnershipClassification.NO_ROOT_VISIBLE);
        noCallback.attachInstantResult(result);
        assertFalse(new FabricChatClefCommandOutcomeClassifier().classify(noCallback).terminal());
        var ownedTask = execution("overlay off", true, FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);
        ownedTask.attachInstantResult(result);
        assertFalse(new FabricChatClefCommandOutcomeClassifier().classify(ownedTask).terminal());
    }

    @Test void unverifiedLegacyReloadReturnsUnknownAndReleasesCompletedInvocation() {
        var execution = execution("reload_settings", true, FabricChatClefRootOwnershipClassification.NO_ROOT_VISIBLE);
        execution.attachInstantResult(new InstantCommandResult("reload_settings", "reload_settings", "unknown", "RELOAD_RETURNED",
                Map.of("callback_returned", true, "configuration_files_verified", false)));
        assertEquals("unknown", new FabricChatClefCommandOutcomeClassifier().classify(execution).result().toMap().get("status"));
    }

    private FabricChatClefCommandExecution execution(String command, boolean callback,
            FabricChatClefRootOwnershipClassification ownership) {
        var request = new FabricChatClefCommandRequest();
        request.requestId = "req-instant";
        request.command = command;
        request.source = "test";
        var context = new FabricChatClefCommandContext(request, "corr-instant", "session-instant", 1L);
        var execution = new FabricChatClefCommandExecution(context, "@" + command, FabricChatClefTaskOwnershipEvidence.empty());
        execution.openExecutorExecuteInvocation();
        if (callback) execution.markFinishCallbackReceived(null);
        execution.closeExecutorExecuteInvocation();
        execution.markDispatchReturned(FabricChatClefTaskOwnershipEvidence.empty(), ownership);
        return execution;
    }
}
