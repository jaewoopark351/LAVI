#20260901_kpopmodder: Validate one approved endpoint kind without owning submission.
from __future__ import annotations

from ...submission.command_submission_transport import (
    CommandSubmissionTransport,
)


_APPROVED_TRANSPORT_VALUES = frozenset(
    transport.value for transport in CommandSubmissionTransport
)


def validate_approved_command_transport(
    approved_transport: object,
    selected_transport: object,
) -> str:
    if (
        type(approved_transport) is not str
        or approved_transport not in _APPROVED_TRANSPORT_VALUES
    ):
        return "approved command transport is invalid"
    if type(selected_transport) is not CommandSubmissionTransport:
        return "selected command transport is invalid"
    if selected_transport.value != approved_transport:
        return "approved command transport does not match selected transport"
    return ""
