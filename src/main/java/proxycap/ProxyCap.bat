@echo off
setlocal enabledelayedexpansion

echo =============================================
echo     ProxyCap ��װ/�����Զ�������
echo =============================================
echo.
echo ���������ʼִ��ȫ������...
pause >nul

rem ===============================
rem �������ԱȨ��
rem ===============================
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo ���Թ���Ա������д˽ű������Ҽ� -> �Թ���Ա������У�
    pause
    goto :EOF
)

rem ===============================
rem ��������
rem ===============================
set "URL=https://proxy.jpy.wang/www.proxycap.com/download/pcap542_x64.msi"
for %%A in (%URL:/= %) do set "FILENAME=%%A"
set "TARGET_DIR=C:\ProxyCapReset"
set "TARGET_FILE=%TARGET_DIR%\%FILENAME%"
set "SERVICE_NAME=pcapsvc"
set "PROCESS_NAME=pcapui.exe"
set "BACKUP_REG=%TARGET_DIR%\backup_proxycap.reg"

echo.
echo === ��ʼ��Ŀ¼ ===
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%" 2>nul

rem ===============================
rem ���ذ�װ��
rem ===============================
if exist "%TARGET_FILE%" (
    echo ��װ���Ѵ��ڣ�%TARGET_FILE%
) else (
    echo �������أ�%URL%
    curl -L -o "%TARGET_FILE%" "%URL%"
    if errorlevel 1 (
        echo ����ʧ�ܣ������������ӡ�
        pause
        goto :EOF
    )
    echo ������ɣ�%TARGET_FILE%
)

rem ===============================
rem ����ע���
rem ===============================
echo.
echo === ���� ProxyCap ע���ֵ ===
if exist "%BACKUP_REG%" del /f /q "%BACKUP_REG%" >nul 2>&1
reg export "HKLM\SOFTWARE\WOW6432Node\Proxy Labs\ProxyCap" "%BACKUP_REG%" /y >nul 2>&1

if exist "%BACKUP_REG%" (
    echo ע����ѱ��ݵ���%BACKUP_REG%
    echo.
    echo ɾ�� Registration ���������ָ���
    reg delete "HKLM\SOFTWARE\WOW6432Node\Proxy Labs\ProxyCap\Registration" /f >nul 2>&1
) else (
    echo δ�ҵ�ע����� ProxyCap���������ݡ�
)

rem ===============================
rem ֹͣ���� & ɱ����
rem ===============================
echo.
echo === ֹͣ ProxyCap ����ͽ��� ===
net stop %SERVICE_NAME% >nul 2>&1
if %errorlevel% equ 0 (echo ���� %SERVICE_NAME% ��ֹͣ) else (echo ���� %SERVICE_NAME% �����ڻ���ֹͣ��)

taskkill /im %PROCESS_NAME% /f >nul 2>&1
if %errorlevel% equ 0 (echo ����ֹ���� %PROCESS_NAME%) else (echo ���� %PROCESS_NAME% δ���С�)

rem ===============================
rem ����ע���
rem ===============================
echo.
echo === �����ע����� ===
reg delete "HKLM\SOFTWARE\WOW6432Node\Proxy Labs" /f >nul 2>&1
reg delete "HKLM\SOFTWARE\WOW6432Node\SB" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\pcapsvc" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\Arp" /f >nul 2>&1
echo ������ɡ�

rem ===============================
rem ж�ؾɰ汾
rem ===============================
echo.
echo === ж�ؾɰ汾 ===
if exist "%TARGET_FILE%" (
    msiexec /x "%TARGET_FILE%" /quiet /norestart
    echo ж�ز�����ɣ�����δ��װ�������
) else (
    echo δ�ҵ���װ��������ж�ء�
)

rem ===============================
rem ���°�װ
rem ===============================
echo.
echo === ���°�װ����Ĭ�� ===
if exist "%TARGET_FILE%" (
    msiexec /i "%TARGET_FILE%" /quiet /norestart
    if errorlevel 1 (
        echo ��װʧ�ܣ������룺%errorlevel%����
    ) else (
        echo ��װ��ɡ�
    )
) else (
    echo δ�ҵ���װ����
)

rem ===============================
rem �ָ�ע���
rem ===============================
echo.
echo === �ָ� ProxyCap ע��� ===
if exist "%BACKUP_REG%" (
    reg import "%BACKUP_REG%" >nul 2>&1
    if errorlevel 1 (
        echo �ָ�ʧ�ܣ�����ע����ļ���%BACKUP_REG%
    ) else (
        echo ע���ָ���ɡ�
    )
) else (
    echo δ�ҵ�ע������ļ��������ָ���
)

rem ===============================
rem ������ʾ
rem ===============================
echo.
echo =============================================
echo ��������ִ����ɣ�
echo �������������ʹ������Ч��
echo =============================================
echo.
pause
endlocal
