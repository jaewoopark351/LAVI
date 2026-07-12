#20260707_kpopmodder: Added a tiny helper for running StarCraft2 asyncio work on a background thread.
from __future__ import annotations

import asyncio
import threading
import time
from typing import Any, Callable, Dict, Optional

from core.logger import log_print


class StarCraft2ThreadedAsyncRunner:
    #20260707_kpopmodder: UI callbacks must not own the SC2 asyncio loop.
    def __init__(self, name: str = "StarCraft2AsyncRunner"):
        self.name = name
        self.thread = None
        self.loop = None
        self.started_at = None
        self.last_error = ""
        self.last_result = None

    def start(self, coroutine_factory: Callable[[], Any]) -> bool:
        if self.is_running():
            return True
        self.last_error = ""
        self.thread = threading.Thread(
            target=self._thread_main,
            args=(coroutine_factory,),
            name=self.name,
            daemon=True,
        )
        self.started_at = time.time()
        self.thread.start()
        return True

    def start_sync(self, function_factory: Callable[[], Any]) -> bool:
        #20260707_kpopmodder: burnysc2.run_game owns asyncio.run(), so it must run outside this runner's event loop.
        if self.is_running():
            return True
        self.last_error = ""
        self.thread = threading.Thread(
            target=self._sync_thread_main,
            args=(function_factory,),
            name=self.name,
            daemon=True,
        )
        self.started_at = time.time()
        self.thread.start()
        return True

    def join(self, timeout: Optional[float] = None) -> bool:
        thread = self.thread
        if thread is not None and thread.is_alive():
            thread.join(timeout=timeout)
        return not self.is_running()

    def is_running(self) -> bool:
        return bool(self.thread and self.thread.is_alive())

    def get_status(self) -> Dict[str, Any]:
        return {
            "running": self.is_running(),
            "started_at": self.started_at,
            "last_error": self.last_error,
            "last_result": self.last_result,
        }

    def _thread_main(self, coroutine_factory: Callable[[], Any]) -> None:
        loop = asyncio.new_event_loop()
        self.loop = loop
        try:
            asyncio.set_event_loop(loop)
            result = loop.run_until_complete(coroutine_factory())
            self.last_result = result
        except Exception as e:
            self.last_error = str(e)
            log_print(f"[{self.name}] failed: {e}")
        finally:
            try:
                loop.close()
            except Exception:
                pass
            self.loop = None

    def _sync_thread_main(self, function_factory: Callable[[], Any]) -> None:
        self.loop = None
        try:
            self.last_result = function_factory()
        except Exception as e:
            self.last_error = str(e)
            log_print(f"[{self.name}] failed: {e}")
