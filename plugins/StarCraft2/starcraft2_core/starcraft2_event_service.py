#20260713_kpopmodder: Extract ladder-proxy event parsing and engine event routing.

from __future__ import annotations

import json
import re
import time

from .starcraft2_contracts import StarCraft2Event
from .starcraft2_event_bus import StarCraft2EventBus
from core.logger import log_print


class StarCraft2EngineEventService:
    #20260715_kpopmodder: Public event-state adapter for SC2 engine events.
    def __init__(self, state, status_event_callback=None, event_bus: StarCraft2EventBus | None = None):
        self.state = state
        self.event_bus = event_bus or StarCraft2EventBus()

    def set_status_event_callback(self, callback):
        #20260714_kpopmodder: Keep API compatibility for legacy callers,
        # but route callback through subscribe path only. None clears it.
        if self.event_bus is not None:
            self.event_bus.set_status_event_callback(callback)

    def update_state(self, event: StarCraft2Event) -> None:
        normalized = StarCraft2Event.from_mapping(event)
        #20260715_kpopmodder: RuntimeState now receives the DTO directly.
        self.state.update_event(normalized)
        if self.event_bus is not None:
            self.event_bus.emit(normalized)


class StarCraft2LadderProxyEventService:
    #20260715_kpopmodder: Public stdout/event parser boundary for ladder-proxy output.
    def __init__(
        self,
        engine_event_service: StarCraft2EngineEventService,
        observation_tracker,
        event_bus: StarCraft2EventBus | None = None,
    ):
        self.engine_event_service = engine_event_service
        self.observation_tracker = observation_tracker
        self.event_bus = event_bus
        #20260713_kpopmodder: Ignore known RESPONSE_NOT_SET tail noise after a match end.
        self._response_not_set_ignore_until = 0.0
        #20260716_kpopmodder: SC2 can emit PyInstaller/protocol noise while
        # tearing down after a valid match result. Keep that tail log-only.
        self._terminal_shutdown_error_ignore_until = 0.0
        #20260715_kpopmodder: Both SC2 clients report ended; emit one match event.
        self._game_ended_emitted = False

    def on_ladder_proxy_line(
        self,
        stream_name: str,
        line: str,
    ) -> None:
        text = str(line or "").strip()
        if not text:
            return
        log_print(f"[StarCraft2] ladder_proxy {stream_name}: {text[:1000]}")
        for event in self.parse_line(stream_name, text):
            self.engine_event_service.update_state(event)

    def parse_line(self, stream_name: str, line: str) -> list[StarCraft2Event]:
        """Convert one ladder-proxy output line into typed SC2 events."""
        text = str(line or "").strip()
        if not text:
            return []

        telemetry_prefix = "LAV_OBSERVATION "
        #20260710_kpopmodder: Ladder Proxy prepends its own timestamp
        # to stdout lines, so telemetry may not begin at position zero.
        telemetry_index = text.find(telemetry_prefix)
        if telemetry_index >= 0:
            try:
                snapshot = json.loads(text[telemetry_index + len(telemetry_prefix):])
            except (TypeError, ValueError, json.JSONDecodeError):
                return []
            #20260715_kpopmodder: ObservationTracker is the typed telemetry boundary.
            return list(self.observation_tracker.update(snapshot))

        #20260710_kpopmodder: Convert local-match lifecycle lines into
        # the existing LAV reaction/TTS callback path.
        lower = text.lower()
        event_type = ""
        if "starting the match" in lower:
            event_type = "game_started"
            self._response_not_set_ignore_until = 0.0
            self._terminal_shutdown_error_ignore_until = 0.0
            self._game_ended_emitted = False
            self.observation_tracker.reset()
        elif "client changed status from in_game to ended" in lower:
            now = time.time()
            self._response_not_set_ignore_until = now + 5.0
            self._terminal_shutdown_error_ignore_until = now + 30.0
            if not self._game_ended_emitted:
                event_type = "game_ended"
                self._game_ended_emitted = True
        elif "finished with result:" in lower:
            self._terminal_shutdown_error_ignore_until = time.time() + 30.0
            # LavHumanVsBot assigns Player1 to LAVHuman and Player2 to
            # the AI. Report the result from the AI/TTS perspective.
            if "initializationerror" in lower or "initialization error" in lower:
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

        if not event_type:
            return []
        return [
            StarCraft2Event(
                event_type=event_type,
                details={"result": text, "source": str(stream_name or "stdout")},
            )
        ]

    def _is_response_not_set_end_tail(self, lower: str, now: float) -> bool:
        if "response_not_set" not in lower:
            return False

        if "not supported if game has already ended" in lower or "already ended" in lower:
            return True

        # The first RESPONSE_NOT_SET line is usually the paired message that
        # arrives right before/at game end, followed by one more summary line.
        # Treat this burst as benign if it falls in the ignore window.
        if now <= self._response_not_set_ignore_until:
            return True
        if (
            "expected query but got response_not_set" in lower
            or "expected action but got response_not_set" in lower
            or "response response_not_set has" in lower
        ):
            self._response_not_set_ignore_until = now + 2.0
            return True

        return False

    def _is_terminal_shutdown_error_line(self, lower: str, now: float) -> bool:
        #20260716_kpopmodder: Do not promote the well-known "game already
        # ended" cleanup burst into engine_error after the result is known.
        already_ended = "already ended" in lower or "game has already ended" in lower
        terminal_protocol = (
            already_ended
            and (
                "not supported" in lower
                or "protocolerror" in lower
                or "response_not_set" in lower
            )
        )
        if terminal_protocol:
            self._response_not_set_ignore_until = now + 5.0
            self._terminal_shutdown_error_ignore_until = now + 30.0
            return True

        if now > self._terminal_shutdown_error_ignore_until:
            return False

        shutdown_followup_markers = (
            "[pyi-",
            "failed to execute script",
            "unhandled exception",
            "unclosed client session",
            "task exception was never retrieved",
            "expected query but got response_not_set",
            "expected action but got response_not_set",
            "response response_not_set has",
        )
        return any(marker in lower for marker in shutdown_followup_markers)

    def is_ladder_proxy_error_line(self, lower_line: str) -> bool:
        lower = str(lower_line or "").lower()
        if not lower:
            return False

        now = time.time()
        if self._is_terminal_shutdown_error_line(lower, now):
            return False
        if self._is_response_not_set_end_tail(lower, now):
            return False

        #20260715_kpopmodder: Native BotLaunchDiagnostics reports boolean
        # fields such as failed=false. Remove only the successful field before
        # checking for real failure words elsewhere on the same line.
        error_probe = re.sub(r"\bfailed\s*=\s*false\b", "", lower)

        #20260712_kpopmodder: Native diagnostics include harmless fields like
        # error_count=0 in successful CreateGame/JoinGame summaries. Do not
        # convert those normal summaries into engine_error events.
        if "error_count=0" in error_probe and "error:" not in error_probe:
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
            if not any(term in error_probe for term in blocked_terms):
                return False
        return (
            "error:" in error_probe
            or "timeout/closed/error" in error_probe
            or "waiting for a response had a timeout" in error_probe
            or "response_not_set" in error_probe
            or " failed" in error_probe
            or "failed " in error_probe
            or " crashed" in error_probe
            or "crashed " in error_probe
            or "exception" in error_probe
        )


_StarCraft2EngineEventService = StarCraft2EngineEventService
_StarCraft2LadderProxyEventService = StarCraft2LadderProxyEventService
