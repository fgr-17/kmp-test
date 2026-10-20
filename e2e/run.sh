#!/usr/bin/env bash
# Run the shared Gherkin feature with one driver (web|android|ios|desktop).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DRIVER="${1:-}"
if [[ -z "${DRIVER}" ]]; then
  echo "Usage: $0 <web|android|ios|desktop>"
  echo
  echo "Prereqs depend on the driver — see docs/QUALITY_AND_TESTING.md"
  exit 1
fi

case "${DRIVER}" in
  web|android|ios|desktop) ;;
  *)
    echo "Unknown driver: ${DRIVER}"
    exit 1
    ;;
esac

cd "${ROOT}/e2e"
mkdir -p reports/html "reports/junit/${DRIVER}"

VENV="${ROOT}/e2e/.venv"
if [[ ! -x "${VENV}/bin/behave" ]]; then
  echo "Creating e2e Python environment"
  python3 -m venv "${VENV}"
  "${VENV}/bin/pip" install -r "${ROOT}/e2e/requirements.txt"
fi
BEHAVE="${VENV}/bin/behave"
# macOS system Python links against LibreSSL. urllib3 2 warns about that, and
# Selenium requires urllib3 2, so the message is filtered. The category cannot
# be named here: Python applies PYTHONWARNINGS before third-party packages import.
export PYTHONWARNINGS="${PYTHONWARNINGS:+${PYTHONWARNINGS},}ignore:urllib3 v2 only supports OpenSSL"

if [[ "${DRIVER}" == "web" ]]; then
  if ! "${VENV}/bin/python" -c 'import os
from playwright.sync_api import sync_playwright
playwright = sync_playwright().start()
try:
    path = playwright.chromium.executable_path
    raise SystemExit(0 if path and os.path.exists(path) else 1)
finally:
    playwright.stop()' >/dev/null 2>&1; then
    echo "Installing Playwright Chromium"
    "${VENV}/bin/playwright" install chromium
  fi
fi

bash "${ROOT}/scripts/ensure_services.sh" "${DRIVER}"

stop_android_emulator() {
  local adb
  adb="$(bash "${ROOT}/scripts/android_sdk_dir.sh")/platform-tools/adb"
  [[ -x "${adb}" ]] || return 0
  local serial
  serial="$("${adb}" devices | awk '/^emulator-/{print $1; exit}')"
  [[ -n "${serial}" ]] || return 0
  echo "Stopping Android emulator ${serial}"
  "${adb}" -s "${serial}" emu kill >/dev/null 2>&1 || true
  local i
  for ((i = 1; i <= 20; i++)); do
    if ! "${adb}" devices | grep -q "^${serial}[[:space:]]"; then
      return 0
    fi
    sleep 0.5
  done
}

if [[ "${DRIVER}" == "android" ]]; then
  trap stop_android_emulator EXIT
fi

# Behave 1.2.x notes:
# - JUnit is --junit (there is no -f junit)
# - Interleave -f/-o so pretty stays on stdout and html goes to a file
PYTHONPATH="${ROOT}/e2e${PYTHONPATH:+:$PYTHONPATH}" \
  "${BEHAVE}" \
    -D "driver=${DRIVER}" \
    features/offline_notes.feature \
    -f pretty -o /dev/stdout \
    -f html -o "reports/html/${DRIVER}.html" \
    --junit \
    --junit-directory="reports/junit/${DRIVER}" \
    --no-capture
