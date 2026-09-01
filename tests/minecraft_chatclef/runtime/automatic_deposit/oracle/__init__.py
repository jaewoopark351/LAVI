"""Pure automatic-deposit evidence parsers and verdicts."""

from .diagnostic_event_parser import parse_bounded_diagnostic_event
from .matrix_verdict import AutomaticDepositVerdict
from .row_verdict import evaluate_automatic_deposit_row

__all__ = (
    "AutomaticDepositVerdict",
    "evaluate_automatic_deposit_row",
    "parse_bounded_diagnostic_event",
)
