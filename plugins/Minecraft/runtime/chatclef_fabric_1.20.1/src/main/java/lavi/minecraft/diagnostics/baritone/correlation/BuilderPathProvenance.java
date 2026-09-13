package lavi.minecraft.diagnostics.baritone.correlation;

import lavi.minecraft.diagnostics.baritone.builder.BuilderPathSnapshot;
import java.util.ArrayList;
import java.util.Collections;

//20260913_kpopmodder: Store compact immutable calculation origin independently from the legacy registry.
public record BuilderPathProvenance(long calculation, String request, String taskContext, String goal, String phase,
                                    String firstInput, String secondInput, BuilderPathSnapshot snapshot) {
    public BuilderPathProvenance transformed(String phase, String first, String second, BuilderPathSnapshot result) {
        return new BuilderPathProvenance(calculation, request, taskContext, goal, phase, first, second, result);
    }

    public Object[] fields() {
        ArrayList<Object> fields = new ArrayList<>();
        Collections.addAll(fields, "calculationGeneration", calculation,
                "requestedTaskContext", taskContext, "requestedGoal", goal, "pathPhase", phase, "firstInputPathIdentity", firstInput,
                "secondInputPathIdentity", secondInput, "originStatus", calculation < 0 ? "UNBOUND" : "BOUND");
        for (String item : request.split(";")) {
            int delimiter = item.indexOf('=');
            if (delimiter <= 0) continue;
            fields.add("requested_" + item.substring(0, delimiter));
            fields.add(item.substring(delimiter + 1));
        }
        return fields.toArray();
    }
}
