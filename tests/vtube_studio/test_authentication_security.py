#20260904_kpopmodder: Verify token persistence without credential disclosure in authentication logs.
import json
import tempfile
import threading
from pathlib import Path

from plugins.VtubeStudio.vtube_studio_core.authentication import (
    vtube_studio_auth_manager as auth_manager_module,
)
from plugins.VtubeStudio.vtube_studio_core.authentication.vtube_studio_auth_manager import (
    VTubeStudioAuthManager,
)
from plugins.VtubeStudio.vtube_studio_core.authentication.vtube_studio_token_store import (
    VTubeStudioTokenStore,
)


def test_authentication_logs_redact_token_and_full_response(monkeypatch):
    secret_token = "top-secret-vtube-token"
    private_payload = "private-response-field"
    log_messages = []
    sent_messages = []
    authenticated_attempts = []
    token_path = Path(tempfile.mkdtemp(prefix="vtube-auth-")) / "token.txt"
    token_store = VTubeStudioTokenStore(str(token_path))

    monkeypatch.setattr(
        auth_manager_module,
        "log_print",
        lambda message, level="info": log_messages.append((level, message)),
    )
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        token_store=token_store,
        send_callback=sent_messages.append,
        authenticated_callback=lambda: None,
        attempt_authenticated_callback=authenticated_attempts.append,
    )
    assert manager.on_open(7) is True

    token_response = json.dumps({
        "messageType": "AuthenticationTokenResponse",
        "data": {
            "authenticationToken": secret_token,
            "privatePayload": private_payload,
        },
    })
    authentication_response = json.dumps({
        "messageType": "AuthenticationResponse",
        "data": {"authenticated": True},
    })

    assert manager.on_message(token_response, attempt_id=7) is True
    assert manager.on_message(authentication_response, attempt_id=7) is True

    combined_logs = "\n".join(message for _, message in log_messages)
    assert token_path.read_text(encoding="utf-8") == secret_token
    assert manager.token == secret_token
    assert sent_messages[-1]["messageType"] == "AuthenticationRequest"
    assert authenticated_attempts == [7]
    assert "authentication token received" in combined_logs
    assert "authentication succeeded" in combined_logs
    assert secret_token not in combined_logs
    assert private_payload not in combined_logs
    assert token_response not in combined_logs


def test_existing_empty_token_requests_a_new_token_without_deleting_file():
    token_path = Path(tempfile.mkdtemp(prefix="vtube-empty-token-")) / "token.txt"
    token_path.write_text("", encoding="utf-8")
    sent = []
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        send_callback=lambda message: sent.append((message, None)),
        authenticated_callback=lambda: None,
        attempt_send_callback=lambda message, attempt: (
            sent.append((message, attempt)) or True
        ),
    )

    assert manager.on_open(attempt_id=3) is True
    assert token_path.exists()
    assert token_path.read_text(encoding="utf-8") == ""
    assert len(sent) == 1
    assert sent[0][1] == 3
    assert sent[0][0]["messageType"] == "AuthenticationTokenRequest"


def test_stale_token_response_cannot_change_token_state_or_file():
    token_path = Path(tempfile.mkdtemp(prefix="vtube-stale-token-")) / "token.txt"
    token_path.write_text("existing-token", encoding="utf-8")
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
    )
    assert manager.begin_attempt(4) is True
    assert manager.reset_authentication(4) is True

    accepted = manager.on_message(json.dumps({
        "messageType": "AuthenticationTokenResponse",
        "data": {"authenticationToken": "late-token"},
    }), attempt_id=4)

    assert accepted is False
    assert manager.token == ""
    assert token_path.read_text(encoding="utf-8") == "existing-token"


def test_authentication_rejection_does_not_overwrite_or_log_existing_token(
    monkeypatch,
):
    token_path = Path(tempfile.mkdtemp(prefix="vtube-rejected-token-")) / "token.txt"
    secret_token = "existing-token"
    secret_reason = "denied-for-existing-token"
    log_messages = []
    token_path.write_text(secret_token, encoding="utf-8")
    monkeypatch.setattr(
        auth_manager_module,
        "log_print",
        lambda message, level="info": log_messages.append((level, message)),
    )
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
    )
    assert manager.on_open(attempt_id=5) is True

    assert manager.on_message(json.dumps({
        "messageType": "AuthenticationResponse",
        "data": {"authenticated": False, "reason": secret_reason},
    }), attempt_id=5) is True

    assert manager.is_authenticated is False
    assert token_path.read_text(encoding="utf-8") == secret_token
    combined_logs = "\n".join(message for _, message in log_messages)
    assert "reason=api_rejected" in combined_logs
    assert secret_token not in combined_logs
    assert secret_reason not in combined_logs


def test_repeated_malformed_authentication_logs_are_sampled(monkeypatch):
    log_messages = []
    token_path = Path(tempfile.mkdtemp(prefix="vtube-auth-log-limit-")) / "token.txt"
    monkeypatch.setattr(
        auth_manager_module,
        "log_print",
        lambda message, level="info": log_messages.append((level, message)),
    )
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
    )
    assert manager.begin_attempt(6) is True

    for _ in range(16):
        assert manager.on_message("not-json", attempt_id=6) is False

    invalid_json_logs = [
        message
        for _, message in log_messages
        if "reason=invalid_json" in message
    ]
    assert len(invalid_json_logs) == 6


def test_authentication_log_sampling_has_a_hard_per_event_cap(monkeypatch):
    log_messages = []
    token_path = Path(tempfile.mkdtemp(prefix="vtube-auth-log-cap-")) / "token.txt"
    monkeypatch.setattr(
        auth_manager_module,
        "log_print",
        lambda message, level="info": log_messages.append((level, message)),
    )
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
    )
    assert manager.begin_attempt(8) is True

    for _ in range(4096):
        manager.on_message("not-json", attempt_id=8)

    assert len(log_messages) == 8


def test_unsolicited_token_response_cannot_overwrite_existing_token():
    token_path = Path(tempfile.mkdtemp(prefix="vtube-unsolicited-token-")) / "token.txt"
    token_path.write_text("existing-token", encoding="utf-8")
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
    )
    assert manager.on_open(attempt_id=9) is True

    accepted = manager.on_message(json.dumps({
        "messageType": "AuthenticationTokenResponse",
        "data": {"authenticationToken": "replacement-token"},
    }), attempt_id=9)

    assert accepted is False
    assert manager.token == "existing-token"
    assert token_path.read_text(encoding="utf-8") == "existing-token"


def test_duplicate_authentication_success_notifies_only_once():
    token_path = Path(tempfile.mkdtemp(prefix="vtube-duplicate-auth-")) / "token.txt"
    token_path.write_text("existing-token", encoding="utf-8")
    authenticated_attempts = []
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
        attempt_authenticated_callback=authenticated_attempts.append,
    )
    assert manager.on_open(attempt_id=10) is True
    response = json.dumps({
        "messageType": "AuthenticationResponse",
        "data": {"authenticated": True},
    })

    assert manager.on_message(response, attempt_id=10) is True
    assert manager.on_message(response, attempt_id=10) is False
    assert authenticated_attempts == [10]


def test_untrusted_message_type_is_not_copied_to_logs(monkeypatch):
    secret_message_type = "top-secret-token-shaped-message-type"
    log_messages = []
    token_path = Path(tempfile.mkdtemp(prefix="vtube-auth-untrusted-log-")) / "token.txt"
    monkeypatch.setattr(
        auth_manager_module,
        "log_print",
        lambda message, level="info": log_messages.append((level, message)),
    )
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
    )
    assert manager.begin_attempt(11) is True

    assert manager.on_message(json.dumps({
        "messageType": secret_message_type,
        "data": {},
    }), attempt_id=11) is False
    assert manager.on_message(json.dumps({
        "messageType": secret_message_type,
    }), attempt_id=11) is False

    combined_logs = "\n".join(message for _, message in log_messages)
    assert secret_message_type not in combined_logs
    assert "message_type_kind=unknown" in combined_logs


def test_staged_token_cannot_publish_after_authentication_stop(monkeypatch):
    token_directory = Path(tempfile.mkdtemp(prefix="vtube-staged-stop-"))
    token_path = token_directory / "token.txt"
    token_path.write_text("existing-token", encoding="utf-8")
    token_store = VTubeStudioTokenStore(str(token_path))
    stage_entered = threading.Event()
    release_stage = threading.Event()
    original_stage = token_store.stage

    def blocking_stage(token):
        stage_entered.set()
        assert release_stage.wait(1.0)
        return original_stage(token)

    monkeypatch.setattr(token_store, "stage", blocking_stage)
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        token_store=token_store,
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
    )
    assert manager.begin_attempt(12) is True
    assert manager.get_token(12) is True
    result = []
    response_thread = threading.Thread(
        target=lambda: result.append(manager.on_message(json.dumps({
            "messageType": "AuthenticationTokenResponse",
            "data": {"authenticationToken": "late-token"},
        }), attempt_id=12)),
        daemon=True,
    )
    response_thread.start()
    assert stage_entered.wait(1.0)

    assert manager.stop() is True
    assert token_path.read_text(encoding="utf-8") == "existing-token"
    release_stage.set()
    response_thread.join(timeout=1.0)

    assert result == [False]
    assert token_path.read_text(encoding="utf-8") == "existing-token"
    assert list(token_directory.glob(".vtube-token-*.tmp")) == []


def test_started_token_publish_finishes_before_authentication_stop(monkeypatch):
    token_directory = Path(tempfile.mkdtemp(prefix="vtube-publish-stop-"))
    token_path = token_directory / "token.txt"
    token_path.write_text("existing-token", encoding="utf-8")
    token_store = VTubeStudioTokenStore(str(token_path))
    publish_entered = threading.Event()
    release_publish = threading.Event()
    stop_finished = threading.Event()
    operation_order = []
    original_publish = token_store.publish_staged

    def blocking_publish(staged_path):
        publish_entered.set()
        assert release_publish.wait(1.0)
        original_publish(staged_path)
        operation_order.append("published")

    monkeypatch.setattr(token_store, "publish_staged", blocking_publish)
    manager = VTubeStudioAuthManager(
        token_path=str(token_path),
        token_store=token_store,
        send_callback=lambda message: True,
        authenticated_callback=lambda: None,
    )
    assert manager.begin_attempt(13) is True
    assert manager.get_token(13) is True
    response_result = []
    response_thread = threading.Thread(
        target=lambda: response_result.append(manager.on_message(json.dumps({
            "messageType": "AuthenticationTokenResponse",
            "data": {"authenticationToken": "published-token"},
        }), attempt_id=13)),
        daemon=True,
    )
    response_thread.start()
    assert publish_entered.wait(1.0)

    def stop_manager():
        manager.stop()
        operation_order.append("stopped")
        stop_finished.set()

    stop_thread = threading.Thread(target=stop_manager, daemon=True)
    stop_thread.start()
    assert stop_finished.wait(0.05) is False

    release_publish.set()
    response_thread.join(timeout=1.0)
    stop_thread.join(timeout=1.0)

    assert response_thread.is_alive() is False
    assert stop_thread.is_alive() is False
    assert operation_order == ["published", "stopped"]
    assert token_path.read_text(encoding="utf-8") == "published-token"
    assert manager.token == ""
    assert response_result in ([False], [True])
    assert list(token_directory.glob(".vtube-token-*.tmp")) == []
