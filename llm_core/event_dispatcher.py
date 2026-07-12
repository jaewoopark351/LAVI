from core.logger import log_print


class LLMEventDispatcher:#20260620_kpopmodder
    def __init__(self):
        self.output_event_listeners = []
        self.full_output_event_listeners = []

    def send_output(self, output):
        for subscriber in list(self.output_event_listeners):
            try:
                subscriber(output)
            except Exception as e:
                log_print(f"[LLMEventDispatcher] output listener failed: {e}")#20260703_kpopmodder

    def send_full_output(self, output):
        for subscriber in list(self.full_output_event_listeners):
            try:
                subscriber(output)
            except Exception as e:
                log_print(f"[LLMEventDispatcher] full output listener failed: {e}")#20260703_kpopmodder

    def add_output_event_listener(self, function, full_response=False):
        listeners = (
            self.full_output_event_listeners
            if full_response
            else self.output_event_listeners
        )
        #20260623_kpopmodder: Keep listener registration idempotent for shutdown/rebuild safety.
        if function in listeners:
            return

        if full_response:
            self.full_output_event_listeners.append(function)
        else:
            self.output_event_listeners.append(function)

    def remove_output_event_listener(self, function, full_response=False):
        listeners = (
            self.full_output_event_listeners
            if full_response
            else self.output_event_listeners
        )
        removed = False
        while function in listeners:
            listeners.remove(function)
            removed = True
        return removed

    def clear_listeners(self):
        self.output_event_listeners.clear()
        self.full_output_event_listeners.clear()
