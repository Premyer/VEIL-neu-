#!/usr/bin/env bash
set -euo pipefail

veil_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
veil_source="${VEIL_FIREFOX_DIR:-${RUNTIME_ROOT:-$HOME/.cache/veil}/repos/firefox}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
export MOZCONFIG="$veil_root/browser/mozconfig"

cd "$veil_root"
python3 -m unittest discover -s browser/tests -v
for script in browser/build.sh browser/check.sh; do
    bash -n "$script"
done

cd "$veil_source"
mkdir -p artifacts
if ./mach gradle fenix:lintDebug \
    --no-daemon --max-workers=1 --console=plain \
    '-Dorg.gradle.jvmargs=-Xmx4g -Xms256m -XX:MaxMetaspaceSize=1g' \
    -Pkotlin.compiler.execution.strategy=in-process -PdisableLeakCanary -PdisableAndroidComponentsTasks \
    > artifacts/veil-lint.log 2>&1; then
    python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

report = Path("objdir-frontend/gradle/build/mobile/android/fenix/app/reports/lint-results-debug.xml")
issues = ET.parse(report).getroot().findall("issue")
errors = [issue for issue in issues if issue.get("severity") in {"Error", "Fatal"}]
for issue in errors:
    print(issue.get("id"), issue.get("message"))
if errors:
    raise SystemExit(f"Android lint found {len(errors)} errors.")
print(f"Android lint: no errors; {len(issues)} other reported issues.")
PY
else
    veil_result=$?
    cat artifacts/veil-lint.log >&2
    exit "$veil_result"
fi

# Release lint's heap before compiling/running the unit-test suite.
if ./mach gradle fenix:testDebugUnitTest \
    --tests org.mozilla.fenix.theme.VeilThemeTest \
    --tests org.mozilla.fenix.utils.SettingsTest \
    --no-daemon --max-workers=1 --console=plain \
    '-Dorg.gradle.jvmargs=-Xmx3g -Xms256m -XX:MaxMetaspaceSize=1g' \
    -Pkotlin.compiler.execution.strategy=in-process -PdisableLeakCanary -PdisableAndroidComponentsTasks \
    > artifacts/veil-unit-tests.log 2>&1; then
    echo "VEIL theme/settings unit tests passed."
else
    veil_result=$?
    cat artifacts/veil-unit-tests.log >&2
    exit "$veil_result"
fi
