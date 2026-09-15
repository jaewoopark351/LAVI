#20260915_kpopmodder: Validate compressed registry metadata without executing or resolving commands.
from __future__ import annotations

import base64
import binascii
import hashlib
import json
import re
import zlib
import unicodedata
from types import MappingProxyType


class FabricCommandCatalogueDecoder:
    MAX_ENCODED = 768 * 1024
    MAX_DECODED = 16 * 1024 * 1024
    MAX_ENTRIES = 65536
    _ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z", re.ASCII)
    _TOKEN = re.compile(r"[A-Za-z0-9_:./-]+\Z", re.ASCII)
    _COMMANDS = frozenset({"get", "equip", "deposit", "deposit_all", "give", "scan", "attack", "find"})

    def decode(self, wrapper: object):
        if (type(wrapper) is not dict or wrapper.get("available") is not True
                or wrapper.get("encoding") != "gzip+base64"):
            raise ValueError("catalogue_unavailable")
        encoded = wrapper.get("payload")
        declared = wrapper.get("uncompressed_bytes")
        count = wrapper.get("entry_count")
        digest = wrapper.get("sha256")
        if (type(encoded) is not str or not 0 < len(encoded) <= self.MAX_ENCODED
                or type(declared) is not int or not 0 < declared <= self.MAX_DECODED
                or type(count) is not int or not 0 < count <= self.MAX_ENTRIES
                or type(digest) is not str or re.fullmatch(r"[0-9a-fA-F]{64}", digest) is None):
            raise ValueError("catalogue_invalid_envelope")
        try:
            compressed = base64.b64decode(encoded, validate=True)
            inflater = zlib.decompressobj(16 + zlib.MAX_WBITS)
            raw = inflater.decompress(compressed, declared + 1)
        except (ValueError, binascii.Error, zlib.error) as error:
            raise ValueError("catalogue_invalid_compression") from error
        if (len(raw) != declared or not inflater.eof or inflater.unused_data
                or inflater.unconsumed_tail):
            raise ValueError("catalogue_size_mismatch")
        if hashlib.sha256(raw).hexdigest() != digest.lower():
            raise ValueError("catalogue_hash_mismatch")
        try:
            data = json.loads(raw.decode("utf-8"), object_pairs_hook=self._unique_object,
                              parse_constant=self._invalid_constant)
        except (ValueError, UnicodeError, RecursionError) as error:
            raise ValueError("catalogue_invalid_json") from error
        if (type(data) is not dict or type(data.get("schema_version")) is not int
                or data.get("schema_version") != 1 or data.get("minecraft_version") != "1.20.1"):
            raise ValueError("catalogue_version_mismatch")
        entries = data.get("entries")
        if type(entries) is not list or len(entries) != count:
            raise ValueError("catalogue_count_mismatch")
        seen = set()
        frozen = []
        for entry in entries:
            if type(entry) is not dict:
                raise ValueError("catalogue_invalid_entry")
            kind, identifier = entry.get("kind"), entry.get("id")
            key, korean = entry.get("translation_key"), entry.get("korean_name")
            if (type(kind) is not str or kind not in {"item", "block", "entity"} or type(identifier) is not str
                    or len(identifier) > 256 or self._ID.fullmatch(identifier) is None
                    or type(key) is not str or not 0 < len(key) <= 256
                    or any(unicodedata.category(c)[0] == "C" for c in key)
                    or (korean is not None and (type(korean) is not str or len(korean) > 256
                        or any(unicodedata.category(c)[0] == "C" for c in korean)))):
                raise ValueError("catalogue_invalid_identity")
            identity = (kind, identifier)
            if identity in seen:
                raise ValueError("catalogue_duplicate_identity")
            seen.add(identity)
            tokens, capabilities = entry.get("tokens"), entry.get("capabilities")
            if (type(tokens) is not dict or type(capabilities) is not list
                    or len(tokens) > len(self._COMMANDS) or len(capabilities) > len(self._COMMANDS)
                    or any(type(c) is not str or c not in self._COMMANDS for c in capabilities)
                    or len(set(capabilities)) != len(capabilities)):
                raise ValueError("catalogue_invalid_capability")
            for command, token in tokens.items():
                if (command not in capabilities or type(token) is not str
                        or not 0 < len(token) <= 256 or self._TOKEN.fullmatch(token) is None):
                    raise ValueError("catalogue_invalid_token")
            aliases = entry.get("catalogue_aliases", [])
            if (type(aliases) is not list or len(aliases) > 1024
                    or any(type(a) is not str or not 0 < len(a) <= 256
                           or self._TOKEN.fullmatch(a) is None for a in aliases)):
                raise ValueError("catalogue_invalid_aliases")
            frozen.append(MappingProxyType({"kind": kind, "id": identifier,
                "translation_key": key, "korean_name": korean,
                "tokens": MappingProxyType(dict(tokens)), "capabilities": tuple(capabilities),
                "catalogue_aliases": tuple(aliases)}))
        butler = data.get("butler_user")
        if butler is not None and (type(butler) is not str or re.fullmatch(r"[A-Za-z0-9_]{1,16}", butler) is None):
            raise ValueError("catalogue_invalid_player")
        registered = data.get("registered_commands", [])
        if (type(registered) is not list or len(registered) > 512
                or any(type(c) is not str or not 0 < len(c) <= 64
                       or any(unicodedata.category(ch)[0] == "C" for ch in c) for c in registered)):
            raise ValueError("catalogue_invalid_registered_commands")
        return MappingProxyType({"schema_version": 1, "minecraft_version": "1.20.1",
            "entries": tuple(frozen), "butler_user": butler, "catalogue_sha256": digest.lower(),
            "registered_commands": tuple(registered)})

    @staticmethod
    def _unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("duplicate_json_key")
            result[key] = value
        return result

    @staticmethod
    def _invalid_constant(value):
        raise ValueError("nonfinite_json_number")
