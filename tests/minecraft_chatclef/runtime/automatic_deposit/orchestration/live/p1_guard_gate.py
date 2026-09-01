#20260901_kpopmodder: Convert one exact guard observation into a fail-closed gate.
from __future__ import annotations

from collections.abc import Callable

from minecraft_chatclef.runtime.submission.guard_state import OneShotGuardState

from .p1_guard_gate_result import P1GuardGateResult
from .p1_guard_observation_reader import read_p1_guard_observation


_GUARD_REASONS = {
    OneShotGuardState.LIVE_RUN_BLOCK_PRESENT: "LIVE_ONE_SHOT_BLOCK_PRESENT",
    OneShotGuardState.RECONCILIATION_REQUIRED: (
        "LIVE_ONE_SHOT_RECONCILIATION_REQUIRED"
    ),
    OneShotGuardState.GUARD_STATE_UNREADABLE: (
        "LIVE_ONE_SHOT_GUARD_STATE_UNREADABLE"
    ),
}


def evaluate_p1_guard_gate(
    repository_root: str,
    reader: Callable[[str], object],
) -> P1GuardGateResult:
    guard_result = read_p1_guard_observation(repository_root, reader)
    observation = guard_result.observation
    if observation is None:
        return P1GuardGateResult(
            clear=False,
            reason="LIVE_ONE_SHOT_GUARD_STATE_UNREADABLE",
            error_type=guard_result.error_type,
        )
    if observation.state is OneShotGuardState.CLEAR:
        return P1GuardGateResult(
            clear=True,
            reason="LIVE_ONE_SHOT_GUARD_CLEAR",
            guard_state=observation.state.value,
            guard_reason=observation.reason,
        )
    return P1GuardGateResult(
        clear=False,
        reason=_GUARD_REASONS.get(
            observation.state,
            "LIVE_ONE_SHOT_GUARD_STATE_UNREADABLE",
        ),
        guard_state=observation.state.value,
        guard_reason=observation.reason,
    )
