# #20260701_kpopmodder: Tracks the C++ BWAPI-RM shim scaffold without building or injecting it.
# import os


# class BWAPIShimManifest:
#     #20260701_kpopmodder: Keeps status checks file-based and side-effect free.
#     REQUIRED_FILES = (
#         "CMakeLists.txt",
#         "include\\BWAPI.h",
#         "include\\BWAPI\\Client.h",
#         "include\\LAVBWAPIRM\\Bridge.h",
#         "include\\LAVBWAPIRM\\CompatRunner.h",
#         "include\\LAVBWAPIRM\\FileBridge.h",
#         "include\\LAVBWAPIRM\\GameStateProvider.h",
#         "include\\LAVBWAPIRM\\MockBridge.h",
#         "include\\LAVBWAPIRM\\MockGameStateProvider.h",
#         "src\\BWAPI.cpp",
#         "src\\Bridge.cpp",
#         "src\\CompatRunner.cpp",
#         "src\\FileBridge.cpp",
#         "src\\MockBridge.cpp",
#         "src\\MockGameStateProvider.cpp",
#         "examples\\minimal_saida_style_bot.cpp",
#         "examples\\mock_runtime_probe.cpp",
#         "examples\\scr_readonly_runtime.cpp",
#         "examples\\saida_mock_runtime.cpp",
#         "docs\\saida_compatibility_matrix.md",
#     )

#     def __init__(self, plugin_root):
#         self.plugin_root = plugin_root
#         self.shim_root = os.path.join(plugin_root, "bwapi_shim")

#     def required_paths(self):
#         return [
#             os.path.join(self.shim_root, relative_path)
#             for relative_path in self.REQUIRED_FILES
#         ]

#     def status(self):
#         missing = [
#             os.path.relpath(path, self.plugin_root)
#             for path in self.required_paths()
#             if not os.path.isfile(path)
#         ]
#         return {
#             "shim_root": self.shim_root,
#             "source_level_compatibility": True,
#             "binary_abi_compatibility": False,
#             "native_injection": False,
#             "missing_files": missing,
#             "ready": not missing,
#         }
