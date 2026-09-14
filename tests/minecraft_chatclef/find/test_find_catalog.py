#20260914_kpopmodder: Verify complete atomic catalog publication, bounds, staleness and formatted output.
from dataclasses import asdict, replace
from types import SimpleNamespace
import unittest

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.fabric.chatclef.diagnostics import FabricChatClefDiagnostics
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import FabricChatClefConnectionOwnership
from plugins.Minecraft.fabric.chatclef.transport.find_catalog import FindCatalogReceiver
from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import catalog_digest

from .fixtures import records


class FindCatalogTests(unittest.TestCase):
    def setUp(self):
        self.websocket = SimpleNamespace(closed=False)
        self.owner = FabricChatClefConnectionOwnership()
        self.owner.try_activate(websocket=self.websocket, session_id="catalog-session")
        self.clock = 0.0
        self.lines = []
        self.receiver = FindCatalogReceiver(connection_ownership=self.owner,
            diagnostics=FabricChatClefDiagnostics(self.lines.append), clock=lambda: self.clock)

    def page(self, *, index=0, count=1, selected=None, **changes):
        current = records()
        serialized = []
        for record in selected if selected is not None else current:
            value = asdict(record)
            if record.target_kind != "entity":
                value.pop("eligibility")
            serialized.append(value)
        payload = dict(event="find_catalog_page", catalog_version=1, resource_generation=3,
            connection_generation=self.owner.active_generation, catalog_digest=catalog_digest(current),
            page_index=index, page_count=count, record_count=len(current), complete=True, records=serialized)
        payload.update(changes)
        return BridgeEnvelopeDTO(protocol_version=1, message_type=BridgeMessageType.EVENT,
            message_id="catalog-message", timestamp_ms=1, session_id="catalog-session", payload=payload)

    def receive(self, envelope):
        self.assertTrue(self.receiver.receive(self.websocket, envelope))

    def test_pages_publish_only_when_complete_and_validate_source_counts_digest_and_exact_ids(self):
        self.receive(self.page(count=2, selected=records()[:3]))
        self.assertIsNone(self.receiver.snapshot())
        self.receive(self.page(index=1, count=2, selected=records()[3:]))
        snapshot = self.receiver.snapshot()
        self.assertEqual(records(), snapshot.records)
        self.assertEqual("catalog-session", snapshot.session_id)
        self.assertTrue(any("event=published" in line and "records=7" in line for line in self.lines))

    def test_incomplete_source_corrupt_digest_duplicate_unknown_keys_and_controls_never_publish(self):
        for changes in ({"complete": False}, {"record_count": len(records()) + 1},
                {"catalog_digest": "0" * 64}, {"extra": True}, {"connection_generation": True}):
            with self.subTest(changes=changes):
                self.receive(self.page(**changes))
                self.assertIsNone(self.receiver.snapshot())
        bad = self.page()
        bad.payload["records"][0]["korean_name"] = "주민\n"
        self.receive(bad)
        self.assertIsNone(self.receiver.snapshot())
        bad = self.page()
        bad.payload["records"][1] = dict(bad.payload["records"][0])
        self.receive(bad)
        self.assertIsNone(self.receiver.snapshot())

    def test_sequence_deadline_and_finite_caps_do_not_leave_a_complete_snapshot(self):
        self.receive(self.page(count=2, selected=records()[:3]))
        self.clock = 10.01
        self.receive(self.page(index=1, count=2, selected=records()[3:]))
        self.assertIsNone(self.receiver.snapshot())
        for changes in ({"page_count": 257}, {"record_count": 50001}, {"page_index": 2},
                {"records": [{}] * 257}):
            self.receive(self.page(**changes))
            self.assertIsNone(self.receiver.snapshot())

    def test_resource_invalidation_and_disconnect_retire_published_snapshot(self):
        self.receive(self.page())
        self.assertIsNotNone(self.receiver.snapshot())
        invalidation = BridgeEnvelopeDTO(protocol_version=1, message_type=BridgeMessageType.EVENT,
            message_id="invalidate", timestamp_ms=1, session_id="catalog-session", payload=dict(
                event="find_catalog_invalidated", catalog_version=1, resource_generation=4,
                connection_generation=self.owner.active_generation, reason="CATALOG_REPLACED"))
        self.receive(invalidation)
        self.assertIsNone(self.receiver.snapshot())
        self.receive(self.page())
        self.owner.clear()
        self.assertIsNone(self.receiver.snapshot())

    def test_foreign_socket_session_and_generation_cannot_publish(self):
        self.assertTrue(self.receiver.receive(SimpleNamespace(closed=False), self.page()))
        self.assertIsNone(self.receiver.snapshot())
        page = self.page()
        object.__setattr__(page, "session_id", "foreign")
        self.receive(page)
        self.assertIsNone(self.receiver.snapshot())
        self.receive(self.page(connection_generation=42))
        self.assertIsNone(self.receiver.snapshot())

    def test_logging_failure_does_not_change_complete_catalog_admission(self):
        def broken(_):
            raise OSError("output unavailable")
        receiver = FindCatalogReceiver(connection_ownership=self.owner, diagnostics=FabricChatClefDiagnostics(broken))
        self.assertTrue(receiver.receive(self.websocket, self.page()))
        self.assertIsNotNone(receiver.snapshot())

    def test_malformed_enum_json_and_large_java_long_generation_are_handled_without_transport_errors(self):
        for key in ("target_kind", "eligibility"):
            for malformed in ([], {}):
                page = self.page()
                page.payload["records"][0][key] = malformed
                receiver = FindCatalogReceiver(connection_ownership=self.owner, diagnostics=FabricChatClefDiagnostics(self.lines.append))
                self.assertTrue(receiver.receive(self.websocket, page))
                self.assertIsNone(receiver.snapshot())
        page = self.page()
        page.payload["event"] = []
        self.assertFalse(self.receiver.receive(self.websocket, page))
        invalidation = self.page()
        invalidation.payload.clear()
        invalidation.payload.update(event="find_catalog_invalidated", catalog_version=1, resource_generation=4,
            connection_generation=self.owner.active_generation, reason=[])
        self.receive(invalidation)
        self.assertIsNone(self.receiver.snapshot())
        self.receive(self.page(resource_generation=2**63 - 1))
        self.assertEqual(2**63 - 1, self.receiver.snapshot().resource_generation)

    def test_repeated_page_zero_cannot_restart_deadline_or_republish_an_incomplete_generation(self):
        self.receive(self.page(count=2, selected=records()[:3]))
        self.clock = 9.0
        self.receive(self.page(count=2, selected=records()[:3]))
        self.clock = 10.01
        self.receive(self.page(index=1, count=2, selected=records()[3:]))
        self.assertIsNone(self.receiver.snapshot())
        self.receive(self.page())
        self.assertIsNone(self.receiver.snapshot())
        self.receive(self.page(resource_generation=4))
        self.assertIsNotNone(self.receiver.snapshot())

    def test_java_equivalent_unicode_vocabulary_and_codepoint_limits_publish_or_reject_exactly(self):
        allowed = ("\ue000", "\u0378", "\U0001f600" * 256, "é")
        rejected = ("\x00", "\x7f", "\x85", "\u200d", "\ud800", "\u2028", "\u2029", "\U0001f600" * 257)
        for text in (*allowed, *rejected):
            current = (replace(records()[0], korean_name=text), *records()[1:])
            receiver = FindCatalogReceiver(connection_ownership=self.owner, diagnostics=FabricChatClefDiagnostics(self.lines.append))
            page = self.page(selected=current, catalog_digest=catalog_digest(current) if text in allowed else catalog_digest(records()))
            self.assertTrue(receiver.receive(self.websocket, page))
            with self.subTest(text=repr(text)):
                self.assertEqual(text in allowed, receiver.snapshot() is not None)
        page = self.page()
        page.payload["records"][0]["translation_key"] = ""
        self.receive(page)
        self.assertIsNone(self.receiver.snapshot())


def test_formatted_catalog_logs_reach_a_file_sink_without_full_catalog_or_raw_names():
    import tempfile
    from pathlib import Path
    target = Path(tempfile.mkdtemp(prefix="find-log-")) / "find_catalog_output.log"
    with target.open("w", encoding="utf-8") as sink:
        test = FindCatalogTests()
        test.setUp()
        test.receiver._observer._diagnostics = FabricChatClefDiagnostics(lambda line: sink.write(line + "\n"))
        test.receive(test.page())
    text = target.read_text(encoding="utf-8")
    assert "[MinecraftFabricChatClef] FIND_CATALOG event=published reason=complete_validated" in text
    assert "주민" not in text
    assert "records=7" in text
