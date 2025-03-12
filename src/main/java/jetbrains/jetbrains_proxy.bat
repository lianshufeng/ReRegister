@echo off
cd /d %~dp0

:: 定义下载链接
set "MITMDUMP_URL=https://dl.jpy.wang/jetbrains/mitmdump.exe"
set "MODIFY_SCRIPT_URL=https://dl.jpy.wang/jetbrains/modify_scripts.py"

:: 定义文件路径
set "MITMDUMP_EXE=mitmdump.exe"
set "MODIFY_SCRIPT=modify_scripts.py"

:: 检查 mitmdump.exe 是否存在
if not exist "%MITMDUMP_EXE%" (
    echo mitmdump.exe 未找到，正在下载...
    curl -o "%MITMDUMP_EXE%" "%MITMDUMP_URL%"
    if %errorlevel% neq 0 (
        echo 下载 mitmdump.exe 失败！
        exit /b 1
    )
    echo mitmdump.exe 下载完成！
)

:: 检查 modify_scripts.py 是否存在
if not exist "%MODIFY_SCRIPT%" (
    echo modify_scripts.py 未找到，正在下载...
    curl -o "%MODIFY_SCRIPT%" "%MODIFY_SCRIPT_URL%"
    if %errorlevel% neq 0 (
        echo 下载 modify_scripts.py 失败！
        exit /b 1
    )
    echo modify_scripts.py 下载完成！
)

echo 启动服务...
start mitmdump -p 8888 -s .\modify_scripts.py 

echo 延迟启动并导入证书到系统 : %USERPROFILE%\.mitmproxy
timeout 5

:: 导入证书
certutil -addstore root %USERPROFILE%\.mitmproxy\mitmproxy-ca-cert.cer
