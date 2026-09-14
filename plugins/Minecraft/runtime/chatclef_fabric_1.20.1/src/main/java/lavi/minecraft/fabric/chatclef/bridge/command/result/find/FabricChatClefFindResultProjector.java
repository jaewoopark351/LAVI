//#if MC == 12001
//20260914_kpopmodder: Project only a matching FIND root's frozen evidence, never generic task completion.
package lavi.minecraft.fabric.chatclef.bridge.command.result.find;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.task.find.FindRequest;
import lavi.minecraft.task.find.FindTask;
import java.util.LinkedHashMap;

public final class FabricChatClefFindResultProjector {
    private FabricChatClefFindResultProjector() { }
    public static FabricChatClefCommandResultPayload fromMatchingTask(String requestId, String command,
            FabricChatClefCommandResultDataPayload base,
            FabricChatClefCommandTerminationObservation observation, boolean userStopBound) {
        if (command == null || !(command.equals("@find") || command.startsWith("@find "))) return null;
        if (userStopBound || observation == null || observation.taskStopped()
                || !(observation.task() instanceof FindTask task) || task.outcome() == null) {
            return FabricChatClefCommandResult.unknown(requestId, "FIND completion evidence was unavailable.", base);
        }
        try {
            if (!task.request().equals(FindRequest.parse(command))) {
                return FabricChatClefCommandResult.unknown(requestId, "FIND request did not match its task.", base);
            }
        } catch (IllegalArgumentException invalid) {
            return FabricChatClefCommandResult.unknown(requestId, "FIND request was invalid.", base);
        }
        var outcome = task.outcome();
        var data = new LinkedHashMap<String, Object>(base.toMap());
        data.put("find", outcome.toMap());
        var payload = FabricChatClefCommandResultDataPayload.fromMap(data);
        return outcome.success()
                ? FabricChatClefCommandResult.completed(requestId, outcome.koreanMessage(), payload)
                : FabricChatClefCommandResult.failed(requestId, outcome.koreanMessage(), payload);
    }
}
//#endif
