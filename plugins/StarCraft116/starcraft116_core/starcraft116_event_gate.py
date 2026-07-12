#20260706_kpopmodder: Added this helper to isolate StarCraft116 event filtering, cooldown, and duplicate suppression.
from dataclasses import dataclass

from .starcraft116_reaction_policy import build_starcraft116_game_event_key


@dataclass(frozen=True)
class StarCraft116GameEventDecision:
    allowed: bool
    reason: str
    event_key: str = ""
    now: float = 0.0
    cooldown: float = 0.0


def game_event_cooldown(config_manager):
    return max(
        0.0,
        config_manager.get_float(
            "game_events_reaction_cooldown_sec",
            8.0,
        ),
    )


def decide_game_event_emit(
    event,
    callback,
    config_manager,
    game_event_key_times,
    last_game_event_emit_time,
    now,
    use_global_cooldown=True,
):
    #20260706_kpopmodder: Keep the old event gate rules together so the facade stays thin.
    if not event:
        return StarCraft116GameEventDecision(False, "empty_event", now=now)
    if not callable(callback):
        return StarCraft116GameEventDecision(False, "missing_callback", now=now)

    event_key = build_starcraft116_game_event_key(event)
    cooldown = game_event_cooldown(config_manager)
    if now - game_event_key_times.get(event_key, 0.0) < cooldown:
        return StarCraft116GameEventDecision(
            False,
            "duplicate_cooldown",
            event_key=event_key,
            now=now,
            cooldown=cooldown,
        )
    if use_global_cooldown and now - last_game_event_emit_time < cooldown:
        return StarCraft116GameEventDecision(
            False,
            "global_cooldown",
            event_key=event_key,
            now=now,
            cooldown=cooldown,
        )

    return StarCraft116GameEventDecision(
        True,
        "allowed",
        event_key=event_key,
        now=now,
        cooldown=cooldown,
    )


def mark_game_event_emitted(
    decision,
    game_event_key_times,
    last_game_event_emit_time,
    use_global_cooldown=True,
):
    if not decision.allowed:
        return game_event_key_times, last_game_event_emit_time

    game_event_key_times[decision.event_key] = decision.now
    if use_global_cooldown:
        last_game_event_emit_time = decision.now
    return game_event_key_times, last_game_event_emit_time


def trim_game_event_keys(game_event_key_times, now, cooldown):
    #20260706_kpopmodder: Preserve the bounded duplicate-event cache behavior after moving it out of the runtime wrapper.
    if len(game_event_key_times) <= 256:
        return game_event_key_times
    keep_after = now - max(cooldown * 4, 60.0)
    return {
        key: timestamp
        for key, timestamp in game_event_key_times.items()
        if timestamp >= keep_after
    }
