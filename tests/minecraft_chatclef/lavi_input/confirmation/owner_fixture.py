#20260915_kpopmodder: Deterministic fixture for the confirmation lifecycle; no transport or gameplay.
from types import SimpleNamespace
from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.confirmation import (
    KoreanCommandConfirmationOwner,
)


def event(text="Alex 따라가 줘", source="lavi_chat_ui", identity="a" * 32, final=True):
    return LaviInputEvent(
        text=text,
        source=source,
        event_id=identity,
        event_kind="final_transcript"
        if source == "voice_input_final"
        else "chat_submit",
        final=final,
        provider_id="VoiceInput" if source == "voice_input_final" else source,
        fallback_payload=text,
    )


def fixture(log_callback=None):
    state = SimpleNamespace(
        time=10.0, generation=1, session="session-one", ready=True, trusted=True
    )
    messages = []
    extension = SimpleNamespace(
        korean_command_registry=SimpleNamespace(
            spec=lambda _: SimpleNamespace(safety_tier="R3")
        )
    )

    def inspect(_):
        return SimpleNamespace(
            ready=state.ready,
            reason="minecraft_command_busy",
            status={
                "details": {
                    "commands": {
                        "active_session_id": state.session,
                        "active_generation": state.generation,
                    }
                }
            },
        )

    owner = KoreanCommandConfirmationOwner(
        extension=extension,
        submission_precheck=SimpleNamespace(inspect=inspect),
        live_proof_validator=lambda proof, row: state.trusted and proof is row,
        log_callback=log_callback or messages.append,
        now=lambda: state.time,
    )
    original = event()
    translation = {
        "command": "follow Alex",
        "intent": {"original_text": original.text, "target": "Alex"},
        "resolved_target": "Alex",
    }
    return SimpleNamespace(
        owner=owner,
        state=state,
        original=original,
        translation=translation,
        messages=messages,
    )


def begin(f, original=None):
    row = original or f.original
    return f.owner.begin(
        event=row, proof=row, command_text=row.text, translation=f.translation
    )


def confirm(f, text="확인", source="lavi_chat_ui", identity="b" * 32):
    row = event(text, source, identity)
    return f.owner.reply(event=row, proof=row)


def request(receipt):
    row = receipt.pending.event
    return SimpleNamespace(
        command=receipt.pending.command,
        source=row.source,
        metadata={
            "korean_confirmation": receipt.binding_metadata(),
            "input_event": {
                "event_id": row.event_id,
                "source": row.source,
                "provider_id": row.provider_id,
                "event_kind": row.event_kind,
                "final": True,
            },
        },
    )
