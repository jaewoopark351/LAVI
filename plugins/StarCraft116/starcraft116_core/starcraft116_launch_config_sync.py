#20260705_kpopmodder: Added this helper to isolate StarCraft116 BWAPI launch config sync.

from core.logger import log_print


class StarCraft116LaunchConfigSync:
    #20260705_kpopmodder: Preserve existing exporter/bwapi.ini sync behavior outside the UI facade.
    def __init__(self, config_manager, exporter_manager, is_monster_profile):
        self.config_manager = config_manager
        self.exporter_manager = exporter_manager
        self.is_monster_profile = is_monster_profile

    def sync(self, profile_name):
        messages = []
        use_exporter = False
        try:
            if self.is_monster_profile(profile_name):
                #20260705_kpopmodder: Monster.exe is an external BWAPI client; keep DLL bot config untouched.
                return True, "Monster profile uses a standalone BWAPI observer; skipped BWAPI exporter config."

            #20260704_kpopmodder: Keep bwapi.ini synced before launch so BWAPI does not fall back to ExampleAIModule.dll.
            if self.config_manager.get_bool("bwapi_event_exporter_enabled", False):
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
