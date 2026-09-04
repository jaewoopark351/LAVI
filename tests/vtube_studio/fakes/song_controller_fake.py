#20260904_kpopmodder: Record song-expression reset ownership in runtime shutdown tests.


class SongControllerFake:
    def __init__(self):
        self.reset_calls = 0

    def reset(self):
        self.reset_calls += 1
        return True
