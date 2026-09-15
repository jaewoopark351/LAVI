package lavi.minecraft.task.find;

import java.io.*;
import java.nio.charset.StandardCharsets;
import lavi.minecraft.diagnostics.FindDiagnosticEmitterBridge;
import static lavi.minecraft.task.find.FindRuntimeHarness.*;

/** Checks actual formatting/admission/stdout capture, not Minecraft log-file persistence. */
public final class FindDiagnosticHarness {
    public static void main(String[] args) throws Exception {
        int before=checks;
        var sink=new ByteArrayOutputStream(); var previous=System.out;
        try (var output=new PrintStream(sink, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            var m=reset(); FindDiagnosticEmitterBridge.reset(true);
            var t=start("find entity minecraft:villager"); expire(t); for(int i=0;i<100;i++)tick(t);
            check(!t.isFinished(),"actual output path does not stop a long search");
            entity(m,2); tick(t);
            String text=sink.toString(StandardCharsets.UTF_8);
            check(t.outcome().code().equals("ARRIVED"),"real output path preserves gameplay outcome");
            check(text.contains("event=FIND ") && text.contains("reason=explore_started"),"actual emitter writes exploration event");
            check(text.contains("event=FIND_TERMINAL") && text.contains("code=ARRIVED"),"terminal uses reserved existing event family");
            check(text.contains("findOperationId="+t.outcome().operationId()),"actual output retains operation identity");
            check(text.contains("traceId=") && text.contains("clientTickId=") && text.contains("taskInstanceId="),"canonical lifecycle identifiers emitted");
            check(text.contains("resolutionSource=registry_id") && text.contains("id=minecraft:villager"),"actual emitter exposes language-neutral ID resolution");
            check(text.lines().filter(s->s.contains("reason=scan_complete")).count()<=8,"owner repetition cap reaches actual emitter");
            check(FindDiagnosticEmitterBridge.completed()>0,"real session records emission completion");
            check(text.contains("searchLifetime=UNTIL_FOUND_OR_STOP"),"actual emitter exposes continuous-search policy");
            check(text.contains("approachActiveTimeoutMs=90000"),"actual emitter exposes separate approach budget");
            String evidence=text;

            sink.reset(); m=reset(); FindDiagnosticEmitterBridge.reset(true);
            entity(m,20); t=start("find entity minecraft:villager"); expireApproach(t); tick(t);
            text=sink.toString(StandardCharsets.UTF_8);
            check(t.outcome().code().equals("APPROACH_TIMEOUT"),"bounded selected-target approach still terminates");
            check(text.contains("reason=approach_time_limit") && text.contains("approachActiveMs=90000"),
                    "actual emitter writes evaluated approach timeout budget");
            check(text.contains("event=FIND_TERMINAL") && text.contains("code=APPROACH_TIMEOUT"),
                    "actual output carries the distinct timeout terminal");
            evidence += text;

            sink.reset(); m=reset(); FindDiagnosticEmitterBridge.reset(false);
            t=start("find entity minecraft:villager"); entity(m,2); tick(t);
            check(t.outcome().code().equals("ARRIVED") && sink.size()==0,"real OFF mode emits no lines and preserves result");
            check(FindDiagnosticEmitterBridge.completed()==0,"OFF has no admitted emissions");

            sink.reset(); m=reset(); FindDiagnosticEmitterBridge.reset(true);
            FindDiagnosticEmitterBridge.exhaustOrdinaryBudget();
            t=start("find entity minecraft:villager"); entity(m,2); tick(t);
            text=sink.toString(StandardCharsets.UTF_8);
            check(text.contains("event=FIND_TERMINAL") && text.contains("code=ARRIVED"),"ordinary cap does not spend terminal reserve");
            check(!text.contains("reason=scan_complete"),"ordinary detail remains suppressed after cap");
            check(t.outcome().code().equals("ARRIVED"),"admission cap does not alter search result");
            System.setOut(previous);
            System.out.println("ACTUAL_FIND_EMITTER_OUTPUT_BEGIN");
            System.out.print(evidence);
            System.out.println("ACTUAL_FIND_EMITTER_OUTPUT_END");
            System.out.println("ACTUAL_DIAGNOSTIC_OUTPUT_CHECKS="+(checks-before)+"; TEST FACADE, REAL EMITTER/MODE/ADMISSION/UTF8/STDOUT, NOT GAME LOG FILE");
        } finally { System.setOut(previous); FindDiagnosticEmitterBridge.enabled=false; }
    }
}
