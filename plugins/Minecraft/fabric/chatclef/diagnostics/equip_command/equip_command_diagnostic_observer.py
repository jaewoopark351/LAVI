#20260915_kpopmodder: Emit one bounded verdict at the existing once-only terminal claim; logs never select behavior.
from plugins.Minecraft.fabric.chatclef.result.equip import EquipEffectPayload


class EquipCommandDiagnosticObserver:
    def __init__(self, log_callback=None):
        self._log = log_callback

    def evidence_decided(self, *, result, context, profile, evaluation, reason):
        if self._log is None or getattr(profile, "command_name", None) != "equip":
            return
        observed = evaluation.projection or evaluation.failure_projection
        valid = type(observed) is EquipEffectPayload
        try:
            self._log("equip_command_evidence "
                f"request={self._token(getattr(result, 'request_id', ''))} "
                f"event={self._token(getattr(context, 'event_id', ''))} "
                f"session={self._token(getattr(context, 'session_id', ''))} "
                f"generation={self._token(getattr(context, 'generation', ''))} "
                f"status={self._token(getattr(getattr(result, 'status', None), 'value', 'unknown'))} "
                f"verified={str(evaluation.verified).lower()} reason={self._token(reason)} "
                f"observed={observed.outcome if valid else 'unavailable'} "
                f"targets={len(observed.targets) if valid else 0} "
                f"satisfied={observed.satisfied_count if valid else 0} "
                f"before_ms={observed.before_observed_at_ms if valid else 'unknown'} "
                f"after_ms={observed.after_observed_at_ms if valid else 'unknown'} "
                f"source={'minecraft_client_equipment_slots' if valid else 'unknown'}")
        except Exception:
            # No retry, state change, or alternate effect verdict on sink failure.
            pass

    @staticmethod
    def _token(value):
        return "".join(c if c.isascii() and (c.isalnum() or c in "_-.") else "_" for c in str(value))[:128]
