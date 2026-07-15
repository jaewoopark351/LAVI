#ifdef _WIN32

#include "Tools.h"
#include "LadderManager.h"
#include <Windows.h>
#include <array>
#include <chrono>
#include <fstream>
#include <sstream>
#include <vector>
#include <Wincrypt.h>

namespace
{
//20260715_kpopmodder: Preserve native bot launch failure details in the parent log.
std::string FormatWindowsError(const DWORD errorCode)
{
	LPSTR messageBuffer = nullptr;
	const DWORD size = FormatMessageA(
		FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
		nullptr,
		errorCode,
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
		reinterpret_cast<LPSTR>(&messageBuffer),
		0,
		nullptr);
	const std::string message = messageBuffer != nullptr && size > 0
		? std::string(messageBuffer, size)
		: std::string("unknown Windows error");
	if (messageBuffer != nullptr)
	{
		LocalFree(messageBuffer);
	}
	return message;
}

std::string ReadAppendedLogTail(const std::string& path, const std::streamoff initialSize)
{
	constexpr std::streamoff maxTailBytes = 4096;
	std::ifstream input(path, std::ios::binary | std::ios::ate);
	if (!input)
	{
		return std::string();
	}
	const std::streamoff finalSize = input.tellg();
	if (finalSize <= initialSize)
	{
		return std::string();
	}
	const std::streamoff appendedSize = finalSize - initialSize;
	const std::streamoff readSize = appendedSize < maxTailBytes ? appendedSize : maxTailBytes;
	input.seekg(finalSize - readSize);
	std::string tail(static_cast<size_t>(readSize), '\0');
	input.read(&tail[0], readSize);
	tail.resize(static_cast<size_t>(input.gcount()));
	return tail;
}
}

void StartBotProcess(const BotConfig &Agent, const std::string &CommandLine, unsigned long *ProcessId)
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Executes the given command using CreateProcess() and WaitForSingleObject().
	// Returns FALSE if the command could not be executed or if the exit code could not be determined.

	SECURITY_ATTRIBUTES securityAttributes;
	securityAttributes.nLength = sizeof(securityAttributes);
	securityAttributes.lpSecurityDescriptor = NULL;
	securityAttributes.bInheritHandle = TRUE;
	std::string stderrLogFile = Agent.RootPath + "/data/stderr.log";
	std::streamoff stderrInitialSize = 0;
	{
		std::ifstream existingStderr(stderrLogFile, std::ios::binary | std::ios::ate);
		if (existingStderr)
		{
			stderrInitialSize = existingStderr.tellg();
		}
	}
	HANDLE stderrfile = CreateFile(stderrLogFile.c_str(),
		FILE_APPEND_DATA,
		FILE_SHARE_WRITE | FILE_SHARE_READ,
		&securityAttributes,
		OPEN_ALWAYS,
		FILE_ATTRIBUTE_NORMAL,
		NULL);
	if (stderrfile == INVALID_HANDLE_VALUE)
	{
		const DWORD errorCode = GetLastError();
		PrintThread{} << "[BotLaunchDiagnostics] bot=" << Agent.BotName
			<< " stage=open_stderr failed=true win32_error=" << errorCode
			<< " message=" << FormatWindowsError(errorCode)
			<< " path=" << stderrLogFile << std::endl;
	}

	HANDLE stdoutfile = NULL;
	if (Agent.Debug)
	{
		std::string stdoutFile = Agent.RootPath + "/data/stdout.log";
		stdoutfile = CreateFile(stdoutFile.c_str(),
			FILE_APPEND_DATA,
			FILE_SHARE_WRITE | FILE_SHARE_READ,
			&securityAttributes,
			OPEN_ALWAYS,
			FILE_ATTRIBUTE_NORMAL,
			NULL);
	}

	PROCESS_INFORMATION processInformation;
	STARTUPINFO startupInfo;
	DWORD flags = NORMAL_PRIORITY_CLASS | CREATE_NEW_CONSOLE; //CREATE_NO_WINDOW <-- also possible, but we don't see easily if bot is still running.

	ZeroMemory(&processInformation, sizeof(PROCESS_INFORMATION));
	ZeroMemory(&startupInfo, sizeof(STARTUPINFO));
	startupInfo.cb = sizeof(STARTUPINFO);
	startupInfo.dwFlags |= STARTF_USESTDHANDLES;
	startupInfo.hStdInput = INVALID_HANDLE_VALUE;
	startupInfo.hStdError = stderrfile != INVALID_HANDLE_VALUE
		? stderrfile
		: GetStdHandle(STD_ERROR_HANDLE);
	startupInfo.hStdOutput = stdoutfile;

	DWORD exitCode;
	std::vector<char> mutableCommandLine(CommandLine.begin(), CommandLine.end());
	mutableCommandLine.push_back('\0');
	LPSTR cmdLine = mutableCommandLine.data();
	const auto launchStartedAt = std::chrono::steady_clock::now();
	// Create the process

	BOOL result = CreateProcess(NULL, cmdLine,
		NULL, NULL, TRUE,
		flags,
		NULL, Agent.RootPath.c_str(), &startupInfo, &processInformation);


	if (!result)
	{
		const DWORD errorCode = GetLastError();
		PrintThread{} << "[BotLaunchDiagnostics] bot=" << Agent.BotName
			<< " stage=create_process failed=true win32_error=" << errorCode
			<< " message=" << FormatWindowsError(errorCode)
			<< " cwd=" << Agent.RootPath
			<< " stderr_path=" << stderrLogFile << std::endl;
		// We failed.
		exitCode = -1;
	}
	else
	{
		PrintThread{} << "Starting bot: " << Agent.BotName << " with command:" << CommandLine << std::endl;
		PrintThread{} << "[BotLaunchDiagnostics] bot=" << Agent.BotName
			<< " stage=create_process failed=false pid=" << processInformation.dwProcessId
			<< " cwd=" << Agent.RootPath
			<< " stderr_path=" << stderrLogFile << std::endl;
		// Successfully created the process.  Wait for it to finish.
		*ProcessId = processInformation.dwProcessId;
		WaitForSingleObject(processInformation.hProcess, INFINITE);

		// Get the exit code.
		result = GetExitCodeProcess(processInformation.hProcess, &exitCode);
		const DWORD exitCodeError = result ? ERROR_SUCCESS : GetLastError();
		const auto runtimeMs = std::chrono::duration_cast<std::chrono::milliseconds>(
			std::chrono::steady_clock::now() - launchStartedAt).count();
		PrintThread{} << "[BotLaunchDiagnostics] bot=" << Agent.BotName
			<< " stage=process_exit pid=" << processInformation.dwProcessId
			<< " runtime_ms=" << runtimeMs
			<< " exit_code=" << (result ? static_cast<long long>(exitCode) : -1LL)
			<< " get_exit_code_error=" << exitCodeError << std::endl;

		// Close the handles.
		CloseHandle(processInformation.hProcess);
		CloseHandle(processInformation.hThread);

		if (!result)
		{
			// Could not get exit code.
			exitCode = -1;
		}

		// We succeeded.
	}
	if (stderrfile != INVALID_HANDLE_VALUE)
	{
		FlushFileBuffers(stderrfile);
		CloseHandle(stderrfile);
	}
	if (Agent.Debug)
	{
		if (stdoutfile != INVALID_HANDLE_VALUE && stdoutfile != NULL)
		{
			CloseHandle(stdoutfile);
		}
	}
	const std::string stderrTail = ReadAppendedLogTail(stderrLogFile, stderrInitialSize);
	if (!stderrTail.empty())
	{
		PrintThread{} << "[BotLaunchDiagnostics] bot=" << Agent.BotName
			<< " stage=stderr_tail path=" << stderrLogFile
			<< " bytes=" << stderrTail.size() << std::endl
			<< stderrTail << std::endl;
	}
}

void StartExternalProcess(const std::string &CommandLine)
{
	STARTUPINFO si;
	PROCESS_INFORMATION pi;

	// set the size of the structures
	ZeroMemory(&si, sizeof(si));
	si.cb = sizeof(si);
	ZeroMemory(&pi, sizeof(pi));
	LPSTR cmdLine = const_cast<char *>(CommandLine.c_str());
	// Create the process

	BOOL result = CreateProcess(NULL, cmdLine,
		NULL, NULL, TRUE,
		0,
		NULL, NULL, &si, &pi);

	// Close process and thread handles. 
	CloseHandle(pi.hProcess);
	CloseHandle(pi.hThread);
}

void SleepFor(int seconds)
{
	Sleep(seconds * CLOCKS_PER_SEC);
}

void KillBotProcess(unsigned long pid)
{
	DWORD dwDesiredAccess = PROCESS_TERMINATE;
	BOOL  bInheritHandle = FALSE;
	HANDLE hProcess = OpenProcess(dwDesiredAccess, bInheritHandle, pid);
	if (hProcess == NULL)
		return;

	TerminateProcess(hProcess, 0);
	CloseHandle(hProcess);
}

bool MoveReplayFile(const char* lpExistingFileName, const char* lpNewFileName) {
	return MoveFile(lpExistingFileName, lpNewFileName);
}

std::string PerformRestRequest(const std::string &location, const std::vector<std::string> &arguments)
{
	std::array<char, 10000> buffer;
	std::string result;
	std::string command = "curl";
	for (const std::string &NextArgument : arguments)
	{
		command = command + NextArgument;
	}
	command = command + " " + location;
    PrintThread{} << command << std::endl;
	std::shared_ptr<FILE> pipe(_popen(command.c_str(), "r"), _pclose);
	if (!pipe)
	{
		throw std::runtime_error("popen() failed!");
	}
	while (!feof(pipe.get())) 
	{
		if (fgets(buffer.data(), 10000, pipe.get()) != nullptr)
		{
			result += buffer.data();
		}
	}
	return result;
}

bool ZipArchive(const std::string &InDirectory, const std::string &OutArchive)
{
    std::array<char, 10000> buffer;
    std::string CommandLIne = "powershell.exe -nologo -noprofile -command \"& { Add-Type -A 'System.IO.Compression.FileSystem'; [IO.Compression.ZipFile]::CreateFromDirectory('" + InDirectory + "', '" + OutArchive + "'); }\"";
	std::shared_ptr<FILE> pipe(_popen(CommandLIne.c_str(), "r"), _pclose);
	if (!pipe)
	{
		return false;
	}
    while (!feof(pipe.get()))
    {
        fgets(buffer.data(), 10000, pipe.get());
    }

	return true;
}

bool UnzipArchive(const std::string &InArchive, const std::string &OutDirectory)
{
    std::array<char, 10000> buffer;
    std::string CommandLIne = "powershell.exe -nologo -noprofile -command \"& { Add-Type -A 'System.IO.Compression.FileSystem'; [IO.Compression.ZipFile]::ExtractToDirectory('" + InArchive + "', '" + OutDirectory + "'); }\"";
	std::shared_ptr<FILE> pipe(_popen(CommandLIne.c_str(), "r"), _pclose);
	if (!pipe)
	{
		return false;
	}
    while (!feof(pipe.get()))
    {
        fgets(buffer.data(), 10000, pipe.get());
    }
    return true;
}

std::string GenerateMD5(std::string& filename)
{
    constexpr int BufferSize = 1024;
    constexpr int MD5Len = 16;
    std::string ReturnString;
    BOOL bResult = FALSE;
    HCRYPTPROV hProv = 0;
    HCRYPTHASH hHash = 0;
    HANDLE hFile = NULL;
    BYTE rgbFile[BufferSize];
    DWORD cbRead = 0;
    BYTE rgbHash[MD5Len];
    DWORD cbHash = 0;
    CHAR rgbDigits[] = "0123456789abcdef";
    // Logic to check usage goes here.

    hFile = CreateFile(filename.c_str(), GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_FLAG_SEQUENTIAL_SCAN, NULL);

    if (INVALID_HANDLE_VALUE == hFile)
    {
        printf("Error opening file %s\nError: %d\n", filename.c_str(), GetLastError());
        return ReturnString;
    }

    // Get handle to the crypto provider
    if (!CryptAcquireContext(&hProv, NULL, NULL, PROV_RSA_FULL, CRYPT_VERIFYCONTEXT))
    {
        printf("CryptAcquireContext failed: %d\n", GetLastError());
        CloseHandle(hFile);
        return ReturnString;
    }

    if (!CryptCreateHash(hProv, CALG_MD5, 0, 0, &hHash))
    {
        printf("CryptAcquireContext failed: %d\n", GetLastError());
        CloseHandle(hFile);
        CryptReleaseContext(hProv, 0);
        return ReturnString;
    }

    while (bResult = ReadFile(hFile, rgbFile, BufferSize,
        &cbRead, NULL))
    {
        if (0 == cbRead)
        {
            break;
        }

        if (!CryptHashData(hHash, rgbFile, cbRead, 0))
        {
            printf("CryptHashData failed: %d\n", GetLastError());
            CryptReleaseContext(hProv, 0);
            CryptDestroyHash(hHash);
            CloseHandle(hFile);
            return ReturnString;
        }
    }

    if (!bResult)
    {
        printf("ReadFile failed: %d\n", GetLastError());
        CryptReleaseContext(hProv, 0);
        CryptDestroyHash(hHash);
        CloseHandle(hFile);
        return ReturnString;
    }
    cbHash = MD5Len;
    if (CryptGetHashParam(hHash, HP_HASHVAL, rgbHash, &cbHash, 0))
    {
        constexpr int ArrayLen = MD5Len * 2 + 1;
        int CurrentChar = 0;
        for (DWORD i = 0; i < cbHash; i++)
        {
            ReturnString += rgbDigits[rgbHash[i] >> 4];
            ReturnString += rgbDigits[rgbHash[i] & 0xf];
        }
    }
    else
    {
        printf("CryptGetHashParam failed: %d\n", GetLastError());
    }

    CryptDestroyHash(hHash);
    CryptReleaseContext(hProv, 0);
    CloseHandle(hFile);

    return ReturnString;
}

bool MakeDirectory(const std::string& directory_name)
{
    return CreateDirectory(directory_name.c_str(), NULL);
}

#endif
