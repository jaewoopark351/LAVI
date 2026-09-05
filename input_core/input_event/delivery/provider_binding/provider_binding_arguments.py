#20260905_kpopmodder: Keeps provider-binding boundary validation independent from lifecycle orchestration.


def validate_constructor_callbacks(provider_list_callback, output_callback) -> None:
    if not callable(provider_list_callback):
        raise TypeError("provider_list_callback must be callable")
    if not callable(output_callback):
        raise TypeError("output_callback must be callable")


def validate_bind_request(provider_id, listener_factory) -> None:
    if type(provider_id) is not str or not provider_id:
        raise ValueError("provider_id must be a non-empty exact str")
    if not callable(listener_factory):
        raise TypeError("listener_factory must be callable")


def validate_provider_listener(listener) -> None:
    if not callable(listener):
        raise TypeError("provider listener factory must return a callable")


__all__ = (
    "validate_bind_request",
    "validate_constructor_callbacks",
    "validate_provider_listener",
)
