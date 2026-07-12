#20260622_kpopmodder: Verify canonical core imports after removing root modules.
import logging
import os
import tempfile
import unittest
from pathlib import Path


class CoreImportTests(unittest.TestCase):
    def test_config_manager_exports_singleton(self):
        from core.config_manager import ConfigManager, config_manager

        self.assertIsInstance(config_manager, ConfigManager)

    def test_config_manager_uses_project_root_for_relative_config(self):#20260703_kpopmodder
        from core.config_manager import ConfigManager

        original_cwd = os.getcwd()
        with tempfile.TemporaryDirectory() as temp_dir:
            try:
                os.chdir(temp_dir)
                manager = ConfigManager("config.ini")
            finally:
                os.chdir(original_cwd)

        self.assertEqual(
            Path(__file__).resolve().parents[1] / "config.ini",
            Path(manager.config_file),
        )

    def test_config_manager_reads_and_writes_utf8(self):#20260703_kpopmodder
        from core.config_manager import ConfigManager

        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = Path(temp_dir) / "config.ini"
            manager = ConfigManager(str(config_path))
            manager.save_config("section", "key", "한글 설정")
            reloaded = ConfigManager(str(config_path))

        self.assertEqual("한글 설정", reloaded.load_config("section", "key"))

    def test_event_manager_exports_event_contracts(self):
        from core.event_manager import EventManager, EventType, event_manager

        self.assertIsInstance(event_manager, EventManager)
        self.assertIs(EventType.INTERRUPT.__class__, EventType)

    def test_global_state_exports_initialized_singleton(self):
        from core.global_state import GlobalKeys, GlobalState, global_state

        self.assertIsInstance(global_state, GlobalState)
        self.assertTrue(global_state.get_value(GlobalKeys.IS_IDLE))
        self.assertFalse(global_state.get_value(GlobalKeys.IS_AI_SPEAKING))

    def test_logger_exports_logging_helpers(self):
        from core.logger import (
            LOG_PATH,
            MEMORY_LOGGER_NAME,
            debug_print,
            get_log_path,
            log_exception,
            log_print,
            logger,
        )

        self.assertIs(get_log_path(), LOG_PATH)
        self.assertTrue(callable(log_print))
        self.assertTrue(callable(debug_print))
        self.assertTrue(callable(log_exception))
        self.assertEqual(logger.name, "LAV")
        memory_logger = logging.getLogger(MEMORY_LOGGER_NAME)
        self.assertEqual(memory_logger.name, "LAV.memory_core")
        self.assertEqual(memory_logger.level, logging.INFO)
        self.assertTrue(memory_logger.propagate)

    def test_app_core_gradio_launch_exports_port_helper(self):
        from app_core.gradio_launch import find_available_port#20260630_kpopmodder

        self.assertTrue(callable(find_available_port))


if __name__ == "__main__":
    unittest.main()
