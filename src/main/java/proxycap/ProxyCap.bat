@echo off

:: work
echo ProxyCap ReRegister...
cd /d %tmp%

:: download
echo download... Registration.reg
curl https://web.api.jpy.wang/proxycap/Registration.reg -o _Registration.reg
type _Registration.reg


:: load
echo loading... Registration.reg
regedit /s _Registration.reg
ping -n 5 127.0.0.1>nul

:: clean
del _Registration.reg

echo finish...
echo -----------------------------------------
pause