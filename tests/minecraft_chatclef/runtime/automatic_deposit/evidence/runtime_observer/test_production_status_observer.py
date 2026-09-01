#20260901_kpopmodder: Lock the canonical production Fabric status observation boundary.
from __future__ import annotations

import unittest
import threading
from dataclasses import replace

from plugins.Minecraft.fabric.chatclef.session.fabric_chatclef_session_registry import (
    FabricChatClefSessionRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.server.fabric_chatclef_status_snapshot_builder import (
    FabricChatClefStatusSnapshotBuilder,
)

from .production_status_observer import observe_production_fabric_status


class ProductionFabricStatusObserverTests(unittest.TestCase):
    def test_observes_status_built_by_the_actual_production_builder(self):
        session_registry = FabricChatClefSessionRegistry()
        session_registry.upsert(
            session_id="fabric-chatclef-session-1",
            timestamp_ms=1_000,
            protocol_version=1,
            capabilities={"command": True},
            metadata={
                "backend": "fabric_chatclef",
                "loader": "fabric",
                "phase": "phase_4_tick_dispatch",
            },
        )
        builder = FabricChatClefStatusSnapshotBuilder(
            connection_ownership=_ConnectedIdleOwnership(),
            command_lock=threading.RLock(),
            session_registry=session_registry,
        )
        production_payload = builder.build(
            enabled=True,
            last_error=None,
            is_running=True,
            endpoint="ws://127.0.0.1:8765",
            bound_host="127.0.0.1",
            bound_port=8765,
        ).to_dict()

        direct, direct_reason = observe_production_fabric_status(production_payload)
        wrapped, wrapped_reason = observe_production_fabric_status(
            {"details": production_payload}
        )

        self.assertEqual("PRODUCTION_FABRIC_STATUS_OBSERVED", direct_reason)
        self.assertEqual("PRODUCTION_FABRIC_STATUS_OBSERVED", wrapped_reason)
        self.assertEqual(direct, wrapped)
        self.assertEqual(7, direct.connection_generation)

    def test_observes_actual_connected_idle_status_shape_as_sealed_evidence(self):
        observation, reason = observe_production_fabric_status(_production_status())

        self.assertEqual("PRODUCTION_FABRIC_STATUS_OBSERVED", reason)
        self.assertIsNotNone(observation)
        self.assertEqual("fabric_chatclef", observation.backend_id)
        self.assertEqual("connected", observation.lifecycle_state)
        self.assertEqual("fabric-chatclef-session-1", observation.session_id)
        self.assertEqual(7, observation.connection_generation)
        self.assertIsNone(observation.active_request_id)
        with self.assertRaises(ValueError):
            replace(observation, connection_generation=8)

    def test_rejects_the_old_harness_backend_alias(self):
        status = _production_status()
        status["backend_id"] = "fabric_chatclef_1.20.1"

        observation, reason = observe_production_fabric_status(status)

        self.assertIsNone(observation)
        self.assertEqual("PRODUCTION_STATUS_BACKEND_MISMATCH", reason)

    def test_rejects_busy_or_inconsistently_owned_session_status(self):
        cases = (
            (
                lambda status: status["details"]["commands"].update(
                    active_request_id="request-in-flight"
                ),
                "PRODUCTION_STATUS_COMMAND_NOT_IDLE",
            ),
            (
                lambda status: status["details"]["commands"].update(
                    active_session_id="another-session"
                ),
                "PRODUCTION_STATUS_SESSION_OWNERSHIP_MISMATCH",
            ),
            (
                lambda status: status["details"]["commands"].update(
                    active_generation=0
                ),
                "PRODUCTION_STATUS_CONNECTION_GENERATION_INVALID",
            ),
        )
        for mutate, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                status = _production_status()
                mutate(status)

                observation, reason = observe_production_fabric_status(status)

                self.assertIsNone(observation)
                self.assertEqual(expected_reason, reason)

    def test_does_not_treat_handshake_metadata_as_runtime_artifact_evidence(self):
        status = _production_status()
        status["details"]["sessions"]["active_session"]["metadata"].update(
            {
                "runtimeCodeSourcePath": "C:/forged/chatclef.jar",
                "runtimeJarSha256": "f" * 64,
            }
        )
        status["details"]["runtime_artifact"] = {
            "loaded_code_source": "C:/forged/chatclef.jar"
        }

        observation, reason = observe_production_fabric_status(status)

        self.assertEqual("PRODUCTION_FABRIC_STATUS_OBSERVED", reason)
        self.assertIsNotNone(observation)
        self.assertFalse(hasattr(observation, "runtime_code_source"))
        self.assertFalse(hasattr(observation, "runtime_sha256"))


def _production_status() -> dict[str, object]:
    session_id = "fabric-chatclef-session-1"
    return {
        "backend_id": "fabric_chatclef",
        "enabled": True,
        "connected": True,
        "lifecycle_state": "connected",
        "detail": "Fabric ChatClef bridge client is connected.",
        "details": {
            "endpoint": "ws://127.0.0.1:8765",
            "host": "127.0.0.1",
            "port": 8765,
            "sessions": {
                "active_session_id": session_id,
                "session_count": 1,
                "active_session": {
                    "session_id": session_id,
                    "connected_at_ms": 1_000,
                    "last_seen_at_ms": 2_000,
                    "protocol_version": 1,
                    "capabilities": {"command": True},
                    "metadata": {
                        "backend": "fabric_chatclef",
                        "loader": "fabric",
                        "phase": "phase_4_tick_dispatch",
                    },
                },
            },
            "commands": {
                "active_session_id": session_id,
                "active_generation": 7,
                "active_request_id": None,
                "active_command_message_id": None,
                "active_command": None,
                "active_command_source": None,
                "active_started_at_ms": None,
                "active_age_ms": None,
                "last_result": None,
            },
        },
        "last_error_code": None,
        "last_error_message": None,
    }


class _ConnectedIdleOwnership:
    active_generation = 7

    @staticmethod
    def is_connected() -> bool:
        return True

    @staticmethod
    def local_admission_snapshot() -> dict[str, object]:
        return _production_status()["details"]["commands"]


if __name__ == "__main__":
    unittest.main()
