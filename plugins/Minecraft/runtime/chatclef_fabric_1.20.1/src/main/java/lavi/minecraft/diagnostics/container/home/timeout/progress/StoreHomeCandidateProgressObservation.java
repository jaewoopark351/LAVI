package lavi.minecraft.diagnostics.container.home.timeout.progress;

//20260828_kpopmodder: Added this type file to keep one primary Java type per file.
public record StoreHomeCandidateProgressObservation(
        String progressKind,
        String fingerprint,
        boolean semanticStateChanged,
        boolean sampleDue) {

    public boolean shouldEmit() {
        return semanticStateChanged || sampleDue;
    }
}
