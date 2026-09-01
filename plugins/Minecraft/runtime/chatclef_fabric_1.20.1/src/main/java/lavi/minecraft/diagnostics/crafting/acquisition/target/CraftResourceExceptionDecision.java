package lavi.minecraft.diagnostics.crafting.acquisition.target;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260901_kpopmodder: Return bounded exception evidence without influencing exception propagation.
public record CraftResourceExceptionDecision(
        CraftResourceExceptionDisposition disposition,
        String eventName,
        boolean emissionRequested,
        boolean stackIncluded,
        int framesAvailable,
        int framesSelected,
        int framesOmitted,
        List<String> selectedStackFrames,
        String fingerprint,
        Optional<String> exceptionType,
        Optional<String> exceptionMessage) {

    public CraftResourceExceptionDecision {
        disposition = Objects.requireNonNull(disposition, "disposition");
        eventName = Objects.requireNonNull(eventName, "eventName");
        selectedStackFrames = List.copyOf(Objects.requireNonNull(
                selectedStackFrames,
                "selectedStackFrames"
        ));
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        exceptionType = Objects.requireNonNull(exceptionType, "exceptionType");
        exceptionMessage = Objects.requireNonNull(exceptionMessage, "exceptionMessage");
    }
}
