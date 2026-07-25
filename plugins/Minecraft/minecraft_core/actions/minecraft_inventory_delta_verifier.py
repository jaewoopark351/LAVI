#20260725_kpopmodder: Added this verifier to compare before/after inventory counts for item actions.
from __future__ import annotations

from typing import Any, Dict


class MinecraftInventoryDeltaVerifier:
    def verify(
        self,
        *,
        action: str,
        item: str,
        requested_count: int,
        before: Dict[str, Any],
        completion: Dict[str, Any],
        after: Dict[str, Any] | None,
    ) -> Dict[str, Any]:
        base = {
            "enabled": True,
            "action": action,
            "item": item,
            "requested_count": requested_count,
            "action_id": self._action_id(completion),
            "completion_status": completion.get("completion_status"),
        }

        if not before.get("ok"):
            return {
                **base,
                "ok": False,
                "status": "skipped",
                "reason": "before_inventory_unavailable",
                "message": "Skipped inventory verification because the initial inventory could not be read.",
                "before": before,
            }

        before_count = int(before.get("count", 0))
        base["before_count"] = before_count

        completion_status = completion.get("completion_status")
        if completion_status != "succeeded":
            status = "timeout" if completion_status == "timeout" else "action_not_succeeded"
            message = (
                "Minecraft action is still running after the verification timeout."
                if completion_status == "timeout"
                else "Minecraft action did not finish successfully."
            )
            return {
                **base,
                "ok": False,
                "status": status,
                "message": message,
                "completion": completion,
            }

        if not isinstance(after, dict) or not after.get("ok"):
            return {
                **base,
                "ok": False,
                "status": "after_inventory_unavailable",
                "message": "Minecraft action completed, but final inventory could not be read.",
                "after": after,
            }

        after_count = int(after.get("count", 0))
        expected_min_count = before_count + requested_count
        actual_delta = after_count - before_count
        verified = after_count >= expected_min_count
        return {
            **base,
            "ok": verified,
            "status": "verified" if verified else "mismatch",
            "message": (
                "Minecraft inventory verification passed."
                if verified
                else "Minecraft inventory count did not increase enough."
            ),
            "after_count": after_count,
            "expected_min_count": expected_min_count,
            "actual_delta": actual_delta,
        }

    def disabled(self, *, action: str, item: str, requested_count: int) -> Dict[str, Any]:
        return {
            "enabled": False,
            "ok": False,
            "status": "disabled",
            "action": action,
            "item": item,
            "requested_count": requested_count,
            "message": "Minecraft inventory verification is disabled in config.",
        }

    def _action_id(self, completion: Dict[str, Any]) -> str | None:
        action = completion.get("action")
        if isinstance(action, dict):
            action_id = action.get("action_id")
            return str(action_id) if action_id else None
        return None
