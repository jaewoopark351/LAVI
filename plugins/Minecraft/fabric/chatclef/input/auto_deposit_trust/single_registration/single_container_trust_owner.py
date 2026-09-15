#20260915_kpopmodder: Bind exact single-container registration to the original live trusted input under the route lock.
from collections.abc import Mapping
import re

from plugins.Minecraft.fabric.chatclef.intent.grammar.control.korean_control_rule_parser import (
    KoreanControlRuleParser,
)
from plugins.Minecraft.fabric.chatclef.intent.grammar.validation.korean_command_request_guard import (
    KoreanCommandRequestGuard,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from .single_container_trust_receipt import SingleContainerTrustReceipt


class SingleContainerTrustOwner:
    def __init__(
        self, *, extension, submission_precheck, live_proof_validator, log_callback
    ):
        self._extension = extension
        self._precheck = submission_precheck
        self._proof = live_proof_validator
        self._log = log_callback
        self._receipts = {}

    def issue(self, *, event, proof, translation):
        # Match the existing ordinary translation's outer-space normalization;
        # raw guards and immutable event authority still inspect the original input.
        parsed = KoreanControlRuleParser().parse(event.text.strip())
        valid = (
            KoreanCommandRequestGuard.reason(event.text) is None
            and parsed is not None
            and parsed.intent_type is ChatClefIntentType.AUTO_DEPOSIT_TRUST
            and translation.get("command") == "auto_deposit_trust"
            and translation.get("intent") == parsed.to_dict()
            and event.source in {"lavi_chat_ui", "voice_input_final"}
            and event.final is True
            and self._proof(proof, event) is True
        )
        session = self._session() if valid else None
        if not valid or session is None:
            self._emit("rejected", event)
            return None, MinecraftChatClefInputRouteDecision.handled_result(
                reason="single_container_trust_input_rejected",
                response_text="현재 입력이나 연결 정보를 확인하지 못해서 자동 보관 장소를 등록하지 않았어.",
                route_kind="minecraft_command",
                publish_external_response=True,
            )
        receipt = SingleContainerTrustReceipt(self, event, proof, session)
        self._receipts[event.source] = receipt
        self._emit("issued", event)
        return receipt, None

    def is_live(self, receipt):
        return (
            type(receipt) is SingleContainerTrustReceipt
            and receipt._owner is self
            and self._receipts.get(getattr(receipt.event, "source", None)) is receipt
            and self._proof(receipt.proof, receipt.event) is True
            and self._session() == receipt.session
        )

    def commit(self, receipt, request):
        metadata = getattr(request, "metadata", None)
        event = metadata.get("input_event") if isinstance(metadata, Mapping) else None
        expected = receipt.event
        binding = {
            "event_id": expected.event_id,
            "source": expected.source,
            "provider_id": expected.provider_id,
            "event_kind": expected.event_kind,
            "final": True,
        }
        live = SingleContainerTrustOwner.is_live(self, receipt)
        matches = (
            getattr(request, "command", None) == "auto_deposit_trust"
            and getattr(request, "source", None) == expected.source
            and isinstance(event, Mapping)
            and all(event.get(key) == value for key, value in binding.items())
            and event.get("final") is True
            and metadata.get("korean_single_trust") == receipt.binding_metadata()
        )
        SingleContainerTrustOwner.abandon(
            self,
            receipt,
            reason="committed" if live and matches else "commit_rejected",
            live=live,
            matches=matches,
        )
        return live and matches

    def abandon(self, receipt, reason="abandoned", live=False, matches=False):
        if (
            type(receipt) is not SingleContainerTrustReceipt
            or self._receipts.get(receipt.event.source) is not receipt
        ):
            return
        self._receipts.pop(receipt.event.source, None)
        self._emit(reason, receipt.event, live=live, matches=matches)

    def _session(self):
        readiness = self._precheck.inspect(self._extension)
        details = readiness.status.get("details")
        commands = details.get("commands") if isinstance(details, Mapping) else None
        if readiness.ready is not True or not isinstance(commands, Mapping):
            return None
        session, generation = (
            commands.get("active_session_id"),
            commands.get("active_generation"),
        )
        if (
            type(session) is not str
            or not session
            or len(session) > 256
            or type(generation) is not int
            or generation <= 0
        ):
            return None
        return session, generation

    def _emit(self, reason, event, *, live=False, matches=False):
        identity = re.sub(r"[^a-zA-Z0-9_-]", "_", str(event.event_id))[:64]
        try:
            self._log(
                f"event=single_container_trust reason={reason} event_id={identity} "
                f"receipt_live={str(live).lower()} request_match={str(matches).lower()} retained={len(self._receipts)}"
            )
        except Exception:
            pass
