#20260705_kpopmodder: Added this helper to keep StarCraft116 status display formatting outside the facade.
import json


def discovery_json(last_discovery):
    #20260705_kpopmodder: Match the previous empty discovery placeholder used by the Setup tab.
    if not last_discovery:
        return "{}"
    return json.dumps(last_discovery, indent=2, ensure_ascii=False)


def external_status_json(external_status):
    #20260705_kpopmodder: Keep ensure_ascii=False so Korean/Windows paths stay readable in Gradio.
    return json.dumps(
        external_status,
        indent=2,
        ensure_ascii=False,
    )


def status_summary_text(external_status):
    #20260705_kpopmodder: Preserve the previous summary line order shown in the Launch tab.
    summary = (external_status or {}).get(
        "summary",
        {},
    )
    lines = []
    phase = summary.get("phase", "")
    severity = summary.get("severity", "")
    if phase or severity:
        lines.append(f"{severity.upper()}: {phase}")
    lines.extend(summary.get("messages", []))
    next_actions = summary.get("next_actions", [])
    if next_actions:
        lines.append("Next:")
        lines.extend(f"- {action}" for action in next_actions)
    return "\n".join(lines)


def game_event_status_dict(
    config_manager,
    game_event_thread,
    last_game_event_emit_time,
    monster_log_events_active,
    bwapi_proxy_events_active,
):
    #20260705_kpopmodder: Build a display snapshot only; do not start/stop watcher threads here.
    return {
        "enabled": config_manager.get_bool("game_events_enabled", True),
        "path": config_manager.resolve_game_events_path(),
        "watching": bool(
            game_event_thread is not None and game_event_thread.is_alive()
        ),
        "last_emit_at": last_game_event_emit_time or None,
        "monster_log_enabled": monster_log_events_active,
        "monster_log_tts_enabled": config_manager.get_bool(
            "monster_log_tts_enabled",
            False,
        ),
        "monster_log_path": config_manager.resolve_monster_log_path(),
        "bwapi_proxy_events_enabled": bwapi_proxy_events_active,
        "bwapi_proxy_events_tts_enabled": config_manager.get_bool(
            "bwapi_proxy_events_tts_enabled",
            True,
        ),
        "bwapi_proxy_events_path": (
            config_manager.resolve_bwapi_proxy_events_path()
        ),
    }
