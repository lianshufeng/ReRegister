@echo off
cd /d %~dp0

:: 定义下载链接
set "MITMDUMP_URL=https://dl.jpy.wang/jetbrains/mitmdump.exe"
set "MODIFY_SCRIPT_URL=https://dl.jpy.wang/jetbrains/modify_scripts.py"

:: 定义文件路径
set "MITMDUMP_EXE=mitmdump.exe"
set "MODIFY_SCRIPT=modify_scripts.py"

:: 下载 mitmdump.exe（如果不存在）
if not exist "%MITMDUMP_EXE%" (
    echo mitmdump.exe 未找到，正在下载...
    curl -o "%MITMDUMP_EXE%" "%MITMDUMP_URL%"
    if %errorlevel% neq 0 (
        echo 下载 mitmdump.exe 失败！
        exit /b 1
    )
    echo mitmdump.exe 下载完成！
)

:: 强制重新下载 modify_scripts.py
echo 重新下载 modify_scripts.py ...
curl -o "%MODIFY_SCRIPT%" "%MODIFY_SCRIPT_URL%"
if %errorlevel% neq 0 (
    echo 下载 modify_scripts.py 失败！
    exit /b 1
)
echo modify_scripts.py 下载完成！

:: 启动 mitmdump 服务
echo 启动服务...
start mitmdump -p 8888 -s "%MODIFY_SCRIPT%"

:: 延迟 5 秒后导入证书
echo 延迟启动并导入证书到系统: %USERPROFILE%\.mitmproxy
timeout /t 5 /nobreak >nul

:: 导入证书
certutil -addstore root "%USERPROFILE%\.mitmproxy\mitmproxy-ca-cert.cer"

echo 任务完成！
