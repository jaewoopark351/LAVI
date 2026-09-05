#20260905_kpopmodder: Owns construction and lazy compatibility binding for the Input facade graph.
from __future__ import annotations

from input_core.component.lifecycle import InputComponentLifecycleCoordinator
from input_core.component.output import InputEventOutputDispatcher
from input_core.component.provider_selection import (
    InputProviderBindingRequestRegistry,
    InputProviderSelectionCoordinator,
)
from input_core.component.ui import InputComponentUiBuilder
from input_core.input_event.delivery import ProviderInputEventBindingLifecycle


class InputCompatibilityGraphInstaller:
    def __init__(
        self,
        facade,
        *,
        create_provider_selection_ui_callback,
        create_all_provider_ui_callback,
        select_provider_callback,
        base_shutdown_callback,
    ) -> None:
        callbacks = (
            create_provider_selection_ui_callback,
            create_all_provider_ui_callback,
            select_provider_callback,
            base_shutdown_callback,
        )
        if not all(callable(callback) for callback in callbacks):
            raise TypeError("Input compatibility callbacks must be callable")
        self._facade = facade
        self._create_provider_selection_ui_callback = (
            create_provider_selection_ui_callback
        )
        self._create_all_provider_ui_callback = (
            create_all_provider_ui_callback
        )
        self._select_provider_callback = select_provider_callback
        self._base_shutdown_callback = base_shutdown_callback

    def install(self) -> None:
        facade = self._facade
        facade._shutdown = False
        facade._input_event_output_dispatcher = InputEventOutputDispatcher()
        facade._input_event_normalizer = (
            facade._input_event_output_dispatcher.normalizer
        )
        facade._provider_input_event_binding_lifecycle = (
            ProviderInputEventBindingLifecycle(
                provider_list_callback=lambda: facade.provider_list,
                output_callback=facade.send_output,
            )
        )
        facade._provider_binding_request_registry = (
            InputProviderBindingRequestRegistry(
                facade._provider_input_event_binding_lifecycle.bind
            )
        )
        facade._input_provider_selection_coordinator = (
            InputProviderSelectionCoordinator(
                create_all_provider_ui_callback=(
                    self._create_all_provider_ui_callback
                ),
                select_provider_callback=self._select_provider_callback,
                sync_provider_listeners_callback=facade._sync_provider_listeners,
            )
        )
        facade._input_component_ui_builder = InputComponentUiBuilder(
            create_provider_selection_ui_callback=(
                self._create_provider_selection_ui_callback
            ),
            create_all_provider_ui_callback=(
                self._create_all_provider_ui_callback
            ),
            sync_provider_listeners_callback=facade._sync_provider_listeners,
        )
        facade._input_component_lifecycle_coordinator = (
            InputComponentLifecycleCoordinator(
                shutdown_state_callback=lambda: getattr(
                    facade,
                    "_shutdown",
                    False,
                ),
                mark_shutdown_callback=lambda: setattr(
                    facade,
                    "_shutdown",
                    True,
                ),
                shutdown_provider_bindings_callback=(
                    facade._provider_input_event_binding_lifecycle.shutdown
                ),
                clear_provider_binding_requests_callback=(
                    facade._provider_binding_request_registry.clear
                ),
                clear_output_listeners_callback=(
                    facade._input_event_output_dispatcher.clear
                ),
                base_shutdown_callback=self._base_shutdown_callback,
            )
        )
        facade._sync_provider_listeners()

    def ensure_output_dispatcher(self):
        facade = self._facade
        dispatcher = getattr(facade, "_input_event_output_dispatcher", None)
        if dispatcher is None:
            dispatcher = InputEventOutputDispatcher(
                normalizer=getattr(facade, "_input_event_normalizer", None),
            )
            legacy_listeners = facade.__dict__.get("output_event_listeners")
            if legacy_listeners is not None:
                dispatcher.listeners = legacy_listeners
            facade._input_event_output_dispatcher = dispatcher
            facade._input_event_normalizer = dispatcher.normalizer
        return dispatcher

    def ensure_provider_selection_coordinator(self):
        facade = self._facade
        coordinator = getattr(
            facade,
            "_input_provider_selection_coordinator",
            None,
        )
        if coordinator is None:
            coordinator = InputProviderSelectionCoordinator(
                create_all_provider_ui_callback=(
                    self._create_all_provider_ui_callback
                ),
                select_provider_callback=self._select_provider_callback,
                sync_provider_listeners_callback=facade._sync_provider_listeners,
            )
            facade._input_provider_selection_coordinator = coordinator
        return coordinator

    def ensure_ui_builder(self):
        facade = self._facade
        builder = getattr(facade, "_input_component_ui_builder", None)
        if builder is None:
            builder = InputComponentUiBuilder(
                create_provider_selection_ui_callback=(
                    self._create_provider_selection_ui_callback
                ),
                create_all_provider_ui_callback=(
                    self._create_all_provider_ui_callback
                ),
                sync_provider_listeners_callback=(
                    facade._sync_provider_listeners
                ),
            )
            facade._input_component_ui_builder = builder
        return builder

    def ensure_lifecycle_coordinator(self):
        facade = self._facade
        coordinator = getattr(
            facade,
            "_input_component_lifecycle_coordinator",
            None,
        )
        if coordinator is None:
            coordinator = InputComponentLifecycleCoordinator(
                shutdown_state_callback=lambda: getattr(
                    facade,
                    "_shutdown",
                    False,
                ),
                mark_shutdown_callback=lambda: setattr(
                    facade,
                    "_shutdown",
                    True,
                ),
                shutdown_provider_bindings_callback=(
                    facade._provider_input_event_binding_lifecycle.shutdown
                ),
                clear_provider_binding_requests_callback=(
                    facade._provider_binding_request_registry.clear
                ),
                clear_output_listeners_callback=(
                    self.ensure_output_dispatcher().clear
                ),
                base_shutdown_callback=self._base_shutdown_callback,
            )
            facade._input_component_lifecycle_coordinator = coordinator
        return coordinator


__all__ = ("InputCompatibilityGraphInstaller",)
