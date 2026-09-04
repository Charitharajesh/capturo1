#!/usr/bin/env bash
# Runs inside the reactivecircus/android-emulator-runner action, against the
# emulator it just booted. Kept as a real script file (rather than inline in
# the workflow YAML) because that action executes each line of a multi-line
# `script:` block as its own separate shell invocation, which breaks
# multi-line constructs like `for ... do ... done`.
set -euo pipefail

echo "Installing the debug APK..."
adb install -r frontend/app/build/outputs/apk/debug/app-debug.apk

cd appium-mobile

echo "Starting Appium server..."
npx appium --log-level info --port 4723 > appium.log 2>&1 &

echo "Waiting for Appium to be ready..."
ready=0
for i in $(seq 1 30); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:4723/status || true)
  if [ "$code" = "200" ]; then
    echo "Appium is ready"
    ready=1
    break
  fi
  sleep 2
done

if [ "$ready" != "1" ]; then
  echo "Appium never became ready; server log:"
  cat appium.log || true
  exit 1
fi

npm test
