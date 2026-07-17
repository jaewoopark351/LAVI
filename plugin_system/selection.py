#20260622_kpopmodder: Canonical plugin selection base implementation.
import gradio as gr

from core.config_manager import config_manager#20260627_kpopmodder
from core.logger import log_print  #20260612_kpopmodder
from plugin_system.contracts import (
    PluginRuntimeRequirements,
    PluginSelectionSnapshotDTO,
    ProviderDiagnosticDTO,
)
from plugin_system.loader import plugin_loader
from plugin_system.selection_core.plugin_startup_error import PluginStartupError
from plugin_system.selection_core.provider import Provider
from plugin_system.selection_core.selection_requirements import (
    _coerce_runtime_requirements,
)


# place holder until config saving is implemented
#temp_default = ["Local_EN_to_JA", "voicevox", "VoiceInput","RanaLLM"]no_translate
temp_default = ["NoTranslate", "gpt_sovits", "VoiceInput","AyaLLM"]

#20260627_kpopmodder: Keep LLM default explicit when ChatGPT_OpenAI and Hybrid_OpenAI_LLM are both enabled.
CATEGORY_DEFAULT_PROVIDERS = {
    "language_model": "Hybrid_OpenAI_LLM",
}


def _provider_matches_requirements(provider, requirements):
    requirements = _coerce_runtime_requirements(requirements)
    if requirements.is_empty():
        return True
    matcher = getattr(provider, "matches_requirements", None)
    if callable(matcher):
        return matcher(requirements)
    return False


def select_default_provider(
    providers,
    category_name,
    configured_default_name="",
    runtime_requirements=None,
):
    providers = [
        provider
        for provider in list(providers or [])
        if _provider_matches_requirements(provider, runtime_requirements)
    ]
    empty_provider = Provider()
    if not providers:
        return empty_provider, "empty"

    preferred_names = [
        (
            str(configured_default_name or "").strip(),
            "config",
        ),
    ]
    preferred_names.extend((name, "legacy") for name in temp_default)
    preferred_names.append((
        CATEGORY_DEFAULT_PROVIDERS.get(category_name, ""),
        "builtin",
    ))

    for preferred_name, source in preferred_names:
        if not preferred_name:
            continue
        for provider in providers:
            if provider.name == preferred_name:
                return provider, source

    return providers[0], "first_available"


class PluginSelectionBase():#20260622_kpopmodder
    def __init__(self, plugin_type, runtime_requirements=None) -> None:
        # load plugin
        self.provider_list = []
        self.plugin_type = plugin_type
        self.category_name = plugin_loader.interface_to_category[self.plugin_type]
        self.runtime_requirements = _coerce_runtime_requirements(runtime_requirements)
        for plugin_entry in plugin_loader.plugins[self.category_name]:
            provider = Provider()
            if hasattr(plugin_entry, "construct"):
                provider.handle = plugin_entry
                provider.name = plugin_entry.name
            else:
                provider.plugin = plugin_entry
                provider.name = plugin_entry.__class__.__name__
            self.provider_list.append(provider)

        self.default_provider, self.default_provider_source = (
            select_default_provider(
                self.provider_list,
                self.category_name,
                self._configured_default_provider_name(),
                runtime_requirements=self.runtime_requirements,
            )
        )
        self.current_provider = None
        self.current_plugin = None
        self._log_selected_provider()

        #20260630_kpopmodder: Only the selected provider is initialized at startup; others are lazy.
        if self.load_provider(self.default_provider.name):
            self.current_provider = self.default_provider
            self.current_plugin = self.default_provider.plugin
        else:
            self._activate_first_available_provider(exclude_name=self.default_provider.name)

        if self.current_plugin is None:
            available_names = [provider.name for provider in self.provider_list]
            raise PluginStartupError(
                "No usable provider for required plugin category "
                f"{self.category_name}. available={available_names}"
            )

    # Creates the dropdown menu for selecting current plugin
    def create_plugin_selection_ui(self):
        current_provider_name = (
            self.current_provider.name
            if self.current_provider is not None
            else self.default_provider.name
        )
        self.provider_dropdown = gr.Dropdown(

            choices=[provider.name for provider in self.provider_list],
            value=current_provider_name,
            type="value",
            label="Provider: ",
            info="",
            interactive=True)
        self.provider_dropdown.change(
            self.on_dropdown_change,
            inputs=self.provider_dropdown,
            outputs=self.provider_dropdown,
        )

    # Creates the custom UI from each plugin
    def create_plugin_ui(self):
        #20260716_kpopmodder: P1-B keeps provider listing metadata-only; unselected providers stay unconstructed.
        self._ensure_provider_ui_created(self.current_provider)

    def create_all_provider_ui(self):
        #20260716_kpopmodder: Input providers can be simultaneous sources, so keep their legacy panels visible.
        for provider in self.provider_list:
            plugin = self._ensure_provider_constructed(provider)
            if plugin is None:
                continue
            self._create_provider_ui(provider, plugin)

    def _ensure_provider_ui_created(self, provider):
        if provider is None or provider.disabled:
            return None
        if provider.ui is not None:
            return provider.ui
        if not self.load_provider(provider.name):
            return None
        plugin = provider.plugin
        return self._create_provider_ui(provider, plugin)

    def _create_provider_ui(self, provider, plugin):
        if provider.ui is not None:
            return provider.ui
        try:
            provider.ui = plugin.create_ui()
            return provider.ui
        except Exception as e:
            #20260630_kpopmodder: UI failure disables only this provider.
            provider.disabled = True
            provider.init_error = str(e)
            self._mark_provider_handle(
                provider,
                "mark_failed",
                e,
                reason_code="ui_failed",
            )
            log_print(
                f"[PluginSelection] provider UI disabled: "
                f"{provider.name}: {e}"
            )
            return None

    def on_dropdown_change(self, provider_name):
        provider = self.find_provider_by_name(self.provider_list, provider_name)
        if provider is None:
            log_print(f"[PluginSelection] provider not found: {provider_name}")
            return self._current_provider_name()
        if not self.load_provider(provider.name):
            #20260630_kpopmodder: Lazy-init failures keep the previous active provider.
            log_print(
                f"[PluginSelection] provider unavailable: {provider.name}"
            )
            return self._current_provider_name()

        self.current_provider = provider
        self.current_plugin = provider.plugin
        return self._current_provider_name()

    def load_provider(self, provider_name):
        # log_print(f"Loading {self.plugin_type} Module...")#20260612_kpopmodder
        # log_print(f"Looking for {provider_name} in installed plugins...")#20260612_kpopmodder
        found_provider = self.find_provider_by_name(
            self.provider_list, provider_name)
        if found_provider is None:
            log_print(f"[PluginSelection] provider not found: {provider_name}")
            return False
        if found_provider.disabled:
            return False
        #20260717_kpopmodder
        if not self._provider_matches_runtime_requirements(found_provider):
            found_provider.init_error = "provider does not meet runtime requirements"
            log_print(
                f"[PluginSelection] provider skipped by runtime requirements: "
                f"{found_provider.name}: {self.runtime_requirements.to_dict()}"
            )
            return False
        if found_provider.initialized:
            return True
        # log_print(f"Found {found_provider} .")#20260612_kpopmodder
        plugin = self._ensure_provider_constructed(found_provider)
        if plugin is None:
            return False

        if not issubclass(type(found_provider.plugin), self.plugin_type):
            #20260630_kpopmodder: Bad provider contract disables only the provider.
            found_provider.disabled = True
            found_provider.init_error = "plugin does not implement interface"
            self._cleanup_failed_provider(found_provider)
            log_print(
                f"[PluginSelection] provider disabled: "
                f"{found_provider.name}: {found_provider.init_error}"
            )
            return False

        try:
            self._mark_provider_handle(found_provider, "mark_starting")
            found_provider.needs_shutdown = True
            found_provider.plugin.init()
        except Exception as e:
            #20260630_kpopmodder: Init crash is contained to this provider.
            found_provider.disabled = True
            found_provider.init_error = str(e)
            self._mark_provider_handle(
                found_provider,
                "mark_failed",
                e,
                reason_code="init_failed",
            )
            log_print(
                f"[PluginSelection] provider init failed; disabled "
                f"{found_provider.name}: {e}"
            )
            self._cleanup_failed_provider(found_provider)
            return False

        start = getattr(found_provider.plugin, "start", None)
        if callable(start):
            try:
                start()
            except Exception as e:
                found_provider.disabled = True
                found_provider.init_error = str(e)
                self._mark_provider_handle(
                    found_provider,
                    "mark_failed",
                    e,
                    reason_code="start_failed",
                )
                log_print(
                    f"[PluginSelection] provider start failed; disabled "
                    f"{found_provider.name}: {e}"
                )
                self._cleanup_failed_provider(found_provider)
                return False
        found_provider.initialized = True
        self._mark_provider_handle(found_provider, "mark_running")
        log_print(
            f"[PluginSelection] provider initialized: {found_provider.name}"
        )
        return True

    def _mark_provider_handle(self, provider, method_name, *args, **kwargs):
        handle = getattr(provider, "handle", None)
        method = getattr(handle, method_name, None)
        if not callable(method):
            return
        method(*args, **kwargs)

    def _ensure_provider_constructed(self, provider):
        if provider.plugin is not None:
            return provider.plugin
        if provider.handle is None:
            provider.disabled = True
            provider.init_error = "provider has no plugin handle"
            return None

        plugin = provider.handle.construct(self.plugin_type)
        if plugin is None:
            provider.disabled = True
            provider.init_error = provider.handle.error or "plugin construct failed"
            return None
        provider.plugin = plugin
        provider.needs_shutdown = True
        return provider.plugin

    def _activate_first_available_provider(self, exclude_name=""):
        for provider in self.providers_matching(self.runtime_requirements):
            if provider.name == exclude_name:
                continue
            if not self.load_provider(provider.name):
                continue
            self.current_provider = provider
            self.current_plugin = provider.plugin
            log_print(
                "[PluginSelection] fallback provider selected: "
                f"{self.category_name} -> {provider.name}"
            )#20260716_kpopmodder
            return True
        return False

    # Gradio doesn't support dynamic showing/hiding of elements
    # def hide_other_ui(self, provider_name):
    #     for provider in self.provider_list:
    #         print(
    #             f"updating {provider.name} to {provider.name == provider_name}")
    #         provider.ui = provider.name == provider_name
    #     return [provider.ui for provider in self.provider_list]

    def find_provider_by_name(self, providers, name):
        for provider in providers:
            if provider.name == name:
                return provider
        return None

    def get_current_plugin(self):
        return self.current_plugin

    #20260717_kpopmodder
    def get_provider_contracts(self):
        return {
            provider.name: provider.runtime_contract_dict()
            for provider in self.provider_list
        }

    def get_provider_diagnostics(self):
        return [
            diagnostic.to_dict()
            for diagnostic in self.provider_diagnostics()
        ]

    def provider_diagnostics(self):
        diagnostics = []
        for provider in self.provider_list:
            diagnostics.append(self._provider_diagnostic_dto(provider))
        return diagnostics

    def _provider_diagnostic_dto(self, provider):
        #20260718_kpopmodder: Keep provider diagnostics typed until the legacy UI/API edge.
        handle = getattr(provider, "handle", None)
        diagnostic_snapshot = getattr(handle, "diagnostic_snapshot", None)
        if callable(diagnostic_snapshot):
            snapshot = diagnostic_snapshot()
        else:
            snapshot = {
                "plugin_id": provider.name,
                "name": provider.name,
                "category": self.category_name,
                "state": "RUNNING" if provider.initialized else "READY",
                "detail": provider.init_error,
                "diagnostic": {},
                "runtime_contract": provider.runtime_contract_dict(),
            }
        plugin_diagnostic = self._plugin_diagnostic_dict(provider)
        if plugin_diagnostic:
            snapshot["diagnostic"] = plugin_diagnostic
        return ProviderDiagnosticDTO.from_snapshot(
            snapshot,
            selected=provider is self.current_provider,
            initialized=bool(provider.initialized),
            disabled=bool(provider.disabled),
        )

    def selection_snapshot(self):
        return PluginSelectionSnapshotDTO(
            category=self.category_name,
            selected_provider=self._current_provider_name(),
            default_provider=self.default_provider.name,
            default_provider_source=self.default_provider_source,
            available_providers=tuple(
                provider.name for provider in self.provider_list
            ),
            provider_diagnostics=tuple(self.provider_diagnostics()),
            runtime_requirements=self.runtime_requirements,
        )

    def get_selection_snapshot(self):
        return self.selection_snapshot().to_dict()

    def _plugin_diagnostic_dict(self, provider):
        #20260717_kpopmodder: Prefer provider-owned diagnostics after construction without forcing lazy providers to load.
        plugin = getattr(provider, "plugin", None)
        diagnostics = getattr(plugin, "diagnostics", None)
        if not callable(diagnostics):
            return {}
        try:
            value = diagnostics()
        except Exception as e:
            return {
                "reason_code": "diagnostics_failed",
                "human_readable_message": str(e),
            }
        if hasattr(value, "to_dict"):
            return value.to_dict()
        if isinstance(value, dict):
            return dict(value)
        return {"human_readable_message": str(value)}

    def providers_matching(
        self,
        runtime_requirements=None,
        required_capabilities=(),
        supports_offline=None,
        supports_cpu=None,
        requires_gpu=None,
    ):
        requirements = _coerce_runtime_requirements(
            runtime_requirements,
            required_capabilities=required_capabilities,
            supports_offline=supports_offline,
            supports_cpu=supports_cpu,
            requires_gpu=requires_gpu,
        )
        return [
            provider
            for provider in self.provider_list
            if _provider_matches_requirements(provider, requirements)
        ]

    def _provider_matches_runtime_requirements(self, provider):
        requirements = getattr(
            self,
            "runtime_requirements",
            PluginRuntimeRequirements(),
        )
        return _provider_matches_requirements(provider, requirements)

    def _current_provider_name(self):
        if self.current_provider is not None:
            return self.current_provider.name
        return self.default_provider.name

    def _configured_default_provider_name(self):
        config = config_manager.load_section("PluginSelection")
        return (
            config.get(f"default_{self.category_name}_provider")
            or config.get(f"{self.category_name}_default_provider")
            or config.get("default_provider")
            or ""
        )

    def _log_selected_provider(self):
        names = [provider.name for provider in self.provider_list]
        log_print(
            "[PluginSelection] "
            f"category={self.category_name} "
            f"default_provider={self.default_provider.name} "
            f"source={self.default_provider_source} "
            f"available={names}"
        )

    def _cleanup_failed_provider(self, provider):
        #20260716_kpopmodder: Roll back partial init/start failures without blocking fallback provider selection.
        self._shutdown_provider(provider, failed_cleanup=True)

    def _shutdown_provider(self, provider, failed_cleanup=False):
        if getattr(provider, "shutdown_attempted", False):
            return
        plugin = getattr(provider, "plugin", None)
        if plugin is None:
            provider.shutdown_attempted = True
            return

        context = "cleanup" if failed_cleanup else "shutdown"
        seen_methods = set()
        for method_name in ("stop", "shutdown", "cleanup"):
            cleanup = getattr(plugin, method_name, None)
            if not callable(cleanup):
                continue
            method_key = id(cleanup)
            if method_key in seen_methods:
                continue
            seen_methods.add(method_key)
            try:
                cleanup()
            except Exception as e:
                log_print(
                    f"[PluginSelection] {context} {method_name} failed: "
                    f"{provider.name}: {e}"
                )
        provider.shutdown_attempted = True
        if not failed_cleanup:
            self._mark_provider_handle(provider, "mark_stopped")

    def shutdown(self):
        for provider in list(self.provider_list):
            if not (
                getattr(provider, "initialized", False)
                or getattr(provider, "needs_shutdown", False)
            ):
                #20260630_kpopmodder: Lazy providers that never loaded have nothing to shut down.
                continue
            self._shutdown_provider(provider)
