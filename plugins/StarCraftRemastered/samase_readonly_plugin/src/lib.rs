//20260701_kpopmodder: Samase plugin entrypoint that writes read-only state JSON only.
use std::ffi::{c_void, CString, OsString};
use std::fs;
use std::io::Write;
use std::mem;
use std::os::windows::ffi::OsStringExt;
use std::path::Path;
use std::ptr;
use std::sync::atomic::{AtomicBool, AtomicPtr, AtomicU32, Ordering};
use std::sync::OnceLock;
use std::thread;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use samase_plugin::{PluginApi, VarId};

type Handle = *mut c_void;

const MAX_MODULES: usize = 160;
const MAX_PATH_CHARS: usize = 260;
const MAX_MODULE_NAME_CHARS: usize = 256;
const MAX_INTERESTING_SECTIONS: usize = 24;
const MAX_SAFE_SCAN_RANGES: usize = 12;
const MAX_SCAN_RANGE_BYTES: usize = 16 * 1024;
const MAX_DISCOVERY_RANGE_BYTES: usize = 4 * 1024;
const DISCOVERY_WINDOW_BYTES: usize = 4 * 1024;
const MAX_DISCOVERY_WINDOW_PROFILES_PER_MODULE: usize = 64;
const DISCOVERY_DRILLDOWN_CHUNK_BYTES: usize = 256;
const MAX_DISCOVERY_FOCUSED_WINDOWS_PER_MODULE: usize = 6;
const MAX_RESOURCE_FOCUSED_WINDOWS_PER_MODULE: usize = 96;
const DEFAULT_RESOURCE_FOCUSED_WINDOWS_PER_MODULE: usize = 64;
const MAX_DISCOVERY_CANDIDATES_PER_KIND: usize = 12;
const MAX_U32_WATCHLIST_CANDIDATES: usize = 16;
const SCAN_ALIGNMENT_BYTES: usize = 4;
const TH32CS_SNAPMODULE: u32 = 0x00000008;
const TH32CS_SNAPMODULE32: u32 = 0x00000010;
const IMAGE_SCN_MEM_EXECUTE: u32 = 0x20000000;
const IMAGE_SCN_MEM_READ: u32 = 0x40000000;
const IMAGE_SCN_MEM_WRITE: u32 = 0x80000000;
const MEM_COMMIT: u32 = 0x1000;
const PAGE_NOACCESS: u32 = 0x01;
const PAGE_READONLY: u32 = 0x02;
const PAGE_READWRITE: u32 = 0x04;
const PAGE_WRITECOPY: u32 = 0x08;
const PAGE_EXECUTE_READ: u32 = 0x20;
const PAGE_EXECUTE_READWRITE: u32 = 0x40;
const PAGE_EXECUTE_WRITECOPY: u32 = 0x80;
const PAGE_GUARD: u32 = 0x100;
const LEGACY_BW_BASE: usize = 0x0040_0000;

#[repr(C)]
struct ModuleEntry32W {
    dw_size: u32,
    th32_module_id: u32,
    th32_process_id: u32,
    glblcnt_usage: u32,
    proccnt_usage: u32,
    mod_base_addr: *mut u8,
    mod_base_size: u32,
    h_module: Handle,
    sz_module: [u16; MAX_MODULE_NAME_CHARS],
    sz_exe_path: [u16; MAX_PATH_CHARS],
}

#[repr(C)]
struct MemoryBasicInformation {
    base_address: *mut c_void,
    allocation_base: *mut c_void,
    allocation_protect: u32,
    region_size: usize,
    state: u32,
    protect: u32,
    type_: u32,
}

#[derive(Clone)]
struct ModuleInfo {
    name: String,
    path: String,
    base: usize,
    size: u32,
}

#[derive(Clone)]
struct SectionInfo {
    name: String,
    virtual_address: u32,
    virtual_size: u32,
    raw_size: u32,
    raw_pointer: u32,
    characteristics: u32,
}

struct ModuleFingerprint {
    file_size: u64,
    modified_unix: u64,
    fnv1a64: u64,
    machine: u16,
    pe_time_date_stamp: u32,
    pe_size_of_image: u32,
    pe_checksum: u32,
    pe_section_count: u16,
    entry_point: u32,
    subsystem: u16,
    sections: Vec<SectionInfo>,
}

struct FixedReadCandidate {
    name: &'static str,
    legacy_address: usize,
    size: usize,
    note: &'static str,
}

struct PrimitiveReadResult {
    name: &'static str,
    ok: bool,
    value: Option<u32>,
    json: String,
}

struct InGameDetectorResult {
    in_game: bool,
    json: String,
}

struct U32WatchSignalResult {
    status: String,
    read_count: usize,
    nonzero_count: usize,
    zero_count: usize,
    threshold: usize,
    observations: Vec<String>,
}

struct VerifiedScanRange {
    section_name: String,
    start: usize,
    end: usize,
    section_start: usize,
    section_end: usize,
    range_bytes: usize,
    writable: bool,
}

struct U32WatchCandidate {
    module_name: String,
    section_name: String,
    rva_text: String,
    rva: usize,
    confidence: String,
    resource_field: String,
}

#[derive(Default)]
struct ResourceWatchValues {
    minerals: Option<u32>,
    gas: Option<u32>,
}

#[link(name = "kernel32")]
extern "system" {
    fn CloseHandle(handle: Handle) -> i32;
    fn CreateToolhelp32Snapshot(flags: u32, process_id: u32) -> Handle;
    fn GetCurrentProcessId() -> u32;
    fn Module32FirstW(snapshot: Handle, module_entry: *mut ModuleEntry32W) -> i32;
    fn Module32NextW(snapshot: Handle, module_entry: *mut ModuleEntry32W) -> i32;
    fn VirtualQuery(
        address: *const c_void,
        buffer: *mut MemoryBasicInformation,
        length: usize,
    ) -> usize;
}

static API: AtomicPtr<PluginApi> = AtomicPtr::new(ptr::null_mut());
static FRAME_COUNT: AtomicU32 = AtomicU32::new(0);
static INITIALIZE_THREAD_STARTED: AtomicBool = AtomicBool::new(false);
static PLUGIN_API_ACTIVE: AtomicBool = AtomicBool::new(false);
static STATE_PATH: OnceLock<String> = OnceLock::new();
static WRITE_EVERY_N_FRAMES: OnceLock<u32> = OnceLock::new();
static INITIALIZE_HEARTBEAT_INTERVAL_MS: OnceLock<u64> = OnceLock::new();

#[no_mangle]
pub unsafe extern "C" fn Initialize() -> u32 {
    STATE_PATH.get_or_init(resolve_state_path);
    WRITE_EVERY_N_FRAMES.get_or_init(resolve_write_interval);
    INITIALIZE_HEARTBEAT_INTERVAL_MS.get_or_init(resolve_initialize_heartbeat_interval);
    let path = STATE_PATH.get_or_init(resolve_state_path);
    let initial_write_ok = write_state_json(
        path,
        0,
        0,
        false,
        false,
        "InitializeBridge v3: SAMASE_MORE_DLLS Initialize entry",
        "samase_more_dll_initialize",
    )
    .is_ok();
    start_initialize_heartbeat_thread();
    match initial_write_ok {
        true => 1,
        false => 0,
    }
}

fn start_initialize_heartbeat_thread() {
    if INITIALIZE_THREAD_STARTED
        .compare_exchange(false, true, Ordering::AcqRel, Ordering::Acquire)
        .is_err()
    {
        return;
    }

    let _ = thread::Builder::new()
        .name("lav-samase-readonly-heartbeat".to_string())
        .spawn(initialize_heartbeat_loop);
}

fn initialize_heartbeat_loop() {
    loop {
        if PLUGIN_API_ACTIVE.load(Ordering::Acquire) {
            break;
        }

        let interval =
            *INITIALIZE_HEARTBEAT_INTERVAL_MS.get_or_init(resolve_initialize_heartbeat_interval);
        thread::sleep(Duration::from_millis(interval));

        if PLUGIN_API_ACTIVE.load(Ordering::Acquire) {
            break;
        }

        let frame = FRAME_COUNT.fetch_add(1, Ordering::Relaxed) + 1;
        let path = STATE_PATH.get_or_init(resolve_state_path);
        let _ = write_state_json(
            path,
            frame,
            0,
            false,
            false,
            "InitializeBridge v3: SAMASE_MORE_DLLS heartbeat thread",
            "samase_more_dll_thread",
        );
    }
}

#[no_mangle]
pub unsafe extern "C" fn lav_samase_readonly_write_test_state() -> u32 {
    let path = STATE_PATH.get_or_init(resolve_state_path);
    match write_state_json(
        path,
        999_999,
        0,
        false,
        false,
        "manual exported heartbeat test",
        "manual_export_test",
    ) {
        Ok(()) => 1,
        Err(_) => 0,
    }
}

#[no_mangle]
pub unsafe extern "C" fn samase_plugin_init(api: *const PluginApi) {
    if api.is_null() {
        return;
    }

    API.store(api as *mut PluginApi, Ordering::Release);
    PLUGIN_API_ACTIVE.store(true, Ordering::Release);
    STATE_PATH.get_or_init(resolve_state_path);
    WRITE_EVERY_N_FRAMES.get_or_init(resolve_write_interval);
    INITIALIZE_HEARTBEAT_INTERVAL_MS.get_or_init(resolve_initialize_heartbeat_interval);

    let hook_result = ((*api).hook_game_loop_start)(write_state_on_game_loop);
    let observation = format!("samase plugin init; hook_game_loop_start={}", hook_result);
    let path = STATE_PATH.get_or_init(resolve_state_path);
    let _ = write_state_json(
        path,
        FRAME_COUNT.load(Ordering::Relaxed),
        0,
        false,
        false,
        &observation,
        "samase_plugin_api_init",
    );
    print_text(api, "[LAV] Samase read-only state plugin loaded");
}

unsafe extern "C" fn write_state_on_game_loop() {
    let frame = FRAME_COUNT.fetch_add(1, Ordering::Relaxed) + 1;
    let every_n = *WRITE_EVERY_N_FRAMES.get_or_init(resolve_write_interval);
    if every_n > 1 && frame % every_n != 0 {
        return;
    }

    let api = API.load(Ordering::Acquire);
    if api.is_null() {
        return;
    }

    let local_player = read_var(api, VarId::LocalPlayerId) as u32;
    let is_multiplayer = read_var(api, VarId::IsMultiplayer) != 0
        || call_optional_u32(((*api).is_multiplayer)()) != 0;
    let in_game = local_player < 12;
    let path = STATE_PATH.get_or_init(resolve_state_path);

    if let Err(error) = write_state_json(
        path,
        frame,
        local_player,
        is_multiplayer,
        in_game,
        "samase plugin heartbeat",
        "samase_plugin_api",
    ) {
        if frame == every_n {
            let message = format!("[LAV] read-only state write failed: {}", error);
            print_text(api, &message);
        }
    }
}

unsafe fn read_var(api: *const PluginApi, var: VarId) -> usize {
    let vars = [var as u16];
    let mut values = [0usize];
    ((*api).read_vars)(vars.as_ptr(), values.as_mut_ptr(), values.len());
    values[0]
}

unsafe fn call_optional_u32(func: Option<unsafe extern "C" fn() -> u32>) -> u32 {
    match func {
        Some(func) => func(),
        None => 0,
    }
}

fn write_state_json(
    path: &str,
    frame: u32,
    local_player: u32,
    is_multiplayer: bool,
    in_game: bool,
    observation: &str,
    loader: &str,
) -> std::io::Result<()> {
    if let Some(parent) = Path::new(path).parent() {
        if !parent.as_os_str().is_empty() {
            fs::create_dir_all(parent)?;
        }
    }

    let temp_path = format!("{}.tmp", path);
    let mut file = fs::File::create(&temp_path)?;
    let detector = detect_in_game_state(loader, local_player, is_multiplayer, in_game);
    let effective_in_game = detector.in_game;
    let resource_values = if effective_in_game && !is_multiplayer {
        resource_watch_values_from_u32_watch()
    } else {
        ResourceWatchValues::default()
    };
    let self_minerals = resource_values.minerals.unwrap_or(0);
    let self_gas = resource_values.gas.unwrap_or(0);
    let bridge_json = build_bridge_diagnostics_json(loader, &detector.json);
    let json = format!(
        concat!(
            "{{\n",
            "  \"schema\": \"lav_samase_readonly_state_v1\",\n",
            "  \"written_at\": {},\n",
            "  \"source\": \"lav_samase_readonly_plugin\",\n",
            "  \"loader\": \"{}\",\n",
            "  \"safety\": {{\n",
            "    \"single_player_only\": true,\n",
            "    \"battle_net_blocked\": true,\n",
            "    \"multiplayer_blocked\": {},\n",
            "    \"reason\": \"{}\"\n",
            "  }},\n",
            "  \"game\": {{\n",
            "    \"connected\": true,\n",
            "    \"in_game\": {},\n",
            "    \"single_player\": {},\n",
            "    \"battle_net_screen\": false,\n",
            "    \"multiplayer_screen\": {},\n",
            "    \"frame_count\": {},\n",
            "    \"map_name\": \"\",\n",
            "    \"map_width\": 0,\n",
            "    \"map_height\": 0,\n",
            "    \"self\": {{\n",
            "      \"id\": {},\n",
            "      \"name\": \"LocalPlayer\",\n",
            "      \"race\": \"\",\n",
            "      \"minerals\": {},\n",
            "      \"gas\": {},\n",
            "      \"supply_used\": 0,\n",
            "      \"supply_total\": 0,\n",
            "      \"start_location\": null\n",
            "    }},\n",
            "    \"enemy\": {{\n",
            "      \"id\": 2,\n",
            "      \"name\": \"Enemy\",\n",
            "      \"race\": \"\",\n",
            "      \"start_location\": null\n",
            "    }}\n",
            "  }},\n",
            "  \"bridge\": {},\n",
            "  \"units\": {{\n",
            "    \"my\": [],\n",
            "    \"enemy\": [],\n",
            "    \"neutral\": []\n",
            "  }},\n",
            "  \"observation\": \"{}\"\n",
            "}}\n"
        ),
        unix_timestamp_secs(),
        escape_json(loader),
        bool_text(!is_multiplayer),
        if is_multiplayer {
            "multiplayer blocked by read-only bridge"
        } else {
            ""
        },
        bool_text(effective_in_game),
        bool_text(!is_multiplayer),
        bool_text(is_multiplayer),
        frame,
        local_player,
        self_minerals,
        self_gas,
        bridge_json,
        escape_json(observation)
    );
    file.write_all(json.as_bytes())?;
    file.flush()?;
    drop(file);
    fs::rename(temp_path, path)?;
    Ok(())
}

fn build_bridge_diagnostics_json(loader: &str, in_game_detector_json: &str) -> String {
    let pid = current_process_id();
    let modules = enumerate_process_modules(pid);
    let api_active = PLUGIN_API_ACTIVE.load(Ordering::Acquire);
    let starcraft = find_module(&modules, &["starcraft.exe", "broodwar.exe"]);
    let clientsdk = find_module(&modules, &["clientsdk.dll"]);
    let samase_temp = find_samase_temp_module(&modules);
    let bridge = find_module(&modules, &["lav_samase_readonly_plugin.dll"]);

    format!(
        concat!(
            "{{\n",
            "    \"schema\": \"lav_initialize_bridge_v3\",\n",
            "    \"compat_schema\": \"lav_initialize_bridge_v2\",\n",
            "    \"legacy_compat_schema\": \"lav_initialize_bridge_v1\",\n",
            "    \"version\": 3,\n",
            "    \"entry\": \"SAMASE_MORE_DLLS.Initialize\",\n",
            "    \"loader\": \"{}\",\n",
            "    \"api_active\": {},\n",
            "    \"process\": {{\n",
            "      \"pid\": {},\n",
            "      \"pointer_width_bits\": {},\n",
            "      \"module_count\": {}\n",
            "    }},\n",
            "    \"in_game_detector\": {},\n",
            "    \"scr_version_snapshot\": {{\n",
            "      \"starcraft_module\": {},\n",
            "      \"clientsdk_module\": {},\n",
            "      \"samase_temp_module\": {},\n",
            "      \"bridge_module\": {}\n",
            "    }},\n",
            "    \"fingerprints\": {{\n",
            "      \"starcraft_module\": {},\n",
            "      \"clientsdk_module\": {},\n",
            "      \"samase_temp_module\": {},\n",
            "      \"bridge_module\": {}\n",
            "    }},\n",
            "    \"section_scan_preparation\": {},\n",
            "    \"offset_discovery\": {},\n",
            "    \"focused_u32_watch\": {},\n",
            "    \"read_only_memory_probe\": {{\n",
            "      \"mode\": \"bounded_candidates_only\",\n",
            "      \"direct_memory_reads\": false,\n",
            "      \"reason\": \"InitializeBridge v3 creates verified section-bounded scan candidates only; read probes and pointer dereferences remain deferred.\",\n",
            "      \"candidates\": [\n",
            "        {},\n",
            "        {},\n",
            "        {},\n",
            "        {},\n",
            "        {},\n",
            "        {},\n",
            "        {},\n",
            "        {}\n",
            "      ]\n",
            "    }},\n",
            "    \"modules\": {}\n",
            "  }}"
        ),
        escape_json(loader),
        bool_text(api_active),
        pid,
        mem::size_of::<usize>() * 8,
        modules.len(),
        in_game_detector_json,
        module_option_json(starcraft),
        module_option_json(clientsdk),
        module_option_json(samase_temp),
        module_option_json(bridge),
        module_fingerprint_option_json("starcraft_module", starcraft),
        module_fingerprint_option_json("clientsdk_module", clientsdk),
        module_fingerprint_option_json("samase_temp_module", samase_temp),
        module_fingerprint_option_json("bridge_module", bridge),
        section_scan_preparation_json(starcraft, clientsdk, samase_temp),
        offset_discovery_json(starcraft, clientsdk, samase_temp),
        focused_u32_watch_json(starcraft),
        surface_candidate_json(
            "samase_plugin_api",
            if api_active { "active" } else { "unavailable" },
            "Preferred PluginApi path; unavailable when Samase only calls Initialize.",
            None,
        ),
        surface_candidate_json(
            "starcraft_exe_module",
            module_status(starcraft),
            "Main SCR module base candidate for later read-only offset research.",
            starcraft,
        ),
        surface_candidate_json(
            "clientsdk_module",
            module_status(clientsdk),
            "Blizzard client SDK module candidate; diagnostic only.",
            clientsdk,
        ),
        surface_candidate_json(
            "samase_temp_module",
            module_status(samase_temp),
            "Samase temporary injected module candidate; fingerprinted before any memory scan.",
            samase_temp,
        ),
        surface_candidate_json(
            "bridge_module",
            module_status(bridge),
            "This read-only bridge DLL; confirms injected code location.",
            bridge,
        ),
        pointer_candidate_json(
            "in_game_state_pointer",
            "deferred_validation",
            "Will only be tested after module fingerprint, section bounds, and single-player gates pass.",
        ),
        pointer_candidate_json(
            "resource_state_pointer",
            "deferred_validation",
            "Minerals, gas, and supply candidates are recorded as names and bounded ranges only at v3.",
        ),
        pointer_candidate_json(
            "unit_table_pointer",
            "deferred_validation",
            "Unit list candidates must pass range and structure validation before dereference.",
        ),
        modules_json(&modules)
    )
}

fn detect_in_game_state(
    loader: &str,
    local_player: u32,
    is_multiplayer: bool,
    api_reported_in_game: bool,
) -> InGameDetectorResult {
    if loader == "samase_plugin_api" {
        let in_game = api_reported_in_game && !is_multiplayer && local_player < 12;
        let reason = if in_game {
            "PluginApi LocalPlayerId is in player range and IsMultiplayer is false."
        } else {
            "PluginApi heartbeat is active, but player/multiplayer gates did not pass."
        };
        return InGameDetectorResult {
            in_game,
            json: format!(
                concat!(
                    "{{",
                    "\"schema\":\"lav_in_game_detector_v1\",",
                    "\"source\":\"samase_plugin_api\",",
                    "\"enabled\":true,",
                    "\"in_game\":{},",
                    "\"confidence\":\"{}\",",
                    "\"reason\":\"{}\",",
                    "\"api_reported_in_game\":{},",
                    "\"local_player_id\":{},",
                    "\"is_multiplayer\":{},",
                    "\"direct_memory_reads\":false,",
                    "\"direct_primitive_reads\":false,",
                    "\"read_now\":false,",
                    "\"pointer_dereferences\":false,",
                    "\"observations\":[]",
                    "}}"
                ),
                bool_text(in_game),
                if in_game { "high" } else { "none" },
                escape_json(reason),
                bool_text(api_reported_in_game),
                local_player,
                bool_text(is_multiplayer)
            ),
        };
    }

    detect_initialize_in_game_state(api_reported_in_game)
}

fn offset_discovery_json(
    starcraft: Option<&ModuleInfo>,
    clientsdk: Option<&ModuleInfo>,
    samase_temp: Option<&ModuleInfo>,
) -> String {
    format!(
        concat!(
            "{{",
            "\"schema\":\"lav_scr_offset_discovery_v2\",",
            "\"compat_schema\":\"lav_scr_offset_discovery_v1\",",
            "\"mode\":\"bounded_readonly_scan\",",
            "\"enabled\":{},",
            "\"candidate_only\":true,",
            "\"bounded_by_verified_scan_ranges\":true,",
            "\"direct_memory_reads\":{},",
            "\"direct_primitive_reads\":{},",
            "\"pointer_dereferences\":false,",
            "\"scan_policy\":{{",
            "\"writable_sections_only\":true,",
            "\"allowed_section_names\":[\".data\"],",
            "\"zero_suppressed\":true,",
            "\"max_range_bytes\":{},",
            "\"alignment_bytes\":{},",
            "\"max_candidates_per_kind\":{},",
            "\"candidate_kinds\":[\"small_u32_nonzero\",\"bool_u8_true\",\"readable_pointer_u32\"],",
            "\"window_profile_schema\":\"lav_scr_offset_window_profiles_v1\",",
            "\"window_profile_bytes\":{},",
            "\"max_window_profiles_per_module\":{},",
            "\"focused_drilldown_schema\":\"lav_scr_focused_window_drilldown_v2\",",
            "\"focused_drilldown_compat_schema\":\"lav_scr_focused_window_drilldown_v1\",",
            "\"focused_chunk_bytes\":{},",
            "\"seed_focused_windows_per_module\":{},",
            "\"resource_focused_scan_enabled\":{},",
            "\"resource_focused_start_window\":{},",
            "\"max_resource_focused_windows_per_module\":{}",
            "}},",
            "\"module_groups\":[{},{},{}]",
            "}}"
        ),
        bool_text(resolve_offset_discovery_enabled()),
        bool_text(resolve_offset_discovery_enabled()),
        bool_text(resolve_offset_discovery_enabled()),
        MAX_DISCOVERY_RANGE_BYTES,
        SCAN_ALIGNMENT_BYTES,
        MAX_DISCOVERY_CANDIDATES_PER_KIND,
        DISCOVERY_WINDOW_BYTES,
        MAX_DISCOVERY_WINDOW_PROFILES_PER_MODULE,
        DISCOVERY_DRILLDOWN_CHUNK_BYTES,
        MAX_DISCOVERY_FOCUSED_WINDOWS_PER_MODULE,
        bool_text(resolve_resource_focused_scan_enabled()),
        resolve_resource_focused_start_window(),
        resolve_resource_focused_window_limit(),
        offset_discovery_module_json("starcraft_module", starcraft),
        offset_discovery_module_json("clientsdk_module", clientsdk),
        offset_discovery_module_json("samase_temp_module", samase_temp)
    )
}

fn focused_u32_watch_json(starcraft: Option<&ModuleInfo>) -> String {
    let enabled = resolve_u32_watch_enabled();
    let path = resolve_u32_watchlist_path();
    if !enabled {
        return format!(
            concat!(
                "{{",
                "\"schema\":\"lav_scr_focused_u32_watch_v1\",",
                "\"enabled\":false,",
                "\"status\":\"disabled\",",
                "\"source_path\":\"{}\",",
                "\"direct_memory_reads\":false,",
                "\"pointer_dereferences\":false,",
                "\"candidates\":[]",
                "}}"
            ),
            escape_json(&path)
        );
    }

    let candidates = match load_u32_watch_candidates(&path) {
        Ok(candidates) => candidates,
        Err(error) => {
            return format!(
                concat!(
                    "{{",
                    "\"schema\":\"lav_scr_focused_u32_watch_v1\",",
                    "\"enabled\":true,",
                    "\"status\":\"missing_or_invalid_watchlist\",",
                    "\"source_path\":\"{}\",",
                    "\"error\":\"{}\",",
                    "\"direct_memory_reads\":false,",
                    "\"pointer_dereferences\":false,",
                    "\"candidates\":[]",
                    "}}"
                ),
                escape_json(&path),
                escape_json(&error)
            );
        }
    };

    let Some(starcraft) = starcraft else {
        return format!(
            concat!(
                "{{",
                "\"schema\":\"lav_scr_focused_u32_watch_v1\",",
                "\"enabled\":true,",
                "\"status\":\"missing_starcraft_module\",",
                "\"source_path\":\"{}\",",
                "\"loaded_candidates\":{},",
                "\"direct_memory_reads\":false,",
                "\"pointer_dereferences\":false,",
                "\"candidates\":[]",
                "}}"
            ),
            escape_json(&path),
            candidates.len()
        );
    };

    let fingerprint = match module_fingerprint(starcraft) {
        Ok(fingerprint) => fingerprint,
        Err(error) => {
            return format!(
                concat!(
                    "{{",
                    "\"schema\":\"lav_scr_focused_u32_watch_v1\",",
                    "\"enabled\":true,",
                    "\"status\":\"fingerprint_error\",",
                    "\"source_path\":\"{}\",",
                    "\"loaded_candidates\":{},",
                    "\"error\":\"{}\",",
                    "\"direct_memory_reads\":false,",
                    "\"pointer_dereferences\":false,",
                    "\"candidates\":[]",
                    "}}"
                ),
                escape_json(&path),
                candidates.len(),
                escape_json(&error)
            );
        }
    };

    let mut rows = Vec::new();
    let mut read_count = 0usize;
    for candidate in candidates.iter().take(MAX_U32_WATCHLIST_CANDIDATES) {
        let row = focused_u32_watch_candidate_json(starcraft, &fingerprint.sections, candidate);
        if row.contains("\"status\":\"read\"") {
            read_count += 1;
        }
        rows.push(row);
    }

    format!(
        concat!(
            "{{",
            "\"schema\":\"lav_scr_focused_u32_watch_v1\",",
            "\"enabled\":true,",
            "\"status\":\"read\",",
            "\"source_path\":\"{}\",",
            "\"loaded_candidates\":{},",
            "\"printed_candidates\":{},",
            "\"read_count\":{},",
            "\"direct_memory_reads\":true,",
            "\"read_type\":\"direct_u32_le\",",
            "\"pointer_dereferences\":false,",
            "\"candidates\":{}",
            "}}"
        ),
        escape_json(&path),
        candidates.len(),
        rows.len(),
        read_count,
        json_array(&rows)
    )
}

fn focused_u32_watch_candidate_json(
    module: &ModuleInfo,
    sections: &[SectionInfo],
    candidate: &U32WatchCandidate,
) -> String {
    if !candidate
        .module_name
        .eq_ignore_ascii_case(module.name.as_str())
    {
        return focused_u32_watch_skip_json(candidate, "module_name_mismatch");
    }

    let Some(section) = find_section_for_rva(sections, candidate.rva, 4) else {
        return focused_u32_watch_skip_json(candidate, "rva_outside_readable_section");
    };
    if !candidate.section_name.is_empty()
        && !candidate
            .section_name
            .eq_ignore_ascii_case(section.name.as_str())
    {
        return focused_u32_watch_skip_json(candidate, "section_name_mismatch");
    }

    let Some(address) = module.base.checked_add(candidate.rva) else {
        return focused_u32_watch_skip_json(candidate, "address_overflow");
    };
    if !memory_region_readable(address, 4) {
        return focused_u32_watch_skip_json(candidate, "memory_region_not_readable");
    }
    let Some(value) = (unsafe { read_primitive_value(address, 4) }) else {
        return focused_u32_watch_skip_json(candidate, "read_failed");
    };

    format!(
        concat!(
            "{{",
            "\"module_name\":\"{}\",",
            "\"section_name\":\"{}\",",
            "\"rva\":\"{}\",",
            "\"address\":\"{}\",",
            "\"confidence\":\"{}\",",
            "\"resource_field\":\"{}\",",
            "\"status\":\"read\",",
            "\"ok\":true,",
            "\"value\":{},",
            "\"hex_value\":\"{}\",",
            "\"value_type\":\"u32_le\",",
            "\"read_now\":true,",
            "\"direct_memory_read\":true,",
            "\"pointer_dereference\":false",
            "}}"
        ),
        escape_json(&candidate.module_name),
        escape_json(&section.name),
        escape_json(&candidate.rva_text),
        hex_address(address),
        escape_json(&candidate.confidence),
        escape_json(&candidate.resource_field),
        value,
        hex_u32(value)
    )
}

fn focused_u32_watch_skip_json(candidate: &U32WatchCandidate, reason: &str) -> String {
    format!(
        concat!(
            "{{",
            "\"module_name\":\"{}\",",
            "\"section_name\":\"{}\",",
            "\"rva\":\"{}\",",
            "\"confidence\":\"{}\",",
            "\"resource_field\":\"{}\",",
            "\"status\":\"skipped\",",
            "\"ok\":false,",
            "\"error\":\"{}\",",
            "\"value_type\":\"u32_le\",",
            "\"read_now\":false,",
            "\"direct_memory_read\":false,",
            "\"pointer_dereference\":false",
            "}}"
        ),
        escape_json(&candidate.module_name),
        escape_json(&candidate.section_name),
        escape_json(&candidate.rva_text),
        escape_json(&candidate.confidence),
        escape_json(&candidate.resource_field),
        escape_json(reason)
    )
}

fn focused_u32_watch_signal_for_module(module: &ModuleInfo) -> Option<U32WatchSignalResult> {
    if !resolve_u32_watch_enabled() {
        return None;
    }
    let path = resolve_u32_watchlist_path();
    let candidates = load_u32_watch_candidates(&path).ok()?;
    if candidates.is_empty() {
        return None;
    }
    let fingerprint = module_fingerprint(module).ok()?;
    let mut observations = Vec::new();
    let mut read_count = 0usize;
    let mut nonzero_count = 0usize;
    let mut zero_count = 0usize;

    for candidate in candidates.iter().take(MAX_U32_WATCHLIST_CANDIDATES) {
        let observation =
            read_focused_u32_watch_observation(module, &fingerprint.sections, candidate);
        if observation.ok {
            read_count += 1;
            if observation.value == 0 {
                zero_count += 1;
            } else {
                nonzero_count += 1;
            }
        }
        observations.push(observation.json);
    }

    let threshold = resolve_u32_watch_active_threshold();
    let status = if read_count == 0 {
        "no_read_values"
    } else if nonzero_count >= threshold {
        "active"
    } else {
        "menu_like"
    };
    Some(U32WatchSignalResult {
        status: status.to_string(),
        read_count,
        nonzero_count,
        zero_count,
        threshold,
        observations,
    })
}

fn resource_watch_values_from_u32_watch() -> ResourceWatchValues {
    let mut values = ResourceWatchValues::default();
    if !resolve_u32_watch_enabled() {
        return values;
    }

    let modules = enumerate_process_modules(current_process_id());
    let Some(starcraft) = find_module(&modules, &["starcraft.exe", "broodwar.exe"]) else {
        return values;
    };
    let path = resolve_u32_watchlist_path();
    let Ok(candidates) = load_u32_watch_candidates(&path) else {
        return values;
    };
    let Ok(fingerprint) = module_fingerprint(starcraft) else {
        return values;
    };

    for candidate in candidates.iter().take(MAX_U32_WATCHLIST_CANDIDATES) {
        let is_minerals = candidate.resource_field.eq_ignore_ascii_case("minerals");
        let is_gas = candidate.resource_field.eq_ignore_ascii_case("gas");
        if !is_minerals && !is_gas {
            continue;
        }
        let observation =
            read_focused_u32_watch_observation(starcraft, &fingerprint.sections, candidate);
        if observation.ok && is_minerals {
            values.minerals = Some(observation.value);
        } else if observation.ok && is_gas {
            values.gas = Some(observation.value);
        }
        if values.minerals.is_some() && values.gas.is_some() {
            break;
        }
    }

    values
}

struct U32WatchObservation {
    ok: bool,
    value: u32,
    json: String,
}

fn read_focused_u32_watch_observation(
    module: &ModuleInfo,
    sections: &[SectionInfo],
    candidate: &U32WatchCandidate,
) -> U32WatchObservation {
    if !candidate
        .module_name
        .eq_ignore_ascii_case(module.name.as_str())
    {
        return focused_u32_watch_observation_error(candidate, "module_name_mismatch");
    }

    let Some(section) = find_section_for_rva(sections, candidate.rva, 4) else {
        return focused_u32_watch_observation_error(candidate, "rva_outside_readable_section");
    };
    if !candidate.section_name.is_empty()
        && !candidate
            .section_name
            .eq_ignore_ascii_case(section.name.as_str())
    {
        return focused_u32_watch_observation_error(candidate, "section_name_mismatch");
    }

    let Some(address) = module.base.checked_add(candidate.rva) else {
        return focused_u32_watch_observation_error(candidate, "address_overflow");
    };
    if !memory_region_readable(address, 4) {
        return focused_u32_watch_observation_error(candidate, "memory_region_not_readable");
    }
    let Some(value) = (unsafe { read_primitive_value(address, 4) }) else {
        return focused_u32_watch_observation_error(candidate, "read_failed");
    };

    U32WatchObservation {
        ok: true,
        value,
        json: format!(
            concat!(
                "{{",
                "\"name\":\"focused_u32_watch\",",
                "\"status\":\"read\",",
                "\"ok\":true,",
                "\"module_name\":\"{}\",",
                "\"section_name\":\"{}\",",
                "\"rva\":\"{}\",",
                "\"resource_field\":\"{}\",",
                "\"value\":{},",
                "\"hex_value\":\"{}\",",
                "\"confidence\":\"{}\",",
                "\"read_now\":true,",
                "\"direct_memory_read\":true,",
                "\"pointer_dereference\":false",
                "}}"
            ),
            escape_json(&candidate.module_name),
            escape_json(&section.name),
            escape_json(&candidate.rva_text),
            escape_json(&candidate.resource_field),
            value,
            hex_u32(value),
            escape_json(&candidate.confidence)
        ),
    }
}

fn focused_u32_watch_observation_error(
    candidate: &U32WatchCandidate,
    reason: &str,
) -> U32WatchObservation {
    U32WatchObservation {
        ok: false,
        value: 0,
        json: format!(
            concat!(
                "{{",
                "\"name\":\"focused_u32_watch\",",
                "\"status\":\"skipped\",",
                "\"ok\":false,",
                "\"module_name\":\"{}\",",
                "\"section_name\":\"{}\",",
                "\"rva\":\"{}\",",
                "\"resource_field\":\"{}\",",
                "\"error\":\"{}\",",
                "\"read_now\":false,",
                "\"direct_memory_read\":false,",
                "\"pointer_dereference\":false",
                "}}"
            ),
            escape_json(&candidate.module_name),
            escape_json(&candidate.section_name),
            escape_json(&candidate.rva_text),
            escape_json(&candidate.resource_field),
            escape_json(reason)
        ),
    }
}

fn offset_discovery_module_json(role: &str, module: Option<&ModuleInfo>) -> String {
    if !resolve_offset_discovery_enabled() {
        return format!(
            concat!(
                "{{",
                "\"role\":\"{}\",",
                "\"status\":\"disabled\",",
                "\"module\":null,",
                "\"ranges_scanned\":0,",
                "\"bytes_scanned\":0,",
                "\"candidates\":[]",
                "}}"
            ),
            escape_json(role)
        );
    }

    let Some(module) = module else {
        return format!(
            concat!(
                "{{",
                "\"role\":\"{}\",",
                "\"status\":\"missing_module\",",
                "\"module\":null,",
                "\"ranges_scanned\":0,",
                "\"bytes_scanned\":0,",
                "\"candidates\":[]",
                "}}"
            ),
            escape_json(role)
        );
    };

    match module_fingerprint(module) {
        Ok(fingerprint) => {
            let ranges = discovery_ranges_for_module(module, &fingerprint.sections);
            let bytes_scanned: usize = ranges.iter().map(|range| range.range_bytes).sum();
            let candidates = discover_offset_candidates(module, &ranges);
            let window_profiles = discovery_window_profiles_json(module, &fingerprint.sections);
            let focused_windows =
                discovery_focused_windows_json(role, module, &fingerprint.sections);
            format!(
                concat!(
                    "{{",
                    "\"role\":\"{}\",",
                    "\"status\":\"scanned\",",
                    "\"module\":{},",
                    "\"ranges_scanned\":{},",
                    "\"bytes_scanned\":{},",
                    "\"ranges\":{},",
                    "\"candidates\":{},",
                    "\"window_profiles\":{},",
                    "\"focused_window_drilldown\":{}",
                    "}}"
                ),
                escape_json(role),
                module_json(module),
                ranges.len(),
                bytes_scanned,
                discovery_ranges_json(module, &ranges),
                candidates,
                window_profiles,
                focused_windows
            )
        }
        Err(error) => format!(
            concat!(
                "{{",
                "\"role\":\"{}\",",
                "\"status\":\"fingerprint_error\",",
                "\"module\":{},",
                "\"ranges_scanned\":0,",
                "\"bytes_scanned\":0,",
                "\"error\":\"{}\",",
                "\"candidates\":[]",
                "}}"
            ),
            escape_json(role),
            module_json(module),
            escape_json(&error)
        ),
    }
}

fn discovery_ranges_for_module(
    module: &ModuleInfo,
    sections: &[SectionInfo],
) -> Vec<VerifiedScanRange> {
    let mut ranges = Vec::new();
    for section in sections {
        if ranges.len() >= MAX_SAFE_SCAN_RANGES {
            break;
        }
        if !section_is_discovery_eligible(section) {
            continue;
        }
        let Some(range) = verified_scan_range(module, section, MAX_DISCOVERY_RANGE_BYTES) else {
            continue;
        };
        if !memory_region_readable(range.start, range.range_bytes) {
            continue;
        }
        ranges.push(range);
    }
    ranges
}

fn section_is_discovery_eligible(section: &SectionInfo) -> bool {
    section_is_readable(section)
        && section_is_writable(section)
        && !section_is_executable(section)
        && section.name.eq_ignore_ascii_case(".data")
}

fn discover_offset_candidates(module: &ModuleInfo, ranges: &[VerifiedScanRange]) -> String {
    let mut small_u32 = Vec::new();
    let mut bool_u8 = Vec::new();
    let mut readable_pointer_u32 = Vec::new();

    for range in ranges {
        if small_u32.len() >= MAX_DISCOVERY_CANDIDATES_PER_KIND
            && bool_u8.len() >= MAX_DISCOVERY_CANDIDATES_PER_KIND
            && readable_pointer_u32.len() >= MAX_DISCOVERY_CANDIDATES_PER_KIND
        {
            break;
        }

        let bytes =
            unsafe { std::slice::from_raw_parts(range.start as *const u8, range.range_bytes) };
        for offset in 0..bytes.len() {
            let address = range.start + offset;
            if bool_u8.len() < MAX_DISCOVERY_CANDIDATES_PER_KIND {
                let value = bytes[offset];
                if value == 1 {
                    bool_u8.push(discovery_candidate_json(
                        module,
                        range,
                        address,
                        offset,
                        "bool_u8_true",
                        value as u32,
                        "Byte is 1 inside a writable state section; zero values are suppressed.",
                    ));
                }
            }

            if offset % SCAN_ALIGNMENT_BYTES != 0 || offset + 4 > bytes.len() {
                continue;
            }

            let value = u32::from_le_bytes([
                bytes[offset],
                bytes[offset + 1],
                bytes[offset + 2],
                bytes[offset + 3],
            ]);
            if small_u32.len() < MAX_DISCOVERY_CANDIDATES_PER_KIND && (1..=12).contains(&value) {
                small_u32.push(discovery_candidate_json(
                    module,
                    range,
                    address,
                    offset,
                    "small_u32_nonzero",
                    value,
                    "Aligned u32 is in the nonzero small player/state-id range.",
                ));
            }
            if readable_pointer_u32.len() < MAX_DISCOVERY_CANDIDATES_PER_KIND
                && pointer_value_plausible(value)
            {
                readable_pointer_u32.push(discovery_candidate_json(
                    module,
                    range,
                    address,
                    offset,
                    "readable_pointer_u32",
                    value,
                    "Aligned u32 points at a readable region; value was not dereferenced.",
                ));
            }
        }
    }

    format!(
        concat!(
            "{{",
            "\"small_u32_nonzero\":{},",
            "\"bool_u8_true\":{},",
            "\"readable_pointer_u32\":{}",
            "}}"
        ),
        json_array(&small_u32),
        json_array(&bool_u8),
        json_array(&readable_pointer_u32)
    )
}

fn discovery_window_profiles_json(module: &ModuleInfo, sections: &[SectionInfo]) -> String {
    let mut profiles = Vec::new();
    let module_end = match module.base.checked_add(module.size as usize) {
        Some(value) => value,
        None => return "[]".to_string(),
    };

    for section in sections {
        if profiles.len() >= MAX_DISCOVERY_WINDOW_PROFILES_PER_MODULE {
            break;
        }
        if !section_is_discovery_eligible(section) {
            continue;
        }

        let section_span = section.virtual_size.max(section.raw_size) as usize;
        if section_span == 0 {
            continue;
        }
        let Some(section_start) = module.base.checked_add(section.virtual_address as usize) else {
            continue;
        };
        let Some(section_end) = section_start.checked_add(section_span) else {
            continue;
        };
        if section_start < module.base || section_end > module_end {
            continue;
        }

        let mut offset = 0usize;
        while offset < section_span && profiles.len() < MAX_DISCOVERY_WINDOW_PROFILES_PER_MODULE {
            let Some(window_start) = section_start.checked_add(offset) else {
                break;
            };
            let remaining = section_span.saturating_sub(offset);
            let window_bytes = remaining.min(DISCOVERY_WINDOW_BYTES);
            if window_bytes == 0 {
                break;
            }
            let Some(window_end) = window_start.checked_add(window_bytes) else {
                break;
            };
            if window_end > section_end || window_end > module_end {
                break;
            }
            if memory_region_readable(window_start, window_bytes) {
                let bytes =
                    unsafe { std::slice::from_raw_parts(window_start as *const u8, window_bytes) };
                profiles.push(discovery_window_profile_json(
                    module,
                    section,
                    profiles.len(),
                    window_start,
                    offset,
                    bytes,
                ));
            }
            offset = offset.saturating_add(DISCOVERY_WINDOW_BYTES);
        }
    }

    json_array(&profiles)
}

fn discovery_window_profile_json(
    module: &ModuleInfo,
    section: &SectionInfo,
    window_index: usize,
    window_start: usize,
    section_offset: usize,
    bytes: &[u8],
) -> String {
    let nonzero_bytes = bytes.iter().filter(|byte| **byte != 0).count();
    let bool_u8_true_count = bytes.iter().filter(|byte| **byte == 1).count();
    let mut small_u32_nonzero_count = 0usize;
    let mut readable_pointer_u32_count = 0usize;

    if bytes.len() >= 4 {
        for offset in (0..=bytes.len() - 4).step_by(SCAN_ALIGNMENT_BYTES) {
            let value = u32::from_le_bytes([
                bytes[offset],
                bytes[offset + 1],
                bytes[offset + 2],
                bytes[offset + 3],
            ]);
            if (1..=12).contains(&value) {
                small_u32_nonzero_count += 1;
            }
            if pointer_value_plausible(value) {
                readable_pointer_u32_count += 1;
            }
        }
    }

    format!(
        concat!(
            "{{",
            "\"module_name\":\"{}\",",
            "\"section_name\":\"{}\",",
            "\"window_index\":{},",
            "\"rva_start\":\"{}\",",
            "\"memory_start\":\"{}\",",
            "\"section_offset\":{},",
            "\"bytes\":{},",
            "\"fnv1a64\":\"{}\",",
            "\"nonzero_bytes\":{},",
            "\"small_u32_nonzero_count\":{},",
            "\"bool_u8_true_count\":{},",
            "\"readable_pointer_u32_count\":{},",
            "\"bytes_hex\":\"{}\",",
            "\"read_now\":true,",
            "\"direct_memory_read\":true,",
            "\"pointer_dereference\":false",
            "}}"
        ),
        escape_json(&module.name),
        escape_json(&section.name),
        window_index,
        hex_address(window_start.saturating_sub(module.base)),
        hex_address(window_start),
        section_offset,
        bytes.len(),
        hex_u64(fnv1a64(bytes)),
        nonzero_bytes,
        small_u32_nonzero_count,
        bool_u8_true_count,
        readable_pointer_u32_count,
        hex_bytes(bytes)
    )
}

fn discovery_focused_windows_json(
    role: &str,
    module: &ModuleInfo,
    sections: &[SectionInfo],
) -> String {
    if role != "starcraft_module" {
        return "[]".to_string();
    }

    let mut windows = Vec::new();
    let mut seen_rvas = Vec::new();
    for rva in focused_seed_window_rvas_for_role(role) {
        if seen_rvas.len() >= MAX_DISCOVERY_FOCUSED_WINDOWS_PER_MODULE {
            break;
        }
        push_discovery_focused_window(
            module,
            sections,
            *rva,
            "seed_rva",
            &mut seen_rvas,
            &mut windows,
        );
    }

    if resolve_resource_focused_scan_enabled() {
        push_resource_focused_windows(
            module,
            sections,
            resolve_resource_focused_start_window(),
            resolve_resource_focused_window_limit(),
            &mut seen_rvas,
            &mut windows,
        );
    }

    json_array(&windows)
}

fn focused_seed_window_rvas_for_role(role: &str) -> &'static [u32] {
    if role == "starcraft_module" {
        &[
            0x00B2A000, 0x00B34000, 0x00B30000, 0x00B35000, 0x00B04000, 0x00B3C000,
        ]
    } else {
        &[]
    }
}

fn push_discovery_focused_window(
    module: &ModuleInfo,
    sections: &[SectionInfo],
    rva_start: u32,
    source: &str,
    seen_rvas: &mut Vec<u32>,
    windows: &mut Vec<String>,
) {
    if seen_rvas.contains(&rva_start) {
        return;
    }
    let Some(window_json) = discovery_focused_window_json(module, sections, rva_start, source)
    else {
        return;
    };
    seen_rvas.push(rva_start);
    windows.push(window_json);
}

fn push_resource_focused_windows(
    module: &ModuleInfo,
    sections: &[SectionInfo],
    start_window_index: usize,
    limit: usize,
    seen_rvas: &mut Vec<u32>,
    windows: &mut Vec<String>,
) {
    if limit == 0 {
        return;
    }
    let mut added = 0usize;
    let module_end = match module.base.checked_add(module.size as usize) {
        Some(value) => value,
        None => return,
    };
    let mut windows_to_skip = start_window_index;

    for section in sections {
        if added >= limit {
            break;
        }
        if !section_is_discovery_eligible(section) {
            continue;
        }
        let section_span = section.virtual_size.max(section.raw_size) as usize;
        if section_span < DISCOVERY_WINDOW_BYTES {
            continue;
        }
        let Some(section_start) = module.base.checked_add(section.virtual_address as usize) else {
            continue;
        };
        let Some(section_end) = section_start.checked_add(section_span) else {
            continue;
        };
        if section_start < module.base || section_end > module_end {
            continue;
        }

        let section_windows = section_span / DISCOVERY_WINDOW_BYTES;
        if windows_to_skip >= section_windows {
            windows_to_skip = windows_to_skip.saturating_sub(section_windows);
            continue;
        }

        let mut offset = 0usize;
        if windows_to_skip > 0 {
            offset = windows_to_skip.saturating_mul(DISCOVERY_WINDOW_BYTES);
            windows_to_skip = 0;
        }
        while offset + DISCOVERY_WINDOW_BYTES <= section_span && added < limit {
            let Some(rva_start) = (section.virtual_address as usize)
                .checked_add(offset)
                .and_then(|value| u32::try_from(value).ok())
            else {
                break;
            };
            let before_count = windows.len();
            push_discovery_focused_window(
                module,
                sections,
                rva_start,
                "resource_sweep",
                seen_rvas,
                windows,
            );
            if windows.len() > before_count {
                added += 1;
            }
            offset = offset.saturating_add(DISCOVERY_WINDOW_BYTES);
        }
    }
}

fn discovery_focused_window_json(
    module: &ModuleInfo,
    sections: &[SectionInfo],
    rva_start: u32,
    source: &str,
) -> Option<String> {
    let section = section_containing_rva(sections, rva_start, DISCOVERY_WINDOW_BYTES)?;
    if !section_is_discovery_eligible(section) {
        return None;
    }

    let module_end = module.base.checked_add(module.size as usize)?;
    let window_start = module.base.checked_add(rva_start as usize)?;
    let window_end = window_start.checked_add(DISCOVERY_WINDOW_BYTES)?;
    let section_start = module.base.checked_add(section.virtual_address as usize)?;
    let section_end =
        section_start.checked_add(section.virtual_size.max(section.raw_size) as usize)?;
    if window_start < module.base || window_end > module_end || window_end > section_end {
        return None;
    }
    if !memory_region_readable(window_start, DISCOVERY_WINDOW_BYTES) {
        return None;
    }

    let bytes =
        unsafe { std::slice::from_raw_parts(window_start as *const u8, DISCOVERY_WINDOW_BYTES) };
    let mut chunks = Vec::new();
    let mut offset = 0usize;
    while offset < bytes.len() {
        let chunk_bytes = (bytes.len() - offset).min(DISCOVERY_DRILLDOWN_CHUNK_BYTES);
        let chunk_start = window_start.checked_add(offset)?;
        chunks.push(discovery_focused_chunk_json(
            module,
            section,
            rva_start,
            offset,
            chunk_start,
            &bytes[offset..offset + chunk_bytes],
        ));
        offset = offset.saturating_add(DISCOVERY_DRILLDOWN_CHUNK_BYTES);
    }

    Some(format!(
        concat!(
            "{{",
            "\"schema\":\"lav_scr_focused_window_drilldown_v2\",",
            "\"compat_schema\":\"lav_scr_focused_window_drilldown_v1\",",
            "\"module_name\":\"{}\",",
            "\"section_name\":\"{}\",",
            "\"source\":\"{}\",",
            "\"rva_start\":\"{}\",",
            "\"memory_start\":\"{}\",",
            "\"bytes\":{},",
            "\"chunk_bytes\":{},",
            "\"chunk_count\":{},",
            "\"fnv1a64\":\"{}\",",
            "\"chunks\":{}",
            "}}"
        ),
        escape_json(&module.name),
        escape_json(&section.name),
        escape_json(source),
        hex_u32(rva_start),
        hex_address(window_start),
        bytes.len(),
        DISCOVERY_DRILLDOWN_CHUNK_BYTES,
        chunks.len(),
        hex_u64(fnv1a64(bytes)),
        json_array(&chunks)
    ))
}

fn discovery_focused_chunk_json(
    module: &ModuleInfo,
    section: &SectionInfo,
    window_rva_start: u32,
    window_offset: usize,
    chunk_start: usize,
    bytes: &[u8],
) -> String {
    let nonzero_bytes = bytes.iter().filter(|byte| **byte != 0).count();
    let bool_u8_true_count = bytes.iter().filter(|byte| **byte == 1).count();
    let mut small_u32_nonzero_count = 0usize;
    let mut readable_pointer_u32_count = 0usize;
    if bytes.len() >= 4 {
        for offset in (0..=bytes.len() - 4).step_by(SCAN_ALIGNMENT_BYTES) {
            let value = u32::from_le_bytes([
                bytes[offset],
                bytes[offset + 1],
                bytes[offset + 2],
                bytes[offset + 3],
            ]);
            if (1..=12).contains(&value) {
                small_u32_nonzero_count += 1;
            }
            if pointer_value_plausible(value) {
                readable_pointer_u32_count += 1;
            }
        }
    }

    format!(
        concat!(
            "{{",
            "\"module_name\":\"{}\",",
            "\"section_name\":\"{}\",",
            "\"window_rva_start\":\"{}\",",
            "\"chunk_rva_start\":\"{}\",",
            "\"memory_start\":\"{}\",",
            "\"window_offset\":{},",
            "\"bytes\":{},",
            "\"fnv1a64\":\"{}\",",
            "\"nonzero_bytes\":{},",
            "\"small_u32_nonzero_count\":{},",
            "\"bool_u8_true_count\":{},",
            "\"readable_pointer_u32_count\":{},",
            "\"bytes_hex\":\"{}\",",
            "\"read_now\":true,",
            "\"direct_memory_read\":true,",
            "\"pointer_dereference\":false",
            "}}"
        ),
        escape_json(&module.name),
        escape_json(&section.name),
        hex_u32(window_rva_start),
        hex_address(chunk_start.saturating_sub(module.base)),
        hex_address(chunk_start),
        window_offset,
        bytes.len(),
        hex_u64(fnv1a64(bytes)),
        nonzero_bytes,
        small_u32_nonzero_count,
        bool_u8_true_count,
        readable_pointer_u32_count,
        hex_bytes(bytes)
    )
}

fn section_containing_rva(
    sections: &[SectionInfo],
    rva_start: u32,
    byte_len: usize,
) -> Option<&SectionInfo> {
    let rva_start = rva_start as usize;
    let rva_end = rva_start.checked_add(byte_len)?;
    sections.iter().find(|section| {
        let section_start = section.virtual_address as usize;
        let section_end =
            section_start.saturating_add(section.virtual_size.max(section.raw_size) as usize);
        rva_start >= section_start && rva_end <= section_end
    })
}

fn discovery_candidate_json(
    module: &ModuleInfo,
    range: &VerifiedScanRange,
    address: usize,
    range_offset: usize,
    kind: &str,
    value: u32,
    note: &str,
) -> String {
    format!(
        concat!(
            "{{",
            "\"kind\":\"{}\",",
            "\"module_name\":\"{}\",",
            "\"section_name\":\"{}\",",
            "\"address\":\"{}\",",
            "\"rva\":\"{}\",",
            "\"range_offset\":{},",
            "\"value\":{},",
            "\"hex_value\":\"{}\",",
            "\"read_now\":true,",
            "\"direct_memory_read\":true,",
            "\"pointer_dereference\":false,",
            "\"candidate_only\":true,",
            "\"note\":\"{}\"",
            "}}"
        ),
        escape_json(kind),
        escape_json(&module.name),
        escape_json(&range.section_name),
        hex_address(address),
        hex_address(address.saturating_sub(module.base)),
        range_offset,
        value,
        hex_u32(value),
        escape_json(note)
    )
}

fn discovery_ranges_json(module: &ModuleInfo, ranges: &[VerifiedScanRange]) -> String {
    let mut items = Vec::new();
    for range in ranges {
        items.push(format!(
            concat!(
                "{{",
                "\"module_name\":\"{}\",",
                "\"section_name\":\"{}\",",
                "\"memory_start\":\"{}\",",
                "\"memory_end\":\"{}\",",
                "\"section_memory_start\":\"{}\",",
                "\"section_memory_end\":\"{}\",",
                "\"range_bytes\":{},",
                "\"writable\":{},",
                "\"read_now\":true,",
                "\"direct_memory_read\":true,",
                "\"pointer_dereference\":false",
                "}}"
            ),
            escape_json(&module.name),
            escape_json(&range.section_name),
            hex_address(range.start),
            hex_address(range.end),
            hex_address(range.section_start),
            hex_address(range.section_end),
            range.range_bytes,
            bool_text(range.writable)
        ));
    }
    json_array(&items)
}

fn detect_initialize_in_game_state(api_reported_in_game: bool) -> InGameDetectorResult {
    if !resolve_in_game_detector_enabled() {
        return InGameDetectorResult {
            in_game: false,
            json: concat!(
                "{",
                "\"schema\":\"lav_in_game_detector_v1\",",
                "\"source\":\"initialize_legacy_primitive_reads\",",
                "\"enabled\":false,",
                "\"in_game\":false,",
                "\"confidence\":\"none\",",
                "\"reason\":\"Disabled by LAV_SAMASE_INGAME_DETECTOR.\",",
                "\"api_reported_in_game\":false,",
                "\"direct_memory_reads\":false,",
                "\"direct_primitive_reads\":false,",
                "\"read_now\":false,",
                "\"pointer_dereferences\":false,",
                "\"observations\":[]",
                "}"
            )
            .to_string(),
        };
    }

    let modules = enumerate_process_modules(current_process_id());
    let Some(starcraft) = find_module(&modules, &["starcraft.exe", "broodwar.exe"]) else {
        return InGameDetectorResult {
            in_game: false,
            json: concat!(
                "{",
                "\"schema\":\"lav_in_game_detector_v1\",",
                "\"source\":\"initialize_legacy_primitive_reads\",",
                "\"enabled\":true,",
                "\"in_game\":false,",
                "\"confidence\":\"none\",",
                "\"reason\":\"StarCraft module is not available for bounded primitive reads.\",",
                "\"api_reported_in_game\":false,",
                "\"direct_memory_reads\":false,",
                "\"direct_primitive_reads\":false,",
                "\"read_now\":false,",
                "\"pointer_dereferences\":false,",
                "\"observations\":[]",
                "}"
            )
            .to_string(),
        };
    };

    if let Some(watch_signal) = focused_u32_watch_signal_for_module(starcraft) {
        if watch_signal.status == "active" {
            return InGameDetectorResult {
                in_game: true,
                json: format!(
                    concat!(
                        "{{",
                        "\"schema\":\"lav_in_game_detector_v2\",",
                        "\"compat_schema\":\"lav_in_game_detector_v1\",",
                        "\"source\":\"focused_u32_watch\",",
                        "\"enabled\":true,",
                        "\"in_game\":true,",
                        "\"confidence\":\"medium\",",
                        "\"reason\":\"Focused u32 watch values crossed the active nonzero threshold.\",",
                        "\"api_reported_in_game\":{},",
                        "\"direct_memory_reads\":true,",
                        "\"direct_primitive_reads\":true,",
                        "\"read_now\":true,",
                        "\"pointer_dereferences\":false,",
                        "\"watch_signal\":{{",
                        "\"status\":\"{}\",",
                        "\"read_count\":{},",
                        "\"nonzero_count\":{},",
                        "\"zero_count\":{},",
                        "\"threshold\":{}",
                        "}},",
                        "\"observations\":{}",
                        "}}"
                    ),
                    bool_text(api_reported_in_game),
                    escape_json(&watch_signal.status),
                    watch_signal.read_count,
                    watch_signal.nonzero_count,
                    watch_signal.zero_count,
                    watch_signal.threshold,
                    json_array(&watch_signal.observations)
                ),
            };
        }
    }

    let fingerprint = module_fingerprint(starcraft);
    let observations = match fingerprint {
        Ok(fingerprint) => legacy_in_game_candidates()
            .iter()
            .map(|candidate| read_legacy_candidate(starcraft, &fingerprint.sections, candidate))
            .collect::<Vec<_>>(),
        Err(error) => {
            let json = format!(
                concat!(
                    "{{",
                    "\"name\":\"module_fingerprint\",",
                    "\"status\":\"fingerprint_error\",",
                    "\"ok\":false,",
                    "\"read_now\":false,",
                    "\"direct_memory_read\":false,",
                    "\"dereference_now\":false,",
                    "\"error\":\"{}\"",
                    "}}"
                ),
                escape_json(&error)
            );
            vec![PrimitiveReadResult {
                name: "module_fingerprint",
                ok: false,
                value: None,
                json,
            }]
        }
    };

    let local_player_ok = primitive_value(&observations, "LocalPlayerId")
        .map(|value| value < 12)
        .unwrap_or(false);
    let unique_player_ok = primitive_value(&observations, "LocalUniquePlayerId")
        .map(|value| value < 12)
        .unwrap_or(false);
    let single_player_ok = primitive_value(&observations, "IsMultiplayer")
        .map(|value| value == 0)
        .unwrap_or(false);
    let not_replay_ok = primitive_value(&observations, "IsReplay")
        .map(|value| value == 0)
        .unwrap_or(false);
    let main_state_ok = primitive_value(&observations, "ScMainState")
        .map(|value| (1..=20).contains(&value))
        .unwrap_or(false);
    let active_unit_pointer_ok = primitive_value(&observations, "FirstActiveUnit")
        .map(pointer_value_plausible)
        .unwrap_or(false);

    let in_game = local_player_ok
        && unique_player_ok
        && single_player_ok
        && not_replay_ok
        && (main_state_ok || active_unit_pointer_ok);
    let direct_reads = observations.iter().any(|observation| observation.ok);
    let confidence = if in_game && active_unit_pointer_ok {
        "high"
    } else if in_game {
        "medium"
    } else {
        "none"
    };
    let reason = if in_game {
        "Legacy primitive candidates passed player, single-player, replay, and game-state gates."
    } else if !direct_reads {
        "No legacy primitive candidate could be read safely."
    } else {
        "Legacy primitive candidates were readable but did not pass the in-game gate."
    };

    InGameDetectorResult {
        in_game,
        json: format!(
            concat!(
                "{{",
                "\"schema\":\"lav_in_game_detector_v1\",",
                "\"source\":\"initialize_legacy_primitive_reads\",",
                "\"offset_source\":\"samase_1161_shim_offsets\",",
                "\"enabled\":true,",
                "\"in_game\":{},",
                "\"confidence\":\"{}\",",
                "\"reason\":\"{}\",",
                "\"api_reported_in_game\":{},",
                "\"direct_memory_reads\":{},",
                "\"direct_primitive_reads\":{},",
                "\"read_now\":{},",
                "\"pointer_dereferences\":false,",
                "\"gates\":{{",
                "\"local_player_ok\":{},",
                "\"unique_player_ok\":{},",
                "\"single_player_ok\":{},",
                "\"not_replay_ok\":{},",
                "\"main_state_ok\":{},",
                "\"active_unit_pointer_ok\":{}",
                "}},",
                "\"observations\":[{}]",
                "}}"
            ),
            bool_text(in_game),
            confidence,
            escape_json(reason),
            bool_text(api_reported_in_game),
            bool_text(direct_reads),
            bool_text(direct_reads),
            bool_text(direct_reads),
            bool_text(local_player_ok),
            bool_text(unique_player_ok),
            bool_text(single_player_ok),
            bool_text(not_replay_ok),
            bool_text(main_state_ok),
            bool_text(active_unit_pointer_ok),
            primitive_reads_json(&observations)
        ),
    }
}

fn legacy_in_game_candidates() -> [FixedReadCandidate; 6] {
    [
        FixedReadCandidate {
            name: "ScMainState",
            legacy_address: 0x0059_6904,
            size: 4,
            note: "Old BW/Samase shim main-state primitive; used only as a weak gate.",
        },
        FixedReadCandidate {
            name: "LocalPlayerId",
            legacy_address: 0x0051_2684,
            size: 4,
            note: "Old BW/Samase shim local player id primitive.",
        },
        FixedReadCandidate {
            name: "LocalUniquePlayerId",
            legacy_address: 0x0051_2688,
            size: 4,
            note: "Old BW/Samase shim unique local player id primitive.",
        },
        FixedReadCandidate {
            name: "IsMultiplayer",
            legacy_address: 0x0057_F0B4,
            size: 1,
            note: "Old BW/Samase shim multiplayer flag primitive.",
        },
        FixedReadCandidate {
            name: "IsReplay",
            legacy_address: 0x006D_0F14,
            size: 4,
            note: "Old BW/Samase shim replay flag primitive.",
        },
        FixedReadCandidate {
            name: "FirstActiveUnit",
            legacy_address: 0x0062_8430,
            size: 4,
            note: "Old BW/Samase shim active-unit pointer value; value is not dereferenced.",
        },
    ]
}

fn read_legacy_candidate(
    module: &ModuleInfo,
    sections: &[SectionInfo],
    candidate: &FixedReadCandidate,
) -> PrimitiveReadResult {
    let Some(rva) = candidate.legacy_address.checked_sub(LEGACY_BW_BASE) else {
        return primitive_read_error(candidate, "legacy address is below expected BW base");
    };
    let Some(address) = module.base.checked_add(rva) else {
        return primitive_read_error(candidate, "candidate address overflowed module base");
    };
    let Some(end) = address.checked_add(candidate.size) else {
        return primitive_read_error(candidate, "candidate address range overflowed");
    };
    let module_end = module.base.saturating_add(module.size as usize);
    if end > module_end {
        return primitive_read_error(candidate, "candidate address is outside module bounds");
    }

    let section = find_section_for_rva(sections, rva, candidate.size);
    let Some(section) = section else {
        return primitive_read_error(candidate, "candidate address is outside parsed PE sections");
    };
    if section_is_executable(section) {
        return primitive_read_error(
            candidate,
            "candidate address is in an executable section; refusing primitive state read",
        );
    }
    if !section_scan_name_allowed(&section.name) {
        return primitive_read_error(
            candidate,
            "candidate address is outside the allowed non-executable state sections",
        );
    }
    let page_readable = memory_region_readable(address, candidate.size);
    if !page_readable {
        return primitive_read_error(candidate, "candidate page is not readable");
    }

    let value = unsafe { read_primitive_value(address, candidate.size) };
    let Some(value) = value else {
        return primitive_read_error(candidate, "unsupported primitive read size");
    };

    let json = format!(
        concat!(
            "{{",
            "\"name\":\"{}\",",
            "\"status\":\"read\",",
            "\"ok\":true,",
            "\"source\":\"samase_1161_shim_offsets\",",
            "\"legacy_address\":\"{}\",",
            "\"rva\":\"{}\",",
            "\"address\":\"{}\",",
            "\"size\":{},",
            "\"value\":{},",
            "\"hex_value\":\"{}\",",
            "\"section_name\":\"{}\",",
            "\"section_readable\":{},",
            "\"section_writable\":{},",
            "\"section_executable\":{},",
            "\"page_readable\":true,",
            "\"read_now\":true,",
            "\"direct_memory_read\":true,",
            "\"dereference_now\":false,",
            "\"pointer_dereference\":false,",
            "\"note\":\"{}\"",
            "}}"
        ),
        escape_json(candidate.name),
        hex_address(candidate.legacy_address),
        hex_address(rva),
        hex_address(address),
        candidate.size,
        value,
        hex_u32(value),
        escape_json(&section.name),
        bool_text(section_is_readable(section)),
        bool_text(section_is_writable(section)),
        bool_text(section_is_executable(section)),
        escape_json(candidate.note)
    );

    PrimitiveReadResult {
        name: candidate.name,
        ok: true,
        value: Some(value),
        json,
    }
}

fn primitive_read_error(candidate: &FixedReadCandidate, reason: &str) -> PrimitiveReadResult {
    PrimitiveReadResult {
        name: candidate.name,
        ok: false,
        value: None,
        json: format!(
            concat!(
                "{{",
                "\"name\":\"{}\",",
                "\"status\":\"skipped\",",
                "\"ok\":false,",
                "\"source\":\"samase_1161_shim_offsets\",",
                "\"legacy_address\":\"{}\",",
                "\"size\":{},",
                "\"read_now\":false,",
                "\"direct_memory_read\":false,",
                "\"dereference_now\":false,",
                "\"pointer_dereference\":false,",
                "\"error\":\"{}\",",
                "\"note\":\"{}\"",
                "}}"
            ),
            escape_json(candidate.name),
            hex_address(candidate.legacy_address),
            candidate.size,
            escape_json(reason),
            escape_json(candidate.note)
        ),
    }
}

fn primitive_value(reads: &[PrimitiveReadResult], name: &str) -> Option<u32> {
    reads
        .iter()
        .find(|read| read.name == name && read.ok)
        .and_then(|read| read.value)
}

fn primitive_reads_json(reads: &[PrimitiveReadResult]) -> String {
    let mut output = String::new();
    for (index, read) in reads.iter().enumerate() {
        if index > 0 {
            output.push(',');
        }
        output.push_str(&read.json);
    }
    output
}

fn pointer_value_plausible(value: u32) -> bool {
    let address = value as usize;
    address >= 0x0001_0000 && address < 0x7FFF_0000 && memory_region_readable(address, 4)
}

fn find_section_for_rva<'a>(
    sections: &'a [SectionInfo],
    rva: usize,
    size: usize,
) -> Option<&'a SectionInfo> {
    let end = rva.checked_add(size)?;
    sections.iter().find(|section| {
        let start = section.virtual_address as usize;
        let span = section.virtual_size.max(section.raw_size) as usize;
        let Some(section_end) = start.checked_add(span) else {
            return false;
        };
        rva >= start && end <= section_end && section_is_readable(section)
    })
}

fn memory_region_readable(address: usize, size: usize) -> bool {
    let Some(end) = address.checked_add(size) else {
        return false;
    };
    let mut info = MemoryBasicInformation {
        base_address: ptr::null_mut(),
        allocation_base: ptr::null_mut(),
        allocation_protect: 0,
        region_size: 0,
        state: 0,
        protect: 0,
        type_: 0,
    };
    let result = unsafe {
        VirtualQuery(
            address as *const c_void,
            &mut info,
            mem::size_of::<MemoryBasicInformation>(),
        )
    };
    if result == 0 || info.state != MEM_COMMIT || !page_protect_readable(info.protect) {
        return false;
    }
    let region_start = info.base_address as usize;
    let Some(region_end) = region_start.checked_add(info.region_size) else {
        return false;
    };
    address >= region_start && end <= region_end
}

fn page_protect_readable(protect: u32) -> bool {
    if protect & (PAGE_NOACCESS | PAGE_GUARD) != 0 {
        return false;
    }
    protect
        & (PAGE_READONLY
            | PAGE_READWRITE
            | PAGE_WRITECOPY
            | PAGE_EXECUTE_READ
            | PAGE_EXECUTE_READWRITE
            | PAGE_EXECUTE_WRITECOPY)
        != 0
}

unsafe fn read_primitive_value(address: usize, size: usize) -> Option<u32> {
    match size {
        1 => Some(ptr::read_unaligned(address as *const u8) as u32),
        4 => Some(ptr::read_unaligned(address as *const u32)),
        _ => None,
    }
}

fn current_process_id() -> u32 {
    unsafe { GetCurrentProcessId() }
}

fn enumerate_process_modules(process_id: u32) -> Vec<ModuleInfo> {
    let snapshot =
        unsafe { CreateToolhelp32Snapshot(TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, process_id) };
    if snapshot == invalid_handle_value() || snapshot.is_null() {
        return Vec::new();
    }

    let mut modules = Vec::new();
    let mut entry = ModuleEntry32W {
        dw_size: mem::size_of::<ModuleEntry32W>() as u32,
        th32_module_id: 0,
        th32_process_id: 0,
        glblcnt_usage: 0,
        proccnt_usage: 0,
        mod_base_addr: ptr::null_mut(),
        mod_base_size: 0,
        h_module: ptr::null_mut(),
        sz_module: [0; MAX_MODULE_NAME_CHARS],
        sz_exe_path: [0; MAX_PATH_CHARS],
    };

    let mut ok = unsafe { Module32FirstW(snapshot, &mut entry) } != 0;
    while ok && modules.len() < MAX_MODULES {
        modules.push(module_entry_to_info(&entry));
        ok = unsafe { Module32NextW(snapshot, &mut entry) } != 0;
    }

    unsafe {
        CloseHandle(snapshot);
    }
    modules
}

fn invalid_handle_value() -> Handle {
    (-1isize) as Handle
}

fn module_entry_to_info(entry: &ModuleEntry32W) -> ModuleInfo {
    ModuleInfo {
        name: wide_string(&entry.sz_module),
        path: wide_string(&entry.sz_exe_path),
        base: entry.mod_base_addr as usize,
        size: entry.mod_base_size,
    }
}

fn wide_string(value: &[u16]) -> String {
    let len = value.iter().position(|ch| *ch == 0).unwrap_or(value.len());
    OsString::from_wide(&value[..len])
        .to_string_lossy()
        .into_owned()
}

fn find_module<'a>(modules: &'a [ModuleInfo], names: &[&str]) -> Option<&'a ModuleInfo> {
    modules.iter().find(|module| {
        names
            .iter()
            .any(|name| module.name.eq_ignore_ascii_case(name))
    })
}

fn find_samase_temp_module(modules: &[ModuleInfo]) -> Option<&ModuleInfo> {
    modules.iter().find(|module| {
        let name = module.name.to_ascii_lowercase();
        let path = module.path.to_ascii_lowercase().replace('/', "\\");
        (name.starts_with("samase_") && name.ends_with(".dll"))
            || path.contains("\\temp\\samase\\samase_")
    })
}

fn module_fingerprint_option_json(role: &str, module: Option<&ModuleInfo>) -> String {
    let Some(module) = module else {
        return format!(
            concat!(
                "{{",
                "\"role\":\"{}\",",
                "\"status\":\"missing\",",
                "\"module\":null,",
                "\"fingerprint\":null,",
                "\"sections\":[]",
                "}}"
            ),
            escape_json(role)
        );
    };

    match module_fingerprint(module) {
        Ok(fingerprint) => format!(
            concat!(
                "{{",
                "\"role\":\"{}\",",
                "\"status\":\"fingerprinted\",",
                "\"module\":{},",
                "\"fingerprint\":{},",
                "\"sections\":{}",
                "}}"
            ),
            escape_json(role),
            module_json(module),
            fingerprint_json(&fingerprint),
            module_sections_json(module, &fingerprint.sections)
        ),
        Err(error) => format!(
            concat!(
                "{{",
                "\"role\":\"{}\",",
                "\"status\":\"fingerprint_error\",",
                "\"module\":{},",
                "\"fingerprint\":{{\"error\":\"{}\"}},",
                "\"sections\":[]",
                "}}"
            ),
            escape_json(role),
            module_json(module),
            escape_json(&error)
        ),
    }
}

fn module_fingerprint(module: &ModuleInfo) -> Result<ModuleFingerprint, String> {
    let data = fs::read(&module.path)
        .map_err(|error| format!("failed to read module file {}: {}", module.path, error))?;
    let metadata = fs::metadata(&module.path).ok();
    let file_size = metadata
        .as_ref()
        .map(|metadata| metadata.len())
        .unwrap_or(data.len() as u64);
    let modified_unix = metadata
        .and_then(|metadata| metadata.modified().ok())
        .and_then(|modified| modified.duration_since(UNIX_EPOCH).ok())
        .map(|duration| duration.as_secs())
        .unwrap_or(0);

    let e_lfanew = read_u32(&data, 0x3C)? as usize;
    if data.get(0..2) != Some(b"MZ") {
        return Err("missing MZ header".to_string());
    }
    if read_slice(&data, e_lfanew, 4)? != b"PE\0\0" {
        return Err("missing PE signature".to_string());
    }

    let coff = checked_offset(e_lfanew, 4)?;
    let machine = read_u16(&data, coff)?;
    let pe_section_count = read_u16(&data, checked_offset(coff, 2)?)?;
    let pe_time_date_stamp = read_u32(&data, checked_offset(coff, 4)?)?;
    let optional_header_size = read_u16(&data, checked_offset(coff, 16)?)? as usize;
    let optional = checked_offset(coff, 20)?;
    let magic = read_u16(&data, optional)?;
    if magic != 0x10B && magic != 0x20B {
        return Err(format!(
            "unsupported PE optional-header magic 0x{:04X}",
            magic
        ));
    }

    let entry_point = read_u32(&data, checked_offset(optional, 16)?)?;
    let pe_size_of_image = read_u32(&data, checked_offset(optional, 56)?)?;
    let pe_checksum = read_u32(&data, checked_offset(optional, 64)?)?;
    let subsystem = read_u16(&data, checked_offset(optional, 68)?)?;
    let section_table = checked_offset(optional, optional_header_size)?;
    let section_limit = usize::from(pe_section_count).min(MAX_INTERESTING_SECTIONS);
    let mut sections = Vec::new();

    for index in 0..section_limit {
        let section_offset = index
            .checked_mul(40)
            .ok_or_else(|| format!("section {} offset overflow", index))?;
        let offset = checked_offset(section_table, section_offset)?;
        sections.push(SectionInfo {
            name: section_name(read_slice(&data, offset, 8)?),
            virtual_size: read_u32(&data, checked_offset(offset, 8)?)?,
            virtual_address: read_u32(&data, checked_offset(offset, 12)?)?,
            raw_size: read_u32(&data, checked_offset(offset, 16)?)?,
            raw_pointer: read_u32(&data, checked_offset(offset, 20)?)?,
            characteristics: read_u32(&data, checked_offset(offset, 36)?)?,
        });
    }

    Ok(ModuleFingerprint {
        file_size,
        modified_unix,
        fnv1a64: fnv1a64(&data),
        machine,
        pe_time_date_stamp,
        pe_size_of_image,
        pe_checksum,
        pe_section_count,
        entry_point,
        subsystem,
        sections,
    })
}

fn read_u16(data: &[u8], offset: usize) -> Result<u16, String> {
    let bytes = read_slice(data, offset, 2)?;
    Ok(u16::from_le_bytes([bytes[0], bytes[1]]))
}

fn read_u32(data: &[u8], offset: usize) -> Result<u32, String> {
    let bytes = read_slice(data, offset, 4)?;
    Ok(u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]))
}

fn checked_offset(offset: usize, add: usize) -> Result<usize, String> {
    offset
        .checked_add(add)
        .ok_or_else(|| format!("offset {} add {} overflow", offset, add))
}

fn read_slice(data: &[u8], offset: usize, len: usize) -> Result<&[u8], String> {
    let end = offset
        .checked_add(len)
        .ok_or_else(|| format!("offset {} length {} overflow", offset, len))?;
    data.get(offset..end)
        .ok_or_else(|| format!("offset {} length {} out of range", offset, len))
}

fn section_name(bytes: &[u8]) -> String {
    let len = bytes
        .iter()
        .position(|byte| *byte == 0)
        .unwrap_or(bytes.len());
    String::from_utf8_lossy(&bytes[..len]).into_owned()
}

fn fnv1a64(data: &[u8]) -> u64 {
    let mut hash = 0xCBF29CE484222325u64;
    for byte in data {
        hash ^= u64::from(*byte);
        hash = hash.wrapping_mul(0x00000100000001B3);
    }
    hash
}

fn fingerprint_json(fingerprint: &ModuleFingerprint) -> String {
    format!(
        concat!(
            "{{",
            "\"file_size\":{},",
            "\"modified_unix\":{},",
            "\"fnv1a64\":\"{}\",",
            "\"machine\":\"{}\",",
            "\"pe_time_date_stamp\":{},",
            "\"pe_size_of_image\":{},",
            "\"pe_checksum\":\"{}\",",
            "\"pe_section_count\":{},",
            "\"entry_point\":\"{}\",",
            "\"subsystem\":{}",
            "}}"
        ),
        fingerprint.file_size,
        fingerprint.modified_unix,
        hex_u64(fingerprint.fnv1a64),
        hex_u16(fingerprint.machine),
        fingerprint.pe_time_date_stamp,
        fingerprint.pe_size_of_image,
        hex_u32(fingerprint.pe_checksum),
        fingerprint.pe_section_count,
        hex_u32(fingerprint.entry_point),
        fingerprint.subsystem
    )
}

fn module_sections_json(module: &ModuleInfo, sections: &[SectionInfo]) -> String {
    let mut output = String::from("[");
    for (index, section) in sections.iter().enumerate() {
        if index > 0 {
            output.push(',');
        }
        output.push_str(&section_json(module, section));
    }
    output.push(']');
    output
}

fn section_json(module: &ModuleInfo, section: &SectionInfo) -> String {
    let span = section.virtual_size.max(section.raw_size);
    let start = module.base.saturating_add(section.virtual_address as usize);
    let end = start.saturating_add(span as usize);
    format!(
        concat!(
            "{{",
            "\"name\":\"{}\",",
            "\"virtual_address\":\"{}\",",
            "\"virtual_size\":{},",
            "\"raw_size\":{},",
            "\"raw_pointer\":\"{}\",",
            "\"characteristics\":\"{}\",",
            "\"readable\":{},",
            "\"writable\":{},",
            "\"executable\":{},",
            "\"memory_start\":\"{}\",",
            "\"memory_end\":\"{}\"",
            "}}"
        ),
        escape_json(&section.name),
        hex_u32(section.virtual_address),
        section.virtual_size,
        section.raw_size,
        hex_u32(section.raw_pointer),
        hex_u32(section.characteristics),
        bool_text(section_is_readable(section)),
        bool_text(section_is_writable(section)),
        bool_text(section_is_executable(section)),
        hex_address(start),
        hex_address(end)
    )
}

fn section_scan_preparation_json(
    starcraft: Option<&ModuleInfo>,
    clientsdk: Option<&ModuleInfo>,
    samase_temp: Option<&ModuleInfo>,
) -> String {
    format!(
        concat!(
            "{{",
            "\"mode\":\"bounded_candidates_only\",",
            "\"direct_memory_reads\":false,",
            "\"source\":\"Toolhelp module bases plus PE headers read from module files on disk\",",
            "\"scan_policy\":{{",
            "\"candidate_only\":true,",
            "\"scan_now\":false,",
            "\"read_now\":false,",
            "\"max_range_bytes\":{},",
            "\"alignment_bytes\":{},",
            "\"allowed_section_names\":[\".data\",\".rdata\",\".rodata\",\"_RDATA\"],",
            "\"requires_readable_section\":true,",
            "\"excludes_executable_sections\":true",
            "}},",
            "\"validation_stages\":[",
            "{{\"name\":\"module_fingerprint_match\",\"status\":\"prepared\",\"dereference_now\":false,\"read_now\":false}},",
            "{{\"name\":\"section_bounds_check\",\"status\":\"prepared\",\"dereference_now\":false,\"read_now\":false}},",
            "{{\"name\":\"bounded_scan_range_candidate\",\"status\":\"prepared\",\"dereference_now\":false,\"read_now\":false}},",
            "{{\"name\":\"single_player_gate\",\"status\":\"prepared\",\"dereference_now\":false}},",
            "{{\"name\":\"candidate_pointer_probe\",\"status\":\"deferred\",\"dereference_now\":false,\"read_now\":false}}",
            "],",
            "\"candidate_groups\":[",
            "{},{},{}",
            "],",
            "\"deferred_pointer_candidates\":[",
            "{},{},{}",
            "]",
            "}}"
        ),
        MAX_SCAN_RANGE_BYTES,
        SCAN_ALIGNMENT_BYTES,
        section_candidate_group_json(
            "starcraft_readable_sections",
            starcraft,
            "Readable StarCraft sections plus bounded non-executable scan candidates.",
        ),
        section_candidate_group_json(
            "clientsdk_readable_sections",
            clientsdk,
            "ClientSdk readable sections; diagnostic range map only.",
        ),
        section_candidate_group_json(
            "samase_temp_sections",
            samase_temp,
            "Samase temp DLL sections; fingerprinted before any plugin-surface research.",
        ),
        pointer_candidate_json(
            "in_game_state_pointer",
            "deferred_validation",
            "No dereference until fingerprint and section validation are implemented.",
        ),
        pointer_candidate_json(
            "resource_state_pointer",
            "deferred_validation",
            "Minerals, gas, and supply offsets are intentionally not read in v3.",
        ),
        pointer_candidate_json(
            "unit_table_pointer",
            "deferred_validation",
            "Unit array/table candidates are not dereferenced in v3.",
        )
    )
}

fn section_candidate_group_json(name: &str, module: Option<&ModuleInfo>, note: &str) -> String {
    let Some(module) = module else {
        return format!(
            concat!(
                "{{",
                "\"name\":\"{}\",",
                "\"status\":\"missing_module\",",
                "\"dereference_now\":false,",
                "\"direct_memory_reads\":false,",
                "\"note\":\"{}\",",
                "\"module\":null,",
                "\"sections\":[],",
                "\"verified_scan_ranges\":[]",
                "}}"
            ),
            escape_json(name),
            escape_json(note)
        );
    };

    match module_fingerprint(module) {
        Ok(fingerprint) => {
            let sections: Vec<SectionInfo> = fingerprint
                .sections
                .into_iter()
                .filter(|section| {
                    section.characteristics & (IMAGE_SCN_MEM_READ | IMAGE_SCN_MEM_EXECUTE) != 0
                })
                .collect();
            format!(
                concat!(
                    "{{",
                    "\"name\":\"{}\",",
                    "\"status\":\"prepared\",",
                    "\"dereference_now\":false,",
                    "\"direct_memory_reads\":false,",
                    "\"note\":\"{}\",",
                    "\"module\":{},",
                    "\"sections\":{},",
                    "\"verified_scan_ranges\":{}",
                    "}}"
                ),
                escape_json(name),
                escape_json(note),
                module_json(module),
                module_sections_json(module, &sections),
                scan_ranges_for_sections_json(module, &sections)
            )
        }
        Err(error) => format!(
            concat!(
                "{{",
                "\"name\":\"{}\",",
                "\"status\":\"fingerprint_error\",",
                "\"dereference_now\":false,",
                "\"direct_memory_reads\":false,",
                "\"note\":\"{}\",",
                "\"module\":{},",
                "\"error\":\"{}\",",
                "\"sections\":[],",
                "\"verified_scan_ranges\":[]",
                "}}"
            ),
            escape_json(name),
            escape_json(note),
            module_json(module),
            escape_json(&error)
        ),
    }
}

fn scan_ranges_for_sections_json(module: &ModuleInfo, sections: &[SectionInfo]) -> String {
    let mut output = String::from("[");
    let mut count = 0usize;

    for section in sections.iter() {
        if count >= MAX_SAFE_SCAN_RANGES {
            break;
        }
        if !section_is_scan_eligible(section) {
            continue;
        }
        let Some(range_json) = verified_scan_range_json(module, section) else {
            continue;
        };
        if count > 0 {
            output.push(',');
        }
        output.push_str(&range_json);
        count += 1;
    }

    output.push(']');
    output
}

fn verified_scan_range_json(module: &ModuleInfo, section: &SectionInfo) -> Option<String> {
    let range = verified_scan_range(module, section, MAX_SCAN_RANGE_BYTES)?;

    Some(format!(
        concat!(
            "{{",
            "\"name\":\"{}:{}\",",
            "\"module_name\":\"{}\",",
            "\"module_base\":\"{}\",",
            "\"section_name\":\"{}\",",
            "\"probe_kind\":\"{}\",",
            "\"memory_start\":\"{}\",",
            "\"memory_end\":\"{}\",",
            "\"section_memory_start\":\"{}\",",
            "\"section_memory_end\":\"{}\",",
            "\"range_bytes\":{},",
            "\"max_range_bytes\":{},",
            "\"alignment_bytes\":{},",
            "\"within_module_bounds\":true,",
            "\"within_section_bounds\":true,",
            "\"readable\":true,",
            "\"writable\":{},",
            "\"executable\":false,",
            "\"scan_now\":false,",
            "\"read_now\":false,",
            "\"dereference_now\":false",
            "}}"
        ),
        escape_json(&module.name),
        escape_json(&section.name),
        escape_json(&module.name),
        hex_address(module.base),
        escape_json(&section.name),
        scan_probe_kind(section),
        hex_address(range.start),
        hex_address(range.end),
        hex_address(range.section_start),
        hex_address(range.section_end),
        range.range_bytes,
        MAX_SCAN_RANGE_BYTES,
        SCAN_ALIGNMENT_BYTES,
        bool_text(range.writable)
    ))
}

fn verified_scan_range(
    module: &ModuleInfo,
    section: &SectionInfo,
    max_range_bytes: usize,
) -> Option<VerifiedScanRange> {
    let section_span = section.virtual_size.max(section.raw_size) as usize;
    if section_span == 0 {
        return None;
    }

    let module_end = module.base.checked_add(module.size as usize)?;
    let section_start = module.base.checked_add(section.virtual_address as usize)?;
    let section_end = section_start.checked_add(section_span)?;
    if section_start < module.base || section_end > module_end {
        return None;
    }

    let range_bytes = section_span.min(max_range_bytes);
    let range_end = section_start.checked_add(range_bytes)?;
    if range_end > section_end || range_end > module_end {
        return None;
    }

    Some(VerifiedScanRange {
        section_name: section.name.clone(),
        start: section_start,
        end: range_end,
        section_start,
        section_end,
        range_bytes,
        writable: section_is_writable(section),
    })
}

fn section_is_scan_eligible(section: &SectionInfo) -> bool {
    section_is_readable(section)
        && !section_is_executable(section)
        && section_scan_name_allowed(&section.name)
}

fn section_scan_name_allowed(name: &str) -> bool {
    matches!(
        name.to_ascii_lowercase().as_str(),
        ".data" | ".rdata" | ".rodata" | "_rdata"
    )
}

fn scan_probe_kind(section: &SectionInfo) -> &'static str {
    if section_is_writable(section) {
        "mutable_state_candidate"
    } else {
        "static_pointer_or_marker_candidate"
    }
}

fn section_is_readable(section: &SectionInfo) -> bool {
    section.characteristics & IMAGE_SCN_MEM_READ != 0
}

fn section_is_writable(section: &SectionInfo) -> bool {
    section.characteristics & IMAGE_SCN_MEM_WRITE != 0
}

fn section_is_executable(section: &SectionInfo) -> bool {
    section.characteristics & IMAGE_SCN_MEM_EXECUTE != 0
}

fn pointer_candidate_json(name: &str, status: &str, note: &str) -> String {
    format!(
        concat!(
            "{{",
            "\"name\":\"{}\",",
            "\"status\":\"{}\",",
            "\"read_only\":true,",
            "\"dereference_now\":false,",
            "\"read_now\":false,",
            "\"direct_memory_reads\":false,",
            "\"validation_required\":true,",
            "\"note\":\"{}\"",
            "}}"
        ),
        escape_json(name),
        escape_json(status),
        escape_json(note)
    )
}

fn module_status(module: Option<&ModuleInfo>) -> &'static str {
    if module.is_some() {
        "present"
    } else {
        "missing"
    }
}

fn module_option_json(module: Option<&ModuleInfo>) -> String {
    match module {
        Some(module) => module_json(module),
        None => "null".to_string(),
    }
}

fn module_json(module: &ModuleInfo) -> String {
    format!(
        concat!(
            "{{",
            "\"name\":\"{}\",",
            "\"path\":\"{}\",",
            "\"base\":\"{}\",",
            "\"size\":{}",
            "}}"
        ),
        escape_json(&module.name),
        escape_json(&module.path),
        hex_address(module.base),
        module.size
    )
}

fn modules_json(modules: &[ModuleInfo]) -> String {
    let mut output = String::from("[");
    for (index, module) in modules.iter().enumerate() {
        if index > 0 {
            output.push(',');
        }
        output.push_str(&module_json(module));
    }
    output.push(']');
    output
}

fn surface_candidate_json(
    name: &str,
    status: &str,
    note: &str,
    module: Option<&ModuleInfo>,
) -> String {
    let module_json = match module {
        Some(module) => format!(
            concat!(
                "\"module_name\":\"{}\",",
                "\"module_base\":\"{}\",",
                "\"module_size\":{}"
            ),
            escape_json(&module.name),
            hex_address(module.base),
            module.size
        ),
        None => "\"module_name\":null,\"module_base\":null,\"module_size\":0".to_string(),
    };
    format!(
        concat!(
            "{{",
            "\"name\":\"{}\",",
            "\"status\":\"{}\",",
            "\"read_only\":true,",
            "\"direct_memory_reads\":false,",
            "\"note\":\"{}\",",
            "{}",
            "}}"
        ),
        escape_json(name),
        escape_json(status),
        escape_json(note),
        module_json
    )
}

fn hex_address(value: usize) -> String {
    if mem::size_of::<usize>() <= 4 {
        format!("0x{:08X}", value)
    } else {
        format!("0x{:016X}", value)
    }
}

fn hex_u16(value: u16) -> String {
    format!("0x{:04X}", value)
}

fn hex_u32(value: u32) -> String {
    format!("0x{:08X}", value)
}

fn hex_u64(value: u64) -> String {
    format!("0x{:016X}", value)
}

fn hex_bytes(bytes: &[u8]) -> String {
    let mut output = String::with_capacity(bytes.len() * 2);
    for byte in bytes {
        output.push_str(&format!("{:02X}", byte));
    }
    output
}

fn json_array(items: &[String]) -> String {
    let mut output = String::from("[");
    for (index, item) in items.iter().enumerate() {
        if index > 0 {
            output.push(',');
        }
        output.push_str(item);
    }
    output.push(']');
    output
}

fn escape_json(value: &str) -> String {
    value
        .replace('\\', "\\\\")
        .replace('"', "\\\"")
        .replace('\n', "\\n")
        .replace('\r', "\\r")
        .replace('\t', "\\t")
}

fn load_u32_watch_candidates(path: &str) -> Result<Vec<U32WatchCandidate>, String> {
    let content =
        fs::read_to_string(path).map_err(|error| format!("read watchlist failed: {}", error))?;
    let Some(array) = json_array_body_for_key(&content, "candidates") else {
        return Err("missing candidates array".to_string());
    };

    let mut candidates = Vec::new();
    for object in top_level_json_objects(array) {
        if candidates.len() >= MAX_U32_WATCHLIST_CANDIDATES {
            break;
        }
        let read_policy = json_string_field(object, "read_policy").unwrap_or_default();
        if read_policy != "direct_read_only_no_pointer_deref" {
            continue;
        }
        let value_type = json_string_field(object, "value_type").unwrap_or_default();
        if value_type != "u32_le" {
            continue;
        }
        let rva_text = json_string_field(object, "rva").unwrap_or_default();
        let Some(rva) = parse_hex_usize(&rva_text) else {
            continue;
        };
        candidates.push(U32WatchCandidate {
            module_name: json_string_field(object, "module_name").unwrap_or_default(),
            section_name: json_string_field(object, "section_name").unwrap_or_default(),
            confidence: json_string_field(object, "confidence").unwrap_or_default(),
            resource_field: json_string_field(object, "resource_field").unwrap_or_default(),
            rva_text,
            rva,
        });
    }

    Ok(candidates)
}

fn json_array_body_for_key<'a>(content: &'a str, key: &str) -> Option<&'a str> {
    let key_pattern = format!("\"{}\"", key);
    let key_start = content.find(&key_pattern)?;
    let after_key = &content[key_start + key_pattern.len()..];
    let array_start_relative = after_key.find('[')?;
    let array_start = key_start + key_pattern.len() + array_start_relative;
    let bytes = content.as_bytes();
    let mut in_string = false;
    let mut escaped = false;
    let mut depth = 0usize;
    for index in array_start..bytes.len() {
        let byte = bytes[index];
        if in_string {
            if escaped {
                escaped = false;
            } else if byte == b'\\' {
                escaped = true;
            } else if byte == b'"' {
                in_string = false;
            }
            continue;
        }
        if byte == b'"' {
            in_string = true;
            continue;
        }
        if byte == b'[' {
            depth += 1;
            continue;
        }
        if byte == b']' {
            depth = depth.saturating_sub(1);
            if depth == 0 {
                return content.get(array_start + 1..index);
            }
        }
    }
    None
}

fn top_level_json_objects(array_body: &str) -> Vec<&str> {
    let bytes = array_body.as_bytes();
    let mut objects = Vec::new();
    let mut in_string = false;
    let mut escaped = false;
    let mut depth = 0usize;
    let mut object_start: Option<usize> = None;
    for index in 0..bytes.len() {
        let byte = bytes[index];
        if in_string {
            if escaped {
                escaped = false;
            } else if byte == b'\\' {
                escaped = true;
            } else if byte == b'"' {
                in_string = false;
            }
            continue;
        }
        if byte == b'"' {
            in_string = true;
            continue;
        }
        if byte == b'{' {
            if depth == 0 {
                object_start = Some(index);
            }
            depth += 1;
            continue;
        }
        if byte == b'}' && depth > 0 {
            depth -= 1;
            if depth == 0 {
                if let Some(start) = object_start.take() {
                    if let Some(object) = array_body.get(start..=index) {
                        objects.push(object);
                    }
                }
            }
        }
    }
    objects
}

fn json_string_field(object: &str, key: &str) -> Option<String> {
    let key_pattern = format!("\"{}\"", key);
    let key_start = object.find(&key_pattern)?;
    let after_key = &object[key_start + key_pattern.len()..];
    let colon = after_key.find(':')?;
    let after_colon = after_key[colon + 1..].trim_start();
    if !after_colon.starts_with('"') {
        return None;
    }
    let bytes = after_colon.as_bytes();
    let mut output = String::new();
    let mut escaped = false;
    for index in 1..bytes.len() {
        let byte = bytes[index];
        if escaped {
            output.push(match byte {
                b'"' => '"',
                b'\\' => '\\',
                b'n' => '\n',
                b'r' => '\r',
                b't' => '\t',
                other => other as char,
            });
            escaped = false;
        } else if byte == b'\\' {
            escaped = true;
        } else if byte == b'"' {
            return Some(output);
        } else {
            output.push(byte as char);
        }
    }
    None
}

fn parse_hex_usize(value: &str) -> Option<usize> {
    let trimmed = value.trim();
    let without_prefix = trimmed
        .strip_prefix("0x")
        .or_else(|| trimmed.strip_prefix("0X"))
        .unwrap_or(trimmed);
    usize::from_str_radix(without_prefix, 16).ok()
}

fn resolve_state_path() -> String {
    std::env::var("LAV_SAMASE_STATE_PATH")
        .ok()
        .filter(|value| !value.trim().is_empty())
        .unwrap_or_else(|| "lav_samase_readonly_state.json".to_string())
}

fn resolve_write_interval() -> u32 {
    std::env::var("LAV_SAMASE_STATE_EVERY_N_FRAMES")
        .ok()
        .and_then(|value| value.trim().parse::<u32>().ok())
        .filter(|value| *value > 0)
        .unwrap_or(8)
}

fn resolve_initialize_heartbeat_interval() -> u64 {
    std::env::var("LAV_SAMASE_HEARTBEAT_INTERVAL_MS")
        .ok()
        .and_then(|value| value.trim().parse::<u64>().ok())
        .filter(|value| *value >= 100)
        .unwrap_or(1000)
}

fn resolve_in_game_detector_enabled() -> bool {
    std::env::var("LAV_SAMASE_INGAME_DETECTOR")
        .ok()
        .map(|value| {
            let normalized = value.trim().to_ascii_lowercase();
            !matches!(normalized.as_str(), "0" | "false" | "no" | "off")
        })
        .unwrap_or(true)
}

fn resolve_offset_discovery_enabled() -> bool {
    std::env::var("LAV_SAMASE_OFFSET_DISCOVERY")
        .ok()
        .map(|value| {
            let normalized = value.trim().to_ascii_lowercase();
            !matches!(normalized.as_str(), "0" | "false" | "no" | "off")
        })
        .unwrap_or(true)
}

fn resolve_u32_watch_enabled() -> bool {
    std::env::var("LAV_SAMASE_U32_WATCH")
        .ok()
        .map(|value| {
            let normalized = value.trim().to_ascii_lowercase();
            !matches!(normalized.as_str(), "0" | "false" | "no" | "off")
        })
        .unwrap_or(true)
}

fn resolve_u32_watch_active_threshold() -> usize {
    std::env::var("LAV_SAMASE_U32_WATCH_ACTIVE_THRESHOLD")
        .ok()
        .and_then(|value| value.trim().parse::<usize>().ok())
        .filter(|value| *value > 0)
        .unwrap_or(3)
}

fn resolve_resource_focused_scan_enabled() -> bool {
    std::env::var("LAV_SAMASE_RESOURCE_FOCUSED_SCAN")
        .ok()
        .map(|value| {
            let normalized = value.trim().to_ascii_lowercase();
            !matches!(normalized.as_str(), "0" | "false" | "no" | "off")
        })
        .unwrap_or(true)
}

fn resolve_resource_focused_start_window() -> usize {
    std::env::var("LAV_SAMASE_RESOURCE_FOCUSED_START_WINDOW")
        .ok()
        .and_then(|value| value.trim().parse::<usize>().ok())
        .unwrap_or(0)
}

fn resolve_resource_focused_window_limit() -> usize {
    std::env::var("LAV_SAMASE_RESOURCE_FOCUSED_WINDOWS")
        .ok()
        .and_then(|value| value.trim().parse::<usize>().ok())
        .filter(|value| *value > 0)
        .map(|value| value.min(MAX_RESOURCE_FOCUSED_WINDOWS_PER_MODULE))
        .unwrap_or(DEFAULT_RESOURCE_FOCUSED_WINDOWS_PER_MODULE)
}

fn resolve_u32_watchlist_path() -> String {
    if let Ok(value) = std::env::var("LAV_SAMASE_U32_WATCHLIST_PATH") {
        if !value.trim().is_empty() {
            return value;
        }
    }
    let state_path = STATE_PATH.get_or_init(resolve_state_path);
    let state_path = Path::new(state_path);
    if let Some(states_dir) = state_path.parent() {
        if let Some(probe_dir) = states_dir.parent() {
            return probe_dir
                .join("focused_u32_watchlist.json")
                .to_string_lossy()
                .to_string();
        }
    }
    "focused_u32_watchlist.json".to_string()
}

fn bool_text(value: bool) -> &'static str {
    if value {
        "true"
    } else {
        "false"
    }
}

fn unix_timestamp_secs() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_secs())
        .unwrap_or(0)
}

unsafe fn print_text(api: *const PluginApi, message: &str) {
    if let Some(print_text) = ((*api).print_text)() {
        if let Ok(message) = CString::new(message) {
            print_text(message.as_ptr() as *const u8);
        }
    }
}
