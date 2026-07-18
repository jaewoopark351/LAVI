#20260630_kpopmodder: Moved Chess reaction runtime wiring out of main.py.
import threading

from core.logger import log_print
from llm_core.speech_style import apply_game_reaction_speech_style
from plugins.Chess.chess_core.chess_reaction_policy import (
    build_chess_ai_reaction_tts_text,
    build_chess_ai_reaction_system_prompt,
    build_chess_ai_reaction_user_message,
    build_chess_fallback_reaction,
    should_request_openai_chess_reaction,
)


def speak_chess_ai_reaction(tts, event, reaction, speech_style_source=None):#20260630_kpopmodder
    if tts is None:
        return
    receive_input = getattr(tts, "receive_input", None)
    if not callable(receive_input):
        return

    tts_text = build_chess_ai_reaction_tts_text(event, reaction)
    tts_text = apply_game_reaction_speech_style(
        tts_text,
        speech_style_source,
    )
    if not tts_text:
        return

    log_print(f"[ChessReaction] TTS: {tts_text}")
    receive_input(tts_text)


def run_chess_ai_reaction(llm, chess_plugin, tts, event):#20260630_kpopmodder
    event_id = event.get("event_id")
    try:
        reaction = llm.generate_text_only(
            build_chess_ai_reaction_user_message(event),
            build_chess_ai_reaction_system_prompt(llm),
            preferred_provider_name="Hybrid_OpenAI_LLM",
        )
        if not reaction or reaction.startswith("[Hybrid_OpenAI_LLM"):
            raise RuntimeError(reaction or "empty chess reaction")
        if chess_plugin.set_ai_reaction(event_id, reaction):
            log_print(f"[ChessReaction] {reaction}")
            speak_chess_ai_reaction(tts, event, reaction, llm)
    except Exception as e:
        fallback_reaction = build_chess_fallback_reaction(event)
        updated = chess_plugin.set_ai_reaction(event_id, fallback_reaction)
        log_print(f"[ChessReaction] text-only reaction failed: {e}")
        if updated:
            speak_chess_ai_reaction(tts, event, fallback_reaction, llm)


def handle_chess_ai_move_applied(llm, chess_plugin, tts, event):#20260630_kpopmodder
    event_id = event.get("event_id")
    display_text = str(event.get("display_text", "") or "").strip()
    spoken_text = str(event.get("spoken_text", "") or "").strip()
    if display_text:
        chess_plugin.set_ai_reaction(event_id, display_text)
    log_print(
        "[ChessReaction] ai_move_applied: "
        f"display={display_text} spoken={spoken_text}"
    )
    if not should_request_openai_chess_reaction(event):
        speak_chess_ai_reaction(tts, event, display_text, llm)
        return

    thread = threading.Thread(
        target=run_chess_ai_reaction,
        args=(llm, chess_plugin, tts, dict(event)),
        name="ChessAIReaction",
        daemon=True,
    )
    thread.start()
