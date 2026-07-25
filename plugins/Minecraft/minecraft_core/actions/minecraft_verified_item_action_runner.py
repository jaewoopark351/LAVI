#20260725_kpopmodder: Added this runner to compose action submission with inventory verification.
from __future__ import annotations

from typing import Any, Callable, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig
from .minecraft_action_completion_poller import MinecraftActionCompletionPoller
from .minecraft_inventory_delta_verifier import MinecraftInventoryDeltaVerifier
from .minecraft_item_count_snapshot_reader import MinecraftItemCountSnapshotReader


class MinecraftVerifiedItemActionRunner:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
        snapshot_reader: MinecraftItemCountSnapshotReader | None = None,
        completion_poller: MinecraftActionCompletionPoller | None = None,
        delta_verifier: MinecraftInventoryDeltaVerifier | None = None,
    ):
        self.config_manager = config_manager
        self.client_provider = client_provider
        self.snapshot_reader = snapshot_reader or MinecraftItemCountSnapshotReader(
            client_provider
        )
        self.completion_poller = completion_poller or MinecraftActionCompletionPoller(
            client_provider
        )
        self.delta_verifier = delta_verifier or MinecraftInventoryDeltaVerifier()

    def run(
        self,
        *,
        action: str,
        item: str,
        count: int,
        submit: Callable[[], Dict[str, Any]],
    ) -> Dict[str, Any]:
        if not self.config_manager.action_verification_enabled():
            result = self._result_dict(submit())
            result["verification"] = self.delta_verifier.disabled(
                action=action,
                item=item,
                requested_count=count,
            )
            return result

        before = self.snapshot_reader.read(item)
        result = self._result_dict(submit())
        action_id = self._action_id(result)
        if not result.get("ok") or not action_id:
            result["verification"] = {
                "enabled": True,
                "ok": False,
                "status": "skipped",
                "action": action,
                "item": item,
                "requested_count": count,
                "reason": "action_not_accepted",
                "message": "Skipped inventory verification because the action was not accepted.",
            }
            result["verified"] = False
            return result

        completion = self.completion_poller.wait(
            action_id,
            timeout_sec=self.config_manager.action_verification_timeout_sec(),
            poll_interval_sec=self.config_manager.action_verification_poll_interval_sec(),
        )
        after = self.snapshot_reader.read(item) if completion.get("ok") else None
        verification = self.delta_verifier.verify(
            action=action,
            item=item,
            requested_count=count,
            before=before,
            completion=completion,
            after=after,
        )
        self._attach_completion(result, completion)
        self._attach_verification(result, verification)
        return result

    def _attach_completion(
        self,
        result: Dict[str, Any],
        completion: Dict[str, Any],
    ) -> None:
        result["completion"] = {
            key: value
            for key, value in completion.items()
            if key in {"ok", "completion_status", "action_status", "message", "action"}
        }
        if isinstance(completion.get("action"), dict):
            result["action"] = completion["action"]

        completion_status = str(completion.get("completion_status") or "").strip().lower()
        if completion_status == "succeeded":
            return
        if completion_status == "timeout":
            result["ok"] = False
            result.setdefault("error", "action_completion_pending")
            result.setdefault("message", completion.get("message"))
            return
        if completion_status:
            result["ok"] = False
            result.setdefault("error", "action_completion_failed")
            result.setdefault("message", completion.get("message"))

    def _attach_verification(
        self,
        result: Dict[str, Any],
        verification: Dict[str, Any],
    ) -> None:
        result["verification"] = verification
        result["verified"] = bool(verification.get("ok"))
        if verification.get("status") in {"after_inventory_unavailable", "mismatch"}:
            result["ok"] = False
            result.setdefault("error", "inventory_verification_failed")
            result.setdefault("message", verification.get("message"))

    def _action_id(self, result: Dict[str, Any]) -> str | None:
        action_payload = result.get("action")
        if not isinstance(action_payload, dict):
            return None
        action_id = action_payload.get("action_id")
        return str(action_id) if action_id else None

    def _result_dict(self, result: Dict[str, Any]) -> Dict[str, Any]:
        return dict(result) if isinstance(result, dict) else {"ok": False, "raw": result}
