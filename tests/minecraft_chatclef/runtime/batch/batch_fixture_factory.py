#20260818_kpopmodder: Build deterministic batch decision fixtures without live submission.
from __future__ import annotations


def batch_environment_fixture(
    command: str,
    invocation_id: str,
) -> dict[str, object]:
    return {"command": command, "invocation_id": invocation_id}


def completed_command_result_fixture() -> dict[str, object]:
    return {
        "preflight": {"status": "ok"},
        "observation": {
            "submission_outcome": "accepted",
            "gradio_submit_call_count": 1,
            "adapter_command_request_count": "unknown",
            "automatic_resubmit_count": 0,
            "automatic_rerun_count": 0,
            "connection_state_verified": True,
            "terminal_lifecycle_observed": True,
            "terminal_status": "completed",
            "active_request_clear": True,
            "active_clear_observation": "same_snapshot",
            "runtime_reported_completion": True,
            "observer_timeout": False,
            "reconciliation_required": True,
        },
    }


def complete_inventory_baseline_fixture() -> dict[str, object]:
    return {
        "ok": True,
        "reason": "fixture baseline",
        "player_data_path": "fixture-player.dat",
        "snapshot_mtime_ns": 100,
        "target_item_id": "minecraft:fixture_item",
        "requested_count": 1,
        "target_count_before": 4,
    }


def complete_gameplay_checkpoint_fixture() -> dict[str, bool]:
    return {
        "gameplay_observation_complete": True,
        "gameplay_effect_observed": True,
        "expected_gameplay_effect_verified": True,
        "partial_gameplay_effect_observed": False,
        "unexpected_effect_observed": False,
        "prohibited_effect_absence_verified": True,
    }
