#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# A dedicated host prevents the test APK from replacing a user's local workspace.
app_id=com.example.attendance.local.testhost
./gradlew phase3Check -PattendanceApplicationId="$app_id"
if [[ "${1:-}" == "--device" ]]; then
    # Keep the three instrumented suites sequential on the attached device.
    for module in core:data core:device app; do
        ./gradlew ":${module}:connectedDebugAndroidTest" -PattendanceApplicationId="$app_id"
    done
fi
