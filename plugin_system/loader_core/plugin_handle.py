#20260717_kpopmodder: Added this module to keep one project class per Python file.
from core.logger import log_print
from plugin_system.contracts_core.contract_helpers import validate_plugin_lifecycle
from plugin_system.contracts_core.plugin_diagnostic import PluginDiagnostic
from plugin_system.contracts_core.plugin_diagnostic_snapshot import (
    PluginDiagnosticSnapshot,
)
from plugin_system.contracts_core.plugin_state import PluginState
from plugin_system.loader_core.plugin_load_error import PluginLoadError


class PluginHandle:
    #20260716_kpopmodder: Lazy provider handle; imports and constructs only when selected.
    def __init__(self, descriptor, loader, status=PluginState.READY, diagnostic=None):
        self.descriptor = descriptor
        self.loader = loader
        self.instance = None
        self.status = status
        self.error = ""
        self.diagnostic = diagnostic

    @property
    def name(self):
        return self.descriptor.class_name

    @property
    def runtime_contract(self):
        return self.descriptor.runtime_contract

    def diagnostic_snapshot(self):
        return PluginDiagnosticSnapshot(
            plugin_id=self.descriptor.id,
            name=self.name,
            category=self.descriptor.category,
            state=self.status,
            detail=self.error,
            diagnostic=self.diagnostic,
            runtime_contract=self.runtime_contract,
        ).to_dict()

    def _set_state(self, state, detail="", diagnostic=None):
        self.status = state
        self.error = str(detail or "")
        self.diagnostic = diagnostic
        self.loader.set_plugin_status(
            self.descriptor.status_key,
            state,
            detail=detail,
            diagnostic=diagnostic.to_dict() if diagnostic else None,
            runtime_contract=self.runtime_contract.to_dict(),
        )

    def check_availability(self):
        if self.status in (PluginState.UNAVAILABLE, PluginState.FAILED):
            return False
        return self.refresh_availability()

    def refresh_availability(self, force=False):
        #20260718_kpopmodder: Allow explicit UNAVAILABLE re-probes while keeping FAILED sticky.
        if self.status == PluginState.FAILED:
            return False
        if self.status == PluginState.UNAVAILABLE and not force:
            return False

        diagnostic = self.loader.availability_diagnostic(self.descriptor)
        if diagnostic is None:
            self._set_state(PluginState.READY)
            return True

        self._set_state(
            PluginState.UNAVAILABLE,
            detail=diagnostic.human_readable_message,
            diagnostic=diagnostic,
        )
        log_print(
            "[PluginLoader] plugin unavailable; "
            f"skipped {self.descriptor.status_key}: "
            f"{diagnostic.human_readable_message}"
        )#20260716_kpopmodder
        return False

    def construct(self, expected_interface=None):
        if self.instance is not None:
            return self.instance
        if self.status in (PluginState.UNAVAILABLE, PluginState.FAILED):
            return None
        if not self.check_availability():
            return None

        try:
            module = self.loader.import_descriptor_module(self.descriptor)
            plugin_class = getattr(module, self.descriptor.class_name)
            if not isinstance(plugin_class, type):
                raise PluginLoadError(
                    f"{self.descriptor.class_name} is not a class"
                )
            if plugin_class.__module__ != module.__name__:
                raise PluginLoadError(
                    f"{self.descriptor.class_name} is imported, not defined in {module.__name__}"
                )
            if expected_interface is None:
                expected_interface = self.loader.interface_name_to_class[
                    self.descriptor.interface_name
                ]
            if not issubclass(plugin_class, expected_interface):
                raise PluginLoadError(
                    f"{self.descriptor.class_name} does not implement {expected_interface.__name__}"
                )
            self.instance = plugin_class()
            lifecycle_issues = validate_plugin_lifecycle(
                self.instance,
                plugin_id=self.descriptor.id,
            )
            if lifecycle_issues:
                diagnostic = PluginDiagnostic(
                    plugin_id=self.descriptor.id,
                    state=PluginState.FAILED,
                    reason_code="lifecycle_contract_failed",
                    human_readable_message="; ".join(
                        issue.message for issue in lifecycle_issues
                    ),
                    log_reference=(
                        "PluginLoader lifecycle validation for "
                        f"{self.descriptor.status_key}"
                    ),
                )
                self.instance = None
                self._set_state(
                    PluginState.FAILED,
                    detail=diagnostic.human_readable_message,
                    diagnostic=diagnostic,
                )
                return None
            self._set_state(PluginState.READY)
            return self.instance
        except Exception as e:
            self.mark_failed(e, reason_code="construct_failed")
            log_print(
                "[PluginLoader] plugin construct failed; "
                f"skipped {self.descriptor.status_key}: {e}"
            )#20260716_kpopmodder
            return None

    def mark_starting(self):
        if self.status == PluginState.FAILED:
            return
        self._set_state(PluginState.STARTING)

    def mark_running(self):
        if self.status == PluginState.FAILED:
            return
        self._set_state(PluginState.RUNNING)

    def mark_failed(self, error, reason_code="plugin_failed"):
        diagnostic = PluginDiagnostic(
            plugin_id=self.descriptor.id,
            state=PluginState.FAILED,
            reason_code=reason_code,
            human_readable_message=str(error),
        )
        self._set_state(PluginState.FAILED, detail=error, diagnostic=diagnostic)

    def mark_stopped(self):
        if self.status == PluginState.FAILED:
            return
        self._set_state(PluginState.STOPPED)
