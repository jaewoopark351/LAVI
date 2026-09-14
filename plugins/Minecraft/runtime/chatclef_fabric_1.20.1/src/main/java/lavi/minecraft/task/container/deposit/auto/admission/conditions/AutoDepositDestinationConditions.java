package lavi.minecraft.task.container.deposit.auto.admission.conditions;

import java.util.List;
import java.util.Objects;

//20260914_kpopmodder: Freeze the observed candidate set independently of ordering and cache identity.
record AutoDepositDestinationConditions(String scope, List<String> states) {
    AutoDepositDestinationConditions {
        Objects.requireNonNull(scope, "scope");
        states = List.copyOf(states);
    }
}
