#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="$ROOT_DIR/.toolchain/jdk/Contents/Home"

"$ROOT_DIR/.toolchain/maven/bin/mvn" \
  -Dmaven.repo.local="$ROOT_DIR/.m2repo" \
  package \
  -DskipTests
