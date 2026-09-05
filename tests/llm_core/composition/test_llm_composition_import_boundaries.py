#20260905_kpopmodder: Verify clean LLM composition imports in both directions.
from __future__ import annotations

import subprocess
import sys

import pytest


@pytest.mark.parametrize(
    "modules",
    (
        (
            "llm_core.composition.llm_compatibility_graph_installer",
            "llm_core.llm_component",
        ),
        (
            "llm_core.llm_component",
            "llm_core.composition.llm_compatibility_graph_installer",
        ),
        (
            "llm_core.input_routing.routed_input_dispatch_component_graph",
            "llm_core.input_routing.routed_input_dispatch_coordinator",
        ),
        (
            "llm_core.input_routing.routed_input_dispatch_coordinator",
            "llm_core.input_routing.routed_input_dispatch_component_graph",
        ),
        (
            "llm_core.input_queue.llm_input_queue_worker_component_graph",
            "llm_core.input_queue_worker",
        ),
        (
            "llm_core.input_queue_worker",
            "llm_core.input_queue.llm_input_queue_worker_component_graph",
        ),
        (
            "llm_core.content.llm_system_prompt_content_component_graph",
            "llm_core.content.llm_system_prompt_content_repository",
        ),
        (
            "llm_core.content.llm_system_prompt_content_repository",
            "llm_core.content.llm_system_prompt_content_component_graph",
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
