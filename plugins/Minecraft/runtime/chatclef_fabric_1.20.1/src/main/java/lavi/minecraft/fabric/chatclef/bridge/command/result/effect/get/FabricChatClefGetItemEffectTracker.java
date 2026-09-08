package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefCommandEffectTracker;

import java.util.Objects;

//20260907_kpopmodder: Own one command's bounded before/terminal GET count observations.
public final class FabricChatClefGetItemEffectTracker
        implements FabricChatClefCommandEffectTracker {
    private final FabricChatClefGetItemEffectProfile profile;
    private final FabricChatClefGetItemTargetCountReader countReader;
    private final FabricChatClefGetItemCountObservation before;
    private final FabricChatClefGetItemEffectResultProjector resultProjector =
            new FabricChatClefGetItemEffectResultProjector();
    private FabricChatClefGetItemEffectEvidence terminalEvidence;

    private FabricChatClefGetItemEffectTracker(
            FabricChatClefGetItemEffectProfile profile,
            FabricChatClefGetItemTargetCountReader countReader
    ) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.countReader = Objects.requireNonNull(countReader, "countReader");
        this.before = profile.tracked()
                ? readOrUnavailable("before_reader_returned_null")
                : FabricChatClefGetItemCountObservation.unavailable("not_applicable");
    }

    public static FabricChatClefGetItemEffectTracker capture(String normalizedCommand) {
        FabricChatClefGetItemEffectProfile profile =
                FabricChatClefGetItemEffectProfile.fromNormalizedCommand(normalizedCommand);
        FabricChatClefGetItemTargetCountReader reader = profile.tracked()
                ? new FabricChatClefInventoryTargetCountReader(profile.targetMatchIds())
                : () -> FabricChatClefGetItemCountObservation.unavailable("not_applicable");
        return new FabricChatClefGetItemEffectTracker(profile, reader);
    }

    static FabricChatClefGetItemEffectTracker withReader(
            FabricChatClefGetItemEffectProfile profile,
            FabricChatClefGetItemTargetCountReader countReader
    ) {
        return new FabricChatClefGetItemEffectTracker(
                profile,
                countReader
        );
    }

    public boolean tracked() {
        return profile.tracked();
    }

    @Override
    public FabricChatClefCommandResultDataPayload fromMatchingCompletion(
            FabricChatClefCommandResultDataPayload basePayload
    ) {
        return resultProjector.fromMatchingCompletion(basePayload, this);
    }

    synchronized FabricChatClefGetItemEffectEvidence terminalEvidence() {
        if (terminalEvidence == null) {
            FabricChatClefGetItemCountObservation after = profile.tracked()
                    ? readOrUnavailable("after_reader_returned_null")
                    : FabricChatClefGetItemCountObservation.unavailable("not_applicable");
            terminalEvidence = FabricChatClefGetItemEffectEvidence.evaluate(
                    profile,
                    before,
                    after
            );
        }
        return terminalEvidence;
    }

    private FabricChatClefGetItemCountObservation readOrUnavailable(String nullReason) {
        FabricChatClefGetItemCountObservation observation = countReader.read();
        return observation == null
                ? FabricChatClefGetItemCountObservation.unavailable(nullReason)
                : observation;
    }
}
