#20260915_kpopmodder: Bound correlation and lengths without logging display IDs or spoken content.
class RoutedResponseTextProjectionFormatter:
    @staticmethod
    def format(request):
        def atom(value):
            if type(value) is not str or not value or len(value) > 128:
                return "unbound"
            return "".join(c if c.isascii() and (c.isalnum() or c in "_-.:") else "_" for c in value)

        return (
            "event=routed_response_text_projection "
            f"event_id={atom(request.event_id)} route_kind={atom(request.route_kind)} "
            f"response_kind={atom(request.response_kind)} mode=separate_speech "
            f"display_chars={len(request.text)} speech_chars={len(request.speech_text)} "
            "filter_policy=existing_output_pipeline"
        )
