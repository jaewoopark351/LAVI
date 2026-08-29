package lavi.minecraft.diagnostics.formatting;

//20260828_kpopmodder: Keep one immutable result for bounded diagnostic event formatting.
public record DiagnosticBoundedEventText(
        String text,
        boolean partial,
        int omittedOptionalFieldCount) {
}
