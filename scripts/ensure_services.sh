#!/usr/bin/env bash
# Start local services an E2E driver needs when they are not already up.
# Usage: scripts/ensure_services.sh <web|android|ios|desktop>
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DRIVER="${1:-}"
CLOUD_URL="${GREETING_CLOUD_URL:-http://127.0.0.1:8081}"
ANDROID_SDK="$(bash "${ROOT}/scripts/android_sdk_dir.sh")"
export ANDROID_HOME="${ANDROID_SDK}"
export PATH="${ANDROID_SDK}/platform-tools:${ANDROID_SDK}/emulator:${PATH}"

mkdir -p "${ROOT}/build/logs" "${ROOT}/build/run"

listening() {
  lsof -nP -tiTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

wait_until() {
  local description="$1"
  local attempts="$2"
  shift 2
  local i
  for ((i = 1; i <= attempts; i++)); do
    if "$@"; then
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for ${description}" >&2
  return 1
}

# Detach so the service survives the Make process that started it.
start_logged() {
  local name="$1"
  local workdir="$2"
  shift 2
  echo "Starting ${name}"
  ROOT="${ROOT}" python3 -c '
import os, sys
name, workdir = sys.argv[1], sys.argv[2]
cmd = sys.argv[3:]
root = os.environ["ROOT"]
os.makedirs(os.path.join(root, "build", "logs"), exist_ok=True)
os.makedirs(os.path.join(root, "build", "run"), exist_ok=True)
log = open(os.path.join(root, "build", "logs", name + ".log"), "a")
pid = os.fork()
if pid > 0:
    open(os.path.join(root, "build", "run", name + ".pid"), "w").write(str(pid))
    raise SystemExit(0)
os.setsid()
os.chdir(workdir)
os.dup2(log.fileno(), 1)
os.dup2(log.fileno(), 2)
log.close()
os.execvp(cmd[0], cmd)
' "$name" "$workdir" "$@"
}

ensure_backend() {
  if curl -sf "${CLOUD_URL}/health" >/dev/null; then
    return 0
  fi
  case "${CLOUD_URL}" in
    http://127.0.0.1:8081|http://127.0.0.1:8081/*|http://localhost:8081|http://localhost:8081/*) ;;
    *)
      echo "Cloud backend is not up at ${CLOUD_URL}/health" >&2
      echo "Auto-start only applies to localhost:8081. Start that server yourself." >&2
      exit 1
      ;;
  esac
  echo "Cloud backend is down; starting it"
  start_logged backend "${ROOT}/backend" go run .
  wait_until "cloud backend" 90 curl -sf "${CLOUD_URL}/health" >/dev/null
}

ensure_web() {
  if listening 8080; then
    return 0
  fi
  start_logged web "${ROOT}" "${ROOT}/gradlew" :webApp:wasmJsBrowserDevelopmentRun --console=plain
  wait_until "Wasm dev server on :8080" 240 listening 8080
}

ensure_desktop() {
  if listening 18081; then
    return 0
  fi
  GREETING_E2E=1 start_logged desktop "${ROOT}" "${ROOT}/gradlew" :desktopApp:run --console=plain
  wait_until "desktop E2E bridge on :18081" 180 listening 18081
}

ensure_appium() {
  if curl -sf http://127.0.0.1:4723/status >/dev/null; then
    return 0
  fi
  if ! command -v appium >/dev/null 2>&1; then
    echo "Appium is not installed (needed for ${DRIVER})" >&2
    exit 1
  fi
  start_logged appium "${ROOT}" appium --address 127.0.0.1 --port 4723
  wait_until "Appium on :4723" 40 curl -sf http://127.0.0.1:4723/status >/dev/null
}

ensure_emulator() {
  local serial
  serial="$(adb devices | awk '/^emulator-/{print $1; exit}')"
  if [[ -n "${serial}" ]]; then
    return 0
  fi
  local avd="${GREETING_ANDROID_AVD:-Pixel_9_Pro}"
  if ! emulator -list-avds | grep -qx "${avd}"; then
    avd="$(emulator -list-avds | head -n 1 || true)"
  fi
  if [[ -z "${avd}" ]]; then
    echo "No Android emulator AVD found" >&2
    exit 1
  fi
  local -a emulator_args
  emulator_args=(-avd "${avd}" -no-snapshot-save -netdelay none -netspeed full)
  if [[ "${SHOW_UI:-0}" != "1" ]]; then
    emulator_args+=(-no-window)
  fi
  start_logged emulator "${ROOT}" emulator "${emulator_args[@]}"
  wait_until "Android emulator" 180 bash -c 'adb devices | grep -q "^emulator-.*device"'
  wait_until "Android boot" 180 bash -c '[[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")" == "1" ]]'
}

# Xcode 27 replaced Simulator.app with DeviceHub.app. Older Xcodes still
# ship Simulator.app next to the developer directory.
open_simulator_ui() {
  local dev_dir apps
  dev_dir="$(xcode-select -p)"
  apps="$(cd "${dev_dir}/../Applications" && pwd)"
  local app=""
  if [[ -d "${apps}/Simulator.app" ]]; then
    app="${apps}/Simulator.app"
  elif [[ -d "${dev_dir}/Applications/Simulator.app" ]]; then
    app="${dev_dir}/Applications/Simulator.app"
  elif [[ -d "${apps}/DeviceHub.app" ]]; then
    app="${apps}/DeviceHub.app"
  fi
  if [[ -z "${app}" ]]; then
    echo "No Simulator.app or DeviceHub.app under ${dev_dir}" >&2
    exit 1
  fi
  echo "Opening $(basename "${app}" .app)"
  open "${app}"
}

ensure_simulator() {
  if xcrun simctl list devices booted | grep -q Booted; then
    if [[ "${SHOW_UI:-0}" == "1" ]]; then
      open_simulator_ui
    fi
    return 0
  fi
  local udid="${GREETING_IOS_UDID:-}"
  if [[ -z "${udid}" ]]; then
    udid="$(xcrun simctl list devices available | sed -n 's/.*(\([A-F0-9-]\{36\}\)).*/\1/p' | head -n 1)"
  fi
  if [[ -z "${udid}" ]]; then
    echo "No iOS simulator available to boot" >&2
    exit 1
  fi
  echo "Booting iOS simulator ${udid}"
  xcrun simctl boot "${udid}" >/dev/null 2>&1 || true
  wait_until "booted iOS simulator" 60 bash -c 'xcrun simctl list devices booted | grep -q Booted'
  if [[ "${SHOW_UI:-0}" == "1" ]]; then
    open_simulator_ui
  fi
}

ensure_backend

case "${DRIVER}" in
  web) ensure_web ;;
  desktop) ensure_desktop ;;
  android)
    # run.sh cds into e2e/ before this script. Gradle treats that cwd as the
    # project directory, and e2e/ is not a project in settings.gradle.kts.
    (cd "${ROOT}" && "${ROOT}/gradlew" :androidApp:assembleDebug --console=plain)
    ensure_emulator
    ensure_appium
    ;;
  ios)
    ensure_simulator
    ensure_appium
    ;;
  *)
    echo "Usage: $0 <web|android|ios|desktop>" >&2
    exit 1
    ;;
esac
