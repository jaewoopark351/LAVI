#20260818_kpopmodder: Provide one explicit CLI entrypoint for the supervised GET batch.
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

if __package__:
    from ..preflight.runtime_environment import load_live_runtime_environment
    from .live_batch_application import run_live_get_batch_application
else:
    tests_root = Path(__file__).resolve().parents[3]
    sys.path.insert(0, str(tests_root))
    from minecraft_chatclef.runtime.batch.live_batch_application import (
        run_live_get_batch_application,
    )
    from minecraft_chatclef.runtime.preflight.runtime_environment import (
        load_live_runtime_environment,
    )


BATCH_APPROVAL_ENVIRONMENT_VARIABLE = (
    "LAVI_MINECRAFT_RUNTIME_BATCH_APPROVAL_JSON"
)


def main() -> int:
    environment = load_live_runtime_environment()
    result = run_live_get_batch_application(
        environment,
        os.environ.get(BATCH_APPROVAL_ENVIRONMENT_VARIABLE),
    )
    print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
    return 0 if result.get("status") in {"completed", "skipped"} else 1


if __name__ == "__main__":
    raise SystemExit(main())
