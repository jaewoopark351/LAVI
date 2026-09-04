#20260905_kpopmodder: Prove every post-claim H5 failure spends authority without replay.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest import mock

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)
from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class AutoDepositTrustEarlyFailureContractTests(unittest.TestCase):
    COMMAND_TEXT = "@auto_deposit_trust area 16x16"

    def test_post_claim_failures_spend_receipt_and_block_replay(self):
        cases = (
            ("precheck_failure", "minecraft_bridge_disconnected", 0, 0, 0),
            (
                "exact_adapter_exception",
                "auto_deposit_trust_input_internal_error",
                0,
                0,
                0,
            ),
            (
                "translation_exception",
                "auto_deposit_trust_input_internal_error",
                1,
                0,
                0,
            ),
            (
                "translation_malformed",
                "auto_deposit_trust_input_internal_error",
                1,
                0,
                0,
            ),
            (
                "translation_rejected",
                "minecraft_translation_rejected",
                1,
                0,
                0,
            ),
            (
                "router_request_factory_exception",
                "auto_deposit_trust_input_internal_error",
                1,
                0,
                0,
            ),
            (
                "extension_request_factory_exception",
                "minecraft_command_rejected",
                1,
                1,
                0,
            ),
            (
                "claim_commit_failure",
                "minecraft_command_rejected",
                1,
                1,
                0,
            ),
            (
                "submitter_exception",
                "minecraft_submission_outcome_unknown",
                1,
                1,
                1,
            ),
            (
                "submitter_unknown",
                "minecraft_submission_outcome_unknown",
                1,
                1,
                1,
            ),
        )

        for index, (
            case,
            expected_reason,
            expected_translations,
            expected_extension_submissions,
            expected_adapter_submissions,
        ) in enumerate(cases, start=1):
            with self.subTest(case=case):
                harness = _build_harness(case)
                event = _chat_event(self.COMMAND_TEXT, f"{index:032x}")

                first = harness.router.route(event)
                receipt = harness.claim_registry.last_receipt

                self.assertTrue(first.handled)
                self.assertEqual(expected_reason, first.reason)
                self.assertIsNotNone(receipt)
                self.assertEqual("SPENT", harness.claim_registry.state(receipt))
                self.assertEqual(1, harness.claim_registry.claimed_count)
                self.assertEqual(0, harness.claim_registry.issued_count)
                self.assertEqual(
                    expected_translations,
                    harness.natural_language_service.translate.call_count,
                )
                self.assertEqual(
                    expected_extension_submissions,
                    harness.extension_submit.call_count,
                )
                self.assertEqual(
                    expected_adapter_submissions,
                    harness.adapter.submit_command.call_count,
                )

                calls_before_replay = (
                    harness.natural_language_service.translate.call_count,
                    harness.extension_submit.call_count,
                    harness.adapter.submit_command.call_count,
                )
                replay = harness.router.route(event)

                self.assertTrue(replay.handled)
                self.assertEqual(
                    "auto_deposit_trust_duplicate_input_event",
                    replay.reason,
                )
                self.assertEqual(
                    calls_before_replay,
                    (
                        harness.natural_language_service.translate.call_count,
                        harness.extension_submit.call_count,
                        harness.adapter.submit_command.call_count,
                    ),
                )
                self.assertEqual("SPENT", harness.claim_registry.state(receipt))
                self.assertEqual(0, harness.claim_registry.issued_count)


class _CapturingClaimRegistry(AutoDepositTrustInputEventClaimRegistry):
    def __init__(self):
        super().__init__()
        self.last_receipt = None

    def claim(self, event, *, claim_owner=None):
        receipt, reason = super().claim(event, claim_owner=claim_owner)
        if receipt is not None:
            self.last_receipt = receipt
        return receipt, reason


def _build_harness(case: str) -> SimpleNamespace:
    claim_registry = _CapturingClaimRegistry()
    adapter = _recording_adapter(
        connected=case != "precheck_failure",
        submit_mode=case,
    )
    service = mock.Mock(spec=ChatClefNaturalLanguageService)
    service.translate.return_value = _valid_translation()
    if case == "translation_exception":
        service.translate.side_effect = RuntimeError("translation failed")
    elif case == "translation_malformed":
        malformed = mock.Mock()
        malformed.to_dict.return_value = {"status": "not-a-status"}
        service.translate.return_value = malformed
    elif case == "translation_rejected":
        service.translate.return_value = ChatClefTranslationResultDTO.rejected(
            ChatClefIntentStatus.INVALID,
            "h5_rejected_for_test",
            "Rejected for the failure-path contract test.",
        )

    extension = MinecraftFabricChatClefExtension(
        adapter=adapter,
        natural_language_service=service,
        auto_deposit_trust_claim_registry=claim_registry,
    )
    exact_adapter = None
    if case == "exact_adapter_exception":
        exact_adapter = mock.Mock()
        exact_adapter.adapt.side_effect = RuntimeError("adaptation failed")
    router = MinecraftChatClefInputRouter(
        extension=extension,
        auto_deposit_trust_exact_input_adapter=exact_adapter,
        log_callback=lambda _message: None,
    )

    if case == "router_request_factory_exception":
        router._submission_boundary._request_factory = _raising_factory(
            "router request build failed"
        )
    elif case == "extension_request_factory_exception":
        extension._natural_language_commands._request_factory = _raising_factory(
            "extension request build failed"
        )
    elif case == "claim_commit_failure":
        claim_registry.commit = mock.Mock(return_value=False)

    extension_submit = mock.Mock(wraps=extension.submit_translated_command)
    extension.submit_translated_command = extension_submit
    return SimpleNamespace(
        router=router,
        extension_submit=extension_submit,
        natural_language_service=service,
        claim_registry=claim_registry,
        adapter=adapter,
    )


def _raising_factory(message: str) -> SimpleNamespace:
    return SimpleNamespace(build=mock.Mock(side_effect=RuntimeError(message)))


def _valid_translation() -> ChatClefTranslationResultDTO:
    return ChatClefTranslationResultDTO.validated(
        command="auto_deposit_trust area 16x16",
        intent=ChatClefIntentDTO(
            intent_type=ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA,
            original_text="auto_deposit_trust area 16x16",
            source="rule",
        ),
    )


def _chat_event(text: str, event_id: str) -> LaviInputEvent:
    return LaviInputEvent(
        text=text,
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id=event_id,
        fallback_payload=text,
    )


def _recording_adapter(*, connected: bool, submit_mode: str) -> SimpleNamespace:
    adapter = SimpleNamespace(backend_id="fabric_chatclef", requests=[])

    def submit_command(request):
        adapter.requests.append(request)
        if submit_mode == "submitter_exception":
            raise RuntimeError("adapter submission failed")
        if submit_mode == "submitter_unknown":
            return CommandResultDTO(
                request_id=request.request_id,
                ok=False,
                status=CommandResultStatus.UNKNOWN,
                error_code=BridgeErrorCode.INTERNAL_ERROR,
                message="submission outcome unknown",
                data={
                    "submission_outcome": "submission_outcome_unknown",
                    "reconciliation_required": True,
                },
            )
        return CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            message="accepted",
            data={"command": request.command},
        )

    def get_status():
        return StatusSnapshotDTO(
            backend_id=adapter.backend_id,
            enabled=True,
            connected=connected,
            lifecycle_state=(
                BridgeLifecycleState.CONNECTED
                if connected
                else BridgeLifecycleState.DISCONNECTED
            ),
            detail="connected" if connected else "disconnected for test",
            details={"commands": {"active_request_id": None}},
        )

    adapter.submit_command = mock.Mock(side_effect=submit_command)
    adapter.get_status = get_status
    return adapter


if __name__ == "__main__":
    unittest.main()
