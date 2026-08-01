#20260801_kpopmodder: Keep Fabric ChatClef WebSocket server construction isolated
# from session and command message handling.
from __future__ import annotations

from collections.abc import Awaitable, Callable
from typing import Any

WebSocketHandler = Callable[[Any, str], Awaitable[None]]


async def serve_fabric_chatclef_websocket(
    handler: WebSocketHandler,
    host: str,
    port: int,
) -> Any:
    from websockets.legacy.server import serve

    #20260801_kpopmodder: Java HttpClient sends Content-Length: 0 during
    # WebSocket upgrade; websockets 15's new parser rejects that handshake.
    return await serve(handler, host, port)
