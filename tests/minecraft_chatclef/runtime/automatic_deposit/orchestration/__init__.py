"""Fail-closed automatic-deposit matrix orchestration."""

from .live_matrix_application import run_automatic_deposit_live_matrix_application
from .matrix_runner import run_automatic_deposit_matrix_row

__all__ = (
    "run_automatic_deposit_live_matrix_application",
    "run_automatic_deposit_matrix_row",
)
