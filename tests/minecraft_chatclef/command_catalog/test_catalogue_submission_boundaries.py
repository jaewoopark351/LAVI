#20260915_kpopmodder: Exercise refresh custody, locked admission identities and physical diagnostic output.
from io import StringIO
import logging
import threading
from types import SimpleNamespace

import pytest

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.fabric.chatclef.session import FabricChatClefSessionRegistry
from plugins.Minecraft.fabric.chatclef.transport.command_submission.admission.korean_submission_context_validator import KoreanSubmissionContextValidator
from plugins.Minecraft.fabric.chatclef.transport.server.client_session.catalogue.fabric_chatclef_catalogue_event_handler import FabricChatClefCatalogueEventHandler
from .test_runtime_command_catalogue import _wrapper


def test_refresh_is_correlated_and_logged_through_real_formatter(tmp_path):
    stream = StringIO()
    logger = logging.Logger("catalogue-boundary-test")
    handler = logging.StreamHandler(stream)
    handler.setFormatter(logging.Formatter("%(levelname)s %(message)s"))
    logger.addHandler(handler)
    output = tmp_path / "catalogue-refresh.log"
    file_handler = logging.FileHandler(output, encoding="utf-8")
    file_handler.setFormatter(handler.formatter)
    logger.addHandler(file_handler)
    sessions = FabricChatClefSessionRegistry()
    sessions.upsert(session_id="s", timestamp_ms=1)
    socket = object()
    owner = SimpleNamespace(active_session_id="s", active_generation=3, is_active_websocket=lambda ws: ws is socket)
    observer = FabricChatClefCatalogueEventHandler(connection_ownership=owner, command_lock=threading.RLock(),
                                                  session_registry=sessions, diagnostics=logger)
    def envelope(session):
        return BridgeEnvelopeDTO(protocol_version=1, message_type=BridgeMessageType.EVENT,
            message_id="refresh", session_id=session, timestamp_ms=1,
            payload={"event_type": "korean_command_catalogue_v1", "korean_command_catalogue_v1": _wrapper()})
    assert observer.handle(object(), envelope("s"))
    assert sessions.command_catalogue("s") is None
    assert observer.handle(socket, envelope("foreign"))
    assert sessions.command_catalogue("s") is None
    assert observer.handle(socket, envelope("s"))
    assert sessions.command_catalogue("s") is not None
    lines = stream.getvalue().splitlines()
    assert len(lines) == 3
    assert "generation=3 active=True available=True entries=1 reason=validated" in lines[-1]
    assert "payload" not in stream.getvalue()
    file_handler.flush()
    assert output.read_text(encoding="utf-8") == stream.getvalue()
    file_handler.close()
    # A throwing sink cannot invalidate a validated replacement.
    observer._diagnostics = SimpleNamespace(info=lambda _: (_ for _ in ()).throw(RuntimeError("sink")))
    assert observer.handle(socket, envelope("s"))
    assert sessions.command_catalogue("s") is not None


@pytest.mark.parametrize("context_key", ("korean_confirmation", "korean_single_trust"))
def test_final_submission_rechecks_generation_after_confirmation_or_single_trust(context_key):
    owner = SimpleNamespace(active_session_id="s", active_generation=3)
    guard = KoreanSubmissionContextValidator(owner)
    request = CommandRequestDTO(request_id="r", command="auto_deposit_trust", source="lavi_chat_ui",
        metadata={context_key: {"expected_session_id": "s", "expected_generation": 3}})
    assert guard.rejection_reason(request) is None
    owner.active_generation = 4
    assert guard.rejection_reason(request) == context_key + "_session_changed"


def test_final_submission_rejects_reloaded_names_but_preserves_unrelated_english_request():
    sessions = FabricChatClefSessionRegistry()
    sessions.upsert(session_id="s", timestamp_ms=1, metadata={"korean_command_catalogue_v1": _wrapper()})
    owner = SimpleNamespace(active_session_id="s", active_generation=3)
    guard = KoreanSubmissionContextValidator(owner, lambda: sessions.command_catalogue("s"))
    snapshot = sessions.command_catalogue("s")
    request = CommandRequestDTO(request_id="r", command="give Alex item.example.gear 1", source="lavi_chat_ui",
        metadata={"natural_language": {"translation": {"data": {"runtime_catalogue": {
            "session_id": "s", "catalogue_sha256": snapshot["catalogue_sha256"]}}}}})
    assert guard.rejection_reason(request) is None
    sessions.update_command_catalogue("s", {"available": False, "reason": "reload"})
    assert guard.rejection_reason(request) == "korean_command_catalogue_changed"
    english = CommandRequestDTO(request_id="english", command="give Alex iron_ingot 1", source="lavi_gui")
    assert guard.rejection_reason(english) is None


@pytest.mark.parametrize("kind,slots,target", (("attack", {}, "zombie"), ("scan", {}, "DIAMOND_ORE"),
    ("follow", {"butler_user": True}, ""), ("give_item", {}, "item.example.gear")))
def test_runtime_dependent_translation_cannot_drop_its_catalogue_fingerprint(kind, slots, target):
    guard = KoreanSubmissionContextValidator(SimpleNamespace(active_session_id="s", active_generation=3))
    request = CommandRequestDTO(request_id="r", command="scan DIAMOND_ORE", source="lavi_chat_ui",
        metadata={"natural_language": {"translation": {"intent": {"intent_type": kind,
            "item_phrase": "대상", "slots": slots}, "resolved_target": target, "data": {}}}})
    assert guard.rejection_reason(request) == "korean_command_catalogue_missing"


def test_butler_binding_is_checked_again_at_final_submission():
    owner = SimpleNamespace(active_session_id="s", active_generation=3)
    current = {"session_id": "s", "catalogue_sha256": "a" * 64, "butler_user": "Steve"}
    guard = KoreanSubmissionContextValidator(owner, lambda: current)
    request = CommandRequestDTO(request_id="r", command="follow", source="lavi_chat_ui",
        metadata={"natural_language": {"translation": {"intent": {"intent_type": "follow",
            "slots": {"butler_user": True}}, "data": {"butler_user_bound": "Alex",
                "runtime_catalogue": {"session_id": "s", "catalogue_sha256": "a" * 64}}}}})
    assert guard.rejection_reason(request) == "korean_butler_user_changed"
