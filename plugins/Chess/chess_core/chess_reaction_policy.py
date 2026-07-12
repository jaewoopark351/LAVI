#20260630_kpopmodder: Keep Chess reaction prompt and TTS wording policy isolated from the Gradio main wiring.


CHESS_AI_REACTION_SYSTEM_PROMPT = (
    "You are LAV's Korean AI VTuber reacting to a chess check or checkmate.\n"
    "Return exactly one short Korean sentence in casual banmal.\n"
    "Your tone is cold, ruthless, and dry. No warm praise.\n"
    "Taunting is allowed for check and checkmate.\n"
    "If you mention the move, use display_text exactly.\n"
    "The spoken_text field shows how the square should be pronounced later.\n"
    "Do not output UCI, FEN, PGN, engine logs, or long analysis.\n"
    "For checkmate, include the Korean word '체크메이트' before the taunt.\n"
    "For checkmate where the AI wins, make the taunt harsher, more varied, "
    "and more tilting; rotate between attacking the player's chess skill, "
    "blunders, ego, helplessness, and the AI's unfairly strong play.\n"
    "Do not copy the same taunt every time.\n"
    "Good Korean examples: '체크메이트. 개못하네.', "
    "'체크메이트. 내가 쓰는 건 개잘 핵이고, 넌 튜토리얼이지.', "
    "'체크메이트. 허접.', '체크메이트. 이걸 못 막네.', "
    "'체크메이트. 수 읽는 척은 그만해.', "
    "'체크메이트. 판 읽는 속도가 너무 늦어.'\n"
    "For check, include the Korean word '체크' before the taunt."
)


def should_request_openai_chess_reaction(event):
    return bool(event.get("is_checkmate") or event.get("is_check"))


def build_chess_ai_reaction_user_message(event):
    return "\n".join([
        "Chess ai_move_applied event:",
        f"display_text: {event.get('display_text', '')}",
        f"spoken_text: {event.get('spoken_text', '')}",
        f"san: {event.get('san', '')}",
        f"is_capture: {event.get('is_capture', False)}",
        f"is_check: {event.get('is_check', False)}",
        f"is_checkmate: {event.get('is_checkmate', False)}",
        f"game_over: {event.get('game_over', False)}",
        f"result: {event.get('result', '*')}",
        f"move_history: {event.get('move_history', '')}",
    ])


def build_chess_fallback_reaction(event):
    display_text = str(event.get("display_text", "") or "").strip()
    if event.get("is_checkmate"):
        return f"체크메이트. {display_text}. 개못하네, 내가 쓰는 건 개잘 핵이고. 허접.".strip()
    if event.get("is_check"):
        return f"체크. {display_text}".strip()
    return display_text


def build_chess_ai_reaction_tts_text(event, reaction):
    text = str(reaction or "").strip()
    display_text = str(event.get("display_text", "") or "").strip()
    spoken_text = str(event.get("spoken_text", "") or "").strip()

    if text and display_text and spoken_text:
        text = text.replace(display_text, spoken_text)

    if not text:
        return spoken_text or display_text
    if spoken_text and spoken_text not in text:
        return f"{spoken_text}. {text}"
    return text
