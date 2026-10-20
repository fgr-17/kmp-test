#!/usr/bin/env bash
# Print the Android SDK directory. Agents should set ANDROID_HOME.
# The fallbacks match the default install path on each OS.
if [[ -n "${ANDROID_HOME:-}" ]]; then
  printf '%s\n' "${ANDROID_HOME}"
elif [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
  printf '%s\n' "${ANDROID_SDK_ROOT}"
elif [[ "$(uname -s)" == "Darwin" ]]; then
  printf '%s\n' "${HOME}/Library/Android/sdk"
elif [[ -n "${LOCALAPPDATA:-}" ]]; then
  printf '%s\n' "${LOCALAPPDATA}/Android/Sdk"
else
  printf '%s\n' "${HOME}/Android/Sdk"
fi
