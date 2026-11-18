@echo off
cd /d "%~dp0"
java -jar applytrack.jar %*
if errorlevel 1 pause
