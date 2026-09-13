package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.gotoresult;

//20260913_kpopmodder: Freeze only stored Task terminal facts for diagnostic output.
public record GotoTerminalTaskSnapshot(
        String owner, String identity, String arrived, String failureReason,
        boolean boundRootMatched, String terminationKind
) {
}
