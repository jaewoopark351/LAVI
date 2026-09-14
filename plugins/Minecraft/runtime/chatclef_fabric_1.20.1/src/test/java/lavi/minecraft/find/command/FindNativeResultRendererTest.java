//#if MC == 12001
package lavi.minecraft.find.command;

import lavi.minecraft.find.model.FindRequest;
import lavi.minecraft.find.result.FindOutcome;
import lavi.minecraft.find.result.FindTerminalPhaseEvidence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FindNativeResultRendererTest {
    private String render(FindOutcome outcome) { return FindNativeResultRenderer.render(outcome, outcome.terminalEvidence().userRoot()); }
    private FindOutcome failure(String mode, String reason, boolean discovered) {
        var request = new FindRequest("entity", "minecraft:villager", mode, "a".repeat(64), 1);
        return new FindOutcome("op", request, reason.contains("unreachable") || reason.equals("exploration_route_unavailable") ? "UNREACHABLE" : "TIMEOUT",
                false, "", null, 4, discovered ? 1 : 0, false, reason,
                new FindTerminalPhaseEvidence("op", request, new Object(), discovered
                        ? FindTerminalPhaseEvidence.Phase.APPROACHING : FindTerminalPhaseEvidence.Phase.EXPLORING,
                        discovered, true, false, discovered, false, true));
    }
    @Test void beforeAndAfterDiscoveryFailureTextCannotPromoteExplorationToFound() {
        String searching = render(failure("report", "exploration_route_unavailable", false));
        assertTrue(searching.contains("탐험을 계속할 수 없어서")); assertFalse(searching.contains("발견했지만"));
        String approach = render(failure("approach", "post_discovery_approach_unreachable", true));
        assertTrue(approach.contains("발견했지만")); assertFalse(approach.contains("찾았어"));
        assertTrue(render(failure("report", "discovery_deadline_exhausted", false)).contains("탐색을 완료하지는"));
        assertTrue(render(failure("approach", "approach_deadline_exhausted", true)).contains("접근 시간"));
        assertTrue(render(failure("report", "exploration_step_deadline_exhausted", false)).contains("탐험 이동의"));
        assertTrue(render(failure("approach", "native_approach_step_deadline_exhausted", true)).contains("접근 이동의"));
        assertFalse(render(failure("approach", "parent_deadline_exhausted", true)).contains("발견"));
        assertEquals("찾기 결과를 확인할 수 없어.", render(failure("report", "post_discovery_approach_unreachable", false)));
        var outcome = failure("approach", "post_discovery_approach_unreachable", true);
        assertEquals("찾기 결과를 확인할 수 없어.", FindNativeResultRenderer.render(outcome, new Object()));
        assertEquals("찾기 결과를 확인할 수 없어.", FindNativeResultRenderer.render(outcome));
    }
}

//#endif
