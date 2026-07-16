import configparser
import os
from pathlib import Path
import tempfile

from core.paths import get_lavi_paths


#20260622_kpopmodder: Canonical config manager module for app-wide config state.

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))
CONFIG_FILE_ENV_VAR = "LAVI_CONFIG_FILE"


class ConfigManager:
    def __init__(self, config_file="config.ini"):
        self.config = configparser.ConfigParser()
        self.config_file = self._resolve_config_file(config_file)

        # Load the config file if it exists
        if os.path.exists(self.config_file):
            self.config.read(self.config_file, encoding="utf-8")

    def _resolve_config_file(self, config_file):
        config_file = str(config_file or "config.ini")
        if config_file == "config.ini":
            config_file = os.environ.get(CONFIG_FILE_ENV_VAR, config_file)
        if os.path.isabs(config_file):
            return config_file
        return str(get_lavi_paths(PROJECT_ROOT).resolve_path(config_file))

    def _write_config(self):
        #20260716_kpopmodder: Atomic replace protects user config.ini on write failure.
        target_path = Path(self.config_file)
        target_path.parent.mkdir(parents=True, exist_ok=True)
        temp_path = None
        try:
            with tempfile.NamedTemporaryFile(
                "w",
                encoding="utf-8",
                dir=target_path.parent,
                prefix=f".{target_path.name}.",
                suffix=".tmp",
                delete=False,
            ) as configfile:
                temp_path = Path(configfile.name)
                self.config.write(configfile)
            os.replace(temp_path, target_path)
        finally:
            if temp_path is not None and temp_path.exists():
                try:
                    temp_path.unlink()
                except OSError:
                    pass

    def save_config(self, section, key, value):
        """Save a configuration value under a section (group)."""
        if not self.config.has_section(section):
            self.config.add_section(section)

        self.config.set(section, key, value)

        self._write_config()

    def remove_config(self, section, key):
        """Remove a configuration value if it exists."""
        if not self.config.has_section(section):
            return False
        removed = self.config.remove_option(section, key)
        self._write_config()
        return removed

    def load_config(self, section, key):
        """Load a configuration value from a section."""
        if self.config.has_section(section) and self.config.has_option(section, key):
            return self.config.get(section, key)
        else:
            return ""

    def load_section(self, section):
        """Load all key-value pairs from a section (group)."""
        if self.config.has_section(section):
            return dict(self.config.items(section))
        else:
            return {}


config_manager = ConfigManager()
