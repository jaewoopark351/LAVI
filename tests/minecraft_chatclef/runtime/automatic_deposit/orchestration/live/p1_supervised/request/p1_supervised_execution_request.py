#20260901_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from dataclasses import dataclass

from minecraft_chatclef.runtime.submission.command_submission_transport import (
    CommandSubmissionTransport,
)


@dataclass(frozen=True, slots=True)
class P1SupervisedExecutionRequest:
    command: str
    transport: CommandSubmissionTransport
    invocation_id: str
    approval_json: str
    expected_run_manifest_id: str
    expected_candidate_position: str
    gradio_url: str
    expected_backend: str
    expected_instance: str
    expected_world: str
    repository_root: str
