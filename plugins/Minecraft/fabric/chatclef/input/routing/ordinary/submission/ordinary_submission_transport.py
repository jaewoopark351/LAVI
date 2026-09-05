#20260905_kpopmodder: Isolate ordinary one-shot submission transport failures.
from __future__ import annotations


class OrdinarySubmissionTransport:
    def __init__(self, *, extension, submission_boundary, failure_handler):
        self._extension = extension
        self._submission_boundary = submission_boundary
        self._failure_handler = failure_handler

    def submit(
        self,
        *,
        event: object,
        command_text: str,
        translation: object,
    ):
        try:
            return (
                self._submission_boundary.submit_once(
                    self._extension,
                    event,
                    translation,
                    original_text=command_text,
                    translation_input_text=command_text,
                ),
                None,
            )
        except Exception as error:
            return None, self._failure_handler.decision(
                "submission_failed",
                error,
                translation,
            )


__all__ = ("OrdinarySubmissionTransport",)
