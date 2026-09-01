#20260901_kpopmodder: Select exactly the single canonical P1 matrix declaration.
from __future__ import annotations

from ....scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ....scenario.matrix_row import AutomaticDepositMatrixRow


def select_p1_matrix_row() -> AutomaticDepositMatrixRow | None:
    rows = tuple(row for row in automatic_deposit_matrix_catalog() if row.row_id == "P1")
    return rows[0] if len(rows) == 1 else None
