package lavi.minecraft.diagnostics.mining.baritone.process;

import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

import java.util.List;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritoneProcessTickDiagnostics {
    private BaritoneProcessTickDiagnostics() {
    }

    public static void log(Object manager,
                           IBaritoneProcess inControlLastTick,
                           IBaritoneProcess inControlThisTick,
                           PathingCommand command,
                           List<IBaritoneProcess> activeProcesses) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String managerIdentity = BaritonePathObjectFormatters.identity(manager);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PROCESS_CONTROL_TICK",
                BaritonePathObjectFormatters.className(inControlLastTick),
                BaritonePathObjectFormatters.className(inControlThisTick),
                BaritonePathObjectFormatters.commandType(command),
                BaritonePathObjectFormatters.commandGoalType(command),
                BaritoneActiveProcessPayload.semanticFingerprint(activeProcesses)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_PROCESS_CONTROL_TICK",
                "baritone_process_control_tick",
                "baritone_pathing_control_manager_observer",
                "pathing_control_manager_pre_tick_returned",
                "baritone_process_control|" + managerIdentity,
                fingerprint,
                () -> new Object[]{
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
