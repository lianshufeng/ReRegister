@echo off
setlocal enabledelayedexpansion

echo =============================================
echo     ProxyCap 安装/重置自动化工具
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
set "URL=https://proxy.jpy.wang/www.proxycap.com/download/pcap542_x64.msi"
for %%A in (%URL:/= %) do set "FILENAME=%%A"
set "TARGET_DIR=C:\ProxyCapReset"
set "TARGET_FILE=%TARGET_DIR%\%FILENAME%"
set "SERVICE_NAME=pcapsvc"
set "PROCESS_NAME=pcapui.exe"
set "BACKUP_REG=%TARGET_DIR%\backup_proxycap.reg"

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
rem 备份注册表
rem ===============================
echo.
echo === 备份 ProxyCap 注册表值 ===
if exist "%BACKUP_REG%" del /f /q "%BACKUP_REG%" >nul 2>&1
reg export "HKLM\SOFTWARE\WOW6432Node\Proxy Labs\ProxyCap" "%BACKUP_REG%" /y >nul 2>&1

if exist "%BACKUP_REG%" (
    echo 注册表已备份到：%BACKUP_REG%
    echo.
    echo 删除 Registration 子项（不参与恢复）
    reg delete "HKLM\SOFTWARE\WOW6432Node\Proxy Labs\ProxyCap\Registration" /f >nul 2>&1
) else (
    echo 未找到注册表项 ProxyCap，跳过备份。
)

rem ===============================
rem 停止服务 & 杀进程
rem ===============================
echo.
echo === 停止 ProxyCap 服务和进程 ===
net stop %SERVICE_NAME% >nul 2>&1
if %errorlevel% equ 0 (echo 服务 %SERVICE_NAME% 已停止) else (echo 服务 %SERVICE_NAME% 不存在或已停止。)

taskkill /im %PROCESS_NAME% /f >nul 2>&1
if %errorlevel% equ 0 (echo 已终止进程 %PROCESS_NAME%) else (echo 进程 %PROCESS_NAME% 未运行。)

rem ===============================
rem 清理注册表
rem ===============================
echo.
echo === 清理旧注册表项 ===
reg delete "HKLM\SOFTWARE\WOW6432Node\Proxy Labs" /f >nul 2>&1
reg delete "HKLM\SOFTWARE\WOW6432Node\SB" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\pcapsvc" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\Arp" /f >nul 2>&1
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
rem 恢复注册表
rem ===============================
echo.
echo === 恢复 ProxyCap 注册表 ===
if exist "%BACKUP_REG%" (
    reg import "%BACKUP_REG%" >nul 2>&1
    if errorlevel 1 (
        echo 恢复失败，请检查注册表文件：%BACKUP_REG%
    ) else (
        echo 注册表恢复完成。
    )
) else (
    echo 未找到注册表备份文件，跳过恢复。
)

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
