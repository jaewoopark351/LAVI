package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.EquipEffectExecutionFixture;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipEffectProfile;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class EquipEffectLifecycleTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.createGameVersion(); Bootstrap.initialize();
        try (var client = lavi.minecraft.testsupport.HeadlessMinecraftClientSession.inGame()) { adris.altoclef.TaskCatalogue.resourceNames(); }
    }
    @Test void matchingNativeTaskCompletionAddsEffectWithoutChangingLifecycleStatus() throws Exception {
        var reads = new AtomicInteger(); var task = new EquipArmorTask(Items.DIAMOND_HELMET);
        var execution = execution(task, reads);
        execution.markFinishCallbackReceived(task);
        assertFalse(new FabricChatClefCommandOutcomeClassifier().classify(execution).terminal());
        assertEquals(1, reads.get());
        execution.markTaskFinishedObservation(FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, task)));
        var result = new FabricChatClefCommandOutcomeClassifier().classify(execution).result().toMap();
        assertEquals("completed", result.get("status"));
        var data = (Map<?, ?>) result.get("data");
        assertEquals("matching_task_finished", data.get("result_reason"));
        assertEquals("satisfied", ((Map<?, ?>) data.get("effect_payload")).get("effect_observation_status"));
        assertEquals(2, reads.get());
        String output = System.getProperty("allCommands.equipEvidenceOutput");
        if (output != null) {
            var path = java.nio.file.Path.of(output).toAbsolutePath().normalize();
            var allowed = java.nio.file.Path.of(System.getProperty("user.dir")).resolve("../../../../test/test_Isolation/korean_followup_20260915").toAbsolutePath().normalize();
            if (!path.startsWith(allowed)) throw new IllegalArgumentException("EQUIP fixture output outside approved isolation path");
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
        }
    }
    @Test void differentTaskAndDispatchFailureNeverInvokeTerminalEquipmentReader() {
        var reads = new AtomicInteger(); var task = new EquipArmorTask(Items.DIAMOND_HELMET);
        var execution = execution(task, reads);
        execution.markFinishCallbackReceived(task);
        execution.markTaskFinishedObservation(FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, new EquipArmorTask(Items.DIAMOND_HELMET))));
        var decision = new FabricChatClefCommandOutcomeClassifier().classify(execution);
        assertEquals("unknown", decision.result().toMap().get("status"));
        assertFalse(((Map<?, ?>) decision.result().toMap().get("data")).containsKey("effect_payload"));
        var failed = execution.failedFromDispatchException(new IllegalArgumentException("fixture"), FabricChatClefTaskSnapshot.capture(task));
        assertEquals("failed", failed.toMap().get("status"));
        assertEquals(1, reads.get());
    }
    @Test void stoppedTaskKeepsFailedTerminationAndDoesNotReadEquipmentAgain() throws Exception {
        var reads = new AtomicInteger(); var task = new EquipArmorTask(Items.DIAMOND_HELMET);
        var execution = execution(task, reads);
        var stopped = Task.class.getDeclaredField("stopped"); stopped.setAccessible(true); stopped.setBoolean(task, true);
        execution.markFinishCallbackReceived(task);
        execution.markTaskFinishedObservation(FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, task)));
        var result = new FabricChatClefCommandOutcomeClassifier().classify(execution).result().toMap();
        assertEquals("failed", result.get("status"));
        assertFalse(((Map<?, ?>) result.get("data")).containsKey("effect_payload"));
        assertEquals(1, reads.get());
    }
    private static FabricChatClefCommandExecution execution(EquipArmorTask task, AtomicInteger reads) {
        var context = EquipEffectTrackerTest.context(); context.request().command = "equip diamond_helmet";
        var tracker = new EquipEffectTracker(EquipEffectProfile.capture(context.request().command), context, () ->
                EquipEffectEvidenceTest.slots(10 + reads.getAndIncrement(), reads.get() == 1 ? Map.of() : Map.of("head", "minecraft:diamond_helmet")));
        var execution = EquipEffectExecutionFixture.create(context, "@equip diamond_helmet", tracker);
        execution.markDispatchReturned(FabricChatClefTaskOwnershipEvidence.of(task, FabricChatClefTaskSnapshot.capture(task),
                FabricChatClefTaskOwnershipSnapshot.empty(), 10, 10, 1, Thread.currentThread().getName()), FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);
        return execution;
    }
}
