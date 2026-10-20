#!/usr/bin/env bash
# Stop the local processes this repo starts for the notes demo and E2E.
# Does not stop the Gradle daemon or the IDE.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

stop_pid() {
  local pid="$1"
  local why="$2"
  if ! kill -0 "$pid" 2>/dev/null; then
    return 0
  fi
  echo "Stopping ${why} (pid ${pid})"
  kill "$pid" 2>/dev/null || true
}

stop_port() {
  local port="$1"
  local pids
  pids="$(lsof -nP -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)"
  if [[ -z "${pids}" ]]; then
    return 0
  fi
  local pid
  for pid in ${pids}; do
    stop_pid "$pid" "listener on :${port}"
  done
}

stop_repo_tasks() {
  local pattern pid cwd
  for pattern in "wasmJsBrowserDevelopmentRun" ":desktopApp:run" "desktopApp:run"; do
    while read -r pid; do
      [[ -z "${pid}" ]] && continue
      if [[ "${pid}" == "$$" || "${pid}" == "${PPID}" ]]; then
        continue
      fi
      cwd="$(lsof -a -p "${pid}" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' || true)"
      if [[ "${cwd}" == "${ROOT}" || "${cwd}" == "${ROOT}/"* ]]; then
        stop_pid "${pid}" "${pattern}"
      fi
    done < <(pgrep -f "${pattern}" || true)
  done
}

if command -v docker >/dev/null 2>&1; then
  echo "Stopping docker compose api (if it is up)"
  docker compose -f "${ROOT}/docker-compose.yml" down --remove-orphans >/dev/null 2>&1 || true
fi

stop_repo_tasks
stop_port 8080
stop_port 8081
stop_port 18081
stop_port 4723

ADB="$(bash "${ROOT}/scripts/android_sdk_dir.sh")/platform-tools/adb"
if [[ -x "${ADB}" ]]; then
  while read -r serial; do
    [[ -z "${serial}" ]] && continue
    echo "Stopping Android emulator ${serial}"
    "${ADB}" -s "${serial}" emu kill >/dev/null 2>&1 || true
  done < <("${ADB}" devices | awk '/^emulator-/{print $1}')
fi

if command -v xcrun >/dev/null 2>&1; then
  echo "Shutting down booted iOS simulators"
  xcrun simctl shutdown booted >/dev/null 2>&1 || true
fi

sleep 0.4
# Second pass: a parent may have exited and left the port, or SIGTERM was ignored.
stop_port 8080
stop_port 8081
stop_port 18081
stop_port 4723
for port in 8080 8081 18081 4723; do
  pids="$(lsof -nP -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)"
  for pid in ${pids}; do
    echo "Force-stopping leftover listener on :${port} (pid ${pid})"
    kill -9 "${pid}" 2>/dev/null || true
  done
done

echo "Down."
