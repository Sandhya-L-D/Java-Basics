@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
if not exist build\test-classes mkdir build\test-classes
(for /r src\main\java %%f in (*.java) do @echo "%%f") > build\main-sources.txt
javac -encoding UTF-8 -source 8 -target 8 -d build\classes @build\main-sources.txt
if errorlevel 1 exit /b 1
(for /r src\test\java %%f in (*.java) do @echo "%%f") > build\test-sources.txt
javac -encoding UTF-8 -source 8 -target 8 -cp build\classes -d build\test-classes @build\test-sources.txt
if errorlevel 1 exit /b 1
java -cp "build\classes;build\test-classes" com.sandhya.authlog.AllTests
if errorlevel 1 exit /b 1
jar cfe authlog-analyzer.jar com.sandhya.authlog.Main -C build\classes .
if errorlevel 1 exit /b 1
echo Built authlog-analyzer.jar
