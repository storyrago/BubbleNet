#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
javac -encoding UTF-8 --release 17 -d build/classes src/main/java/arcade/*.java src/main/java/arcade/*/*.java
java -cp build/classes arcade.Main "$@"
