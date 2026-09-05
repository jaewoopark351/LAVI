#20260905_kpopmodder: Verifies routed-response modules import in clean permutations.
from __future__ import annotations

import subprocess
import sys

import pytest


@pytest.mark.parametrize(
    "modules",
    (
        (
            "llm_core.routed_response.routed_response_sink_delivery",
            "llm_core.routed_response.composition",
        ),
        (
            "llm_core.routed_response.composition",
            "llm_core.routed_response.routed_response_sink_delivery",
        ),
        (
            "llm_core.routed_response.routed_external_response_publisher",
            "llm_core.routed_response.composition",
        ),
    ),
)
def test_clean_import_permutations(modules):
    statement = "; ".join(f"import {module}" for module in modules)
    result = subprocess.run(
        [sys.executable, "-c", statement],
        check=False,
        capture_output=True,
        text=True,
    )

    assert result.returncode == 0, result.stderr
