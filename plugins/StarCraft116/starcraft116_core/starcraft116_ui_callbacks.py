#20260706_kpopmodder: Added this helper to keep StarCraft116 Gradio UI callback orchestration outside the facade.
import gradio as gr

class StarCraft116UiCallbacks:
    #20260706_kpopmodder: This helper moves callback glue only; launch/config/path behavior stays on the existing services.
    def __init__(self, owner):
        self.owner = owner

    def on_profile_change(self, profile_name):
        owner = self.owner
        owner._select_profile(profile_name, reload_config=False)
        owner.last_launch_message = (
            f"Selected StarCraft 1.16 profile: "
            f"{owner.config_manager.get_active_profile_name()}"
        )
        return owner._ui_values()

    def on_validate_paths(self, profile_name):
        owner = self.owner
        owner._select_profile(profile_name, reload_config=True)
        validation = owner.config_manager.validate_paths(profile_name=profile_name)
        owner.last_launch_message = validation.message()
        owner.start_game_event_watcher()
        return owner._ui_values()

    def on_launch_click(self, profile_name):
        owner = self.owner
        return owner._get_launch_coordinator().on_launch_click(profile_name)

    def on_refresh_click(self, profile_name):
        owner = self.owner
        owner._select_profile(profile_name, reload_config=False)
        owner._refresh_process_state()
        owner._write_state_log()
        return owner._ui_values(emit_status_event=True, event_source="refresh")

    def on_open_management_click(self, profile_name, target_key, target_type):
        owner = self.owner
        owner._select_profile(profile_name, reload_config=False)
        owner.last_launch_message = owner._open_management_target(
            target_key,
            target_type,
        )
        return owner._ui_values()

    def on_clear_tracking_click(self, profile_name):
        owner = self.owner
        owner._select_profile(profile_name, reload_config=False)
        owner.process_entries = []
        owner.state.processes = []
        owner.state.running = False
        owner.state.last_message = (
            "Cleared LAV-owned StarCraft 1.16 launch tracking."
        )
        owner.last_launch_message = (
            "Cleared LAV-owned launch tracking. External status remains read-only."
        )
        owner._write_state_log()
        return owner._ui_values()

    def on_scan_install_click(self, install_dir):
        owner = self.owner
        discovery = owner.config_manager.discover_install(install_dir)
        owner.last_discovery = discovery.to_dict()
        owner.last_setup_message = discovery.message()
        return (
            owner.last_setup_message,
            owner._discovery_json(),
        )

    def on_generate_config_click(self, install_dir):
        owner = self.owner
        discovery = owner.config_manager.write_config_from_install(install_dir)
        owner.last_discovery = discovery.to_dict()
        owner.state.profile = owner.config_manager.get_active_profile_name()
        owner.last_setup_message = (
            f"Config generated: {owner.config_manager.config_path}\n"
            f"{owner.config_manager.config_message()}\n"
            f"{discovery.message()}"
        )
        owner.start_game_event_watcher()
        profile_choices = owner.config_manager.profile_dropdown_choices()
        return (
            owner.last_setup_message,
            owner._discovery_json(),
            owner.config_manager.config_message(),
            gr.update(
                choices=profile_choices,
                value=owner.config_manager.get_active_profile_name(),
            ),
        )
