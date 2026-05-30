@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

title NetDetour 重装工具

echo.
echo ============================================================
echo                    NetDetour 重装工具
echo ============================================================
echo.

rem ============================================================
rem 风险确认
rem ============================================================
echo ************************************************************
echo *                       重 要 提 示                         *
echo ************************************************************
echo.
echo 本工具将执行以下操作：
echo.
echo   1. 关闭 NetDetour 相关进程
echo   2. 停止 NetDetour 服务
echo   3. 删除旧注册表配置
echo   4. 卸载当前版本
echo   5. 重新安装 NetDetour
echo   6. 恢复 UsingWfp 配置项
echo.
echo 可能产生的影响：
echo.
echo   - NetDetour 现有配置可能丢失
echo   - 规则、策略、节点等配置可能无法恢复
echo.
echo 请务必提前备份重要配置。
echo.

choice /c YN /m "确认继续执行"

if errorlevel 2 (
    echo.
    echo 已取消操作。
    pause
    goto :EOF
)

rem ============================================================
rem 检查管理员权限
rem ============================================================
echo.
echo [1/8] 检查管理员权限...

net session >nul 2>&1

if not "%errorlevel%"=="0" (
    echo.
    echo 错误：请以管理员身份运行本工具。
    echo.
    echo 操作步骤：
    echo   1. 关闭当前窗口
    echo   2. 右键点击本脚本
    echo   3. 选择“以管理员身份运行”
    echo.
    pause
    goto :EOF
)

echo 管理员权限检查通过。

rem ============================================================
rem 变量定义
rem ============================================================
set "URL=https://www.netdetour.com/download/ndt114-64.msi"
for %%A in (%URL:/= %) do set "FILENAME=%%A"

set "TARGET_DIR=C:\NetDetour"
set "TARGET_FILE=%TARGET_DIR%\%FILENAME%"

set "SERVICE_NAME=ndetourd"
set "PROCESS_NAME=ndtgui.exe"

set "BACKUP_WFP="

rem ============================================================
rem 创建目录
rem ============================================================
echo.
echo [2/8] 初始化目录...

if not exist "%TARGET_DIR%" (
    mkdir "%TARGET_DIR%" >nul 2>&1
)

echo 工作目录：
echo %TARGET_DIR%

rem ============================================================
rem 下载安装包
rem ============================================================
echo.
echo [3/8] 检查安装包...

if exist "%TARGET_FILE%" (
    echo 已存在安装包：
    echo %TARGET_FILE%
) else (
    echo 正在下载安装包...
    echo.
    echo 下载地址：
    echo %URL%
    echo.

    curl -k -L -o "%TARGET_FILE%" "%URL%"

    if errorlevel 1 (
        echo.
        echo 错误：安装包下载失败。
        echo 请检查网络连接后重试。
        pause
        goto :EOF
    )

    echo.
    echo 安装包下载完成。
)

rem ============================================================
rem 备份注册表
rem ============================================================
echo.
echo [4/8] 备份配置...

for /f "tokens=3" %%A in ('
    reg query "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v UsingWfp 2^>nul ^| find /i "UsingWfp"
') do (
    set "BACKUP_WFP=%%A"
)

if defined BACKUP_WFP (
    echo 已备份 UsingWfp 值：!BACKUP_WFP!
) else (
    echo 未发现 UsingWfp 配置。
    echo 安装后将使用默认值 0。
)

rem ============================================================
rem 停止程序和服务
rem ============================================================
echo.
echo [5/8] 关闭程序和服务...

taskkill /f /im %PROCESS_NAME% >nul 2>&1

if "%errorlevel%"=="0" (
    echo 已关闭进程：%PROCESS_NAME%
) else (
    echo 进程未运行：%PROCESS_NAME%
)

net stop %SERVICE_NAME% >nul 2>&1

if "%errorlevel%"=="0" (
    echo 已停止服务：%SERVICE_NAME%
) else (
    echo 服务未运行或不存在：%SERVICE_NAME%
)

rem ============================================================
rem 清理注册表
rem ============================================================
echo.
echo [6/8] 清理旧配置...

reg delete "HKLM\SOFTWARE\WOW6432Node\NetDetour" /f >nul 2>&1
reg delete "HKLM\SOFTWARE\WOW6432Node\bscupcli.dll" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\VSS\Diag\SPP" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\MSTCP" /f >nul 2>&1

echo 注册表清理完成。

rem ============================================================
rem 卸载旧版本
rem ============================================================
echo.
echo [7/8] 卸载旧版本...

if exist "%TARGET_FILE%" (
    msiexec /x "%TARGET_FILE%" /quiet /norestart

    echo 卸载完成。
) else (
    echo 未找到安装包，跳过卸载。
)

rem ============================================================
rem 安装新版本
rem ============================================================
echo.
echo [8/8] 安装新版本...

msiexec /i "%TARGET_FILE%" /quiet /norestart

if errorlevel 1 (
    echo.
    echo 安装失败。
    echo 错误代码：%errorlevel%
    pause
    goto :EOF
)

echo 安装完成。

rem ============================================================
rem 恢复配置
rem ============================================================
echo.
echo 正在恢复配置...

reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /f >nul

if defined BACKUP_WFP (
    reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" ^
        /v "UsingWfp" ^
        /t REG_DWORD ^
        /d !BACKUP_WFP! ^
        /f >nul

    echo 已恢复 UsingWfp=!BACKUP_WFP!
) else (
    reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" ^
        /v "UsingWfp" ^
        /t REG_DWORD ^
        /d 0 ^
        /f >nul

    echo 已写入默认值 UsingWfp=0
)

echo.
echo ============================================================
echo                       执 行 完 成
echo ============================================================
echo.
echo NetDetour 已重新安装完成。
echo.
echo 建议操作：
echo   1. 重启计算机
echo   2. 启动 NetDetour
echo   3. 导入备份配置
echo   4. 检查代理规则是否正常
echo.
echo ============================================================
echo.

pause
endlocal
exit /b 0