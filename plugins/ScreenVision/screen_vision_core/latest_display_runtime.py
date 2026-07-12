#20260706_kpopmodder: Added this helper to isolate ScreenVision latest-display loop lifecycle.
import threading

from core.logger import log_print


class ScreenVisionLatestDisplayRuntime:
    #20260706_kpopmodder: Uses the owner's existing fields so shutdown and UI behavior stay compatible.
    def __init__(self, owner):
        self.owner = owner

    def start(self):
        owner = self.owner
        if (
            owner.latest_display_thread is not None
            and owner.latest_display_thread.is_alive()
        ):
            return

        owner.latest_display_stop_event.clear()
        owner.latest_display_thread = threading.Thread(
            target=self.loop,
            daemon=True,
        )
        owner.latest_display_thread.start()
        owner.live_textbox.print(
            f"[ScreenVision] Latest Display started. interval={owner.latest_display_interval_seconds:.1f}s"
        )

    def stop(self):
        owner = self.owner
        owner.latest_display_stop_event.set()
        owner.live_textbox.print("[ScreenVision] Latest Display stopped.")

    def shutdown(self, timeout=0.5):
        owner = self.owner
        owner.latest_display_stop_event.set()
        latest_thread = owner.latest_display_thread
        if (
            latest_thread is not None
            and latest_thread.is_alive()
            and threading.current_thread() != latest_thread
        ):
            try:
                latest_thread.join(timeout=timeout)
            except KeyboardInterrupt:
                log_print("[ScreenVision shutdown] latest display join skipped during Ctrl+C shutdown.")#20260630_kpopmodder

    def loop(self):
        owner = self.owner
        while not owner.latest_display_stop_event.is_set():
            owner.force_latest_screen(block_for_lock=False)
            owner.latest_display_stop_event.wait(
                owner.latest_display_interval_seconds
            )
