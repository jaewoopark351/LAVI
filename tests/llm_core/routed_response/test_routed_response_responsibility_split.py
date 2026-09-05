# 20260905_kpopmodder: Verifies routed-response responsibilities remain isolated.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError

from llm_core.routed_response import (
    CommandFeedbackDeliveryLogger,
    RoutedExternalResponsePublisher,
    RoutedResponseCapabilityAuthorizer,
    RoutedResponseDeliveryObserver,
    RoutedResponseFullOutputSinkDelivery,
    RoutedResponseHistorySinkDelivery,
    RoutedResponseOutputSinkDelivery,
    RoutedResponsePublicationCoordinator,
    RoutedResponseRequest,
    RoutedResponseRequestValidator,
    RoutedResponseSinkDelivery,
)


class RoutedResponseResponsibilitySplitTests(unittest.TestCase):
    def test_request_validator_returns_an_immutable_normalized_request(self):
        request = RoutedResponseRequestValidator(history_available=False).validate(
            "  response  ",
            source="minecraft_chatclef",
            send_output=True,
            send_full_output=False,
            remember_history=False,
            event_id="a" * 32,
            route_kind="minecraft_command",
            response_kind="immediate",
        )

        self.assertEqual("response", request.text)
        with self.assertRaises(FrozenInstanceError):
            request.text = "changed"
        with self.assertRaises(TypeError):
            RoutedResponseRequestValidator(history_available=False).validate(
                "response",
                source="minecraft_chatclef",
                send_output=1,
                send_full_output=False,
                remember_history=False,
                event_id=None,
                route_kind="minecraft_command",
                response_kind="immediate",
            )

    def test_facade_composes_each_focused_responsibility(self):
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: 1,
            build_output_payload_callback=lambda text, generation: (
                text,
                generation,
            ),
            send_output_callback=lambda _payload: None,
            send_full_output_callback=lambda _text: None,
        )

        self.assertIsInstance(
            publisher._request_validator,
            RoutedResponseRequestValidator,
        )
        self.assertIsInstance(
            publisher._capability_authorizer,
            RoutedResponseCapabilityAuthorizer,
        )
        self.assertIsInstance(
            publisher._delivery_observer,
            RoutedResponseDeliveryObserver,
        )
        self.assertIsInstance(
            publisher._sink_delivery,
            RoutedResponseSinkDelivery,
        )
        self.assertIsInstance(
            publisher._publication_coordinator,
            RoutedResponsePublicationCoordinator,
        )
        self.assertIsInstance(
            publisher._sink_delivery._output_delivery,
            RoutedResponseOutputSinkDelivery,
        )
        self.assertIsInstance(
            publisher._sink_delivery._full_output_delivery,
            RoutedResponseFullOutputSinkDelivery,
        )
        self.assertIsInstance(
            publisher._sink_delivery._history_delivery,
            RoutedResponseHistorySinkDelivery,
        )

    def test_sink_failure_diagnostics_exclude_response_and_error_text(self):
        canonical_logs = []
        bounded_logs = []
        observer = RoutedResponseDeliveryObserver(
            delivery_logger=CommandFeedbackDeliveryLogger(canonical_logs.append),
            log_callback=bounded_logs.append,
        )
        delivery = RoutedResponseSinkDelivery(
            begin_generation_callback=lambda: 4,
            build_output_payload_callback=lambda _text, _generation: "SECRET_PAYLOAD",
            send_output_callback=lambda _payload: (_ for _ in ()).throw(
                RuntimeError("SECRET_ERROR")
            ),
            send_full_output_callback=lambda _text: None,
            observer=observer,
        )
        request = RoutedResponseRequest(
            text="SECRET_RESPONSE",
            source="minecraft_chatclef",
            send_output=True,
            send_full_output=False,
            remember_history=False,
            event_id="b" * 32,
            route_kind="minecraft_command",
            response_kind="immediate",
        )

        emission = delivery.deliver(request)

        self.assertFalse(emission.output_delivered)
        combined = " ".join(canonical_logs + bounded_logs)
        self.assertNotIn("SECRET_RESPONSE", combined)
        self.assertNotIn("SECRET_PAYLOAD", combined)
        self.assertNotIn("SECRET_ERROR", combined)
        self.assertIn("reason=delivery_failed", canonical_logs[0])


if __name__ == "__main__":
    unittest.main()
