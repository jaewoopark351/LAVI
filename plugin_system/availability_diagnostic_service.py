#20260718_kpopmodder: Added this module to keep availability diagnostics shared across plugin loaders.
from plugin_system.availability_probe_service import AvailabilityProbeService
from plugin_system.contracts import PluginDiagnostic, PluginState


class AvailabilityDiagnosticService:
    def __init__(self, probe_service=None):
        self.probe_service = probe_service or AvailabilityProbeService()

    def build_diagnostic(
        self,
        *,
        plugin_id,
        display_name,
        availability_probe,
        resolve_file,
        dependency_group="",
        suggested_command="",
        log_reference="",
        extra_missing_files=(),
    ):
        missing_packages = self.probe_service.missing_python_packages(
            availability_probe.required_python_packages,
        )
        missing_files = self.probe_service.missing_files(
            availability_probe.required_files,
            resolve_file,
        )
        missing_executables = self.probe_service.missing_executables(
            availability_probe.required_executables,
        )
        missing_services = self.probe_service.missing_services(
            availability_probe.required_services,
            timeout_sec=availability_probe.timeout_sec,
        )
        all_missing_files = tuple(missing_files) + tuple(extra_missing_files or ())

        if not (
            missing_packages
            or all_missing_files
            or missing_executables
            or missing_services
        ):
            return None

        reason_code, message = self._reason_and_message(
            display_name=display_name,
            missing_packages=missing_packages,
            missing_files=all_missing_files,
            missing_executables=missing_executables,
            missing_services=missing_services,
            extra_missing_files=extra_missing_files,
        )
        return PluginDiagnostic(
            plugin_id=plugin_id,
            state=PluginState.UNAVAILABLE,
            reason_code=reason_code,
            human_readable_message=message,
            missing_python_packages=tuple(missing_packages),
            missing_files=tuple(all_missing_files),
            missing_executables=tuple(missing_executables),
            missing_services=tuple(missing_services),
            suggested_install_profile=dependency_group,
            suggested_command=suggested_command,
            log_reference=log_reference,
        )

    def _reason_and_message(
        self,
        *,
        display_name,
        missing_packages,
        missing_files,
        missing_executables,
        missing_services,
        extra_missing_files,
    ):
        if extra_missing_files:
            return (
                "missing_model_configuration",
                (
                    f"{display_name} is enabled but selected model files "
                    "or model configuration are missing."
                ),
            )
        if missing_services and not (
            missing_packages or missing_files or missing_executables
        ):
            return (
                "required_service_unavailable",
                (
                    f"{display_name} is enabled but a required local service "
                    "or device probe failed."
                ),
            )
        return (
            "missing_static_dependency",
            (
                f"{display_name} is enabled but required Python packages, "
                "files, executables, services, or model files are unavailable."
            ),
        )
