#!/usr/bin/env bash
#
# Capture App Store / Play Store screenshots for Mars Rover Photos.
# Runs the Maestro flow twice — once in light theme, once in dark — and restores the theme.
# Does NOT clear app data: favorite the photos you want shown on the Favorites screen first.
#
# Usage:
#   ./.maestro/capture.sh android   # runs on a connected Android device/emulator (adb)
#   ./.maestro/capture.sh ios       # runs on the booted iPhone simulator (simctl)
#   ./.maestro/capture.sh ipad      # runs on the booted iPad simulator → screenshots/ipad/
#
# The target device is PINNED so Maestro can't pick the wrong one when several are connected:
#   - Android: $ANDROID_SERIAL if set, else `adb get-serialno` (the single connected device).
#   - iOS:     $IOS_TARGET if set, else the currently booted simulator's UDID.
# Override to choose a specific target, e.g.  IOS_TARGET=<udid> ./.maestro/capture.sh ios
#
# Prerequisites: Maestro installed, the device/sim booted with the app installed, network access.
# Screenshots land in .maestro/screenshots/<platform>/.
set -euo pipefail

PLATFORM="${1:-}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$DIR/screenshots/$PLATFORM"

run_flow() {
  local theme="$1"
  echo "==> Capturing $PLATFORM ($theme theme) on device ${DEVICE}..."
  ( cd "$DIR" && maestro --device "$DEVICE" test \
      -e THEME="$theme" -e PLATFORM="$PLATFORM" store-screenshots.yaml )
}

case "$PLATFORM" in
  android)
    DEVICE="${ANDROID_SERIAL:-$(adb get-serialno)}"
    set_theme() { adb -s "$DEVICE" shell "cmd uimode night $1" >/dev/null; }
    set_theme no;  run_flow light
    set_theme yes; run_flow dark
    set_theme no   # restore
    ;;
  ios)
    # Prefer a booted iPhone (an iPad may also be booted for the `ipad` target).
    DEVICE="${IOS_TARGET:-$(xcrun simctl list devices booted | grep -i iphone | grep -oE '[0-9A-Fa-f-]{36}' | head -1)}"
    if [ -z "$DEVICE" ]; then echo "No booted iPhone simulator found. Boot one or set IOS_TARGET." >&2; exit 1; fi
    set_theme() { xcrun simctl ui "$DEVICE" appearance "$1" >/dev/null; }
    set_theme light; run_flow light
    set_theme dark;  run_flow dark
    set_theme light  # restore
    ;;
  ipad)
    # Same as iOS but pins a booted iPad and writes to screenshots/ipad/ (a separate App Store size).
    DEVICE="${IOS_TARGET:-$(xcrun simctl list devices booted | grep -i ipad | grep -oE '[0-9A-Fa-f-]{36}' | head -1)}"
    if [ -z "$DEVICE" ]; then echo "No booted iPad simulator found. Boot one or set IOS_TARGET." >&2; exit 1; fi
    set_theme() { xcrun simctl ui "$DEVICE" appearance "$1" >/dev/null; }
    set_theme light; run_flow light
    set_theme dark;  run_flow dark
    set_theme light  # restore
    # A landscape iPad is captured into the native PORTRAIT framebuffer (2064x2752) PLUS an EXIF
    # Orientation=6 tag. Just rotating pixels (sips -r) leaves the tag, so Finder/Preview re-rotate
    # and the image looks sideways. Rotate 90° CW AND strip all metadata so every viewer agrees.
    if ! python3 - "$DIR/screenshots/ipad" 2>/dev/null <<'PY'
import sys, glob, os
from PIL import Image
for p in glob.glob(os.path.join(sys.argv[1], "*.png")):
    im = Image.open(p).rotate(-90, expand=True)        # 90° clockwise
    clean = Image.new(im.mode, im.size)
    clean.putdata(list(im.getdata()))                  # drop EXIF/XMP/orientation entirely
    clean.save(p)
PY
    then
      echo "WARN: Pillow (python3 -m pip install pillow) unavailable — falling back to sips; the" >&2
      echo "      EXIF orientation tag will remain and Finder/Preview may show frames rotated." >&2
      for f in "$DIR/screenshots/ipad/"*.png; do sips -r 90 "$f" >/dev/null 2>&1; done
    fi
    ;;
  *)
    echo "Usage: $0 {android|ios|ipad}" >&2
    exit 1
    ;;
esac

echo "==> Done. Screenshots in: $DIR/screenshots/$PLATFORM/"
ls -1 "$DIR/screenshots/$PLATFORM/"
