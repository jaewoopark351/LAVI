"""Immutable automatic-deposit scenario contracts."""

from .matrix_catalog import automatic_deposit_matrix_catalog
from .matrix_row import AutomaticDepositMatrixRow
from .transport_mode import AutomaticDepositTransportMode

__all__ = (
    "AutomaticDepositMatrixRow",
    "AutomaticDepositTransportMode",
    "automatic_deposit_matrix_catalog",
)
