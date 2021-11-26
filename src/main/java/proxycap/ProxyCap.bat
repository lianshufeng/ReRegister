@echo off

:: build
mkdir %tmp%\_proxycap
cd /d %tmp%\_proxycap

:: download
curl https://web.api.jpy.wang/proxycap/Registration.reg -o Registration.reg
type Registration.reg

:: load
echo loading... Registration.reg
regedit /s Registration.reg
ping -n 3 127.0.0.1>nul

:: clean
rd /s /q %tmp%\_proxycap

pause