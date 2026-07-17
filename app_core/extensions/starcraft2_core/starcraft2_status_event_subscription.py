#20260717_kpopmodder: Isolates StarCraft2 status callback unsubscription behavior.


class _StarCraft2StatusEventSubscription:
    #20260714_kpopmodder: Keep callback lifecycle explicit across bridge-first and
    # GameExtensionInterface-style callback registration paths.
    def __init__(self, unsubscribe_callback):
        self._unsubscribe_callback = unsubscribe_callback

    def unsubscribe(self) -> None:
        callback = self._unsubscribe_callback
        if callable(callback):
            callback()
