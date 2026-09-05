#20260905_kpopmodder: Preserve the legacy STOP terminal validator as a thin facade.
from __future__ import annotations

from .stop_control_terminal_decision import StopControlTerminalDecision
from .validation import (
    StopControlTerminalIdentityValidator,
    StopControlTerminalNoMutationProfileValidator,
    StopControlTerminalShapeValidator,
    StopControlTerminalSuccessProfileValidator,
    StopControlTerminalTargetValidator,
    StopControlTerminalTickValidator,
    StopControlTerminalUncertaintyProfileValidator,
    StopControlTerminalValidationCoordinator,
)
from .validation.stop_control_terminal_profile_schema import (
    STOP_CONTROL_NO_MUTATION_REASONS,
    STOP_CONTROL_REQUIRED_DATA_FIELDS,
    STOP_CONTROL_REQUIRED_RESULT_FIELDS,
    STOP_CONTROL_SUCCESS_PROFILES,
    STOP_CONTROL_UNKNOWN_REASONS,
    STOP_VERIFY_MAX_LATER_TICKS,
)


class StopControlTerminalResultValidator:
    STOP_VERIFY_MAX_LATER_TICKS = STOP_VERIFY_MAX_LATER_TICKS
    _REQUIRED_RESULT_FIELDS = STOP_CONTROL_REQUIRED_RESULT_FIELDS
    _REQUIRED_DATA_FIELDS = STOP_CONTROL_REQUIRED_DATA_FIELDS
    _SUCCESS_PROFILES = STOP_CONTROL_SUCCESS_PROFILES
    _NO_MUTATION_REASONS = STOP_CONTROL_NO_MUTATION_REASONS
    _UNKNOWN_REASONS = STOP_CONTROL_UNKNOWN_REASONS

    def __init__(self):
        target_validator = StopControlTerminalTargetValidator()
        tick_validator = StopControlTerminalTickValidator()
        self._coordinator = StopControlTerminalValidationCoordinator(
            shape_validator=StopControlTerminalShapeValidator(
                target_validator=target_validator,
                tick_validator=tick_validator,
            ),
            identity_validator=StopControlTerminalIdentityValidator(),
            success_validator=StopControlTerminalSuccessProfileValidator(
                target_validator=target_validator,
                tick_validator=tick_validator,
            ),
            no_mutation_validator=(
                StopControlTerminalNoMutationProfileValidator(
                    target_validator=target_validator,
                    tick_validator=tick_validator,
                )
            ),
            uncertainty_validator=(
                StopControlTerminalUncertaintyProfileValidator(
                    target_validator=target_validator,
                    tick_validator=tick_validator,
                )
            ),
        )

    def inspect(
        self,
        envelope: object,
        result: object,
        tracker: object,
    ) -> StopControlTerminalDecision:
        return self._coordinator.inspect(envelope, result, tracker)


__all__ = ("StopControlTerminalResultValidator",)
