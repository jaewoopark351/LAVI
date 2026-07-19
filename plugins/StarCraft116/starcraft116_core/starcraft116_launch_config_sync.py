#20260705_kpopmodder: Added this helper to isolate StarCraft116 BWAPI launch config sync.

from core.logger import log_print
from .starcraft116_bwapi_proxy_installer import StarCraft116BWAPIProxyInstaller
from .starcraft116_bwapi_runtime_sync import StarCraft116BWAPIRuntimeSync


class StarCraft116LaunchConfigSync:
    #20260705_kpopmodder: Preserve existing exporter/bwapi.ini sync behavior outside the UI facade.
    def __init__(self, config_manager, exporter_manager, is_monster_profile, bwapi_runtime_sync=None):
        self.config_manager = config_manager
        self.exporter_manager = exporter_manager
        self.is_monster_profile = is_monster_profile
        self.bwapi_proxy_installer = StarCraft116BWAPIProxyInstaller(config_manager)
        self.bwapi_runtime_sync = bwapi_runtime_sync or StarCraft116BWAPIRuntimeSync(config_manager)

    def sync(self, profile_name):
        messages = []
        use_exporter = False
        try:
            if self.is_monster_profile(profile_name):
                #20260705_kpopmodder: Monster.exe is an external BWAPI client; keep DLL bot config untouched.
                proxy_result = self.bwapi_proxy_installer.ensure_installed(profile_name)
                if not proxy_result.get("ok"):
                    return False, proxy_result.get("message", "")
                if proxy_result.get("installed") and proxy_result.get("message"):
                    messages.append(proxy_result.get("message", ""))
                runtime_result = self.bwapi_runtime_sync.sync(profile_name)
                if not runtime_result.get("ok"):
                    return False, runtime_result.get("message", "")
                if runtime_result.get("synced") and runtime_result.get("message"):
                    messages.append(runtime_result.get("message", ""))
                messages.insert(
                    0,
                    "Monster profile uses a standalone BWAPI observer; skipped BWAPI exporter config.",
                )
                return True, "\n".join([
                    message for message in messages if message
                ]).strip()

            runtime_result = self.bwapi_runtime_sync.sync(profile_name)
            if not runtime_result.get("ok"):
                return False, runtime_result.get("message", "")
            if runtime_result.get("synced") and runtime_result.get("message"):
                messages.append(runtime_result.get("message", ""))

            #20260704_kpopmodder: Keep bwapi.ini synced before launch so BWAPI does not fall back to ExampleAIModule.dll.
            if self._should_use_exporter(profile_name):
                try:
                    exporter_ok, exporter_message = self.exporter_manager.write_ini(
                        profile_name
                    )
                    if not exporter_ok:
                        return False, exporter_message
                    if exporter_message:
                        messages.append(exporter_message)
                    use_exporter = True
                except PermissionError as e:
                    #20260704_kpopmodder: Prefer launching the bot directly when the optional event exporter ini is locked.
                    exporter_message = (
                        "Skipped LAVEventExporter.ini update because it was blocked "
                        f"by permissions: {e}"
                    )
                    messages.append(exporter_message)

            bwapi_ok, bwapi_message = self.exporter_manager.write_bwapi_ini_ai(
                profile_name,
                use_exporter=use_exporter,
            )
            if not bwapi_ok:
                return False, bwapi_message
            if bwapi_message:
                messages.append(bwapi_message)

            return True, "\n".join(messages)
        except Exception as e:
            message = f"Failed to update BWAPI launch config: {e}"
            log_print(f"[StarCraft116] {message}")
            return False, message

    def _should_use_exporter(self, profile_name):
        should_use_exporter = getattr(self.exporter_manager, "should_use_exporter", None)
        if callable(should_use_exporter):
            return bool(should_use_exporter(profile_name))
        #20260719_kpopmodder: Keep older exporter-manager shims using the legacy global toggle.
        return self.config_manager.get_bool("bwapi_event_exporter_enabled", False)
