@echo off
cd /d "%~dp0"
java -jar authlog-analyzer.jar %*
if errorlevel 1 pause
