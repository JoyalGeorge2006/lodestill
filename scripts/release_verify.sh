#!/bin/bash
# Installs the signed release APK on the running emulator and checks it.
# Kept as a file because the emulator runner executes workflow script lines one by one,
# which cannot run a multi-line shell function.
rm -f /tmp/lodestill_test_failed
adb install -r app-release.apk || touch /tmp/lodestill_test_failed
adb shell pm dump dev.joyal.lodestill | grep -m2 "versionName\|versionCode" || touch /tmp/lodestill_test_failed
adb shell settings put global window_animation_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global animator_duration_scale 0
adb shell monkey -p dev.joyal.lodestill -c android.intent.category.LAUNCHER 1 || touch /tmp/lodestill_test_failed
sleep 8
FOCUS=$(adb shell dumpsys window | grep -E "mCurrentFocus"); echo "release focus: $FOCUS"
echo "$FOCUS" | grep -q "dev.joyal.lodestill/dev.joyal.lodestill.MainActivity" || touch /tmp/lodestill_test_failed
adb shell screencap -p /sdcard/release_light.png || touch /tmp/lodestill_test_failed
# tap_text TEXT OUTFILE: dump UI up to 6 times, tap centre of first node whose text matches
tap_text() {
  for n in 1 2 3 4 5 6; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb pull /sdcard/ui.xml "$2" >/dev/null 2>&1
    B=$(python3 scripts/tapfind.py "$1" "$2")
    echo "tap '$1' attempt $n coords: [$B]"
    if [ -n "$B" ]; then adb shell input tap $B; sleep 3; return 0; fi
    sleep 3
  done
  return 1
}
tap_text "Start a session" ui1.xml || touch /tmp/lodestill_test_failed
tap_text "Begin" ui2.xml || touch /tmp/lodestill_test_failed
sleep 4
adb shell uiautomator dump /sdcard/ui3.xml || touch /tmp/lodestill_test_failed
adb pull /sdcard/ui3.xml ui3.xml || touch /tmp/lodestill_test_failed
grep -q 'text="End early"' ui3.xml && grep -q 'text="Pause"' ui3.xml && echo "SESSION ACTIVE ON RELEASE APK" || { echo "session active markers missing"; touch /tmp/lodestill_test_failed; }
adb shell screencap -p /sdcard/release_active.png || touch /tmp/lodestill_test_failed
tap_text "Keep screen on" ui4.xml || touch /tmp/lodestill_test_failed
sleep 2
adb shell dumpsys window | grep -E "mCurrentFocus"
adb shell dumpsys window | grep -E "FLAG_KEEP_SCREEN_ON|keepScreenOn" | head -3 || true
adb shell screencap -p /sdcard/release_timer.png || touch /tmp/lodestill_test_failed
adb shell settings put system ui_night_mode 2 || touch /tmp/lodestill_test_failed
sleep 3
adb shell monkey -p dev.joyal.lodestill -c android.intent.category.LAUNCHER 1 || touch /tmp/lodestill_test_failed
sleep 4
adb shell screencap -p /sdcard/release_dark.png || touch /tmp/lodestill_test_failed
adb shell settings put system ui_night_mode 0 || true
adb pull /sdcard/release_light.png . || touch /tmp/lodestill_test_failed
adb pull /sdcard/release_active.png . || touch /tmp/lodestill_test_failed
adb pull /sdcard/release_timer.png . || touch /tmp/lodestill_test_failed
adb pull /sdcard/release_dark.png . || touch /tmp/lodestill_test_failed
test ! -f /tmp/lodestill_test_failed
