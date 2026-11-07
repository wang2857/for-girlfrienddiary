#!/usr/bin/env bash
# 这台机器原本没有 Java/Android SDK，工具链装在仓库同级的 .toolchain 里（不污染系统）。
# 任何 gradle 命令都可以通过它执行，例如：./build.sh assembleDebug
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

export JAVA_HOME="$ROOT/.toolchain/jdk/amazon-corretto-21.jdk/Contents/Home"
export ANDROID_HOME="$ROOT/.toolchain/android-sdk"
export ANDROID_USER_HOME="$ROOT/.toolchain/android-user-home"
export GRADLE_USER_HOME="$ROOT/.toolchain/gradle-home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

exec "$ROOT/.toolchain/gradle/bin/gradle" -p "$ROOT/diary" "$@"
