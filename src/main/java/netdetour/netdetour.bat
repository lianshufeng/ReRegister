@echo off
setlocal enabledelayedexpansion

echo =============================================
echo   NetDetour ��װ/�����Զ�������
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
set "URL=https://www.netdetour.com/download/ndt112-64.msi"
for %%A in (%URL:/= %) do set "FILENAME=%%A"
set "TARGET_DIR=C:\netdetour"
set "TARGET_FILE=%TARGET_DIR%\%FILENAME%"
set "SERVICE_NAME=ndetourd"
set "PROCESS_NAME=ndtgui.exe"
set "BACKUP_WFP="

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
rem ���� UsingWfp ֵ������
rem ===============================
echo.
echo === ���� UsingWfp ע���ֵ ===
for /f "tokens=3" %%A in ('reg query "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v UsingWfp 2^>nul ^| find /i "UsingWfp"') do (
    set "BACKUP_WFP=%%A"
)
if defined BACKUP_WFP (
    echo �ѱ��� UsingWfp ֵ��!BACKUP_WFP!
) else (
    echo δ�ҵ� UsingWfp��ʹ��Ĭ��ֵ 0��
)

rem ===============================
rem ɱ���� & ͣ����
rem ===============================
echo.
echo === �������� ndtgui.exe ===
taskkill /im %PROCESS_NAME% /f >nul 2>&1
if %errorlevel% equ 0 (echo ����ֹ���� %PROCESS_NAME%) else (echo ���� %PROCESS_NAME% δ���С�)

echo.
echo === ֹͣ���� ndetourd ===
net stop %SERVICE_NAME% >nul 2>&1
if %errorlevel% equ 0 (echo ���� %SERVICE_NAME% ��ֹͣ) else (echo ���� %SERVICE_NAME% �����ڻ���ֹͣ��)

rem ===============================
rem ����ע���
rem ===============================
echo.
echo === �����ע����� ===
reg delete "HKLM\SOFTWARE\WOW6432Node\NetDetour" /f >nul 2>&1
reg delete "HKLM\SOFTWARE\WOW6432Node\bscupcli.dll" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\VSS\Diag\SPP" /f >nul 2>&1
reg delete "HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\MSTCP" /f >nul 2>&1
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
rem д��ע���
rem ===============================
echo.
echo === д��ע���ֵ ===
reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /f >nul
reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "NCK" /t REG_SZ /d "C:\\ProgramData\\NetDetour\\nck.dat" /f >nul
if defined BACKUP_WFP (
    rem ��ԭʼʮ������ֵ�ָ�
    reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "UsingWfp" /t REG_DWORD /d !BACKUP_WFP! /f >nul
    echo �ָ� UsingWfp ֵ��!BACKUP_WFP!
) else (
    reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "UsingWfp" /t REG_DWORD /d 0 /f >nul
    echo д��Ĭ�� UsingWfp=0
)
reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "UPD" /t REG_SZ /d "C:\\ProgramData\\NetDetour\\updates.dat" /f >nul
reg add "HKLM\SOFTWARE\WOW6432Node\NetDetour" /v "Nps" /t REG_SZ /d "C:\\ProgramData\\NetDetour\\config.nps" /f >nul
echo ע���д����ɡ�

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
