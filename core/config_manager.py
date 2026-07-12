#20260622_kpopmodder: Canonical config manager module for app-wide config state.
import configparser
import os

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))


class ConfigManager:
    def __init__(self, config_file="config.ini"):
        self.config = configparser.ConfigParser()
        self.config_file = self._resolve_config_file(config_file)

        # Load the config file if it exists
        if os.path.exists(self.config_file):
            self.config.read(self.config_file, encoding="utf-8")

    def _resolve_config_file(self, config_file):
        config_file = str(config_file or "config.ini")
        if os.path.isabs(config_file):
            return config_file
        return os.path.join(PROJECT_ROOT, config_file)

    def save_config(self, section, key, value):
        """Save a configuration value under a section (group)."""
        if not self.config.has_section(section):
            self.config.add_section(section)

        self.config.set(section, key, value)

        # Write the updated configuration to the file
        with open(self.config_file, "w", encoding="utf-8") as configfile:
            self.config.write(configfile)

    def remove_config(self, section, key):
        """Remove a configuration value if it exists."""
        if not self.config.has_section(section):
            return False
        removed = self.config.remove_option(section, key)
        with open(self.config_file, "w", encoding="utf-8") as configfile:
            self.config.write(configfile)
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
