#20260901_kpopmodder: Export synthetic runtime-evidence correlation used by hermetic matrix tests.
"""Synthetic runtime-log correlation retained for hermetic evidence tests."""

from .synthetic_evidence_correlation import (
    parse_correlated_synthetic_runtime_events,
    verify_synthetic_runtime_log_delta,
)

__all__ = (
    "parse_correlated_synthetic_runtime_events",
    "verify_synthetic_runtime_log_delta",
)
