#20260717_kpopmodder: Added this module to keep shared plugin contract constants class-free.
PLUGIN_LIFECYCLE_METHODS = ("init", "start", "stop", "shutdown")
PLUGIN_MANIFEST_FIELDS = (
    "id",
    "display_name",
    "api_version",
    "category",
    "entrypoint",
    "dependency_group",
)
PLUGIN_AVAILABILITY_PROBE_FIELDS = (
    "required_python_packages",
    "required_files",
    "required_executables",
    "required_services",
)
