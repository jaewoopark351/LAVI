package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefCommandEffectTracker;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.binding.EquipEffectBinding;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.diagnostics.EquipEffectDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.evidence.EquipEffectEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotReader;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.payload.EquipEffectPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipEffectProfile;
import java.util.Map;
import java.util.function.Supplier;

//20260915_kpopmodder: One request owns two observations; retries serialize the same frozen terminal evidence.
public final class EquipEffectTracker implements FabricChatClefCommandEffectTracker {
    private final EquipEffectProfile profile;
    private final FabricChatClefCommandContext context;
    private final EquipEffectBinding binding;
    private final Supplier<EquipSlotObservation> reader;
    private final EquipSlotObservation before;
    private FabricChatClefCommandResultDataPayload terminal;

    public static EquipEffectTracker capture(String command, FabricChatClefCommandContext context) {
        return new EquipEffectTracker(EquipEffectProfile.capture(command), context, new EquipSlotReader());
    }
    EquipEffectTracker(EquipEffectProfile profile, FabricChatClefCommandContext context, Supplier<EquipSlotObservation> reader) {
        this.profile = profile;
        this.context = context;
        this.binding = EquipEffectBinding.capture(context);
        this.reader = reader;
        before = profile.tracked() && "available".equals(profile.reason()) ? read() : EquipSlotObservation.unavailable(profile.reason());
        if (profile.tracked()) EquipEffectDiagnostics.emit("capture", binding, profile, before, "", "captured", profile.reason());
    }
    public boolean tracked() { return profile.tracked(); }
    @Override public synchronized FabricChatClefCommandResultDataPayload fromMatchingCompletion(FabricChatClefCommandResultDataPayload basePayload) {
        if (!profile.tracked()) return basePayload;
        if (terminal == null) {
            Map<String, Object> base = basePayload == null ? Map.of() : basePayload.toMap();
            String bindingReason = binding.validate(context, base, profile.command());
            EquipSlotObservation after = "valid".equals(bindingReason) && "available".equals(profile.reason())
                    ? read() : EquipSlotObservation.unavailable("valid".equals(bindingReason) ? profile.reason() : bindingReason);
            EquipEffectEvidence evidence = EquipEffectEvidence.evaluate(profile, before, after, bindingReason);
            terminal = EquipEffectPayload.attach(base, profile, binding, before, after, evidence);
            EquipEffectDiagnostics.emit("terminal", binding, profile, after, EquipEffectBinding.taskIdentity(base), evidence.status(), evidence.reason());
        }
        return terminal;
    }
    private EquipSlotObservation read() {
        try {
            EquipSlotObservation value = reader.get();
            return value == null ? EquipSlotObservation.unavailable("slot_reader_returned_null") : value;
        } catch (RuntimeException | LinkageError error) { return EquipSlotObservation.unavailable("slot_read_failed"); }
    }
}
