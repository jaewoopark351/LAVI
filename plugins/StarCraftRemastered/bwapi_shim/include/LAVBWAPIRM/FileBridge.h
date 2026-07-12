//20260701_kpopmodder: File bridge for SAIDA-style tests without process injection.
#pragma once

#include <string>

#include "LAVBWAPIRM/Bridge.h"

namespace LAVBWAPIRM {

class FileBridge final : public Bridge {
public:
    FileBridge(std::string snapshotPath, std::string commandQueuePath);

    bool connect() override;
    void disconnect() override;
    GameSnapshot snapshot() override;
    bool sendCommand(const Command& command) override;
    void stopAllControl() override;

private:
    std::string snapshotPath_;
    std::string commandQueuePath_;
    bool connected_ = false;

    GameSnapshot parseSnapshot(const std::string& jsonText) const;
    std::string readTextFile(const std::string& path) const;
    bool appendCommand(const std::string& line) const;
    std::string commandToJson(const Command& command) const;
};

} // namespace LAVBWAPIRM
