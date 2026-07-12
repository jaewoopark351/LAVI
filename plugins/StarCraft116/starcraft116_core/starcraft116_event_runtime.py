#20260705_kpopmodder: Added this helper to keep StarCraft116 game-event emit policy outside the facade.
import time

from core.logger import log_print
from . import starcraft116_event_gate


def is_noisy_unknown_enemy_destroyed_event(raw_event):
    #20260705_kpopmodder: Preserve the previous BWAPI probe-destroy TTS suppression rule exactly.
    if str(raw_event.get("event_type", "") or "") != "unit_destroyed":
        return False
    details = raw_event.get("details", {})
    if not isinstance(details, dict):
        return False
    unit = details.get("unit", {})
    if not isinstance(unit, dict):
        return False
    if str(unit.get("owner", "") or "").strip().lower() != "enemy":
        return False
    if str(unit.get("type", "") or "").strip().lower() != "unit":
        return False
    try:
        type_id = int(unit.get("type_id"))
    except (TypeError, ValueError):
        return False
    return type_id == 64


def trim_game_event_keys(game_event_key_times, now, cooldown):
    return starcraft116_event_gate.trim_game_event_keys(
        game_event_key_times,
        now,
        cooldown,
    )


def maybe_emit_game_event(
    event,
    callback,
    config_manager,
    game_event_key_times,
    last_game_event_emit_time,
    use_global_cooldown=True,
    log_callback=log_print,
):
    #20260705_kpopmodder: Return updated state so StarCraft116 can remain the public facade.
    now = time.time()
    decision = starcraft116_event_gate.decide_game_event_emit(
        event=event,
        callback=callback,
        config_manager=config_manager,
        game_event_key_times=game_event_key_times,
        last_game_event_emit_time=last_game_event_emit_time,
        now=now,
        use_global_cooldown=use_global_cooldown,
    )
    if not decision.allowed:
        return False, game_event_key_times, last_game_event_emit_time

    try:
        callback(event)
    except Exception as e:
        log_callback(f"[StarCraft116GameEvents] event callback failed: {e}")
        return False, game_event_key_times, last_game_event_emit_time

    (
        game_event_key_times,
        last_game_event_emit_time,
    ) = starcraft116_event_gate.mark_game_event_emitted(
        decision,
        game_event_key_times,
        last_game_event_emit_time,
        use_global_cooldown=use_global_cooldown,
    )
    game_event_key_times = trim_game_event_keys(
        game_event_key_times,
        decision.now,
        decision.cooldown,
    )
    return True, game_event_key_times, last_game_event_emit_time
