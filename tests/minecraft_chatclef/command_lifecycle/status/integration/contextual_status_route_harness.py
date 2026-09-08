#20260908_kpopmodder: Exercise one active descriptor through the real STATUS coordinator and route sequence.
from __future__ import annotations

from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_input_route_sequence import (
    MinecraftInputRouteSequence,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQueryClassifier,
    CommandStatusRouteOwner,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackStatusCoordinator,
    CommandTerminalEvidenceProfileRegistry,
)


class ContextualStatusRouteHarness:
    def __init__(self, *, proof_validator, status_inspector=None) -> None:
        self.owner_token = object()
        self.current_descriptor = None
        self.last_query = None
        self.last_snapshot = None
        self.ordinary_route_calls = 0
        self._status_inspector = status_inspector
        self.renderer = CommandLifecycleResponseRenderer()
        self.classifier = CommandStatusQueryClassifier()
        self.status_coordinator = CommandFeedbackStatusCoordinator(
            evidence_profiles=CommandTerminalEvidenceProfileRegistry(),
        )
        self.status_owner = CommandStatusRouteOwner(
            extension=SimpleNamespace(
                inspect_command_feedback_status=self._inspect_status,
            ),
            live_proof_validator=proof_validator,
            classifier=self.classifier,
            response_renderer=self.renderer,
        )
        self.route_sequence = MinecraftInputRouteSequence(
            input_event_normalizer=SimpleNamespace(normalize=lambda value: value),
            stop_control_route_owner=None,
            crafting_status_route_owner=self.status_owner,
            generic_crafting_defaults_route_owner=SimpleNamespace(
                try_route=self._unexpected_later_route,
            ),
            auto_deposit_trust_route_coordinator=SimpleNamespace(
                route=self._unexpected_later_route,
            ),
            ordinary_command_route_coordinator=SimpleNamespace(
                route=self._ordinary_route,
            ),
        )

    def route(self, *, event: object, proof: object, descriptor: object):
        self.current_descriptor = descriptor
        self.last_query = None
        self.last_snapshot = None
        return self.route_sequence.route(
            event,
            korean_eligibility_proof=proof,
            optional_route_callback=self._invoke_optional_route,
            gate_inspection_callback=self._unexpected_gate_inspection,
        )

    def _inspect_status(self, query: object):
        self.last_query = query
        if self._status_inspector is not None:
            self.last_snapshot = self._status_inspector(query)
            return self.last_snapshot
        self.last_snapshot = self.status_coordinator.inspect(
            state=SimpleNamespace(
                context=SimpleNamespace(
                    descriptor=self.current_descriptor,
                    owner_token=self.owner_token,
                ),
                terminal_claimed=False,
                latest_result_reason="dispatch_started",
                latest_status="running",
            ),
            active_command=self.owner_token,
            connected=True,
            quarantine_active=False,
            query=query,
        )
        return self.last_snapshot

    @staticmethod
    def _invoke_optional_route(
        route_owner: object,
        event: object,
        proof: object,
        _failure_reason: str,
    ):
        if route_owner is None:
            return None
        return route_owner.try_route(event, proof)

    def _ordinary_route(self, *_args, **_kwargs):
        self.ordinary_route_calls += 1
        raise AssertionError("a claimed STATUS query reached ordinary submission")

    @staticmethod
    def _unexpected_later_route(*_args, **_kwargs):
        raise AssertionError("a STATUS candidate reached a later Minecraft owner")

    @staticmethod
    def _unexpected_gate_inspection(_text: str):
        raise AssertionError("a STATUS candidate reached Minecraft intent gating")


__all__ = ("ContextualStatusRouteHarness",)
