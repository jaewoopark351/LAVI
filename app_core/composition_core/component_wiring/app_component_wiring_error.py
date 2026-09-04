#20260905_kpopmodder: Provides a typed fail-closed error for required app component wiring.


class AppComponentWiringError(RuntimeError):
    def __init__(self, stage: str, detail: str):
        self.stage = stage
        self.detail = detail
        super().__init__(f"App component wiring failed at {stage}: {detail}")


__all__ = ["AppComponentWiringError"]
