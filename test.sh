#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
javac -encoding UTF-8 --release 17 -d build/classes src/main/java/arcade/*.java src/main/java/arcade/*/*.java src/test/java/arcade/*.java
java -Djava.awt.headless=true -cp build/classes arcade.GameTest
