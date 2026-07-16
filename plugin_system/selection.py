#20260622_kpopmodder: Canonical plugin selection base implementation.
import gradio as gr

from core.config_manager import config_manager#20260627_kpopmodder
from core.logger import log_print  #20260612_kpopmodder
from plugin_system.loader import plugin_loader


class PluginStartupError(RuntimeError):
    #20260716_kpopmodder: Raise readable startup errors when required categories have no usable provider.
    pass


class Provider():
    #20260630_kpopmodder: Track lazy init state per provider so startup does not load every backend.
    def __init__(self):
        self.plugin = None
        self.handle = None
        self.name = ""
        self.ui = None
        self.initialized = False
        self.disabled = False
        self.init_error = ""


# place holder until config saving is implemented
#temp_default = ["Local_EN_to_JA", "voicevox", "VoiceInput","RanaLLM"]no_translate
temp_default = ["NoTranslate", "gpt_sovits", "VoiceInput","AyaLLM"]

#20260627_kpopmodder: Keep LLM default explicit when ChatGPT_OpenAI and Hybrid_OpenAI_LLM are both enabled.
CATEGORY_DEFAULT_PROVIDERS = {
    "language_model": "Hybrid_OpenAI_LLM",
}


def select_default_provider(providers, category_name, configured_default_name=""):
    providers = list(providers or [])
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
    def __init__(self, plugin_type) -> None:
        # load plugin
        self.provider_list = []
        self.plugin_type = plugin_type
        self.category_name = plugin_loader.interface_to_category[self.plugin_type]
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
        self.provider_dropdown = gr.Dropdown(

            choices=[provider.name for provider in self.provider_list],
            value=self.default_provider.name,
            type="value",
            label="Provider: ",
            info="",
            interactive=True)
        self.provider_dropdown.change(
            self.on_dropdown_change, inputs=self.provider_dropdown)

    # Creates the custom UI from each plugin
    def create_plugin_ui(self):
        for provider in self.provider_list:
            plugin = self._ensure_provider_constructed(provider)
            if plugin is None:
                continue
            try:
                provider.ui = plugin.create_ui()
            except Exception as e:
                #20260630_kpopmodder: UI failure disables only this provider.
                provider.disabled = True
                provider.init_error = str(e)
                log_print(
                    f"[PluginSelection] provider UI disabled: "
                    f"{provider.name}: {e}"
                )

    def on_dropdown_change(self, provider_name):
        provider = self.find_provider_by_name(self.provider_list, provider_name)
        if provider is None:
            log_print(f"[PluginSelection] provider not found: {provider_name}")
            return
        if not self.load_provider(provider.name):
            #20260630_kpopmodder: Lazy-init failures keep the previous active provider.
            log_print(
                f"[PluginSelection] provider unavailable: {provider.name}"
            )
            return

        self.current_provider = provider
        self.current_plugin = provider.plugin

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
            log_print(
                f"[PluginSelection] provider disabled: "
                f"{found_provider.name}: {found_provider.init_error}"
            )
            return False

        try:
            found_provider.plugin.init()
        except Exception as e:
            #20260630_kpopmodder: Init crash is contained to this provider.
            found_provider.disabled = True
            found_provider.init_error = str(e)
            log_print(
                f"[PluginSelection] provider init failed; disabled "
                f"{found_provider.name}: {e}"
            )
            return False

        start = getattr(found_provider.plugin, "start", None)
        if callable(start):
            try:
                start()
            except Exception as e:
                found_provider.disabled = True
                found_provider.init_error = str(e)
                log_print(
                    f"[PluginSelection] provider start failed; disabled "
                    f"{found_provider.name}: {e}"
                )
                return False
        found_provider.initialized = True
        provider_handle = getattr(found_provider, "handle", None)
        if provider_handle is not None:
            provider_handle.mark_running()
        log_print(
            f"[PluginSelection] provider initialized: {found_provider.name}"
        )
        return True

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
        return provider.plugin

    def _activate_first_available_provider(self, exclude_name=""):
        for provider in self.provider_list:
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

    def shutdown(self):
        for provider in list(self.provider_list):
            if not getattr(provider, "initialized", False):
                #20260630_kpopmodder: Lazy providers that never loaded have nothing to shut down.
                continue
            plugin = getattr(provider, "plugin", None)
            shutdown = getattr(plugin, "shutdown", None)
            if not callable(shutdown):
                continue

            try:
                shutdown()
            except Exception as e:
                log_print(
                    f"[PluginSelection] shutdown failed: "
                    f"{provider.name}: {e}"
                )
