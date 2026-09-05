package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Expose strict STOP validation while delegating independent validation stages.

import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;

import java.util.Optional;

public final class FabricChatClefStopControlRequestValidator {
    private final FabricChatClefStopControlBaseRequestValidator baseValidator;
    private final FabricChatClefStopControlProfileValidator profileValidator;

    public FabricChatClefStopControlRequestValidator() {
        FabricChatClefStopControlJsonFieldValidator fields = new FabricChatClefStopControlJsonFieldValidator();
        this.baseValidator = new FabricChatClefStopControlBaseRequestValidator(
                fields,
                new FabricChatClefStopControlRequestFingerprint()
        );
        this.profileValidator = new FabricChatClefStopControlProfileValidator(fields);
    }

    public FabricChatClefStopControlBaseValidation validateBase(JsonNode envelope, long javaSocketGeneration) {
        return baseValidator.validate(envelope, javaSocketGeneration);
    }

    public FabricChatClefStopControlValidationDecision validateProfile(
            FabricChatClefStopControlBaseRequest base,
            JsonNode envelope,
            Optional<FabricChatClefAcceptedSessionIdentity> acceptedIdentity,
            long nowMs
    ) {
        return profileValidator.validate(base, envelope, acceptedIdentity, nowMs);
    }
}
