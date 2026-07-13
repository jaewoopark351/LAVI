#20260713_kpopmodder: Extract ladder-proxy event parsing and engine event routing.

from __future__ import annotations

import json

from core.logger import log_print
from .starcraft2_event_bus import _StarCraft2EventBus


class _StarCraft2EngineEventService:
    def __init__(self, state, status_event_callback=None, event_bus: _StarCraft2EventBus | None = None):
        self.state = state
        self.status_event_callback = status_event_callback
        self.event_bus = event_bus or _StarCraft2EventBus()

    def set_status_event_callback(self, callback):
        self.status_event_callback = callback
        if self.event_bus is not None:
            self.event_bus.set_status_event_callback(callback)

    def update_state(self, event):
        event = dict(event or {})
        self.state.update_event(event)
        if self.event_bus is not None:
            self.event_bus.publish(event)
            return
        if callable(self.status_event_callback):
            try:
                self.status_event_callback(event)
            except Exception as e:
                log_print(f"[StarCraft2] status event callback failed: {e}")


class _StarCraft2LadderProxyEventService:
    def __init__(
        self,
        engine_event_service: _StarCraft2EngineEventService,
        observation_tracker,
        event_bus: _StarCraft2EventBus | None = None,
    ):
        self.engine_event_service = engine_event_service
        self.observation_tracker = observation_tracker
        self.event_bus = event_bus

    def on_ladder_proxy_line(
        self,
        stream_name: str,
        line: str,
        status_event_callback=None,
        tts=None,
    ) -> None:
        text = str(line or "").strip()
        if text:
            log_print(f"[StarCraft2] ladder_proxy {stream_name}: {text[:1000]}")
            telemetry_prefix = "LAV_OBSERVATION "
            #20260710_kpopmodder: Ladder Proxy prepends its own timestamp
            # to stdout lines, so telemetry may not begin at position zero.
            telemetry_index = text.find(telemetry_prefix)
            if telemetry_index >= 0:
                try:
                    snapshot = json.loads(text[telemetry_index + len(telemetry_prefix):])
                except (TypeError, ValueError, json.JSONDecodeError):
                    snapshot = None
                for event in self.observation_tracker.update(snapshot):
                    self.engine_event_service.update_state(event)
                return
            #20260710_kpopmodder: Convert local-match lifecycle lines into
            # the existing LAV reaction/TTS callback path.
            lower = text.lower()
            event_type = ""
            if "starting the match" in lower:
                event_type = "game_started"
            elif "client changed status from in_game to ended" in lower:
                event_type = "game_ended"
            elif "finished with result:" in lower:
                # LavHumanVsBot assigns Player1 to LAVHuman and Player2 to
                # the AI. Report the result from the AI/TTS perspective;
                # checking only for the word "win" reverses Player1Win.
                if "initializationerror" in lower or "initialization error" in lower:
                    #20260711_kpopmodder: Startup failures are diagnostics, not
                    # match losses; engine_error stays log-only in the SC2 TTS policy.
                    event_type = "engine_error"
                elif "player2win" in lower or "player2 win" in lower:
                    event_type = "game_won"
                elif "player1win" in lower or "player1 win" in lower:
                    event_type = "game_lost"
                elif "player2loss" in lower or "player2 loss" in lower:
                    event_type = "game_lost"
                elif "player1loss" in lower or "player1 loss" in lower:
                    event_type = "game_won"
                else:
                    event_type = "game_won" if "win" in lower else "game_lost"
            elif self.is_ladder_proxy_error_line(lower):
                event_type = "engine_error"
            if event_type:
                event = {
                    "event_type": event_type,
                    "details": {"result": text, "source": stream_name},
                }
                if event_type == "game_started":
                    self.observation_tracker.reset()
                if self.event_bus is not None:
                    consumed = self.event_bus.publish(event)
                    if consumed:
                        return
                if callable(status_event_callback):
                    try:
                        status_event_callback(event)
                        return
                    except Exception as e:
                        log_print(f"[StarCraft2] ladder proxy TTS callback failed: {e}")
                if tts is not None:
                    receive_input = getattr(tts, "receive_input", None)
                    if callable(receive_input):
                        receive_input(f"StarCraft2 {event_type}")

    def is_ladder_proxy_error_line(self, lower_line: str) -> bool:
        lower = str(lower_line or "").lower()
        if not lower:
            return False
        #20260712_kpopmodder: Native diagnostics include harmless fields like
        # error_count=0 in successful CreateGame/JoinGame summaries. Do not
        # convert those normal summaries into engine_error events.
        if "error_count=0" in lower and "error:" not in lower:
            blocked_terms = (
                " failed",
                "failed ",
                " timeout",
                "timeout/",
                "closed/error",
                " crashed",
                "crashed ",
                "exception",
            )
            if not any(term in lower for term in blocked_terms):
                return False
        return (
            "error:" in lower
            or "timeout/closed/error" in lower
            or "waiting for a response had a timeout" in lower
            or " failed" in lower
            or "failed " in lower
            or " crashed" in lower
            or "crashed " in lower
            or "exception" in lower
        )
