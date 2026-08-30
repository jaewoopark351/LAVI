package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Own only Baritone process and command diagnostic summaries.
final class BaritoneProcessDiagnosticFormatter {
    private BaritoneProcessDiagnosticFormatter() {
    }

    static String summarizeProcess(IBaritoneProcess process) {
        if (process == null) {
            return "none";
        }
        return BaritoneDiagnosticIdentityFormatter.className(process)
                + "#"
                + BaritoneDiagnosticIdentityFormatter.identity(process)
                + ",displayName="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(process::displayName)
                + ",active="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(process::isActive)
                + ",temporary="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(process::isTemporary);
    }

    static String commandType(PathingCommand command) {
        return command == null ? "none" : String.valueOf(command.commandType);
    }

    static String commandGoalType(PathingCommand command) {
        return command == null ? "none" : BaritoneDiagnosticIdentityFormatter.className(command.goal);
    }

    static String commandGoalSummary(PathingCommand command) {
        return command == null ? "none" : BaritonePathDiagnosticFormatter.summarizeGoal(command.goal);
    }
}
