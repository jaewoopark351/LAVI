#20260904_kpopmodder: Represent only the alive state used by runtime controller tests.


class ThreadStateFake:
    def __init__(self):
        self.alive = False

    def is_alive(self):
        return self.alive
