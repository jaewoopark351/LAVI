#20260703_kpopmodder: Runs StarCraft 1.16 status reactions through OpenAI text-only generation and TTS.
import threading

from core.event_manager import event_manager, EventType
from core.logger import log_print
from .starcraft116_reaction_policy import (
    STARCRAFT116_REACTION_SYSTEM_PROMPT,
    build_starcraft116_fallback_reaction,
    build_starcraft116_reaction_tts_text,
    build_starcraft116_reaction_user_message,
    should_speak_starcraft116_event,
)


#20260705_kpopmodder: Bump this when a game starts/ends so stale reaction threads cannot enqueue old TTS.
_STARCRAFT116_REACTION_GENERATION_LOCK = threading.Lock()
_STARCRAFT116_REACTION_GENERATION = 0


def _bump_starcraft116_reaction_generation(reason):#20260705_kpopmodder
    global _STARCRAFT116_REACTION_GENERATION
    with _STARCRAFT116_REACTION_GENERATION_LOCK:
        _STARCRAFT116_REACTION_GENERATION += 1
        generation = _STARCRAFT116_REACTION_GENERATION
    log_print(
        "[StarCraft116Reaction] reaction generation bumped: "
        f"generation={generation} reason={reason}"
    )
    return generation


def _get_starcraft116_reaction_generation():#20260705_kpopmodder
    with _STARCRAFT116_REACTION_GENERATION_LOCK:
        return _STARCRAFT116_REACTION_GENERATION


def _is_stale_starcraft116_reaction_generation(generation):#20260705_kpopmodder
    return generation != _get_starcraft116_reaction_generation()


def _handle_starcraft116_reaction_boundary(event):#20260705_kpopmodder
    if event.get("source") != "game_event":
        return

    event_type = str(event.get("event_type", "") or "").lower()
    if event_type == "game_started":
        _bump_starcraft116_reaction_generation("game_started")
        return

    if event_type == "game_ended":
        _bump_starcraft116_reaction_generation("game_ended")
        log_print("[StarCraft116Reaction] game ended; interrupting pending TTS")
        event_manager.trigger(EventType.INTERRUPT)


def speak_starcraft116_reaction(tts, event, reaction, reaction_generation=None):
    if tts is None:
        return
    if (
        reaction_generation is not None
        and _is_stale_starcraft116_reaction_generation(reaction_generation)
    ):
        #20260705_kpopmodder: Drop LLM output that finished after the StarCraft game boundary changed.
        log_print(
            "[StarCraft116Reaction] stale reaction TTS skipped: "
            f"generation={reaction_generation}"
        )
        return
    receive_input = getattr(tts, "receive_input", None)
    if not callable(receive_input):
        return

    tts_text = build_starcraft116_reaction_tts_text(event, reaction)
    if not tts_text:
        return

    log_print(f"[StarCraft116Reaction] TTS: {tts_text}")
    receive_input(tts_text)


def run_starcraft116_status_reaction(llm, tts, event, reaction_generation=None):
    if not should_speak_starcraft116_event(event):
        return

    try:
        raw_reaction = llm.generate_text_only(
            build_starcraft116_reaction_user_message(event),
            STARCRAFT116_REACTION_SYSTEM_PROMPT,
            preferred_provider_name="Hybrid_OpenAI_LLM",
        )
        if not raw_reaction or str(raw_reaction).startswith("[Hybrid_OpenAI_LLM"):
            raise RuntimeError(raw_reaction or "empty StarCraft 1.16 reaction")
        if (
            reaction_generation is not None
            and _is_stale_starcraft116_reaction_generation(reaction_generation)
        ):
            log_print(
                "[StarCraft116Reaction] stale text reaction skipped: "
                f"generation={reaction_generation}"
            )
            return
        reaction = build_starcraft116_reaction_tts_text(event, raw_reaction)
        log_print(f"[StarCraft116Reaction] {reaction}")
        speak_starcraft116_reaction(
            tts,
            event,
            reaction,
            reaction_generation=reaction_generation,
        )
    except Exception as e:
        if (
            reaction_generation is not None
            and _is_stale_starcraft116_reaction_generation(reaction_generation)
        ):
            log_print(
                "[StarCraft116Reaction] stale fallback skipped: "
                f"generation={reaction_generation}"
            )
            return
        fallback_reaction = build_starcraft116_fallback_reaction(event)
        log_print(f"[StarCraft116Reaction] text-only reaction failed: {e}")
        speak_starcraft116_reaction(
            tts,
            event,
            fallback_reaction,
            reaction_generation=reaction_generation,
        )


def handle_starcraft116_status_event(llm, tts, event):
    if event.get("source") == "game_event":
        event_type = event.get("event_type", "")
        severity = event.get("severity", "")
        log_print(
            "[StarCraft116Reaction] game_event: "
            f"type={event_type} severity={severity}"
        )
    else:
        phase = event.get("phase", "")
        severity = event.get("severity", "")
        log_print(
            "[StarCraft116Reaction] status_event: "
            f"phase={phase} severity={severity}"
        )
    _handle_starcraft116_reaction_boundary(event)
    if not should_speak_starcraft116_event(event):
        log_print("[StarCraft116Reaction] log_only event; TTS skipped")
        return
    reaction_generation = _get_starcraft116_reaction_generation()
    thread = threading.Thread(
        target=run_starcraft116_status_reaction,
        args=(llm, tts, dict(event), reaction_generation),
        name="StarCraft116StatusReaction",
        daemon=True,
    )
    thread.start()


def build_starcraft116_status_event_callback(llm, tts):
    #20260705_kpopmodder: Keep AppComposer wiring thin while preserving the existing callback entry point.
    def _handle_status_event(event):
        return handle_starcraft116_status_event(llm, tts, event)

    return _handle_status_event
