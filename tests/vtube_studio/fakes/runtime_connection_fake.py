#20260904_kpopmodder: Model attempt authentication and disconnect ownership for runtime-only tests.


class RuntimeConnectionFake:
    def __init__(self):
        self.current_attempt_id = 1
        self.authenticated_attempt_id = None
        self.disconnections = []
        self.shutdown_calls = 0
        self.worker_alive = False
        self.cleanup_pending = False

    def begin_attempt(self, attempt_id):
        self.current_attempt_id = attempt_id
        self.authenticated_attempt_id = None

    def mark_authenticated(self, attempt_id):
        if attempt_id != self.current_attempt_id:
            return False
        self.authenticated_attempt_id = attempt_id
        return True

    def is_attempt_authenticated(self, attempt_id):
        return self.authenticated_attempt_id == attempt_id

    def disconnect_attempt(self, attempt_id, reason):
        if attempt_id != self.current_attempt_id:
            return False
        self.disconnections.append((attempt_id, reason))
        self.authenticated_attempt_id = None
        return True

    def shutdown(self):
        self.shutdown_calls += 1
        return True
