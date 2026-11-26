#!/bin/bash
# Focused check of the PUBLISHED signed APK: upgrade over v0.1.0, exit timer-only, dark theme.
fail() { echo "CHECK FAILED: $1"; touch /tmp/lodestill_test_failed; }
rm -f /tmp/lodestill_test_failed
BASE=https://github.com/JoyalGeorge2006/lodestill/releases/download
curl -sL "$BASE/v0.1.0-test/Lodestill-v0.1.0-test.apk" -o old.apk
curl -sL "$BASE/v0.2.0-amoled/Lodestill-v0.2.0-amoled.apk" -o new.apk
sha256sum old.apk new.apk
for f in old.apk new.apk; do echo "== $f"; $ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs $f | grep -i "SHA-256 digest" | head -1; done
OLD=$($ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs old.apk | grep -i "SHA-256 digest" | head -1)
NEW=$($ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs new.apk | grep -i "SHA-256 digest" | head -1)
[ "$OLD" = "$NEW" ] && echo "SIGNER MATCH v0.1.0 and v0.2.0" || fail "signer differs"
adb shell settings put global window_animation_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global animator_duration_scale 0
adb install -r old.apk || fail "install old"
adb install -r new.apk && echo "UPGRADE INSTALL OVER v0.1.0 OK" || fail "upgrade install"
adb shell cmd uimode night yes
adb shell cmd uimode night
adb shell monkey -p dev.joyal.lodestill -c android.intent.category.LAUNCHER 1
sleep 8
adb shell screencap -p /sdcard/p_idle_dark.png
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
tap_text "Start a session" p1.xml || fail "start"
tap_text "Begin" p2.xml || fail "begin"
sleep 3
adb shell screencap -p /sdcard/p_active_dark.png
tap_text "Keep screen on" p3.xml || fail "keep on"
sleep 3
adb shell screencap -p /sdcard/p_timer.png
adb shell input tap 160 320
sleep 3
adb shell uiautomator dump /sdcard/p4.xml; adb pull /sdcard/p4.xml p4.xml
grep -q 'text="Pause"' p4.xml && grep -q 'text="End early"' p4.xml && echo "TIMER EXITED: active screen back" || fail "timer did not exit"
adb shell screencap -p /sdcard/p_after_exit_dark.png
tap_text "SETTINGS" p5.xml || fail "settings"
adb shell screencap -p /sdcard/p_settings_dark.png
adb shell cmd uimode night no
for f in p_idle_dark p_active_dark p_timer p_after_exit_dark p_settings_dark; do adb pull /sdcard/$f.png . || fail "pull $f"; done
test ! -f /tmp/lodestill_test_failed
