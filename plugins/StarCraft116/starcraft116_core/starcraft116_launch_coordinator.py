#20260706_kpopmodder: Keeps launch click flow in one place without changing UI callback contracts.
class StarCraft116LaunchCoordinator:
    def __init__(self, owner):
        self.owner = owner

    def sync_exporter_config(self, profile_name):
        return self.owner.launch_config_sync.sync(profile_name)

    def on_launch_click(self, profile_name):
        owner = self.owner
        owner._select_profile(profile_name, reload_config=True)
        owner.start_game_event_watcher()
        owner._refresh_process_state()
        if owner.state.running:
            owner.last_launch_message = "StarCraft 1.16 profile is already running."
            return owner._ui_values()
        owner.start(profile_name=profile_name, launch_source="manual")
        owner._write_state_log()
        return owner._ui_values(emit_status_event=True, event_source="launch")
