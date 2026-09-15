#20260915_kpopmodder: Read the existing authoritative connection snapshot; never infer missing identity.
from collections.abc import Mapping


class KoreanConfirmationSessionReader:
    def __init__(self, *, extension, submission_precheck):
        self._extension = extension
        self._precheck = submission_precheck

    def inspect(self):
        readiness = self._precheck.inspect(self._extension)
        if readiness.ready is not True:
            return None, readiness.reason
        details = readiness.status.get("details")
        commands = details.get("commands") if isinstance(details, Mapping) else None
        if not isinstance(commands, Mapping):
            return None, "confirmation_session_unavailable"
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
            return None, "confirmation_session_unavailable"
        return (session, generation), "ready"
