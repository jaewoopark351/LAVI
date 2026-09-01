#20260831_kpopmodder: Define the complete immutable automatic-deposit runtime matrix.
from __future__ import annotations

from .evidence_requirement import AutomaticDepositEvidenceRequirement
from .matrix_row import AutomaticDepositMatrixRow
from .transport_mode import AutomaticDepositTransportMode


def automatic_deposit_matrix_catalog() -> tuple[AutomaticDepositMatrixRow, ...]:
    automatic = AutomaticDepositTransportMode.NO_COMMAND_AUTOMATIC_TRIGGER
    manual = AutomaticDepositTransportMode.OPERATOR_MANUAL_OBSERVE_ONLY
    runtime = "RUNTIME_LOG"
    fixture = "OPERATOR_FIXTURE"
    artifact = "ARTIFACT_PREFLIGHT"
    java = "JAVA_DETERMINISTIC"
    gameplay = "GAMEPLAY_OBSERVATION"

    def required(
        key: str,
        owner: str = runtime,
        expected_value: str = "true",
    ) -> AutomaticDepositEvidenceRequirement:
        return AutomaticDepositEvidenceRequirement(key, owner, expected_value)

    common_fixture = (
        "fixture_fingerprint",
        "world_snapshot_id",
        "inventory_snapshot_id",
        "operator_confirmed",
        "carry_on_state",
    )
    common_evidence = (
        required("artifact_identity_verified", artifact),
        required("fixture_identity_verified", fixture),
        required("runtime_log_complete"),
        required("runtime_reported_completion"),
        required("gameplay_observation_complete", gameplay),
        required("expected_gameplay_effect_verified", gameplay),
        required(
            "partial_gameplay_effect_observed",
            gameplay,
            expected_value="false",
        ),
        required(
            "unexpected_effect_observed",
            gameplay,
            expected_value="false",
        ),
        required("prohibited_effect_absence_verified", gameplay),
        required(
            "helper_submit_call_count",
            "HARNESS_CONTROL",
            expected_value="0",
        ),
    )
    automatic_evidence = common_evidence + (
        required("automatic_trigger_observed"),
    )
    manual_evidence = common_evidence + (
        required("operator_action_observed"),
    )
    handoff_evidence = (
        required("placement_observed"),
        required("post_place_world_reread_observed"),
        required("container_open_observed"),
        required("transfer_observed"),
        required("typed_terminal_observed"),
        required("generation_boundary_verified", java),
        required("one_tick_barrier_verified", java),
    )

    return (
        AutomaticDepositMatrixRow(
            "R1a",
            "automatic general places CHEST with Carry On installed",
            automatic,
            common_fixture
            + (
                "container_type",
                "placement_site",
                "eligible_existing_container_count",
                "placeable_container_available",
            ),
            automatic_evidence + handoff_evidence,
            expected_fixture_values=(
                ("container_type", "CHEST"),
                ("carry_on_state", "INSTALLED"),
                ("eligible_existing_container_count", "0"),
                ("placeable_container_available", "true"),
            ),
            exercises_frozen_handoff=True,
        ),
        AutomaticDepositMatrixRow(
            "R1b",
            "automatic general places CHEST with Carry On absent",
            automatic,
            common_fixture
            + (
                "container_type",
                "placement_site",
                "eligible_existing_container_count",
                "placeable_container_available",
            ),
            automatic_evidence
            + handoff_evidence
            + (required("carry_on_absence_or_linkage_verified"),),
            expected_fixture_values=(
                ("container_type", "CHEST"),
                ("carry_on_state", "ABSENT"),
                ("eligible_existing_container_count", "0"),
                ("placeable_container_available", "true"),
            ),
            exercises_frozen_handoff=True,
        ),
        AutomaticDepositMatrixRow(
            "R2a",
            "automatic general places BARREL with Carry On installed",
            automatic,
            common_fixture
            + (
                "container_type",
                "placement_site",
                "eligible_existing_container_count",
                "placeable_container_available",
            ),
            automatic_evidence + handoff_evidence,
            expected_fixture_values=(
                ("container_type", "BARREL"),
                ("carry_on_state", "INSTALLED"),
                ("eligible_existing_container_count", "0"),
                ("placeable_container_available", "true"),
            ),
            exercises_frozen_handoff=True,
        ),
        AutomaticDepositMatrixRow(
            "R2b",
            "automatic general places BARREL with Carry On absent",
            automatic,
            common_fixture
            + (
                "container_type",
                "placement_site",
                "eligible_existing_container_count",
                "placeable_container_available",
            ),
            automatic_evidence
            + handoff_evidence
            + (required("carry_on_absence_or_linkage_verified"),),
            expected_fixture_values=(
                ("container_type", "BARREL"),
                ("carry_on_state", "ABSENT"),
                ("eligible_existing_container_count", "0"),
                ("placeable_container_available", "true"),
            ),
            exercises_frozen_handoff=True,
        ),
        AutomaticDepositMatrixRow(
            "R3",
            "automatic general uses an existing CHEST directly",
            automatic,
            common_fixture
            + (
                "container_type",
                "existing_container_position",
                "eligible_existing_container_count",
            ),
            automatic_evidence
            + (
                required("placement_attempt_count", expected_value="0"),
                required("obtain_attempt_count", expected_value="0"),
                required("handoff_attempt_count", expected_value="0"),
                required("container_open_observed"),
                required("transfer_observed"),
                required("typed_terminal_observed"),
            ),
            expected_fixture_values=(
                ("container_type", "CHEST"),
                ("carry_on_state", "ABSENT"),
                ("diagnostics_mode", "BOUNDARY"),
                ("eligible_existing_container_count", "1"),
            ),
        ),
        AutomaticDepositMatrixRow(
            "R4",
            "manual @deposit_all places a new CHEST",
            manual,
            common_fixture
            + (
                "container_type",
                "placement_site",
                "eligible_existing_container_count",
                "placeable_container_available",
            ),
            manual_evidence
            + handoff_evidence
            + (
                required("manual_ephemeral_operation_verified"),
                required("persistent_handoff_enabled", expected_value="false"),
            ),
            expected_fixture_values=(
                ("container_type", "CHEST"),
                ("carry_on_state", "ABSENT"),
                ("eligible_existing_container_count", "0"),
                ("placeable_container_available", "true"),
            ),
            expected_operator_action="@deposit_all",
            exercises_frozen_handoff=True,
        ),
        AutomaticDepositMatrixRow(
            "R5",
            "trusted automatic uses a registered BARREL",
            automatic,
            common_fixture
            + (
                "container_type",
                "trusted_destination_fingerprint",
                "trusted_container_position",
                "trusted_binding_count",
            ),
            automatic_evidence
            + (
                required("trusted_binding_verified"),
                required("general_handoff_attempt_count", expected_value="0"),
                required("transfer_observed"),
                required("terminal_reason", expected_value="ALL_STORED"),
            ),
            expected_fixture_values=(
                ("container_type", "BARREL"),
                ("carry_on_state", "INSTALLED"),
                ("trusted_binding_count", "1"),
            ),
        ),
        AutomaticDepositMatrixRow(
            "R6",
            "bounded diagnostics stress preserves gameplay",
            automatic,
            common_fixture + ("diagnostics_mode", "stress_fixture_id"),
            automatic_evidence
            + (
                required("terminal_count_at_least_nine"),
                required("routine_then_abnormal_observed"),
                required("diagnostics_snapshot_bounded"),
                required("gameplay_result_unchanged"),
            ),
            expected_fixture_values=(("diagnostics_mode", "BOUNDARY"),),
        ),
        AutomaticDepositMatrixRow(
            "R7",
            "operator stops an active automatic Store operation",
            manual,
            common_fixture + ("active_operation_id", "diagnostics_mode"),
            manual_evidence
            + (
                required("cancellation_observed"),
                required("finalizer_observed"),
                required("owned_cleanup_observed"),
                required("restart_stale_state_count", expected_value="0"),
            ),
            expected_fixture_values=(("diagnostics_mode", "BOUNDARY"),),
            expected_operator_action="@stop",
        ),
        AutomaticDepositMatrixRow(
            "R8a",
            "trusted destination A is full and B has capacity",
            automatic,
            common_fixture
            + (
                "trusted_destination_fingerprint",
                "trusted_a_position",
                "trusted_b_position",
                "trusted_a_inventory_fingerprint",
                "trusted_b_inventory_fingerprint",
                "trusted_a_full",
                "trusted_b_has_capacity",
            ),
            automatic_evidence
            + (
                required("trusted_a_inventory_delta", expected_value="0"),
                required("trusted_a_cursor_delta", expected_value="0"),
                required("trusted_b_selected"),
                required("transfer_observed"),
                required("terminal_reason", expected_value="ALL_STORED"),
                required("general_fallback_count", expected_value="0"),
            ),
            expected_fixture_values=(
                ("trusted_a_full", "true"),
                ("trusted_b_has_capacity", "true"),
            ),
        ),
        AutomaticDepositMatrixRow(
            "R8b",
            "all trusted destinations are full",
            automatic,
            common_fixture
            + (
                "trusted_destination_fingerprint",
                "trusted_capacity_fingerprint",
                "all_trusted_full",
            ),
            automatic_evidence
            + (
                required("all_trusted_rejections_observed"),
                required("false_success_count", expected_value="0"),
                required("cursor_delta", expected_value="0"),
                required("candidate_exhaustion_observed"),
                required("general_fallback_count", expected_value="0"),
            ),
            expected_fixture_values=(("all_trusted_full", "true"),),
        ),
        AutomaticDepositMatrixRow(
            "R9a",
            "automatic deposit obtains and places a CHEST",
            automatic,
            common_fixture
            + (
                "container_type",
                "acquisition_fixture_id",
                "eligible_existing_container_count",
                "container_item_count",
                "acquisition_route_available",
            ),
            automatic_evidence
            + handoff_evidence
            + (
                required("container_acquisition_observed"),
                required("acquired_container_identity_verified"),
            ),
            expected_fixture_values=(
                ("container_type", "CHEST"),
                ("eligible_existing_container_count", "0"),
                ("container_item_count", "0"),
                ("acquisition_route_available", "true"),
            ),
            exercises_frozen_handoff=True,
        ),
        AutomaticDepositMatrixRow(
            "R9b",
            "container acquisition is impossible",
            automatic,
            common_fixture
            + (
                "acquisition_fixture_id",
                "requested_container_type",
                "eligible_existing_container_count",
                "container_item_count",
                "acquisition_route_available",
            ),
            automatic_evidence
            + (
                required("bounded_refusal_or_terminal_observed"),
                required("unbounded_loop_absent"),
            ),
            expected_fixture_values=(
                ("requested_container_type", "CHEST"),
                ("eligible_existing_container_count", "0"),
                ("container_item_count", "0"),
                ("acquisition_route_available", "false"),
            ),
        ),
        AutomaticDepositMatrixRow(
            "R10",
            "pressure threshold has no safe surplus",
            automatic,
            common_fixture
            + (
                "protected_inventory_fingerprint",
                "occupied_slot_count",
                "inventory_slot_count",
                "pressure_threshold",
                "pressure_triggered",
            ),
            automatic_evidence
            + (
                required("terminal_reason", expected_value="NO_SAFE_SURPLUS_WAIT"),
                required("store_root_start_count", expected_value="0"),
                required("transfer_attempt_count", expected_value="0"),
                required("placement_attempt_count", expected_value="0"),
                required("immediate_rerun_count", expected_value="0"),
            ),
            expected_fixture_values=(
                ("pressure_threshold", "0.9"),
                ("pressure_triggered", "true"),
            ),
        ),
        AutomaticDepositMatrixRow(
            "P1",
            "manual @store_home uses a registered trusted container",
            manual,
            common_fixture
            + (
                "trusted_destination_fingerprint",
                "trusted_container_position",
                "trusted_binding_count",
            ),
            manual_evidence
            + (
                required("trusted_binding_verified"),
                required("transfer_observed"),
                required("typed_terminal_observed"),
            ),
            expected_fixture_values=(("trusted_binding_count", "1"),),
            expected_operator_action="@store_home",
        ),
        AutomaticDepositMatrixRow(
            "P2",
            "generic Minecraft interactions remain unchanged",
            manual,
            common_fixture + ("interaction_fixture_fingerprint",),
            manual_evidence
            + (
                required("chest_open_preserved"),
                required("door_preserved"),
                required("trapdoor_preserved"),
                required("bed_preserved"),
                required("button_preserved"),
                required("lever_preserved"),
                required("block_placement_preserved"),
                required("item_use_preserved"),
                required("generic_right_click_preserved"),
            ),
            expected_operator_action="operator generic-interaction sequence",
        ),
        AutomaticDepositMatrixRow(
            "P3",
            "interruptions do not leak operation-owned state",
            manual,
            common_fixture + ("interruption_fixture_id",),
            manual_evidence
            + (
                required("death_cleanup_verified"),
                required("dimension_cleanup_verified"),
                required("disconnect_reconnect_cleanup_verified"),
                required("stale_owned_state_count", expected_value="0"),
                required("unrelated_state_cleanup_count", expected_value="0"),
            ),
            expected_operator_action="operator interruption sequence",
        ),
        AutomaticDepositMatrixRow(
            "P4",
            "Carry On pickup, placement, and absence contracts",
            automatic,
            common_fixture
            + (
                "carry_on_installed_fixture_fingerprint",
                "carry_on_absent_fixture_fingerprint",
                "carry_on_installed_run_id",
                "carry_on_absent_run_id",
            ),
            automatic_evidence
            + (
                required("pickup_not_carrying_to_carrying_observed"),
                required("placement_carrying_to_not_carrying_observed"),
                required("carry_on_absent_load_verified"),
                required("installed_fixture_identity_verified", fixture),
                required("absent_fixture_identity_verified", fixture),
                required("paired_chatclef_artifact_identity_equal", artifact),
            ),
        ),
        AutomaticDepositMatrixRow(
            "P5",
            "diagnostics OFF and BOUNDARY preserve gameplay parity",
            automatic,
            common_fixture
            + (
                "off_run_id",
                "boundary_run_id",
                "off_fixture_fingerprint",
                "boundary_fixture_fingerprint",
            ),
            automatic_evidence
            + (
                required("off_boundary_gameplay_parity_verified"),
                required("off_boundary_operational_log_parity_verified"),
                required("diagnostics_internal_mutation_verified", java),
                required("off_runtime_log_complete"),
                required("boundary_runtime_log_complete"),
                required("paired_chatclef_artifact_identity_equal", artifact),
                required("paired_gameplay_fixture_identity_equal", fixture),
            ),
        ),
    )
