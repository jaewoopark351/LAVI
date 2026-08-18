#20260818_kpopmodder: Parse only the operator-supplied batch approval JSON boundary.
from __future__ import annotations

import json
from collections.abc import Mapping


def parse_batch_approval_record(raw_json: object) -> tuple[dict[str, object], str]:
    if not isinstance(raw_json, str) or not raw_json.strip():
        return {}, "batch approval record is required"
    try:
        payload = json.loads(raw_json)
    except (TypeError, ValueError, json.JSONDecodeError) as error:
        return {}, f"batch approval record is invalid JSON: {type(error).__name__}"
    if not isinstance(payload, Mapping):
        return {}, "batch approval record must be a JSON object"
    return dict(payload), ""
