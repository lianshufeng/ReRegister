@echo off
setlocal enabledelayedexpansion

echo =============================================
echo   NetDetour 安装/重置自动化工具
echo =============================================
echo.
echo 按任意键开始执行全部操作...
pause >nul

rem ===============================
rem 请检查管理员权限
rem ===============================
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo 请以管理员身份运行此脚本！（右键 -> 以管理员身份运行）
    pause
    goto :EOF
)

rem ===============================
rem 变量定义
rem ===============================
set "URL=https://www.netdetour.com/download/ndt112-64.msi"
for %%A in (%URL:/= %) do set "FILENAME=%%A"
set "TARGET_DIR=C:\netdetour"
set "TARGET_FILE=%TARGET_DIR%\%FILENAME%"
set "SERVICE_NAME=ndetourd"
set "PROCESS_NAME=ndtgui.exe"
set "BACKUP_WFP="

echo.
echo === 初始化目录 ===
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%" 2>nul

rem ===============================
rem 下载安装包
rem ===============================
if exist "%TARGET_FILE%" (
    echo 安装包已存在：%TARGET_FILE%
) else (
    echo 正在下载：%URL%
    curl -L -o "%TARGET_FILE%" "%URL%"
    if errorlevel 1 (
        echo 下载失败，请检查网络连接。
        pause
        goto :EOF
    )
    echo 下载完成：%TARGET_FILE%
)

rem ===============================
rem 备份 UsingWfp 值到变量
rem ===============================
echo.
echo === 备份 UsingWfp 注册表值 ===
for /f "tokens=3" %%A in ('reg query "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v UsingWfp 2^>nul ^| find /i "UsingWfp"') do (
    set "BACKUP_WFP=%%A"
)
if defined BACKUP_WFP (
    echo 已备份 UsingWfp 值：!BACKUP_WFP!
) else (
    echo 未找到 UsingWfp，使用默认值 0。
)

rem ===============================
rem 杀进程 & 停服务
rem ===============================
echo.
echo === 结束进程 ndtgui.exe ===
taskkill /im %PROCESS_NAME% /f >nul 2>&1
if %errorlevel% equ 0 (echo 已终止进程 %PROCESS_NAME%) else (echo 进程 %PROCESS_NAME% 未运行。)

echo.
echo === 停止服务 ndetourd ===
net stop %SERVICE_NAME% >nul 2>&1
if %errorlevel% equ 0 (echo 服务 %SERVICE_NAME% 已停止) else (echo 服务 %SERVICE_NAME% 不存在或已停止。)

rem ===============================
rem 清理注册表
rem ===============================
echo.
echo === 清理旧注册表项 ===
reg delete "HKLM\SOFTWARE\WOW6432Node\NetDetour" /f >nul 2>&1
reg delete "HKLM\SOFTWARE\WOW6432Node\bscupcli.dll" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\VSS\Diag\SPP" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\MSTCP" /f >nul 2>&1
echo 清理完成。

rem ===============================
rem 卸载旧版本
rem ===============================
echo.
echo === 卸载旧版本 ===
if exist "%TARGET_FILE%" (
    msiexec /x "%TARGET_FILE%" /quiet /norestart
    echo 卸载操作完成（忽略未安装情况）。
) else (
    echo 未找到安装包，跳过卸载。
)

rem ===============================
rem 重新安装
rem ===============================
echo.
echo === 重新安装（静默） ===
if exist "%TARGET_FILE%" (
    msiexec /i "%TARGET_FILE%" /quiet /norestart
    if errorlevel 1 (
        echo 安装失败（错误码：%errorlevel%）。
    ) else (
        echo 安装完成。
    )
) else (
    echo 未找到安装包。
)

rem ===============================
rem 写入注册表
rem ===============================
echo.
echo === 写入注册表值 ===
reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /f >nul
reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "NCK" /t REG_SZ /d "C:\\ProgramData\\NetDetour\\nck.dat" /f >nul
if defined BACKUP_WFP (
    rem 将原始十六进制值恢复
    reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "UsingWfp" /t REG_DWORD /d !BACKUP_WFP! /f >nul
    echo 恢复 UsingWfp 值：!BACKUP_WFP!
) else (
    reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "UsingWfp" /t REG_DWORD /d 0 /f >nul
    echo 写入默认 UsingWfp=0
)
reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "UPD" /t REG_SZ /d "C:\\ProgramData\\NetDetour\\updates.dat" /f >nul
reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "Nps" /t REG_SZ /d "C:\\ProgramData\\NetDetour\\config.nps" /f >nul
echo 注册表写入完成。

rem ===============================
rem 结束提示
rem ===============================
echo.
echo =============================================
echo 所有任务执行完成！
echo 请重启计算机以使更改生效。
echo =============================================
echo.
pause
endlocal
