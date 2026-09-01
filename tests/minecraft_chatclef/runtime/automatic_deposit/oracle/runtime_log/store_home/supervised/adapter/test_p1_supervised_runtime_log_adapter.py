#20260901_kpopmodder: Apply the production scanner before supervised P1 identity projection.
from __future__ import annotations

import unittest

from minecraft_chatclef.runtime.automatic_deposit.evidence.latest_log_delta_result import (
    _create_latest_log_delta_result,
)
from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised._test_fixture import (
    ready_p1_supervised_live_fixture,
    supervised_p1_log_delta,
    supervised_store_home_line,
)

from .p1_supervised_runtime_log_adapter import (
    adapt_p1_supervised_store_home_runtime_log,
)


class P1SupervisedRuntimeLogAdapterTests(unittest.TestCase):
    def test_adapts_complete_production_delta_with_actual_submitted_id(self):
        result = _adapt(
            supervised_p1_log_delta("lavi-gui-request-17"),
        )

        self.assertTrue(result.ok, result.reason)
        self.assertEqual("lavi-gui-request-17", result.command_request_id)

    def test_malformed_target_line_fails_closed(self):
        malformed = supervised_store_home_line(
            3,
            "STORE_HOME_CANDIDATE_ACTIVATED",
            "candidateActivated=true",
            "lavi-gui-request-17",
        ).replace(" diagnosticCaptureStatus=complete", "")
        text = malformed + "\n"
        delta = _create_latest_log_delta_result(
            True,
            "LATEST_LOG_DELTA_READ",
            None,
            100,
            100 + len(text.encode("utf-8")),
            text,
            "utf-8",
            "a" * 64,
        )

        result = _adapt(delta)

        self.assertFalse(result.ok)
        self.assertTrue(
            result.reason.startswith("P1_SUPERVISED_PRODUCTION_LOG_SCAN_FAILED")
        )


def _adapt(delta):
    fixture = ready_p1_supervised_live_fixture()
    position = ", ".join(str(value) for value in fixture.trusted_position)
    canonical = "|".join(
        (
            fixture.world_key,
            fixture.dimension,
            *(str(value) for value in fixture.trusted_position),
        )
    )
    return adapt_p1_supervised_store_home_runtime_log(
        delta,
        expected_candidate_position=position,
        expected_run_manifest_id="opaque-p1-run",
        expected_command_request_id="lavi-gui-request-17",
        expected_command_session_id="fabric-chatclef-session-1",
        expected_world_key=fixture.world_key,
        expected_dimension=fixture.dimension,
        expected_destination_canonical_key=canonical,
        expected_destination_id=fixture.trusted_destination_id,
    )


if __name__ == "__main__":
    unittest.main()
