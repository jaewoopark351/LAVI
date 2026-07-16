#20260716_kpopmodder: Allow python -m lavi without moving the existing main.py entrypoint.
from lavi.cli import main


if __name__ == "__main__":
    raise SystemExit(main())
