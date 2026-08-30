package lavi.minecraft.diagnostics.mining.baritone.process;

import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

import java.util.List;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritoneProcessCancelDiagnostics {
    private BaritoneProcessCancelDiagnostics() {
    }

    public static void log(Object manager,
                           String phase,
                           IBaritoneProcess inControlLastTick,
                           IBaritoneProcess inControlThisTick,
                           PathingCommand command,
                           List<IBaritoneProcess> activeProcesses) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String managerIdentity = BaritonePathObjectFormatters.identity(manager);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PROCESS_CANCEL_EVERYTHING_BOUNDARY",
                phase,
                BaritonePathObjectFormatters.className(inControlLastTick),
                BaritonePathObjectFormatters.className(inControlThisTick),
                BaritonePathObjectFormatters.commandType(command),
                BaritonePathObjectFormatters.commandGoalType(command),
                BaritoneActiveProcessPayload.semanticFingerprint(activeProcesses)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_PROCESS_CANCEL_EVERYTHING_BOUNDARY",
                "baritone_process_cancel_everything_boundary",
                "baritone_pathing_control_manager_observer",
                "pathing_control_manager_cancel_everything_" + phase,
                "baritone_process_cancel|" + managerIdentity,
                fingerprint,
                () -> new Object[]{
                        "phase", phase,
                        "managerIdentity", managerIdentity,
                        "inControlLastTickSummary", BaritonePathObjectFormatters.summarizeProcess(inControlLastTick),
                        "inControlThisTickSummary", BaritonePathObjectFormatters.summarizeProcess(inControlThisTick),
                        "commandType", BaritonePathObjectFormatters.commandType(command),
                        "commandGoalType", BaritonePathObjectFormatters.commandGoalType(command),
                        "commandGoalSummary", BaritonePathObjectFormatters.commandGoalSummary(command),
                        "activeProcessCount", BaritoneActiveProcessPayload.count(activeProcesses),
                        "activeProcessSummary", BaritoneActiveProcessPayload.detailedSummary(activeProcesses)
                }
        );
    }
}
