#20260907_kpopmodder: Lock acceptance, presentation, and delivery boundaries.
from __future__ import annotations

import inspect
import unittest
from unittest.mock import Mock

from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.terminal import (
    CraftingFeedbackTerminalPresenter,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackResultAcceptance,
    CraftingFeedbackResultCoordinator,
    CraftingFeedbackTerminalDelivery,
    CraftingFeedbackTerminalPublication,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting.publication.crafting_feedback_terminal_publication import (
    CraftingFeedbackTerminalPublication as PublicationSource,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting.result.acceptance.crafting_feedback_result_acceptance import (
    CraftingFeedbackResultAcceptance as AcceptanceSource,
)
from plugins.Minecraft.fabric.chatclef.transport.server.runtime.fabric_chatclef_server_component_graph import (
    FabricChatClefServerComponentGraph,
)


class CraftingFeedbackResultBoundaryTests(unittest.TestCase):
    def test_coordinator_delegates_acceptance_and_injected_presentation(self):
        presenter = Mock()
        presenter.present = Mock(return_value=object())
        stop_arbitrator = object()
        evidence_failure_reporter = object()
        coordinator = CraftingFeedbackResultCoordinator(
            tracker=Mock(),
            effect_verifier=Mock(),
            response_renderer=presenter,
            stop_terminal_arbitrator=stop_arbitrator,
            evidence_failure_reporter=evidence_failure_reporter,
        )

        self.assertIsInstance(
            coordinator._acceptance,
            CraftingFeedbackResultAcceptance,
        )
        self.assertIs(coordinator._terminal_presenter, presenter)
        self.assertIs(
            coordinator._acceptance._coordinator._stop_terminal_arbitrator,
            stop_arbitrator,
        )
        self.assertIs(
            coordinator._acceptance._coordinator._evidence_failures,
            evidence_failure_reporter,
        )

    def test_delivery_forwards_rendered_response_without_using_factory(self):
        listener = Mock()
        factory = Mock()
        delivery = CraftingFeedbackTerminalDelivery(
            terminal_listener=listener,
            diagnostics=Mock(),
            terminal_response_factory=factory,
        )
        response = object()

        delivery.publish(response)

        listener.publish.assert_called_once_with(response)
        factory.assert_not_called()

    def test_transport_core_has_no_response_or_presentation_import(self):
        sources = (
            inspect.getsource(CraftingFeedbackResultCoordinator),
            inspect.getsource(CraftingFeedbackTerminalDelivery),
            inspect.getsource(CraftingFeedbackTerminalPublication),
            inspect.getsource(AcceptanceSource),
            inspect.getsource(PublicationSource),
        )
        for source in sources:
            with self.subTest(source=source.splitlines()[0]):
                self.assertNotIn("fabric.chatclef.response", source)
                self.assertNotIn("fabric.chatclef.presentation", source)

    def test_production_graph_injects_presenter_and_stop_arbitrator(self):
        source = inspect.getsource(FabricChatClefServerComponentGraph.__init__)

        self.assertIn(CraftingFeedbackTerminalPresenter.__name__, source)
        self.assertIn("CraftingFeedbackTerminalPublication", source)
        self.assertIn("CommandStopTerminalArbitrator", source)
        self.assertIn("CommandTerminalEvidenceFailureReporter(diagnostics)", source)
        self.assertLess(
            source.index("self.stop_control_tracker_registry"),
            source.index("CraftingFeedbackResultCoordinator("),
        )


if __name__ == "__main__":
    unittest.main()
