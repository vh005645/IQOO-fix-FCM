#!/usr/bin/env bash
set -euo pipefail

# Run on a computer with Android Platform Tools installed and USB debugging enabled.
# This changes Android's device-idle (Doze) whitelist only; it does NOT grant
# OriginOS autostart/background-run permissions and may be reset by ROM updates.
ADB="${ADB:-adb}"

"$ADB" devices
for package in com.google.android.gms com.google.android.gsf com.fcmkeeper.iqoo; do
  echo "Adding $package to device-idle whitelist (if supported)..."
  "$ADB" shell dumpsys deviceidle whitelist "+$package" || {
    echo "Could not whitelist $package; check device authorization / ROM restrictions." >&2
  }
done

echo
echo "Current matching whitelist entries:"
"$ADB" shell dumpsys deviceidle whitelist | grep -E 'com\.google\.android\.gms|com\.google\.android\.gsf|com\.fcmkeeper\.iqoo' || true
echo
echo "Now check OriginOS 5 app info manually for Background power consumption / Autostart."
