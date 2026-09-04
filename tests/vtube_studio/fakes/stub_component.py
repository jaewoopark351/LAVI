#20260904_kpopmodder: Provide one inert component type for core-composition isolation tests.


class StubComponent:
    def __init__(self, *args, **kwargs):
        self.args = args
        self.kwargs = kwargs
