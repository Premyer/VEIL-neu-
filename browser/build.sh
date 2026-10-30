#!/usr/bin/env bash
set -euo pipefail

veil_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
veil_source="${VEIL_FIREFOX_DIR:-${RUNTIME_ROOT:-$HOME/.cache/veil}/repos/firefox}"
veil_output="${VEIL_OUTPUT_DIR:-$veil_root/build/outputs/apk}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"

if [[ "${1:-}" != "" && "${1:-}" != "--setup" ]]; then
    echo "Usage: bash browser/build.sh [--setup]" >&2
    exit 2
fi

python3 "$veil_root/browser/fetch_source.py" "$veil_source"
export MOZCONFIG="$veil_root/browser/mozconfig"
# Also applies to Gradle invoked internally by mach build.
export GRADLE_FLAGS="${GRADLE_FLAGS:-} --no-daemon -Pkotlin.compiler.execution.strategy=in-process"
export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.jvmargs=\"-Xmx3g -Xms256m -XX:MaxMetaspaceSize=1g\" -Dorg.gradle.workers.max=2"
mkdir -p "$veil_source/artifacts"
cd "$veil_source"

run_step() {
    local name="$1"
    shift
    echo "$name (log: $veil_source/artifacts/$name.log)"
    if "$@" > "artifacts/$name.log" 2>&1; then
        return 0
    else
        local result=$?
        cat "artifacts/$name.log" >&2
        return "$result"
    fi
}

if [[ "${1:-}" == "--setup" ]]; then
    # Mozilla bootstrap uses this legacy variable for SDK discovery. Keep it
    # scoped there so Android tooling retains the normal user debug-keystore path.
    run_step bootstrap env ANDROID_SDK_HOME="$ANDROID_HOME" ./mach --no-interactive bootstrap --application-choice=mobile_android_artifact_mode
fi
python3 "$veil_root/browser/apply_overlay.py" "$veil_source"
run_step configure ./mach configure
run_step native-artifacts ./mach build
run_step veil-apk ./mach gradle fenix:assembleDebug --no-daemon --max-workers=2 --console=plain \
    '-Dorg.gradle.jvmargs=-Xmx3g -Xms256m -XX:MaxMetaspaceSize=1g' \
    -Pkotlin.compiler.execution.strategy=in-process -PdisableLeakCanary
python3 "$veil_root/browser/export_apk.py" "$veil_source" "$veil_output"
