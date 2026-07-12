#20260706_kpopmodder: Added this helper to keep StarCraft116 event file polling outside the plugin facade.
from core.logger import log_print
from .starcraft116_game_events import StarCraft116GameEventTailer
from .starcraft116_reaction_policy import build_starcraft116_game_event


class StarCraft116EventPoller:
    #20260706_kpopmodder: Owns game/Monster/BWAPI polling while StarCraft116 keeps public lifecycle methods.
    def __init__(self, owner):
        self.owner = owner

    def poll_game_events(self):
        owner = self.owner
        if not owner.config_manager.get_bool("openai_reactions_enabled", True):
            return 0
        if not owner.config_manager.get_bool("game_events_enabled", True):
            return 0

        emitted = 0
        path = owner.config_manager.resolve_game_events_path()
        result = owner.game_event_tailer.read_new_events(
            path,
            max_events=owner.config_manager.get_int(
                "game_events_max_events_per_poll",
                6,
            ),
        )
        for error in result.errors:
            log_print(f"[StarCraft116GameEvents] {error}")

        for raw_event in result.events:
            event = build_starcraft116_game_event(
                raw_event,
                profile=owner.config_manager.get_active_profile_name(),
            )
            if owner._maybe_emit_game_event(event):
                emitted += 1
        emitted += self.poll_monster_log_events()
        emitted += self.poll_bwapi_proxy_events()
        return emitted

    def poll_monster_log_events(self):
        owner = self.owner
        if not owner._is_monster_log_events_active():
            return 0

        path = owner.config_manager.resolve_monster_log_path()
        result = owner.monster_log_tailer.read_new_events(
            path,
            max_events=owner.config_manager.get_int(
                "game_events_max_events_per_poll",
                6,
            ),
        )
        for error in result.errors:
            log_print(f"[StarCraft116MonsterLogEvents] {error}")

        emitted = 0
        for raw_event in result.events:
            event_details = raw_event.get("details") or {}
            event_type = str(raw_event.get("event_type", "")) or ""
            severity = str(raw_event.get("severity", "")).lower()
            exit_code_log = ""
            if raw_event.get("event_type") == "monster_exit_code":
                exit_code = event_details.get("exit_code")
                cause = event_details.get("cause") or event_details.get("exit_code_cause")
                reason = event_details.get("reason") or event_details.get("exit_code_reason")
                if exit_code is not None:
                    exit_code_log = f" exit_code={exit_code}"
                if cause:
                    exit_code_log += f" cause={cause}"
                else:
                    exit_code_log += " cause=unknown"
                if reason:
                    exit_code_log += f" reason={reason}"
                else:
                    exit_code_log += " reason=unknown"

            if raw_event.get("event_type") == "monster_exit_code" and severity == "error":
                log_print(
                    "[StarCraft116MonsterLogEvents][ERROR] monster_exit_code: "
                    f"type={event_type} severity={raw_event.get('severity', '')}"
                    f"{exit_code_log}"
                )
            else:
                log_print(
                    "[StarCraft116MonsterLogEvents] event: "
                    f"type={event_type} "
                    f"severity={raw_event.get('severity', '')}"
                    f"{exit_code_log}"
                )
            if not raw_event.get("tts_eligible", False):
                continue
            event = build_starcraft116_game_event(
                raw_event,
                profile=owner.config_manager.get_active_profile_name(),
            )
            if owner._maybe_emit_game_event(event):
                emitted += 1
        return emitted

    def poll_bwapi_proxy_events(self):
        owner = self.owner
        if not owner._is_bwapi_proxy_events_active():
            return 0

        path = owner.config_manager.resolve_bwapi_proxy_events_path()
        tailer = getattr(owner, "bwapi_proxy_event_tailer", None)
        if tailer is None:
            tailer = StarCraft116GameEventTailer(start_at_end=True)
            owner.bwapi_proxy_event_tailer = tailer
        result = tailer.read_new_events(
            path,
            max_events=owner.config_manager.get_int(
                "game_events_max_events_per_poll",
                6,
            ),
        )
        for error in result.errors:
            log_print(f"[StarCraft116BWAPIProxyEvents] {error}")

        emitted = 0
        tts_enabled = owner.config_manager.get_bool(
            "bwapi_proxy_events_tts_enabled",
            True,
        )
        for raw_event in result.events:
            event_type = str(raw_event.get("event_type", "") or "")
            log_print(
                "[StarCraft116BWAPIProxyEvents] event: "
                f"type={event_type} "
                f"severity={raw_event.get('severity', '')}"
            )
            if not tts_enabled or not raw_event.get("tts_eligible", False):
                continue
            if owner._is_noisy_unknown_enemy_destroyed_event(raw_event):
                continue
            raw_event = dict(raw_event)
            raw_event.setdefault(
                "profile",
                owner.config_manager.get_active_profile_name(),
            )
            event = build_starcraft116_game_event(
                raw_event,
                profile=owner.config_manager.get_active_profile_name(),
            )
            use_global_cooldown = event_type not in {
                "bwapi_proxy_loaded",
                "bwapi_real_loaded",
                "bwapi_real_load_failed",
            }
            if owner._maybe_emit_game_event(
                event,
                use_global_cooldown=use_global_cooldown,
            ):
                emitted += 1
        return emitted
