#20260915_kpopmodder: Emit bounded decision snapshots through the existing router logger.
import re


class KoreanConfirmationLogger:
    def __init__(self, callback):
        self._callback = callback

    def record(
        self, reason, *, event=None, command="", pending=0, decision_values=None
    ):
        def token(value):
            return re.sub(r"[^a-zA-Z0-9_.:-]", "_", str(value))[:80] or "none"

        message = (
            "event=korean_confirmation "
            f"reason={token(reason)} event_id={token(getattr(event, 'event_id', ''))} "
            f"source={token(getattr(event, 'source', ''))} command={token(command.split(' ', 1)[0])} "
            f"pending={int(pending)}"
        )
        for key, value in tuple((decision_values or {}).items())[:8]:
            if type(value) is bool:
                message += f" {token(key)}={str(value).lower()}"
            elif type(value) is int:
                message += f" {token(key)}={value}"
        try:
            self._callback(message)
        except Exception:
            # Logging cannot grant confirmation, submit, or change cleanup.
            pass
