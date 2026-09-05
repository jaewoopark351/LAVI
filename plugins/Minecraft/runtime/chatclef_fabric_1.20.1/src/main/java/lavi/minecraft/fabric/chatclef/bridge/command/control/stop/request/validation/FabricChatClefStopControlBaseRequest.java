package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Preserve the exact result-echo identity before semantic STOP validation begins.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;

public final class FabricChatClefStopControlBaseRequest {
    private final FabricChatClefStopControlIdentity identity;
    private final long javaSocketGeneration;
    private final String fingerprint;
    private final String acceptedSessionIdAtValidation;
    private final Long acceptedServerConnectionGenerationAtValidation;

    public FabricChatClefStopControlBaseRequest(
            FabricChatClefStopControlIdentity identity,
            long javaSocketGeneration,
            String fingerprint
    ) {
        this(identity, javaSocketGeneration, fingerprint, null, null);
    }

    private FabricChatClefStopControlBaseRequest(
            FabricChatClefStopControlIdentity identity,
            long javaSocketGeneration,
            String fingerprint,
            String acceptedSessionIdAtValidation,
            Long acceptedServerConnectionGenerationAtValidation
    ) {
        this.identity = identity;
        this.javaSocketGeneration = javaSocketGeneration;
        this.fingerprint = fingerprint;
        this.acceptedSessionIdAtValidation = acceptedSessionIdAtValidation;
        this.acceptedServerConnectionGenerationAtValidation = acceptedServerConnectionGenerationAtValidation;
    }

    public FabricChatClefStopControlIdentity identity() {
        return identity;
    }

    public long javaSocketGeneration() {
        return javaSocketGeneration;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public FabricChatClefStopControlBaseRequest withAcceptedValidationIdentity(
            String acceptedSessionId,
            Long acceptedServerConnectionGeneration
    ) {
        return new FabricChatClefStopControlBaseRequest(
                identity,
                javaSocketGeneration,
                fingerprint,
                acceptedSessionId,
                acceptedServerConnectionGeneration
        );
    }

    public String acceptedSessionIdAtValidation() {
        return acceptedSessionIdAtValidation;
    }

    public Long acceptedServerConnectionGenerationAtValidation() {
        return acceptedServerConnectionGenerationAtValidation;
    }
}
