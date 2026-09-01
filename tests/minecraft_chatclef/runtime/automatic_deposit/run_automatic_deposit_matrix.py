#20260831_kpopmodder: Provide a non-mutating fail-closed automatic-deposit CLI boundary.
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

if __package__:
    from ..preflight.runtime_environment import load_live_runtime_environment
    from .orchestration.live_matrix_application import (
        run_automatic_deposit_live_matrix_application,
    )
else:
    tests_root = Path(__file__).resolve().parents[3]
    repository_root = Path(__file__).resolve().parents[4]
    sys.path[:0] = [str(repository_root), str(tests_root)]
    from minecraft_chatclef.runtime.automatic_deposit.orchestration import (
        run_automatic_deposit_live_matrix_application,
    )
    from minecraft_chatclef.runtime.preflight.runtime_environment import (
        load_live_runtime_environment,
    )


def main() -> int:
    environment = load_live_runtime_environment()
    result = run_automatic_deposit_live_matrix_application(
        environment,
        row_id=os.environ.get("LAVI_MINECRAFT_AUTOMATIC_DEPOSIT_ROW_ID", ""),
    )
    print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
