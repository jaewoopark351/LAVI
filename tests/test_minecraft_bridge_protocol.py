#20260801_kpopmodder: Verify Minecraft bridge DTO/schema contract without transport dependencies.
import json
import unittest
from pathlib import Path

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SCHEMA_PATH = (
    PROJECT_ROOT
    / "plugins"
    / "Minecraft"
    / "common"
    / "schema"
    / "lavi_minecraft_bridge_v1.schema.json"
)


class MinecraftBridgeProtocolTests(unittest.TestCase):
    def load_schema(self):
        return json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))

    def test_envelope_round_trip_keeps_message_and_command_ids_separate(self):
        envelope = BridgeEnvelopeDTO(
            protocol_version=1,
            message_type=BridgeMessageType.COMMAND_REQUEST,
            message_id="msg-1",
            correlation_id=None,
            session_id=None,
            timestamp_ms=123,
            payload={"request_id": "cmd-1"},
        )

        payload = envelope.to_dict()
        restored = BridgeEnvelopeDTO.from_mapping(payload)

        self.assertEqual("msg-1", restored.message_id)
        self.assertNotIn("request_id", payload)
        self.assertEqual({"request_id": "cmd-1"}, restored.payload)

    def test_command_request_uses_nullable_absolute_deadline(self):
        request = CommandRequestDTO(
            request_id="cmd-1",
            command="@get gold_ingot 1",
            source="test",
            deadline_ms=None,
            metadata={"priority": "manual"},
        )

        payload = request.to_dict()
        restored = CommandRequestDTO.from_mapping(payload)

        self.assertIsNone(restored.deadline_ms)
        self.assertEqual(payload, restored.to_dict())

    def test_command_result_status_controls_ok_contract(self):
        accepted = CommandResultDTO(
            request_id="cmd-1",
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            error_code=None,
            message="accepted",
            data={},
        )
        rejected = CommandResultDTO(
            request_id="cmd-2",
            ok=False,
            status=CommandResultStatus.REJECTED,
            error_code=BridgeErrorCode.NOT_IMPLEMENTED,
            message="not implemented",
            data={},
        )

        self.assertTrue(accepted.ok)
        self.assertIsNone(accepted.error_code)
        self.assertFalse(rejected.ok)
        self.assertEqual("not_implemented", rejected.to_dict()["error_code"])
        with self.assertRaises(ValueError):
            CommandResultDTO(
                request_id="bad",
                ok=True,
                status=CommandResultStatus.REJECTED,
            )

    def test_status_snapshot_round_trip_uses_nullable_last_error(self):
        snapshot = StatusSnapshotDTO(
            backend_id="fabric_chatclef",
            enabled=False,
            connected=False,
            lifecycle_state=BridgeLifecycleState.NOT_IMPLEMENTED,
            detail="Fabric ChatClef bridge is not implemented in Phase 1.",
            details={},
            last_error_code=None,
            last_error_message=None,
        )

        payload = snapshot.to_dict()
        restored = StatusSnapshotDTO.from_mapping(payload)

        self.assertEqual("not_implemented", payload["lifecycle_state"])
        self.assertIsNone(payload["last_error_code"])
        self.assertEqual(payload, restored.to_dict())

    def test_schema_required_fields_match_dto_fields(self):
        schema = self.load_schema()
        defs = schema["$defs"]

        expected = {
            "bridge_envelope": set(BridgeEnvelopeDTO().to_dict()),
            "command_request": set(CommandRequestDTO().to_dict()),
            "command_result": set(CommandResultDTO().to_dict()),
            "status_snapshot": set(StatusSnapshotDTO().to_dict()),
        }

        for name, fields in expected.items():
            self.assertEqual(fields, set(defs[name]["required"]))
            self.assertEqual(fields, set(defs[name]["properties"]))
            self.assertFalse(defs[name]["additionalProperties"])

    def test_schema_enum_values_match_protocol_enums(self):
        schema = self.load_schema()
        defs = schema["$defs"]

        self.assertEqual(
            {item.value for item in BridgeMessageType},
            set(defs["bridge_envelope"]["properties"]["message_type"]["enum"]),
        )
        self.assertEqual(
            {item.value for item in CommandResultStatus},
            set(defs["command_result"]["properties"]["status"]["enum"]),
        )
        self.assertEqual(
            {item.value for item in BridgeLifecycleState},
            set(defs["status_snapshot"]["properties"]["lifecycle_state"]["enum"]),
        )
        schema_errors = {
            value
            for value in defs["command_result"]["properties"]["error_code"]["enum"]
            if value is not None
        }
        self.assertEqual({item.value for item in BridgeErrorCode}, schema_errors)
        self.assertNotIn("none", schema_errors)


if __name__ == "__main__":
    unittest.main()
