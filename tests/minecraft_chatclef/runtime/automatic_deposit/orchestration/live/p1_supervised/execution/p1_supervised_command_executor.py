#20260901_kpopmodder: Delegate one exact RAW StoreHome request to the existing common runner once.
from __future__ import annotations

from collections.abc import Mapping

from minecraft_chatclef.runtime.submission.command_submission_transport import (
    CommandSubmissionTransport,
)

from ..request.p1_supervised_execution_request import (
    P1SupervisedExecutionRequest,
)
from .p1_supervised_execution_dependencies import (
    P1SupervisedExecutionDependencies,
)
from .p1_supervised_execution_result import (
    P1SupervisedExecutionResult,
    seal_p1_supervised_execution_result,
)


def execute_p1_supervised_command(
    request: P1SupervisedExecutionRequest,
    runner_environment: Mapping[str, object],
    dependencies: P1SupervisedExecutionDependencies,
) -> P1SupervisedExecutionResult:
    try:
        gateway = dependencies.gateway_factory(request.gradio_url)
        common_result = dependencies.runner(
            dict(runner_environment),
            gateway,
            transport=CommandSubmissionTransport.RAW,
        )
    except Exception as error:
        return seal_p1_supervised_execution_result(
            {},
            result_error=f"{type(error).__name__}:{error}",
        )
    return seal_p1_supervised_execution_result(common_result)
