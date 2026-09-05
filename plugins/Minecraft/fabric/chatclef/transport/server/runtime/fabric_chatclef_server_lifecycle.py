#20260905_kpopmodder: Own only the Fabric ChatClef websocket thread and event-loop lifecycle.
from __future__ import annotations

import asyncio
import threading
from typing import Any, Callable


class FabricChatClefServerLifecycle:
    def __init__(
        self,
        *,
        config,
        diagnostics,
        websocket_server_factory: Callable[..., Any],
    ) -> None:
        self._config = config
        self._diagnostics = diagnostics
        self._websocket_server_factory = websocket_server_factory
        self._client_handler = None
        self._shutdown_state_reset = None
        self._thread: threading.Thread | None = None
        self._loop: asyncio.AbstractEventLoop | None = None
        self._server: Any = None
        self._started_event = threading.Event()
        self._start_error: str | None = None
        self._last_error: str | None = None
        self._bound_host = config.host
        self._bound_port = config.port
        self._stopping = False

    def bind(self, *, client_handler, shutdown_state_reset) -> None:
        self._client_handler = client_handler
        self._shutdown_state_reset = shutdown_state_reset

    @property
    def endpoint(self) -> str:
        return f"ws://{self._bound_host}:{self._bound_port}"

    @property
    def last_error(self) -> str | None:
        return self._last_error

    @property
    def is_running(self) -> bool:
        return (
            self._server is not None
            and self._thread is not None
            and self._thread.is_alive()
        )

    @property
    def loop(self) -> asyncio.AbstractEventLoop | None:
        return self._loop

    @property
    def stopping(self) -> bool:
        return self._stopping

    @property
    def bound_host(self) -> str:
        return self._bound_host

    @property
    def bound_port(self) -> int:
        return self._bound_port

    def record_client_error(self, message: str) -> None:
        self._last_error = message

    def start(self) -> None:
        if self.is_running:
            return
        self._started_event.clear()
        self._start_error = None
        self._last_error = None
        self._stopping = False
        self._thread = threading.Thread(
            target=self._run_loop,
            name="LAVI-MinecraftFabricChatClef-WebSocket",
            daemon=True,
        )
        self._thread.start()
        if not self._started_event.wait(self._config.startup_timeout_sec):
            self._last_error = "Fabric ChatClef WebSocket startup timed out."
            raise RuntimeError(self._last_error)
        if self._start_error:
            raise RuntimeError(self._start_error)

    def stop(self) -> None:
        self._stopping = True
        loop = self._loop
        if loop is not None and loop.is_running():
            future = asyncio.run_coroutine_threadsafe(self._close_async(), loop)
            future.result(timeout=self._config.startup_timeout_sec)
            loop.call_soon_threadsafe(loop.stop)
        if self._thread is not None:
            self._thread.join(timeout=self._config.startup_timeout_sec)
        self._require_shutdown_state_reset().reset_for_shutdown()
        self._thread = None
        self._loop = None
        self._server = None
        self._stopping = False

    def _run_loop(self) -> None:
        loop = asyncio.new_event_loop()
        self._loop = loop
        asyncio.set_event_loop(loop)
        try:
            loop.run_until_complete(self._start_async())
            self._started_event.set()
            loop.run_forever()
        except Exception as error:
            self._start_error = (
                "Fabric ChatClef WebSocket server failed: "
                f"{type(error).__name__}: {error}"
            )
            self._last_error = self._start_error
            self._started_event.set()
        finally:
            try:
                loop.run_until_complete(self._close_async())
            finally:
                loop.close()

    async def _start_async(self) -> None:
        client_handler = self._client_handler
        if client_handler is None:
            raise RuntimeError("Fabric ChatClef client handler is not bound.")
        self._server = await self._websocket_server_factory(
            client_handler.handle_legacy_client,
            self._config.host,
            self._config.port,
        )
        socket = self._server.sockets[0] if self._server.sockets else None
        if socket is not None:
            sockname = socket.getsockname()
            self._bound_host = str(sockname[0])
            self._bound_port = int(sockname[1])
        self._diagnostics.info(f"server started at {self.endpoint}")

    async def _close_async(self) -> None:
        server = self._server
        if server is None:
            return
        self._server = None
        server.close()
        await server.wait_closed()
        self._require_shutdown_state_reset().clear_connection()
        if not self._stopping:
            self._diagnostics.info("server closed")

    def _require_shutdown_state_reset(self):
        if self._shutdown_state_reset is None:
            raise RuntimeError("Fabric ChatClef shutdown reset is not bound.")
        return self._shutdown_state_reset
