#20260915_kpopmodder: Emit one bounded correlated verdict after the existing terminal owner decides.
from plugins.Minecraft.fabric.chatclef.result.instant import InstantCommandPayload


class InstantCommandDiagnosticObserver:
    def __init__(self, log_callback=None):
        self._log = log_callback

    def evidence_decided(self, *, result, context, profile, evaluation, reason):
        if (self._log is None or getattr(profile, "command_name", None) not in InstantCommandPayload.COMMANDS):
            return
        try:
            observed = evaluation.projection or evaluation.failure_projection
            code = observed.reason if type(observed) is InstantCommandPayload else "unverified"
            list_detail = ""
            if (type(observed) is InstantCommandPayload
                    and observed.command_name == "auto_deposit_trusted_list"
                    and observed.outcome == "completed"):
                #20260915_kpopmodder: Observe the validated snapshot, not ID text or a second rendering.
                values = observed.values
                list_detail = (
                    f" list_total={values['total']} list_validated={values['listed']}"
                    f" list_truncated={str(values['truncated']).lower()}"
                )
            self._log("instant_command_evidence "
                f"request={self._token(getattr(result, 'request_id', ''))} "
                f"event={self._token(getattr(context, 'event_id', ''))} "
                f"command={self._token(profile.command_name)} "
                f"status={self._token(getattr(getattr(result, 'status', None), 'value', 'unknown'))} "
                f"verified={str(evaluation.verified).lower()} "
                f"reason={self._token(reason)} native_reason={self._token(code)}" + list_detail)
        except Exception:
            # Logging never supplies evidence, retries, or a replacement result.
            pass

    @staticmethod
    def _token(value):
        return "".join(c if c.isascii() and (c.isalnum() or c in "_-.") else "_" for c in str(value))[:128]
