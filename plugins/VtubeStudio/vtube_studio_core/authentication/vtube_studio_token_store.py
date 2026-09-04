#20260904_kpopmodder: Isolate VTube Studio token-file persistence from authentication protocol state.
import os
import tempfile


class VTubeStudioTokenStore:
    def __init__(self, token_path):
        self.token_path = token_path

    def exists(self):
        return os.path.exists(self.token_path)

    def ensure_exists(self):
        if self.exists():
            return False
        with open(self.token_path, "w", encoding="utf-8") as file:
            file.write("")
        return True

    def read(self):
        with open(self.token_path, "r", encoding="utf-8") as file:
            return file.read().strip()

    def write(self, token):
        staged_path = self.stage(token)
        try:
            self.publish_staged(staged_path)
        except Exception:
            self.discard(staged_path)
            raise

    def has_nonempty_token(self):
        try:
            return bool(self.read())
        except Exception:
            return False

    def stage(self, token):
        token_directory = os.path.dirname(os.path.abspath(self.token_path))
        file_descriptor, staged_path = tempfile.mkstemp(
            prefix=".vtube-token-",
            suffix=".tmp",
            dir=token_directory,
            text=True,
        )
        try:
            with os.fdopen(file_descriptor, "w", encoding="utf-8") as file:
                file.write(str(token))
        except Exception:
            self.discard(staged_path)
            raise
        return staged_path

    def publish_staged(self, staged_path):
        """Publish with one same-volume atomic replace; do not add blocking callbacks."""
        os.replace(staged_path, self.token_path)

    def discard(self, staged_path):
        try:
            os.remove(staged_path)
        except FileNotFoundError:
            return False
        return True
