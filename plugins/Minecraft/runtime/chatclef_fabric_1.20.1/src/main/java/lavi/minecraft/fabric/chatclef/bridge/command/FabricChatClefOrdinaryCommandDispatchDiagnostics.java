package lavi.minecraft.fabric.chatclef.bridge.command;

//20260905_kpopmodder: Render only bounded ordinary-command dispatch diagnostics.

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefOrdinaryCommandDispatchDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefOrdinaryCommandDispatchDiagnostics(
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.diagnostics = diagnostics;
    }

    public void started(
            FabricChatClefCommandContext context,
            FabricChatClefCommandRequest request,
            String command
    ) {
        diagnostics.info(
                "dispatch command request="
                        + context.requestId()
                        + " source="
                        + request.source
                        + " normalized_command="
                        + command
                        + " generation="
                        + context.connectionGeneration()
        );
    }

    public void runningResultSubmissionFailed(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultSendSubmission submission
    ) {
        diagnostics.warn(
                "running result send submit failed request="
                        + context.requestId()
                        + " outcome="
                        + submission.diagnosticMessage()
        );
    }

    //20260915_kpopmodder: One bounded observation per synchronous command, using the existing bridge sink.
    public void instantResultObserved(FabricChatClefCommandContext context,
            lavi.minecraft.command.result.instant.InstantCommandResult result) {
        if (result == null) return;
        try {
            String values = result.values().entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof Number || entry.getValue() instanceof Boolean)
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .sorted().collect(java.util.stream.Collectors.joining(","));
            String reason = result.reason().replaceAll("[^A-Za-z0-9_]", "_");
            if (reason.length() > 80) reason = reason.substring(0, 80);
            diagnostics.info("instant_command_result request=" + context.requestId()
                    + " generation=" + context.connectionGeneration() + " command=" + result.commandName()
                    + " outcome=" + result.outcome() + " reason=" + reason + " values=" + values);
        } catch (RuntimeException ignored) {
            // Diagnostic failure cannot select or suppress the command's terminal result.
        }
    }

    public void catalogueAdmissionRejected(FabricChatClefCommandContext context, String reason) {
        try {
            diagnostics.warn("catalogue_admission request=" + context.requestId() + " generation=" + context.connectionGeneration()
                    + " accepted=false reason=" + reason + " executor_invocations=0");
        } catch (RuntimeException ignored) { /* logging cannot grant admission */ }
    }

    public void koreanAttackBound(FabricChatClefCommandContext context) {
        try { diagnostics.info("attack_permission request=" + context.requestId() + " generation=" + context.connectionGeneration()
                + " mob_only=true player_targets=false frozen_for_task=true"); }
        catch (RuntimeException ignored) { /* the permission does not depend on logging */ }
    }
}
