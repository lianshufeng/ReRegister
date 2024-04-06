@echo off

:: remove reg
reg delete "HKEY_CURRENT_USER\Software\JavaSoft\Prefs\3t\mongochef\enterprise" /f

:: remove diskfile
rd /s /q %USERPROFILE%\\AppData\\Local\\ftuwWNWoJl-STeZhVGHKkQ--
rd /s /q %USERPROFILE%\\AppData\\Local\\t3
rd /s /q %USERPROFILE%\\AppData\\Local\\Temp\\ftuwWNWoJl-STeZhVGHKkQ--
rd /s /q %USERPROFILE%\\AppData\\Local\\Temp\\t3
rd /s /q %PUBLIC%\\t3
rd /s /q %USERPROFILE%\\.cache\\ftuwWNWoJl-STeZhVGHKkQ--
rd /s /q %USERPROFILE%\\.3T\\studio-3t\\soduz3vqhnnja46uvu3szq--


echo finish...
echo -----------------------------------------
pause