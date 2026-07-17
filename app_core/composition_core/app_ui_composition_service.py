#20260718_kpopmodder: Keep UI component creation order outside AppComposer.


class AppUiCompositionService:
    #20260718_kpopmodder: Owns runtime UI cross-wiring while AppComposer keeps assembly order.
    def create_component_ui(
        self,
        *,
        input_component,
        llm,
        translate,
        tts,
        vtuber,
        song_player=None,
        chess_plugin=None,
        starcraft_plugin=None,
        starcraft116_plugin=None,
        starcraft2_plugin=None,
        screen_vision=None,
    ):
        import gradio as gr
        from audio_device_manager import audio_device_manager

        input_component.create_ui()
        llm.create_ui()
        translate.create_ui()
        tts.create_ui()
        if song_player is not None:
            song_player.create_ui()
        if chess_plugin is not None:
            chess_plugin.create_ui()
        if starcraft_plugin is not None:
            starcraft_plugin.create_ui()
        if starcraft116_plugin is not None:
            starcraft116_plugin.create_ui()
        if starcraft2_plugin is not None:
            starcraft2_plugin.create_ui()
        with gr.Tab("Setting"):
            with gr.Tabs():
                vtuber.create_ui()
                audio_device_manager.create_ui()
                if screen_vision is not None:
                    screen_vision.create_ui()
