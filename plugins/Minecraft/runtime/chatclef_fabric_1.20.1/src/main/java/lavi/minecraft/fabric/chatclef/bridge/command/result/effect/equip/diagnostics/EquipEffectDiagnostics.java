package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.diagnostics;

import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.binding.EquipEffectBinding;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipEffectProfile;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import java.util.stream.Collectors;

//20260915_kpopmodder: Two bounded normal bridge boundary logs per EQUIP request; no diagnostic-mode or tick behavior.
public final class EquipEffectDiagnostics {
    private EquipEffectDiagnostics() { }
    public static void emit(String phase, EquipEffectBinding binding, EquipEffectProfile profile,
            EquipSlotObservation observation, String taskIdentity, String status, String reason) {
        try {
            String targets = profile.targets().stream().limit(8).map(target -> "index=" + target.index()
                    + ":requested=" + target.requestedCount() + ":matches="
                    + target.matches().stream().limit(4).map(match -> safe(match.itemId()) + "@" + match.slot()).collect(Collectors.joining(","))
                    + ":omitted=" + Math.max(0, target.matches().size() - 4)).collect(Collectors.joining(";"));
            String slots = observation.slots().stream().map(slot -> slot.slot() + "=" + safe(slot.itemId())
                    + ":" + slot.count()).collect(Collectors.joining(","));
            new FabricChatClefBridgeDiagnostics().info("equip_effect phase=" + phase
                    + " request=" + safe(binding.requestId()) + " session=" + safe(binding.sessionId())
                    + " server_generation=" + binding.serverGeneration() + " java_socket_generation=" + binding.socketGeneration()
                    + " task=" + safe(taskIdentity) + " status=" + status + " reason=" + reason
                    + " observed_at_ms=" + observation.observedAtMs() + " source=minecraft_client_equipment_slots"
                    + " observation_available=" + observation.available() + " observation_reason=" + observation.reason()
                    + " targets_total=" + profile.targets().size() + " targets_omitted=" + Math.max(0, profile.targets().size() - 8)
                    + " targets=[" + targets + "] slots=[" + slots + "]");
        } catch (RuntimeException | LinkageError error) {
            // Existing bridge sink failure cannot select a result or change a native task.
        }
    }
    private static String safe(String value) {
        if (value == null) return "unavailable";
        return value.substring(0, Math.min(128, value.length())).replaceAll("[^a-zA-Z0-9_.:/-]", "_");
    }
}
