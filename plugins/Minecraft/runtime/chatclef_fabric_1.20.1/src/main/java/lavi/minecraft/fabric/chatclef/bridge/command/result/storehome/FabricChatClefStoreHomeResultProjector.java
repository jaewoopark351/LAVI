package lavi.minecraft.fabric.chatclef.bridge.command.result.storehome;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.task.container.home.command.StoreHomeCommand;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import lavi.minecraft.task.container.home.result.StoreHomeOutcome;

import java.util.Optional;

//20260827_kpopmodder: Project only the matching request's typed STORE_HOME outcome.
public final class FabricChatClefStoreHomeResultProjector {
    private static final String NORMALIZED_COMMAND = "@" + StoreHomeCommand.COMMAND_NAME;

    public FabricChatClefCommandResultDataPayload fromMatchingTask(
            FabricChatClefCommandResultDataPayload basePayload,
            String normalizedCommand,
            FabricChatClefCommandTerminationObservation observation
    ) {
        if (!isStoreHomeCommand(normalizedCommand) || observation == null) {
            return basePayload;
        }
        Task task = observation.task();
        if (!(task instanceof StoreHomeTask storeHomeTask)) {
            return basePayload;
        }
        Optional<StoreHomeOutcome> outcome = storeHomeTask.outcome();
        return outcome
                .<FabricChatClefCommandResultDataPayload>map(value ->
                        new FabricChatClefStoreHomeResultDataPayload(basePayload, value))
                .orElse(basePayload);
    }

    public FabricChatClefCommandResultDataPayload fromCommandWithoutUserTask(
            FabricChatClefCommandResultDataPayload basePayload,
            String normalizedCommand
    ) {
        if (!isStoreHomeCommand(normalizedCommand)) {
            return basePayload;
        }
        return new FabricChatClefStoreHomeResultDataPayload(
                basePayload,
                StoreHomeOutcome.cursorNotEmptyAtCommandGate()
        );
    }

    private boolean isStoreHomeCommand(String normalizedCommand) {
        return NORMALIZED_COMMAND.equals(
                normalizedCommand == null ? "" : normalizedCommand.trim()
        );
    }
}
