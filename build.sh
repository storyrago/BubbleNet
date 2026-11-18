#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/package-classes
javac -encoding UTF-8 --release 17 -d build/package-classes src/main/java/arcade/*.java src/main/java/arcade/*/*.java
jar --create --file build/bubble-arcade.jar --main-class arcade.Main -C build/package-classes arcade assets/original assets/audio assets/ATTRIBUTION.md
printf '%s\n' 'Built: build/bubble-arcade.jar'
