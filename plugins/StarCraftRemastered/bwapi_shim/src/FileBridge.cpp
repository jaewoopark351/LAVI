//20260701_kpopmodder: Implements read-only snapshot exchange through JSON files.
#include "LAVBWAPIRM/FileBridge.h"

#include <cctype>
#include <fstream>
#include <initializer_list>
#include <map>
#include <sstream>
#include <utility>

namespace LAVBWAPIRM {
namespace {

struct JsonValue {
    enum class Type {
        Null,
        Bool,
        Number,
        String,
        Array,
        Object
    };

    Type type = Type::Null;
    bool boolValue = false;
    double numberValue = 0.0;
    std::string stringValue;
    std::vector<JsonValue> arrayValue;
    std::map<std::string, JsonValue> objectValue;

    const JsonValue* field(const std::string& name) const
    {
        if (type != Type::Object) {
            return nullptr;
        }
        auto iter = objectValue.find(name);
        return iter == objectValue.end() ? nullptr : &iter->second;
    }
};

class JsonParser {
public:
    explicit JsonParser(const std::string& text)
        : text_(text)
    {
    }

    bool parse(JsonValue& value)
    {
        skipWhitespace();
        if (!parseValue(value)) {
            return false;
        }
        skipWhitespace();
        return position_ == text_.size();
    }

private:
    bool parseValue(JsonValue& value)
    {
        skipWhitespace();
        if (position_ >= text_.size()) {
            return false;
        }

        const char ch = text_[position_];
        if (ch == '{') {
            return parseObject(value);
        }
        if (ch == '[') {
            return parseArray(value);
        }
        if (ch == '"') {
            value.type = JsonValue::Type::String;
            return parseString(value.stringValue);
        }
        if (ch == 't' || ch == 'f') {
            return parseBool(value);
        }
        if (ch == 'n') {
            return parseNull(value);
        }
        if (ch == '-' || std::isdigit(static_cast<unsigned char>(ch))) {
            return parseNumber(value);
        }
        return false;
    }

    bool parseObject(JsonValue& value)
    {
        if (!consume('{')) {
            return false;
        }

        value = {};
        value.type = JsonValue::Type::Object;
        skipWhitespace();
        if (consume('}')) {
            return true;
        }

        while (position_ < text_.size()) {
            std::string key;
            if (!parseString(key)) {
                return false;
            }
            skipWhitespace();
            if (!consume(':')) {
                return false;
            }

            JsonValue child;
            if (!parseValue(child)) {
                return false;
            }
            value.objectValue[key] = std::move(child);

            skipWhitespace();
            if (consume('}')) {
                return true;
            }
            if (!consume(',')) {
                return false;
            }
            skipWhitespace();
        }
        return false;
    }

    bool parseArray(JsonValue& value)
    {
        if (!consume('[')) {
            return false;
        }

        value = {};
        value.type = JsonValue::Type::Array;
        skipWhitespace();
        if (consume(']')) {
            return true;
        }

        while (position_ < text_.size()) {
            JsonValue child;
            if (!parseValue(child)) {
                return false;
            }
            value.arrayValue.push_back(std::move(child));

            skipWhitespace();
            if (consume(']')) {
                return true;
            }
            if (!consume(',')) {
                return false;
            }
            skipWhitespace();
        }
        return false;
    }

    bool parseString(std::string& value)
    {
        if (!consume('"')) {
            return false;
        }

        value.clear();
        while (position_ < text_.size()) {
            const char ch = text_[position_++];
            if (ch == '"') {
                return true;
            }

            if (ch != '\\') {
                value.push_back(ch);
                continue;
            }

            if (position_ >= text_.size()) {
                return false;
            }

            const char escaped = text_[position_++];
            switch (escaped) {
            case '"':
            case '\\':
            case '/':
                value.push_back(escaped);
                break;
            case 'b':
                value.push_back('\b');
                break;
            case 'f':
                value.push_back('\f');
                break;
            case 'n':
                value.push_back('\n');
                break;
            case 'r':
                value.push_back('\r');
                break;
            case 't':
                value.push_back('\t');
                break;
            case 'u':
                if (position_ + 4 > text_.size()) {
                    return false;
                }
                position_ += 4;
                value.push_back('?');
                break;
            default:
                return false;
            }
        }
        return false;
    }

    bool parseBool(JsonValue& value)
    {
        if (match("true")) {
            value = {};
            value.type = JsonValue::Type::Bool;
            value.boolValue = true;
            return true;
        }
        if (match("false")) {
            value = {};
            value.type = JsonValue::Type::Bool;
            value.boolValue = false;
            return true;
        }
        return false;
    }

    bool parseNull(JsonValue& value)
    {
        if (!match("null")) {
            return false;
        }
        value = {};
        value.type = JsonValue::Type::Null;
        return true;
    }

    bool parseNumber(JsonValue& value)
    {
        const size_t start = position_;
        if (position_ < text_.size() && text_[position_] == '-') {
            ++position_;
        }
        while (position_ < text_.size()
            && std::isdigit(static_cast<unsigned char>(text_[position_]))) {
            ++position_;
        }
        if (position_ < text_.size() && text_[position_] == '.') {
            ++position_;
            while (position_ < text_.size()
                && std::isdigit(static_cast<unsigned char>(text_[position_]))) {
                ++position_;
            }
        }
        if (position_ < text_.size()
            && (text_[position_] == 'e' || text_[position_] == 'E')) {
            ++position_;
            if (position_ < text_.size()
                && (text_[position_] == '+' || text_[position_] == '-')) {
                ++position_;
            }
            while (position_ < text_.size()
                && std::isdigit(static_cast<unsigned char>(text_[position_]))) {
                ++position_;
            }
        }

        try {
            value = {};
            value.type = JsonValue::Type::Number;
            value.numberValue = std::stod(text_.substr(start, position_ - start));
            return true;
        } catch (...) {
            return false;
        }
    }

    bool consume(char expected)
    {
        skipWhitespace();
        if (position_ >= text_.size() || text_[position_] != expected) {
            return false;
        }
        ++position_;
        return true;
    }

    bool match(const char* literal)
    {
        const std::string value(literal);
        if (text_.compare(position_, value.size(), value) != 0) {
            return false;
        }
        position_ += value.size();
        return true;
    }

    void skipWhitespace()
    {
        while (position_ < text_.size()
            && std::isspace(static_cast<unsigned char>(text_[position_]))) {
            ++position_;
        }
    }

    const std::string& text_;
    size_t position_ = 0;
};

bool parseJson(const std::string& text, JsonValue& value)
{
    JsonParser parser(text);
    return parser.parse(value);
}

const JsonValue* field(
    const JsonValue* value,
    std::initializer_list<const char*> names
)
{
    if (!value || value->type != JsonValue::Type::Object) {
        return nullptr;
    }

    for (const char* name : names) {
        if (const JsonValue* child = value->field(name)) {
            return child;
        }
    }
    return nullptr;
}

const JsonValue* objectField(
    const JsonValue* value,
    std::initializer_list<const char*> names
)
{
    const JsonValue* child = field(value, names);
    return child && child->type == JsonValue::Type::Object ? child : nullptr;
}

const JsonValue* arrayField(
    const JsonValue* value,
    std::initializer_list<const char*> names
)
{
    const JsonValue* child = field(value, names);
    return child && child->type == JsonValue::Type::Array ? child : nullptr;
}

bool boolValue(const JsonValue* value, bool fallback)
{
    if (!value) {
        return fallback;
    }
    if (value->type == JsonValue::Type::Bool) {
        return value->boolValue;
    }
    if (value->type == JsonValue::Type::Number) {
        return value->numberValue != 0.0;
    }
    if (value->type == JsonValue::Type::String) {
        const std::string text = value->stringValue;
        return text == "1" || text == "true" || text == "yes" || text == "on";
    }
    return fallback;
}

int intValue(const JsonValue* value, int fallback)
{
    if (!value) {
        return fallback;
    }
    if (value->type == JsonValue::Type::Number) {
        return static_cast<int>(value->numberValue);
    }
    if (value->type == JsonValue::Type::String) {
        try {
            return std::stoi(value->stringValue);
        } catch (...) {
            return fallback;
        }
    }
    return fallback;
}

std::string stringValue(const JsonValue* value, const std::string& fallback = "")
{
    if (!value) {
        return fallback;
    }
    if (value->type == JsonValue::Type::String) {
        return value->stringValue;
    }
    if (value->type == JsonValue::Type::Number) {
        std::ostringstream stream;
        stream << static_cast<int>(value->numberValue);
        return stream.str();
    }
    if (value->type == JsonValue::Type::Bool) {
        return value->boolValue ? "true" : "false";
    }
    return fallback;
}

std::vector<std::string> stringListValue(const JsonValue* value)
{
    std::vector<std::string> items;
    if (!value || value->type != JsonValue::Type::Array) {
        return items;
    }

    for (const JsonValue& item : value->arrayValue) {
        std::string text = stringValue(&item);
        if (!text.empty()) {
            items.push_back(text);
        }
    }
    return items;
}

std::map<std::string, int> intMapValue(const JsonValue* value)
{
    std::map<std::string, int> items;
    if (!value || value->type != JsonValue::Type::Object) {
        return items;
    }

    for (const auto& entry : value->objectValue) {
        items[entry.first] = intValue(&entry.second, 0);
    }
    return items;
}

Position positionValue(const JsonValue* value, Position fallback = {})
{
    if (!value) {
        return fallback;
    }
    if (value->type == JsonValue::Type::Array && value->arrayValue.size() >= 2) {
        return {
            intValue(&value->arrayValue[0], fallback.x),
            intValue(&value->arrayValue[1], fallback.y),
        };
    }
    if (value->type == JsonValue::Type::Object) {
        return {
            intValue(field(value, {"x", "X"}), fallback.x),
            intValue(field(value, {"y", "Y"}), fallback.y),
        };
    }
    return fallback;
}

PlayerSnapshot playerFromJson(
    const JsonValue* playerObject,
    const JsonValue* gameObject,
    bool selfPlayer
)
{
    PlayerSnapshot player;
    if (!playerObject) {
        playerObject = gameObject;
    }

    player.id = intValue(field(playerObject, {"id", "player_id", "playerId"}), selfPlayer ? 1 : 2);
    player.name = stringValue(field(playerObject, {"name"}), selfPlayer ? "Self" : "Enemy");
    player.race = stringValue(
        field(playerObject, {"race"}),
        stringValue(field(gameObject, {selfPlayer ? "player_race" : "enemy_race"}))
    );
    player.minerals = intValue(field(playerObject, {"minerals"}), 0);
    player.gas = intValue(field(playerObject, {"gas"}), 0);
    player.supplyUsed = intValue(
        field(playerObject, {"supply_used", "supplyUsed"}),
        intValue(field(gameObject, {"supply_used", "supplyUsed"}), 0)
    );
    player.supplyTotal = intValue(
        field(playerObject, {"supply_total", "supplyTotal"}),
        intValue(field(gameObject, {"supply_total", "supplyTotal"}), 0)
    );
    player.startLocation = positionValue(
        field(playerObject, {"start_location", "startLocation"}),
        positionValue(field(gameObject, {selfPlayer ? "my_start_location" : "enemy_start_location"}))
    );
    player.researchedTechs = stringListValue(field(playerObject, {"researched_techs", "researchedTechs"}));
    player.researchingTechs = stringListValue(field(playerObject, {"researching_techs", "researchingTechs"}));
    player.upgradingUpgrades = stringListValue(field(playerObject, {"upgrading_upgrades", "upgradingUpgrades"}));
    player.upgradeLevels = intMapValue(field(playerObject, {"upgrade_levels", "upgradeLevels"}));
    return player;
}

UnitSnapshot unitFromJson(const JsonValue& object, const std::string& ownerFallback)
{
    UnitSnapshot unit;
    unit.id = intValue(field(&object, {"unit_id", "unitId", "id"}), 0);
    unit.type = stringValue(field(&object, {"unit_type", "unitType", "type"}));
    unit.owner = stringValue(field(&object, {"owner"}), ownerFallback);
    unit.ownerId = intValue(field(&object, {"owner_id", "ownerId"}), 0);
    unit.position = positionValue(
        field(&object, {"position", "pos"}),
        {
            intValue(field(&object, {"x", "X"}), 0),
            intValue(field(&object, {"y", "Y"}), 0),
        }
    );
    unit.hitPoints = intValue(field(&object, {"hp", "hit_points", "hitPoints"}), 0);
    unit.shields = intValue(field(&object, {"shields"}), 0);
    unit.energy = intValue(field(&object, {"energy"}), 0);
    unit.resources = intValue(field(&object, {"resources"}), 0);
    unit.completed = boolValue(field(&object, {"is_completed", "completed"}), false);
    unit.visible = boolValue(field(&object, {"is_visible", "visible"}), true);
    unit.selected = boolValue(field(&object, {"is_selected", "selected"}), false);
    unit.flying = boolValue(field(&object, {"is_flying", "flying"}), false);
    unit.idle = boolValue(field(&object, {"is_idle", "idle"}), false);
    unit.order = stringValue(field(&object, {"current_order", "order"}));
    return unit;
}

std::vector<UnitSnapshot> unitsFromJson(
    const JsonValue* arrayValue,
    const std::string& ownerFallback
)
{
    std::vector<UnitSnapshot> units;
    if (!arrayValue || arrayValue->type != JsonValue::Type::Array) {
        return units;
    }

    for (const JsonValue& value : arrayValue->arrayValue) {
        if (value.type != JsonValue::Type::Object) {
            continue;
        }
        UnitSnapshot unit = unitFromJson(value, ownerFallback);
        if (unit.id != 0) {
            units.push_back(std::move(unit));
        }
    }
    return units;
}

std::string escapeJson(const std::string& value)
{
    std::string escaped;
    for (char ch : value) {
        if (ch == '\\' || ch == '"') {
            escaped.push_back('\\');
        }
        escaped.push_back(ch);
    }
    return escaped;
}

} // namespace

FileBridge::FileBridge(std::string snapshotPath, std::string commandQueuePath)
    : snapshotPath_(std::move(snapshotPath))
    , commandQueuePath_(std::move(commandQueuePath))
{
}

bool FileBridge::connect()
{
    connected_ = !snapshotPath_.empty();
    return connected_;
}

void FileBridge::disconnect()
{
    connected_ = false;
}

GameSnapshot FileBridge::snapshot()
{
    const std::string jsonText = readTextFile(snapshotPath_);
    if (jsonText.empty()) {
        GameSnapshot emptySnapshot;
        emptySnapshot.connected = false;
        emptySnapshot.singlePlayer = true;
        return emptySnapshot;
    }
    return parseSnapshot(jsonText);
}

bool FileBridge::sendCommand(const Command& command)
{
    if (!connected_ || commandQueuePath_.empty()) {
        return false;
    }
    return appendCommand(commandToJson(command));
}

void FileBridge::stopAllControl()
{
    if (commandQueuePath_.empty()) {
        return;
    }
    Command command;
    command.type = CommandType::Stop;
    command.payload = "stop_all_control";
    appendCommand(commandToJson(command));
}

GameSnapshot FileBridge::parseSnapshot(const std::string& jsonText) const
{
    GameSnapshot snapshot;
    JsonValue root;
    if (!parseJson(jsonText, root) || root.type != JsonValue::Type::Object) {
        snapshot.connected = false;
        snapshot.singlePlayer = true;
        return snapshot;
    }

    const JsonValue* game = objectField(&root, {"game"});
    if (!game) {
        game = &root;
    }

    snapshot.connected = boolValue(field(game, {"connected", "is_connected"}), false);
    snapshot.inGame = boolValue(field(game, {"in_game", "is_in_game"}), false);
    snapshot.singlePlayer = boolValue(field(game, {"single_player", "is_single_player"}), true);
    snapshot.battleNetScreen = boolValue(field(game, {"battle_net_screen", "is_battlenet_screen"}), false);
    snapshot.multiplayerScreen = boolValue(field(game, {"multiplayer_screen", "is_multiplayer_screen"}), false);
    snapshot.frameCount = intValue(field(game, {"frame_count", "frameCount"}), 0);
    snapshot.mapName = stringValue(field(game, {"map_name", "mapName"}));
    snapshot.mapWidth = intValue(field(game, {"map_width", "mapWidth"}), 128);
    snapshot.mapHeight = intValue(field(game, {"map_height", "mapHeight"}), 128);
    if (snapshot.mapWidth <= 0) {
        snapshot.mapWidth = 128;
    }
    if (snapshot.mapHeight <= 0) {
        snapshot.mapHeight = 128;
    }

    snapshot.self = playerFromJson(objectField(game, {"self", "self_player"}), game, true);
    snapshot.enemy = playerFromJson(objectField(game, {"enemy", "enemy_player"}), game, false);

    const JsonValue* units = objectField(&root, {"units"});
    if (units) {
        snapshot.myUnits = unitsFromJson(arrayField(units, {"my", "self", "my_units"}), "self");
        snapshot.enemyUnits = unitsFromJson(arrayField(units, {"enemy", "enemy_units"}), "enemy");
        snapshot.neutralUnits = unitsFromJson(arrayField(units, {"neutral", "neutral_units"}), "neutral");
    }
    if (snapshot.myUnits.empty()) {
        snapshot.myUnits = unitsFromJson(arrayField(&root, {"my_units", "myUnits"}), "self");
    }
    if (snapshot.enemyUnits.empty()) {
        snapshot.enemyUnits = unitsFromJson(arrayField(&root, {"enemy_units", "enemyUnits"}), "enemy");
    }
    if (snapshot.neutralUnits.empty()) {
        snapshot.neutralUnits = unitsFromJson(arrayField(&root, {"neutral_units", "neutralUnits"}), "neutral");
    }
    return snapshot;
}

std::string FileBridge::readTextFile(const std::string& path) const
{
    std::ifstream file(path);
    if (!file) {
        return "";
    }
    std::ostringstream buffer;
    buffer << file.rdbuf();
    return buffer.str();
}

bool FileBridge::appendCommand(const std::string& line) const
{
    if (commandQueuePath_.empty()) {
        return false;
    }
    std::ofstream file(commandQueuePath_, std::ios::app);
    if (!file) {
        return false;
    }
    file << line << "\n";
    return true;
}

std::string FileBridge::commandToJson(const Command& command) const
{
    std::ostringstream json;
    json << "{\"schema\":\"lav_bwapi_rm_command_v1\",";
    json << "\"type\":\"" << commandTypeName(command.type) << "\",";
    json << "\"unit_ids\":[";
    for (size_t index = 0; index < command.unitIds.size(); ++index) {
        if (index > 0) {
            json << ",";
        }
        json << command.unitIds[index];
    }
    json << "],";
    json << "\"target_unit_id\":" << command.targetUnitId << ",";
    json << "\"target_position\":["
         << command.targetPosition.x << ","
         << command.targetPosition.y << "],";
    json << "\"unit_name\":\"" << escapeJson(command.unitName) << "\",";
    json << "\"building_name\":\"" << escapeJson(command.buildingName) << "\",";
    json << "\"ability_name\":\"" << escapeJson(command.abilityName) << "\",";
    json << "\"payload\":\"" << escapeJson(command.payload) << "\"}";
    return json.str();
}

} // namespace LAVBWAPIRM
