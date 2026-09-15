#20260915_kpopmodder: Verify whole-snapshot validation and session replacement without game I/O.
import base64
import gzip
import hashlib
import json

import pytest

from plugins.Minecraft.fabric.chatclef.session.catalogue import FabricCommandCatalogueDecoder
from plugins.Minecraft.fabric.chatclef.session import FabricChatClefSessionRegistry


def _payload(entries=None):
    return {"schema_version": 1, "minecraft_version": "1.20.1", "butler_user": None,
        "registered_commands": ["give", "scan"], "entries": entries or [{"kind": "item",
        "id": "example:gear", "translation_key": "item.example.gear", "korean_name": "톱니바퀴",
        "tokens": {"give": "item.example.gear"}, "capabilities": ["give"],
        "catalogue_aliases": []}]}


def _wrapper(payload=None):
    payload = payload or _payload()
    raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    return {"available": True, "encoding": "gzip+base64", "payload": base64.b64encode(gzip.compress(raw)).decode(),
            "sha256": hashlib.sha256(raw).hexdigest(), "uncompressed_bytes": len(raw),
            "entry_count": len(payload["entries"])}


def test_snapshot_preserves_mod_native_token_and_is_deeply_immutable():
    result = FabricCommandCatalogueDecoder().decode(_wrapper())
    assert result["entries"][0]["tokens"]["give"] == "item.example.gear"
    assert result["entries"][0]["korean_name"] == "톱니바퀴"
    with pytest.raises(TypeError):
        result["entries"][0]["tokens"]["give"] = "forged"


@pytest.mark.parametrize("field,value", [("available", False), ("available", 1), ("sha256", "0" * 64), ("uncompressed_bytes", 1),
    ("uncompressed_bytes", True), ("entry_count", 2), ("payload", "!")])
def test_corrupted_wrapper_rejects_the_entire_snapshot(field, value):
    wrapper = _wrapper()
    wrapper[field] = value
    with pytest.raises(ValueError):
        FabricCommandCatalogueDecoder().decode(wrapper)


def test_duplicate_registered_identity_is_not_silently_overwritten():
    entry = _payload()["entries"][0]
    with pytest.raises(ValueError, match="duplicate_identity"):
        FabricCommandCatalogueDecoder().decode(_wrapper(_payload([entry, entry])))


def test_decompression_limit_and_trailing_stream_are_rejected():
    wrapper = _wrapper()
    wrapper["payload"] = base64.b64encode(base64.b64decode(wrapper["payload"]) + gzip.compress(b"x")).decode()
    with pytest.raises(ValueError, match="size_mismatch"):
        FabricCommandCatalogueDecoder().decode(wrapper)


def test_refresh_disconnect_and_reconnect_never_reuse_stale_names():
    sessions = FabricChatClefSessionRegistry()
    sessions.upsert(session_id="s1", timestamp_ms=1, metadata={"korean_command_catalogue_v1": _wrapper()})
    assert sessions.command_catalogue("s1")["session_id"] == "s1"
    status = sessions.snapshot()
    assert "payload" not in repr(status)
    assert sessions.update_command_catalogue("foreign", _wrapper())["available"] is False
    assert sessions.command_catalogue("s1") is not None
    sessions.update_command_catalogue("s1", {"available": False, "reason": "reload"})
    assert sessions.command_catalogue("s1") is None
    sessions.update_command_catalogue("s1", _wrapper())
    sessions.remove("s1")
    assert sessions.command_catalogue("s1") is None
    sessions.upsert(session_id="s1", timestamp_ms=2)
    assert sessions.command_catalogue("s1") is None
