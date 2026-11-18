#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/test-classes
find src/main/java -name '*.java' | sort > build/main-sources.txt
javac -encoding UTF-8 -source 8 -target 8 -d build/classes @build/main-sources.txt
find src/test/java -name '*.java' | sort > build/test-sources.txt
javac -encoding UTF-8 -source 8 -target 8 -cp build/classes -d build/test-classes @build/test-sources.txt
java -cp build/classes:build/test-classes com.sandhya.applytrack.AllTests
jar cfe applytrack.jar com.sandhya.applytrack.Main -C build/classes .
echo "Built applytrack.jar"
