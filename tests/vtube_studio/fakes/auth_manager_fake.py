#20260904_kpopmodder: Record authentication cleanup without protocol or file I/O.


class AuthManagerFake:
    def __init__(self):
        self.stop_calls = 0

    def stop(self):
        self.stop_calls += 1
        return True

    def reset_authentication(self, attempt_id=None):
        return True
