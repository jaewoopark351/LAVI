#20260818_kpopmodder: Aggregate observed automatic batch action counts.
from __future__ import annotations


def aggregate_automatic_counts(
    batch_result: dict[str, object],
    *,
    resubmit_count: object,
    rerun_count: object,
) -> None:
    _aggregate_count(batch_result, "automatic_resubmit_count", resubmit_count)
    _aggregate_count(batch_result, "automatic_rerun_count", rerun_count)
    batch_result["automatic_retry_count"] = batch_result[
        "automatic_resubmit_count"
    ]
    batch_result["automatic_replay_count"] = batch_result[
        "automatic_rerun_count"
    ]


def _aggregate_count(
    batch_result: dict[str, object],
    field: str,
    observed: object,
) -> None:
    if type(observed) is not int or observed < 0:
        return
    current = batch_result.get(field)
    if type(current) is not int or current < 0:
        raise TypeError(f"batch result count is invalid: {field}")
    batch_result[field] = current + observed
