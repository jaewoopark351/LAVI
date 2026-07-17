#20260717_kpopmodder: Common runtime contract surface for plugin interface implementations.
from plugin_system.contracts import (
    AvailabilityProbeContract,
    PluginDiagnostic,
    PluginRuntimeContract,
    PluginState,
    PluginSupports,
)


class RuntimePluginContractMixin:
    #20260717_kpopmodder: Plugin instances inherit a diagnostics/contract facade without loading provider resources.
    PLUGIN_METADATA = {}

    @classmethod
    def plugin_metadata(cls):
        metadata = getattr(cls, "PLUGIN_METADATA", {}) or {}
        if isinstance(metadata, dict):
            return dict(metadata)
        return {}

    @property
    def manifest(self):
        return dict(self.runtime_contract.manifest)

    @property
    def config_schema(self):
        return dict(self.runtime_contract.config_schema)

    @property
    def availability_probe(self):
        return self.runtime_contract.availability_probe.to_dict()

    @property
    def capabilities(self):
        return tuple(self.runtime_contract.capabilities)

    @property
    def supports_offline(self):
        return bool(self.runtime_contract.supports.offline)

    @property
    def supports_cpu(self):
        return bool(self.runtime_contract.supports.cpu)

    @property
    def requires_gpu(self):
        return bool(self.runtime_contract.supports.requires_gpu)

    @property
    def runtime_contract(self):
        metadata = self.plugin_metadata()
        manifest = self._manifest_from_metadata(metadata)
        probe = self._availability_probe_from_metadata(metadata)
        return PluginRuntimeContract(
            plugin_id=manifest["id"],
            manifest=manifest,
            config_schema=self._dict_value(metadata, "config_schema"),
            availability_probe=probe,
            capabilities=tuple(self._sequence_value(metadata, "capabilities")),
            supports=self._supports_from_metadata(metadata),
        )

    def diagnostics(self):
        contract = self.runtime_contract
        return PluginDiagnostic(
            plugin_id=contract.plugin_id,
            state=PluginState.READY,
            reason_code="runtime_instance_ready",
            human_readable_message=(
                f"{self.__class__.__name__} runtime instance is available."
            ),
        ).to_dict()

    @classmethod
    def _manifest_from_metadata(cls, metadata):
        nested = cls._dict_value(metadata, "manifest")
        class_name = cls.__name__
        return {
            "id": cls._text_value(metadata, nested, "id", class_name),
            "display_name": cls._text_value(
                metadata,
                nested,
                "display_name",
                class_name,
            ),
            "api_version": cls._text_value(metadata, nested, "api_version", "1"),
            "category": cls._text_value(metadata, nested, "category", "plugin"),
            "entrypoint": cls._text_value(
                metadata,
                nested,
                "entrypoint",
                f"{cls.__module__}:{class_name}",
            ),
            "dependency_group": cls._text_value(
                metadata,
                nested,
                "dependency_group",
                "plugin",
            ),
        }

    @classmethod
    def _availability_probe_from_metadata(cls, metadata):
        nested = cls._dict_value(metadata, "availability_probe")
        return AvailabilityProbeContract(
            required_python_packages=tuple(
                cls._contract_sequence(metadata, nested, "required_python_packages")
            ),
            required_files=tuple(
                cls._contract_sequence(metadata, nested, "required_files")
            ),
            required_executables=tuple(
                cls._contract_sequence(metadata, nested, "required_executables")
            ),
            required_services=tuple(
                cls._contract_sequence(metadata, nested, "required_services")
            ),
            timeout_sec=cls._positive_number(metadata, nested, "timeout_sec", 0.25),
            log_reference=cls._text_value(metadata, nested, "log_reference", ""),
        )

    @classmethod
    def _supports_from_metadata(cls, metadata):
        supports = cls._dict_value(metadata, "supports")
        return PluginSupports(
            offline=cls._bool_value(
                metadata,
                supports,
                "offline",
                "supports_offline",
                False,
            ),
            cpu=cls._bool_value(
                metadata,
                supports,
                "cpu",
                "supports_cpu",
                True,
            ),
            requires_gpu=cls._bool_value(
                metadata,
                supports,
                "requires_gpu",
                "requires_gpu",
                False,
            ),
        )

    @staticmethod
    def _dict_value(metadata, key):
        value = metadata.get(key)
        if isinstance(value, dict):
            return dict(value)
        return {}

    @staticmethod
    def _sequence_value(metadata, key):
        value = metadata.get(key)
        if isinstance(value, str):
            return [value] if value.strip() else []
        if isinstance(value, (list, tuple)):
            return [str(item).strip() for item in value if str(item).strip()]
        return []

    @classmethod
    def _contract_sequence(cls, metadata, nested, key):
        if key in nested:
            return cls._sequence_from_value(nested.get(key))
        return cls._sequence_from_value(metadata.get(key))

    @staticmethod
    def _sequence_from_value(value):
        if isinstance(value, str):
            return [value] if value.strip() else []
        if isinstance(value, (list, tuple)):
            return [str(item).strip() for item in value if str(item).strip()]
        return []

    @staticmethod
    def _text_value(metadata, nested, key, default):
        value = nested.get(key, metadata.get(key, default))
        if isinstance(value, str) and value.strip():
            return value.strip()
        return default

    @staticmethod
    def _positive_number(metadata, nested, key, default):
        value = nested.get(key, metadata.get(key, default))
        if isinstance(value, bool) or not isinstance(value, (int, float)) or value <= 0:
            return default
        return float(value)

    @staticmethod
    def _bool_value(metadata, nested, nested_key, top_level_key, default):
        if top_level_key in metadata:
            value = metadata.get(top_level_key)
        elif nested_key in nested:
            value = nested.get(nested_key)
        else:
            return default
        return value if isinstance(value, bool) else default
