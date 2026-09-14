package lavi.minecraft.task.container.deposit.auto.rearm;

import java.util.LinkedHashSet;
import java.util.Set;

//20260914_kpopmodder: Remember attempted gameplay conditions by destination without identity recharges.
final class AutoDepositFailureHistory {
    private static final int MAX_CONDITIONS = 128;
    private final Set<Condition> attempted = new LinkedHashSet<>();

    boolean contains(String scope, String condition) {
        return condition == null || attempted.contains(new Condition(scope, condition));
    }

    void record(String scope, String condition) {
        if (condition != null) {
            attempted.add(new Condition(scope, condition));
            if (attempted.size() > MAX_CONDITIONS) attempted.remove(attempted.iterator().next());
        }
    }

    void remove(String scope, String condition) { attempted.remove(new Condition(scope, condition)); }
    void clear() { attempted.clear(); }

    private record Condition(String scope, String condition) { }
}
