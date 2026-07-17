#20260717_kpopmodder: Added this module to keep one project class per Python file.
class PluginState:
    #20260716_kpopmodder: Canonical lifecycle state names for discovered providers.
    DISABLED = "DISABLED"
    UNAVAILABLE = "UNAVAILABLE"
    READY = "READY"
    STARTING = "STARTING"
    RUNNING = "RUNNING"
    FAILED = "FAILED"
    STOPPED = "STOPPED"
    BROKEN = FAILED  # Backward-compatible alias for older tests/log checks.
