#!/bin/bash
# Check of the PUBLISHED signed APKs: v0.2.0 -> v0.3.0 upgrade with data retention, then blocked screen.
fail() { echo "CHECK FAILED: $1"; touch /tmp/lodestill_test_failed; }
rm -f /tmp/lodestill_test_failed
P=dev.joyal.lodestill
BASE=https://github.com/JoyalGeorge2006/lodestill/releases/download
curl -sL "$BASE/v0.2.0-amoled/Lodestill-v0.2.0-amoled.apk" -o old.apk
curl -sL "$BASE/v0.3.0-blocked-screen/Lodestill-v0.3.0-blocked-screen.apk" -o new.apk
sha256sum old.apk new.apk
echo "$(sha256sum new.apk | cut -d' ' -f1)" | grep -q 6143442dc40e0dc260b90900b74cd6c6744229a0723d95de9bcfab4af59bee56 && echo "NEW APK HASH MATCHES RELEASE" || fail "hash"
OLD=$($ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs old.apk | grep -i "SHA-256 digest" | head -1)
NEW=$($ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs new.apk | grep -i "SHA-256 digest" | head -1)
echo "$OLD"; echo "$NEW"
[ "$OLD" = "$NEW" ] && echo "SIGNER MATCH v0.2.0 and v0.3.0" || fail "signer differs"
adb shell settings put global window_animation_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global animator_duration_scale 0
adb install -r old.apk || fail "install old"
adb shell dumpsys package $P | grep versionName
adb shell pm grant $P android.permission.POST_NOTIFICATIONS
tap_text() {
for n in 1 2 3 4 5 6; do
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb pull /sdcard/ui.xml "$2" >/dev/null 2>&1
cp "$2" "d_$n_$2" 2>/dev/null
B=$(python3 scripts/tapfind.py "$1" "$2")
echo "tap '$1' attempt $n coords: [$B]"
if [ -n "$B" ]; then adb shell input tap $B; sleep 3; return 0; fi
sleep 2
adb shell input swipe 160 500 160 150 300
sleep 2
done
return 1
}
adb shell monkey -p $P -c android.intent.category.LAUNCHER 1; sleep 8
tap_text "SETTINGS" q1.xml || fail "settings"
tap_text "Block" q2.xml || fail "block seg"
tap_text "I understand, continue" q3.xml || fail "disclosure"
adb shell input keyevent KEYCODE_BACK; sleep 3
tap_text "Distracting apps" q4.xml || fail "distracting"
tap_text "Clock" q5.xml || fail "pick clock"
adb shell screencap -p /sdcard/p_old_apps.png
adb shell input keyevent KEYCODE_BACK; sleep 3
adb shell screencap -p /sdcard/p_old_settings.png
tap_text "TODAY" q6.xml || fail "today"
tap_text "Start a session" q7.xml || fail "start"
tap_text "Begin" q8.xml || fail "begin"
sleep 3
adb shell uiautomator dump /sdcard/q9.xml; adb pull /sdcard/q9.xml q9.xml
grep -q 'text="Pause"' q9.xml && echo "OLD v0.2.0: SESSION ACTIVE" || fail "old session not active"
adb shell screencap -p /sdcard/p_old_session.png
adb install -r new.apk && echo "UPGRADE INSTALL OVER v0.2.0 OK" || fail "upgrade install"
adb shell dumpsys package $P | grep versionName
adb shell settings put secure enabled_accessibility_services $P/$P.service.BlockAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell monkey -p $P -c android.intent.category.LAUNCHER 1; sleep 8
adb shell uiautomator dump /sdcard/q10.xml; adb pull /sdcard/q10.xml q10.xml
grep -q 'text="Pause"' q10.xml && grep -q 'text="End early"' q10.xml && echo "DATA RETAINED: SESSION STILL ACTIVE AFTER UPGRADE" || fail "session lost after upgrade"
adb shell screencap -p /sdcard/p_new_session.png
tap_text "SETTINGS" q11.xml || fail "settings2"
grep -q "1 selected" q11.xml && echo "DATA RETAINED: 1 distracting app selected" || fail "blocked app lost"
adb shell screencap -p /sdcard/p_new_settings.png
adb shell input keyevent KEYCODE_HOME; sleep 2
adb shell settings get secure enabled_accessibility_services
sleep 3
adb shell monkey -p com.google.android.deskclock -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || adb shell monkey -p com.android.deskclock -c android.intent.category.LAUNCHER 1; sleep 8
FOCUS=$(adb shell dumpsys window | grep -E "mCurrentFocus"); echo "blocked focus: $FOCUS"
echo "$FOCUS" | grep -q "$P/$P.BlockedActivity" && echo "BLOCKED SCREEN SHOWN (signed v0.3.0)" || fail "no blocked screen"
adb shell screencap -p /sdcard/p_blocked.png
adb shell uiautomator dump /sdcard/q12.xml; adb pull /sdcard/q12.xml q12.xml
grep -q "Clock is blocked" q12.xml && grep -q "Back to session" q12.xml && grep -q "TIME REMAINING" q12.xml && echo "BLOCKED SCREEN CONTENT OK" || fail "blocked content"
adb shell input tap $(python3 scripts/tapfind.py "Back to session" q12.xml)
sleep 4
FOCUS=$(adb shell dumpsys window | grep -E "mCurrentFocus"); echo "after Back to session: $FOCUS"
echo "$FOCUS" | grep -q "$P/$P.MainActivity" && echo "BACK TO SESSION OK" || fail "back to session"
adb shell screencap -p /sdcard/p_after_back.png
adb shell input keyevent KEYCODE_HOME; sleep 3
adb shell monkey -p com.google.android.deskclock -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 7
FOCUS=$(adb shell dumpsys window | grep -E "mCurrentFocus"); echo "second block: $FOCUS"
echo "$FOCUS" | grep -q "BlockedActivity" || fail "second block"
adb shell input keyevent KEYCODE_BACK; sleep 4
FOCUS=$(adb shell dumpsys window | grep -E "mCurrentFocus"); echo "after system Back: $FOCUS"
echo "$FOCUS" | grep -q "$P/$P.MainActivity" && echo "SYSTEM BACK OK" || fail "system back"
adb shell monkey -p com.google.android.deskclock -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 7
adb shell input keyevent KEYCODE_HOME; sleep 4
FOCUS=$(adb shell dumpsys window | grep -E "mCurrentFocus"); echo "after Home: $FOCUS"
echo "$FOCUS" | grep -q "BlockedActivity" && fail "Home stuck on blocked screen" || echo "HOME OK"
adb shell cmd uimode night yes; sleep 2
adb shell monkey -p com.google.android.deskclock -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 7
adb shell screencap -p /sdcard/p_blocked_dark.png
adb shell cmd uimode night no
adb logcat -d | grep -i "BlockSvc" | tail -20
for f in p_old_apps p_old_settings p_old_session p_new_session p_new_settings p_blocked p_after_back p_blocked_dark; do adb pull /sdcard/$f.png . || fail "pull $f"; done
test ! -f /tmp/lodestill_test_failed && echo "ALL CHECKS PASSED"
test ! -f /tmp/lodestill_test_failed
