#!/usr/bin/env bash
# Convenience wrapper: pins the JDK 21 this project builds with.
set -euo pipefail
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Eclipse Adoptium/jdk-21.0.5.11-hotspot}"
export PATH="$JAVA_HOME/bin:$PATH"
cd "$(dirname "$0")/.."
exec ./gradlew "${@:-assembleDebug}"
