# #20260708_kpopmodder: Added LAN room discovery for StarCraft2 proxy experiments without touching SC2 directly.
# #20260712_kpopmodder: Archived with LAN Lobby remote-human; entrypoints block this module during normal maintenance.
# from __future__ import annotations

# LAN_LOBBY_ARCHIVED_SOURCE = r'''
# import copy
# import json
# import select
# import socket
# import threading
# import time
# import uuid
# from collections.abc import Iterable
# from typing import Any, Dict, List, Optional, Tuple

# from core.logger import log_print
# from plugins.StarCraft2.sc2_lan_port_relay import normalize_lan_port_layout


# LAN_DISCOVERY_PROTOCOL = "lav.sc2.lan_room"
# LAN_DISCOVERY_VERSION = 1
# DEFAULT_DISCOVERY_PORT = 47624
# DEFAULT_JOIN_PORT = DEFAULT_DISCOVERY_PORT + 1
# DEFAULT_HUMAN_CLIENT_PORT = 5679
# DEFAULT_REMOTE_START_PORT = DEFAULT_DISCOVERY_PORT + 2
# DEFAULT_MAP_DOWNLOAD_PORT = DEFAULT_DISCOVERY_PORT + 3
# LAN_JOIN_PROTOCOL = "lav.sc2.lobby_join"
# LAN_JOIN_ACK_PROTOCOL = "lav.sc2.lobby_join_ack"
# LAN_JOIN_VERSION = 1
# REMOTE_HUMAN_START_PROTOCOL = "lav.sc2.remote_human_start"
# REMOTE_HUMAN_START_ACK_PROTOCOL = "lav.sc2.remote_human_start_ack"
# REMOTE_HUMAN_START_VERSION = 1


# class SC2LanDiscovery:
#     """UDP broadcast discovery for LAV-managed SC2 ladder/proxy rooms.

#     This does not carry SC2 commands. It only advertises enough metadata for
#     another LAV instance to find a host and later connect through a proxy layer.
#     """

#     def __init__(
#         self,
#         discovery_port: int = DEFAULT_DISCOVERY_PORT,
#         join_port: Optional[int] = None,
#         announce_interval_sec: float = 2.0,
#         room_ttl_sec: float = 10.0,
#         broadcast_addresses: Optional[Iterable[str]] = None,
#         now_func=None,
#     ):
#         self.discovery_port = int(discovery_port or DEFAULT_DISCOVERY_PORT)
#         self.join_port = int(join_port or (self.discovery_port + 1))
#         self.announce_interval_sec = max(0.2, float(announce_interval_sec or 2.0))
#         self.room_ttl_sec = max(1.0, float(room_ttl_sec or 10.0))
#         self.broadcast_addresses = [
#             str(item or "").strip()
#             for item in (broadcast_addresses or ["255.255.255.255"])
#             if str(item or "").strip()
#         ] or ["255.255.255.255"]
#         self.source_id = uuid.uuid4().hex
#         self._now = now_func or time.time
#         self._rooms: Dict[str, Dict[str, Any]] = {}
#         self._lock = threading.Lock()
#         self._host_stop = threading.Event()
#         self._join_stop = threading.Event()
#         self._scan_stop = threading.Event()
#         self._host_thread = None
#         self._join_thread = None
#         self._scan_thread = None
#         self._host_room: Dict[str, Any] = {}
#         self._joined_players: Dict[str, Dict[str, Any]] = {}
#         self._joined_lock = threading.Lock()
#         self.last_error = ""

#     def configure(self, config: Dict[str, Any]) -> None:
#         """Apply runtime config safely without starting network activity."""
#         if not isinstance(config, dict):
#             return
#         self.discovery_port = self._int_value(
#             config.get("discovery_port"),
#             self.discovery_port,
#         )
#         self.join_port = self._int_value(
#             config.get("join_port"),
#             self.discovery_port + 1,
#         )
#         self.announce_interval_sec = max(
#             0.2,
#             self._float_value(config.get("announce_interval_sec"), self.announce_interval_sec),
#         )
#         self.room_ttl_sec = max(
#             1.0,
#             self._float_value(config.get("room_ttl_sec"), self.room_ttl_sec),
#         )
#         addresses = config.get("broadcast_addresses")
#         if isinstance(addresses, list):
#             cleaned = [str(item or "").strip() for item in addresses if str(item or "").strip()]
#             if cleaned:
#                 self.broadcast_addresses = cleaned

#     def start_host(self, room_info: Dict[str, Any]) -> Dict[str, Any]:
#         self._host_room = self._normalize_room_info(room_info)
#         if self.is_hosting():
#             log_print(
#                 "[SC2LanDiscovery] host already running "
#                 f"port={self.discovery_port} room={self._host_room.get('room_name')}"
#             )
#             return {"ok": True, "hosting": True, "status": self.get_status()}

#         log_print(
#             "[SC2LanDiscovery] host starting "
#             f"port={self.discovery_port} room={self._host_room.get('room_name')} "
#             f"join_port={self.join_port} "
#             f"proxy_host={self._host_room.get('proxy_host')} "
#             f"proxy_ports={self._host_room.get('proxy_ports')} "
#             f"broadcast_addresses={self.broadcast_addresses}"
#         )
#         with self._joined_lock:
#             self._joined_players.clear()
#         self._host_stop.clear()
#         self._join_stop.clear()
#         self._host_thread = threading.Thread(
#             target=self._announce_loop,
#             name="SC2LanDiscovery.host",
#             daemon=True,
#         )
#         self._join_thread = threading.Thread(
#             target=self._join_listen_loop,
#             name="SC2LanDiscovery.join",
#             daemon=True,
#         )
#         self._host_thread.start()
#         self._join_thread.start()
#         announce_result = self.announce_once(self._host_room)
#         log_print(f"[SC2LanDiscovery] host started announce_result={announce_result}")
#         return {
#             "ok": True,
#             "hosting": True,
#             "room": copy.deepcopy(self._host_room),
#             "announce_result": announce_result,
#         }

#     def stop_host(self) -> Dict[str, Any]:
#         thread = self._host_thread
#         if thread is not None and thread.is_alive():
#             log_print(f"[SC2LanDiscovery] host stopping port={self.discovery_port}")
#             self._host_stop.set()
#             thread.join(timeout=2.0)
#         join_thread = self._join_thread
#         if join_thread is not None and join_thread.is_alive():
#             log_print(f"[SC2LanDiscovery] join listener stopping port={self.join_port}")
#             self._join_stop.set()
#             join_thread.join(timeout=2.0)
#         self._host_thread = None
#         self._join_thread = None
#         self._host_stop.clear()
#         self._join_stop.clear()
#         with self._joined_lock:
#             self._joined_players.clear()
#         log_print(f"[SC2LanDiscovery] host stopped port={self.discovery_port}")
#         return {"ok": True, "hosting": False, "status": self.get_status()}

#     def start_scan(self) -> Dict[str, Any]:
#         if self.is_scanning():
#             log_print(f"[SC2LanDiscovery] scan already running port={self.discovery_port}")
#             return {"ok": True, "scanning": True, "status": self.get_status()}

#         log_print(f"[SC2LanDiscovery] scan starting port={self.discovery_port}")
#         self._scan_stop.clear()
#         self._scan_thread = threading.Thread(
#             target=self._listen_loop,
#             name="SC2LanDiscovery.scan",
#             daemon=True,
#         )
#         self._scan_thread.start()
#         return {"ok": True, "scanning": True, "status": self.get_status()}

#     def stop_scan(self) -> Dict[str, Any]:
#         thread = self._scan_thread
#         if thread is not None and thread.is_alive():
#             log_print(f"[SC2LanDiscovery] scan stopping port={self.discovery_port}")
#             self._scan_stop.set()
#             thread.join(timeout=2.0)
#         self._scan_thread = None
#         self._scan_stop.clear()
#         log_print(f"[SC2LanDiscovery] scan stopped port={self.discovery_port}")
#         return {"ok": True, "scanning": False, "status": self.get_status()}

#     def stop(self) -> Dict[str, Any]:
#         self.stop_host()
#         self.stop_scan()
#         return {"ok": True, "status": self.get_status()}

#     def announce_once(self, room_info: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
#         room = self._normalize_room_info(room_info or self._host_room)
#         payload = self._build_payload(room)
#         data = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
#         sent = []
#         try:
#             sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#             sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
#             for address in self.broadcast_addresses:
#                 try:
#                     sock.sendto(data, (address, self.discovery_port))
#                     sent.append(address)
#                 except OSError as e:
#                     self.last_error = str(e)
#                     log_print(f"[SC2LanDiscovery] announce failed to {address}: {e}")
#             sock.close()
#         except OSError as e:
#             self.last_error = str(e)
#             return {"ok": False, "error": str(e)}
#         return {"ok": bool(sent), "sent": sent, "payload": payload}

#     def rooms(self) -> List[Dict[str, Any]]:
#         self._prune_stale()
#         with self._lock:
#             return [
#                 copy.deepcopy(room)
#                 for room in sorted(
#                     self._rooms.values(),
#                     key=lambda item: str(item.get("room_name") or ""),
#                 )
#             ]

#     def get_status(self) -> Dict[str, Any]:
#         return {
#             "discovery_port": self.discovery_port,
#             "join_port": self.join_port,
#             "announce_interval_sec": self.announce_interval_sec,
#             "room_ttl_sec": self.room_ttl_sec,
#             "broadcast_addresses": list(self.broadcast_addresses),
#             "hosting": self.is_hosting(),
#             "join_listening": self.is_join_listening(),
#             "scanning": self.is_scanning(),
#             "host_room": copy.deepcopy(self._host_room),
#             "joined_players": self._joined_players_snapshot(),
#             "rooms": self.rooms(),
#             "last_error": self.last_error,
#             "source_id": self.source_id,
#         }

#     def request_remote_human_start(
#         self,
#         player: Dict[str, Any],
#         *,
#         room_info: Optional[Dict[str, Any]] = None,
#         timeout_sec: float = 60.0,
#     ) -> Dict[str, Any]:
#         """Ask a joined HumanJoiner client to launch SC2 before the proxy starts."""
#         if not isinstance(player, dict):
#             return {"ok": False, "error": "remote_human_player_missing"}
#         host = str(player.get("remote_addr") or "").strip()
#         if not host:
#             return {"ok": False, "error": "remote_human_host_missing"}
#         port = self._int_value(player.get("remote_start_port"), DEFAULT_REMOTE_START_PORT)
#         if port <= 0 or port > 65535:
#             port = DEFAULT_REMOTE_START_PORT
#         room = self._normalize_room_info(room_info or self._host_room)
#         payload = {
#             "protocol": REMOTE_HUMAN_START_PROTOCOL,
#             "version": REMOTE_HUMAN_START_VERSION,
#             "room": room,
#             "room_id": str(room.get("room_id") or ""),
#             "source_id": self.source_id,
#             "client_id": str(player.get("client_id") or ""),
#             "player_name": str(player.get("player_name") or ""),
#             "ready_timeout_sec": max(1.0, float(timeout_sec or 60.0)),
#             "timestamp": self._now(),
#         }
#         return self._send_remote_start_request(host, port, payload, timeout_sec=timeout_sec)

#     def request_remote_native_joiner_start(
#         self,
#         player: Dict[str, Any],
#         *,
#         room_info: Optional[Dict[str, Any]] = None,
#         native_joiner: Optional[Dict[str, Any]] = None,
#         timeout_sec: float = 10.0,
#     ) -> Dict[str, Any]:
#         """Ask a joined HumanJoiner client to run its local native JoinGame helper."""
#         if not isinstance(player, dict):
#             return {"ok": False, "error": "remote_human_player_missing"}
#         host = str(player.get("remote_addr") or "").strip()
#         if not host:
#             return {"ok": False, "error": "remote_human_host_missing"}
#         port = self._int_value(player.get("remote_start_port"), DEFAULT_REMOTE_START_PORT)
#         if port <= 0 or port > 65535:
#             port = DEFAULT_REMOTE_START_PORT
#         room = self._normalize_room_info(room_info or self._host_room)
#         payload = {
#             "protocol": REMOTE_HUMAN_START_PROTOCOL,
#             "version": REMOTE_HUMAN_START_VERSION,
#             "command": "start_native_joiner",
#             "room": room,
#             "room_id": str(room.get("room_id") or ""),
#             "source_id": self.source_id,
#             "client_id": str(player.get("client_id") or ""),
#             "player_name": str(player.get("player_name") or ""),
#             "native_joiner": dict(native_joiner or {}),
#             "ready_timeout_sec": max(1.0, float(timeout_sec or 10.0)),
#             "timestamp": self._now(),
#         }
#         return self._send_remote_start_request(host, port, payload, timeout_sec=timeout_sec)

#     def is_hosting(self) -> bool:
#         return bool(self._host_thread is not None and self._host_thread.is_alive())

#     def is_join_listening(self) -> bool:
#         return bool(self._join_thread is not None and self._join_thread.is_alive())

#     def is_scanning(self) -> bool:
#         return bool(self._scan_thread is not None and self._scan_thread.is_alive())

#     def _announce_loop(self) -> None:
#         while not self._host_stop.is_set():
#             self.announce_once(self._host_room)
#             self._host_stop.wait(self.announce_interval_sec)

#     def _listen_loop(self) -> None:
#         sock = None
#         try:
#             sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#             sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
#             sock.settimeout(0.5)
#             sock.bind(("", self.discovery_port))
#             log_print(f"[SC2LanDiscovery] scan listening port={self.discovery_port}")
#             while not self._scan_stop.is_set():
#                 try:
#                     data, address = sock.recvfrom(8192)
#                 except socket.timeout:
#                     self._prune_stale()
#                     continue
#                 except OSError as e:
#                     if not self._scan_stop.is_set():
#                         self.last_error = str(e)
#                         log_print(f"[SC2LanDiscovery] listen failed: {e}")
#                     break
#                 room = self._parse_payload(data, address)
#                 if room:
#                     self._remember_room(room)
#         except OSError as e:
#             self.last_error = str(e)
#             log_print(f"[SC2LanDiscovery] scan failed: {e}")
#         finally:
#             if sock is not None:
#                 try:
#                     sock.close()
#                 except OSError:
#                     pass

#     def _join_listen_loop(self) -> None:
#         tcp_sock = None
#         udp_sock = None
#         try:
#             tcp_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#             tcp_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
#             tcp_sock.bind(("", self.join_port))
#             tcp_sock.listen(16)
#             udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#             udp_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
#             udp_sock.bind(("", self.join_port))
#             log_print(f"[SC2LanDiscovery] join TCP/UDP listening port={self.join_port}")
#             while not self._join_stop.is_set():
#                 try:
#                     ready, _, _ = select.select([tcp_sock, udp_sock], [], [], 0.5)
#                 except (OSError, ValueError) as e:
#                     if not self._join_stop.is_set():
#                         self.last_error = str(e)
#                         log_print(f"[SC2LanDiscovery] join listen failed: {e}")
#                     break
#                 if not ready:
#                     continue
#                 for ready_sock in ready:
#                     if ready_sock is tcp_sock:
#                         self._handle_tcp_join_request(tcp_sock)
#                     elif ready_sock is udp_sock:
#                         self._handle_udp_join_request(udp_sock)
#         except OSError as e:
#             self.last_error = str(e)
#             log_print(f"[SC2LanDiscovery] join listener failed: {e}")
#         finally:
#             for sock in (tcp_sock, udp_sock):
#                 if sock is not None:
#                     try:
#                         sock.close()
#                     except OSError:
#                         pass

#     def _handle_tcp_join_request(self, sock: socket.socket) -> None:
#         try:
#             conn, address = sock.accept()
#         except OSError as e:
#             if not self._join_stop.is_set():
#                 self.last_error = str(e)
#                 log_print(f"[SC2LanDiscovery] TCP join accept failed: {e}")
#             return
#         with conn:
#             log_print(f"[SC2LanDiscovery] TCP join accepted from {address}")
#             data = self._recv_join_payload(conn)
#             log_print(
#                 "[SC2LanDiscovery] TCP join payload received "
#                 f"from={address} bytes={len(data)} preview={self._join_payload_preview(data)}"
#             )
#             player, error = self._parse_join_payload(data, address)
#             if player:
#                 self._remember_joined_player(player)
#                 log_print(
#                     "[SC2LanDiscovery] TCP join parsed "
#                     f"client_id={player.get('client_id')} "
#                     f"player={player.get('player_name')} "
#                     f"remote={player.get('remote_addr')}:{player.get('remote_port')} "
#                     f"start_port={player.get('remote_start_port')} "
#                     f"human_port={player.get('human_client_port')} "
#                     f"layout={player.get('lan_port_layout')}"
#                 )
#                 self._send_join_ack(conn, address, True, "joined", player)
#             else:
#                 error = error or "invalid_join_payload"
#                 log_print(f"[SC2LanDiscovery] TCP join rejected from={address} error={error}")
#                 self._send_join_ack(conn, address, False, error)

#     def _handle_udp_join_request(self, sock: socket.socket) -> None:
#         try:
#             data, address = sock.recvfrom(8192)
#         except OSError as e:
#             if not self._join_stop.is_set():
#                 self.last_error = str(e)
#                 log_print(f"[SC2LanDiscovery] UDP join recv failed: {e}")
#             return
#         log_print(
#             "[SC2LanDiscovery] UDP join payload received "
#             f"from={address} bytes={len(data)} preview={self._join_payload_preview(data)}"
#         )
#         player, error = self._parse_join_payload(data, address)
#         if player:
#             self._remember_joined_player(player)
#             log_print(
#                 "[SC2LanDiscovery] UDP join parsed "
#                 f"client_id={player.get('client_id')} "
#                 f"player={player.get('player_name')} "
#                 f"remote={player.get('remote_addr')}:{player.get('remote_port')} "
#                 f"start_port={player.get('remote_start_port')} "
#                 f"human_port={player.get('human_client_port')} "
#                 f"layout={player.get('lan_port_layout')}"
#             )
#             self._send_join_ack_datagram(sock, address, True, "joined", player)
#         else:
#             error = error or "invalid_join_payload"
#             log_print(f"[SC2LanDiscovery] UDP join rejected from={address} error={error}")
#             self._send_join_ack_datagram(sock, address, False, error)

#     def _build_payload(self, room_info: Dict[str, Any]) -> Dict[str, Any]:
#         now = self._now()
#         payload = self._normalize_room_info(room_info)
#         payload.update(
#             {
#                 "protocol": LAN_DISCOVERY_PROTOCOL,
#                 "version": LAN_DISCOVERY_VERSION,
#                 "source_id": self.source_id,
#                 "timestamp": now,
#                 "expires_sec": self.room_ttl_sec,
#             }
#         )
#         return payload

#     def _parse_payload(self, data: bytes, address: Tuple[str, int]) -> Optional[Dict[str, Any]]:
#         try:
#             payload = json.loads(data.decode("utf-8", errors="replace"))
#         except (UnicodeDecodeError, json.JSONDecodeError):
#             return None
#         if not isinstance(payload, dict):
#             return None
#         if payload.get("protocol") != LAN_DISCOVERY_PROTOCOL:
#             return None
#         if int(payload.get("version") or 0) != LAN_DISCOVERY_VERSION:
#             return None

#         room = self._normalize_room_info(payload)
#         room["source_id"] = str(payload.get("source_id") or "")
#         if room["source_id"] == self.source_id:
#             return None
#         room["remote_addr"] = str(address[0] or "")
#         if not room.get("proxy_host"):
#             room["proxy_host"] = room["remote_addr"]
#         room["timestamp"] = self._float_value(payload.get("timestamp"), self._now())
#         room["expires_sec"] = self._float_value(payload.get("expires_sec"), self.room_ttl_sec)
#         room["last_seen"] = self._now()
#         return room

#     def _parse_join_payload(
#         self,
#         data: bytes,
#         address: Tuple[str, int],
#     ) -> Tuple[Optional[Dict[str, Any]], str]:
#         if not data:
#             return None, "empty_payload"
#         try:
#             payload = json.loads(data.decode("utf-8", errors="replace"))
#         except (UnicodeDecodeError, json.JSONDecodeError):
#             return None, "invalid_json"
#         if not isinstance(payload, dict):
#             return None, "invalid_payload_type"
#         if payload.get("protocol") != LAN_JOIN_PROTOCOL:
#             return None, "unsupported_protocol"
#         try:
#             version = int(payload.get("version") or 0)
#         except (TypeError, ValueError):
#             return None, "unsupported_version"
#         if version != LAN_JOIN_VERSION:
#             return None, "unsupported_version"

#         requested_room_id = str(payload.get("room_id") or "").strip()
#         host_room_id = str(self._host_room.get("room_id") or "").strip()
#         if host_room_id and requested_room_id and requested_room_id != host_room_id:
#             return None, "room_id_mismatch"

#         client_id = str(payload.get("client_id") or "").strip() or uuid.uuid4().hex
#         now = self._now()
#         player_name = str(payload.get("player_name") or "Human").strip() or "Human"
#         player = {
#             "client_id": client_id,
#             "room_id": requested_room_id or host_room_id,
#             "player_name": player_name,
#             "host_name": str(payload.get("host_name") or "").strip(),
#             "remote_addr": str(address[0] or ""),
#             "remote_port": int(address[1] or 0),
#             "remote_start_port": self._int_value(
#                 payload.get("remote_start_port"),
#                 DEFAULT_REMOTE_START_PORT,
#             ),
#             "human_client_port": self._int_value(
#                 payload.get("human_client_port"),
#                 DEFAULT_HUMAN_CLIENT_PORT,
#             ),
#             "lan_connect_mode": self._normalize_lan_connect_mode(
#                 payload.get("lan_connect_mode"),
#                 payload.get("multiplayer_relay_enabled", True),
#             ),
#             "lan_port_layout": normalize_lan_port_layout(
#                 payload.get("lan_port_layout")
#             ),
#             "multiplayer_relay_enabled": bool(
#                 payload.get("multiplayer_relay_enabled", True)
#             ),
#             "multiplayer_relay_ports": self._normalize_optional_ports(
#                 payload.get("multiplayer_relay_ports")
#             ),
#             "multiplayer_relay_bind_host": str(
#                 payload.get("multiplayer_relay_bind_host") or ""
#             ).strip(),
#             "joined_at": now,
#             "last_seen": now,
#             "timestamp": self._float_value(payload.get("timestamp"), now),
#         }
#         return player, ""

#     def _remember_room(self, room: Dict[str, Any]) -> None:
#         room_id = str(room.get("room_id") or room.get("source_id") or room.get("remote_addr") or "")
#         if not room_id:
#             return
#         room["room_id"] = room_id
#         with self._lock:
#             self._rooms[room_id] = copy.deepcopy(room)

#     def _remember_joined_player(self, player: Dict[str, Any]) -> None:
#         client_id = str(player.get("client_id") or "").strip()
#         if not client_id:
#             client_id = f"{player.get('remote_addr', '')}:{player.get('remote_port', '')}"
#         if not client_id:
#             return
#         with self._joined_lock:
#             duplicate_id = self._joined_player_duplicate_id(player, client_id)
#             if duplicate_id and duplicate_id != client_id:
#                 self._joined_players.pop(duplicate_id, None)
#             existing = self._joined_players.get(client_id, {})
#             merged = copy.deepcopy(existing)
#             merged.update(copy.deepcopy(player))
#             merged.setdefault("joined_at", self._now())
#             merged["last_seen"] = self._now()
#             self._joined_players[client_id] = merged

#     def _joined_player_duplicate_id(
#         self,
#         player: Dict[str, Any],
#         client_id: str,
#     ) -> str:
#         remote_addr = str(player.get("remote_addr") or "").strip()
#         human_client_port = self._int_value(
#             player.get("human_client_port"),
#             DEFAULT_HUMAN_CLIENT_PORT,
#         )
#         remote_start_port = self._int_value(
#             player.get("remote_start_port"),
#             DEFAULT_REMOTE_START_PORT,
#         )
#         if not remote_addr:
#             return ""
#         for existing_id, existing in self._joined_players.items():
#             if existing_id == client_id:
#                 continue
#             if str(existing.get("remote_addr") or "").strip() != remote_addr:
#                 continue
#             if self._int_value(existing.get("human_client_port"), DEFAULT_HUMAN_CLIENT_PORT) != human_client_port:
#                 continue
#             if self._int_value(existing.get("remote_start_port"), DEFAULT_REMOTE_START_PORT) != remote_start_port:
#                 continue
#             return existing_id
#         return ""

#     def _recv_join_payload(self, conn: socket.socket) -> bytes:
#         chunks = []
#         total = 0
#         try:
#             conn.settimeout(5.0)
#             while total <= 65536:
#                 chunk = conn.recv(8192)
#                 if not chunk:
#                     break
#                 chunks.append(chunk)
#                 total += len(chunk)
#                 if len(chunk) < 8192:
#                     break
#         except OSError as e:
#             self.last_error = str(e)
#             log_print(f"[SC2LanDiscovery] join payload recv failed: {e}")
#         return b"".join(chunks)

#     def _join_payload_preview(self, data: bytes, limit: int = 220) -> str:
#         if not data:
#             return "<empty>"
#         text = data[: max(0, int(limit or 0))].decode("utf-8", errors="replace")
#         text = text.replace("\r", "\\r").replace("\n", "\\n")
#         if len(data) > limit:
#             text += "...<truncated>"
#         return text

#     def _build_join_ack_data(
#         self,
#         ok: bool,
#         message: str,
#         player: Optional[Dict[str, Any]] = None,
#     ) -> bytes:
#         payload = {
#             "protocol": LAN_JOIN_ACK_PROTOCOL,
#             "version": LAN_JOIN_VERSION,
#             "ok": bool(ok),
#             "message": str(message or ""),
#             "room_id": str(self._host_room.get("room_id") or ""),
#             "source_id": self.source_id,
#             "timestamp": self._now(),
#             "joined_count": len(self._joined_players_snapshot()),
#         }
#         if player:
#             payload["client_id"] = str(player.get("client_id") or "")
#             payload["player_name"] = str(player.get("player_name") or "")
#         return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")

#     def _send_join_ack(
#         self,
#         sock: socket.socket,
#         address: Tuple[str, int],
#         ok: bool,
#         message: str,
#         player: Optional[Dict[str, Any]] = None,
#     ) -> None:
#         data = self._build_join_ack_data(ok, message, player)
#         try:
#             sock.sendall(data)
#             log_print(
#                 "[SC2LanDiscovery] TCP join ack sent "
#                 f"to={address} ok={bool(ok)} message={message} bytes={len(data)}"
#             )
#         except OSError as e:
#             self.last_error = str(e)
#             log_print(f"[SC2LanDiscovery] join ack failed to {address}: {e}")

#     def _send_join_ack_datagram(
#         self,
#         sock: socket.socket,
#         address: Tuple[str, int],
#         ok: bool,
#         message: str,
#         player: Optional[Dict[str, Any]] = None,
#     ) -> None:
#         data = self._build_join_ack_data(ok, message, player)
#         try:
#             sock.sendto(data, address)
#             log_print(
#                 "[SC2LanDiscovery] UDP join ack sent "
#                 f"to={address} ok={bool(ok)} message={message} bytes={len(data)}"
#             )
#         except OSError as e:
#             self.last_error = str(e)
#             log_print(f"[SC2LanDiscovery] UDP join ack failed to {address}: {e}")

#     def _joined_players_snapshot(self) -> List[Dict[str, Any]]:
#         with self._joined_lock:
#             return [
#                 copy.deepcopy(player)
#                 for player in sorted(
#                     self._joined_players.values(),
#                     key=lambda item: str(item.get("player_name") or ""),
#                 )
#             ]

#     def _prune_stale(self) -> None:
#         now = self._now()
#         with self._lock:
#             stale_ids = []
#             for room_id, room in self._rooms.items():
#                 last_seen = self._float_value(room.get("last_seen"), room.get("timestamp", now))
#                 ttl = self._float_value(room.get("expires_sec"), self.room_ttl_sec)
#                 if now - last_seen > ttl:
#                     stale_ids.append(room_id)
#             for room_id in stale_ids:
#                 self._rooms.pop(room_id, None)

#     def _normalize_room_info(self, room_info: Optional[Dict[str, Any]]) -> Dict[str, Any]:
#         value = room_info if isinstance(room_info, dict) else {}
#         configured_proxy_host = str(value.get("proxy_host") or "").strip()
#         proxy_host = self._advertisable_proxy_host(configured_proxy_host)
#         room_id = str(value.get("room_id") or f"{self.source_id}:starcraft2").strip()
#         return {
#             "room_id": room_id,
#             "room_name": str(value.get("room_name") or "LAV StarCraft II").strip(),
#             "host_name": str(value.get("host_name") or socket.gethostname()).strip(),
#             "player_name": str(value.get("player_name") or "LAV").strip(),
#             "mode": str(value.get("mode") or "observer").strip(),
#             "preferred_bot": str(value.get("preferred_bot") or "Changeling").strip(),
#             "preferred_map": str(value.get("preferred_map") or "").strip(),
#             "proxy_host": proxy_host,
#             "configured_proxy_host": configured_proxy_host,
#             "proxy_ports": self._normalize_ports(value.get("proxy_ports")),
#             "start_port": self._int_value(value.get("start_port"), 5690),
#             "join_port": self._int_value(value.get("join_port"), self.join_port),
#             "human_client_port": self._int_value(
#                 value.get("human_client_port"),
#                 DEFAULT_HUMAN_CLIENT_PORT,
#             ),
#             "remote_start_port": self._int_value(
#                 value.get("remote_start_port"),
#                 DEFAULT_REMOTE_START_PORT,
#             ),
#             "lan_connect_mode": self._normalize_lan_connect_mode(
#                 value.get("lan_connect_mode"),
#                 value.get("multiplayer_relay_enabled", True),
#             ),
#             "lan_port_layout": normalize_lan_port_layout(
#                 value.get("lan_port_layout")
#             ),
#             "multiplayer_relay_enabled": bool(
#                 value.get("multiplayer_relay_enabled", True)
#             ),
#             "multiplayer_relay_bind_host": str(
#                 value.get("multiplayer_relay_bind_host") or ""
#             ).strip(),
#             "multiplayer_relay_ports": self._normalize_optional_ports(
#                 value.get("multiplayer_relay_ports")
#             ),
#             "map_file_name": str(value.get("map_file_name") or "").strip(),
#             "map_size": self._int_value(value.get("map_size"), 0),
#             "map_sha256": str(value.get("map_sha256") or "").strip(),
#             "map_download_port": self._int_value(
#                 value.get("map_download_port"),
#                 DEFAULT_MAP_DOWNLOAD_PORT,
#             ),
#             "map_download_path": str(value.get("map_download_path") or "").strip(),
#             "room_state": str(value.get("room_state") or "waiting").strip(),
#             "notes": str(value.get("notes") or "").strip(),
#         }

#     def _send_remote_start_request(
#         self,
#         host: str,
#         port: int,
#         payload: Dict[str, Any],
#         *,
#         timeout_sec: float,
#     ) -> Dict[str, Any]:
#         data = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
#         socket_timeout = max(0.1, float(timeout_sec or 20.0) + 5.0)
#         try:
#             with socket.create_connection((host, int(port)), timeout=min(5.0, socket_timeout)) as sock:
#                 sock.settimeout(socket_timeout)
#                 sock.sendall(data)
#                 sock.shutdown(socket.SHUT_WR)
#                 chunks = []
#                 while True:
#                     chunk = sock.recv(8192)
#                     if not chunk:
#                         break
#                     chunks.append(chunk)
#         except OSError as e:
#             self.last_error = str(e)
#             log_print(f"[SC2LanDiscovery] remote human start failed {host}:{port}: {e}")
#             return {
#                 "ok": False,
#                 "target_host": host,
#                 "target_port": int(port),
#                 "error": str(e),
#             }
#         try:
#             response = json.loads(b"".join(chunks).decode("utf-8", errors="replace"))
#         except (TypeError, ValueError, json.JSONDecodeError) as e:
#             return {
#                 "ok": False,
#                 "target_host": host,
#                 "target_port": int(port),
#                 "error": f"invalid_remote_start_ack: {e}",
#             }
#         if not isinstance(response, dict):
#             return {
#                 "ok": False,
#                 "target_host": host,
#                 "target_port": int(port),
#                 "error": "invalid_remote_start_ack",
#             }
#         response.setdefault("target_host", host)
#         response.setdefault("target_port", int(port))
#         if response.get("protocol") != REMOTE_HUMAN_START_ACK_PROTOCOL:
#             response["ok"] = False
#             response["error"] = "unsupported_remote_start_ack_protocol"
#         return response

#     def _normalize_ports(self, value: Any) -> List[int]:
#         if isinstance(value, str):
#             raw_values = [part.strip() for part in value.split(",")]
#         elif isinstance(value, Iterable):
#             raw_values = list(value)
#         else:
#             raw_values = [5677, 5678]
#         ports = []
#         for item in raw_values:
#             try:
#                 port = int(item)
#             except (TypeError, ValueError):
#                 continue
#             if 0 < port <= 65535 and port not in ports:
#                 ports.append(port)
#         return ports or [5677, 5678]

#     def _normalize_optional_ports(self, value: Any) -> List[int]:
#         if value is None or value == "":
#             return []
#         if isinstance(value, str):
#             raw_values = [part.strip() for part in value.split(",")]
#         elif isinstance(value, Iterable):
#             raw_values = list(value)
#         else:
#             raw_values = [value]
#         ports = []
#         for item in raw_values:
#             try:
#                 port = int(item)
#             except (TypeError, ValueError):
#                 continue
#             if 0 < port <= 65535 and port not in ports:
#                 ports.append(port)
#         return ports

#     def _normalize_lan_connect_mode(
#         self,
#         value: Any,
#         multiplayer_relay_enabled: Any = True,
#     ) -> str:
#         text = str(value or "").strip().lower()
#         if text in {"direct", "lan", "no-relay", "norelay"}:
#             return "direct"
#         if text == "relay":
#             return "relay"
#         return "relay" if bool(multiplayer_relay_enabled) else "direct"

#     def _local_ip_hint(self) -> str:
#         try:
#             address = socket.gethostbyname(socket.gethostname())
#         except OSError:
#             return ""
#         if address.startswith("127."):
#             return ""
#         return address

#     def _advertisable_proxy_host(self, value: Any) -> str:
#         proxy_host = str(value or "").strip()
#         local_hint = self._local_ip_hint()
#         if local_hint:
#             return local_hint
#         if not self._is_unspecified_or_loopback_host(proxy_host):
#             return proxy_host
#         return ""

#     def _is_unspecified_or_loopback_host(self, value: str) -> bool:
#         text = str(value or "").strip().lower()
#         if not text:
#             return True
#         if text in {"0.0.0.0", "::", "[::]", "localhost", "::1", "[::1]"}:
#             return True
#         return text.startswith("127.")

#     def _int_value(self, value: Any, default: int) -> int:
#         try:
#             return int(value)
#         except (TypeError, ValueError):
#             return int(default)

#     def _float_value(self, value: Any, default: float) -> float:
#         try:
#             return float(value)
#         except (TypeError, ValueError):
#             return float(default)
# '''


# #20260712_kpopmodder: LAN Lobby source above is intentionally commented out in
# # a raw string so importing this module cannot open sockets or start threads.
# LAN_LOBBY_ARCHIVED_ERROR = "lan_lobby_archived"
# LAN_LOBBY_ARCHIVED_MESSAGE = "LAN Lobby remote-human source is archived/disabled."
# DEFAULT_MAP_DOWNLOAD_PORT = 47627
# DEFAULT_REMOTE_START_PORT = 47626
# LAN_JOIN_PROTOCOL = "lav.sc2.lobby_join"
# LAN_JOIN_VERSION = 1


# class SC2LanDiscovery:
#     #20260712_kpopmodder: No-op compatibility stub for archived LAN Lobby code.
#     def __init__(self, *args, **kwargs):
#         self.config = {}

#     def configure(self, config):
#         self.config = dict(config or {}) if isinstance(config, dict) else {}

#     def start_host(self, *args, **kwargs):
#         return self._archived_result("start_host")

#     def stop_host(self):
#         return self._archived_result("stop_host")

#     def start_scan(self):
#         return self._archived_result("start_scan")

#     def stop_scan(self):
#         return self._archived_result("stop_scan")

#     def stop(self):
#         return self._archived_result("stop")

#     def rooms(self):
#         return []

#     def get_status(self):
#         return {"archived": True, "error": LAN_LOBBY_ARCHIVED_ERROR}

#     def request_remote_human_start(self, *args, **kwargs):
#         return self._archived_result("request_remote_human_start")

#     def request_remote_native_joiner_start(self, *args, **kwargs):
#         return self._archived_result("request_remote_native_joiner_start")

#     def _archived_result(self, action):
#         return {
#             "ok": False,
#             "action": str(action or "lan_lobby"),
#             "error": LAN_LOBBY_ARCHIVED_ERROR,
#             "message": LAN_LOBBY_ARCHIVED_MESSAGE,
#         }
